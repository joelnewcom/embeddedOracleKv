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

import java.util.Date;

import oracle.kv.impl.api.table.TableBuilder;
import oracle.kv.impl.api.table.TableImpl;
import oracle.kv.impl.rep.stats.IndexLeaseManager.IndexLeaseInfo;
import oracle.kv.impl.rep.stats.StatsLeaseManager.LeaseInfo;
import oracle.kv.table.PrimaryKey;
import oracle.kv.table.Row;
import oracle.kv.table.TableAPI;

/**
 * StatsLeaseTable is used to manage the shard-wide lease associated with the
 * gathering of index statistics for a shard.
 */
public class IndexLeaseManager extends StatsLeaseManager<IndexLeaseInfo> {
    /* Table name */
    public static final String TABLE_NAME =
        TableImpl.SYSTEM_TABLE_PREFIX + "IndexStatsLease";

    /* The index-specific columns in the lease table  */
    protected static final String COL_NAME_TABLE_NAME = "tableName";
    protected static final String COL_NAME_INDEX_NAME = "indexName";
    protected static final String COL_NAME_SHARD_ID = "shardId";

    public IndexLeaseManager(TableAPI tableAPI) {
        super(tableAPI);
    }

    @Override
    protected PrimaryKey createPrimaryKey(IndexLeaseInfo info) {
        PrimaryKey primaryKey = leaseTable.createPrimaryKey();
        primaryKey.put(COL_NAME_TABLE_NAME, info.getTableName());
        primaryKey.put(COL_NAME_INDEX_NAME, info.getIndexName());
        primaryKey.put(COL_NAME_SHARD_ID, info.getShardId());
        return primaryKey;
    }

    @Override
    protected Row createRow(IndexLeaseInfo info, boolean terminated) {
        Row row = leaseTable.createRow();
        row.put(COL_NAME_TABLE_NAME, info.getTableName());
        row.put(COL_NAME_INDEX_NAME, info.getIndexName());
        row.put(COL_NAME_SHARD_ID, info.getShardId());

        row.put(COL_NAME_LEASE_RN, info.getLeaseRN());

        final long currentTime = System.currentTimeMillis();
        final Date currentDate = new Date(currentTime);
        final String currentDateStr =
                StatsLeaseManager.DATE_FORMAT.format(currentDate);

        /*
         * Mark last updated data means we are done gathering statistics
         * for the index and no longer need to extend the lease.
         */
        if (terminated) {
            row.put(COL_NAME_LEASE_DATE, currentDateStr);

            /* Update the lastUpdated after the lease scanning completes */
            row.put(COL_NAME_LAST_UPDATE, currentDateStr);
        } else {
            final long nextTime = currentTime + info.getLeaseTime();
            final Date nextDate = new Date(nextTime);
            final String nextDateStr =
                    StatsLeaseManager.DATE_FORMAT.format(nextDate);
            row.put(COL_NAME_LEASE_DATE, nextDateStr);

            /*
             * Set the lastUpdated as the old one when the lease scanning is on
             * progress
             */
            row.put(COL_NAME_LAST_UPDATE, lastUpdatedDateStr);
        }


        return row;
    }

    @Override
    protected String getLeaseTableName() {
        return TABLE_NAME;
    }

    /**
     * Get table builder of IndexStatsLease
     */
    public static TableBuilder getTableBuilder() {
        final TableBuilder builder =
            TableBuilder.createSystemTableBuilder(TABLE_NAME);

        /* Types of all fields within this table */
        builder.addString(COL_NAME_TABLE_NAME);
        builder.addString(COL_NAME_INDEX_NAME);
        builder.addInteger(COL_NAME_SHARD_ID);
        builder.addString(COL_NAME_LEASE_RN);
        builder.addString(COL_NAME_LEASE_DATE);
        builder.addString(COL_NAME_LAST_UPDATE);

        builder.primaryKey(COL_NAME_TABLE_NAME,
                           COL_NAME_INDEX_NAME,
                           COL_NAME_SHARD_ID);

        builder.shardKey(COL_NAME_TABLE_NAME,
                         COL_NAME_INDEX_NAME,
                         COL_NAME_SHARD_ID);

        return builder;
    }

    /**
     * This class is to save a row of the table IndexStatsLease, and it also
     * converts passed arguments into a table row.
     */
    public static class IndexLeaseInfo extends LeaseInfo {

        private final String tableName;
        private final String indexName;
        private final int shardId;

        public IndexLeaseInfo(String tableName,
                              String indexName,
                              int shardId,
                              String leaseRN,
                              long leaseTime) {
            super(leaseRN, leaseTime);

            this.tableName = tableName;
            this.indexName = indexName;
            this.shardId = shardId;
        }

        public String getTableName() {
            return tableName;
        }

        public String getIndexName() {
            return indexName;
        }

        public int getShardId() {
            return shardId;
        }

        @Override
        public String toString() {
            return "index " + tableName + "." + indexName + " in shard-" +
                    shardId + " by " + leaseRN;
        }
    }

}
