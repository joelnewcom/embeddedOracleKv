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

import static oracle.kv.impl.api.ops.OperationHandler.CURSOR_READ_COMMITTED;

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.Transaction;

import java.util.HashSet;

import oracle.kv.Direction;
import oracle.kv.Value;
import oracle.kv.ValueVersion;
import oracle.kv.impl.api.ops.IndexKeysIterate;
import oracle.kv.impl.api.ops.IndexKeysIterateHandler;
import oracle.kv.impl.api.ops.IndexScanner;
import oracle.kv.impl.api.ops.InternalOperation.OpCode;
import oracle.kv.impl.api.ops.MultiGetTableKeys;
import oracle.kv.impl.api.ops.MultiGetTableKeysHandler;
import oracle.kv.impl.api.ops.MultiTableOperationHandler;
import oracle.kv.impl.api.ops.MultiTableOperationHandler.OperationTableInfo;
import oracle.kv.impl.api.ops.OperationHandler;
import oracle.kv.impl.api.ops.Scanner;
import oracle.kv.impl.api.table.BinaryValueImpl;
import oracle.kv.impl.api.table.FieldDefImpl;
import oracle.kv.impl.api.table.IndexImpl;
import oracle.kv.impl.api.table.IndexKeyImpl;
import oracle.kv.impl.api.table.IndexRange;
import oracle.kv.impl.api.table.RowImpl;
import oracle.kv.impl.api.table.TableAPIImpl;
import oracle.kv.impl.api.table.TableImpl;
import oracle.kv.impl.api.table.TableKey;
import oracle.kv.impl.api.table.TargetTables;
import oracle.kv.impl.query.QueryStateException;
import oracle.kv.impl.query.runtime.RuntimeControlBlock;
import oracle.kv.impl.topo.PartitionId;
import oracle.kv.table.FieldRange;
import oracle.kv.table.IndexKey;
import oracle.kv.table.PrimaryKey;
import oracle.kv.table.Row;

/**
 * This class encapsulates 2-part table scanning performed from a query
 * operation. It allows filtering based on keys to be done before fetching
 * data associated with a query. An instance of this class is created before
 * the server-side query operation starts. When a table iterator is
 * encountered in the query it creates an instance of TableScanner and
 * then does one of several things:
 *  1. Calls Row nextKey() to get the "next" key, which returns only key
 *   fields in the Row.
 *    a. filters based on the key fields and then, if necessary.
 *    b. calls Row fetchData() to get the data
 * 2. Calls Row nextRow() if key-based filtering is not relevant.
 *
 * Because partially populated Row instances are returned this pattern works for
 * index scans as well as primary key scans.
 *
 * TableScanner instances must be closed in order to release resources.
 */
public class TableScannerFactory {

    final private Transaction txn;
    final private PartitionId pid;
    private final MultiGetTableKeysHandler primaryOpHandler;
    private final IndexKeysIterateHandler secondaryOpHandler;

    /*
     * The interface for scanners returned by this factory.
     */
    public interface TableScanner {

        /**
         * Returns the next key, deserialized, or null if there is no next key.
         * In this case the record may not be locked. If the row is to be used
         * as a result either lockKey() or fetchData() must be called to lock
         * the record. If either of those fails it means that the row was
         * removed out from under this operation, which is fine.
         */
        public Row nextKey();

        /**
         * Returns the next row, deserialized, or null if there is no next row.
         * The data is locked.
         */
        public Row nextRow();

        /**
         * Locks the current row. This interface may only be called after
         * nextKey() has returned non-null.  The intial scan may have been done
         * using READ_UNCOMMITTED, which is why this call, or currentRow(), is
         * necessary to lock the record.
         * @return true if the record is locked, false if it cannot be locked,
         * which means that the record is no longer present.
         */
        public boolean lockRow();

        /**
         * Locks and returns the full row associated with the "current" key.
         * This interface may only be called after nextKey() has returned
         * non-null.
         * @return the complete row or null if the row has been removed.
         */
        public Row currentRow();

        /**
         * Closes the scanner. This must be called to avoid resource leaks.
         */
        public void close();
    }

    public TableScannerFactory(
        final Transaction txn,
        final PartitionId pid,
        final OperationHandler oh) {

        this.txn = txn;
        this.pid = pid;
        primaryOpHandler = (MultiGetTableKeysHandler)
            oh.getHandler(OpCode.MULTI_GET_TABLE_KEYS);
        secondaryOpHandler = (IndexKeysIterateHandler)
            oh.getHandler(OpCode.INDEX_KEYS_ITERATE);
    }

    /*
     * Returns a TableScanner. This is an index scanner if indexKey is not null,
     * otherwise it is a primary key scanner. In both cases the object must be
     * closed to avoid leaking resources and/or leaving records locked.
     */
    public TableScanner getTableScanner(
        RuntimeControlBlock rcb,
        Direction dir,
        PrimaryKey primaryKey,
        IndexKey indexKey,
        FieldRange range,
        boolean eliminateDups) {

        assert primaryKey == null || indexKey == null;

        if (indexKey != null) {
            return getSecondaryTableScanner(rcb, dir, indexKey, range,
                                            eliminateDups);
        }
        return getPrimaryTableScanner(rcb, dir, primaryKey, range);
    }

    /*
     * Methods associated with a primary key scanner
     */
    private TableScanner getPrimaryTableScanner(
        RuntimeControlBlock rcb,
        Direction dir,
        PrimaryKey key,
        FieldRange range) {

        /*
         * Create a MultiTableOperation for a single target table
         */
        TableImpl table = (TableImpl) key.getTable();

        TableKey tableKey = TableKey.createKey(table, key, true);
        assert tableKey != null;

        final TargetTables targetTables = new TargetTables(table, null, null);

        final MultiGetTableKeys op =
            new MultiGetTableKeys(tableKey.getKeyBytes(),
                                  targetTables,
                                  (range != null ?
                                   TableAPIImpl.createKeyRange(range) :
                                   null));
        primaryOpHandler.verifyTableAccess(op);

        /*
         * Create a key-only scanner using dirty reads. This means that in order
         * to use the record, it must be locked, and if the data is required, it
         * must be fetched.
         */
        final OperationTableInfo tableInfo = new OperationTableInfo();
        Scanner scanner = primaryOpHandler.getScanner(
            op,
            tableInfo,
            txn,
            pid,
            tableKey.getMajorKeyComplete(),
            dir,
            rcb.getPrimaryResumeKey(),
            CURSOR_READ_COMMITTED,
            LockMode.READ_UNCOMMITTED_ALL,
            true); /* use a key-only scanner; fetch data in the "next" call */

        return new PrimaryTableScanner(op,
                                       tableInfo,
                                       scanner,
                                       (TableImpl)key.getTable(),
                                       rcb);
    }

    /*
     * Methods associated with a secondary key scanner
     */
    private TableScanner getSecondaryTableScanner(
        RuntimeControlBlock rcb,
        Direction dir,
        IndexKey key,
        FieldRange range,
        boolean eliminateDups) {

        IndexImpl index = (IndexImpl) key.getIndex();
        TableImpl table = (TableImpl) index.getTable();

        /*
         * Create an IndexOperation for a single target table
         */
        IndexRange indexRange = new IndexRange((IndexKeyImpl)key, range, dir);

        final TargetTables targetTables = new TargetTables(table, null, null);

        final IndexKeysIterate op =
            new IndexKeysIterate(index.getName(),
                                 targetTables,
                                 indexRange,
                                 rcb.getSecondaryResumeKey(),
                                 rcb.getPrimaryResumeKey(),
                                 0 /* batch size not needed */);

        secondaryOpHandler.verifyTableAccess(op);

        /*
         * Create a key-only scanner using dirty reads. This means that in order
         * to use the record, it must be locked, and if the data is required, it
         * must be fetched.
         */
        IndexScanner scanner =
            secondaryOpHandler.getIndexScanner(
                op,
                txn,
                OperationHandler.CURSOR_READ_COMMITTED,
                LockMode.READ_UNCOMMITTED_ALL,
                true);

        return new SecondaryTableScanner(rcb,
                                         (TableImpl) index.getTable(),
                                         index,
                                         scanner,
                                         eliminateDups);
    }

    /*
     * The underlying Scanner used by PrimaryTableScanner uses
     * DIRTY_READ_ALL lockmode and does a key-only scan.
     *
     * theRow:
     * The table Row that stores the record pointed to by the current 
     * primary-index entry (the one that the scanner is positioned on).
     * Initially, this row is sparsely populated: tt contains only the
     * prim key columns from the current index entry. The row is filled in
     * completely only if the currentRow() method is called.
     *
     * theDataEntry:
     * A DataEntry used to retrieve the data portion of the record pointed to
     * by the current index entry.
     */
    private class PrimaryTableScanner implements TableScanner {

        final MultiGetTableKeys op;

        final OperationTableInfo tableInfo;

        final TableImpl table;

        final RuntimeControlBlock rcb;

        final Scanner scanner;

        boolean theMoreElements;

        RowImpl theRow;

        final DatabaseEntry theDataEntry;

        PrimaryTableScanner(
            MultiGetTableKeys op,
            OperationTableInfo tableInfo,
            Scanner scanner,
            TableImpl table,
            RuntimeControlBlock rcb) {

            this.op = op;
            this.tableInfo = tableInfo;
            this.scanner = scanner;
            this.table = table;
            this.rcb = rcb;
            theMoreElements = true;
            theDataEntry = new DatabaseEntry();
        }

        @Override
        public void close() {
            scanner.close();
        }

        @Override
        public Row nextKey() {
            getNextKey();
            return theRow;
        }

        @Override
        public boolean lockRow() {
            return scanner.getCurrent();
        }

        @Override
        public Row nextRow() {

            getNextKey();

            while (theRow != null) {
                Row row = currentRow();
                if (row != null) {
                    return row;
                }
                getNextKey();
            }
            return null;
        }

        /**
         * Fetches the data for the Row.
         */
        @Override
        public Row currentRow() {

            if (!scanner.getLockedData(theDataEntry)) {
                return null;
            }

            byte[] data = theDataEntry.getData();

            if (!MultiTableOperationHandler.isTableData(data, null)) {
                return null;
            }

            if (data == null || data.length <= 1) {
                /* a key-only table, no data to fetch */
                return theRow;
            }

            Value.Format format = Value.Format.fromFirstByte(data[0]);

            if (table.initRowFromByteValue(theRow, data, format, 1)) {
                return theRow;
            }

            return null;
        }

        private void getNextKey() {

            while (theMoreElements && scanner.next()) {
                createKey();
                if (theRow != null) {
                    theMoreElements = true;
                    return;
                }
            }

            rcb.setPrimaryResumeKey(null);
            theRow = null;
            theMoreElements = false;
        }

        private void createKey() {

            int match = primaryOpHandler.keyInTargetTable(op,
                                                          tableInfo,
                                                          scanner.getKey(),
                                                          scanner.getData(),
                                                          scanner.getCursor());
            if (match <= 0) {
                if (match < 0) {
                    theMoreElements = false;
                }
                theRow = null;
                return;
            }

            /*
             * Create the row from key bytes
             */
            byte[] primKey = scanner.getKey().getData();
            rcb.setPrimaryResumeKey(primKey);
            theRow = table.createRowFromKeyBytes(primKey);
        }
    }

    /**
     * The underlying IndexScanner used by SecondaryTableScanner uses
     * DIRTY_READ_ALL lockmode and does a key-only scan.
     *
     * theRow:
     * The table Row that stores the record pointed to by the current index
     * entry (the one that the scanner is positioned on). Initially, this
     * row is sparsely populated: tt contains only the index fields (including
     * the pk fields) of the current index entry. The row is filled in
     * completely only if the currentRow() method is called.
     *
     * theDataEntry:
     * A DataEntry used to retrieve the data portion of the record pointed to
     * by the current index entry.
     */
    private class SecondaryTableScanner implements TableScanner {

        final RuntimeControlBlock rcb;

        final TableImpl table;

        final IndexImpl index;

        final IndexScanner scanner;

        final boolean theEliminateDups;

        boolean theMoreElements;

        RowImpl theRow;

        final DatabaseEntry theDataEntry;

        final HashSet<BinaryValueImpl> thePrimKeysSet;

        SecondaryTableScanner(
            RuntimeControlBlock rcb,
            TableImpl table,
            IndexImpl index,
            IndexScanner scanner,
            boolean eliminateDups) {

            this.rcb = rcb;
            this.table = table;
            this.index = index;
            this.scanner = scanner;
            theEliminateDups = eliminateDups;
            theMoreElements = true;
            theDataEntry = new DatabaseEntry();

            if (eliminateDups) {
                thePrimKeysSet = new HashSet<BinaryValueImpl>(1000);
            } else {
                thePrimKeysSet = null;
            }
        }

        @Override
        public void close() {
            scanner.close();
            if (thePrimKeysSet != null) {
                thePrimKeysSet.clear();
            }
        }

        @Override
        public Row nextKey() {
            getNextKey();
            return theRow;
        }

        @Override
        public boolean lockRow() {
            return scanner.getCurrent();
        }

        @Override
        public Row nextRow() {

            getNextKey();

            while (theRow != null) {
                Row row = currentRow();
                if (row != null) {
                    return row;
                }
                getNextKey();
            }
            return null;
        }

        /**
         * Fetches the data for the Row.
         * fields.
         */
        @Override
        public Row currentRow() {

            if (!scanner.getLockedData(theDataEntry)) {
                return null;
            }

            byte[] data = theDataEntry.getData();

            if (data == null || data.length <= 1) {
                /* a key-only table, no data to fetch */
                throw new QueryStateException(
                    "currentRow() should never be called on a key-only " +
                    "table, because the index should be a covering one");
            }

            /* TBD: method to unpack Row directly from byte[] rather than
             * ValueVersion (see TableImpl).
             */
            ValueVersion vv = new ValueVersion(Value.fromByteArray(data), null);

            return (table.rowFromValueVersion(vv, theRow) ? theRow : null);
        }

        private void getNextKey() {

            if (!theMoreElements) {
                return;
            }

            while (scanner.next()) {
                createKey();
                if (theRow != null) {
                    theMoreElements = true;
                    return;
                }
            }

            rcb.setPrimaryResumeKey(null);
            rcb.setSecondaryResumeKey(null);
            theRow = null;
            theMoreElements = false;
        }

        private void createKey() {

            DatabaseEntry indexKeyEntry = scanner.getIndexKey();
            DatabaseEntry primaryKeyEntry = scanner.getPrimaryKey();
            assert(indexKeyEntry != null && primaryKeyEntry != null);

            byte[] primKeyBytes = primaryKeyEntry.getData();

            if (theEliminateDups) {
                BinaryValueImpl primKeyVal = 
                    FieldDefImpl.binaryDef.createBinary(primKeyBytes);

                boolean added = thePrimKeysSet.add(primKeyVal);
                if (!added) {
                    theRow = null;
                    return;
                }
            }

            rcb.setPrimaryResumeKey(primaryKeyEntry.getData());
            rcb.setSecondaryResumeKey(indexKeyEntry.getData());

            /* Create Row from primary key bytes */
            theRow = table.createRowFromKeyBytes(primKeyBytes);

            /* Add the index fields to the above row */
            index.rowFromIndexKey(indexKeyEntry.getData(), theRow);
        }
    }
}
