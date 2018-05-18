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

import oracle.kv.impl.api.table.BooleanValueImpl;
import oracle.kv.impl.api.table.DoubleValueImpl;
import oracle.kv.impl.api.table.FieldDefImpl;
import oracle.kv.impl.api.table.FieldValueImpl;
import oracle.kv.impl.api.table.FloatValueImpl;
import oracle.kv.impl.api.table.IntegerValueImpl;
import oracle.kv.impl.api.table.LongValueImpl;
import oracle.kv.impl.api.table.StringValueImpl;
import oracle.kv.impl.query.QueryException;
import oracle.kv.impl.query.QueryException.Location;
import oracle.kv.impl.query.compiler.Expr.ExprKind;
import oracle.kv.impl.query.compiler.FunctionLib.FuncCode;
import oracle.kv.impl.query.runtime.CompOpIter;
import oracle.kv.impl.query.runtime.PlanIter;
import oracle.kv.impl.query.types.TypeManager;
import oracle.kv.table.FieldDef.Type;

/*
 * boolean comp(any*, any*)
 *
 * Returns NULL if any operand returns NULL.
 * Returns false if any operand returns zero or more than 1 items.
 * Returns false if the items to compare are not comparable.
 */
public class FuncCompOp extends Function {

    FuncCompOp(FuncCode code, String name) {
        super(
            code,
            name,
            TypeManager.ANY_STAR(), /* param1 */
            TypeManager.ANY_STAR(), /* param2 */
            TypeManager.BOOLEAN_ONE()); /* RetType */
    }

    @Override
    boolean isValueComparison() {
        return true;
    }

    @Override
    boolean mayReturnNULL(ExprFuncCall fncall) {

        return (fncall.getArg(0).mayReturnNULL() ||
                fncall.getArg(1).mayReturnNULL());
    }

    @Override
    Expr normalizeCall(ExprFuncCall fncall) {

        Location loc = fncall.getLocation();
        QueryControlBlock qcb = fncall.getQCB();
        StaticContext sctx = fncall.getSctx();
        boolean strict = qcb.strictMode();

        Expr op0 = fncall.getArg(0);
        Expr op1 = fncall.getArg(1);
        FieldDefImpl op0Def = op0.getType().getDef();
        FieldDefImpl op1Def = op1.getType().getDef();
        Type tc0 = op0Def.getType();
        Type tc1 = op1Def.getType();

        /*
         * Nothing to do if the operands have the same kind of type.
         */
        if (tc0 == tc1) {
            return fncall;
        }

        if (!TypeManager.areTypesComparable(op0.getType(), op1.getType())) {

           if (strict) {
               throw new QueryException(
                    "Incompatible types for comparison operator: \n" +
                    "Type1: " + op0.getType() + "\nType2: " + op1.getType(),
                    fncall.getLocation());
           }

           return new ExprConst(qcb, sctx, loc, false);
        }

        return handleConstOperand(fncall, theCode);
    }

    /**
     * Handle the case where one of the operands is a const. In this case, we
     * should be able to create a new const that has the same type as the other
     * operand. This is important for turning comparison predicates into index
     * search keys.
     *
     * Note: When this method is called, we know already that the types of
     * the operands are comparable but not equal. Furthermore, the type of the
     * const operand, if any, cannot be one of the ANY types.
     *
     * Note: opCode is given as an explicit paramater because this method is
     * also called from FuncAnyOp, in which case we want to pass one of the
     * normal comparison op codes instead of the any op code that is stored
     * in the Function obj of fncall.
     */
    static Expr handleConstOperand(ExprFuncCall fncall, FuncCode opCode) {

        Location loc = fncall.getLocation();
        QueryControlBlock qcb = fncall.getQCB();
        StaticContext sctx = fncall.getSctx();

        Expr arg0 = fncall.getArg(0);
        Expr arg1 = fncall.getArg(1);

        Expr varOp = null;
        ExprConst constOp = null;
        int constPos;

        if (arg0.getKind() == ExprKind.CONST) {
            constOp = (ExprConst)arg0;
            constPos = 0;
            varOp = arg1;
            opCode = swapCompOp(opCode);
        } else if (arg1.getKind() == ExprKind.CONST) {
            constOp = (ExprConst)arg1;
            constPos = 1;
            varOp = arg0;
        } else {
            return fncall;
        }

        boolean strict = fncall.getQCB().strictMode();
        FieldDefImpl varDef = varOp.getType().getDef();
        FieldValueImpl constVal = constOp.getValue();

        if (constVal.isJsonNull()) {

            if (opCode == FuncCode.OP_NEQ) {
                /*
                 * json null is always != to anything other than json null
                 */
                if (!varDef.isJson() && !varDef.isAny()) {
                    return new ExprConst(qcb, sctx, loc, true);
                }
            } else {
                /*
                 * For all comp ops except !=, json null is always incomparable
                 * to anything other than json null.
                 */
               if (!varDef.isJson() && !varDef.isAny()) {
                    return new ExprConst(qcb, sctx, loc, false);
                }
            }

            return fncall;
        }

        FieldValueImpl newConstVal = castConstInCompOp(varDef,
                                                       constVal,
                                                       opCode,
                                                       strict);

        if (newConstVal == constVal) {
            return fncall;
        }

        if (newConstVal == BooleanValueImpl.falseValue) {
            return new ExprConst(qcb, sctx, loc, false);
        }

        if (newConstVal == BooleanValueImpl.trueValue) {
            return new ExprConst(qcb, sctx, loc, true);
        }

        ExprConst newConstOp = new ExprConst(qcb, sctx, loc, newConstVal);
        fncall.setArg(constPos, newConstOp, true/*destroy*/);
        return fncall;
    }

    public static FieldValueImpl castConstInCompOp(
        FieldDefImpl targetType,
        FieldValueImpl val,
        FuncCode opCode,
        boolean strict) {

        if (targetType.getType() == val.getType() ||
            targetType.isWildcard() ||
            targetType.getType() == Type.EMPTY) {
            return val;
        }

        switch (targetType.getType()) {

        case INTEGER:
            if (val.isLong()) {

                long lng = ((LongValueImpl)val).get();

                if (lng < Integer.MIN_VALUE || lng > Integer.MAX_VALUE) {
                    switch(opCode) {
                    case OP_EQ:
                        return BooleanValueImpl.falseValue;
                    case OP_LT:
                    case OP_LE:
                        if (lng < Integer.MIN_VALUE) {
                            return BooleanValueImpl.falseValue;
                        }
                        return BooleanValueImpl.trueValue;
                    case OP_GT:
                    case OP_GE:
                        if (lng < Integer.MIN_VALUE) {
                            return BooleanValueImpl.trueValue;
                        }
                        return BooleanValueImpl.falseValue;
                    default:
                        assert(false);
                        return null;
                    }
                }
             
                return FieldDefImpl.integerDef.createInteger((int)lng);
            }

            if (val.isNumeric()) {
                return val;
            }

            break;

        case LONG:
            if (val.isInteger()) {
                return (FieldDefImpl.longDef.
                        createLong(((IntegerValueImpl)val).get()));
            }

            if (val.isNumeric()) {
                return val;
            }

            break;

        case FLOAT:
            if (val.isInteger()) {
                return (FieldDefImpl.floatDef.
                        createFloat(((IntegerValueImpl)val).get()));
            }

            if (val.isLong()) {
                return (FieldDefImpl.floatDef.
                        createFloat(((LongValueImpl)val).get()));
            }

            if (val.isDouble()) {

                double dbl = ((DoubleValueImpl)val).get();

                if (dbl < Float.MIN_VALUE || dbl > Float.MAX_VALUE) {
                    switch(opCode) {
                    case OP_EQ:
                        return BooleanValueImpl.falseValue;
                    case OP_LT:
                    case OP_LE:
                        if (dbl < Float.MIN_VALUE) {
                            return BooleanValueImpl.falseValue;
                        }
                        return BooleanValueImpl.trueValue;
                    case OP_GT:
                    case OP_GE:
                        if (dbl < Float.MIN_VALUE) {
                            return BooleanValueImpl.trueValue;
                        }
                        return BooleanValueImpl.falseValue;
                    default:
                        assert(false);
                        return null;
                    }
                }

                return FieldDefImpl.floatDef.createFloat((float)dbl);
            }
            break;

        case DOUBLE:
            if (val.isInteger()) {
                return (FieldDefImpl.doubleDef.
                        createDouble(((IntegerValueImpl)val).get()));
            }
            if (val.isLong()) {
                return (FieldDefImpl.doubleDef.
                        createDouble(((LongValueImpl)val).get()));
            }
            if (val.isFloat()) {
                return (FieldDefImpl.doubleDef.
                        createDouble(((FloatValueImpl)val).get()));
            }
            break;

        case ENUM:
            if (val.isString()) {
                try {
                    return targetType.createEnum(((StringValueImpl)val).get());
                } catch (IllegalArgumentException e) {
                    if (strict) {
                        throw e;
                    }
                    return BooleanValueImpl.falseValue;
                }
            }
            break;
        default:
            break;
        }

        assert(false);
        return null;
    }

    static FuncCode swapCompOp(FuncCode op) {

        switch (op) {
        case OP_GT:
            return FuncCode.OP_LT;
        case OP_GE:
            return FuncCode.OP_LE;
        case OP_LT:
            return FuncCode.OP_GT;
        case OP_LE:
            return FuncCode.OP_GE;
        default:
            return op;
        }
    }

    static FuncCode getNegationOp(FuncCode op) {

        switch (op) {
        case OP_GT:
            return FuncCode.OP_LE;
        case OP_GE:
            return FuncCode.OP_LT;
        case OP_LT:
            return FuncCode.OP_GE;
        case OP_LE:
            return FuncCode.OP_GT;
        case OP_EQ:
            return FuncCode.OP_NEQ;
        case OP_NEQ:
            return FuncCode.OP_EQ;
        default:
            assert(false);
            return null;
        }
    }

    public static String printOp(FuncCode op) {

        switch (op) {
        case OP_GT:
            return ">";
        case OP_GE:
            return ">=";
        case OP_LT:
            return "<";
        case OP_LE:
            return "<=";
        case OP_EQ:
            return "=";
        case OP_NEQ:
            return "!=";
        default:
            assert(false);
            return null;
        }
    }

    @Override
    PlanIter codegen(
        CodeGenerator codegen,
        Expr fncall,
        PlanIter[] argIters) {

        int resultReg = codegen.allocateResultReg(fncall);

        return new CompOpIter(fncall, resultReg, theCode, argIters);
    }
}
