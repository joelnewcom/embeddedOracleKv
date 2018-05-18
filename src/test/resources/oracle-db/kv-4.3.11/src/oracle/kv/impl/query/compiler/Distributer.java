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

import oracle.kv.impl.api.query.PreparedStatementImpl.DistributionKind;
import oracle.kv.impl.query.compiler.Expr.ExprKind;
import oracle.kv.impl.query.compiler.ExprSFW.FromClause;

/**
 * Distributer is responsible for creating ExprReceive exprs in the exprs graph
 * and placing them at their appropriate position within the graph.
 *
 * It implements an algorithm which traverses the exprs graph looking for
 * ExprBaseTable nodes. For each ExprBaseTable it finds, it creates an
 * ExprReceive and places it right above the associated ExprBaseTable. It
 * then tries to pull-up the ExprReceive as far as it can go.
 *
 * For now, a query can have at most one ExprBaseTable, which if present, is a
 * child of the single ExprSFW in the query. As a result, the work done by the
 * Distributer is very simple.
 */
class Distributer extends ExprVisitor {

    QueryControlBlock theQCB;

    private final ExprWalker theWalker;

    Distributer(QueryControlBlock qcb) {
        theQCB = qcb;
        theWalker = new ExprWalker(this, false/*allocateChildrenIter*/);
    }

    void distributeQuery() {
        theWalker.walk(theQCB.getRootExpr());
    }

    @Override
    void exit(ExprBaseTable e) {
        ExprReceive recv = new ExprReceive(theQCB, theQCB.getInitSctx());
        e.replace(recv, false);
        recv.setInput(e, false/*destroy*/);

        recv.setEliminateIndexDups(e.getEliminateIndexDups());
    }

    @Override
    boolean enter(ExprSFW sfw) {

        theWalker.walk(sfw.getDomainExpr(0));

        if (sfw.getDomainExpr(0).getKind() != ExprKind.RECEIVE) {
            return false;
        }

        /*
         * Pull the receive expr above the SFW expr
         */
        ExprReceive rcv = (ExprReceive)sfw.getDomainExpr(0);

        sfw.setDomainExpr(0, rcv.getInput(), false/*destroy*/);
        sfw.replace(rcv, false);
        rcv.setInput(sfw, false/*destroy*/);

        Expr offset = sfw.getOffset();
        Expr limit = sfw.getLimit();
        boolean hasSort = (sfw.getNumSortExprs() != 0);
        boolean hasOffset = (offset != null);
        boolean hasLimit = (limit != null);
        boolean eliminateIndexDups = rcv.getEliminateIndexDups();

        boolean isSinglePartition =
            (rcv.getDistributionKind() == DistributionKind.SINGLE_PARTITION);

        /* TODO: remove this when dup elim is always done at the server */
        if (sfw.getNumVars() > 1) {
            eliminateIndexDups = false;
            rcv.setEliminateIndexDups(false);
        }

        /*
         * If it is a single-partition query, sfw will be sent as-is to the
         * server and there is no need to add anything in the client-side plan.
         */
        if (isSinglePartition) {
            return false;
        }

        if (!hasSort && !eliminateIndexDups && !(hasOffset || hasLimit)) {
            return false;
        }

        /*
         * We are here because either (a) the SFW has order-by, or (b) it has
         * offset-limit and it's not a single-partition query, or (c) we must
         * do elimination of index duplicates.
         *
         * In all cases, we create a new SFW expr on top of the RCV expr,
         * using the RCV expr as the FROM expr. This new SFW will be executed
         * at the client, whereas the original SFW will be sent to the server.
         *
         * If the SFW expr has sort do the following:
         * - Add the sort exprs in the SELECT clause of the server SFW, if not
         *   there already.
         * - Add to the receive expr the positions of the sort exprs within
         *   the above SELECT clause; also add the sort specs.
         * - Add to the client SELECT clause the fields that correspond to the
         *   fields of the original SFW (before the addition of the sort exprs).
         *
         * If the SFW expr has offset-limit and the query is not single-
         * partition, do the following:
         * - Add the offset and limit to the client SFW. The offset-limit will
         *   essentially be done at the client.
         * - Remove the offset from the server SFW,
         * - Change the limit in the server SFW to be the sum of the original
         *   limit plus the original offset. This is the maximum number of
         *   results that any RN will need to compute and send to the client.
         *
         * If dup elimination is needed:
         * - Add in the SELECT clause of the server SFW exprs to retrieve the
         *   prim key column, if not there already.
         * - Add to the receive expr the positions of the prim key exprs within
         *   the above SELECT clause.
         * - Add to the client SELECT clause the fields that correspond to the
         *   fields of the original SFW.
         */
        int numFields = sfw.getNumFields();

        if (hasSort) {
            int[] sortExprPositions = sfw.addSortExprsToSelect();

            rcv.addSort(sortExprPositions, sfw.getSortSpecs());
        }

        if (eliminateIndexDups) {
            int[] primKeyPositions = sfw.addPrimKeyToSelect();

            rcv.addPrimKeyPositions(primKeyPositions);
        }

        /*
         * If we didn't add any exprs in the server-side sfw and there is no
         * offset/limit, we are done (no need to add a client-side SFW).
         */
        if (numFields == sfw.getNumFields() && (!(hasOffset || hasLimit))) {
            return false;
        }

        ExprSFW clientSFW =
            new ExprSFW(theQCB, sfw.getSctx(), sfw.getLocation());

        rcv.replace(clientSFW, false);

        FromClause fc =
            clientSFW.addFromClause(rcv, theQCB.createInternalVarName("from"));
        ExprVar fromVar = fc.getVar();

        ArrayList<Expr> fieldExprs = new ArrayList<Expr>(numFields);
        ArrayList<String> fieldNames = new ArrayList<String>(numFields);

        if (sfw.getConstructsSelectRecord()) {
            for (int i = 0; i < numFields; ++i) {
                Expr fieldExpr =
                    new ExprFieldStep(theQCB,
                                      sfw.getSctx(),
                                      sfw.getFieldExpr(i).getLocation(),
                                      fromVar,
                                      sfw.getFieldName(i));
                fieldExprs.add(fieldExpr);
                fieldNames.add(sfw.getFieldName(i));
            }
        } else {
            fieldExprs.add(fromVar);
            fieldNames.add(sfw.getFieldName(0));
        }

        clientSFW.addSelectClause(fieldNames,
                                  fieldExprs,
                                  sfw.hasSelectASclauses());

        if (hasOffset || hasLimit) {

            if (hasLimit && hasOffset) {
                sfw.removeOffset(false/*destroy*/);
                Expr newLimit = FuncArithOp.createArithExpr(offset, limit, "+");
                sfw.setLimit(newLimit, false/*destroy*/);
            } else if (hasOffset) {
                sfw.removeOffset(false/*destroy*/);
            }

            clientSFW.addOffsetLimit(offset, limit);
        }

        return false;
    }
}
