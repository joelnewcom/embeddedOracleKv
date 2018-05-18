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

package oracle.kv.impl.query.runtime;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import oracle.kv.impl.api.table.FieldDefImpl;
import oracle.kv.impl.api.table.FieldValueImpl;
import oracle.kv.impl.api.table.NullValueImpl;
import oracle.kv.impl.query.QueryException;
import oracle.kv.impl.query.QueryStateException;
import oracle.kv.impl.query.compiler.Expr;
import oracle.kv.impl.query.compiler.FunctionLib;
import oracle.kv.impl.query.compiler.FunctionLib.FuncCode;
import oracle.kv.impl.query.compiler.QueryFormatter;
import oracle.kv.table.FieldDef;
import oracle.kv.table.FieldDef.Type;

/**
 * Iterator to implement the arithmetic operators
 *
 * any_atomic? ArithOp(any?, ....)
 *
 * ArithOpIter.next() must check that all inputs are numeric values.
 *
 * Result:
 *     if all args are int result is an int
 *     if all args are int or long result is a long
 *     if all args are int, long or float result is a float
 *     if all args are int, long, float or double result is a double
 */
public class ArithOpIter extends PlanIter {

    private final FuncCode theCode;

    private final PlanIter[] theArgs;

    /**
     * For theCode == FuncCode.OP_ADD_SUB
     *   contains order of + or - ops
     * For theCode == FuncCode.OP_MULT_DIV
     *   contains order of * or / ops
     */
    private final String theOps;

    public ArithOpIter(
        Expr e,
        int resultReg,
        FunctionLib.FuncCode code,
        PlanIter[] argIters,
        String theOps) {

        super(e, resultReg);
        theCode = code;

        /* ArithOpIter works only with FunctionLib.FuncCode.OP_ADD_SUB and
         FunctionLib.FuncCode.OP_MULT_DIV
         It must have at least 2 args */
        assert ((theCode == FunctionLib.FuncCode.OP_ADD_SUB ||
                theCode == FunctionLib.FuncCode.OP_MULT_DIV) &&
            argIters.length >= 2 );

        theArgs = argIters;
        this.theOps = theOps;
    }

    /**
     * FastExternalizable constructor.
     */
    public ArithOpIter(DataInput in, short serialVersion) throws IOException {
        super(in, serialVersion);
        short ordinal = in.readShort();
        theCode = FunctionLib.FuncCode.values()[ordinal];
        theArgs = deserializeIters(in, serialVersion);
        theOps = in.readUTF();
    }

    /**
     * FastExternalizable writer.  Must call superclass method first to
     * write common elements.
     */
    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
            throws IOException {

        super.writeFastExternal(out, serialVersion);
        out.writeShort(theCode.ordinal());
        serializeIters(theArgs, out, serialVersion);
        out.writeUTF(theOps);
    }

    @Override
    public PlanIterKind getKind() {
        return PlanIterKind.ARITH_OP;
    }

    @Override
    FunctionLib.FuncCode getFuncCode() {
        return theCode;
    }

    @Override
    public void open(RuntimeControlBlock rcb) {
        rcb.setState(theStatePos, new PlanIterState());
        for (PlanIter argIter : theArgs) {
            argIter.open(rcb);
        }
    }

    @Override
    public boolean next(RuntimeControlBlock rcb) {

        PlanIterState state = rcb.getState(theStatePos);

        if (state.isDone()) {
            return false;
        }

        FieldDef.Type resultType = Type.INTEGER;

        for (int i = 0; i < theArgs.length; i++) {

            PlanIter argIter = theArgs[i];
            boolean opNext = argIter.next(rcb);

            if (!opNext) {
                state.done();
                return false;
            }

            FieldValueImpl argValue = rcb.getRegVal(argIter.getResultReg());

            if (argValue.isNull()) {
                FieldValueImpl res = NullValueImpl.getInstance();
                rcb.setRegVal(theResultReg, res);
                state.done();
                return true;
            }

            FieldDef.Type argType = argValue.getType();

            switch (argType) {
            case INTEGER:
                break;
            case LONG:
                if (resultType == Type.INTEGER) {
                    resultType = Type.LONG;
                }
                break;
            case FLOAT:
                if (resultType == Type.INTEGER || resultType == Type.LONG) {
                    resultType = Type.FLOAT;
                }
                break;
            case DOUBLE:
                resultType = Type.DOUBLE;
                break;
            default:
                throw new QueryException(
                    "Operand in arithmetic operation has illegal type\n" +
                    "Operand : " + i + " type : " +
                    argValue.getDefinition().getDDLString(),
                    getLocation());
            }
        }

        assert theOps.length() == theArgs.length :
            "Not enough operations: ops:" + (theOps.length() - 1) + " args:" +
                theArgs.length;

        int iRes = 0;
        long lRes = 0;
        float fRes = 0;
        double dRes = 0;

        switch (theCode) {
        case OP_ADD_SUB:
            dRes = fRes = lRes = iRes = 0;
            break;
        case OP_MULT_DIV:
            dRes = fRes = lRes = iRes = 1;
            break;
        default:
            throw new QueryStateException(
                "Invalid operation code: " + theCode);
        }

        try {
            for (int i = 0 ; i < theArgs.length; i++) {

                PlanIter argIter = theArgs[i];
                FieldValueImpl argValue = rcb.getRegVal(argIter.getResultReg());
                assert (argValue != null);
                boolean addOrMult = theOps.charAt(i) == '+' ||
                    theOps.charAt(i) == '*';

                if (theCode == FuncCode.OP_ADD_SUB) {
                    if (addOrMult) {
                        switch (resultType) {
                        case INTEGER:
                            iRes += argValue.castAsInt();
                            break;
                        case LONG:
                            lRes += argValue.castAsLong();
                            break;
                        case FLOAT:
                            fRes += argValue.castAsFloat();
                            break;
                        case DOUBLE:
                            dRes += argValue.castAsDouble();
                            break;
                        default:
                            assert(false);
                        }
                    } else {
                        switch (resultType) {
                        case INTEGER:
                            iRes -= argValue.castAsInt();
                            break;
                        case LONG:
                            lRes -= argValue.castAsLong();
                            break;
                        case FLOAT:
                            fRes -= argValue.castAsFloat();
                            break;
                        case DOUBLE:
                            dRes -= argValue.castAsDouble();
                            break;
                        default:
                            assert(false);
                        }
                    }
                } else {
                    if (addOrMult) {
                        switch (resultType) {
                        case INTEGER:
                            iRes *= argValue.castAsInt();
                            break;
                        case LONG:
                            lRes *= argValue.castAsLong();
                            break;
                        case FLOAT:
                            fRes *= argValue.castAsFloat();
                            break;
                        case DOUBLE:
                            dRes *= argValue.castAsDouble();
                            break;
                        default:
                            assert(false);
                        }
                    } else {
                        switch (resultType) {
                        case INTEGER:
                            iRes /= argValue.castAsInt();
                            break;
                        case LONG:
                            lRes /= argValue.castAsLong();
                            break;
                        case FLOAT:
                            fRes /= argValue.castAsFloat();
                            break;
                        case DOUBLE:
                            dRes /= argValue.castAsDouble();
                            break;
                        default:
                            assert(false);
                        }
                    }
                }
            }
        } catch (ArithmeticException ae) {
            throw new QueryException(
                "Arithmetic exception in query: " + ae.getMessage(),
                ae, getLocation());
        }

        FieldValueImpl res = null;
        switch (resultType) {
        case INTEGER:
            res = FieldDefImpl.integerDef.createInteger(iRes);
            break;
        case LONG:
            res = FieldDefImpl.longDef.createLong(lRes);
            break;
        case FLOAT:
            res = FieldDefImpl.floatDef.createFloat(fRes);
            break;
        case DOUBLE:
            res = FieldDefImpl.doubleDef.createDouble(dRes);
            break;
        default:
            assert(false);
        }

        rcb.setRegVal(theResultReg, res);

        state.done();
        return true;
    }

    @Override
    public void reset(RuntimeControlBlock rcb) {
        for (PlanIter argIter : theArgs) {
            argIter.reset(rcb);
        }

        PlanIterState state = rcb.getState(theStatePos);
        state.reset(this);
    }

    @Override
    public void close(RuntimeControlBlock rcb) {

        PlanIterState state = rcb.getState(theStatePos);
        if (state == null) {
            return;
        }

        for (PlanIter argIter : theArgs) {
            argIter.close(rcb);
        }

        state.close();
    }

    @Override
    protected void displayContent(StringBuilder sb, QueryFormatter formatter) {

        int i = 0;
        for (PlanIter argIter : theArgs) {

            formatter.indent(sb);
            if (theCode == FuncCode.OP_ADD_SUB) {
                if (theOps.charAt(i) == '+') {
                    sb.append('+');
                } else {
                    sb.append('-');
                }
            }
            else {
                if (theOps.charAt(i) == '*') {
                    sb.append('*');
                } else {
                    sb.append('/');
                }
            }
            sb.append(",\n");
            argIter.display(sb, formatter);
            if (i < theArgs.length - 1) {
                sb.append(",\n");
            }
            ++i;
        }
    }
}
