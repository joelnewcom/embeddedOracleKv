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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sleepycat.bind.tuple.StringBinding;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.je.Durability;
import com.sleepycat.je.Durability.ReplicaAckPolicy;
import com.sleepycat.je.Durability.SyncPolicy;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.TransactionConfig;
import com.sleepycat.je.rep.NoConsistencyRequiredPolicy;
import com.sleepycat.je.rep.ReplicatedEnvironment;

import oracle.kv.KVVersion;
import oracle.kv.impl.api.table.IndexImpl;
import oracle.kv.impl.api.table.TableImpl;
import oracle.kv.impl.api.table.TableKey;
import oracle.kv.impl.metadata.Metadata;
import oracle.kv.impl.rep.table.TableManager.UpdateThread;
import oracle.kv.impl.test.TestHookExecute;
import oracle.kv.impl.topo.PartitionId;
import oracle.kv.impl.util.DatabaseUtils;
import oracle.kv.impl.util.SerializationUtil;
import oracle.kv.impl.util.TxnUtil;
import oracle.kv.table.PrimaryKey;
import oracle.kv.table.Table;

/**
 * Persistent class containing information on populating secondary databases.
 * An instance of this object is kept in a replicated database for use by the
 * master of the shard.
 */
class SecondaryInfoMap implements Serializable  {
    private static final long serialVersionUID = 1L;

    private static final String SECONDARY_INFO_DB_NAME = "SecondaryInfoDB";

    private static final String SECONDARY_INFO_KEY ="SecondaryInfoMap";

    private final static DatabaseEntry secondaryInfoKey = new DatabaseEntry();

    static {
        StringBinding.stringToEntry(SECONDARY_INFO_KEY, secondaryInfoKey);
    }

    /* Transaction configuration for r/w secondary info */
    static final TransactionConfig SECONDARY_INFO_CONFIG =
        new TransactionConfig().
            setConsistencyPolicy(NoConsistencyRequiredPolicy.NO_CONSISTENCY).
            setDurability(
                   new Durability(Durability.SyncPolicy.SYNC,
                                  Durability.SyncPolicy.SYNC,
                                  Durability.ReplicaAckPolicy.SIMPLE_MAJORITY));

    /*
     * Transaction configuration for secondary cleaner operations. Cleaner
     * operations can have looser durability to improve performance since any
     * lost deletes would be retried when the cleaner was restarted during
     * recovery.
     */
    static final TransactionConfig CLEANER_CONFIG =
        new TransactionConfig().
            setConsistencyPolicy(NoConsistencyRequiredPolicy.NO_CONSISTENCY).
            setDurability(
                   new Durability(Durability.SyncPolicy.NO_SYNC,
                                  Durability.SyncPolicy.NO_SYNC,
                                  Durability.ReplicaAckPolicy.SIMPLE_MAJORITY));

    /*
     * Transaction config to return what is locally at the replica, without
     * waiting for the master to make the state consistent wrt the master.
     * Also, the transaction will not wait for commit acks from the replicas.
     */
    static final TransactionConfig NO_WAIT_CONFIG =
        new TransactionConfig().
            setDurability(new Durability(SyncPolicy.NO_SYNC,
                                         SyncPolicy.NO_SYNC,
                                         ReplicaAckPolicy.NONE)).
            setConsistencyPolicy(NoConsistencyRequiredPolicy.NO_CONSISTENCY);

    private static final int CURRENT_SCHEMA_VERSION = 1;

    /*
     * Maps secondary DB name -> DB info.
     */
    private final Map<String, SecondaryInfo> secondaryMap = new HashMap<>();

    /*
     * Maps table name -> deleted table info. This map contains entries for
     * the tables which have been deleted and their data removed.
     */
    private final Map<String, DeletedTableInfo> deletedTableMap =
                                                        new HashMap<>();

    private int metadataSequenceNum = Metadata.EMPTY_SEQUENCE_NUMBER;

    /* Version of this object. */
    private final int version;

    private SecondaryInfoMap() {
        version = CURRENT_SCHEMA_VERSION;
    }

    /**
     * Gets the last seen metadata sequence number.
     *
     * @return the last seen metadata sequence number
     */
    int getMetadataSequenceNum() {
        return metadataSequenceNum;
    }

    /**
     * Sets the last seen metadata sequence number.
     *
     * @param seqNum
     */
    void setMetadataSequenceNum(int seqNum) {
        assert seqNum >= metadataSequenceNum;
        metadataSequenceNum = seqNum;
    }

    /**
     * Adds an info record for the specified secondary database if it isn't
     * already there.
     *
     * @param dbName the secondary database name
     * @param db the secondary info database
     * @param txn a transaction
     * @param logger
     */
    static void add(String dbName, Database db,
                    Transaction txn, Logger logger) {
        final SecondaryInfoMap infoMap = fetch(db, txn, LockMode.RMW);

        SecondaryInfo info = infoMap.secondaryMap.get(dbName);
        if (info == null) {
            info = new SecondaryInfo();
            infoMap.secondaryMap.put(dbName, info);

            logger.log(Level.FINE, "Adding {0} for {1}, map size= {2}",
                       new Object[]{info, dbName, infoMap.secondaryMap.size()});
            infoMap.persist(db, txn);
        }
    }

    /**
     * Checks to see if an index has been removed and record deleted tables if
     * needed. If an index has been removed the secondary DB for that index
     * is removed. This method is called from the table metadata update thread.
     *
     * @param currentTables the map of tables from the current MD
     * @param indexes the map of indexes in from the current MD
     * @param deletedTables the set of deleted tables
     * @param updateThread the update thread
     * @param logger
     */
    static void check(Map<String, Table> currentTables,
                      Map<String, IndexImpl> indexes,
                      Set<TableImpl> deletedTables,
                      UpdateThread updateThread,
                      Logger logger) {
        assert updateThread.infoDb != null;

        Transaction txn = null;
        try {
            boolean modified = false;

            txn = updateThread.repEnv.beginTransaction(null,
                                                       SECONDARY_INFO_CONFIG);

            final SecondaryInfoMap infoMap = fetch(updateThread.infoDb, txn,
                                                   LockMode.RMW);
            final Iterator<Entry<String, SecondaryInfo>> itr =
                                     infoMap.secondaryMap.entrySet().iterator();

            /* Remove secondary info for indexes that have been dropped */
            while (itr.hasNext()) {
                if (updateThread.isStopped()) {
                    return;
                }
                final Entry<String, SecondaryInfo> entry = itr.next();
                final String dbName = entry.getKey();
                if (!indexes.containsKey(dbName)) {
                    /* The DB must be closed before removing it */
                    if (!updateThread.closeSecondary(dbName)) {
                        continue;
                    }
                    logger.log(Level.FINE, "Removing secondary info for {0}",
                               dbName);
                    try {
                        TestHookExecute.
                                doHookIfSet(TableManager.BEFORE_REMOVE_HOOK, 1);
                        logger.log(Level.INFO,
                                   "Secondary database {0} is not " +
                                   "defined in table metadata " +
                                   "seq# {1} and is being removed.",
                                   new Object[]{dbName,
                                                updateThread.tableMd.
                                                          getSequenceNumber()});
                        /*
                         * Mark the secondary removed, then attempt to remove
                         * the DB. If the remove fails, marking it will keep
                         * it from being used until the DB is sucessfully
                         * removed during some future check.
                         */
                        entry.getValue().setRemoved();
                        modified = true;

                        /* Actually remove the DB */
                        updateThread.repEnv.removeDatabase(txn, dbName);

                        /* Remove the associated SecondaryInfo instance */
                        itr.remove();
                    } catch (DatabaseNotFoundException ignore) {
                        /* Already gone */
                        itr.remove();
                    } catch (RuntimeException re) {
                        /*
                         * Log the exception and go on, leaving the db info in
                         * the map for next time.
                         */
                        logger.log(Level.INFO,
                                   "Exception removing {0}: {1}, operation " +
                                   "will be retried",
                                   new Object[]{dbName, re.getMessage()});
                    } finally {
                        TestHookExecute.
                                 doHookIfSet(TableManager.AFTER_REMOVE_HOOK, 1);
                    }
                }
            }

            final Iterator<Entry<String, DeletedTableInfo>> itr2 =
                                  infoMap.deletedTableMap.entrySet().iterator();
            /*
             * Remove deleted table info for tables which have been removed
             * from the metadata.
             */
            while (itr2.hasNext()) {
                if (updateThread.isStopped()) {
                    return;
                }
                final Entry<String, DeletedTableInfo> entry = itr2.next();
                final Table table = currentTables.get(entry.getKey());

                /* If the table is gone, clean out the info */
                if (table == null) {
                    assert entry.getValue().isDone();
                    itr2.remove();
                    modified = true;
                } else {
                    /* Still in the MD, make sure it was marked for delete */
                    if (!((TableImpl)table).getStatus().isDeleting()) {
                        /*
                         * We think the table is being deleted, yet the MD
                         * says the table is valid (not being deleted). So
                         * something very bad happened.
                         *
                         * TODO - Work needs to be done to determine causes
                         * and remedies.
                         */
                        throw new IllegalStateException(
                                "Table metadata includes table " + table +
                                " but node thinks the table is deleted");
                    }
                }
            }

            /* Add entries for tables marked for delete */
            for (TableImpl table : deletedTables) {
                if (updateThread.isStopped()) {
                    return;
                }
                DeletedTableInfo info =
                            infoMap.getDeletedTableInfo(table.getParentName());

                if (info == null) {
                    info = new DeletedTableInfo(table);
                    infoMap.deletedTableMap.put(table.getFullName(), info);
                    modified = true;
                    continue;
                }
                assert !info.isDone();
            }

            if (modified) {
                try {
                    infoMap.persist(updateThread.infoDb, txn);
                    txn.commit();
                    txn = null;
                } catch (RuntimeException re) {
                    DatabaseUtils.handleException(re, logger,
                                                  "populate info map");
                }
            }
        } finally {
            TxnUtil.abort(txn);
        }
    }

    static void markForSecondaryCleaning(TableManager tableManager,
                                         ReplicatedEnvironment repEnv,
                                         Logger logger) {
        Database db = null;

        try {
            db = openDb(repEnv);

            Transaction txn = null;
            try {
                txn = repEnv.beginTransaction(null, SECONDARY_INFO_CONFIG);

                final SecondaryInfoMap infoMap = fetch(db, txn, LockMode.RMW);

                if (infoMap.secondaryMap.isEmpty()) {
                    return;
                }

                logger.log(Level.FINE, "Marking {0} for cleaning",
                           infoMap.secondaryMap.size());
                for (SecondaryInfo info : infoMap.secondaryMap.values()) {
                    info.markForCleaning();
                }
                try {
                    infoMap.persist(db, txn);
                    txn.commit();
                    txn = null;
                } catch (RuntimeException re) { // TODO - combine trys
                    DatabaseUtils.handleException(re, logger,
                                                  "populate info map");
                }
            } finally {
                if (txn != null) {
                    txn.abort();
                }
            }

            /*
             * Check to see if the primary cleaner thread can be started. Note
             * that we don't retry this as to no hold up the calling thread.
             */
            tableManager.checkMaintenanceThreads(repEnv, db);
        } finally {
            TxnUtil.close(logger, db, "populate info map");
        }
    }

    /**
     * Fetches the SecondaryInfoMap object from the db for
     * read-only use. If the db is empty an empty SecondaryInfoMap is returned.
     *
     * @param repEnv the environment
     * @return a SecondaryInfoMap object
     */
    static SecondaryInfoMap fetch(ReplicatedEnvironment repEnv) {
        Database db = null;

        try {
            db = openDb(repEnv);
            return fetch(db);
        } finally {
            if (db != null) {
                try {
                    db.close();
                } catch (DatabaseException de) {
                    /* Ignore */
                }
            }
        }
    }

    /**
     * Fetches the SecondaryInfoMap object from the db for
     * read-only use. If the db is empty an empty SecondaryInfoMap is returned.
     *
     * @param db the secondary info map db
     * @return a SecondaryInfoMap object
     */
    static SecondaryInfoMap fetch(Database db) {

        final Transaction txn = db.getEnvironment().
                                  beginTransaction(null, NO_WAIT_CONFIG);
        try {
            return fetch(db, txn, LockMode.READ_UNCOMMITTED);
        } finally {

            /* We are just reading. */
            if (txn.isValid()) {
                txn.commit();
            } else {
                TxnUtil.abort(txn);
            }
        }
    }

    /**
     * Fetches the PopulateInfoMap object from the db. If the db is empty an
     * empty SecondaryInfoMap is returned.
     *
     * @param db the secondary info map db
     * @param txn the transaction to use for the get
     * @param lockMode for the get
     * @return a SecondaryInfoMap object
     */
    static SecondaryInfoMap fetch(Database db,
                                  Transaction txn,
                                  LockMode lockMode) {
        if (txn == null) {
            throw new IllegalStateException("transaction can not be null");
        }

        final DatabaseEntry value = new DatabaseEntry();

        db.get(txn, secondaryInfoKey, value, lockMode);

        final SecondaryInfoMap infoMap =
            SerializationUtil.getObject(value.getData(),
                                        SecondaryInfoMap.class);

        /* If none, return an empty object */
        return (infoMap == null) ? new SecondaryInfoMap() : infoMap;
    }

    /**
     * Persists this object to the db using the specified transaction.
     *
     * @param db the secondary info map db
     * @param txn transaction to use for the write
     */
    void persist(Database db, Transaction txn) {
        db.put(txn, secondaryInfoKey,
               new DatabaseEntry(SerializationUtil.getBytes(this)));
    }

    /**
     * Actually opens (or creates) the replicated populate info map DB. The
     * caller is responsible for all exceptions.
     */
    static Database openDb(ReplicatedEnvironment repEnv) {
        final DatabaseConfig dbConfig =
                new DatabaseConfig().setAllowCreate(true).
                                     setTransactional(true);

        final TransactionConfig txnConfig = new TransactionConfig().
              setConsistencyPolicy(NoConsistencyRequiredPolicy.NO_CONSISTENCY);

        Transaction txn = null;
        Database db = null;
        try {
            txn = repEnv.beginTransaction(null, txnConfig);
            db = repEnv.openDatabase(txn, SECONDARY_INFO_DB_NAME, dbConfig);
            txn.commit();
            txn = null;
            final Database ret = db;
            db = null;
            return ret;
        } finally {
            TxnUtil.abort(txn);

            if (db != null) {
                try {
                    db.close();
                } catch (DatabaseException de) {
                    /* Ignore */
                }
            }
        }
    }

    private void readObject(ObjectInputStream ois)
            throws IOException, ClassNotFoundException {

	ois.defaultReadObject();

        if (version > CURRENT_SCHEMA_VERSION) {
            throw new IllegalStateException
                ("The secondary info map is at " +
                 KVVersion.CURRENT_VERSION +
                 ", schema version " +  CURRENT_SCHEMA_VERSION +
                 " but the stored schema is at version " + version +
                 ". Please upgrade this node's NoSQL Database version.");
        }

        /* In R2 there is only one version in existence */
        if (version != CURRENT_SCHEMA_VERSION) {
            throw new IllegalStateException
              ("Unexpected secondary info map store schema version, expected " +
               CURRENT_SCHEMA_VERSION +
               " but the stored schema is at version " + version + ".");
        }
    }

    SecondaryInfo getSecondaryInfo(String dbName) {
        final SecondaryInfo info = secondaryMap.get(dbName);
        return (info == null) ? null : (info.isRemoved() ? null : info);
    }

    boolean secondaryNeedsPopulate() {
        for (SecondaryInfo info : secondaryMap.values()) {
            if (info.needsPopulating()) {
                return true;
            }
        }
        return false;
    }

    boolean secondaryNeedsCleaning() {
        return getNextSecondaryToClean() != null;
    }

    String getNextSecondaryToPopulate() {
        for (Entry<String, SecondaryInfo> entry : secondaryMap.entrySet()) {
            if (entry.getValue().needsPopulating()) {
                return entry.getKey();
            }
        }
        return null;
    }

    String getNextSecondaryToClean() {
        for (Entry<String, SecondaryInfo> entry : secondaryMap.entrySet()) {
            if (entry.getValue().needsCleaning()) {
                return entry.getKey();
            }
        }
        return null;
    }

    DeletedTableInfo getDeletedTableInfo(String tableName) {
        return deletedTableMap.get(tableName);
    }

    void removeDeletedTableInfo(String tableName) {
        deletedTableMap.remove(tableName);
    }

    String getNextDeletedTableToClean() {
        for (Entry<String, DeletedTableInfo> entry : deletedTableMap.entrySet()) {
            if (!entry.getValue().isDone()) {
                return entry.getKey();
            }
        }
        return null;
    }

    boolean tableNeedDeleting() {
        return getNextDeletedTableToClean() != null;
    }

    @Override
    public String toString() {
        return "SecondaryInfoMap[" + secondaryMap.size() + ", " +
               deletedTableMap.size() + "]";
    }

    /*
     * Container for information regarding secondary databases which are
     * being populated.
     */
    static class SecondaryInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        /* true if the DB needds to be populated */
        private boolean needsPopulating = true;

        /*
         * true if the DB needs cleaning. Note that cleaning is disbaled until
         * the db has been populated.
         */
        private boolean needsCleaning = false;

        /* The last key used to populate/clean */
        private DatabaseEntry lastKey = null;

        /* The last data used to clean */
        private DatabaseEntry lastData = null;

        /* The partition being used to populate */
        private PartitionId currentPartition = null;

        /* The set of partitions which have been completed */
        private Set<PartitionId> completed = null;

        /*
         * True if the index for this secondary DB has been dropped, but the
         * secondary DB has not yet been removed.
         *
         * Note that this flag was added and therefore may be lost in an
         * upgrade situation. However, this is more an optimization and things
         * will function property if incorrectly set to false.
         */
        private boolean removed = false;

        /**
         * Returns true if the secondary DB needs to be populated.
         *
         * @return true if the secondary DB needs to be populated
         */
        boolean needsPopulating() {
            return needsPopulating;
        }

        /**
         * Signal that this secondary DB has been populated.
         */
        void donePopulation() {
            assert needsPopulating == true;
            needsPopulating = false;
            completed = null;
            lastKey = null;
            lastData = null;
        }

        /**
         * Sets the needs cleaning flag.
         */
        void markForCleaning() {
            if (!removed) {
                needsCleaning = true;
            }
        }

        /**
         * Returns true if the secondary DB needs cleaning. Note that cleaning
         * is disabled until population is completed.
         *
         * @return true if the secondary DB needs cleaning
         */
        boolean needsCleaning() {
            return needsPopulating ? false : needsCleaning;
        }

        void doneCleaning() {
            assert !needsPopulating;
            needsCleaning = false;
            lastKey = null;
            lastData = null;
        }

        /**
         * Gets the last key set through setLastKey(). If no last key was set,
         * then an empty DatabaseEntry object is returned.
         *
         * @return the last key
         */
        DatabaseEntry getLastKey() {
            if (lastKey == null) {
                lastKey = new DatabaseEntry();
            }
            return lastKey;
        }

        DatabaseEntry getLastData() {
            assert needsPopulating == false;    /* only used for cleaning */
            if (lastData == null) {
                lastData = new DatabaseEntry();
            }
            return lastData;
        }

        /**
         * Gets the partition currently being read from.
         *
         * @return the partition currently being read from or null
         */
        PartitionId getCurrentPartition() {
            assert needsPopulating == true;
            return currentPartition;
        }

        /**
         * Sets the current partition.
         *
         * @param partitionId
         */
        void setCurrentPartition(PartitionId partitionId) {
            assert needsPopulating == true;
            assert partitionId != null;
            assert currentPartition == null;
            currentPartition = partitionId;
        }

        /**
         * Returns true if a populate from the specified partition has been
         * completed.
         *
         * @param partitionId
         * @return true if a populate has been completed
         */
        boolean isCompleted(PartitionId partitionId) {
            assert needsPopulating == true;
            return completed == null ? false : completed.contains(partitionId);
        }

        /**
         * Completes the current populate. Calling this method will add the
         * current partition to the completed list and clear the current
         * partition and last key.
         */
        void completeCurrentPartition() {
            assert needsPopulating == true;
            if (completed == null) {
                completed = new HashSet<>();
            }
            completed.add(currentPartition);
            currentPartition = null;
            lastKey = null;
            lastData = null;
        }

        /**
         * Returns true if the remove flag is set.
         *
         * @return true if the remove flag is set
         */
        private boolean isRemoved() {
            return removed;
        }

        /**
         * Sets the removed flag and clears the other state fields.
         */
        private void setRemoved() {
            removed = true;
            needsPopulating = false;
            needsCleaning = false;
            currentPartition = null;
            lastKey = null;
            lastData = null;
        }

        @Override
        public String toString() {
            return "SecondaryInfo[" + needsPopulating +
                   ", " + currentPartition +
                   ", " + ((completed == null) ? "-" : completed.size()) +
                   ", " + needsCleaning + ", " + removed + "]";
        }
    }

    /*
     * Container for information on tables which are marked for deletion and
     * need to have their repecitive data removed.
     */
    static class DeletedTableInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        /* True if the primary data has been cleaned */
        private boolean done = false;

        /* The partition being cleaned */
        private PartitionId currentPartition = null;

        private Set<PartitionId> completed = null;

        private final boolean majorPathComplete;
        private final boolean isChildTable;
        private final long targetTableId;
        private final byte[] parentKeyBytes;
        /* holds the current state of the iteration for batching */
        private byte[] currentKeyBytes;

        DeletedTableInfo(TableImpl targetTable) {
            TableImpl parentTable = targetTable;
            /*
             * If the target is a child table move the parentTable
             * to the top-level parent.  After this loop it is ok if
             * parentTable == targetTable.
             */
            if (targetTable.getParent() != null) {
                isChildTable = true;
                parentTable = targetTable.getTopLevelTable();
            } else {
                isChildTable = false;
            }
            PrimaryKey pkey = parentTable.createPrimaryKey();
            TableKey key = TableKey.createKey(parentTable, pkey, true);
            parentKeyBytes = key.getKeyBytes();
            majorPathComplete = key.getMajorKeyComplete();
            currentKeyBytes = parentKeyBytes;
            targetTableId = targetTable.getId();
        }

        byte[] getCurrentKeyBytes() {
            return currentKeyBytes;
        }

        void setCurrentKeyBytes(byte[] newKey) {
            currentKeyBytes = newKey;
        }

        byte[] getParentKeyBytes() {
            return parentKeyBytes;
        }

        long getTargetTableId() {
            return targetTableId;
        }

        boolean getMajorPathComplete() {
            return majorPathComplete;
        }

        boolean isChildTable() {
            return isChildTable;
        }

        boolean isDone() {
            return done;
        }

        boolean isCompleted(PartitionId partition) {
            return (completed == null) ? false : completed.contains(partition);
        }

        /**
         * Completes the current populate. Calling this method will add the
         * current partition to the completed list and clear the current
         * partition and last key.
         */
        void completeCurrentPartition() {
            if (completed == null) {
                completed = new HashSet<>();
            }
            completed.add(currentPartition);
            currentPartition = null;
        }

        PartitionId getCurrentPartition() {
            return currentPartition;
        }

        void setCurrentPartition(PartitionId partition) {
            currentPartition = partition;
        }

        void setDone() {
            done = true;
        }

        @Override
        public String toString() {
            return "DeletedTableInfo[" + done + ", " + currentPartition +
                   ", " + ((completed == null) ? "-" : completed.size()) + "]";
        }
    }
}
