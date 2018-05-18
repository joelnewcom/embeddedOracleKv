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

package oracle.kv.impl.rep.table;

import static oracle.kv.impl.api.ops.OperationHandler.CURSOR_DEFAULT;
import static oracle.kv.impl.api.ops.OperationHandler.CURSOR_READ_COMMITTED;

import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.kv.Direction;
import oracle.kv.impl.api.ops.MultiDeleteTable;
import oracle.kv.impl.api.ops.MultiDeleteTableHandler;
import oracle.kv.impl.api.ops.OperationHandler;
import oracle.kv.impl.api.ops.Result;
import oracle.kv.impl.api.ops.Scanner;
import oracle.kv.impl.api.ops.TableIterate;
import oracle.kv.impl.api.ops.TableIterateHandler;
import oracle.kv.impl.api.table.TableKey;
import oracle.kv.impl.api.table.TargetTables;
import oracle.kv.impl.rep.RepNode;
import oracle.kv.impl.rep.table.SecondaryInfoMap.DeletedTableInfo;
import oracle.kv.impl.rep.table.SecondaryInfoMap.SecondaryInfo;
import oracle.kv.impl.topo.PartitionId;
import oracle.kv.impl.util.TxnUtil;
import oracle.kv.table.PrimaryKey;
import oracle.kv.table.Table;

import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockConflictException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.rep.InsufficientAcksException;
import com.sleepycat.je.rep.InsufficientReplicasException;
import com.sleepycat.je.rep.ReplicatedEnvironment;

/**
 * Base class for table maintenance threads. A maintenance thread only runs on
 * the master. Examples of maintenance activities are index population, or
 * removal of records after a partition is migrated from this shard.

 * Maintenance activities are triggered by operations like the creation of an
 * index or partition migration, but execute in an asynchronous way in relation
 * to the instigating action. For simplicity and cleanliness of implementation,
 * a degree of serialization is enforced so
 *
 * - only one maintenance thread runs at a time, per
 *   TableManager.checkMaintenanceThreads
 * - maintenance activity and partition migration cannot occur concurrently.
 */
public abstract class MaintenanceThread extends Thread {
    private static final int NUM_DB_OP_RETRIES = 100;

    /* DB operation delays, in ms */
    private static final long SHORT_RETRY_TIME = 500;

    private static final long LONG_RETRY_TIME = 1000;

    /* Number of records read from the primary during each populate call. */
    private static final int POPULATE_BATCH_SIZE = 100;

    /*
     * Number of records read from a secondary DB partition in a transaction
     * when cleaning after a partition migration.
     */
    private static final int CLEAN_BATCH_SIZE = 100;

    /*
     * Number of records deleted from the partition DB partition in a
     *  transaction during cleaning due to a removed table.
     */
    private static final int DELETE_BATCH_SIZE = 100;

    protected final TableManager tableManager;
    protected final RepNode repNode;
    protected final Logger logger;

    protected final ReplicatedEnvironment repEnv;

    protected volatile boolean stop = false;

    protected MaintenanceThread(String name,
                                TableManager tableManager,
                                RepNode repNode,
                                ReplicatedEnvironment repEnv,
                                Logger logger) {
        super(name);
        this.tableManager = tableManager;
        this.repNode = repNode;
        this.repEnv = repEnv;
        this.logger = logger;
    }

    @Override
    public void run() {
        logger.log(Level.INFO, "Starting {0}", this);

        /*
         * Wait here until all partition migration activity has stopped, or
         * this thread is stopped.
         */
        try {
            repNode.getMigrationManager().awaitIdle(this);
        } catch (InterruptedException ie) {
            /* Should not happen. */
            throw new IllegalStateException(ie);
        }

        DatabaseException de = null;
        long delay = 0;
        int retryCount = NUM_DB_OP_RETRIES;
        Database infoDb = null;
        while (!isStopped()) {
            try {
                infoDb = SecondaryInfoMap.openDb(repEnv);
                doWork(infoDb);
                stop = true;
                // TODO - need to separate this from doWork() in terms
                // of exception handling.
                tableManager.maintenanceThreadExit(repEnv, infoDb);
                return;
            } catch (InsufficientAcksException |
                     InsufficientReplicasException iae) {
                de = iae;
                delay = LONG_RETRY_TIME;
            } catch (LockConflictException lce) {
                de = lce;
                delay = SHORT_RETRY_TIME;
            } catch (RuntimeException re) {
                /*
                 * If bad env, or no longer master just log and exit (likely in
                 * shutdown, or master transfer).
                 */
                if (isStopped()) {
                    logger.log(Level.INFO, "{0} exiting after, {1}",
                               new Object[]{this, re});
                    return;
                }
                throw re;
            } finally {
                if (infoDb != null) {
                    TxnUtil.close(logger, infoDb, null);
                }
            }
            assert de != null;
            retrySleep(retryCount, delay, de);
            retryCount--;
        }
        logger.log(Level.FINE, "{0} stopped", this);
    }

    private void retrySleep(int count, long sleepTime, DatabaseException de) {
        /* If the retry count has expired, re-throw the last exception */
        if (count <= 0) {
            throw de;
        }

        logger.log(Level.INFO,
                   "DB op caused {0}, will retry in {1}ms, attempts left: {2}",
                   new Object[]{de, sleepTime, count});
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException ie) {
            /* Should not happen. */
            throw new IllegalStateException(ie);
        }
    }

    /**
     * Does the actual work of the thread.
     *
     * @param infoDb the opened secondary info database
     */
    abstract void doWork(Database infoDb);

    /**
     * Returns true if the operation is to be stopped, either by external
     * request or by the environment changing.
     *
     * @return true if the thread is to be stopped
     */
    public boolean isStopped() {
        return stop ||
            !repEnv.isValid() ||
            !repEnv.getState().isMaster();
    }

    /**
     * Stops the operation and waits for the thread to exit.
     */
    void waitForStop() {
        assert Thread.currentThread() != this;

        stop = true;

        try {
            join();
        } catch (InterruptedException ie) {
            /* Should not happen. */
            throw new IllegalStateException(ie);
        }
    }

    /**
     * Thread to populate new secondary DBs.
     */
    static class PopulateThread extends MaintenanceThread {

        PopulateThread(TableManager tableManager,
                       RepNode repNode,
                       ReplicatedEnvironment repEnv,
                       Logger logger) {
            super("KV secondary populator",
                  tableManager, repNode, repEnv, logger);
        }

        /*
         * Populate the secondary databases.
         */
        @Override
        protected void doWork(Database infoDb) {
            if (isStopped()) {
                return;
            }

            final OperationHandler oh =
                new OperationHandler(repNode, tableManager.getParams());
            final TableIterateHandlerInternal opHandler =
                new TableIterateHandlerInternal(oh);

            /* The working secondary. */
            String currentSecondary = null;

            while (!isStopped()) {
                Transaction txn = null;
                try {
                    txn = repEnv.beginTransaction(
                        null,
                        SecondaryInfoMap.SECONDARY_INFO_CONFIG);

                    final SecondaryInfoMap infoMap =
                        SecondaryInfoMap.fetch(infoDb, txn, LockMode.RMW);

                    if (currentSecondary == null) {
                        currentSecondary = infoMap.getNextSecondaryToPopulate();

                        /* If no more, we are finally done */
                        if (currentSecondary == null) {
                            logger.info("Completed populating secondary " +
                                        "database(s)");
                            return;
                        }
                        logger.log(Level.FINE,
                                   "Started populating {0}", currentSecondary);
                    }

                    final SecondaryInfo info =
                        infoMap.getSecondaryInfo(currentSecondary);
                    assert info != null;
                    assert info.needsPopulating();

                    if (info.getCurrentPartition() == null) {
                        for (PartitionId partition : repNode.getPartitions()) {
                            if (!info.isCompleted(partition)) {
                                info.setCurrentPartition(partition);
                                break;
                            }
                        }
                    }

                    final SecondaryDatabase db =
                        tableManager.getSecondaryDb(currentSecondary);

                    if (info.getCurrentPartition() == null) {
                        logger.log(Level.FINE, "Finished populating {0} {1}",
                                   new Object[]{currentSecondary, info});

                        assert db != null;
                        db.endIncrementalPopulation();

                        info.donePopulation();
                        infoMap.persist(infoDb, txn);
                        txn.commit();
                        txn = null;

                        currentSecondary = null;
                        continue;
                    }

                    if (db == null) {
                        logger.log(Level.WARNING,
                                   "Failed to populate {0}, secondary " +
                                   "database {1} is missing",
                                   new Object[]{info, currentSecondary});
                        return;
                    }
                    logger.log(Level.FINE, "Populating {0} {1}",
                               new Object[]{currentSecondary, info});

                    final String tableName =
                        TableManager.getTableName(db.getDatabaseName());
                    final Table table = tableManager.getTable(tableName);

                    if (table == null) {
                        logger.log(Level.WARNING,
                                   "Failed to populate {0}, missing table {1}",
                                   new Object[]{info, tableName});
                        return;
                    }

                    final boolean more = populate(table,
                                                  opHandler,
                                                  txn,
                                                  info.getCurrentPartition(),
                                                  info.getLastKey());
                    if (!more) {
                        logger.log(Level.FINE, "Finished partition for {0}",
                                   info);
                        info.completeCurrentPartition();
                    }
                    infoMap.persist(infoDb, txn);
                    txn.commit();
                    txn = null;
                } finally {
                    TxnUtil.abort(txn);
                }
            }
        }

        /**
         * Process records for the specified table from the specified partition.
         * If the bytes in lastKey is not null then use that to start the
         * iteration. Returns true if there are more records to read from the
         * partition. If true is returned the bytes in lastKey are set to the
         * key of the last record read.
         *
         * @param table the source table
         * @param opHandler the internal operation handler
         * @param txn current transaction
         * @param partitionId the source partition
         * @param lastKey a key to start the iteration on input, set to the
         *                last key read on output
         * @param db the secondary DB to populate
         * @return true if there are more records to process
         */
        private boolean populate(Table table,
                                 TableIterateHandlerInternal opHandler,
                                 Transaction txn,
                                 PartitionId partitionId,
                                 DatabaseEntry lastKey) {
            final byte[] resumeKey = lastKey.getData();

            final PrimaryKey pkey = table.createPrimaryKey();
            final TableKey tkey = TableKey.createKey(table, pkey, true);

            /* A single target table */
            final TargetTables targetTables =
                new TargetTables(table, null, null);

            /* Create and execute the iteration */
            final TableIterate op =
                new TableIterate(tkey.getKeyBytes(),
                                 targetTables,
                                 Direction.FORWARD,
                                 null,
                                 tkey.getMajorKeyComplete(),
                                 POPULATE_BATCH_SIZE,
                                 resumeKey);

            return opHandler.populate(op, txn, partitionId, lastKey);
        }
    }

    /*
     * Special internal handler which bypasses security checks.
     */
    private static class TableIterateHandlerInternal
            extends TableIterateHandler {
        TableIterateHandlerInternal(OperationHandler handler) {
            super(handler);
        }

        /*
         * Use CURSOR_DEFAULT so that primary records accessed by the iteration
         * remain locked during the transaction.
         */
        @Override
        protected CursorConfig getCursorConfig() {
            return CURSOR_DEFAULT;
        }

        /*
         * Overriding verifyTableAccess() will bypass keyspace and
         * security checks.
         */
        @Override
        public void verifyTableAccess(TableIterate op) {}

        private boolean populate(TableIterate op,
                                 Transaction txn,
                                 PartitionId partitionId,
                                 DatabaseEntry lastKey) {

            /*
             * Use READ_UNCOMMITTED_ALL and keyOnly to make it inexpensive to
             * skip records that don't match the table. Once they match, lock
             * them, and use them.
             *
             * TODO: can this loop be combined with the loop in TableIterate
             * to share code?
             */
            final OperationTableInfo tableInfo = new OperationTableInfo();
            Scanner scanner = getScanner(op,
                                         tableInfo,
                                         txn,
                                         partitionId,
                                         CURSOR_READ_COMMITTED,
                                         LockMode.READ_UNCOMMITTED_ALL,
                                         true); // key-only
            boolean moreElements = false;

            try {
                DatabaseEntry keyEntry = scanner.getKey();
                DatabaseEntry dataEntry = scanner.getData();

                /* this is used to do a full fetch of data when needed */
                DatabaseEntry dentry = new DatabaseEntry();
                Cursor cursor = scanner.getCursor();
                int numElements = 0;
                while ((moreElements = scanner.next()) == true) {

                    int match = keyInTargetTable(op,
                                                 tableInfo,
                                                 keyEntry,
                                                 dataEntry,
                                                 cursor);
                    if (match > 0) {

                        /*
                         * The iteration was done using READ_UNCOMMITTED_ALL
                         * and with the cursor set to getPartial().  It is
                         * necessary to call getLockedData() here to both lock
                         * the record and fetch the data. populateSecondaries()
                         * must be called with a locked record.
                         */
                        if (scanner.getLockedData(dentry)) {

                            if (!isTableData(dentry.getData(), null)) {
                                continue;
                            }

                            byte[] keyBytes = keyEntry.getData();
                            lastKey.setData(keyBytes); // sets the lastKey return
                            scanner.getDatabase().
                                populateSecondaries(txn, lastKey, dentry,
                                                    scanner.getExpirationTime(),
                                                    null);
                            ++numElements;
                        }
                    } else if (match < 0) {
                        moreElements = false;
                        break;
                    }
                    if (numElements >= POPULATE_BATCH_SIZE) {
                        break;
                    }
                }
            } finally {
                scanner.close();
            }
            return moreElements;
        }
    }

    /**
     * Thread to clean secondary databases. A secondary needs to be "cleaned"
     * when a partition has moved from this node. In this case secondary
     * records that are from primary records in the moved partition need to be
     * removed the secondary. Cleaning is done by reading each record in a
     * secondary DB and checking whether the primary key is from a missing
     * partition. If so, remove the secondary record. Cleaning needs to happen
     * on every secondary whenever a partition has migrated.
     */
    static class SecondaryCleanerThread extends MaintenanceThread {

        SecondaryCleanerThread(TableManager tableManager,
                               RepNode repNode,
                               ReplicatedEnvironment repEnv,
                               Logger logger) {
            super("KV secondary cleaner",
                  tableManager, repNode, repEnv, logger);
        }

        @Override
        protected void doWork(Database infoDb) {
            if (isStopped()) {
                return;
            }

            /*
             * The working secondary. Working on one secondary at a time is
             * an optimization in space and time. It reduces the calls to
             * getNextSecondaryToClean() which iterates over the indexes, and
             * makes it so that only one SecondaryInfo is keeping track of
             * the cleaned partitions.
             */
            String currentSecondary = null;

            while (!isStopped()) {
                Transaction txn = null;
                try {
                    txn = repEnv.beginTransaction(null,
                                                  SecondaryInfoMap.CLEANER_CONFIG);

                    final SecondaryInfoMap infoMap =
                        SecondaryInfoMap.fetch(infoDb, txn, LockMode.RMW);

                    if (currentSecondary == null) {
                        currentSecondary = infoMap.getNextSecondaryToClean();

                        if (currentSecondary != null) {
                            logger.log(Level.INFO, "Cleaning {0}",
                                       currentSecondary);
                        }
                    }

                    /* If no more, we are finally done */
                    if (currentSecondary == null) {
                        logger.info("Completed cleaning secondary database(s)");
                        return;
                    }

                    final SecondaryInfo info =
                        infoMap.getSecondaryInfo(currentSecondary);
                    assert info != null;
                    assert info.needsCleaning();

                    final SecondaryDatabase db =
                        tableManager.getSecondaryDb(currentSecondary);

                    if (db == null) {
                        logger.log(Level.WARNING,
                                   "Failed to clean {0}, secondary " +
                                   "database {1} is missing",
                                   new Object[]{info, currentSecondary});
                        return;
                    }

                    if (!db.deleteObsoletePrimaryKeys(info.getLastKey(),
                                                      info.getLastData(),
                                                      CLEAN_BATCH_SIZE)) {
                        logger.log(Level.INFO, "Completed cleaning {0}",
                                   currentSecondary);
                        info.doneCleaning();
                        currentSecondary = null;
                    }
                    infoMap.persist(infoDb, txn);
                    txn.commit();
                    txn = null;
                } finally {
                    TxnUtil.abort(txn);
                }
            }
        }
    }

    /**
     * Thread to clean primary records. This thread will remove primary
     * records associated with a table which has been marked for deletion.
     */
    static class PrimaryCleanerThread extends MaintenanceThread {

        PrimaryCleanerThread(TableManager tableManager,
                             RepNode repNode,
                             ReplicatedEnvironment repEnv,
                             Logger logger) {
            super("KV primary cleaner",
                  tableManager, repNode, repEnv, logger);
        }

        @Override
        protected void doWork(Database infoDb) {
            if (isStopped()) {
                return;
            }

            final OperationHandler oh =
                new OperationHandler(repNode, tableManager.getParams());
            final MultiDeleteTableHandlerInternal opHandler =
                new MultiDeleteTableHandlerInternal(oh);

            /*
             * NOTE: this code does not attempt to use partition migration
             * streams to propagate deletions because it is assumed to be
             * running as a remove-table plan.  Such plans are mutually
             * exclusive with respect to elasticity plans so there can be no
             * migrations in progress.  If that assumption ever changes this
             * code needs to initialize and use MigrationStreamHandle objects.
             */

            /* The working table. */
            String currentTable = null;

            while (!isStopped()) {
                Transaction txn = null;
                try {
                    txn = repEnv.beginTransaction(null,
                                                  SecondaryInfoMap.CLEANER_CONFIG);

                    final SecondaryInfoMap infoMap =
                        SecondaryInfoMap.fetch(infoDb, txn, LockMode.RMW);

                    if (currentTable == null) {
                        currentTable = infoMap.getNextDeletedTableToClean();
                    }

                    /* If no more, we are finally done */
                    if (currentTable == null) {
                        logger.info("Completed cleaning partition database(s)" +
                                    " for all tables");
                        return;
                    }

                    final DeletedTableInfo info =
                        infoMap.getDeletedTableInfo(currentTable);
                    assert info != null;
                    assert !info.isDone();
                    if (info.getCurrentPartition() == null) {
                        for (PartitionId partition : repNode.getPartitions()) {
                            if (!info.isCompleted(partition)) {
                                info.setCurrentPartition(partition);
                                break;
                            }
                        }
                    }

                    if (info.getCurrentPartition() == null) {
                        logger.log(Level.FINE,
                                   "Completed cleaning partition database(s) " +
                                   "for {0}", currentTable);
                        info.setDone();
                        currentTable = null;
                    } else {

                        // delete some...
                        if (deleteABlock(info, opHandler, txn)) {
                            logger.log(Level.FINE,
                                       "Completed cleaning {0} for {1}",
                                       new Object[] {info.getCurrentPartition(),
                                                     currentTable});
                            // Done with this partition
                            info.completeCurrentPartition();
                        }
                    }

                    infoMap.persist(infoDb, txn);
                    txn.commit();
                    txn = null;
                } finally {
                    TxnUtil.abort(txn);
                }
            }
        }

        /*
         * Deletes up to DELETE_BATCH_SIZE number of records from some
         * primary (partition) database.
         */
        private boolean deleteABlock(DeletedTableInfo info,
                                     MultiDeleteTableHandlerInternal opHandler,
                                     Transaction txn) {
            final MultiDeleteTable op =
                new MultiDeleteTable(info.getParentKeyBytes(),
                                     info.getTargetTableId(),
                                     info.getMajorPathComplete(),
                                     DELETE_BATCH_SIZE,
                                     info.getCurrentKeyBytes());

            final Result result =
                opHandler.execute(op,
                                  txn,
                                  info.getCurrentPartition());
            int num = result.getNDeletions();
            logger.log(Level.FINE, "Deleted {0} records in partition {1}{2}",
                       new Object[]{num, info.getCurrentPartition(),
                                    (num < DELETE_BATCH_SIZE ?
                                     ", partition is complete" : "")});
            if (num < DELETE_BATCH_SIZE) {
                /* done with this partition */
                info.setCurrentKeyBytes(null);
                return true;
            }
            info.setCurrentKeyBytes(op.getLastDeleted());
            return false;
        }
    }

    /*
     * Special internal OP which bypasses security checks.
     */
    private static class MultiDeleteTableHandlerInternal
            extends MultiDeleteTableHandler {
        MultiDeleteTableHandlerInternal(OperationHandler handler) {
            super(handler);
        }

        /*
         * Overriding verifyTableAccess() will bypass keyspace and
         * security checks.
         */
        @Override
        public void verifyTableAccess(MultiDeleteTable op) {}
    }
}
