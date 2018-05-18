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
import java.util.HashMap;
import java.util.Stack;

import oracle.kv.impl.api.table.RecordDefImpl;
import oracle.kv.impl.query.QueryStateException;
import oracle.kv.impl.query.compiler.Expr.ExprKind;
import oracle.kv.impl.query.runtime.ArrayConstrIter;
import oracle.kv.impl.query.runtime.ConcatIter;
import oracle.kv.impl.query.runtime.MapConstrIter;
import oracle.kv.impl.query.runtime.BaseTableIter;
import oracle.kv.impl.query.runtime.CaseIter;
import oracle.kv.impl.query.runtime.CastIter;
import oracle.kv.impl.query.runtime.ConstIter;
import oracle.kv.impl.query.runtime.ExternalVarRefIter;
import oracle.kv.impl.query.runtime.FieldStepIter;
import oracle.kv.impl.query.runtime.ArrayFilterIter;
import oracle.kv.impl.query.runtime.MapFilterIter;
import oracle.kv.impl.query.runtime.IsOfTypeIter;
import oracle.kv.impl.query.runtime.PlanIter;
import oracle.kv.impl.query.runtime.PromoteIter;
import oracle.kv.impl.query.runtime.ReceiveIter;
import oracle.kv.impl.query.runtime.SFWIter;
import oracle.kv.impl.query.runtime.ArraySliceIter;
import oracle.kv.impl.query.runtime.VarRefIter;

/**
 * Walks the expressions tree (actually DAG) and generates the query execution
 * plan (a tree of PlanIter objs) by constructing the appropriate PlanIter for
 * each expression.
 */
public class CodeGenerator extends ExprVisitor {

    private final QueryControlBlock theQCB;

    private final ExprWalker theWalker;

    private final HashMap<Expr, Integer> theResultRegsMap;

    private final HashMap<Expr, int[]> theTupleRegsMap;

    private final Stack<PlanIter> theIters;

    private PlanIter theRootIter;

    private RuntimeException theException = null;

    CodeGenerator(QueryControlBlock qcb) {
        theQCB = qcb;
        theWalker = new ExprWalker(this, false/*allocateChildrenIter*/);
        theResultRegsMap = new HashMap<Expr, Integer>();
        theTupleRegsMap = new HashMap<Expr, int[]>();
        theIters = new Stack<PlanIter>();
    }

    public RuntimeException getException() {
        return theException;
    }

    public void setException(RuntimeException e) {
        theException = e;
    }

    PlanIter getRootIter() {
        return theRootIter;
    }

    public void generatePlan(Expr expr) {

        try {
            if (theQCB.theHaveJsonConstructors) {
                ExprUtils.adjustConstructorTypes(expr);
            }

            theWalker.walk(expr);
            theRootIter = theIters.pop();
            assert(theIters.isEmpty());
        } catch (RuntimeException e) {
            setException(e);
        }
    }

    int allocateResultReg(Expr e) {

        Integer reg = theResultRegsMap.get(e);

        if (reg == null) {
            reg = new Integer(theQCB.getNumRegs());
            theResultRegsMap.put(e, reg);
            theQCB.incNumRegs(1);
        }

        return reg.intValue();
    }

   int allocateLocalReg() {

       Integer reg = new Integer(theQCB.getNumRegs());
       theQCB.incNumRegs(1);

       return reg.intValue();
    }

    void setResultReg(Expr e, int reg) {

        assert(reg >= 0 && reg < theQCB.getNumRegs());

        Integer currReg = theResultRegsMap.get(e);

        if (currReg != null) {
            throw new QueryStateException(
                "Cannot update existing register for expression: " +
                e.display());
        }

        currReg = new Integer(reg);
        theResultRegsMap.put(e, currReg);
    }

    int getResultReg(Expr e) {
        Integer reg = theResultRegsMap.get(e);
        return (reg == null ? -1 : reg.intValue());
    }

    int[] allocateTupleRegs(Expr e, int numRegs) {

        int[] regs = theTupleRegsMap.get(e);

        if (regs == null) {

            regs = new int[numRegs];
            theTupleRegsMap.put(e, regs);

            for (int i = 0; i < numRegs; ++i) {
                regs[i] = theQCB.getNumRegs() + i;
            }

            theQCB.incNumRegs(numRegs);
        }

        return regs;
    }

    void setTupleRegs(Expr e, int[] regs) {

        if (regs == null) {
            return;
        }

        int[] currRegs = theTupleRegsMap.get(e);

        if (currRegs != null) {
            throw new QueryStateException(
                "Cannot update existing tuple registers for expression: " +
                e.display());
        }

        theTupleRegsMap.put(e, regs);
    }

    int[] getTupleRegs(Expr e) {
        return theTupleRegsMap.get(e);
    }

    @Override
    void exit(ExprReceive e) {

        PlanIter inputIter = theIters.pop();
        int resultReg = inputIter.getResultReg();

        PlanIter[] pushedExternalIters = null;

        if (e.getPushedExternals() != null) {

            ArrayList<Expr> pushedExternalExprs = e.getPushedExternals();
            int size = pushedExternalExprs.size();

            pushedExternalIters = new PlanIter[size];

            for (int i = 0; i < size; ++i) {

                Expr expr = pushedExternalExprs.get(i);

                if (expr == null) {
                    pushedExternalIters[i] = null;
                } else {
                    theWalker.walk(expr);
                    PlanIter iter = theIters.pop();
                    pushedExternalIters[i] = iter;
                }
            }
        }

        /*
         * If the ReceiveIter is the root iter, it just propagates to its
         * output the FieldValues (most likely RecordValues) it receves from
         * the RNs. Otherwise, if its input iter produces tuples, the
         * ReceiveIter will recreate these tuples at its output by unnesting
         * into tuples the RecordValues arriving from the RNs.
         */
        int[] tupleRegs = null;

        if (inputIter.producesTuples() && theQCB.getRootExpr() != e) {
            tupleRegs = inputIter.getTupleRegs();
        }

        /*
         * Set the DistributionKind in the QCB so it can be accessed by the
         * upper-level compiler code.
         */
        theQCB.setPushedDistributionKind(e.getDistributionKind());

        PlanIter iter = new ReceiveIter(
            e, resultReg, tupleRegs, inputIter,
            e.getType().getDef(), e.mayReturnNULL(),
            e.getSortFieldPositions(), e.getSortSpecs(),
            e.getPrimKeyPositions(),
            e.getDistributionKind(), e.getPrimaryKey(),
            pushedExternalIters,
            theQCB.getNumRegs(), theQCB.getNumIterators());

        theIters.push(iter);
    }

    @Override
    void exit(ExprConst e) {

        int resultReg = allocateResultReg(e);

        PlanIter constIter = new ConstIter(e, resultReg, e.getValue());
        theIters.push(constIter);
    }

    @Override
    void exit(ExprVar e) {

        String name = e.getName();
        PlanIter varIter;

        if (e.isExternal()) {
            int resultReg = allocateResultReg(e);
            varIter = new ExternalVarRefIter(e, resultReg, e.getId(), name);
        } else {
            /*
             * The registers for a var expr are allocated by the expr that
             * defines the variable.
             */
            int resultReg = getResultReg(e);
            int[] inputTupleRegs = getTupleRegs(e);
            varIter = new VarRefIter(e, resultReg, inputTupleRegs, name);
        }

        theIters.push(varIter);
    }

    @Override
    void exit(ExprArrayConstr e) {

        int numArgs = e.getNumArgs();

        PlanIter argIters[] = new PlanIter[numArgs];

        for (int i = 0; i < e.getNumArgs(); ++i) {
            argIters[numArgs - i - 1] = theIters.pop();
        }

        int resultReg = allocateResultReg(e);

        PlanIter iter = new ArrayConstrIter(e, resultReg, argIters);
        theIters.push(iter);
    }

    @Override
    void exit(ExprMapConstr e) {

        int numArgs = e.getNumArgs();

        PlanIter argIters[] = new PlanIter[numArgs];

        for (int i = 0; i < e.getNumArgs(); ++i) {
            argIters[numArgs - i - 1] = theIters.pop();
        }

        int resultReg = allocateResultReg(e);

        PlanIter iter = new MapConstrIter(e, resultReg, argIters);
        theIters.push(iter);
    }

    @Override
    void exit(ExprFuncCall e) {

        int numArgs = e.getNumArgs();

        PlanIter argIters[] = new PlanIter[numArgs];

        for (int i = 0; i < e.getNumArgs(); ++i) {
            argIters[numArgs - i - 1] = theIters.pop();
        }

        PlanIter iter = e.getFunction().codegen(this, e, argIters);

        theIters.push(iter);
    }

    @Override
    void exit(ExprPromote e) {

        PlanIter inputIter = theIters.pop();
        int resultReg;

        if (inputIter.producesTuples()) {
            resultReg = inputIter.getResultReg();
        } else {
            resultReg = allocateResultReg(e);
        }

        PlanIter promoteIter = new PromoteIter(
            e, resultReg, inputIter, e.getTargetType());

        theIters.push(promoteIter);
    }

    @Override
    void exit(ExprIsOfType e) {

        PlanIter inputIter = theIters.pop();
        int resultReg = allocateResultReg(e);

        PlanIter isOfTypeIter = new IsOfTypeIter(
            e, resultReg, inputIter, e.isNot(), e.getTargetTypes(),
            e.getTargetQuantifiers(), e.getOnlyTargetFlags());

        theIters.push(isOfTypeIter);
    }

    @Override
    void exit(ExprCast e) {

        PlanIter inputIter = theIters.pop();
        int resultReg = allocateResultReg(e);

        PlanIter castIter = new CastIter(e, resultReg, inputIter,
                                         e.getTargetType(),
                                         e.getTargetQuantifier());
        theIters.push(castIter);
    }

    @Override
    boolean enter(ExprFieldStep e) {

        ExprVar ctxItemVar = e.getCtxItemVar();

        if (ctxItemVar != null) {
            allocateResultReg(ctxItemVar);
        }

        return true;
    }

    @Override
    void exit(ExprFieldStep e) {

        PlanIter fieldNameIter = null;
        int ctxItemReg = -1;
        PlanIter inputIter;
        int resultReg;

        /*
         * If we don't have a known field name to select, the field name will be
         * computed in runtime. In this case, if the field name expr references
         * the ctx item, get the register that will hold the value of this var.
         * Furthermore, even if the input iterator produces tuples, we have to
         * allocate a new result reg for the FieldStepIter and "copy" into it
         * the field to select, because if the field to select is different for
         * each input tuple, we cannot update theResultReg of the FieldStepIter
         * to point to the tuple reg that stores the current field to select.
         */
        if (!e.isConst()) {
            fieldNameIter = theIters.pop();
            inputIter = theIters.pop();

            ExprVar ctxItemVar = e.getCtxItemVar();
            ctxItemReg = (ctxItemVar != null ? getResultReg(ctxItemVar) : -1);

            resultReg = allocateResultReg(e);

        } else {
            inputIter = theIters.pop();

            if (inputIter.producesTuples()) {
                int[] inputTupleRegs = inputIter.getTupleRegs();
                resultReg = inputTupleRegs[e.getFieldPos()];
            } else {
                resultReg = allocateResultReg(e);
            }
        }

        PlanIter iter = new FieldStepIter(
            e, resultReg, inputIter, fieldNameIter,
            e.getFieldName(), e.getFieldPos(), ctxItemReg);

        theIters.push(iter);
    }

    @Override
    boolean enter(ExprMapFilter e) {

        if (e.getType().isEmpty()) {
            assert(e.isConst());
            PlanIter argIters[] = new PlanIter[0];
            int resultReg = allocateResultReg(e);
            PlanIter iter = new ConcatIter(e, resultReg, argIters);
            theIters.push(iter);
            return false;
        }

        ExprVar ctxItemVar = e.getCtxItemVar();
        ExprVar ctxElemVar = e.getCtxElemVar();
        ExprVar ctxKeyVar = e.getCtxKeyVar();

        if (ctxItemVar != null) {
            allocateResultReg(ctxItemVar);
        }

        if (ctxElemVar != null) {
            allocateResultReg(ctxElemVar);
        }

        if (ctxKeyVar != null) {
            allocateResultReg(ctxKeyVar);
        }

        return true;
    }

    @Override
    void exit(ExprMapFilter e) {

        if (e.getType().isEmpty()) {
            return;
        }

        assert(e.isConst() || e.getConstValue() == true);

        PlanIter predIter = (e.getPredExpr() != null ? theIters.pop() : null);

        PlanIter inputIter = theIters.pop();

        ExprVar ctxItemVar = e.getCtxItemVar();
        int ctxItemReg = (ctxItemVar != null ? getResultReg(ctxItemVar) : -1);

        ExprVar ctxElemVar = e.getCtxElemVar();
        int ctxElemReg = (ctxElemVar != null ? getResultReg(ctxElemVar) : -1);

        ExprVar ctxKeyVar = e.getCtxKeyVar();
        int ctxKeyReg = (ctxKeyVar != null ? getResultReg(ctxKeyVar) : -1);

        int resultReg = allocateResultReg(e);

        PlanIter iter = new MapFilterIter(
            e, resultReg, inputIter, predIter,
            ctxItemReg, ctxElemReg, ctxKeyReg);

        theIters.push(iter);
    }

    @Override
    boolean enter(ExprArraySlice e) {

        ExprVar ctxItemVar = e.getCtxItemVar();

        if (ctxItemVar != null) {
            allocateResultReg(ctxItemVar);
        }

        return true;
    }

    @Override
    void exit(ExprArraySlice e) {

        PlanIter inputIter;
        PlanIter lowIter = null;
        PlanIter highIter = null;
        int ctxItemReg = -1;
        int resultReg;

        /*
         * If we don't have known boundaries, the boundaries will be computed
         * in runtime. In this case, if a boundary expr references the ctx item,
         * get the register that will hold the value of this var.
         */
        if (!e.isConst()) {
            highIter = (e.getHighExpr() != null ? theIters.pop() : null);
            lowIter = (e.getLowExpr() != null ? theIters.pop() : null);
            inputIter = theIters.pop();

            ExprVar ctxItemVar = e.getCtxItemVar();
            ctxItemReg = (ctxItemVar != null ? getResultReg(ctxItemVar) : -1);
        } else {
            inputIter = theIters.pop();
        }

        resultReg = allocateResultReg(e);

        PlanIter iter = new ArraySliceIter(
            e, resultReg, inputIter, lowIter, highIter,
            e.getLowValue(), e.getHighValue(), ctxItemReg);

        theIters.push(iter);
    }

    @Override
    boolean enter(ExprArrayFilter e) {

        if (e.getType().isEmpty()) {
            assert(e.isConst());
            PlanIter argIters[] = new PlanIter[0];
            int resultReg = allocateResultReg(e);
            PlanIter iter = new ConcatIter(e, resultReg, argIters);
            theIters.push(iter);
            return false;
        }

        ExprVar ctxItemVar = e.getCtxItemVar();
        ExprVar ctxElemVar = e.getCtxElemVar();
        ExprVar ctxElemPosVar = e.getCtxElemPosVar();

        if (ctxItemVar != null) {
            allocateResultReg(ctxItemVar);
        }

        if (ctxElemVar != null) {
            allocateResultReg(ctxElemVar);
        }

        if (ctxElemPosVar != null) {
            allocateResultReg(ctxElemPosVar);
        }

        return true;
    }

    @Override
    void exit(ExprArrayFilter e) {

        PlanIter predIter = (e.getPredExpr() != null ? theIters.pop() : null);

        PlanIter inputIter = theIters.pop();

        ExprVar ctxItemVar = e.getCtxItemVar();
        int ctxItemReg = (ctxItemVar != null ? getResultReg(ctxItemVar) : -1);

        ExprVar ctxElemVar = e.getCtxElemVar();
        int ctxElemReg = (ctxElemVar != null ? getResultReg(ctxElemVar) : -1);

        ExprVar ctxElemPosVar = e.getCtxElemPosVar();
        int ctxElemPosReg = (ctxElemPosVar != null ?
                             getResultReg(ctxElemPosVar) : -1);

        int resultReg = allocateResultReg(e);

       PlanIter iter = new ArrayFilterIter(
            e, resultReg, inputIter, predIter,
            ctxItemReg, ctxElemReg, ctxElemPosReg);

        theIters.push(iter);
    }

    @Override
    void exit(ExprCase e) {

        PlanIter elseIter = null;

        if (e.hasElseClause()) {
            elseIter = theIters.pop();
        }

        PlanIter[] condIters = new PlanIter[e.getNumWhenClauses()];
        PlanIter[] thenIters = new PlanIter[e.getNumWhenClauses()];

        int numWhenClauses = e.getNumWhenClauses();

        for (int i = numWhenClauses - 1; i >= 0; --i) {
            thenIters[i] = theIters.pop();
            condIters[i] = theIters.pop();
        }

        int resultReg = allocateResultReg(e);

        PlanIter iter = new CaseIter(
            e, resultReg, condIters, thenIters, elseIter);

        theIters.push(iter);
    }

    @Override
    boolean enter(ExprBaseTable e) {

        int numTupleRegs =
            ((RecordDefImpl)(e.getType().getDef())).getNumFields();

        int[] tupleRegs = allocateTupleRegs(e, numTupleRegs);
        int resultReg = allocateResultReg(e);

        PlanIter[] pushedExternalIters =  null;

        if (e.getPushedExternals() != null) {

            ArrayList<Expr> pushedExternalExprs = e.getPushedExternals();
            int size = pushedExternalExprs.size();

            pushedExternalIters = new PlanIter[size];

            for (int i = 0; i < size; ++i) {

                Expr expr = pushedExternalExprs.get(i);

                if (expr == null) {
                    pushedExternalIters[i] = null;
                } else {
                    theWalker.walk(expr);
                    PlanIter iter = theIters.pop();
                    pushedExternalIters[i] = iter;
                }
            }
        }

        PlanIter tableIter = new BaseTableIter(
            e, resultReg, tupleRegs, e.getTable(),
            e.getDirection(),
            e.getPrimaryKey(), e.getSecondaryKey(),
            e.getRange(), e.getUsesCoveringIndex(),
            e.getEliminateIndexDups(), pushedExternalIters);

        theIters.push(tableIter);

        /*
         * Return false so that no codegen is done for theFilteringPred, if
         * any. Codegen for theFilteringPred cannot be done here because no
         * registers have been allocated for the variable associated with
         * this ExprBaseTable. Instead, it will be done by the SFW expr (see
         * below).
         */
        return false;
    }

    @Override
    boolean enter(ExprSFW sfw) {

        sfw.removeUnusedVars();

        int numVars = sfw.getNumVars();

        PlanIter[] fromIters = new PlanIter[numVars];
        String[] varNames = new String[numVars];

        for (int i = 0; i < numVars; ++i) {

            ExprVar var = sfw.getVar(i);
            Expr domExpr = sfw.getDomainExpr(i);

            theWalker.walk(domExpr);

            fromIters[i] = theIters.pop();
            varNames[i] = var.getName();

            setResultReg(var, fromIters[i].getResultReg());

            if (fromIters[i].producesTuples()) {
                setTupleRegs(var, fromIters[i].getTupleRegs());
            }

            /* See comment above, in enter(ExprBaseTable e) */
            if (domExpr.getKind() == ExprKind.BASE_TABLE) {

                ExprBaseTable tableExpr = (ExprBaseTable)domExpr;

                if (tableExpr.getFilteringPred() != null) {
                    theWalker.walk(tableExpr.getFilteringPred());
                    ((BaseTableIter)fromIters[i]).setFilterIter(theIters.pop());
                }
            }
        }

        PlanIter whereIter = null;
        if (sfw.getWhereExpr() != null) {
            theWalker.walk(sfw.getWhereExpr());
            whereIter = theIters.pop();
        }

        int numFields = sfw.getNumFields();
        PlanIter[] selectIters = new PlanIter[numFields];
        int[] tupleRegs = null;
        int resultReg = allocateResultReg(sfw);

        if (sfw.getConstructsSelectRecord()) {

            resultReg = allocateResultReg(sfw);
            tupleRegs = new int[numFields];

            for (int i = 0; i < numFields; ++i) {
                theWalker.walk(sfw.getFieldExpr(i));
                selectIters[i] = theIters.pop();
                if (selectIters[i].producesTuples()) {
                    tupleRegs[i] = allocateLocalReg();
                } else {
                    tupleRegs[i] = selectIters[i].getResultReg();
                }
            }
        } else {
            assert(numFields == 1);
            theWalker.walk(sfw.getFieldExpr(0));
            selectIters[0] = theIters.pop();
            resultReg = selectIters[0].getResultReg();
        }

        PlanIter offsetIter = null;
        PlanIter limitIter = null;

        if (sfw.getOffset() != null) {
            theWalker.walk(sfw.getOffset());
            offsetIter = theIters.pop();
        }

        if (sfw.getLimit() != null) {
            theWalker.walk(sfw.getLimit());
            limitIter = theIters.pop();
        }

        PlanIter sfwIter = new SFWIter(
            sfw, resultReg, tupleRegs,
            fromIters, varNames, whereIter, selectIters,
            sfw.getFieldNamesArray(), offsetIter, limitIter);

        theIters.push(sfwIter);
        return false;
    }
}
