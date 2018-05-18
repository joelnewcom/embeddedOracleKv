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

import static oracle.kv.impl.util.SerialVersion.QUERY_VERSION_2;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import oracle.kv.impl.api.table.FieldDefImpl;
import oracle.kv.impl.api.table.FieldValueImpl;
import oracle.kv.impl.api.table.NullValueImpl;
import oracle.kv.impl.api.table.RecordDefImpl;
import oracle.kv.impl.api.table.TupleValue;
import oracle.kv.impl.query.QueryException;
import oracle.kv.impl.query.compiler.Expr;
import oracle.kv.impl.query.compiler.QueryFormatter;
import oracle.kv.impl.util.SerializationUtil;
import oracle.kv.impl.util.SerialVersion;

/**
 * SFWIter evaluates a SELECT-FROM_WHERE query block. The iterator produces
 * a sequence of FieldValues. If the SFW expr does implicit record construction
 * (i.e., the SELECT clause contains more than 1 expr, or a single expr with an
 * AS clause), then the sequence consists of TupleValues. In fact, in this case,
 * a single TupleValue is allocated (during the open() method) and placed in
 * this.theResultReg. The TupleValue points to the registers that hold the
 * field values of the tuple. These registers are filled-in during each next()
 * call. The names of the tuple columns are statically known (i.e., they don't
 * need to be computed during runtime), and as a result, they are not placed 
 * in registers; instead they are available via method calls.
 *
 * theFromIters:
 *
 * theFromVarNames:
 *
 * theWhereIter:
 *
 * theColumnIters:
 *
 * theColumnNames:
 *
 * theTupleRegs:
 *
 * theTypeDefinition:
 * The type of the data returned by this iterator.
 */
public class SFWIter extends PlanIter {

    public static class SFWIterState extends PlanIterState {

        private int theNumBoundVars;

        private long theOffset;
        
        private long theLimit;

        private long theNumResults;

        @Override
        protected void reset(PlanIter iter) {
            super.reset(iter);
            theNumBoundVars = 0;
            theNumResults = 0;
        }
    }

    private final PlanIter[] theFromIters;

    private final String[] theFromVarNames;

    private final PlanIter theWhereIter;

    private final PlanIter[] theColumnIters;

    private final String[] theColumnNames;

    private final int[] theTupleRegs;

    private final FieldDefImpl theTypeDefinition;

    private final PlanIter theOffsetIter;

    private final PlanIter theLimitIter;

    public SFWIter(
        Expr e,
        int resultReg,
        int[] tupleRegs,
        PlanIter[] fromIters,
        String[] fromVarNames,
        PlanIter whereIter,
        PlanIter[] columnIters,
        String[] columnNames,
        PlanIter offsetIter,
        PlanIter limitIter) {

        super(e, resultReg);
        theFromIters = fromIters;
        theFromVarNames = fromVarNames;
        theWhereIter = whereIter;
        theColumnIters = columnIters;
        theColumnNames = columnNames;
        theTupleRegs = tupleRegs;
        theTypeDefinition = e.getType().getDef();
        theOffsetIter = offsetIter;
        theLimitIter = limitIter;
    }

    /**
     * FastExternalizable constructor.
     */
    SFWIter(DataInput in, short serialVersion) throws IOException {

        super(in, serialVersion);
        theTypeDefinition = (FieldDefImpl)deserializeFieldDef(in, serialVersion);
        theTupleRegs = deserializeIntArray(in);
        theColumnNames = deserializeStringArray(in);
        theColumnIters = deserializeIters(in, serialVersion);

        if (serialVersion < QUERY_VERSION_2) {
            theFromIters = new PlanIter[1];
            theFromVarNames = new String[1];
            theFromIters[0] = deserializeIter(in, serialVersion);
            theFromVarNames[0] = SerializationUtil.readString(in);
        } else {
            theFromIters = deserializeIters(in, serialVersion);
            theFromVarNames = new String[theFromIters.length];
            for (int i = 0; i < theFromIters.length; ++i) {
                theFromVarNames[i] = SerializationUtil.readString(in);
            }
        }
        theWhereIter = deserializeIter(in, serialVersion);
        theOffsetIter = deserializeIter(in, serialVersion);
        theLimitIter = deserializeIter(in, serialVersion);
    }

    /**
     * FastExternalizable writer.  Must call superclass method first to
     * write common elements.
     */
    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
            throws IOException {

        super.writeFastExternal(out, serialVersion);
        serializeFieldDef(theTypeDefinition, out, serialVersion);
        serializeIntArray(theTupleRegs, out);
        serializeStringArray(theColumnNames, out);
        serializeIters(theColumnIters, out, serialVersion);

        if (serialVersion < QUERY_VERSION_2) {

            final String QV2String =
                SerialVersion.getKVVersion(QUERY_VERSION_2).
                getNumericVersionString();
            
            if (theTupleRegs == null) {
                throw new QueryException(
                    "Cannot execute a query with a single expression in the " +
                    "SELECT with no associated AS clause at a server whose " +
                    "version is less than " + QV2String +
                    "\nserialVersion = " + serialVersion +
                    " expected version = " + QUERY_VERSION_2);
            }

            if (theFromIters.length > 1) {
                throw new QueryException(
                    "Cannot execute a query with more than one expression in " +
                    "the FROM clause at a server whose version is less than " +
                     QV2String + "\nserialVersion = " + serialVersion +
                    " expected version = " + QUERY_VERSION_2);
            }

            serializeIter(theFromIters[0], out, serialVersion);
            SerializationUtil.writeString(out, theFromVarNames[0]);
        } else {
            serializeIters(theFromIters, out, serialVersion);
            for (int i = 0; i < theFromIters.length; ++i) {
                SerializationUtil.writeString(out, theFromVarNames[i]);
            }
        }
        serializeIter(theWhereIter, out, serialVersion);
        serializeIter(theOffsetIter, out, serialVersion);
        serializeIter(theLimitIter, out, serialVersion);
    }

    @Override
    public PlanIterKind getKind() {
        return PlanIterKind.SFW;
    }

    @Override
    public int[] getTupleRegs() {
        return theTupleRegs;
    }

    public int getNumColumns() {
        return theColumnNames.length;
    }

    public String getColumnName(int i) {
        return theColumnNames[i];
    }

    @Override
    public void open(RuntimeControlBlock rcb) {

        rcb.setState(theStatePos, new SFWIterState());

        if (theTupleRegs != null) {

            TupleValue tuple = new TupleValue((RecordDefImpl)theTypeDefinition,
                                              rcb.getRegisters(),
                                              theTupleRegs);

            rcb.setRegVal(theResultReg, tuple);
        }

        for (int i = 0; i < theFromIters.length; ++i) {
            theFromIters[i].open(rcb);
        }

        if (theWhereIter != null) {
            theWhereIter.open(rcb);
        }

        for (PlanIter columnIter : theColumnIters) {
            columnIter.open(rcb);
        }

        computeOffsetLimit(rcb);
    }

    @Override
    public boolean next(RuntimeControlBlock rcb) {

        SFWIterState state = (SFWIterState)rcb.getState(theStatePos);

        if (state.isDone()) {
            return false;
        }

        if (state.theNumResults >= state.theLimit) {
            state.done();
            theFromIters[0].reset(rcb);
            return false;
        }

        while (true) {

            if (theWhereIter != null) {

                boolean whereValue = true;

                do {
                    if (!getNextFROMTuple(rcb, state)) {
                        return false;
                    }

                    boolean more = theWhereIter.next(rcb);

                    if (!more) {
                        whereValue = false;
                    } else {
                        FieldValueImpl val =
                            rcb.getRegVal(theWhereIter.getResultReg());

                        whereValue = (val.isNull() ? false : val.getBoolean());
                    }

                    theWhereIter.reset(rcb);

                } while (whereValue == false);

            } else {
                if (!getNextFROMTuple(rcb, state)) {
                    return false;
                }
            }

            if (state.theOffset == 0) {
                break;
            }

            --state.theOffset;
        }

        ++state.theNumResults;

        if (theTupleRegs == null) {

            boolean more = theColumnIters[0].next(rcb);

            if (!more) {
                rcb.setRegVal(theResultReg, NullValueImpl.getInstance());
            }

            /*
             * this.theResultReg is the same as theColumnIters[0].theResultReg,
             * so no need to set this.theResultReg.
             */

            theColumnIters[0].reset(rcb);
            return true;
        }

        for (int i = 0; i < theColumnIters.length; ++i) {

            PlanIter columnIter = theColumnIters[i];
            boolean more = columnIter.next(rcb);

            if (!more) {
                rcb.setRegVal(theTupleRegs[i], NullValueImpl.getInstance());

            } else {
                /*
                 * theTupleRegs[i] is the same as theColumnIters[i].theResultReg,
                 * so no need to set theTupleRegs[i], unless the value stored
                 * there is another TupleValue, in which case we convert it to
                 * a record so that we don't have to deal with nested
                 * TupleValues.
                 */
                FieldValueImpl value = rcb.getRegVal(columnIter.getResultReg());
                 if (value.isTuple()) {
                    value = ((TupleValue)value).toRecord();
                    rcb.setRegVal(theTupleRegs[i], value);
                 }
            }

            /* the column iterators need to be reset for the next call to next */
            columnIter.reset(rcb);
        }

        return true;
    }

    boolean getNextFROMTuple(RuntimeControlBlock rcb, SFWIterState state) {

        while (0 <= state.theNumBoundVars &&
               state.theNumBoundVars < theFromIters.length) {

            if (!theFromIters[state.theNumBoundVars].next(rcb)) {
                theFromIters[state.theNumBoundVars].reset(rcb);
                --state.theNumBoundVars;
            } else {
                ++state.theNumBoundVars;
            }
        }

        if (state.theNumBoundVars < 0) {
            state.done();
            return false;
        }

        --state.theNumBoundVars;
        return true;
    }

    @Override
    public void reset(RuntimeControlBlock rcb) {

        for (int i = 0; i < theFromIters.length; ++i) {
            theFromIters[i].reset(rcb);
        }

        if (theWhereIter != null) {
            theWhereIter.reset(rcb);
        }

        for (PlanIter columnIter : theColumnIters) {
            columnIter.reset(rcb);
        }

        if (theOffsetIter != null) {
            theOffsetIter.reset(rcb);
        }

        if (theLimitIter != null) {
            theLimitIter.reset(rcb);
        }

        SFWIterState state = (SFWIterState)rcb.getState(theStatePos);
        state.reset(this);

        computeOffsetLimit(rcb);
    }

    @Override
    public void close(RuntimeControlBlock rcb) {

        SFWIterState state = (SFWIterState)rcb.getState(theStatePos);
        if (state == null) {
            return;
        }

        for (int i = 0; i < theFromIters.length; ++i) {
            theFromIters[i].close(rcb);
        }

        if (theWhereIter != null) {
            theWhereIter.close(rcb);
        }

        for (PlanIter columnIter : theColumnIters) {
            columnIter.close(rcb);
        }

        if (theOffsetIter != null) {
            theOffsetIter.close(rcb);
        }

        if (theLimitIter != null) {
            theLimitIter.close(rcb);
        }

        state.close();
    }

    private void computeOffsetLimit(RuntimeControlBlock rcb) {

        SFWIterState state = (SFWIterState)rcb.getState(theStatePos);

        long offset = 0;
        long limit = -1;

        if (theOffsetIter != null) {
            theOffsetIter.open(rcb);
            theOffsetIter.next(rcb);
            FieldValueImpl val = rcb.getRegVal(theOffsetIter.getResultReg());
            offset = val.getLong();

            if (offset < 0) {
                throw new QueryException(
                   "Offset can not be a negative number",
                    theOffsetIter.theLocation);
            }

            if (offset > Integer.MAX_VALUE) {
                throw new QueryException(
                   "Offset can not be greater than Integer.MAX_VALUE",
                    theOffsetIter.theLocation);
            }
        }

        if (theLimitIter != null) {
            theLimitIter.open(rcb);
            theLimitIter.next(rcb);
            FieldValueImpl val = rcb.getRegVal(theLimitIter.getResultReg());
            limit = val.getLong();

            if (limit < 0) {
                throw new QueryException(
                    "Limit can not be a negative number",
                    theLimitIter.theLocation);
            }

            if (limit > Integer.MAX_VALUE) {
                throw new QueryException(
                   "Limit can not be greater than Integer.MAX_VALUE",
                    theOffsetIter.theLocation);
            }
        }

        long numResultsComputed = rcb.getNumResultsComputed();

        if (limit < 0) {
            limit = Long.MAX_VALUE;
        } else {
            limit -= numResultsComputed;
        }

        if (numResultsComputed > 0) {
            offset = 0;
        }

        state.theOffset = offset;
        state.theLimit = limit;
    }

    @Override
    protected void displayContent(StringBuilder sb, QueryFormatter formatter) {

        for (int i = 0; i < theFromIters.length; ++i) {
            formatter.indent(sb);
            sb.append("FROM:\n");
            theFromIters[i].display(sb, formatter);
            sb.append(" as " +  theFromVarNames[i] + "\n\n");
        }

        if (theWhereIter != null) {
            formatter.indent(sb);
            sb.append("WHERE:\n");
            theWhereIter.display(sb, formatter);
            sb.append("\n\n");
        }

        formatter.indent(sb);
        sb.append("SELECT:\n");

        for (int i = 0; i < theColumnIters.length; ++i) {
            theColumnIters[i].display(sb, formatter);
            if (i < theColumnIters.length - 1) {
                sb.append(",\n");
            }
        }

        if (theOffsetIter != null) {
            sb.append("\n\n");
            formatter.indent(sb);
            sb.append("OFFSET:\n");
            theOffsetIter.display(sb, formatter);
        }

        if (theLimitIter != null) {
            sb.append("\n\n");
            formatter.indent(sb);
            sb.append("LIMIT:\n");
            theLimitIter.display(sb, formatter);
        }
    }
}
