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

import java.util.Stack;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import oracle.kv.impl.api.table.ArrayValueImpl;
import oracle.kv.impl.api.table.FieldValueImpl;
import oracle.kv.impl.api.table.MapValueImpl;
import oracle.kv.impl.api.table.RecordValueImpl;
import oracle.kv.impl.api.table.TupleValue;
import oracle.kv.impl.query.QueryException;
import oracle.kv.impl.query.compiler.Expr;
import oracle.kv.impl.query.compiler.QueryFormatter;
import oracle.kv.impl.util.SerializationUtil;
import oracle.kv.table.FieldDef.Type;

/**
 *
 */
public class FieldStepIter extends PlanIter {

    static private class ArrayAndPos {

        ArrayValueImpl theArray;
        int thePos;

        ArrayAndPos(ArrayValueImpl array) {
            theArray = array;
            thePos = 0;
        }
    }

    static private class FieldStepState extends PlanIterState {

        final boolean theHasTupleInput;

        Stack<ArrayAndPos> theArrays;

        String theFieldName;

        FieldStepState(FieldStepIter iter) {
            super();
            theArrays = new Stack<ArrayAndPos>();
            theFieldName = iter.theFieldName;
            theHasTupleInput = iter.theInputIter.producesTuples();
        }

        @Override
        protected void reset(PlanIter iter) {
            super.reset(iter);
            if (theArrays != null) {
                theArrays.clear();
            }
            theFieldName = ((FieldStepIter)iter).theFieldName;
        }

        @Override
        protected void close() {
            super.close();
            theArrays = null;
            theFieldName = null;
        }
    }

    private final PlanIter theInputIter;

    private final PlanIter theFieldNameIter;

    private final String theFieldName;

    private final int theFieldPos;

    private final int theCtxItemReg;

    public FieldStepIter(
        Expr e,
        int resultReg,
        PlanIter inputIter,
        PlanIter fieldNameIter,
        String fieldName,
        int fieldPos,
        int ctxItemReg) {

        super(e, resultReg);
        theInputIter = inputIter;
        theFieldNameIter = fieldNameIter;
        theFieldName = fieldName;
        theFieldPos = fieldPos;
        theCtxItemReg = ctxItemReg;
    }

    /**
     * FastExternalizable constructor.
     */
    FieldStepIter(DataInput in, short serialVersion) throws IOException {
        super(in, serialVersion);

        theCtxItemReg = in.readInt();
        theFieldPos = in.readInt();
        theInputIter = deserializeIter(in, serialVersion);
        theFieldName = SerializationUtil.readString(in);

        boolean fieldNameIterExists = in.readBoolean();
        if (fieldNameIterExists) {
            theFieldNameIter = deserializeIter(in, serialVersion);
        } else {
            theFieldNameIter = null;
        }
    }

    /**
     * FastExternalizable writer.  Must call superclass method first to
     * write common elements.
     */
    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
            throws IOException {

        super.writeFastExternal(out, serialVersion);
        out.writeInt(theCtxItemReg);
        out.writeInt(theFieldPos);
        serializeIter(theInputIter, out, serialVersion);
        SerializationUtil.writeString(out, theFieldName);

        if (theFieldNameIter != null) {
            out.writeBoolean(true);
            serializeIter(theFieldNameIter, out, serialVersion);
        } else {
            out.writeBoolean(false);
        }
    }

    @Override
    public PlanIterKind getKind() {
        return PlanIterKind.FIELD_STEP;
    }

    @Override
    public void open(RuntimeControlBlock rcb) {
        rcb.setState(theStatePos, new FieldStepState(this));
        theInputIter.open(rcb);
        if (theFieldNameIter != null) {
            theFieldNameIter.open(rcb);
        }
    }

    @Override
    public boolean next(RuntimeControlBlock rcb) {

        FieldStepState state = (FieldStepState)rcb.getState(theStatePos);

        if (state.isDone()) {
            return false;
        }

        /*
         * theFieldPos is > 0 when the field name is known at compile time and
         * the input expr is also known at compile time to return records of
         * known type. If, in addition, the input iterator returns its results
         * as a tuple, there is nothing to do: The result has already been 
         * stored in a register R by the input iter and theResultReg of this
         * iter points to R.
         */
        if (theFieldPos >= 0 && state.theHasTupleInput) {
            assert(theFieldName != null);
            assert(theInputIter.getTupleRegs() != null);

            boolean more = theInputIter.next(rcb);
            if (!more) {
                state.done();
                return false;
            }
            return true;
        }

        /*
         * Compute the field name expression once here, if it does not
         * depend on the ctx item and has not been computed already.
         */
        if (theFieldName == null && theCtxItemReg < 0) {

            computeFieldName(rcb, state, null, false);

            if (state.theFieldName == null) {
                state.done();
                return false;
            }
        }

        while (true) {

            FieldValueImpl ctxItem = null;
            Type ctxItemKind;
            FieldValueImpl result;

            /*
             * Compute the next context item; either from the input iter or
             * from the top stacked array, if any.
             */
            if (state.theArrays.isEmpty()) {

                boolean more = theInputIter.next(rcb);

                if (!more) {
                    state.done();
                    return false;
                }

                int inputReg = theInputIter.getResultReg();
                ctxItem = rcb.getRegVal(inputReg);

                if (ctxItem.isAtomic()) {
                    continue;
                }

                if (ctxItem.isNull()) {
                    rcb.setRegVal(theResultReg, ctxItem);
                    return true;
                }

                ctxItemKind = ctxItem.getType();

            } else {
                ArrayAndPos arrayCtx = state.theArrays.peek();
                ArrayValueImpl array = arrayCtx.theArray;

                ctxItem = array.getElement(arrayCtx.thePos);
                ctxItemKind = ctxItem.getType();

                ++arrayCtx.thePos;
                if (arrayCtx.thePos >= array.size()) {
                    state.theArrays.pop();
                }

               if (ctxItem.isAtomic()) {
                    continue;
                }
            }

            /*
             * We have a candidate ctx item. If it is an array, stack the
             * array and repeat the loop to get a real ctx item.
             */
            if (ctxItemKind == Type.ARRAY) {
                ArrayValueImpl array = (ArrayValueImpl)ctxItem;
                if (array.size() > 0) {
                    ArrayAndPos arrayCtx = new ArrayAndPos(array);
                    state.theArrays.push(arrayCtx);
                }
                continue;
            } else if (ctxItemKind != Type.RECORD && ctxItemKind != Type.MAP) {
                throw new QueryException(
                    "Input value in field step has invalid type.\n" +
                    "Expected a complex type. Actual type is:\n" +
                    ctxItem.getDefinition(), getLocation());
            }

            /*
             * We really have the ctx item now (it's not an array). Bind the $$
             * var and compute the field name again, if it depends on $$. If
             * there is no field name, repeat the loop to get the next ctx item.
             */
            if (theCtxItemReg >= 0) {

                computeFieldName(rcb, state, ctxItem, true);

                if (state.theFieldName == null) {
                    continue;
                }
            }

            /*
             * Return the value of the specified field in the ctx item.
             */
            if (ctxItemKind == Type.RECORD) {

                if (theFieldPos >= 0) {
                    if (ctxItem.isTuple()) {
                        TupleValue tuple = (TupleValue)ctxItem;
                        result = tuple.get(theFieldPos);
                    } else {
                        RecordValueImpl rec = (RecordValueImpl)ctxItem;
                        result = rec.get(theFieldPos);
                    }
                } else {
                    if (ctxItem.isTuple()) {
                        TupleValue tuple = (TupleValue)ctxItem;
                        result = tuple.getFieldValue(state.theFieldName);
                    } else {
                        RecordValueImpl rec = (RecordValueImpl)ctxItem;
                        result = rec.get(state.theFieldName);
                    }
                }

                if (result == null) {
                    throw new QueryException(
                        "There is no field named " + state.theFieldName +
                        " in type\n" + ctxItem.getDefinition(), getLocation());
                }

            } else {
                assert(ctxItemKind == Type.MAP);
                MapValueImpl map = (MapValueImpl)ctxItem;
                result = map.get(state.theFieldName);

                if (result == null) {
                    continue;
                }
            }

            rcb.setRegVal(theResultReg, result);
            return true;
        }
    }

    void computeFieldName(
        RuntimeControlBlock rcb,
        FieldStepState state,
        FieldValueImpl ctxItem,
        boolean reset) {

        if (reset) {
            theFieldNameIter.reset(rcb);
        }

        if (theCtxItemReg >= 0) {
            rcb.setRegVal(theCtxItemReg, ctxItem);
        }

        boolean more = theFieldNameIter.next(rcb);

        if (!more) {
            state.theFieldName = null;
            return;
        }

        int nameReg = theFieldNameIter.getResultReg();
        FieldValueImpl name = rcb.getRegVal(nameReg);

        if (name.isNull()) {
            state.theFieldName = null;
        } else {
            state.theFieldName = name.getString();
        }
    }

    @Override
    public void reset(RuntimeControlBlock rcb) {
        theInputIter.reset(rcb);
        if (theFieldNameIter != null) {
            theFieldNameIter.reset(rcb);
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

        theInputIter.close(rcb);
        if (theFieldNameIter != null) {
            theFieldNameIter.close(rcb);
        }

        state.close();
    }

   @Override
   protected void displayContent(StringBuilder sb, QueryFormatter formatter) {

       theInputIter.display(sb, formatter);

       sb.append(",\n");
       if (theFieldNameIter != null) {
           theFieldNameIter.display(sb, formatter);
       } else {
           formatter.indent(sb);
           sb.append(theFieldName);
       }

       sb.append(",\n");
       formatter.indent(sb);
       sb.append("theFieldPos : ").append(theFieldPos);

       if (theCtxItemReg >= 0) {
           sb.append(",\n");
           formatter.indent(sb);
           sb.append("theCtxItemReg : ").append(theCtxItemReg);
       }
   }
}
