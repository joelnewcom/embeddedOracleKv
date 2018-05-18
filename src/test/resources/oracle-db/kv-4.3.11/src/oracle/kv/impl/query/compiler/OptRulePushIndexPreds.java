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
import java.util.Collections;
import java.util.Map;

import oracle.kv.impl.api.table.IndexImpl;
import oracle.kv.impl.api.table.TableImpl;
import oracle.kv.impl.query.QueryException;
import oracle.kv.impl.query.compiler.ExprBaseTable.IndexHint;
import oracle.kv.table.Index;

/**
 * The goal of this optimization rule is convert WHERE predicates into index
 * scan conditions in order to avoing a full table scan.
 *
 * The rule analyses the predicates in a WHERE clause to find, for each index
 * associated with a given table (including the table's primary index), (a) a
 * starting and/or ending key that could be applied to a scan over that index,
 * and (b) predicates that can be evaluated during the index scan from the
 * index columns only, thus filtering the retrieved index keys further.
 *
 * The rule assumes the WHERE-clause expr is in CNF. For each index, it first
 * collects all CNF factors that are "index predicates", i.e., they can be
 * evaluated fully from the index columns only. For example, if the current
 * index is the primary-key index and C1, C2, C3 are the primary-key columns,
 * "C1 > 10" and "C2 = 3 or C3 < 20" are primary index preds. Then, for each
 * index column, in the order that these columns appear in the index (or primary
 * key) declaration, the rule looks-for and processes index preds that are
 * comparison preds (eg, "C1 > 10" is a comparison pred, but "C2 = 3 or C3 < 20"
 * is not). The possible outomes of processing an index pred w.r.t. an index
 * column are listed in the PredicateStatus enum below. The rule stops
 * processing the current index as soon as it finds an index column for which
 * there is no equality pred to be pushed to the index.
 *
 * After the rule has analyzed all indexes, it chooses the "best" index to
 * use among the indexes that had something pushed down to them.
 *
 * TODO: need a "good" heuristic to choose the "best" index, as well as
 * a compiler hint or USE INDEX clause to let the user decide.
 */
class OptRulePushIndexPreds {

    // TODO move this to the Optimizer obj, when we have one
    private RuntimeException theException = null;

    private ArrayList<IndexAnalyzer> theAnalyzers;

    RuntimeException getException() {
        return theException;
    }

    void apply(ExprSFW sfw) {

        try {
            Expr whereExpr = sfw.getWhereExpr();

            if (whereExpr == null && !sfw.hasSort()) {
                return;
            }

            TableImpl table = sfw.getTable();

            if (table == null) {
                return;
            }

            ExprBaseTable tableExpr = sfw.getTableExpr();

            IndexHint forceIndexHint = tableExpr.getForceIndexHint();

            Map<String, Index> indexes = table.getIndexes();

            theAnalyzers = new ArrayList<IndexAnalyzer>(1+indexes.size());

            /*
             * Try to push predicates in the primary index
             */
            theAnalyzers.add(new IndexAnalyzer(sfw, table, null/*index*/));
            IndexAnalyzer primaryAnalyzer = theAnalyzers.get(0);
            primaryAnalyzer.analyze();

            boolean completePrimaryKey =
                (primaryAnalyzer.thePrimaryKey != null &&
                 primaryAnalyzer.thePrimaryKey.isComplete());

            /* No reason to continue if the WHERE expr is always false */
            if (primaryAnalyzer.theSFW == null) {
                return;
            }

            if (forceIndexHint != null) {

                if (completePrimaryKey) {
                    sfw.removeSort();
                }

                if (sfw.hasSort() &&
                    sfw.getSortingIndex() != forceIndexHint.theIndex) {

                    String hintIndex = (forceIndexHint.theIndex == null ?
                                        "primary" :
                                        forceIndexHint.theIndex.getName());

                    String sortingIndex = (sfw.getSortingIndex() == null ?
                                           "primary" :
                                           sfw.getSortingIndex().getName());

                    throw new QueryException(
                        "Cannot perform order-by because the sorting index " +
                        "is not the same as the one forced via a hint.\n" +
                        "Hint index    : " + hintIndex + "\n" +
                        "Sorting index : " + sortingIndex, sfw.getLocation());
                }

                IndexAnalyzer analyzer =
                    new IndexAnalyzer(sfw, table, forceIndexHint.theIndex);

                analyzer.analyze();

                if (analyzer.theSFW == null) {
                    return;
                }

                analyzer.apply();
                return;
            }

            /*
             * If the query specifies a complete primary key, use the primary
             * index to execute it and remove any order-by.
             */
            if (completePrimaryKey) {
                primaryAnalyzer.apply();
                sfw.removeSort();
                return;
            }

            /*
             * If the query specifies a complete shard key, use the primary
             * index to execute it. In this case, if the query has order-by
             * as well and the sorting index is not the primary one, an error
             * is raised.
             */
            if (primaryAnalyzer.hasShardKey()) {

                if (sfw.hasSort() && sfw.getSortingIndex() != null) {
                    throw new QueryException(
                        "Cannot perform order-by because the query specifies " +
                        "a complete shard key, but the sorting index (" +
                        sfw.getSortingIndex().getName() +
                        ") is a secondary one.", sfw.getLocation());
                }

                primaryAnalyzer.apply();
                return;
            }

            /*
             * If the SFW has sorting, scan the table using the index that
             * sorts the rows in the desired order.
             */
            if (sfw.hasSort()) {

                if (sfw.hasPrimaryIndexBasedSort()) {
                    primaryAnalyzer.apply();
                    return;
                }

                if (sfw.hasSecondaryIndexBasedSort()) {

                    IndexImpl index = sfw.getSortingIndex();

                    IndexAnalyzer analyzer =
                        new IndexAnalyzer(sfw, table, index);

                    analyzer.analyze();

                    if (analyzer.theSFW != null) {
                        analyzer.apply();
                    }

                    return;
                }
            }

            /*
             * Check which of the secondary indexes are applicable for
             * optimizing this query. An index is applicable if the query
             * has any predicates that can be "pushed" to the index.
             */
            boolean alwaysFalse = false;
            for (Map.Entry<String, Index> entry : indexes.entrySet()) {

                IndexImpl index = (IndexImpl)entry.getValue();
                IndexAnalyzer analyzer = new IndexAnalyzer(sfw, table, index);
                theAnalyzers.add(analyzer);

                analyzer.analyze();

                if (analyzer.theSFW == null) {
                    alwaysFalse = true;
                    break;
                }
            }

            /*
             * Choose the "best" of the applicable indexes.
             */
            if (!alwaysFalse) {
                chooseIndex();
            }

        } catch (RuntimeException e) {
            theException = e;
        }
    }

    /**
     * Choose and apply the "best" index among the applicable ones.
     */
    void chooseIndex() {
        IndexAnalyzer bestIndex = Collections.min(theAnalyzers);
        bestIndex.apply();
    }

}
