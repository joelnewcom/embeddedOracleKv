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

package oracle.kv.impl.rep.stats;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.DbInternal.Search;
import com.sleepycat.je.Environment;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationResult;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryCursor;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.TransactionConfig;
import com.sleepycat.je.rep.NoConsistencyRequiredPolicy;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import oracle.kv.impl.api.table.TableBuilder;
import oracle.kv.impl.api.table.TableImpl;
import oracle.kv.impl.api.table.TableMetadata;
import oracle.kv.impl.rep.RepNode;
import oracle.kv.impl.rep.stats.IndexLeaseManager.IndexLeaseInfo;
import oracle.kv.impl.rep.table.TableManager;
import oracle.kv.impl.test.TestHook;
import oracle.kv.impl.test.TestHookExecute;
import oracle.kv.impl.util.TxnUtil;
import oracle.kv.table.Row;
import oracle.kv.table.Table;
import oracle.kv.table.TableAPI;

/**
 * The class is to scan secondary database to get index key statistics and
 * store the scanning results into statistics tables
 *
 */
public class TableIndexScan extends StatsScan<IndexLeaseInfo> {
    /* Table name */
    public static final String TABLE_NAME =
        TableImpl.SYSTEM_TABLE_PREFIX + "TableStatsIndex";

    /* All fields within this table */
    protected static final String COL_NAME_TABLE_NAME = "tableName";
    protected static final String COL_NAME_INDEX_NAME = "indexName";
    protected static final String COL_NAME_SHARD_ID = "shardId";
    protected static final String COL_NAME_COUNT = "count";
    protected static final String COL_NAME_AVG_KEY_SIZE = "avgKeySize";

    private final String tableName;
    private final String indexName;
    private final int groupId;
    private Table indexStatsTable;
    private long count = 0;
    private long keyTotalSize = 0;

    private static long GET_ENV_TIMEOUT = 5000;
    private Database indexDB = null;

    /* The keys to record the last read one. They are used as a resume key */
    private byte[] resumeSecondaryKey;
    private byte[] resumePrimaryKey;

    /* This hook affects before re-open database */
    public static TestHook<Integer> BEFORE_OPEN_HOOK;

    /* This hook affects after re-open database */
    public static TestHook<Integer> AFTER_OPEN_HOOK;

    protected TableIndexScan(TableAPI tableAPI,
                             String tableName,
                             String indexName,
                             int groupId,
                             RepNode repNode,
                             StatsLeaseManager<IndexLeaseInfo> leaseManager,
                             IndexLeaseInfo leaseInfo,
                             long scanInterval,
                             Logger logger) {
        super(repNode, tableAPI, leaseManager, leaseInfo, scanInterval, logger);
        this.tableName = tableName;
        this.indexName = indexName;
        this.groupId = groupId;
    }

    @Override
    protected boolean checkStatsTable(TableMetadata metadata) {
        if (indexStatsTable != null) {
            return true;
        }

        indexStatsTable = metadata.getTable(TABLE_NAME);
        if (indexStatsTable == null) {
            /* Table does not exist, stop to gather statistics info */
            return false;
        }

        return true;
    }

    @Override
    protected void accumulateResult(byte[] indexKey) {
        /* Aggregate the scanned results */
        keyTotalSize += indexKey.length;
        count++;
    }

    @Override
    protected void wrapResult() {
        /* Add scanning results into a cache list */
        /* Wrap Table statistics as the rows of table TableStatsIndex */
        Row row = indexStatsTable.createRow();

        row.put(COL_NAME_TABLE_NAME, tableName);
        row.put(COL_NAME_INDEX_NAME, indexName);
        row.put(COL_NAME_SHARD_ID, groupId);
        row.put(COL_NAME_COUNT, count);
        row.put(COL_NAME_AVG_KEY_SIZE, count != 0?
                (int)(keyTotalSize/count) : 0);

        addRow(row);
    }

    @Override
    /**
     * Get secondary database. To avoid Incremental population is currently
     * enabled exception, it does not get secondary database from table manager.
     * Instead, it opens secondary database as database and open it via calling
     * Environment.openDatabase.
     */
    protected Database getDatabase() {

        final ReplicatedEnvironment repEnv = repNode.getEnv(GET_ENV_TIMEOUT);
        if (repEnv == null) {
            throw new IllegalStateException("Cannot open index DB for index " +
                    indexName + ": ReplicatedEnvironment is null");
        }

        /* Create index DB name */
        String databaseName = TableManager.createDbName(indexName, tableName);

        final TransactionConfig dbTxnConfig = new TransactionConfig().
            setConsistencyPolicy(NoConsistencyRequiredPolicy.NO_CONSISTENCY);

        SecondaryConfig dbConfig = new SecondaryConfig();
        dbConfig.setTransactional(true).
                    setAllowCreate(false).
                    setSortedDuplicates(true).
                    setReadOnly(true).
                    setSecondaryAssociation(repNode.getTableManager());

        /* Open secondary database as normal database */
        Transaction txn = null;

        TestHookExecute.doHookIfSet(BEFORE_OPEN_HOOK, 1);
        try {
             txn = repEnv.beginTransaction(null, dbTxnConfig);
             indexDB = repEnv.openSecondaryDatabase(txn,
                                                    databaseName,
                                                    null,
                                                    dbConfig);
             txn.commit();
             txn = null;
        } catch (IllegalStateException e) {
             throw e;
        } finally {
            TxnUtil.abort(txn);
        }

        TestHookExecute.doHookIfSet(AFTER_OPEN_HOOK, 1);

        if (indexDB == null) {
            throw new IllegalStateException("Missing index DB for index " +
                                            indexName);
        }

        return indexDB;
    }

    @Override
    protected boolean preScan() {
        count = 0;
        keyTotalSize = 0;
        resumeSecondaryKey = null;
        resumePrimaryKey = null;

        return true;
    }

    @Override
    protected void postScan(boolean scanCompleted) {
        /* close the open database */
        if (indexDB != null) {

            final Environment env = indexDB.getEnvironment();

            if ((env == null) || !env.isValid()) {
                return;
            }
            TxnUtil.close(logger, indexDB, "secondary");
        }
    }

    @Override
    protected boolean scanDatabase(Environment env, Database db) {
        /* The type of db should be SecondaryDatabase */
        assert (db instanceof SecondaryDatabase);

        SecondaryDatabase secondaryDb = (SecondaryDatabase)db;
        SecondaryCursor cursor = null;
        Transaction txn = null;
        try {
            txn = env.beginTransaction(null, txnConfig);
            txn.setTxnTimeout(TXN_TIME_OUT, TimeUnit.MILLISECONDS);
            cursor = secondaryDb.openCursor(txn, cursorConfig);

            int nRecords = 0;
            final DatabaseEntry keyEntry = new DatabaseEntry();
            final DatabaseEntry pKeyEntry = new DatabaseEntry();
            final DatabaseEntry dataEntry = new DatabaseEntry();
            dataEntry.setPartial(0, 0, true);
            OperationStatus status;
            OperationResult result;

            if (resumeSecondaryKey == null) {
                status = cursor.getFirst(keyEntry, pKeyEntry, dataEntry,
                                         LockMode.READ_UNCOMMITTED);
            } else {
                keyEntry.setData(resumeSecondaryKey);
                pKeyEntry.setData(resumePrimaryKey);

                /* Search is set as Search.GT and the scanning is ascend */
                final Search search = Search.GT;
                /* First search within dups for the given sec key. */
                result = DbInternal.searchBoth(
                    cursor, keyEntry, pKeyEntry,
                    dataEntry, search,
                    LockMode.READ_UNCOMMITTED.toReadOptions());
                status = result != null ? OperationStatus.SUCCESS :
                    OperationStatus.NOTFOUND;

                /*
                 * If NOTFOUND then we're done because the search is limited to
                 * the dups within the given sec key.
                 */
                if (status != OperationStatus.SUCCESS) {
                    /*
                     * There are no more records with the given sec key.  Move
                     * to the next sec key, and to the first dup for that key.
                     */
                    result = DbInternal.search(
                        cursor, keyEntry, pKeyEntry,
                        dataEntry, search,
                        LockMode.READ_UNCOMMITTED.toReadOptions());
                    status = result != null ? OperationStatus.SUCCESS :
                        OperationStatus.NOTFOUND;

                }
            }

            while (status == OperationStatus.SUCCESS && !stop) {
                /* Record the latest keys as a resume keys */
                resumeSecondaryKey = keyEntry.getData();
                resumePrimaryKey = pKeyEntry.getData();

                /* Accumulate the key into results */
                accumulateResult(resumeSecondaryKey);
                nRecords++;

                if (nRecords >= BATCH_SIZE) {
                    return true;
                }
                dataEntry.setPartial(0, 0, true);
                status = cursor.getNext(keyEntry, pKeyEntry, dataEntry,
                                        LockMode.READ_UNCOMMITTED);
            }
        } catch (DatabaseException | IllegalArgumentException e) {
            logger.log(Level.FINE, "Scanning encounters exception: {0}, " +
                       "iteration scanning exits", e);
        } finally {
            if (cursor != null) {
                TxnUtil.close(cursor);
            }

            /* We are just reading. Abort every transaction */
            TxnUtil.abort(txn);
        }

        return false;
    }

    /**
     * Get table builder of TableStatsIndex
     */
    public static TableBuilder getTableBuilder() {
        final TableBuilder builder =
            TableBuilder.createSystemTableBuilder(TABLE_NAME);

        /* Types of all fields within this table */
        builder.addString(COL_NAME_TABLE_NAME);
        builder.addString(COL_NAME_INDEX_NAME);
        builder.addInteger(COL_NAME_SHARD_ID);
        builder.addLong(COL_NAME_COUNT);
        builder.addInteger(COL_NAME_AVG_KEY_SIZE);
        builder.primaryKey(COL_NAME_TABLE_NAME,
                           COL_NAME_INDEX_NAME,
                           COL_NAME_SHARD_ID);
        builder.shardKey(COL_NAME_TABLE_NAME,
                         COL_NAME_INDEX_NAME,
                         COL_NAME_SHARD_ID);

        return builder;
    }
}
