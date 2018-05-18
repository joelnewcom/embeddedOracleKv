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

import static oracle.kv.impl.rep.PartitionManager.DB_OPEN_RETRY_MS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.kv.Key;
import oracle.kv.Key.BinaryKeyIterator;
import oracle.kv.impl.admin.IllegalCommandException;
import oracle.kv.impl.api.table.IndexImpl;
import oracle.kv.impl.api.table.TableImpl;
import oracle.kv.impl.api.table.TableMetadata;
import oracle.kv.impl.fault.DatabaseNotReadyException;
import oracle.kv.impl.fault.RNUnavailableException;
import oracle.kv.impl.metadata.Metadata;
import oracle.kv.impl.metadata.Metadata.MetadataType;
import oracle.kv.impl.metadata.MetadataInfo;
import oracle.kv.impl.rep.MetadataManager;
import oracle.kv.impl.rep.RepNode;
import oracle.kv.impl.rep.RepNodeService.Params;
import oracle.kv.impl.rep.table.MaintenanceThread.PopulateThread;
import oracle.kv.impl.rep.table.MaintenanceThread.PrimaryCleanerThread;
import oracle.kv.impl.rep.table.MaintenanceThread.SecondaryCleanerThread;
import oracle.kv.impl.rep.table.SecondaryInfoMap.DeletedTableInfo;
import oracle.kv.impl.rep.table.SecondaryInfoMap.SecondaryInfo;
import oracle.kv.impl.test.TestHook;
import oracle.kv.impl.tif.TextIndexFeederManager;
import oracle.kv.impl.topo.PartitionId;
import oracle.kv.impl.util.DatabaseUtils;
import oracle.kv.impl.util.StateTracker;
import oracle.kv.impl.util.TxnUtil;
import oracle.kv.table.Index;
import oracle.kv.table.Table;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.EnvironmentFailureException;
import com.sleepycat.je.SecondaryAssociation;
import com.sleepycat.je.SecondaryConfig;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.TransactionConfig;
import com.sleepycat.je.rep.NoConsistencyRequiredPolicy;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.StateChangeEvent;

/**
 * Manages the secondary database handles for the rep node.
 */
public class TableManager extends MetadataManager<TableMetadata>
                          implements SecondaryAssociation {

    /*
     * Create the name of the secondary DB based on the specified index
     * and table names.  Use index name first.  TODO: is this best order?
     */
    public static String createDbName(String indexName,
                                      String tableName) {
        StringBuilder sb = new StringBuilder();
        sb.append(indexName);
        sb.append(".");
        sb.append(tableName);
        return sb.toString();
    }

    /*
     * Gets the table name from a secondary database name.
     */
    static String getTableName(String dbName) {
        return dbName.split("\\.", 2)[1];
    }

    private final Params params;

    private final StateTracker stateTracker;

    /*
     * The table metadata. TableMetada is not thread safe. The instance
     * referenced by tableMetadata should be treated as immutable. Updates
     * to the table metadata must be made to a copy first and then replace the
     * reference with the copy.
     */
    private volatile TableMetadata tableMetadata = null;

    /*
     * Map of secondary database handles. Modification must be made with
     * the threadLock held.
     */
    private final Map<String, SecondaryDatabase> secondaryDbMap =
                                                            new HashMap<>();

    /* Thread used to asynchronously open secondary database handles */
    private UpdateThread updateThread = null;

    /*
     * Thread used to preform maintenance operations such as populating
     * secondary DBs. Only one thread is run at a time.
     */
    private MaintenanceThread maintenanceThread = null;

    /*
     * Lock controlling updateThread, maintenanceThread, and modification to
     * secondaryDbMap. If object synchronization and the threadLock is needed
     * the synchronization should be first.
     */
    private final ReentrantLock threadLock = new ReentrantLock();

    /*
     * The map is an optimization for secondary DB lookup. This lookup happens
     * on every operation and so should be as efficient as possible. This map
     * parallels the Table map in TableMetadata except that it only contains
     * entries for tables which contain indexes (and therefore reference
     * secondary DBs). The map is reconstructed whenever the table MD is
     * updated.
     *
     * If the map is null, the table metadata has not yet been initialized and
     * operations should not be allowed.
     *
     * Note that secondaryLookupMap may be out-of-date relitive to the
     * non-synchronized tableMetadata.
     */
    private volatile Map<String, TableEntry> secondaryLookupMap = null;

    /*
     * Map to lookup a table via its ID.
     *
     * If the map is null, the table metadata has not yet been initialized and
     * operations should not be allowed.
     *
     * Note that idLookupMap may be out-of-date relative to the non-synchronized
     * tableMetadata.
     */
    private volatile Map<Long, TableImpl> idLookupMap = null;

    /**
     * Map to lookup a r2compat table id via its name.  It is used to
     * accelerate the r2compat table match test in permisson checking.
     *
     * If the map is null, the table metadata has not yet been initialized and
     * operations should not be allowed.
     *
     * Note that r2NameLookupMap may be out-of-date relative to the
     * non-synchronized tableMetadata.
     */
    private volatile Map<String, Long> r2NameIdLookupMap = null;

    /* The hook affects before remove database */
    public static TestHook<Integer> BEFORE_REMOVE_HOOK;
    /* The hook affects after remove database */
    public static TestHook<Integer> AFTER_REMOVE_HOOK;

    public TableManager(RepNode repNode, Params params) {
        super(repNode, params);
        this.params = params;
        logger.log(Level.INFO, "Table manager created");// TODO - FINE?
        stateTracker = new TableManagerStateTracker(logger);
    }

    @Override
    public void shutdown() {
        stateTracker.shutdown();
        threadLock.lock();
        try {
            stopUpdateThread();
            stopMaintenanceThread();
        } finally {
            threadLock.unlock();
        }
        super.shutdown();
    }

    /**
     * Returns the table metadata object. Returns null if there was an error.
     *
     * @return the table metadata object or null
     */
    public TableMetadata getTableMetadata() {

        if (tableMetadata == null) {

            synchronized (this) {
                if (tableMetadata == null) {
                    try {
                        tableMetadata = fetchMetadata();
                    } catch (DatabaseNotReadyException dnre) {
                        /* DB not ready, ignore */
                        return  null;
                    }
                    /*
                     * If the DB is empty, we create a new instance so
                     * that we don't keep re-reading.
                     */
                    if (tableMetadata == null) {
                        tableMetadata = new TableMetadata(true);
                    }
                }
            }
        }
        return tableMetadata;
    }

    /* -- public index APIs -- */

    /**
     * Returns true if the specified index has been successfully added.
     *
     * @param indexName the index ID
     * @param tableName the fully qualified table name
     * @return true if the specified index has been successfully added
     */
    public boolean addIndexComplete(String indexName,
                                    String tableName) {
        final ReplicatedEnvironment repEnv = repNode.getEnv(1);

        if (repEnv == null) {
            return false;
        }

        final SecondaryInfoMap secondaryInfoMap = getSecondaryInfoMap(repEnv);

        /* If there is an issue reading the info object, punt */
        if (secondaryInfoMap == null) {
            return false;
        }

        final String dbName = createDbName(indexName, tableName);

        final SecondaryInfo info = secondaryInfoMap.getSecondaryInfo(dbName);

        logger.log(Level.FINE, "addIndexComplete({0}) returning {1}",
                   new Object[]{dbName, info});
        return (info == null) ? false : !info.needsPopulating();
    }

    /**
     * Returns true if the data associated with the specified table has been
     * removed from the store.
     *
     * @param tableName the fully qualified table name
     * @return true if the table data has been removed
     */
    public boolean removeTableDataComplete(String tableName) {
        final ReplicatedEnvironment repEnv = repNode.getEnv(1);

        if (repEnv == null) {
            return false;
        }

        final SecondaryInfoMap secondaryInfoMap = getSecondaryInfoMap(repEnv);

        /* If there is an issue reading the info object, punt */
        if (secondaryInfoMap == null) {
            return false;
        }

        final DeletedTableInfo info =
                                secondaryInfoMap.getDeletedTableInfo(tableName);
        if (info != null) {
            return info.isDone();
        }
        final TableMetadata md = getTableMetadata();

        return (md == null) ? false : (md.getTable(tableName) == null);
    }

   /**
    * Gets the table instance for the specified ID. If no table is defined
    * null is returned.
    *
    * @param tableId a table ID
    * @return the table instance
    * @throws RNUnavailableException is the table metadata is not yet
    * initialized
    */
    public TableImpl getTable(long tableId) {
        final Map<Long, TableImpl> map = idLookupMap;
        if (map == null) {
            /* Throwing RNUnavailableException should cause a retry */
            throw new RNUnavailableException(
                                "Table metadata is not yet initialized");
        }
        return map.get(tableId);
    }

    /**
     * Gets the table instance for the specified table name. Returns null if no
     * table with that name is in the metadata.
     *
     * @param tableName a table name
     * @return the table instance or null
     */
    TableImpl getTable(String tableName) {
        final TableMetadata md = getTableMetadata();

        return (md == null) ? null : md.getTable(tableName);
    }

    /**
     * Gets a r2-compat table instance for the specified table name. Returns
     * null if no  r2-compat table with that name is defined.
     *
     * @param tableName a table name
     * @return the table instance or null
     */
    public TableImpl getR2CompatTable(String tableName) {
        final Map<String, Long> map = r2NameIdLookupMap;
        if (map == null) {
            /* Throwing RNUnavailableException should cause a retry */
            throw new RNUnavailableException(
                                "Table metadata is not yet initialized");
        }
        final Long tableId = map.get(tableName);
        return tableId == null ? null : getTable(tableId);
    }

    /**
     * Gets the params used to construct this instance.
     */
    Params getParams() {
        return params;
    }

    /**
     * Gets the secondaryInfoMap for read-only. Returns null if there was an
     * error.
     */
    private SecondaryInfoMap getSecondaryInfoMap(ReplicatedEnvironment repEnv) {
        try {
            return SecondaryInfoMap.fetch(repEnv);
        } catch (RuntimeException re) {
            DatabaseUtils.handleException(re, logger, "index populate info");
        }
        return null;
    }

    /* -- Secondary DB methods -- */

    /**
     * Gets the secondary database of the specified name. Returns null if the
     * secondary database does not exist.
     *
     * @param dbName the name of a secondary database
     * @return a secondary database or null
     */
    SecondaryDatabase getSecondaryDb(String dbName) {
        return secondaryDbMap.get(dbName);
    }

    /**
     * Gets the secondary database for the specified index. Returns null if the
     * secondary database does not exist.
     *
     * @param indexName the index name
     * @param tableName the table name
     * @return a secondary database or null
     */
    public SecondaryDatabase getIndexDB(String indexName, String tableName) {
        return getSecondaryDb(createDbName(indexName, tableName));
    }

    /**
     * Closes all secondary DB handles.
     */
    @Override
    public synchronized void closeDbHandles() {
        logger.log(Level.INFO, "Closing secondary database handles");

        threadLock.lock();
        try {
            stopUpdateThread();
            stopMaintenanceThread();

            final Iterator<SecondaryDatabase> itr =
                                        secondaryDbMap.values().iterator();
            while (itr.hasNext()) {
                closeSecondaryDb(itr.next());
                itr.remove();
            }
        } finally {
            threadLock.unlock();
            super.closeDbHandles();
        }
    }

    /**
     * Closes the specified secondary DB. Returns true if the close was
     * successful or db is null.
     *
     * @param db secondary database to close
     * @return true if successful or db is null
     */
    private boolean closeSecondaryDb(SecondaryDatabase db) {
        return (db == null) ? true :
                              TxnUtil.close(logger, db, "secondary") == null;
    }

    /* -- From SecondaryAssociation -- */

    @Override
    public boolean isEmpty() {

        /*
         * This method is called on every operation. It must be as fast
         * as possible.
         */
        final Map<String, TableEntry> map = secondaryLookupMap;
        if (map == null) {
            /* Throwing RNUnavailableException should cause a retry */
            throw new RNUnavailableException(
                                    "Table metadata is not yet initialized");
        }
        return map.isEmpty();
    }

    @Override
    public Database getPrimary(DatabaseEntry primaryKey) {
        return repNode.getPartitionDB(primaryKey.getData());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<SecondaryDatabase>
        getSecondaries(DatabaseEntry primaryKey) {

        /*
         * This is not synchronized so the map may have nulled since isEmpty()
         * was called.
         */
        final Map<String, TableEntry> map = secondaryLookupMap;
        if (map == null) {
            /* Throwing RNUnavailableException should cause a retry */
            throw new RNUnavailableException(
                                "Table metadata is not yet initialized");
        }

        /* Start by looking up the top level table for this key */
        final BinaryKeyIterator keyIter =
                                    new BinaryKeyIterator(primaryKey.getData());

        /* The first element of the key will be the top level table */
        final String rootId = keyIter.next();
        final TableEntry entry = map.get(rootId);

        /* The entry could be null if the table doesn't have indexes */
        if (entry == null) {
            return Collections.EMPTY_SET;
        }

        /* We have a table with indexes, match the rest of the key. */
        final Collection<String> matchedIndexes = entry.matchIndexes(keyIter);

        /* This could be null if the key did not match any table with indexes */
        if (matchedIndexes == null) {
            return Collections.EMPTY_SET;
        }
        final List<SecondaryDatabase> secondaries =
                        new ArrayList<>(matchedIndexes.size());

        /*
         * Get the DB for each of the matched DB. Opening of the DBs is async
         * so we may not be able to complete the list. In this case thow an
         * exception and hopefully the operation will be retried.
         */
        for (String dbName : matchedIndexes) {
            final SecondaryDatabase db = secondaryDbMap.get(dbName);
            if (db == null) {
                /* Throwing RNUnavailableException should cause a retry */
                throw new RNUnavailableException("Secondary db not yet opened "+
                                                 dbName);
            }
            secondaries.add(db);
        }
        return secondaries;
    }

    /* -- Metadata update methods -- */

    /**
     * Updates the table metadata with the specified metadata object. Returns
     * true if the update is successful.
     *
     * @param newMetadata a new metadata
     * @return true if the update is successful
     */
    public synchronized boolean updateMetadata(Metadata<?> newMetadata) {
        if (!(newMetadata instanceof TableMetadata)) {
            throw new IllegalStateException("Bad metadata?" + newMetadata);
        }

        /* If no env, or not a master then we can't do an update */
        final ReplicatedEnvironment repEnv = repNode.getEnv(1);
        if ((repEnv == null) || !repEnv.getState().isMaster()) {
            return false;
        }

        final TableMetadata md = getTableMetadata();

        /* Can't update if we can't read it */
        if (md == null) {
            return false;
        }
        /* If the current md is up-to-date or newer, exit */
        if (md.getSequenceNumber() >= newMetadata.getSequenceNumber()) {
            return true;
        }
        logger.log(Level.INFO, "Updating table metadata with {0}", newMetadata);
        return update((TableMetadata)newMetadata, repEnv);
    }

    /**
     * Updates the table metadata with the specified metadata info object.
     * Returns the sequence number of the table metadata at the end of the
     * update.
     *
     * @param metadataInfo a table metadata info object
     * @return the post update sequence number of the table metadata
     */
    public synchronized int updateMetadata(MetadataInfo metadataInfo) {
        final ReplicatedEnvironment repEnv = repNode.getEnv(1);

        /* If no env or error then report back that we don't have MD */
        if (repEnv == null) {
            return Metadata.EMPTY_SEQUENCE_NUMBER;
        }
        final TableMetadata md = getTableMetadata();
        if (md == null) {
            return Metadata.EMPTY_SEQUENCE_NUMBER;
        }

        /* Only a master can actually update the metadata */
        if (repEnv.getState().isMaster()) {
            final TableMetadata newMetadata = md.getCopy();
            try {
                if (newMetadata.update(metadataInfo)) {
                    logger.log(Level.INFO, "Updating table metadata with {0}",
                               metadataInfo);
                    if (update(newMetadata, repEnv)) {
                        return newMetadata.getSequenceNumber();
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING,
                           "Error updating table metadata with " +
                           metadataInfo, e);
            }
        } else {
            logger.log(Level.INFO, "Metadata update attempted at replica");
        }
        logger.log(Level.INFO, "Metadata update failed, current seq number {0}",
                   md.getSequenceNumber());
        /* Update failed, return the current seq # */
        return md.getSequenceNumber();
    }

    /**
     * Persists the specified table metadata and updates the secondary DB
     * handles. The metadata is persisted only if this node is the master.
     *
     * @return true if the update was successful, or false if in shutdown or
     * unable to restart text index feeder.
     */
    private boolean update(final TableMetadata newMetadata,
                           ReplicatedEnvironment repEnv) {
        assert Thread.holdsLock(this);
        assert repEnv != null;

        /* Only the master can persistMetadata the metadata */
        if (repEnv.getState().isMaster()) {
            if (!persistMetadata(newMetadata)) {
                return true;    /* In shutdown */
            }
        }

        /*
         * The TIF needs to see the new metadata in the RepNode when it
         * restarts, so we set it now; however we must save the old metadata in
         * case the TIF metadata update fails in some way, so that it can be
         * restored.  The success or failure of a metadata update is ultimately
         * determined by whether the new metadata was installed, so if there
         * was a failure we want to make sure we don't return from here with
         * the new metadata installed.
         */
        final TableMetadata oldMetadata = tableMetadata;

        /* Tentatively update the cached metadata. */
        tableMetadata = newMetadata;

        /* Notify the text index feeder, if there is one. */
        TextIndexFeederManager tifm = repNode.getTextIndexFeederManager();
        if (tifm != null) {
            try {
                tifm.newTableMetadata(oldMetadata, newMetadata);
            } catch (Exception e) {
                tableMetadata = oldMetadata; /* update failed, undo */
                throw e;
            }
        }

        /* Finally, update the DB handles */
        updateDbHandles(repEnv);

        return true;
    }

    /**
     * Updates the secondary database handles based on the current table
     * metadata. Update of the handles is done asynchronously. If an update
     * thread is already running it is stopped and a new thread is started.
     *
     * This is called by the RN when 1) the handles have to be renewed due to
     * an environment change 2) when the topology changes, and 3) when the
     * table metadata is updated.
     *
     * The table maps are also set.
     *
     * @param repEnv the replicated environment handle
     *
     * @return true if table metadata handle needed to be updated.
     */
    @Override
    public synchronized boolean updateDbHandles(ReplicatedEnvironment repEnv) {
        final boolean refresh = super.updateDbHandles(repEnv);

        final TableMetadata tableMd = getTableMetadata();
        boolean succeeded = updateTableMaps(tableMd, repEnv);

        threadLock.lock();
        try {
            stopUpdateThread();

            /*
             * If the handles are being refreshed, or the update failed,
             * or we are not the master, shutdown the maintenance thread.
             */
            if (refresh || !succeeded || !repEnv.getState().isMaster()) {
                stopMaintenanceThread();
            }
            if (!succeeded) {
                return refresh;
            }
            updateThread = new UpdateThread(tableMd, repEnv);
            updateThread.start();
        } finally {
            threadLock.unlock();
        }
        return refresh;
    }

    /**
     * Rebuilds the secondaryLookupMap and idLookupMap maps. Returns true if
     * the update was successful. If false is returned the table maps have been
     * set such that operations will be disabled.
     *
     * @return true if the update was successful
     */
    private synchronized boolean updateTableMaps(TableMetadata tableMd,
                                                 ReplicatedEnvironment repEnv) {
        assert repEnv != null;

        /*
         * If env is invalid, or tableMD null, disable ops
         */
        if (!repEnv.isValid() || (tableMd == null)) {
            secondaryLookupMap = null;
            idLookupMap = null;
            r2NameIdLookupMap = null;
            return false;
        }

        /*
         * If empty, then a quick return. Note that we return true so that
         * the update thread runs because there may have been tables/indexes
         * that need cleaning.
         */
        if (tableMd.isEmpty()) {
            secondaryLookupMap = Collections.emptyMap();
            idLookupMap = Collections.emptyMap();
            r2NameIdLookupMap = Collections.emptyMap();
            return true;
        }

        // TODO - optimize if the MD has not changed?

        final Map<String, TableEntry> slm = new HashMap<>();
        final Map<Long, TableImpl> ilm = new HashMap<>();
        final Map<String, Long> r2nilm = new HashMap<>();

        for (Table table : tableMd.getTables().values()) {
            final TableImpl tableImpl = (TableImpl)table;

            /*
             * Add an entry for each table that has indexes somewhere in its
             * hierarchy.
             */
            final TableEntry entry = new TableEntry(tableImpl);

            if (entry.hasSecondaries()) {
                slm.put(tableImpl.getIdString(), entry);
            }

            /*
             * The id map has an entry for each table, so descend into its
             * hierarchy.
             */
            addToMap(tableImpl, ilm, r2nilm);
        }
        secondaryLookupMap = slm;
        idLookupMap = ilm;
        r2NameIdLookupMap = r2nilm;
        return true;
    }

    private void addToMap(TableImpl tableImpl,
                          Map<Long, TableImpl> map,
                          Map<String, Long> r2Map) {
        map.put(tableImpl.getId(), tableImpl);
        if (tableImpl.isR2compatible()) {
            r2Map.put(tableImpl.getFullName(), tableImpl.getId());
        }
        for (Table child : tableImpl.getChildTables().values()) {
            addToMap((TableImpl)child, map, r2Map);
        }
    }

    /**
     * Starts the state tracker
     * TODO - Perhaps start the tracker on-demand in noteStateChange()?
     */
    public void startTracker() {
        stateTracker.start();
    }

    /**
     * Notes a state change in the replicated environment. The actual
     * work to change state is made asynchronously to allow a quick return.
     */
    public void noteStateChange(StateChangeEvent stateChangeEvent) {
        stateTracker.noteStateChange(stateChangeEvent);
    }

    @Override
    protected MetadataType getType() {
        return MetadataType.TABLE;
    }

    /**
     * Refreshes the table metadata due to it being updated by the master.
     * Called from the database trigger.
     */
    @Override
    protected synchronized void update(ReplicatedEnvironment repEnv) {
        /*
         * This will force the tableMetadata to be re-read from the db (in
         * updateDBHandles)
         */
        tableMetadata = null;
        updateDbHandles(repEnv);
    }

    /**
     * Thread to manage replicated environment state changes.
     */
    private class TableManagerStateTracker extends StateTracker {
        TableManagerStateTracker(Logger logger) {
            super(TableManagerStateTracker.class.getSimpleName(),
                  repNode.getRepNodeId(), logger,
                  repNode.getExceptionHandler());
        }

        @Override
        protected void doNotify(StateChangeEvent sce) {

            final ReplicatedEnvironment repEnv = repNode.getEnv(1);
            if (repEnv == null) {
                return;
            }
            logger.log(Level.INFO, "Table manager change state to {0}",
                       sce.getState());

            Database infoDb = null;
            threadLock.lock();
            try {
                if (sce.getState().isMaster()) {
                    infoDb = SecondaryInfoMap.openDb(repEnv);
                    checkMaintenanceThreads(repEnv, infoDb);
                } else {
                    stopMaintenanceThread();
                }
            } catch (RuntimeException re) {
                // TODO - should this be fatal - if so need to separate non-fatal
                // database exceptions.
                logger.log(Level.SEVERE,
                           "Table manager state change failed", re);
            } finally {
                if (infoDb != null) {
                    TxnUtil.close(logger, infoDb, "secondary info db");
                }
                threadLock.unlock();
            }
        }
    }

    /**
     * A container class for quick lookup of secondary DBs.
     */
    private static class TableEntry {
        private final int keySize;
        private final Set<String> secondaries = new HashSet<>();
        private final Map<String, TableEntry> children = new HashMap<>();

        TableEntry(TableImpl table) {
            /* For child tables subtract the key count from parent */
            keySize = (table.getParent() == null ?
                       table.getPrimaryKeySize() :
                       table.getPrimaryKeySize() -
                       ((TableImpl)table.getParent()).getPrimaryKeySize());

            /* For each index, save the secondary DB name */
            for (Index index :
                table.getIndexes(Index.IndexType.SECONDARY).values()) {

                secondaries.add(createDbName(index.getName(),
                                             index.getTable().getFullName()));
            }

            /* Add only children which have indexes */
            for (Table child : table.getChildTables().values()) {
                final TableEntry entry = new TableEntry((TableImpl)child);

                if (entry.hasSecondaries()) {
                    children.put(((TableImpl)child).getIdString(), entry);
                }
            }
        }

        private boolean hasSecondaries() {
            return !secondaries.isEmpty() || !children.isEmpty();
        }

        private Collection<String> matchIndexes(BinaryKeyIterator keyIter) {
            /* Match up the primary keys with the input keys, in number only */
            for (int i = 0; i < keySize; i++) {
                /* If the key is short, then punt */
                if (keyIter.atEndOfKey()) {
                    return null;
                }
                keyIter.skip();
            }

            /* If both are done we have a match */
            if (keyIter.atEndOfKey()) {
                return secondaries;
            }

            /* There is another component, check for a child table */
            final String childId = keyIter.next();
            final TableEntry entry = children.get(childId);
            return (entry == null) ? null : entry.matchIndexes(keyIter);
        }
    }

    /**
     * Stops the update thread if one is running and waits for it to exit.
     */
    private void stopUpdateThread() {
        assert threadLock.isHeldByCurrentThread();

        if (updateThread != null) {
            updateThread.waitForStop();
            updateThread = null;
        }
    }

    /**
     * Stops the maintenance thread that may be running, waiting for it to exit.
     */
    private void stopMaintenanceThread() {
        assert threadLock.isHeldByCurrentThread();

        if (maintenanceThread != null) {
            maintenanceThread.waitForStop();
            maintenanceThread = null;
        }
    }

    /**
     * Checks whether a maintenance thread need to be started. This method
     * will attempt to get the threadLock. If the lock is unavailable, no checks
     * are made and false is returned.
     *
     * The priority of the maintenance threads is:
     *
     * 1. Populate
     * 2. Primary cleaning
     * 3. Secondary cleaning
     *
     * This method will stop a lower priority thread before starting a higher
     * priority thread.
     * Note that 1. Populate and 2. Primary cleaning are waited on by plans
     * and should not be needed at the same time.
     *
     * Any changes or additions to what constitutes maintenance must be
     * reflected in isBusyMaintenance().
     *
     * @return true if the check was successful
     */
    boolean checkMaintenanceThreads(ReplicatedEnvironment repEnv,
                                    Database infoDb) {
        assert infoDb != null;

        if (!repEnv.getState().isMaster()) {
            return true;
        }
        logger.fine("Checking maintenance threads");

        /*
         * We only try to obtain the lock to avoid deadlock.
         */
        if (!threadLock.tryLock()) {
            logger.fine("Failed to acquire maintenance thread lock");
            return false;
        }

        try {
            stopMaintenanceThread();

            final SecondaryInfoMap secondaryInfoMap =
                                                SecondaryInfoMap.fetch(infoDb);
            if (secondaryInfoMap == null) {
                return false;
            }

            if (secondaryInfoMap.secondaryNeedsPopulate()) {
                maintenanceThread =
                        new PopulateThread(this, repNode, repEnv, logger);
                maintenanceThread.start();
                return true;
            }

            if (secondaryInfoMap.tableNeedDeleting()) {
                maintenanceThread =
                        new PrimaryCleanerThread(this, repNode, repEnv, logger);
                maintenanceThread.start();
                return true;
            }

            if (secondaryInfoMap.secondaryNeedsCleaning()) {
                maintenanceThread =
                      new SecondaryCleanerThread(this, repNode, repEnv, logger);
                maintenanceThread.start();
                return true;
            }
        } finally {
            threadLock.unlock();
        }
        return true;
    }

    /**
     * Returns true if there is a pending table maintenance operation. Also
     * returns true if the determination cannot be made. Maintenance operations
     * include secondary DB population, table deletion, and secondary DB
     * cleaning and are started by checkMaintenanceThreads() above.
     *
     * @return true if there is a pending table maintenance operation
     */
    public boolean isBusyMaintenance() {
        final ReplicatedEnvironment repEnv = repNode.getEnv(1);

        /* If not the right env. we don't know so report busy to be safe */
        if ((repEnv == null) || !repEnv.getState().isMaster()) {
            return true;
        }

        Database infoDb = null;
        try {
            infoDb = SecondaryInfoMap.openDb(repEnv);

            final SecondaryInfoMap secondaryInfoMap =
                                                SecondaryInfoMap.fetch(infoDb);
            if (secondaryInfoMap == null) {
                return false;   /* Nothing going on */
            }

            /* If anything is pending, report busy */
            if (secondaryInfoMap.secondaryNeedsPopulate() ||
                secondaryInfoMap.tableNeedDeleting() ||
                secondaryInfoMap.secondaryNeedsCleaning()) {
                return true;
            }
            return false;
        } catch (RuntimeException re) {
            logger.log(Level.WARNING, "Unexpected exception", re);
        } finally {
            if (infoDb != null) {
                TxnUtil.close(logger, infoDb, "secondary info db");
            }
        }
        /* On error return true to be safe */
        return true;
    }

    /**
     * Checks if a new maintenance thread needs to be started. Called when a
     * running maintenance thread is finished and about to exit.
     */
    void maintenanceThreadExit(ReplicatedEnvironment repEnv, Database infoDb) {
        assert maintenanceThread != null;

        /*
         * Only try to obtain lock to prevent deadlocks. Note that if
         * someone else has the lock, then things are stopping or there
         * is an update, which will check for maintenance threads.
         */
        if (!threadLock.tryLock()) {
            return;
        }
        try {
            if (!maintenanceThread.isStopped()) {
                return;
            }

            /*
             * Clear the reference to the current thread to prevent
             * checkMaintenanceThreads() from waiting for it to exit.
             */
            maintenanceThread = null;
            checkMaintenanceThreads(repEnv, infoDb);
        } finally {
            threadLock.unlock();
        }
    }

    public void notifyRemoval(PartitionId partitionId) {
        if (secondaryDbMap.isEmpty()) {
            return;
        }
        logger.log(Level.INFO, "{0} has been removed, removing obsolete " +
                   "records from secondaries", partitionId);

        final ReplicatedEnvironment repEnv = repNode.getEnv(1);

        if (repEnv == null) {
            return; // TODO - humm - lost info?
        }
        /* This will call back here to start the primary cleaner thread */
        SecondaryInfoMap.markForSecondaryCleaning(this, repEnv, logger);
    }

    @Override
    public String toString() {
        return "TableManager[" +
               ((tableMetadata == null) ? "-" :
                                          tableMetadata.getSequenceNumber()) +
               ", " + secondaryDbMap.size() + "]";
    }

    /**
     * Thread to update the secondary database handles. When run, the table
     * metadata is scanned looking for indexes. Each index will have a
     * corresponding secondary DB. If the index is new, (and this is the master)
     * a new secondary DB is created. Populating the new secondary DB is done
     * in a separate thread.
     *
     * The table MD is also checked for changes, such as indexes dropped and
     * tables that need their data deleted.
     */
    class UpdateThread extends Thread {

        final TableMetadata tableMd;
        final ReplicatedEnvironment repEnv;

        Database infoDb = null;

        private volatile boolean stop = false;

        UpdateThread(TableMetadata tableMd, ReplicatedEnvironment repEnv) {
            super("KV secondary handle updater");
            assert repEnv != null;
            assert tableMd != null;
            this.tableMd = tableMd;
            this.repEnv = repEnv;
            setDaemon(true);
            setUncaughtExceptionHandler(repNode.getExceptionHandler());
        }

        @Override
        public void run() {
            logger.log(Level.FINE, "Starting {0}", this);
            try {
                /* Retry as long as there are errors */
                while (update()) {
                    try {
                        Thread.sleep(DB_OPEN_RETRY_MS);
                    } catch (InterruptedException ie) {
                        /* Should not happen. */
                        throw new IllegalStateException(ie);
                    }
                }

                /* If infoDB == null, update() exited early */
                if (infoDb == null) {
                    return;
                }

                /* Check to see if any maintenance threads can be started. */
                while (!isStopped() && repEnv.getState().isMaster()) {

                    if (checkMaintenanceThreads(repEnv, infoDb)) {
                        return;
                    }
                    try {
                        Thread.sleep(DB_OPEN_RETRY_MS);
                    } catch (InterruptedException ie) {
                        /* Should not happen. */
                        throw new IllegalStateException(ie);
                    }
                }
            } catch (RuntimeException re) {

                /*  If stopped or bad env just log and exit. */
                if (isStopped()) {
                    logger.log(Level.INFO,
                               "Table update thread exiting after, {0}", re);
                    return;
                }
                throw re;
            } finally {
                if (infoDb != null) {
                    TxnUtil.close(logger, infoDb, "secondary info db");
                }
            }
        }

        /**
         * Updates the partition database handles.
         *
         * @return true if there was an error and the update should be retried
         */
        private boolean update() {

            if (isStopped()) {
                return false;
            }
            logger.log(Level.FINE,
                       "Establishing secondary database handles, " +
                       "table seq#: {0}",
                       tableMd.getSequenceNumber());

            /*
             * Get the populate info DB.
             */
            if (infoDb == null) {
                try {
                    infoDb = SecondaryInfoMap.openDb(repEnv);
                } catch (Exception e) {
                    logger.log(Level.INFO, "Failed to open info map DB", e);
                    return !isStopped();
                }
            }
            assert infoDb != null;

            /*
             * Map of seondary DB name -> indexes that are defined in the table
             * metadata. This is used to determine what databases to open and if
             * an index has been dropped.
             */
            final Map<String, IndexImpl> indexes = new HashMap<>();

            /*
             * Set tables which are being removed but first need their data
             * deleted.
             */
            final Set<TableImpl> deletedTables = new HashSet<>();

            for (Table table : tableMd.getTables().values()) {
                scanTable((TableImpl)table, indexes, deletedTables);
            }
            logger.log(Level.INFO, "Found {0} indexes and {1} tables marked " +
                       "for deletion in {2}",
                       new Object[]{indexes.size(), deletedTables.size(),
                                    tableMd});

            /*
             * Update the secondary map with any indexes that have been dropped
             * and removed tables. Changes to the secondary map can only be
             * made by the master. This call will also remove the database.
             * This call will not affect secondaryDbMap which is dealt with
             * below.
             */
            if (repEnv.getState().isMaster()) {
                SecondaryInfoMap.check(tableMd.getTables(), indexes,
                                       deletedTables, this, logger);
            }

            /*
             * Remove entries from secondaryDbMap for dropped indexes.
             * The call to SecondaryInfoMap.check above will close the DB
             * when the master, so this will close the DB on the replica.
             */
            final Iterator<Entry<String, SecondaryDatabase>> itr =
                                           secondaryDbMap.entrySet().iterator();

            while (itr.hasNext()) {
                if (isStopped()) {
                    return false;
                }
                final Entry<String, SecondaryDatabase> entry = itr.next();
                final String dbName = entry.getKey();

                if (!indexes.containsKey(dbName)) {
                    if (closeSecondaryDb(entry.getValue())) {
                        itr.remove();
                    }
                }
            }

            int errors = 0;

            /*
             * For each index open its secondary DB
             */
            for (Entry<String, IndexImpl> entry : indexes.entrySet()) {

                /* Exit if the updater has been stopped or the env is bad */
                if (isStopped()) {
                    logger.log(Level.INFO,
                               "Update terminated, established {0} " +
                               "secondary database handles",
                               secondaryDbMap.size());
                    return false;   // Will cause thread to exit
                }

                final String dbName = entry.getKey();
                SecondaryDatabase db = secondaryDbMap.get(dbName);
                try {
                    if (DatabaseUtils.needsRefresh(db, repEnv)) {
                        final IndexImpl index = entry.getValue();
                        db = openSecondaryDb(dbName, index);
                        assert db != null;
                        setIncrementalPopulation(dbName, db);
                        secondaryDbMap.put(dbName, db);
                    } else {
                        setIncrementalPopulation(dbName, db);
                        updateIndexKeyCreator(db, entry.getValue());
                    }
                } catch (RuntimeException re) {

                    /*
                     * If a DB was opened, and there was some other error (like
                     * not getting the info record), close and remove the DB
                     * from the map.
                     */
                    if (db != null) {
                        secondaryDbMap.remove(dbName);
                        TxnUtil.close(logger, db, "secondary db");
                    }

                    if (re instanceof IllegalCommandException) {
                        logger.log(Level.WARNING, "Failed to open database " +
                                   "for {0}. {1}",
                                   new Object[] {dbName, re.getMessage()});
                        continue;
                    }

                    if (DatabaseUtils.handleException(re, logger, dbName)) {
                        errors++;
                    }
                }
            }

            /*
             * If there have been errors return true (unless the update has been
             * stopped) which will cause the update to be retried.
             */
            if (errors > 0) {
                logger.log(Level.INFO,
                           "Established {0} secondary database handles, " +
                           "will retry in {1}ms",
                           new Object[] {secondaryDbMap.size(),
                                         DB_OPEN_RETRY_MS});
                return !isStopped();
            }
            logger.log(Level.INFO, "Established {0} secondary database handles",
                       secondaryDbMap.size());

            return false;   // Done
        }

        /*
         * Update the index in the secondary's key creator.
         */
        private void updateIndexKeyCreator(SecondaryDatabase db,
                                           IndexImpl index) {
            logger.log(Level.FINE,
                       "Updating index metadata for index {0} in table {1}",
                       new Object[]{index.getName(),
                                    index.getTable().getFullName()});
            final SecondaryConfig config = db.getConfig();
            final IndexKeyCreator keyCreator = (IndexKeyCreator)
                        (index.isMultiKey() ? config.getMultiKeyCreator() :
                                              config.getKeyCreator());
            assert keyCreator != null;
            keyCreator.setIndex(index);
        }

        /*
         * Set the incremental populate mode on the secondary DB.
         */
        private void setIncrementalPopulation(String dbName,
                                              SecondaryDatabase db) {
            final SecondaryInfo info =
                        SecondaryInfoMap.fetch(infoDb).getSecondaryInfo(dbName);

            if (info == null) {
                throw new IllegalStateException("Secondary info record for " +
                                                dbName + " is missing");
            }

            if (info.needsPopulating()) {
                db.startIncrementalPopulation();
            } else {
                db.endIncrementalPopulation();
            }
        }

        /*
         * Check if the specified table has any indexes or if the table is
         * deleted (and needs it data deleted). If it has indexes, add them to
         * indexes map. If the table is marked for deletion add that to the
         * deleteingTables map.
         *
         * If the table has children, recursively check those.
         */
        private void scanTable(TableImpl table,
                               Map<String, IndexImpl> indexes,
                               Set<TableImpl> deletedTables) {

            if (table.getStatus().isDeleting()) {

                // TODO - should we check for consistency? If so, exception?
                if (!table.getChildTables().isEmpty()) {
                    throw new IllegalStateException("Table " + table +
                                                " is deleted but has children");
                }
                if (!table.getIndexes(Index.IndexType.SECONDARY).isEmpty()) {
                    throw new IllegalStateException("Table " + table +
                                                 " is deleted but has indexes");
                }
                deletedTables.add(table);
                return;
            }
            for (Table child : table.getChildTables().values()) {
                scanTable((TableImpl)child, indexes, deletedTables);
            }

            for (Index index :
            	table.getIndexes(Index.IndexType.SECONDARY).values()) {

                indexes.put(createDbName(index.getName(),
                                         table.getFullName()),
                            (IndexImpl)index);
            }
        }

        /**
         * Opens the specified secondary database.
         */
        private SecondaryDatabase openSecondaryDb(String dbName,
                                                  IndexImpl index) {
            logger.log(Level.FINE, "Open secondary DB {0}", dbName);

            final IndexKeyCreator keyCreator = new IndexKeyCreator(index);

            /*
             * Use NO_CONSISTENCY so that the handle establishment is not
             * blocked trying to reach consistency particularly when the env is
             * in the unknown state and we want to permit read access.
             */
            final TransactionConfig txnConfig = new TransactionConfig().
               setConsistencyPolicy(NoConsistencyRequiredPolicy.NO_CONSISTENCY);

            final SecondaryConfig dbConfig = new SecondaryConfig();
            dbConfig.setExtractFromPrimaryKeyOnly(keyCreator.primaryKeyOnly()).
                     setSecondaryAssociation(TableManager.this).
                     setTransactional(true).
                     setAllowCreate(true).
                     setDuplicateComparator(Key.BytesComparator.class).
                     setSortedDuplicates(true);

            if (keyCreator.isMultiKey()) {
                dbConfig.setMultiKeyCreator(keyCreator);
            } else {
                dbConfig.setKeyCreator(keyCreator);
            }

            Transaction txn = null;
            try {
                txn = repEnv.beginTransaction(null, txnConfig);
                final SecondaryDatabase db =
                      repEnv.openSecondaryDatabase(txn, dbName, null, dbConfig);

                /*
                 * If we are the master, add the info record for this secondary.
                 */
                if (repEnv.getState().isMaster()) {
                    SecondaryInfoMap.add(dbName, infoDb, txn, logger);
                }
                txn.commit();
                txn = null;
                return db;
            } catch (IllegalStateException e) {

                /*
                 * The exception was most likely thrown because the environment
                 * was closed.  If it was thrown for another reason, though,
                 * then invalidate the environment so that the caller will
                 * attempt to recover by reopening it.
                 */
                if (repEnv.isValid()) {
                    EnvironmentFailureException.unexpectedException(
                        DbInternal.getEnvironmentImpl(repEnv), e);
                }
                throw e;

            } finally {
               TxnUtil.abort(txn);
            }
        }

        /*
         * Returns true if the thread has been stopped or the env has been
         * invalidated.
         */
        boolean isStopped() {
            return stop || !repEnv.isValid();
        }

        /**
         * Stops the updater and waits for the thread to exit.
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
         * Closes the specified secondary database. Returns true if the close
         * succeeded, or the database was not open.
         *
         * @param dbName the name of the secondary DB to close
         * @return true on success
         */
        boolean closeSecondary(String dbName) {
            if (isStopped()) {
                return false;
            }
            return closeSecondaryDb(secondaryDbMap.get(dbName));
        }
    }
}
