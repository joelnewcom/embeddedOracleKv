/*-
 * Copyright (C) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This file was distributed by Oracle as part of a version of Oracle NoSQL
 * Database made available at:
 *
 * http://www.oracle.com/technetwork/database/database-technologies/nosqldb/downloads/index.html
 *
 * Please see the LICENSE file included in the top-level directory of the
 * appropriate version of Oracle NoSQL Database for a copy of the license and
 * additional information.
 */

package oracle.kv.impl.query.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import oracle.kv.impl.api.table.EnumDefImpl;
import oracle.kv.impl.api.table.FieldDefImpl;
import oracle.kv.impl.api.table.FieldValueImpl;
import oracle.kv.impl.api.table.IndexImpl;
import oracle.kv.impl.api.table.IndexImpl.IndexField;
import oracle.kv.impl.api.table.IndexKeyImpl;
import oracle.kv.impl.api.table.PrimaryKeyImpl;
import oracle.kv.impl.api.table.TableImpl;
import oracle.kv.impl.api.table.TablePath;
import oracle.kv.impl.api.table.TimestampDefImpl;
import oracle.kv.impl.query.QueryStateException;
import oracle.kv.impl.query.compiler.Expr.ExprIter;
import oracle.kv.impl.query.compiler.Expr.ExprKind;
import oracle.kv.impl.query.compiler.FunctionLib.FuncCode;
import oracle.kv.impl.query.compiler.ExprMapFilter.FilterKind;
import oracle.kv.table.FieldDef.Type;
import oracle.kv.table.FieldRange;

/**
 * An IndexAnalyzer is associated with a SFW expr and an index I on a table T
 * that appears in the FROM clause of the SFW. I may be the primary index of
 * T. The IndexAnalyzer tries to find the best way to replace a full table scan
 * on T with an index scan on I. It does so by examinig the exprs appearing in
 * SFW clauses to determine which ones can be evaluated by the info stored in
 * the index.
 *
 * theQCB:
 * The QueryControlBlock of the query containing the SFW expr.
 *
 * theSctx:
 * The StaticContext associated with the SFW expr.
 *
 * theSFW:
 * The SFW expr to analyze.
 *
 * theIndex:
 * The index I to analyze. If I is the primary index, theIndex is null.
 *
 * theTable:
 * The table T referenced by the SFW and indexed by I.
 *
 * theIsPrimary:
 * Whether I is T's primary index.
 *
 * theIsMapBothIndex:
 * Whether I is a MapBoth index, i.e., an index that indexes both the keys and
 * the elements of a map, with the keys indexed before all the element paths.
 *
 * theNumFields:
 * the number of index fields in I.
 *
 * theIsHintIndex:
 * Whether I is an index named in a PREFER_INDEXES hint.
 *
 * theStartStopPreds:
 * A list of predicates that may be used as start/stop conditions for the index
 * scan (i.e. they determine the scan boundaries). The list actually stores
 * PredInfo objects, which store the pred expr as well as some assosiated
 * meta-info. This list is built by the collectIndexPreds() method. See there
 * for more details.
 *
 * theFilteringPreds:
 * A list of preds can be evaluated from the index columns only, without
 * retrieving the full rows from the table. Such preds are evaluated during an
 * index scan to filter index entries within the scan boundaries, thus avoiding
 * retrieval of associated table rows. Initially, theFilteringPreds may contain
 * some (or all) of the preds that appear in theStartStopPreds (as well as other
 * preds that are not start/stop preds). If a start/stop pred is actually pushed
 * to the index, it will be removed from theFilteringPreds (if there). After
 * pushing all the pushable start/stop preds, any remaining preds in
 * theFilteringPreds will also be pushed as filtering preds. theFilteringPreds
 * list is built by the collectIndexPreds() method. See there for more details.
 *
 * NOTE: In case of non-mullti-key indexes, theFilteringPreds will initially
 * contain all the preds in theStartStopPreds. In case of multi-key indexes,
 * preds that reference the multi-key columns of the index may be pushable as
 * start/stop preds, but cannot not be used as filtering preds.
 *
 * theHaveMapBothPreds:
 * True if the query has any "MapBoth" preds. This can happen only if the index
 * is a MapBoth one. MapBoth preds are best explaned by some examples....
 * Consider the following DDL:
 *
 * CREATE TABLE foo (
 *     id INTEGER,
 *     col1 INTEGER,
 *     col2 INTEGER,
 *     map MAP(RECORD(mf1 INTEGER, mf2 INTEGER, mf3 INTEGER)),
 *     primary key (id)
 * )
 *
 * CREATE INDEX mapidx on foo (keys(map),
 *                             map[].mf1,
 *                             col2,
 *                             map[].mf2,
 *                             map[].mf3)
 *
 * mapidx is a MapBoth index. Let ICi denote the i-th field/column of the
 * index.
 *
 * A "MapBoth" predicate is a value-comparison pred on one of the map-value
 * index paths, for a particular map key. For example: map.key1.mf1 = 3, and
 * map.key10.mf3 > 10 are both MapBoth preds. Notice that a MapBoth pred is
 * equivalent to an existentially-quantified expr with 2 conditions; for
 * example map.key1.mf1 = 3 is equivalent to:
 * [some $key in keys(map) satisfies $key = "key1" and map.$key = 3].
 * A MapBoth pred may be pushed to a MapBoth index as 2 start/stop preds:
 * an equality pred on the map-key field, and an equality or range pred on
 * a map-value field. Here are some example queries (only the WHERE clause
 * shown):
 *
 * Q1. where map.key10.mf1 > 10
 * The pred can be pushed to the index as [IC1 = "key10" and IC2 > 10]
 *
 * Q2. where map.key5.mf1 = 3 and col2 = 20 and map.key5.mf2 = 5.
 * All preds can be pushed as [IC1 = "key5" and IC2 = 3 and IC3 = 20 and
 * IC4 = 5]
 *
 * Q3. where map.key5.mf1 = 3 and map.key5.mf2 = 5.
 * Both preds is pushable, the 2nd as a filtering pred:
 * [IC1 = "key5" and IC2 = 3 and (filtering) IC4 = 5]
 *
 * Q4. where map.key5.mf1 = 3 and (map.key5.mf2 < 5 or map.key5.mf2 > 15)
 * Both preds is pushable, the 2nd as a filtering pred:
 * [IC1 = "key5" and IC2 = 3 and (filtering) (IC4 < 5 or IC4 > 15)]
 *
 * Q5. where map.key5.mf1 = 3 and col2 = 20 and map.key6.mf2 = 5.
 * The 1st 2 preds can be pushed as [IC1 = "key5" and IC2 = 3 and
 * IC3 = 20]. Alternatively, the 3rd pred can be pushed as
 * [IC1 = "key6" and (filtering) IC4 = 5]. IndexAnalyzer has to choose one of
 * the map keys (key5 or key6). In this case it will to push on key5, as it
 * probably better than key6.
 *
 * Q6. where map.key5.mf1 = 3 and col2 = 20 and map.key6.mf1 = 5.
 * We have a choice whether to push the 1st or the 3rd pred. In this case
 * their "value" is the same, so it doesn't matter which. If we choose
 * the 1st, we push [IC1 = "key5" and IC2 = 3 and IC3 = 20].
 *
 * Q7. where map.key5.mf1 > 3 and col2 = 20 and map.key6.mf1 = 5.
 * We have a choice whether to push the 1st or the 3rd pred. But pushing
 * the 3rd pred is probably better, so we push [IC1 = "key6" and IC2 = 5
 * and IC3 = 20].
 *
 * Q8. where map.key5.mf1 = 3 and col2 = 20 and map.key6.mf1 = 5 and
 *           map.key6.mf2 < 30
 * We can push "key5" preds or "Key6" preds, but "key6" is probably better
 * (because it has more pushable preds), so we push [IC1 = "key6" and
 * IC2 = 5 and IC3 = 20 and IC4 < 30].
 *
 * Notice that if theHaveMapBothPreds is true, the index should basically
 * be treated as a simple index, instead of a multi-key index.
 *
 * theMapBothKeys:
 * A map storing map keys for which MapBoth preds exist. the map associates
 * each such key with a "score", which is used to select the "best" map key
 * on which to push.
 *
 * theMapBothKey:
 * The "best" map key among the one that have MapBoth preds on them.
 *
 * theHaveMapKeyEqPred:
 * Set to true (a) if the index is a map index indexing the map keys (it may
 * also index the values), (b) the query contains a pred of the form
 * keys(map) =any "foo", and (c) the pred can be pushed to the index. In this
 * case there is no need to do duplicate elimination.
 *
 * thePushedAnyPred:
 * Set to true if a pred with an any-comp-op is pused to the index scan.
 * If this happens, no other such pred may be pused.
 *
 * thePrimaryKey:
 * A potentially partial primary key used as a start/end index condition
 * (together with theRange, if any) for the primary index. It "contains"
 * pushed equality predicates on primary-key columns.
 *
 * theSecondaryKey:
 * A potentially partial secondary key used as a start/end index condition
 * (together with theRange, if any) for a secondary index. It "contains"
 * pushed equality predicates (including partially pushed predicates) on
 * secondary-index columns.
 *
 * theRange:
 * It contains the pushed min and/or max preds (at most one min pred and
 * at most one max pred).
 *
 * thePushedPreds:
 * The preds that can actually be pushed into the index as start/end conditions.
 * thePushedPreds is maintained so that these preds can be removed from the
 * WHERE clause if the index is actually applied. thePushedPreds does not
 * include partially pushed preds, as those should not be removed from the
 * WHERE clause. It is also used in determining whether the index is a covering
 * one.
 *
 * thePushedExternals:
 * This is used to handle the cases where a pushable pred contains external
 * variables, eg, foo = $x + $y, where foo is an indexed column of theTable.
 * If foo is an integer, we initially create a placeholder FieldValue with
 * value 0, and place it in the IndexKey or PrimaryKey or FieldRange (i.e.,
 * we push the pred foo = 0). thePushedExternals is then used to register
 * the $x + $y expr. thePushedExternals has one entry for each index field
 * on which an equality pred is pushed, and 2 entries for the single index
 * field on which a FieldRange is pushed. The ordering of the entries in
 * thePushedExternals is the same as the declaration order of the associated
 * index fields. If the predicate(s) pushed on an index field do not have
 * any external vars, the associated entry(s) in thePushedExternals is null.
 * If the current index is applied, thePushedExternals will be copied into
 * the associated ExprBaseTable, and during code generation, it will be
 * converted to an array of PlanIters and placed in the BaseTableIter.
 * During BaseTableIter.open(), the PlanIters stored in
 * BaseTableIter.thePushedExternals, will be evaluated and the resulting
 * values will be used to replace the associated placeholders.
 *
 * theHavePushedExternals:
 * Set to true if at least one entry in thePushedExternals is non-null.
 *
 * theIsMultiKeyRange:
 *
 * theIsCovering:
 * Set to true if the index is a covering one, i.e., the whole query can be
 * evaluated from the index columns, without any need to retrieve any table
 * rows.
 *
 * theEliminateDups:
 *
 * theScore:
 * A crude metric of how effective the index is going to be in optimizing
 * table access. See getScore() method.
 *
 * theScore2:
 * Same as theScore, but without any special treatment for the complete-key
 * case. See getScore() method.
 *
 * theNumEqPredsPushed:
 * The number of equality predicates pushed as start/stop conditions. It
 * includes partially pushed preds. Used to compute theScore and theScore2
 * for each each index in order to choose the "best" applicable index (see
 * getScore() and compareTo() methods).
 */
class IndexAnalyzer implements Comparable<IndexAnalyzer> {

    /*
     * The relative value of each kind of predicate. Used to compute a
     * score for each each index in order to choose the "best" applicable
     * index (see getScore() and compareTo() methods).
     */
    final static int eqValue = 32;
    final static int vrangeValue = 16; // value-range pred
    final static int arangeValue = 8;  // any-range pred
    final static int filterEqValue = 16;
    final static int filterOtherValue = 8;

    final QueryControlBlock theQCB;

    final StaticContext theSctx;

    ExprSFW theSFW;

    final TableImpl theTable;

    final IndexImpl theIndex;

    final boolean theIsPrimary;

    final boolean theIsMapBothIndex;

    final int theNumFields;

    final boolean theIsHintIndex;

    final ArrayList<ArrayList<PredInfo>> theStartStopPreds;

    final ArrayList<Expr> theFilteringPreds;

    boolean theHaveMapBothPreds;

    Map<String, MapBothKeyInfo> theMapBothKeys;

    MapBothKeyInfo theMapBothKey;

    boolean theHaveMapKeyEqPred;

    boolean thePushedAnyPred;

    PrimaryKeyImpl thePrimaryKey;

    IndexKeyImpl theSecondaryKey;

    FieldRange theRange;

    final ArrayList<Expr> thePushedPreds;

    final ArrayList<Expr> thePushedExternals;

    boolean theHavePushedExternals;

    boolean theIsMultiKeyRange;

    boolean theIsCovering;

    boolean theEliminateDups;

    final IndexPathExpr thePath;

    int theScore = -1;

    int theScore2 = -1;

    int theNumEqPredsPushed = 0;

    /**
     * Information about a start/stop predicate. A predicate P is a start/stop
     * pred w.r.t. an index path IP if:
     *
     * a. P is of the form QP op const_expr or const_expr op QP,
     * b. op is a comparison operator (value-comparison or any-comparison) other
     *    than != and !=any.
     * c. const_expr is an expr that is built pout of literals and/or extrernal
     *    vars only (does not reference and internal vars or tables).
     * d. const_expr returns exactly one item, whose type is equal to the
     *    itemtype of QP
     * e. QP is a path expr that "matches" with IP. Such a "match" is defined
     *    as follows:
     *
     * 1. QP and IP match if they are identical.
     *
     * 2. IP has [] and QP does't.
     * If QP has a step that produces arrays and the next step is not [], []
     * is added to QP in order to then attempt an exact match with IP. Notice that
     * an IP over an array will always contain [].
     *
     * 3. QP may contain a slice or a filtering step that may project-out
     * elements from the input apprays/maps. Such a slice/filter step is
     * "partially" matched with a [] step in IP, at the same position. For
     * example, the path expr foo.array[1:5].f matches partially with index
     * path foo.array[].f. A partially matched pred is treated by leaving the
     * original pred in the query and pushing to the index a pred that is the
     * same as the original but with the expr(s) inside the [] removed.
     *
     * 4. A "MapBoth" match. In this case, the index is a "MapBoth" index.
     * QP selects the value associated with a specific map key, and IP selects
     * all the values of the same map. For example QP = foo.map.someKey.bar and
     * IP = foo.map[].bar. A MapBoth match results to two preds being pushed
     * down to the map index, as described in the header javadoc of the
     * IndexAnalyzer class.
     *
     * thePred:
     * The pred expr.
     *
     * theCompOp:
     * The operator in the predicate. If P is of the form const_expr op QP the
     * operator is flipped when P is "stored" in a PredInfo, so that it appears
     * as QP theCompOp const_expr. If the operator is an "any" comp op, theCompOp
     * actually stores the corresponding value-comp op.
     *
     * theIsAnyOp:
     * Whether the pred operator is an "any" comp op.
     *
     * theConstExpr:
     * The const_expr, as defined above.
     *
     * theConstVal
     * If theConstExpr is actually a single literal (ExprConst), theConstVal is
     * the FieldValue stored in theConstExpr.
     *
     * theDoesFiltering:
     * Whether QP includes a non-empty filtering or slicing step, in which case
     * it is matched partially with IP.
     *
     * theIsUnnnested:
     * Whether the input to QP is a FROM var, whose domain expr is multi-valued,
     * in which case it is matched partially with IP. Such a pred does not apply
     * to a table directly, but to an unnested version of the table.
     *
     * theIPathPos:
     * The position of the index path that this pred may be pushed to.
     *
     * theMapKey:
     * If the pred is MapBoth othe, stores the associated map key.
     *
     * theStatus:
     * See the PredicateStatus enum below.
     */
    private class PredInfo {

        Expr thePred;

        FuncCode theCompOp;

        boolean theIsAnyOp;

        Expr theConstExpr;

        FieldValueImpl theConstVal;

        boolean theDoesFiltering;

        boolean theIsDirect;

        boolean theIsUnnested;

        int theIPathPos = -1;

        String theMapKey;

        PredicateStatus theStatus;

        PredInfo(Expr pred) {
            thePred = pred;
        }

        boolean isEq() {
            return theCompOp == FuncCode.OP_EQ;
        }

        boolean isMin() {
            return (theCompOp == FuncCode.OP_GT ||
                    theCompOp == FuncCode.OP_GE);
        }

        boolean isMax() {
            return (theCompOp == FuncCode.OP_LT ||
                    theCompOp == FuncCode.OP_LE);
        }

        boolean isMapBoth() {
            return theMapKey != null;
        }

        boolean isMultiKey() {
            return (theIndex != null &&
                    theMapKey == null &&
                    theIndex.getIndexPath(theIPathPos).isMultiKey());
        }

        boolean isInclusive() {
            return (theCompOp == FuncCode.OP_GE ||
                    theCompOp == FuncCode.OP_LE);
        }

        boolean isPartial() {
            return theDoesFiltering || theIsUnnested;
        }
    }

    /**
     * The values of this enum are possible outcomes of "comparing" two
     * start/stop preds on the same index path. Unless one pred is a min and
     * the other a max, only one of the preds can actually be pushed as a
     * start/stop pred; the other one will be always tru, or always false,
     * or it will be kept and pushed as a filtering pred.
     */
    static enum PredicateStatus {

        /*
         * The pred is a start/stop pred and, at least for now, it should be
         * included in theStartStopPreds.
         */
        KEEP,

        /*
         * The pred should not be included in theStartStopPreds because it
         * "conflicts" with another pred on the same index path.
         */
        SKIP,

        /*
         * The pred is always false. This makes the whole WHERE expr always
         * false.
         */
        ALWAYS_FALSE,

        /*
         * The pred is always true, so it can be removed from the WHERE expr.
         */
        ALWAYS_TRUE
    }

    /**
     * This class serves as an alternative representation of a query path expr.
     * This representation (which includes some additional metadata for the
     * path expr) is used to check whether a query path expr "matches" with an
     * index path. See the javadoc for the PredInfo class above for the
     * definition of a "match" between a query path expr and an index path.
     *
     * thePosition:
     * The ordinal number of the index path that matches with "this" query
     * path. -1 if no match actually exists.
     *
     * theIsMultiKey:
     * True if the path expr may produce more than one items from each of
     * its input items.
     *
     * theMapBothKey:
     * If this path expr selects the value associated with a specific key of
     * a map, theMapBothKey is that specific map key.
     *
     * theDoesFiltering: See PredInfo.theDoesFiltering
     *
     * theIsUnnested: See PredInfo.theIsUnnested
     */
    private static class IndexPathExpr extends TablePath {

        int thePosition = -1;

        boolean theIsMultiKey;

        String theMapBothKey;

        boolean theDoesFiltering;

        boolean theIsDirect;

        boolean theIsUnnested;

        public IndexPathExpr(TableImpl table, String field) {
            super(table, field);
        }

        boolean matched() {
            return thePosition >= 0;
        }

        void setIsMapValue() {
            theIsMultiKey = true;
        }

        @Override
        public void clear() {
            super.clear();
            thePosition = -1;
            theIsMultiKey = false;
            theMapBothKey = null;
            theDoesFiltering = false;
            theIsDirect = true;
            theIsUnnested = false;
        }
    }

    IndexAnalyzer(ExprSFW sfw, TableImpl table, IndexImpl index) {

        theSFW = sfw;
        theQCB = theSFW.getQCB();
        theSctx = theSFW.getSctx();
        theTable = table;
        theIndex = index;
        theIsPrimary = (theIndex == null);

        theNumFields = (theIsPrimary ?
                        theTable.getPrimaryKeySize() :
                        theIndex.numFields());

        ExprBaseTable tableExpr = sfw.getTableExpr();
        theIsHintIndex = tableExpr.isIndexHint(theIndex);

        theStartStopPreds = new ArrayList<ArrayList<PredInfo>>(theNumFields);
        theFilteringPreds = new ArrayList<Expr>();
        thePushedPreds = new ArrayList<Expr>();

        for (int i = 0; i < theNumFields; ++i) {
            theStartStopPreds.add(null);
        }

        if (theIndex != null) {
            if (theIndex.isMapBothIndex()) {
                theIsMapBothIndex = true;
                theMapBothKeys = new HashMap<String, MapBothKeyInfo>();
            } else {
                theIsMapBothIndex = false;
            }
        } else {
            theIsMapBothIndex = false;
        }

        thePushedExternals = new ArrayList<Expr>();

        thePath = new IndexPathExpr(table, null/*path*/);
    }

    private void reset() {

        theStartStopPreds.clear();
        theFilteringPreds.clear();
        thePushedPreds.clear();

        for (int i = 0; i < theNumFields; ++i) {
            theStartStopPreds.add(null);
        }

        if (theIsMapBothIndex) {
            theMapBothKeys.clear();
        }

        thePushedExternals.clear();
        theHavePushedExternals = false;

        theMapBothKey = null;
        theHaveMapBothPreds = false;
        theHaveMapKeyEqPred = false;
        thePushedAnyPred = false;
        thePrimaryKey = null;
        theSecondaryKey = null;
        theRange = null;
        theIsMultiKeyRange = false;
        theIsCovering = false;
        theEliminateDups = false;
    }

    boolean hasShardKey() {
        return (theIsPrimary &&
                thePrimaryKey != null &&
                thePrimaryKey.hasShardKey());
    }

    /**
     * Remove a pred from the WHERE clause. The pred has either been
     * pushed in the index or is always true.
     */
    private void removePred(Expr pred) {

        Expr whereExpr = theSFW.getWhereExpr();

        if (pred == whereExpr) {
            theSFW.removeWhereExpr(true/*destroy*/);
        } else {
            whereExpr.removeChild(pred, true/*destroy*/);

            if (whereExpr.getNumChildren() == 0) {
                theSFW.removeWhereExpr(true/*destroy*/);
            }
        }
    }

    /*
     * The whole WHERE expr was found to be always false. Replace the
     * whole SFW expr with an empty expr.
     */
    private void processAlwaysFalse(Expr pred) {

        reset();
        Function empty = Function.getFunction(FuncCode.OP_CONCAT);
        Expr emptyExpr = ExprFuncCall.create(theQCB, theSctx,
                                             pred.getLocation(),
                                             empty,
                                             new ArrayList<Expr>());
        if (theQCB.getRootExpr() == theSFW) {
            theQCB.setRootExpr(emptyExpr);
        } else {
            theSFW.replace(emptyExpr, true);
        }
        theSFW = null;
    }

    /**
     * Used to sort the IndexAnalyzers in decreasing "value" order, where
     * "value" is a heuristic estimate of how effective the associated
     * index is going to be in optimizing the query.
     */
    @Override
    public int compareTo(IndexAnalyzer other) {

        int numFields1 = theNumFields;
        int numFields2 = other.theNumFields;

        boolean multiKey1 = (theIsPrimary ? false : theIndex.isMultiKey());

        boolean multiKey2 = (other.theIsPrimary ?
                             false :
                             other.theIndex.isMultiKey());

        /* Make sure the index scores are computed */
        getScore();
        other.getScore();

        /*
          String name1 = (theIsPrimary ? "primary" : theIndex.getName());
          String name2 = (other.theIsPrimary ? "primary" : other.theIndex.getName());

          System.out.println(
          "Comparing indexes " + name1 + " and " + name2 +
          "\nscore1 = " + score1 + " score2 = " + score2);
        */

        /*
         * If one of the indexes is covering, ....
         */
        if (theIsCovering != other.theIsCovering) {

            if (theIsCovering) {

                /*
                 * If the other is a preferred index, choose the covering
                 * index if it has at least one eq start/stop condition
                 * or 2 range start/stop conditions.
                 */
                if (!theIsHintIndex && other.theIsHintIndex) {
                    return (theNumEqPredsPushed > 0 ||
                            thePushedPreds.size() > 1 ?
                            -1 : 1);
                }

                /*
                 * If the other index does not have a complete key, choose
                 * the covering index.
                 */
                if (other.theScore != Integer.MAX_VALUE) {
                    return -1;
                }

                /*
                 * The other index has a complete key. Choose the covering
                 * index if its score is >= to the score of the other index
                 * without taking into account the key completeness.
                 */
                return (theScore >= other.theScore2 ? -1 : 1);
            }

            if (other.theIsCovering) {

                if (!other.theIsHintIndex && theIsHintIndex) {
                    return (other.theNumEqPredsPushed > 0 ||
                            other.thePushedPreds.size() > 1 ?
                            1 : -1);
                }

                if (theScore != Integer.MAX_VALUE) {
                    return 1;
                }

                return (other.theScore >= theScore2 ? 1 : -1);
            }
        }

        if (theScore == other.theScore) {

            /*
             * If none of the indexes has any predicates pushed and one of
             * them is the primary index, choose that one.
             */
            if (theScore == 0 && (theIsPrimary || other.theIsPrimary)) {
                return (theIsPrimary ? -1 : 1);
            }

            /*
             * If one of the indexes is specified in a hint, choose that
             * one.
             */
            if (theIsHintIndex != other.theIsHintIndex) {
                return (theIsHintIndex ? -1 : 1);
            }

            /*
             * If one of the indexes is multi-key and other simple, choose
             * the simple one.
             */
            if (multiKey1 != multiKey2) {
                return (multiKey1 ? 1 : -1);
            }

            /*
             * If one of the indexes is the primary index, choose that one.
             */
            if (theIsPrimary || other.theIsPrimary) {
                return (theIsPrimary ? -1 : 1);
            }

            /*
             * Choose the index with the smaller number of fields. This is
             * based on the assumption that if the same number of preds are
             * pushed to both indexes, the more fields the index has the less
             * selective the pushed predicates are going to be.
             */
            if (numFields1 != numFields2) {
                return (numFields1 < numFields2 ? -1 : 1);
            }

            /*
             * TODO ???? Return the one with the smaller key size
             */

            return 0;
        }

        /*
         * If we have a complete key for one of the indexes, choose that
         * one.
         */
        if (theScore == Integer.MAX_VALUE ||
            other.theScore == Integer.MAX_VALUE) {
            return (theScore == Integer.MAX_VALUE ? -1 : 1);
        }

        /*
         * If one of the indexes is specified in a hint, choose that one.
         */
        if (theIsHintIndex != other.theIsHintIndex) {
            return (theIsHintIndex ? -1 : 1);
        }

        return (theScore > other.theScore ? -1 : 1);
    }

    /**
     * Computes the "score" of an index w.r.t. this query, if not done
     * already.
     *
     * Score is a crude estimate of how effective the index is going to
     * be in optimizing table access. Score is only a relative metric,
     * i.e., it doesn't estimate any real metric (e.g. selectivity), but
     * it is meant to be used only in comparing the relative value of two
     * indexes in order to choose the "best" among all applicable indexes.
     *
     * Score is an integer computed as a weighted sum of the predicates
     * that can be pushed into an index scan (as start/stop conditions or
     * filtering preds).  However, if there is a complete key for an index,
     * that index gets the highest score (Integer.MAX_VALUE).
     */
    private int getScore() {

        if (theScore >= 0) {
            return theScore;
        }

        int numIndexFields;

        theScore = 0;
        theScore2 = 0;

        if (theIndex != null) {
            numIndexFields = theIndex.numFields();
        } else {
            numIndexFields = theTable.getNumKeyComponents();
        }

        theScore += theNumEqPredsPushed * eqValue;

        if (theRange != null) {

            if (theRange.getStart() != null) {
                theScore += (theIsMultiKeyRange ? arangeValue : vrangeValue);
            }

            if (theRange.getEnd() != null) {
                theScore += (theIsMultiKeyRange ? arangeValue : vrangeValue);
            }
        }

        for (Expr pred : theFilteringPreds) {
            Function func = pred.getFunction(null);
            if (func != null && func.getCode() == FuncCode.OP_EQ) {
                theScore += filterEqValue;
            } else {
                theScore += filterOtherValue;
            }
        }

        theScore2 = theScore;

        if (theNumEqPredsPushed == numIndexFields) {
            theScore = Integer.MAX_VALUE;
            return theScore;
        }

        return theScore;
    }

    /**
     * The index has been chosen among the applicable indexes, so do the
     * actual pred pushdown and remove all the pushed preds from the
     * where clause.
     */
    void apply() {

        ExprBaseTable tableExpr = theSFW.getTableExpr();

        int numVars = theSFW.getNumVars();

        int[] varRefsCounts = new int[numVars];

        for (int i = 0; i < numVars; ++i) {
            varRefsCounts[i] = theSFW.getVar(i).getNumParents();
        }

        if (theRange != null) {
            assert(thePrimaryKey != null || theSecondaryKey != null);
            tableExpr.addRange(theRange);
            theQCB.setPushedRange(theRange);
        }

        if (theIsPrimary) {
            if (thePrimaryKey == null) {
                thePrimaryKey = theTable.createPrimaryKey();
            }
            tableExpr.addPrimaryKey(thePrimaryKey, theIsCovering);
            theQCB.setPushedPrimaryKey(thePrimaryKey);
        } else {
            if (theSecondaryKey == null) {
                theSecondaryKey = theIndex.createIndexKey();
            }
            tableExpr.addSecondaryKey(theSecondaryKey, theIsCovering);
            theQCB.setPushedSecondaryKey(theSecondaryKey);
        }

        if (theHavePushedExternals) {
            tableExpr.setPushedExternals(thePushedExternals);
        }

        for (Expr pred : thePushedPreds) {
            removePred(pred);
        }

        if (theFilteringPreds.size() > 1) {

            FunctionLib fnlib = CompilerAPI.getFuncLib();
            Function andFunc = fnlib.getFunc(FuncCode.OP_AND);

            Expr pred = ExprFuncCall.create(theQCB, theSctx,
                                            tableExpr.getLocation(),
                                            andFunc,
                                            theFilteringPreds);

            tableExpr.setFilteringPred(pred, false);

            for (Expr pred2 : theFilteringPreds) {
                removePred(pred2);
            }

        } else if (theFilteringPreds.size() == 1) {
            Expr pred = theFilteringPreds.get(0);
            tableExpr.setFilteringPred(pred, false);
            removePred(pred);
        }

        if (theEliminateDups) {
            tableExpr.setEliminateIndexDups();
        }

        /*
         * Remove unused variable. If a var is not used anywhere, it can be
         * removed from the FROM clause if:
         * (a) Its domain expr is scalar, else
         * (b) It was used before applying this index, but is not used after.
         *     This means that all uses of the variable were pushed down to
         *     index. However, if the var ranges over a table, it should not 
         *     be remob=ved.
         */
        for (int i = numVars - 1; i >= 0; --i) {

            ExprVar var = theSFW.getVar(i);
            
            if (var.getNumParents() == 0) {
                
                if(theSFW.getDomainExpr(i).isScalar()) {
                    theSFW.removeFromClause(i, true);
                    
                } else if (varRefsCounts[i] != 0 && var.getTable() == null) {

                    if (theSFW.getDomainExpr(i).isMultiValued() &&
                        (theIndex == null || !theIndex.isMultiKey())) {
                        throw new QueryStateException(
                            "Attempt to remove a multi-valued variable when " +
                            "a non-multikey index is being applied.\n" +
                            "var name = " + var.getName() + " index name = " +
                            theIndex.getName());
                    }

                    theSFW.removeFromClause(i, true);
                }
            }
        }
    }

    /**
     * Do the work!
     *
     * Note: This method will set theSFW to null if it discovers that the whole
     * WHERE expr is always false. If so, it will also replace the whole SFW
     * expr with an empty expr. Callers of this method should check whether
     * theSFW has been set to null.
     */
    void analyze() {
        if (theIsPrimary) {
            analyzePrimaryIndex();
        } else {
            analyzeSecondaryIndex();
        }
    }

    private void analyzePrimaryIndex() {

        /* Collect start/stop and filtering preds for the primary index. */
        boolean done = collectIndexPreds();
        if (done) {
            return;
        }

        /* Create the PrimaryKey into which equality preds will be pushed */
        thePrimaryKey = theTable.createPrimaryKey();

        /*
         * Push start/stop preds onto each primary key column, in the order
         * that these columns appear inside the primary key declaration. Stop
         * as soon as we reach a primary-key column for which there is no
         * equality pred to push.
         */
        List<String> pkColumnNames = theTable.getPrimaryKeyInternal();
        IndexField ipath = null;

        for (int i = 0; i < theNumFields; ++i) {

            String name = pkColumnNames.get(i);

            ipath = new IndexField(theTable, name, i);
            ipath.setTypeDef(theTable.getPrimKeyColumnDef(i));

            done = processIndexColumn(ipath, i);
            if (done) {
                break;
            }
        }

        /* Check if the index is a covering one */
        checkIsCovering();
    }

    private void analyzeSecondaryIndex() {

        /* Collect start/stop and filtering preds for this secondary index. */
        boolean done = collectIndexPreds();
        if (done) {
            return;
        }

        /* Create the IndexKey into which equality preds will be pushed */
        theSecondaryKey = theIndex.createIndexKey();

        /*
         * Push start/stop preds onto each index column, in the order that
         * these columns appear inside the index declaration. Stop as soon
         * as we reach an  column for which there is no equality pred to
         * push, or when we push a pred onto a multi-key index path (we cannot
         * have more than one multi-key pred pushed).
         */
        List<IndexField> indexPaths = theIndex.getIndexFields();
        int i;

        for (i = 0; i < indexPaths.size(); ++i) {

            IndexField ipath = indexPaths.get(i);
            done = processIndexColumn(ipath, i);
            if (done) {
                break;
            }
        }

        if (theIndex.isMultiKey() && theMapBothKey == null) {

            if (!thePushedAnyPred) {
                theEliminateDups = true;

            } else if (i < indexPaths.size() && !theHaveMapKeyEqPred) {
                for (; i < indexPaths.size(); ++i) {
                    IndexField ipath = indexPaths.get(i);
                    if (ipath.isMultiKey()) {
                        theEliminateDups = true;
                    }
                }
            }
        }

        /* Check if the index is a covering one; */
        checkIsCovering();
    }

    /*
     * Check if the index is a covering one.
     */
    private void checkIsCovering() {

        int numIndexPreds = (thePushedPreds.size() +
                             theFilteringPreds.size());
        int numPreds = getNumPreds();

        assert(numIndexPreds <= numPreds);

        /* Any index of key-only table is always covering */
        if (theSFW != null &&
            theTable.getRowDef().getNumFields() ==
            theTable.getPrimKeyDef().getNumFields()) {

            assert(theIsPrimary || !theIndex.isMultiKey());
            assert(numIndexPreds == numPreds);
            assert(!theSFW.hasSort() ||
                   (theSFW.hasPrimaryIndexBasedSort() && theIsPrimary) ||
                   (theSFW.getSortingIndex() == theIndex));

            theIsCovering = true;
            return;
        }

        /* The index must cover all the predicates in the WHERE clause */
        theIsCovering = (theSFW != null && numIndexPreds == numPreds);

        if (!theIsCovering) {
            return;
        }

        /* The index must cover all the exprs in the SELECT clause. */
        int numFields = theSFW.getNumFields();

        for (int i = 0; i < numFields; ++i) {
            Expr expr = theSFW.getFieldExpr(i);
            if (!isIndexOnlyExpr(expr)) {
                theIsCovering = false;
                return;
            }
        }

        /* The index must cover all the exprs in the ORDERBY clause */
        int numSortExprs = theSFW.getNumSortExprs();

        for (int i = 0; i < numSortExprs; ++i) {
            Expr expr = theSFW.getSortExpr(i);
            if (!isIndexOnlyExpr(expr)) {
                theIsCovering = false;
                return;
            }
        }

        int numVars = theSFW.getNumVars();

        for (int i = 0; i < numVars; ++i) {

            ExprVar var = theSFW.getVar(i);
            Expr domExpr = theSFW.getDomainExpr(i);

            if (var.getNumParents() == 0 && !domExpr.isScalar()) {

                if (!isIndexOnlyExpr(domExpr)) {
                    theIsCovering = false;
                    return;
                }
            }
        }
    }

    /**
     * Try to push start/stop preds on the given index path. Return true
     * (meaning no more index paths should be processed) if no equality
     * start/stop pred is pushed. Otherwise, return false.
     */
    private boolean processIndexColumn(IndexField ipath, final int pos) {

        ArrayList<PredInfo> preds = theStartStopPreds.get(pos);

        if (theHaveMapBothPreds && ipath.isMapKey()) {

            FieldValueImpl keyVal;
            keyVal = FieldDefImpl.stringDef.createString(theMapBothKey.theKey);

            theSecondaryKey.put(pos, keyVal);
            thePushedExternals.add(null);
            ++theNumEqPredsPushed;
            theHaveMapKeyEqPred = true;
            return false;
        }

        if (preds == null || preds.isEmpty()) {
            return true;
        }

        assert(preds.size() <= 2);

        PredInfo pi1 = preds.get(0);
        PredInfo pi2 = (preds.size() > 1 ? preds.get(1) : null);

        if (thePushedAnyPred &&
            (pi1.theIsAnyOp || (pi2 != null && pi2.theIsAnyOp))) {
            return true;
        }

        if (pi1.isEq()) {
            assert(preds.size() == 1);

            if (!pi1.isPartial()) {
                thePushedPreds.add(pi1.thePred);
            }
            theFilteringPreds.remove(pi1.thePred);

            if (ipath.isMapKey()) {
                theHaveMapKeyEqPred = true;
            }

            thePushedAnyPred = (thePushedAnyPred || pi1.theIsAnyOp);

            FieldValueImpl constVal;

            if (pi1.theConstVal != null) {
                thePushedExternals.add(null);
                constVal = pi1.theConstVal;
            } else {
                theHavePushedExternals = true;
                thePushedExternals.add(pi1.theConstExpr);
                constVal = createPlaceHolderValue(ipath.getTypeDef());
            }

            if (theIsPrimary) {
                thePrimaryKey.put(ipath.getPathName(), constVal);
            } else {
                theSecondaryKey.put(pos, constVal);
            }
            ++theNumEqPredsPushed;

            return false;
        }

        PredInfo minpi = null;
        PredInfo maxpi = null;

        if (pi1.isMin()) {
            minpi = pi1;
            thePushedAnyPred = (thePushedAnyPred || minpi.theIsAnyOp);

            if (pi2 != null) {
                assert(pi2.isMax());
                maxpi = pi2;
                thePushedAnyPred = (thePushedAnyPred || maxpi.theIsAnyOp);
            }
        } else {
            assert(pi1.isMax());
            maxpi = pi1;
            thePushedAnyPred = (thePushedAnyPred || maxpi.theIsAnyOp);

            if (pi2 != null) {
                assert(pi2.isMin());
                minpi = pi2;
                thePushedAnyPred = (thePushedAnyPred || minpi.theIsAnyOp);
            }
        }

        createRange(ipath, minpi, maxpi);

        return true;
    }

    /**
     *
     */
    private void createRange(IndexField ipath, PredInfo minpi, PredInfo maxpi) {

        int storageSize = (theIsPrimary ?
                           theTable.getPrimaryKeySize(ipath.getPathName()) :
                           0);

        FieldDefImpl rangeDef = ipath.getTypeDef();

        String pathName = (theIsPrimary ? ipath.getPathName() :
                           theIndex.getFieldName(ipath.getPosition()));

        theRange = new FieldRange(pathName,
                                  rangeDef,
                                  storageSize);

        if (minpi != null) {
            if (minpi.theConstVal == null) {
                theHavePushedExternals = true;
                thePushedExternals.add(minpi.theConstExpr);
                FieldValueImpl val = createPlaceHolderValue(rangeDef);
                theRange.setStart(val, minpi.isInclusive(), false);
            } else {
                thePushedExternals.add(null);
                theRange.setStart(minpi.theConstVal, minpi.isInclusive());
            }

            if (minpi.isMultiKey()) {
                theIsMultiKeyRange = true;
                if (!theHaveMapKeyEqPred) {
                    theEliminateDups = true;
                }
            }

            if (!minpi.isPartial()) {
                thePushedPreds.add(minpi.thePred);
            }
            theFilteringPreds.remove(minpi.thePred);

        } else {
            thePushedExternals.add(null);
        }

        if (maxpi != null) {
            if (maxpi.theConstVal == null) {
                theHavePushedExternals = true;
                thePushedExternals.add(maxpi.theConstExpr);
                FieldValueImpl val = createPlaceHolderValue(rangeDef);
                theRange.setEnd(val, maxpi.isInclusive(), false);
            } else {
                thePushedExternals.add(null);
                theRange.setEnd(maxpi.theConstVal, maxpi.isInclusive());
            }

            if (maxpi.isMultiKey()) {
                theIsMultiKeyRange = true;
                if (!theHaveMapKeyEqPred) {
                    theEliminateDups = true;
                }
            }

            if (!maxpi.isPartial()) {
                thePushedPreds.add(maxpi.thePred);
            }
            theFilteringPreds.remove(maxpi.thePred);

        } else {
            thePushedExternals.add(null);
        }
    }

    /**
     * Collect theStartStopPreds and theFilteringPreds. Return true (done)
     * if no preds were collected; otherwise return false.
     */
    private boolean collectIndexPreds() {

        Expr whereExpr = theSFW.getWhereExpr();

        if (whereExpr == null) {
            return true;
        }

        Function andOp = whereExpr.getFunction(FuncCode.OP_AND);

        if (andOp != null) {
            ExprIter children = whereExpr.getChildren();

            while (children.hasNext()) {
                Expr child = children.next();

                PredInfo pi = collectStartStopPred(child);

                if (pi == null) {
                    continue;
                }

                if (pi.theStatus == PredicateStatus.ALWAYS_FALSE) {
                    processAlwaysFalse(pi.thePred);
                    return true;
                }

                if (pi.theStatus == PredicateStatus.ALWAYS_TRUE &&
                    !pi.theDoesFiltering) {
                    children.remove(true);
                }
            }

            if (whereExpr.getNumChildren() == 0) {
                theSFW.removeWhereExpr(true/*destroy*/);
                return true;
            }

            if (theHaveMapBothPreds) {
                chooseMapBothKey();
            }

            /*
             * Go through the preds again to collect any filtering preds that
             * are not start/stop preds. We do this after choosing the best
             * MapBoth key, because only path exprs using the chosen map key
             * can be considered as index-only exprs.
             */
            children.reset();

            while (children.hasNext()) {
                Expr child = children.next();

                if (child.getFilteringPredFlag()) {
                    continue;
                }

                if (isIndexOnlyExpr(child)) {
                    theFilteringPreds.add(child);
                }
            }

        } else {
            PredInfo pi = collectStartStopPred(whereExpr);

            if (pi == null) {
                if (isIndexOnlyExpr(whereExpr)) {
                    theFilteringPreds.add(whereExpr);
                    return false;
                }
                return true;
            }

            if (pi.theStatus == PredicateStatus.ALWAYS_FALSE) {
                processAlwaysFalse(pi.thePred);
                return true;
            }

            if (pi.theStatus == PredicateStatus.ALWAYS_TRUE) {
                if (!pi.theDoesFiltering) {
                    removePred(whereExpr);
                }
                return true;
            }

            if (theHaveMapBothPreds) {
                chooseMapBothKey();
            }
        }

        /*
         * See if we collected any start/stop preds, and remove any ALWAYS_TRUE
         * preds.
         */
        int numPreds = 0;

        for (int i = 0; i < theNumFields; ++i) {

            ArrayList<PredInfo> preds = theStartStopPreds.get(i);

            if (preds == null) {
                continue;
            }

            for (int j = 0; j < preds.size(); ++j) {

                PredInfo pi = preds.get(j);

                if (pi.theStatus == PredicateStatus.ALWAYS_TRUE) {
                    preds.remove(j);
                    --j;
                    theFilteringPreds.remove(pi.thePred);
                    if (!pi.theDoesFiltering) {
                        removePred(pi.thePred);
                    }
                    continue;
                }

                ++numPreds;
            }
        }

        if (numPreds == 0 && theFilteringPreds.isEmpty()) {
            return true;
        }

        return false;
    }

    /**
     * Check if the given pred is a start/stop pred for a path of theIndex.
     * If not, return null. Otherwise, build a PredInfo for it, and check
     * the pred against any other start/stop preds on the same index path.
     * Return the PredInfo, which includes the result of this check.
     */
    private PredInfo collectStartStopPred(Expr pred) {

        pred.clearFilteringPredFlag();

        /*
         * Check whether the pred is a comparison pred and the comp op is not
         * !=. If it is an any-comp, the index must be multikey one.
         */
        Function func = pred.getFunction(null);
        FuncCode op = (func != null ? func.getCode() : null);

        if (func == null ||
            !func.isComparison() ||
            op == FuncCode.OP_NEQ ||
            op == FuncCode.OP_NEQ_ANY) {
            return null;
        }

        if (func.isAnyComparison()) {
            if (theIsPrimary || !theIndex.isMultiKey()) {
                return null;
            }
            op = FuncAnyOp.anyToComp(op);
        }

        ExprFuncCall compExpr = (ExprFuncCall)pred;

        /*
         * Check whether one of the operands is a constant expr returning
         * exactly one item of the same type as the non-const operand.
         */
        Expr arg0 = compExpr.getArg(0);
        Expr arg1 = compExpr.getArg(1);
        Expr constArg = null;
        FieldValueImpl constVal = null;
        Expr varArg = null;

        if (arg0.isConstant()) {
            constArg = arg0;
            varArg = arg1;
            op = FuncCompOp.swapCompOp(op);
        } else if (arg1.isConstant()) {
            constArg = arg1;
            varArg = arg0;
        } else {
            return null;
        }

        if (!checkTypes(varArg, constArg)) {
            return null;
        }

        if (constArg.getKind() == ExprKind.CONST) {
            constVal = ((ExprConst)constArg).getValue();
        }

        /*
         * Check whether the non-constant operand is a path expr that
         * matches one of the index paths.
         */
        matchExprToIndexPath(varArg);

        if (!thePath.matched()) {
            return null;
        }

        /*
         * P is a potential start/stop pred for an index path IP. Check
         * it against other preds (if any) on IP, and if "compatible" with
         * these preds, register it.
         */
        int ipathPos = thePath.thePosition;
        boolean mapBothIndex = (theIndex != null && theIndex.isMapBothIndex());
        String mapKey = thePath.theMapBothKey;
        boolean isMultikeyPath = (theIndex != null &&
                                  theIndex.getIndexPath(ipathPos).isMultiKey());

        ArrayList<PredInfo> preds = theStartStopPreds.get(ipathPos);

        if (preds == null) {
            preds = new ArrayList<PredInfo>();
            theStartStopPreds.set(ipathPos, preds);
        }

        PredInfo pi = new PredInfo(pred);
        pi.theCompOp = op;
        pi.theIsAnyOp = func.isAnyComparison();
        pi.theConstExpr = constArg;
        pi.theConstVal = constVal;
        pi.theIPathPos = thePath.thePosition;
        pi.theMapKey = mapKey;
        pi.theStatus = PredicateStatus.KEEP;
        pi.theIsDirect = thePath.theIsDirect;

        if (mapKey != null && mapBothIndex) {
            theHaveMapBothPreds = true;
            MapBothKeyInfo mki = theMapBothKeys.get(mapKey);
            if (mki == null) {
                mki = new MapBothKeyInfo(mapKey);
                theMapBothKeys.put(mapKey, mki);
            }

        } else if (theIndex != null && theIndex.isMultiKey()) {

            pi.theDoesFiltering = thePath.theDoesFiltering;
            pi.theIsUnnested = thePath.theIsUnnested;

            if (thePath.theIsMultiKey &&
                !thePath.theIsUnnested &&
                func.isValueComparison() &&
                varArg.isMultiValued()) {
                /* 
                 * Set theDoesFilterin flag, so that the pred will be pushed
                 * as a partial one
                 */
                pi.theDoesFiltering = true;   
            }
        }

        for (int i = 0; i < preds.size(); ++i) {

            PredInfo opi = preds.get(i);

            if (mapKey != null) {
                if (opi.theMapKey == null) {
                    preds.remove(i);
                    --i;
                    continue;
                } else if (!mapKey.equals(opi.theMapKey)) {
                    continue;
                }
            } else if (opi.theMapKey != null) {
                pi.theStatus = PredicateStatus.SKIP;
                return pi;
            }

            if (pi.isEq()) {

                if (opi.isEq()) {
                    checkEqEq(pi, opi, isMultikeyPath);
                } else if (opi.isMin()) {
                    checkEqMin(pi, opi, isMultikeyPath);
                } else {
                    assert(opi.isMax());
                    checkEqMax(pi, opi, isMultikeyPath);
                }

            } else if (pi.isMin()) {

                if (opi.isEq()) {
                    checkEqMin(opi, pi, isMultikeyPath);
                } else if (opi.isMin()) {
                    checkMinMin(pi, opi, isMultikeyPath);
                } else {
                    assert(opi.isMax());
                    checkMinMax(pi, opi, pi, isMultikeyPath);
                }

            } else {
                assert(pi.isMax());

                if (opi.isEq()) {
                    checkEqMax(opi, pi, isMultikeyPath);
                } else if (opi.isMin()) {
                    checkMinMax(opi, pi, pi, isMultikeyPath);
                } else {
                    assert(opi.isMax());
                    checkMaxMax(pi, opi, isMultikeyPath);
                }
            }

            if (pi.theStatus == PredicateStatus.ALWAYS_TRUE ||
                pi.theStatus == PredicateStatus.ALWAYS_FALSE) {
                /*
                 * Caller will handle these two cases. We cannot handle them
                 * here because the caller may have an open iterator of the
                 * operands of the root AND operator, and removing this pred
                 * will invalidate the iterator. The case where the other pred
                 * (opi) is always true is also handled by the caller, after
                 * all the preds have been processed.
                 */
                return pi;
            } else if (pi.theStatus == PredicateStatus.SKIP) {
                /*
                 * Simply don't include the current pred in theStartStopPreds
                 */
                break;
            } else if (opi.theStatus == PredicateStatus.SKIP) {
                /*
                 * Remove the other pred from theStartStopPreds
                 */
                preds.remove(i);
                --i;
                continue;
            }
        }

        if (pi.theStatus == PredicateStatus.KEEP) {
            preds.add(pi);
        }

        /* TODO: Allow filtering preds that cross variables */
        if (!pi.isMultiKey() && pi.theIsDirect) {
            theFilteringPreds.add(pred);
            pred.setFilteringPredFlag();
        }

        return pi;
    }

    private boolean checkTypes(Expr varArg, Expr constArg) {

        if (!constArg.isScalar()) {
            return false;
        }

        FieldDefImpl varType = varArg.getType().getDef();
        FieldDefImpl constType = constArg.getType().getDef();
        Type varTypeCode = varType.getType();
        Type constTypeCode = constType.getType();

        switch (varTypeCode) {
        case INTEGER:
            return (constTypeCode == Type.INTEGER ||
                    constTypeCode == Type.LONG);
        case LONG:
            return (constTypeCode == Type.LONG ||
                    constTypeCode == Type.INTEGER);
        case FLOAT:
            return (constTypeCode == Type.FLOAT ||
                    constTypeCode == Type.DOUBLE ||
                    constTypeCode == Type.INTEGER ||
                    constTypeCode == Type.LONG);

        case DOUBLE:
            return (constTypeCode == Type.DOUBLE ||
                    constTypeCode == Type.FLOAT ||
                    constTypeCode == Type.INTEGER ||
                    constTypeCode == Type.LONG);
        case ENUM:
            return (constTypeCode == Type.STRING ||
                    constTypeCode == Type.ENUM);
        case STRING:
        case BOOLEAN:
            return varTypeCode == constTypeCode;
        case TIMESTAMP:
            return (varTypeCode == constTypeCode &&
                    ((TimestampDefImpl)varType).getPrecision() ==
                    ((TimestampDefImpl)constType).getPrecision());
        default:
            return false;
        }
    }

    private void checkEqEq(PredInfo p1, PredInfo p2, boolean multikey) {

        if (p1.theConstVal != null && p1.theConstVal != null) {

            int cmp = p1.theConstVal.compareTo(p2.theConstVal);

            if (cmp == 0) {
                if (p1.theIsUnnested != p2.theIsUnnested) {
                    if (p1.theIsUnnested) {
                        p2.theStatus = PredicateStatus.ALWAYS_TRUE;
                    } else {
                        p1.theStatus = PredicateStatus.ALWAYS_TRUE;
                    }
                } else {
                    p1.theStatus = PredicateStatus.ALWAYS_TRUE;
                }
            } else if (multikey && !(p1.theIsUnnested && p2.theIsUnnested)) {
                p1.theStatus = PredicateStatus.SKIP;
            } else {
                p1.theStatus = PredicateStatus.ALWAYS_FALSE;
                p2.theStatus = PredicateStatus.ALWAYS_FALSE;
            }

        } else if (p1.theIsUnnested != p2.theIsUnnested) {
            if (p1.theIsUnnested) {
                p2.theStatus = PredicateStatus.SKIP;
            } else {
                p1.theStatus = PredicateStatus.SKIP;
            }

        } else if (p1.theConstVal != null) {
            p2.theStatus = PredicateStatus.SKIP;

        } else {
            p1.theStatus = PredicateStatus.SKIP;
        }
    }

    private void checkEqMin(PredInfo p1, PredInfo p2, boolean multikey) {

        if (p1.theConstVal != null && p2.theConstVal != null) {

            int cmp = p1.theConstVal.compareTo(p2.theConstVal);

            if (cmp < 0 || (cmp == 0 && !p2.isInclusive())) {
                if (multikey && !(p1.theIsUnnested && p2.theIsUnnested)) {
                    p2.theStatus = PredicateStatus.SKIP;
                } else {
                    p1.theStatus = PredicateStatus.ALWAYS_FALSE;
                    p2.theStatus = PredicateStatus.ALWAYS_FALSE;
                }
            } else if (p2.theIsUnnested && !p1.theIsUnnested) {
                p2.theStatus = PredicateStatus.SKIP;
            } else {
                p2.theStatus = PredicateStatus.ALWAYS_TRUE;
            }

        } else {
            p2.theStatus = PredicateStatus.SKIP;
        }
    }

    private void checkEqMax(PredInfo p1, PredInfo p2, boolean multikey) {

        if (p1.theConstVal != null && p2.theConstVal != null) {

            int cmp = p1.theConstVal.compareTo(p2.theConstVal);

            if (cmp > 0 || (cmp == 0 && !p2.isInclusive())) {
                if (multikey && !(p1.theIsUnnested && p2.theIsUnnested)) {
                    p2.theStatus = PredicateStatus.SKIP;
                } else {
                    p1.theStatus = PredicateStatus.ALWAYS_FALSE;
                    p2.theStatus = PredicateStatus.ALWAYS_FALSE;
                }
            } else if (p2.theIsUnnested && !p1.theIsUnnested) {
                p2.theStatus = PredicateStatus.SKIP;
            } else {
                p2.theStatus = PredicateStatus.ALWAYS_TRUE;
            }

        } else {
            p2.theStatus = PredicateStatus.SKIP;
        }
    }

    private void checkMinMin(PredInfo p1, PredInfo p2, boolean multikey) {

        if (p1.theConstVal != null && p2.theConstVal != null) {

            int cmp = p1.theConstVal.compareTo(p2.theConstVal);

            if (cmp < 0 || (cmp == 0 && p1.isInclusive())) {
                if (p1.theIsUnnested == p2.theIsUnnested || p2.theIsUnnested) {
                    p1.theStatus = PredicateStatus.ALWAYS_TRUE;
                } else {
                    p1.theStatus = PredicateStatus.SKIP;
                }
            } else {
                if (p1.theIsUnnested == p2.theIsUnnested || p1.theIsUnnested) {
                    p2.theStatus = PredicateStatus.ALWAYS_TRUE;
                } else {
                    p2.theStatus = PredicateStatus.SKIP;
                }
            }

        } else if (multikey) {

            if (p1.theIsUnnested != p2.theIsUnnested) {
                if (p1.theIsUnnested) {
                    p2.theStatus = PredicateStatus.SKIP;
                } else {
                    p1.theStatus = PredicateStatus.SKIP;
                }
            } else if (p1.theDoesFiltering != p2.theDoesFiltering) {
                if (p1.theDoesFiltering) {
                    p1.theStatus = PredicateStatus.SKIP;
                } else {
                    p2.theStatus = PredicateStatus.SKIP;
                }
            } else if (p1.theConstVal != null) {
                p2.theStatus = PredicateStatus.SKIP;
            } else {
                p1.theStatus = PredicateStatus.SKIP;
            }

        } else if (p1.theConstVal != null) {
            p2.theStatus = PredicateStatus.SKIP;

        } else {
            p1.theStatus = PredicateStatus.SKIP;
        }
    }

    private void checkMaxMax(PredInfo p1, PredInfo p2, boolean multikey) {

        if (p1.theConstVal != null && p2.theConstVal != null) {

            int cmp = p1.theConstVal.compareTo(p2.theConstVal);

            if (cmp < 0 || (cmp == 0 && p2.isInclusive())) {
                if (p1.theIsUnnested == p2.theIsUnnested || p1.theIsUnnested) {
                    p2.theStatus = PredicateStatus.ALWAYS_TRUE;
                } else {
                    p2.theStatus = PredicateStatus.SKIP;
                }
            } else {
                if (p1.theIsUnnested == p2.theIsUnnested || p2.theIsUnnested) {
                    p1.theStatus = PredicateStatus.ALWAYS_TRUE;
                } else {
                    p1.theStatus = PredicateStatus.SKIP;
                }
            }

        } else if (multikey) {

            if (p1.theIsUnnested != p2.theIsUnnested) {
                if (p1.theIsUnnested) {
                    p2.theStatus = PredicateStatus.SKIP;
                } else {
                    p1.theStatus = PredicateStatus.SKIP;
                }
            } else if (p1.theDoesFiltering != p2.theDoesFiltering) {
                if (p1.theDoesFiltering) {
                    p1.theStatus = PredicateStatus.SKIP;
                } else {
                    p2.theStatus = PredicateStatus.SKIP;
                }
            } else if (p1.theConstVal != null) {
                p2.theStatus = PredicateStatus.SKIP;
            } else {
                p1.theStatus = PredicateStatus.SKIP;
            }

        } else if (p1.theConstVal != null) {
            p2.theStatus = PredicateStatus.SKIP;

        } else {
            p1.theStatus = PredicateStatus.SKIP;
        }
    }

    private void checkMinMax(
        PredInfo p1,
        PredInfo p2,
        PredInfo current,
        boolean multikey) {

        if (multikey) {

            if (!p1.theIsUnnested && !p2.theIsUnnested) {
                if (current == p1) {
                    p1.theStatus = PredicateStatus.SKIP;
                } else {
                    p2.theStatus = PredicateStatus.SKIP;
                }
                return;
            }

            if (!p1.theIsUnnested) {
                p1.theStatus = PredicateStatus.SKIP;
                return;
            }

            if (!p2.theIsUnnested) {
                p2.theStatus = PredicateStatus.SKIP;
                return;
            }
        }

        if (p1.theConstVal != null && p2.theConstVal != null) {

            int cmp = p1.theConstVal.compareTo(p2.theConstVal);

            if (cmp > 0 ||
                (cmp == 0 && (!p2.isInclusive() || !p1.isInclusive()))) {
                p1.theStatus = PredicateStatus.ALWAYS_FALSE;
                p2.theStatus = PredicateStatus.ALWAYS_FALSE;

            } else if (cmp == 0) {
                p1.theCompOp = FuncCode.OP_EQ;
                p2.theStatus = PredicateStatus.ALWAYS_TRUE;
            }
        }
    }

    /**
     *
     */
    private static class MapBothKeyInfo {

        String theKey;
        int theScore;

        MapBothKeyInfo(String key) {
            theKey = key;
        }
    }

    /**
     *
     */
    private void chooseMapBothKey() {

        if (!theHaveMapBothPreds) {
            return;
        }

        /*
         * For each map key, calculate its score.
         */
        for (MapBothKeyInfo mki : theMapBothKeys.values()) {

            String mapKey = mki.theKey;
            boolean filteringOnly = false;
            boolean eqPredPushed = false;

            for (int i = 0; i < theNumFields; ++i) {

                ArrayList<PredInfo> preds = theStartStopPreds.get(i);

                if (preds == null) {
                    continue;
                }

                if (!theIndex.getIndexPath(i).isMapValue()) {
                    continue; // not a MapValue index path
                }

                /*
                 * Compute the score of the current mapKey, based on the preds
                 * (if any) that are pushable to the current MapValue path for
                 * the current map key
                 */
                for (PredInfo pi : preds) {

                    if (!mapKey.equals(pi.theMapKey)) {
                        continue;
                    }

                    if (filteringOnly) {
                        if (pi.isEq()) {
                            mki.theScore += filterEqValue;
                        } else {
                            mki.theScore += filterOtherValue;
                        }
                    } else {
                        if (pi.isEq()) {
                            mki.theScore += eqValue;
                            eqPredPushed = true;
                        } else {
                            mki.theScore += vrangeValue;
                        }
                    }
                }

                if (!eqPredPushed) {
                    filteringOnly = true;
                }
            }
        }

        /*
         * Now choose the "best" map key
         */
        for (MapBothKeyInfo mki : theMapBothKeys.values()) {
            if (theMapBothKey == null) {
                theMapBothKey = mki;
            } else if (mki.theScore > theMapBothKey.theScore) {
                theMapBothKey = mki;
            }
        }

        /*
         * Throw away all MapBoth preds that are not on the "best" map key.
         */
        for (int i = 0; i < theNumFields; ++i) {

            ArrayList<PredInfo> preds = theStartStopPreds.get(i);

            if (preds == null) {
                continue;
            }

            for (int j = 0; j < preds.size(); ++j) {

                PredInfo pi = preds.get(j);

                if (!pi.isMapBoth()) {
                    continue;
                }

                if (!pi.theMapKey.equals(theMapBothKey.theKey)) {
                    preds.remove(j);
                    theFilteringPreds.remove(pi.thePred);
                    --j;
                }
            }
        }

        /*
          System.out.println(
          "Best MapBoth key : " + theMapBothKey.theKey +
          " theNumEqPreds = " + theMapBothKey.theNumEqPreds +
          " theNumRangePreds = " + theMapBothKey.theNumRangePreds);
        */
    }

    /**
     * This method attempts to match a query expression P with one of the
     * index paths of a given index. If such a match is established, it
     * fills up the info in thePath (an IndexPathExpr instance).
     */
    private void matchExprToIndexPath(Expr expr) {

        thePath.clear();

        boolean multiKeyIndex = false;
        boolean multiKeyMapIndex = false;

        if (theIndex != null) {
            if (theIndex.isMultiKeyMapIndex()) {
                multiKeyIndex = true;
                multiKeyMapIndex = true;
            } else if (theIndex.isMultiKey()) {
                multiKeyIndex = true;
            }
        }

        while (true) {

            switch (expr.getKind()) {

            case FIELD_STEP: {
                ExprFieldStep step = (ExprFieldStep)expr;
                String fieldName = step.getFieldName();

                if (fieldName == null) {
                    return;
                }

                thePath.add(fieldName);

                if (step.getInput().getType().isArray()) {

                    if (!multiKeyIndex || thePath.theIsMultiKey) {
                        return;
                    }

                    thePath.add(TableImpl.BRACKETS);
                    thePath.theIsMultiKey = true;
                }

                expr = expr.getInput();
                break;
            }
            case MAP_FILTER: {
                if (!expr.getInput().getType().isMap()) {
                    return;
                }

                if (!multiKeyMapIndex || thePath.theIsMultiKey) {
                    return;
                }

                ExprMapFilter step = (ExprMapFilter)expr;

                if (step.getFilterKind() == FilterKind.KEYS) {
                    thePath.add(TableImpl.KEY_TAG);
                } else {
                    thePath.setIsMapValue();
                    thePath.add(TableImpl.BRACKETS);
                }
                
                thePath.theIsMultiKey = true;
                thePath.theDoesFiltering = (step.getPredExpr() != null);

                expr = expr.getInput();
                break;
            }
            case ARRAY_SLICE:
            case ARRAY_FILTER: {
                if (!multiKeyIndex || thePath.theIsMultiKey) {
                    return;
                }

                if (!expr.getInput().getType().isArray()) {
                    return;
                }

                thePath.add(TableImpl.BRACKETS);
                thePath.theIsMultiKey = true;

                if (expr.getKind() == ExprKind.ARRAY_SLICE) {
                    ExprArraySlice step = (ExprArraySlice)expr;
                    thePath.theDoesFiltering = step.hasBounds();
                } else {
                    ExprArrayFilter step = (ExprArrayFilter)expr;
                    thePath.theDoesFiltering = (step.getPredExpr() != null);
                }

                expr = expr.getInput();
                break;
            }
            case VAR: {
                ExprVar varExpr = (ExprVar)expr;

                if (varExpr.isExternal() || varExpr.isContext()) {
                    return;
                }

                expr = varExpr.getDomainExpr();

                if (expr.getKind() != ExprKind.BASE_TABLE) {
                    thePath.theIsDirect = false;
                    if (expr.isMultiValued()) {
                        thePath.theIsUnnested = true;
                    }
                }

                break;
            }
            case BASE_TABLE: {
                ExprBaseTable tableExpr = (ExprBaseTable)expr;
                TableImpl table = tableExpr.getTable();

                if (table != theTable) {
                    return;
                }

                thePath.reverseSteps();

                matchPathExprToIndexPath(theTable, theIndex, thePath);
                return;
            }
            default:
                return;
            }
        }
    }

    /**
     * This method attempts to match a query path expr QP with one of
     * the index paths of a given index. If such a match is established, it
     * returns true; otherwise it returns false.
     *
     * Note: the index may be the primary index, in which case the "index"
     * param will be null.
     *
     * The definition of a "match" between a query path expr and an index
     * path is given in the header javadoc of the PredInfo class.
     */
    static boolean matchPathExprToIndexPath(
        TableImpl table,
        IndexImpl index,
        IndexPathExpr epath) {

        if (index == null) {
            if (epath.isComplex()) {
                return false;
            }
            int ipathPos = table.findKeyComponent(epath.getLastStep());
            if (ipathPos >= 0) {
                epath.thePosition = ipathPos;
                return true;
            }
            return false;
        }

        int numFields = index.numFields();
        int ipathPos;
        String mapKey = null;
        boolean mapBothIndex = index.isMapBothIndex();

        for (ipathPos = 0; ipathPos < numFields; ++ipathPos) {

            IndexField ipath = index.getIndexPath(ipathPos);

            if (ipath.numSteps() != epath.numSteps()) {
                continue;
            }

            if (mapBothIndex &&
                ipath.isMapValue() &&
                !epath.theIsMultiKey) {
                /*
                 * We have a map-value index path and a simple query
                 * path expr. Try to match them as a MapBoth expr.
                 */
                for (int i = 0; i < ipath.numSteps(); ++i) {

                    String step = ipath.getStep(i);

                    if (step.equals(epath.getStep(i))) {
                        continue;
                    }

                    if (step.equals(TableImpl.BRACKETS)) {
                        mapKey = epath.getStep(i);
                        continue;
                    }

                    mapKey = null;
                    break;
                }

                if (mapKey != null) {
                    /* P is a MapBoth pred. */
                    epath.theMapBothKey = mapKey;
                    epath.thePosition = ipathPos;
                    return true;
                }

            } else if (ipath.equals(epath)) {
                epath.thePosition = ipathPos;
                return true;
            }
        }

        return false;
    }

    /**
     * This method checks whether the given expr is a expr that can be evaluated
     * using the columns of the current index only (which may be the primary
     * index, if theIndex is null).
     */
    private boolean isIndexOnlyExpr(Expr expr) {

        if (expr.isStepExpr()) {

            thePath.clear();

            do {
                if (expr.getKind() == ExprKind.FIELD_STEP) {

                    ExprFieldStep stepExpr = (ExprFieldStep)expr;
                    String fieldName = stepExpr.getFieldName();
                    Expr fieldNameExpr = stepExpr.getFieldNameExpr();
                    Expr inputExpr = stepExpr.getInput();

                    if (fieldName == null) {
                        if (!isIndexOnlyExpr(fieldNameExpr)) {
                            return false;
                        }
                        if (!isIndexOnlyExpr(inputExpr)) {
                            return false;
                        }
                        return true;
                    }

                    thePath.add(fieldName);

                    if (inputExpr.getType().isArray()) {
                        return false;
                    }

                } else {
                    return false;
                }

                expr = expr.getInput();

                if (expr.getKind() == ExprKind.VAR) {

                    ExprVar varExpr = (ExprVar)expr;

                    if (varExpr.isExternal() || varExpr.isContext()) {
                        return true;
                    }

                    expr = varExpr.getDomainExpr();

                    /*
                     * TODO : allow filtering preds in this case
                     */
                    if (expr.getKind() != ExprKind.BASE_TABLE) {
                        return false;
                    }
                }

            } while (expr.isStepExpr());

            if (expr.getKind() == ExprKind.BASE_TABLE) {

                ExprBaseTable tableExpr = (ExprBaseTable)expr;
                TableImpl table = tableExpr.getTable();

                if (table != theTable) {
                    return false;
                }

                if (!thePath.isComplex() &&
                    theTable.isKeyComponent(thePath.getLastStep())) {

                    return true;

                } else if (!theIsPrimary) {

                    thePath.reverseSteps();

                    if (!matchPathExprToIndexPath(theTable,
                                                  theIndex,
                                                  thePath)) {
                        return false;
                    }

                    if (theHaveMapBothPreds &&
                        (theMapBothKey == null ||
                         !theMapBothKey.theKey.equals(thePath.theMapBothKey))) {
                        return false;
                    }

                    return true;
                }

                return false;

            }

            return isIndexOnlyExpr(expr);

        } else if (expr.getKind() == ExprKind.VAR) {

            ExprVar var = (ExprVar)expr;

            if (var.isExternal() || var.isContext()) {
                return true;
            }

            /*
             * It must be a table var that is accessed "directly", i.e.
             * not via a path expr.
             */
            return false;

        } else {
            ExprIter children = expr.getChildren();

            while (children.hasNext()) {
                Expr child = children.next();
                if (!isIndexOnlyExpr(child)) {
                    children.reset();
                    return false;
                }
            }

            return true;
        }
    }


    private static FieldValueImpl createPlaceHolderValue(FieldDefImpl type) {

        switch (type.getType()) {
        case INTEGER:
            return FieldDefImpl.integerDef.createInteger(0);
        case LONG:
            return FieldDefImpl.longDef.createLong(0);
        case FLOAT:
            return FieldDefImpl.floatDef.createFloat(0.0F);
        case DOUBLE:
            return FieldDefImpl.doubleDef.createDouble(0.0);
        case STRING:
            return FieldDefImpl.stringDef.createString("");
        case ENUM:
            return ((EnumDefImpl)type).createEnum(1);
        default:
            throw new QueryStateException(
                "Unexpected type for index key: " + type);
        }
    }

    /**
     * Return the number of preds in the WHERE clause of the SFW expr
     */
    private int getNumPreds() {

        if (theSFW == null) {
            return 0;
        }

        Expr whereExpr = theSFW.getWhereExpr();

        if (whereExpr == null) {
            return 0;
        }

        Function andOp = whereExpr.getFunction(FuncCode.OP_AND);

        if (andOp != null) {
            return whereExpr.getNumChildren();
        }
        return 1;
    }
}
