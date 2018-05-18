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

package oracle.kv.impl.query.runtime.server;

import java.io.DataOutput;

import com.sleepycat.je.Transaction;

import oracle.kv.Direction;
import oracle.kv.impl.api.ops.OperationHandler;
import oracle.kv.impl.api.table.FieldValueImpl;
import oracle.kv.impl.api.table.RecordDefImpl;
import oracle.kv.impl.api.table.RecordValueImpl;
import oracle.kv.impl.api.table.RowImpl;
import oracle.kv.impl.api.table.TableImpl;
import oracle.kv.impl.api.table.TupleValue;
import oracle.kv.impl.query.QueryStateException;
import oracle.kv.impl.query.QueryException.Location;
import oracle.kv.impl.query.compiler.QueryFormatter;
import oracle.kv.impl.query.runtime.BaseTableIter;
import oracle.kv.impl.query.runtime.PlanIter;
import oracle.kv.impl.query.runtime.RuntimeControlBlock;
import oracle.kv.impl.query.runtime.TableIterFactory;
import oracle.kv.impl.topo.PartitionId;
import oracle.kv.table.FieldRange;
import oracle.kv.table.Row;

/**
 * A truncated version of PlanIter that is used at runtime only, to
 * execute table/index iteration in the server context. It is created
 * by BaseTableIter during its open() call. The BaseTableIter serves as
 * a proxy for this implementation. In the client context BaseTableIter
 * creates and uses ClientTableIter instead.
 *
 * This class exists so that it can include code that uses server-only
 * classes that are not available on the client side.
 */
public class ServerTableIter extends BaseTableIter {

    private static class ServerTableIterState extends BaseTableIterState {

        private final TableScannerFactory theFactory;

        private TableScannerFactory.TableScanner theScanner;

        ServerTableIterState(
            TableScannerFactory factory,
            RuntimeControlBlock rcb,
            ServerTableIter iter) {

            super(rcb, iter);

            this.theFactory = factory;
        }

        private TableScannerFactory.TableScanner getScanner(
            boolean eliminateDups) {

            if (theScanner == null && !theAlwaysFalse) {
                /*
                 * This call will return an index scanner if secKey is not null,
                 * otherwise it returns a primary key scanner.
                 */
                theScanner = theFactory.getTableScanner(
                    theRCB,
                    theDirection,
                    thePrimaryKey,
                    theSecondaryKey,
                    theRange,
                    eliminateDups);
            }
            return theScanner;
        }

        @Override
        protected void reset(PlanIter iter) {
            super.reset(iter);
            if (theScanner != null) {
                theScanner.close();
                theScanner = null;
            }
        }

        @Override
        protected void close() {
            super.close();
            if (theScanner != null) {
                theScanner.close();
                theScanner = null;
            }
        }
    }

    private final TableScannerFactory theFactory;

    private transient TableImpl theTable;

    private ServerTableIter(
        RuntimeControlBlock rcb,
        TableScannerFactory factory,
        Location loc,
        int statePos,
        int resultReg,
        int[] tupleRegs,
        String tableName,
        String indexName,
        RecordDefImpl typeDef,
        Direction dir,
        RecordValueImpl primKey,
        RecordValueImpl secKey,
        FieldRange range,
        PlanIter filterIter,
        boolean usesCoveringIndex,
        boolean eliminateIndexDups,
        PlanIter[] pushedExternals) {

        super(loc, statePos, resultReg, tupleRegs,
              tableName, indexName, typeDef, dir,
              primKey, secKey, range, filterIter,
              usesCoveringIndex, eliminateIndexDups,
              pushedExternals);

        theFactory = factory;

        theTable = rcb.getTableMetadata().getTable(theTableName);
        if (theTable == null) {
            throw new IllegalArgumentException(
                "Table does not exist: " + theTableName);
        }
    }

    @Override
    public void writeFastExternal(DataOutput out, short serialVersion) {
        throwCannotCall("writeFastExternal");
    }

    @Override
    protected TableImpl getTable() {
        return theTable;
    }

    @Override
    public int[] getTupleRegs() {
        throwCannotCall("getTupleRegs");
        return null;
    }

    @Override
    public void open(RuntimeControlBlock rcb) {

        TupleValue tuple = new TupleValue(
            theTypeDef, rcb.getRegisters(), theTupleRegs);

        rcb.setRegVal(theResultReg, tuple);

        ServerTableIterState state = new ServerTableIterState(theFactory,
                                                              rcb,
                                                              this);
        rcb.setState(theStatePos, state);

        if (theFilterIter != null) {
            theFilterIter.open(rcb);
        }

        if (state.isAlwaysFalse()) {
            state.done();
        }
    }

    @Override
    public boolean next(RuntimeControlBlock rcb) {

        ServerTableIterState state =
            (ServerTableIterState) rcb.getState(theStatePos);

        if (state.isDone()) {
            return false;
        }

        assert(theTable != null);

        TableScannerFactory.TableScanner scanner =
            state.getScanner(theEliminateIndexDups);

        Row row;

        /*
         * If there is a filter iterator, get the key data, filter, and only
         * fetch data if there's a match. If there is no filter, fetch the
         * row directly.
         */
        if (theFilterIter != null || theUsesCoveringIndex) {

            row = scanner.nextKey();

            while (row != null) {

                /*
                 * Populate the registers and call the filter. This will
                 * sparsely populate, using only key fields.
                 */
                for (int i = 0; i < ((RowImpl)row).getNumFields(); ++i) {
                    rcb.setRegVal(theTupleRegs[i], (FieldValueImpl)row.get(i));
                }

                boolean match = true;

                if (theFilterIter != null) {

                    match = theFilterIter.next(rcb);

                    if (match) {
                        FieldValueImpl val = rcb.getRegVal(
                            theFilterIter.getResultReg());

                        match = (val.isNull() ? false : val.getBoolean());
                    }

                    theFilterIter.reset(rcb);
                }

                if (match) {

                    if (theUsesCoveringIndex) {
                        if (scanner.lockRow()) {
                            return true;
                        }
                    } else {
                        row = scanner.currentRow();

                        /*
                         * If we got a full row, populate the tuple registers
                         * with the full row below
                         */
                        if (row != null) {
                            break;
                        }
                    }
                }

                /*
                 * No match, or data fetch failed because the record
                 * disappeared from under us. Keep looping.
                 */
                row = scanner.nextKey();
            }

        } else {
            row = scanner.nextRow();
        }

        if (row != null) {
            for (int i = 0; i < ((RowImpl)row).getNumFields(); ++i) {
                rcb.setRegVal(theTupleRegs[i], (FieldValueImpl)row.get(i));
            }
            return true;
        }

        state.done();
        return false;
    }

    @Override
    public void reset(RuntimeControlBlock rcb) {

        ServerTableIterState state =
            (ServerTableIterState)rcb.getState(theStatePos);

        state.reset(this);
        if (theFilterIter != null) {
            theFilterIter.reset(rcb);
        }
    }

    @Override
    public void close(RuntimeControlBlock rcb) {

        ServerTableIterState state =
            (ServerTableIterState)rcb.getState(theStatePos);

        if (state == null) {
            return;
        }

        state.close();
        if (theFilterIter != null) {
            theFilterIter.close(rcb);
        }
    }

    @Override
    protected void displayContent(StringBuilder sb, QueryFormatter formatter) {
        throwCannotCall("displayContent");
    }

    private void throwCannotCall(String method) {
        throw new QueryStateException(
            "ServerTableIter: " + method + " cannot be called");
    }

    /**
     * An instance of ServerIterFactory is created during
     * TableQueryHandler.execute() and is stored in the RCB.
     */
    public static class ServerTableIterFactory implements TableIterFactory {

        private final TableScannerFactory theFactory;

        public ServerTableIterFactory(
            Transaction txn,
            PartitionId partitionId,
            OperationHandler operationHandler) {

            this.theFactory =
                new TableScannerFactory(txn, partitionId, operationHandler);
        }

        @Override
        public PlanIter createTableIter(
            RuntimeControlBlock rcb,
            Location loc,
            int statePos,
            int resultReg,
            int[] tupleRegs,
            String tableName,
            String indexName,
            RecordDefImpl typeDefinition,
            Direction dir,
            RecordValueImpl primKeyRecord,
            RecordValueImpl secKeyRecord,
            FieldRange range,
            PlanIter filterIter,
            boolean usesCoveringIndex,
            boolean eliminateIndexDups,
            PlanIter[] pushedExternals) {

            return new ServerTableIter(
                rcb,
                theFactory,
                loc,
                statePos,
                resultReg,
                tupleRegs,
                tableName,
                indexName,
                typeDefinition,
                dir,
                primKeyRecord,
                secKeyRecord,
                range,
                filterIter,
                usesCoveringIndex,
                eliminateIndexDups,
                pushedExternals);
        }
    }
}
