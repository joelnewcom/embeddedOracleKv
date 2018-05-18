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

package oracle.kv.impl.admin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sleepycat.bind.EntityBinding;
import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

import oracle.kv.impl.admin.AdminDatabase.DB_TYPE;
import oracle.kv.impl.admin.AdminStores.AdminStore;
import oracle.kv.impl.admin.AdminStores.AdminStoreCursor;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.admin.plan.Plan;
import oracle.kv.impl.admin.plan.Planner;
import oracle.kv.impl.util.SerializationUtil;

/**
 * A base class for implementations of the plan store in Admin.
 */
public abstract class PlanStore extends AdminStore {

    private static final int MAXPLANS = 10;

    /*
     * We use non-sticky cursors here to obtain a slight performance advantage
     * and to run in a deadlock-free mode.
     */
    static final CursorConfig CURSOR_READ_COMMITTED =
        new CursorConfig().setNonSticky(true).setReadCommitted(true);

    public static PlanStore getReadOnlyInstance(Logger logger,
                                                 Environment env) {
        return new PlanDatabaseStore(logger, env, true);
    }

    /**
     * Creates a PlanStore instance according to the schema version.  If the
     * schema version is earlier than V4, a DPL-based plan store will be
     * returned for compatibility. Otherwise, a JE HA database-based plan
     * store will be returned.
     */
    static PlanStore getStoreByVersion(int schemaVersion,
                                       Admin admin, EntityStore eStore) {
        /*
         * The plan store was migrated back at version 4.
         */
        if (schemaVersion < AdminSchemaVersion.SCHEMA_VERSION_4) {
            assert eStore != null;
            return new PlanDPLStore(admin.getLogger(), eStore);
        }
        return new PlanDatabaseStore(admin.getLogger(), admin.getEnv(),
                                     false /* read only */);
    }

    private PlanStore(Logger logger) {
        super(logger);
    }

    /**
     * Persists a plan into the store. Callers are responsible for
     * exception handling.
     *
     * The method must be called while synchronized on the plan instance
     * to ensure that the plan instance is not modified while the object is
     * being serialized into bytes before being stored into the database. Note
     * that the synchronization hierarchy requires that no other JE locks are
     * held before the mutex is acquired, so the caller to this method must be
     * careful. The synchronization is done explicitly by the caller, rather
     * than making this method synchronized, to provide more flexibility for
     * obeying the synchronization hierarchy.
     *
     * @param txn the transaction in progress
     */
    abstract void put(Transaction txn, Plan plan);

    /**
     * Fetches a plan from the store. Callers are responsible for
     * exception handling.
     */
    abstract Plan get(Transaction txn, int planId);

    /**
     * Returns a cursor for iterating all plans in the store.  Callers are
     * responsible for exception handling, and should close the cursor via
     * {@link PlanCursor#close} after use.
     */
    public abstract PlanCursor getPlanCursor(Transaction txn,
                                             Integer startPlanId);

    /**
     * Fetches all non-terminal Plans in the given EntityStore as a Map.
     */
    Map<Integer, Plan> getActivePlans(Transaction txn,
                                      Planner planner,
                                      AdminServiceParams aServiceParams) {
        final Map<Integer, Plan> activePlans = new HashMap<>();

        try (final PlanCursor cursor = getPlanCursor(txn, null)) {
            for (Plan p = cursor.first();
                 p != null;
                 p = cursor.next()) {

                if (!p.getState().isTerminal()) {
                    p.initializePlan(planner, aServiceParams);
                    activePlans.put(new Integer(p.getId()), p);
                }
            }
        }
        return activePlans;
    }

    /**
     * Retrieve the beginning plan id and number of plans that satisfy the
     * request.
     *
     * Returns an array of two integers indicating a range of plan id
     * numbers. [0] is the first id in the range, and [1] number of
     * plan ids in the range.
     *
     * Operates in three modes:
     *
     *    mode A requests howMany plans ids following startTime
     *    mode B requests howMany plans ids preceding endTime
     *    mode C requests a range of plan ids from startTime to endTime.
     *
     *    mode A is signified by endTime == 0
     *    mode B is signified by startTime == 0
     *    mode C is signified by neither startTime nor endTime being == 0.
     *        howMany is ignored in mode C.
     *
     * If the owner is not null, only plans with the specified owner will be
     * returned.
     */
    int[] getPlanIdRange(Transaction txn,
                         long startTime,
                         long endTime,
                         int howMany,
                         String ownerId) {
        final int[] range = {0, 0};
        final PlanCursor cursor = getPlanCursor(txn, null /* startPlanId */);

        int n = 0;
        try {
            if (startTime == 0L) {
                /* This is mode B. */
                for (Plan p = cursor.last();
                     p != null && n < howMany;
                     p = cursor.prev()) {

                    if (ownerId != null) {
                        final String planOwnerId =
                            p.getOwner() == null ? null : p.getOwner().id();
                        if (!ownerId.equals(planOwnerId)) {
                            continue;
                        }
                    }

                    long creation = p.getCreateTime().getTime();
                    if (creation < endTime) {
                        n++;
                        range[0] = p.getId();
                    }
                }
                range[1] = n;
            } else {
                for (Plan p = cursor.first();
                     p != null;
                     p = cursor.next()) {

                    if (ownerId != null) {
                        final String planOwnerId =
                            p.getOwner() == null ? null : p.getOwner().id();
                        if (!ownerId.equals(planOwnerId)) {
                            continue;
                        }
                    }

                    long creation = p.getCreateTime().getTime();
                    if (creation >= startTime) {
                        if (range[0] == 0) {
                            range[0] = p.getId();
                        }
                        if (endTime != 0L && creation > endTime) {
                            /* Mode C */
                            break;
                        }
                        if (howMany != 0 && n >= howMany) {
                            /* Mode A */
                            break;
                        }
                        n++;
                    }
                }
                range[1] = n;
            }
        } finally {
            cursor.close();
        }

        return range;
    }

    /**
     * Returns a map of plans starting at firstPlanId.  The number of plans in
     * the map is the lesser of howMany, MAXPLANS, or the number of extant
     * plans with id numbers following firstPlanId.  The range is not
     * necessarily fully populated; while plan ids are mostly sequential, it is
     * possible for values to be skipped. If the owner is not null, only plans
     * with the specified owner will be returned.
     */
    Map<Integer, Plan> getPlanRange(Transaction txn,
                                    Planner planner,
                                    AdminServiceParams aServiceParams,
                                    int firstPlanId,
                                    int howMany,
                                    String ownerId) {
        if (howMany > MAXPLANS) {
            howMany = MAXPLANS;
        }

        final Map<Integer, Plan> fetchedPlans = new HashMap<>();

        try (final PlanCursor cursor = getPlanCursor(txn, firstPlanId)) {
            for (Plan p = cursor.first();
                 p != null && howMany > 0;
                 p = cursor.next()) {

                if (ownerId != null) {
                    final String planOwnerId =
                        p.getOwner() == null ? null : p.getOwner().id();
                    if (!ownerId.equals(planOwnerId)) {
                        continue;
                    }
                }

                p.initializePlan(planner, aServiceParams);
                p.stripForDisplay();
                fetchedPlans.put(new Integer(p.getId()), p);
                howMany--;
            }
        }
        return fetchedPlans;
    }

    /**
     * Returns the Plan corresponding to the given id,
     * fetched from the database; or null if there is no corresponding plan.
     */
    Plan getPlanById(int id,
                     Transaction txn,
                     Planner planner,
                     AdminServiceParams aServiceParams) {
        final Plan p = get(txn, id);

        if (p != null) {
            p.initializePlan(planner, aServiceParams);
        }
        return p;
    }

    /**
     * Returns the <i>howMany</i> most recent plans in the plan history.
     * The plan instance returned will be stripped of memory intensive
     * components and will not be executable.
     */
    @Deprecated
    Map<Integer, Plan> getRecentPlansForDisplay(
                                            int howMany,
                                            Transaction txn,
                                            Planner planner,
                                            AdminServiceParams aServiceParams) {
        final Map<Integer, Plan> fetchedPlans = new HashMap<>();

        try (final PlanCursor cursor = getPlanCursor(txn, null)) {
            int n = 0;
            for (Plan p = cursor.last();
                 p != null && n < howMany;
                 p = cursor.prev(), n++) {

                p.initializePlan(planner, aServiceParams);
                p.stripForDisplay();
                fetchedPlans.put(new Integer(p.getId()), p);
            }
        }
        return fetchedPlans;
    }

    void logPersisting(Plan plan) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Storing plan {0}", plan.getId());
        }
    }

    void logFetching(int planId) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Fetching plan using id {0}", planId);
        }
    }

    /**
     * A plan store using the non-DPL
     * {@link oracle.kv.impl.admin.AdminPlanDatabase} as the underlying storage.
     */
    private static class PlanDatabaseStore extends PlanStore {
        private final AdminDatabase<Integer, Plan> planDb;

        private PlanDatabaseStore(Logger logger, Environment env,
                                  boolean readOnly) {
            super(logger);
            planDb = new AdminDatabase<Integer, Plan>(DB_TYPE.PLAN, logger,
                                                      env, readOnly) {
                @Override
                protected DatabaseEntry keyToEntry(Integer key) {
                    final DatabaseEntry keyEntry = new DatabaseEntry();
                    IntegerBinding.intToEntry(key, keyEntry);
                    return keyEntry;
                }};
        }

        @Override
        void put(Transaction txn, Plan plan) {
            /* Plan is using the id field as the primary key. */
            planDb.put(txn, plan.getId(), plan);
            logPersisting(plan);
        }

        @Override
        Plan get(Transaction txn, int planId) {
            logFetching(planId);
            return planDb.get(txn, planId, LockMode.READ_COMMITTED, Plan.class);
        }

        @Override
        public PlanCursor getPlanCursor(Transaction txn,
                                           Integer startPlanId) {
            final Cursor cursor = planDb.openCursor(txn);
            return new PlanCursor(cursor, startPlanId) {

                @Override
                protected Plan entryToObject(DatabaseEntry key,
                                             DatabaseEntry value) {
                    return SerializationUtil.getObject(value.getData(),
                                                       Plan.class);
                }
            };
        }

        @Override
        public void close() {
            planDb.close();
        }

        @Override
        protected void convertTo(int existingVersion, AdminStore newStore,
                                 Transaction txn) {
            /*
             * Plans were converted at schema version 4, which means that
             * this method may be called when the rest of the data is moved
             * over. If not V4 then call super which will throw an exception.
             */
            if (existingVersion != AdminSchemaVersion.SCHEMA_VERSION_4) {
                super.convertTo(existingVersion, newStore, txn);
            }
        }
    }

    /**
     * A provisional plan store used when not all the nodes in the store have
     * been upgraded to R3.1.0 or later.  The underlying storage is the
     * EntityStore.
     */
    private static class PlanDPLStore extends PlanStore {
        private final EntityStore eStore;

        private PlanDPLStore(Logger logger, EntityStore eStore) {
            super(logger);
            this.eStore = eStore;
        }

        @Override
        public void put(Transaction txn, Plan plan) {
            readOnly();
        }

        @Override
        Plan get(Transaction txn, int planId) {
            logFetching(planId);
            final PrimaryIndex<Integer, AbstractPlan> pi =
                eStore.getPrimaryIndex(Integer.class, AbstractPlan.class);
            return pi.get(txn, new Integer(planId), LockMode.READ_COMMITTED);
        }

        @Override
        public PlanCursor getPlanCursor(Transaction txn, Integer startPlanId) {
            final PrimaryIndex<Integer, AbstractPlan> pi;
            pi = eStore.getPrimaryIndex(Integer.class, AbstractPlan.class);

            final Cursor cursor =
                pi.getDatabase().openCursor(txn, CURSOR_READ_COMMITTED);

            return new PlanCursor(cursor, startPlanId) {

                /*
                 * We need to use EntityBinding to convert a database entry
                 * to a plan object in DPL store.
                 */
                private final EntityBinding<AbstractPlan> planBinding =
                    eStore.getPrimaryIndex(Integer.class, AbstractPlan.class).
                        getEntityBinding();

                @Override
                protected Plan entryToObject(DatabaseEntry key,
                                             DatabaseEntry value) {
                    return planBinding.entryToObject(key, value);
                }
            };
        }

        @Override
        protected void convertTo(int existingVersion, AdminStore newStore,
                                 Transaction txn) {
            final PlanStore newPlanStore = (PlanStore)newStore;
            final PrimaryIndex<Integer, AbstractPlan> pi =
                eStore.getPrimaryIndex(Integer.class, AbstractPlan.class);

            try (final EntityCursor<AbstractPlan> cursor =
                                                    pi.entities(txn, null)) {
                for (AbstractPlan p : cursor) {
                    if (existingVersion < AdminSchemaVersion.SCHEMA_VERSION_3) {
                        p.upgradeToV3();
                    }
                    newPlanStore.put(txn, p);
                }
            }
        }
    }

    /**
     * A cursor class to facilitate the scan of the plan store.
     */
    public abstract static class PlanCursor
            extends AdminStoreCursor<Integer, Plan> {

        private PlanCursor(Cursor cursor,  Integer startKey) {
            super(cursor, startKey);
        }

        @Override
        protected void keyToEntry(Integer key, DatabaseEntry entry) {
            IntegerBinding.intToEntry(key, entry);
        }
    }
}
