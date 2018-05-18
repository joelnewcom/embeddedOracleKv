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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import oracle.kv.Key;
import oracle.kv.impl.api.table.TableBuilder;
import oracle.kv.impl.api.table.TableImpl;
import oracle.kv.impl.api.table.TableMetadata;
import oracle.kv.impl.metadata.Metadata.MetadataType;
import oracle.kv.impl.rep.RepNode;
import oracle.kv.impl.rep.stats.PartitionLeaseManager.PartitionLeaseInfo;
import oracle.kv.impl.topo.PartitionId;
import oracle.kv.impl.util.TxnUtil;
import oracle.kv.table.Row;
import oracle.kv.table.Table;
import oracle.kv.table.TableAPI;

/**
 * The class scans the partition database to calculate primary key statistics
 * and stores the scanned results into statistics tables.
 */
public class PartitionScan extends StatsScan<PartitionLeaseInfo> {
    /* Table name */
    public static final String TABLE_NAME =
        TableImpl.SYSTEM_TABLE_PREFIX + "TableStatsPartition";

    /* All fields within table TableStatsPartition */
    protected static final String COL_NAME_TABLE_NAME = "tableName";
    protected static final String COL_NAME_PARTITION_ID = "partitionId";
    protected static final String COL_NAME_SHARD_ID = "shardId";
    protected static final String COL_NAME_COUNT = "count";
    protected static final String COL_NAME_AVG_KEY_SIZE = "avgKeySize";

    private final PartitionId partId;
    private final int groupId;
    private Table tableStatsTable;
    private SortedKeyToTableLookup tableLookupMap;
    private final Set<String> emptyTableSet = new HashSet<>();

    /* The key to record the last read one. It is used as a resume key */
    private byte[] resumeKey = null;

    /*
     * The name of the fake internal table used to store statistics for
     * KV pairs.
     */
    public static String KV_STATS_TABLE_NAME = "$KV$";

    final Map<String, StatsAccumulator> tableAccMap = new HashMap<>();

    public PartitionScan(TableAPI tableAPI,
                         PartitionId partId,
                         int groupId,
                         RepNode repNode,
                         StatsLeaseManager<PartitionLeaseInfo> leaseManager,
                         PartitionLeaseInfo leaseInfo,
                         long scanInterval,
                         Logger logger) {
        super(repNode, tableAPI, leaseManager, leaseInfo, scanInterval, logger);
        this.partId = partId;
        this.groupId = groupId;
    }

    @Override
    protected boolean checkStatsTable(TableMetadata metadata) {
        if (tableStatsTable !=  null) {
            return true;
        }

        tableStatsTable = metadata.getTable(TABLE_NAME);
        if (tableStatsTable == null) {
            /* Table does not exist, stop gathering statistics info */
            return false;
        }

        return true;
    }

    @Override
    protected void accumulateResult(byte[] key) {
        /*
         * Check whether a key is belong to a table one by one. If a key is
         * belong to a table, store it into a map with the table name; if
         * not, associate it with the fake internal table: KV_STATS_TABLE_NAME.
         */

        /* Filter out internal key space record */
        if (Key.keySpaceIsInternal(key)) {
            return;
        }

        final TableImpl target = tableLookupMap.getTable(key);

        final String tableName = (target == null) ? KV_STATS_TABLE_NAME :
            target.getFullName();
        StatsAccumulator csa = tableAccMap.get(tableName);

        if (csa == null) {
            csa = new StatsAccumulator();
            tableAccMap.put(tableName, csa);

            /* Find data for the table and remove table name from set */
            emptyTableSet.remove(tableName);
        }
        csa.increment(key.length);


    }

    @Override
    protected void wrapResult() {
        /* Deal with empty table */
        for (String tableName : emptyTableSet) {
            StatsAccumulator csa = new StatsAccumulator();
            tableAccMap.put(tableName, csa);
        }

        /*
         * Convert individual table statistics into rows that can be inserted
         * into table TableStatsPartition.
         */
        for (Map.Entry<String, StatsAccumulator> entry :
                tableAccMap.entrySet()) {

            final Row row = tableStatsTable.createRow();
            row.put(COL_NAME_TABLE_NAME, entry.getKey());
            row.put(COL_NAME_PARTITION_ID, partId.getPartitionId());
            row.put(COL_NAME_SHARD_ID, groupId);
            row.put(COL_NAME_COUNT, entry.getValue().count);
            row.put(COL_NAME_AVG_KEY_SIZE, entry.getValue().count != 0?
                                            (int)(entry.getValue().totalSize/
                                            entry.getValue().count) : 0);
            addRow(row);
        }
    }

    @Override
    protected boolean preScan() {
        tableAccMap.clear();
        emptyTableSet.clear();
        resumeKey = null;

        final TableMetadata metadata =
                (TableMetadata)repNode.getMetadata(MetadataType.TABLE);
        if (metadata == null) {
            return false;
        }

        /* Get all table name */
        tableLookupMap = new SortedKeyToTableLookup(metadata);

        /* Initialize with fake table name. */
        emptyTableSet.add(KV_STATS_TABLE_NAME);
        for (String tableName : metadata.getTables().keySet()) {
            emptyTableSet.add(tableName);
        }

        return true;
    }

    @Override
    protected void postScan(boolean scanCompleted) {

    }

    @Override
    protected Database getDatabase() {
        return repNode.getPartitionDB(partId);
    }

    /**
     * A class to assist to record and accumulate the result of scanning.
     */
    private class StatsAccumulator {
        private long count;
        private long totalSize;

        private StatsAccumulator() {
            this.count = 0;
            this.totalSize = 0;
        }

        private void increment(long size) {
            count++;
            totalSize += size;
        }
    }

    /**
     * Utility class for efficient mapping of a sequence of sorted keys as part
     * of a partition iteration. It does this by maintaining a MRU list of
     * tables used to resolve the keys.
     */
    private class SortedKeyToTableLookup {
        /*
         * MRU list of tables used to map a key.
         */
        private final TableImpl[] tables;

        SortedKeyToTableLookup(TableMetadata metadata) {
            super();
            final Collection<Table> coll = metadata.getTables().values();

            /* Use an array to minimize iteration costs. */
            tables = new TableImpl[coll.size()];
            int i=0;
            for (Table t : metadata.getTables().values()) {
                tables[i++] = (TableImpl)t;
            }
        }

        /**
         * Returns the table associated with the key or null, if it's a
         * KV key.
         * Good performance here is essential since it's effectively an inner
         * loop during sorted key iteration.
         *
         * @param key the key to be mapped to a table
         * @return the table associated with the key or null
         */
        private TableImpl getTable(byte[] key) {

            for (int i=0; i < tables.length; i++) {

                final TableImpl target = tables[i].findTargetTable(key);
                if (target == null) {
                    continue;
                }

                if (i != 0) {
                    /*
                     * Sort for MRU
                     */
                    final TableImpl hotTable = tables[i];

                    for (int j = i; j > 0; j--) {
                        tables[j] = tables[j-1];
                    }
                    tables[0] = hotTable;
                }

                return target;
            }

            return null;
        }
    }


    @Override
    protected boolean scanDatabase(Environment env, Database db) {
        Cursor cursor = null;
        Transaction txn = null;
        try {
            txn = env.beginTransaction(null, txnConfig);
            txn.setTxnTimeout(TXN_TIME_OUT, TimeUnit.MILLISECONDS);

            int nRecords = 0;
            cursor = db.openCursor(txn, cursorConfig);

            final DatabaseEntry keyEntry = new DatabaseEntry();
            final DatabaseEntry dataEntry = new DatabaseEntry();
            dataEntry.setPartial(0, 0, true);
            OperationStatus status;

            if (resumeKey == null) {
                status = cursor.getNext(keyEntry, dataEntry,
                                        LockMode.READ_UNCOMMITTED);
            } else {
                keyEntry.setData(resumeKey);
                status = cursor.getSearchKeyRange(keyEntry, dataEntry,
                                                  LockMode.READ_UNCOMMITTED);
                if (status == OperationStatus.SUCCESS &&
                        Arrays.equals(resumeKey, keyEntry.getData())) {
                    status = cursor.getNext(keyEntry, dataEntry,
                                            LockMode.READ_UNCOMMITTED);
                }
            }

            if (status != OperationStatus.SUCCESS) {
                return false;
            }

            boolean hasMoreElement = false;
            while (status == OperationStatus.SUCCESS && !stop) {
                /* Record the latest key as a resume key */
                resumeKey = keyEntry.getData();

                /* Accumulate the key into results */
                accumulateResult(resumeKey);
                nRecords++;

                if (nRecords >= BATCH_SIZE) {
                    hasMoreElement = true;
                    break;
                }
                dataEntry.setPartial(0, 0, true);
                status = cursor.getNext(keyEntry, dataEntry,
                                        LockMode.READ_UNCOMMITTED);
            }
            return hasMoreElement;
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
     * Get table builder of TableStatsPartition
     */
    public static TableBuilder getTableBuilder() {
        final TableBuilder builder =
            TableBuilder.createSystemTableBuilder(TABLE_NAME);

        /* Types of all fields within this table */
        builder.addString(COL_NAME_TABLE_NAME);
        builder.addInteger(COL_NAME_PARTITION_ID);
        builder.addInteger(COL_NAME_SHARD_ID);
        builder.addLong(COL_NAME_COUNT);
        builder.addInteger(COL_NAME_AVG_KEY_SIZE);
        builder.primaryKey(COL_NAME_TABLE_NAME, COL_NAME_PARTITION_ID);
        builder.shardKey(COL_NAME_TABLE_NAME, COL_NAME_PARTITION_ID);

        return builder;
    }
}
