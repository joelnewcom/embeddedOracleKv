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

import static oracle.kv.impl.query.QueryException.Location;

import java.io.DataOutput;

import oracle.kv.Direction;

import oracle.kv.impl.api.table.RecordValueImpl;
import oracle.kv.impl.api.table.RowImpl;
import oracle.kv.impl.api.table.RecordDefImpl;
import oracle.kv.impl.api.table.TableImpl;
import oracle.kv.impl.api.table.TupleValue;

import oracle.kv.impl.query.QueryStateException;
import oracle.kv.impl.query.compiler.QueryFormatter;
import oracle.kv.query.ExecuteOptions;
import oracle.kv.table.FieldRange;
import oracle.kv.table.IndexKey;
import oracle.kv.table.MultiRowOptions;
import oracle.kv.table.PrimaryKey;
import oracle.kv.table.Row;
import oracle.kv.table.TableAPI;
import oracle.kv.table.TableIterator;
import oracle.kv.table.TableIteratorOptions;

/**
 * The iterator that does the work of a BaseTableIter at the client side.
 * It is a truncated version of PlanIter that is used at runtime only, to
 * execute table/index iteration in the client context. It is created
 * by BaseTableIter during its open() call. The BaseTableIter serves as
 * a proxy for this implementation. In the server context BaseTableIter
 * creates and uses ServerTableIter instead.
 *
 * This separation of client and server contexts exists to allow
 * server-only code to exist only in the server implementation, but to keep
 * the implementation of BaseTableIter the same regardless of its context.
 */
public class ClientTableIter extends BaseTableIter {

    private static class ClientTableIterState extends BaseTableIterState {

        private final TableAPI theTableAPI;

        private TableIterator<Row> theIterator;

        ClientTableIterState(
            TableAPI tableAPI,
            RuntimeControlBlock rcb,
            ClientTableIter iter) {

            super(rcb, iter);

            theTableAPI = tableAPI;
        }

        private TableIterator<Row> ensureIterator() {

            if (theIterator == null) {
                if (theSecondaryKey != null) {
                    theIterator = indexIterator(theTableAPI,
                                                theRCB,
                                                theDirection,
                                                theSecondaryKey,
                                                theRange);
                } else {
                    theIterator = tableIterator(theTableAPI,
                                                theRCB,
                                                theDirection,
                                                thePrimaryKey,
                                                theRange);
                }
            }
            return theIterator;
        }

        @Override
        protected void reset(PlanIter iter) {
            super.reset(iter);
            if (theIterator != null) {
                theIterator.close();
                theIterator = null;
            }
        }

        @Override
        protected void close() {
            super.close();
            if (theIterator != null) {
                theIterator.close();
                theIterator = null;
            }
        }
    }

    private final TableAPI theTableAPI;

    private final TableImpl theTable;

    private ClientTableIter(
        RuntimeControlBlock rcb,
        TableAPI tableAPI,
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

        theTableAPI = tableAPI;

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

        rcb.setState(theStatePos,
                     new ClientTableIterState(theTableAPI, rcb, this));

        if (theFilterIter != null) {
            theFilterIter.open(rcb);
        }
    }

    @Override
    public boolean next(RuntimeControlBlock rcb) {

        ClientTableIterState state =
            (ClientTableIterState)rcb.getState(theStatePos);

        if (state.isDone()) {
            return false;
        }

        assert(theTable != null);

        TableIterator<Row> iterator = state.ensureIterator();
        TupleValue tuple = (TupleValue)rcb.getRegVal(theResultReg);

        while (iterator.hasNext()) {

            Row row = iterator.next();
            tuple.toTuple((RowImpl)row);

            if (theFilterIter != null) {
                boolean match = theFilterIter.next(rcb);
                theFilterIter.reset(rcb);
                if (!match) {
                    continue;
                }
            }
            return true;
        }
        state.done();
        return false;
    }

    @Override
    public void reset(RuntimeControlBlock rcb) {

        ClientTableIterState state =
            (ClientTableIterState)rcb.getState(theStatePos);

        state.reset(this);
        if (theFilterIter != null) {
            theFilterIter.reset(rcb);
        }
    }

    @Override
    public void close(RuntimeControlBlock rcb) {

        ClientTableIterState state =
            (ClientTableIterState)rcb.getState(theStatePos);
        if (state == null) {
            return;
        }

        state.close();
        if (theFilterIter != null) {
            theFilterIter.close(rcb);
        }
    }

    @Override
    protected void display(StringBuilder sb, QueryFormatter formatter) {
        throwCannotCall("display");
    }

    @Override
    protected void displayContent(StringBuilder sb, QueryFormatter formatter) {
        throwCannotCall("displayContent");
    }

    private void throwCannotCall(String method) {
        throw new QueryStateException(
            "ClientTableIter: " + method + " cannot be called");
    }


    /* Utility methods */

    static private TableIterator<Row> tableIterator(
        TableAPI tableAPI,
        RuntimeControlBlock rcb,
        Direction dir,
        PrimaryKey key,
        FieldRange range) {

        MultiRowOptions mro = (range != null ?
                               range.createMultiRowOptions() : null);

        ExecuteOptions options = rcb.getExecuteOptions();

        TableIteratorOptions tio =
            (options != null ?
             options.createTableIteratorOptions(dir) :
             new TableIteratorOptions(dir, null, 0, null));

        return tableAPI.tableIterator(key, mro, tio);
    }

    static private TableIterator<Row> indexIterator(
        TableAPI tableAPI,
        RuntimeControlBlock rcb,
        Direction dir,
        IndexKey indexKey,
        FieldRange range) {

        MultiRowOptions mro =
            (range != null ?
             range.createMultiRowOptions() : null);

        ExecuteOptions options = rcb.getExecuteOptions();

        TableIteratorOptions tio =
            (options != null ?
             options.createTableIteratorOptions(dir) :
             new TableIteratorOptions(dir, null, 0, null));

        return tableAPI.tableIterator(indexKey, mro, tio);
    }

    /**
     * Client-side implementation of TableIterFactory interface
     */
    public static class ClientTableIterFactory implements TableIterFactory {

        private final TableAPI theTableAPI;

        public ClientTableIterFactory(TableAPI tableAPI) {
            theTableAPI = tableAPI;
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
            Direction direction,
            RecordValueImpl primKeyRecord,
            RecordValueImpl secKeyRecord,
            FieldRange range,
            PlanIter filterIter,
            boolean usesCoveringIndex,
            boolean eliminateIndexDups,
            PlanIter[] pushedExternals) {

            return new ClientTableIter(
                rcb,
                theTableAPI,
                loc,
                statePos,
                resultReg,
                tupleRegs,
                tableName,
                indexName,
                typeDefinition,
                direction,
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
