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

package oracle.kv.impl.admin.plan;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.AdminServiceParams;
import oracle.kv.impl.admin.CommandResult;
import oracle.kv.impl.admin.IllegalCommandException;
import oracle.kv.impl.admin.NonfatalAssertionException;
import oracle.kv.impl.admin.PlanLocksHeldException;
import oracle.kv.impl.admin.param.StorageNodeParams;
import oracle.kv.impl.admin.plan.task.Utils;
import oracle.kv.impl.api.table.FieldMap;
import oracle.kv.impl.api.table.IndexImpl.AnnotatedField;
import oracle.kv.impl.api.table.TableImpl;
import oracle.kv.impl.fault.CommandFaultException;
import oracle.kv.impl.fault.OperationFaultException;
import oracle.kv.impl.param.ParameterMap;
import oracle.kv.impl.topo.AdminId;
import oracle.kv.impl.topo.AdminType;
import oracle.kv.impl.topo.ArbNodeId;
import oracle.kv.impl.topo.DatacenterId;
import oracle.kv.impl.topo.DatacenterType;
import oracle.kv.impl.topo.RepGroupId;
import oracle.kv.impl.topo.RepNodeId;
import oracle.kv.impl.topo.ResourceId;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.util.KVThreadFactory;
import oracle.kv.impl.util.server.LoggerUtils;
import oracle.kv.table.TimeToLive;
import oracle.kv.util.ErrorMessage;

/**
 * The Planner creates and executes plans.  Plans are the means by which
 * topological changes are made to a store.  They are also used for creating
 * and modifying system metadata, such as tables, indexes, and security
 * information.
 *
 * Plan creation consists of populating the plan with tasks which perform the
 * actual work.  A single plan often comprises a number of tasks. Plan
 * execution is asynchronous, which is especially important to long-running
 * plans and those which affect many nodes in the system.  Plan execution is
 * idempotent in order to be resilient in the face of node failures, including
 * that of the admin node handling the plan.
 *
 * Error Handling
 * ==============
 * IllegalCommandException is used to indicate user error, such as a bad
 * parameter, or a user-provoked illegal plan transition. It is thrown
 * synchronously, in direct response to a user action. Examples:
 *  Bad parameter when creating a plan:
 *  - The user should fix the parameter and resubmit the
 *  User tries an illegal plan state transition, such as executing a plan that
 *  is not approved, approving a plan that is not pending, or executing a plan
 *  that has completed, etc.
 *  - The user should be notified that this was an illegal action
 *
 * OperationFaultException is thrown when plan execution runs into some kind of
 * resource problem, such as a RejectedExecutionException from lack of threads,
 * or a network problem, lack of ports, timeout, etc. In this case, the user is
 * notified and the GUI will present the option of retrying or rolling back the
 * plan.
 *
 * An AdminFaultException is thrown when an unexpected exception occurs during
 * plan execution. The fault handler processes the exception in such a way that
 * the Admin will not go down, but that the exception will be logged under
 * SEVERE and will be dumped to stderr. The problem is not going to get any
 * better without installing a bug fix, but the Admin should not go down.
 * The UI presents the option of retrying or rolling back the plan.
 *
 * Concurrency Limitations:
 * ========================
 * Plans may be created and approved for an indeterminate amount of
 * time before they are executed. However, topology dependent plans must clone
 * a copy of the topology at creation time, and use that to create a set of
 * directions to execute. Because of that, the topology must stay constant from
 * that point to execution point, and therefore only one topology changing
 * plan can be implemented at a time.
 *
 * Synchronization:
 * ========================
 *
 * New plan creation is serialized by synchronizing on the planner object (via
 * method synchronization). Manipulation of existing plans, such as execution
 * or restart etc, is synchronized at the Admin.
 *
 * In general, we would like to express the monitor locking sequence as going
 * from big objects to smaller objects, so the Admin would be locked before the
 * Planner, and the Planner before the Plan.  However we observe that in some
 * cases the plan is locked and then wants to update itself, requiring it the
 * thread to synchronize on the Planner, violating the aforementioned ideal.
 * Hence the desire to eliminate synchronization on the Planner for updates to
 * existing plans.  This is a TBD: see deadlocks described in [#22963] and
 * [#22992], both of which we believe have been eliminated, but other deadlocks
 * might be lurking, and therefore a comprehensive survey of synchronization in
 * Admin is called for.
 */

public class Planner {

    /**
     * The executor that we'll use for carrying out execution of the plan and
     * the tasks within it.
     */
    private ExecutorService executor;

    private final Logger logger;
    private final Admin admin;
    private final AtomicInteger planIdGenerator;

    private final Catalog catalog;

    /**
     */
    public Planner(Admin admin,
                   AdminServiceParams params,
                   int nextPlanId) {

        this.admin = admin;
        logger = LoggerUtils.getLogger(this.getClass(), params);
        executor = Executors.newCachedThreadPool
            (new KVThreadFactory("Planner", logger));
        catalog = new Catalog();
        planIdGenerator = new AtomicInteger(nextPlanId);
    }

    /**
     * Review all in progress plans. Anything that is in RUNNING state did
     * not finish, and should be deemed to be interrupted. Should be called
     * by the Admin explicitly after the planner is constructed.
     * 1.RUNNING plans ->INTERRUPT_REQUESTED -> INTERRUPTED, and
     *    will be restarted.
     * 2.INTERRUPT_REQUESTED plans -> INTERRUPTED and are not restarted. The
     *    failover is as if the cleanup phase was interrupted by the user.
     * 3.INTERRUPTED plans are left as is.
     */
    public Plan recover(Plan inProgressPlan) {
        if (inProgressPlan == null) {
            return null;
        }

        Plan restart = null;
        final Plan.State originalState = inProgressPlan.getState();
        if (inProgressPlan.getState() == Plan.State.RUNNING) {
            inProgressPlan.markAsInterrupted();
            /* Rerun it */
            restart = inProgressPlan;
        }

        if (inProgressPlan.getState() == Plan.State.INTERRUPT_REQUESTED) {
            /*
             * Let it move to interrupted state and stay there. The user had
             * previously requested an interrupt.
             */
            inProgressPlan.markAsInterrupted();
        }

        logger.log(Level.INFO,
                   "{0} originally in {1}, transitioned to {2}, {3} be " +
                   "restarted automatically",
                   new Object[] {inProgressPlan, originalState,
                                 inProgressPlan.getState(),
                                 (restart == null) ? "will not" : "will"});

        /*
         * All non-terminated plans, including those that are in ERROR or
         * INTERRUPT state should be put in the catalog. Even the
         * non-restarted ones need to be there, so the user can decide manually
         * whether to retry them.
         */
        register(inProgressPlan);
        admin.savePlan(inProgressPlan, "Plan Recovery");
        return restart;
    }

    /**
     * Registers the specified plan. Package access for unit test.
     */
    void register(Plan plan) {
        catalog.addNewPlan(plan);
    }

    /* For unit test support. */

    public void clearLocks(int planId) {
        catalog.clearLocks(planId);
    }

    /**
     * Shuts down the planner. No new plans will be executed. If force is true
     * any running plans are interrupted. If force is false and wait is true
     * then this method will wait for executing plans to complete. The wait
     * flag is ignored if force is true.
     *
     * @param force interrupt running plans if true
     * @param wait wait for running plans to complete if force is false
     */
    public void shutdown(boolean force, boolean wait) {
        if (force) {
            executor.shutdownNow();
            return;
        }

        executor.shutdown();

        if (wait) {
            try {
                executor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.log(Level.INFO, "Shutdown Planner failed: {0}", e);
            }
        }
    }

    /**
     * Returns true if the planner has been shutdown.
     *
     * @return true if the planner has been shutdown
     */
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    /*
     * NOTE that all plan creation is serial, so that each type of plan can
     * validate itself in a stable context. The catalog, and plan registration,
     * check for runtime constraints, such as plan exclusiveness, but since
     * registration is only done after the plan is created, it can't validate
     * for non-runtime constraints.  For example, the DeployStorePlan must
     * check whether the topology holds any other repNodes at creation time.
     */

    /**
     * Creates a data center and registers it with the topology.
     */
    public synchronized int
    createDeployDatacenterPlan(String planName,
                               String datacenterName,
                               int repFactor,
                               DatacenterType datacenterType,
                               boolean allowArbiters) {

        final DeployDatacenterPlan plan =
            new DeployDatacenterPlan(planIdGenerator, planName, this,
                                     admin.getCurrentTopology(),
                                     datacenterName, repFactor,
                                     datacenterType,
                                     allowArbiters);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * Creates a storage node and registers it with the topology.
     */
    public synchronized int
        createDeploySNPlan(String planName,
                           DatacenterId datacenterId,
                           StorageNodeParams inputSNP) {

            final DeploySNPlan plan =
                new DeploySNPlan(planIdGenerator, planName, this,
                                 admin.getCurrentTopology(),
                                 datacenterId, inputSNP);
            register(plan);
            return admin.saveCreatePlan(plan);
        }

    /**
     * Creates an Admin instance and updates the Parameters to reflect it.
     */
    public synchronized int createDeployAdminPlan(String name,
                                                  StorageNodeId snid,
                                                  AdminType type) {

        final DeployAdminPlan plan = new DeployAdminPlan(planIdGenerator, name,
                                                         this, snid,
                                                         type);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /*
     * Creates a PRIMARY Admin instance. For unit tests.
     */
    public int createDeployAdminPlan(String name,
                                     StorageNodeId snid) {
        return createDeployAdminPlan(name, snid, AdminType.PRIMARY);
    }

    /**
     * If <code>victim</code> is not <code>null</code>, then removes the Admin
     * with specified <code>AdminId</code>. Otherwise, if <code>dcid</code> is
     * not <code>null</code>, then removes all Admins in the specified
     * datacenter.
     */
    public synchronized int
        createRemoveAdminPlan(String name, DatacenterId dcid, AdminId victim) {

            final RemoveAdminPlan plan =
                new RemoveAdminPlan(planIdGenerator, name, this, dcid, victim);
            register(plan);
            return admin.saveCreatePlan(plan);
        }

    /**
     * Creates a plan to deploy the specified TopologyCandidate.
     */
    public synchronized int createDeployTopoPlan(String planName,
                                                 String candidateName) {
        final DeployTopoPlan plan =
            DeployTopoPlan.create(planIdGenerator,
                                  planName,
                                  this,
                                  admin.getCurrentTopology(),
                                  admin.getCandidate(candidateName));
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    public synchronized int createFailoverPlan(
        String planName,
        Set<DatacenterId> newPrimaryZones,
        Set<DatacenterId> offlineZones) {

        final FailoverPlan plan = FailoverPlan.create(
            planIdGenerator, planName, this, admin.getCurrentTopology(),
            newPrimaryZones, offlineZones);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * Creates a plan to stop of all RepNodes in the store.
     */
    public synchronized int
        createStopAllRepNodesPlan(String planName) {

            final StopAllRepNodesPlan plan =
                new StopAllRepNodesPlan(planIdGenerator, planName, this,
                                        admin.getCurrentTopology());
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * Creates a plan to start all RepNodes the store.
     */
    public synchronized int
        createStartAllRepNodesPlan(String planName) {

            final StartAllRepNodesPlan plan =
                new StartAllRepNodesPlan(planIdGenerator, planName, this,
                                         admin.getCurrentTopology());
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * Creates a plan to stop the given set of RepNodes.
     */
    public synchronized int
        createStopRepNodesPlan(String planName, Set<RepNodeId> rnids) {

        final StopRepNodesPlan plan =
                new StopRepNodesPlan(planIdGenerator, planName, this,
                                     admin.getCurrentTopology(), rnids);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * Creates a plan to start the given set of RepNodes.
     */
    public synchronized int
        createStartRepNodesPlan(String planName, Set<RepNodeId> rnids) {

        final StartRepNodesPlan plan =
                new StartRepNodesPlan(planIdGenerator, planName, this,
                                      admin.getCurrentTopology(), rnids);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * Creates a plan to stop the specified services.
     */
    public synchronized int
        createStopServicesPlan(String planName,
                               Set<? extends ResourceId> serviceIds) {
        final Plan plan = new StopServicesPlan(planIdGenerator, planName, this,
                                               admin.getCurrentTopology(),
                                               serviceIds);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * Creates a plan to start the specified services.
     */
    public synchronized int
        createStartServicesPlan(String planName,
                                Set<? extends ResourceId> serviceIds) {
        final Plan plan = new StartServicesPlan(planIdGenerator, planName, this,
                                                admin.getCurrentTopology(),
                                                serviceIds);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * Creates a plan for replacing a potentially failed node in the store.
     * Any resources known to be allocated on the failed node will be
     * moved to the new node.  The new node must be an as yet unused node.
     */
    public synchronized int
        createMigrateSNPlan(String planName,
                            StorageNodeId oldNode,
                            StorageNodeId newNode) {

            final MigrateSNPlan plan =
                new MigrateSNPlan(planIdGenerator, planName, this,
                                  admin.getCurrentTopology(),
                                  oldNode, newNode);

            register(plan);
            return admin.saveCreatePlan(plan);
        }

    /**
     * Creates a plan for removing a storageNode. Removal is only permitted
     * for stopped storageNodes which do not house any services. It's meant to
     * remove defunct storageNodes after a migration has been run, or if
     * an initial deployment failed.
     */
    public synchronized int
        createRemoveSNPlan(String planName,
                           StorageNodeId targetNode) {
            final RemoveSNPlan plan =
                new RemoveSNPlan(planIdGenerator, planName, this,
                                 admin.getCurrentTopology(), targetNode);

            register(plan);
            return admin.saveCreatePlan(plan);
        }

    /**
     * Creates a plan for removing a datacenter. Removal is only permitted for
     * <em>empty</em> datacenters; that is, datacenters which contain no
     * storage nodes.
     */
    public synchronized int
        createRemoveDatacenterPlan(String planName,
                                   DatacenterId targetId) {
            final RemoveDatacenterPlan plan =
                new RemoveDatacenterPlan(planIdGenerator, planName, this,
                                         admin.getCurrentTopology(),
                                         targetId);

            register(plan);
            return admin.saveCreatePlan(plan);
        }

    public synchronized int createAddTablePlan(String planName,
                                               TableImpl table,
                                               String parentName) {
        final Plan plan = TablePlanGenerator.
            createAddTablePlan(planIdGenerator, planName, this,
                               table, parentName);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    public synchronized int createEvolveTablePlan(String planName,
                                                  String tableName,
                                                  int tableVersion,
                                                  FieldMap fieldMap,
                                                  TimeToLive ttl) {

        final Plan plan = TablePlanGenerator.
            createEvolveTablePlan(planIdGenerator, planName, this,
                                  tableName,
                                  tableVersion, fieldMap,
                                  ttl);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    public synchronized int createRemoveTablePlan(String planName,
                                                  String tableName,
                                                  boolean removeData) {
        final Plan plan = TablePlanGenerator.
            createRemoveTablePlan(planIdGenerator, planName, this,
                                  admin.getCurrentTopology(),
                                  tableName, removeData);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    public synchronized int createAddIndexPlan(String planName,
                                               String indexName,
                                               String tableName,
                                               String[] indexedFields,
                                               String description) {
        final Plan plan = TablePlanGenerator.
            createAddIndexPlan(planIdGenerator, planName, this,
                               admin.getCurrentTopology(),
                               indexName, tableName,
                               indexedFields, description);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    public synchronized int createRemoveIndexPlan(String planName,
                                                  String indexName,
                                                  String tableName,
                                                  boolean override) {
        final Plan plan = TablePlanGenerator.
            createRemoveIndexPlan(planIdGenerator, planName, this,
                                  admin.getCurrentTopology(),
                                  indexName, tableName, override);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    public synchronized int createAddTextIndexPlan
        (String planName, String indexName, String tableName,
         AnnotatedField[] ftsFields, Map<String,String> properties,
         String description, boolean override) {

        final Plan plan = TablePlanGenerator.
            createAddTextIndexPlan(planIdGenerator, planName, this,
                                   indexName, tableName,
                                   ftsFields, properties, description,
                                   override);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    public synchronized int createBroadcastTableMDPlan() {
        final Plan plan = TablePlanGenerator.
            createBroadcastTableMDPlan(planIdGenerator, this);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    public synchronized int createBroadcastSecurityMDPlan() {
        final Plan plan = SecurityMetadataPlan.
            createBroadcastSecurityMDPlan(planIdGenerator, this);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * TODO: future: consolidate change parameters plans for RN, SN, and admin.
     */
    public synchronized int
        createChangeParamsPlan(String planName,
                               ResourceId rid,
                               ParameterMap newParams) {

            Plan plan = null;
            if (rid instanceof RepNodeId) {
                final Set<RepNodeId> ids = new HashSet<>();
                ids.add((RepNodeId) rid);
                plan =
                    new ChangeParamsPlan(planIdGenerator, planName, this,
                                         admin.getCurrentTopology(),
                                         ids, newParams);
            } else if (rid instanceof StorageNodeId) {
                plan =
                    new ChangeSNParamsPlan(planIdGenerator, planName, this,
                                           (StorageNodeId) rid, newParams);
            } else if (rid instanceof AdminId) {
                plan =
                    new ChangeAdminParamsPlan(planIdGenerator, planName, this,
                                              (AdminId) rid, newParams);
            } else if (rid instanceof ArbNodeId) {
                final Set<ArbNodeId> ids = new HashSet<>();
                ids.add((ArbNodeId) rid);
                plan =
                    new ChangeANParamsPlan(planIdGenerator, planName, this,
                                         admin.getCurrentTopology(),
                                         ids, newParams);
            }
            register(plan);
            return admin.saveCreatePlan(plan);
        }

    /**
     * Creates a plan to apply parameters to all ArbNodes deployed in the
     * specified datacenter or if null, to all ArbNodes in the store.
     */
    public synchronized int
        createChangeAllANParamsPlan(String planName,
                                    DatacenterId dcid,
                                    ParameterMap newParams) {
        Set<ArbNodeId> anIds = admin.getCurrentTopology().getArbNodeIds(dcid);
        final Plan plan =
            new ChangeANParamsPlan(planIdGenerator, planName, this,
                                   admin.getCurrentTopology(),
                                   anIds, newParams);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * Creates a plan to apply parameters to all RepNodes deployed in the
     * specified datacenter.
     */
    public synchronized int
        createChangeAllParamsPlan(String planName,
                                  DatacenterId dcid,
                                  ParameterMap newParams) {

            final Plan plan =
                new ChangeAllParamsPlan(planIdGenerator, planName, this,
                                        admin.getCurrentTopology(),
                                        dcid, newParams);
            register(plan);
            return admin.saveCreatePlan(plan);
        }

    /**
     * Creates a plan to apply parameters to all Admins deployed in the
     * specified datacenter.
     */
    public synchronized int
        createChangeAllAdminsPlan(String planName,
                                  DatacenterId dcid,
                                  ParameterMap newParams) {

            final Plan plan =
                new ChangeAdminParamsPlan(planIdGenerator, planName, this, null,
                                          dcid, admin.getCurrentTopology(),
                                          newParams);
            register(plan);
            return admin.saveCreatePlan(plan);
        }

    /**
     * Creates a plan to apply new global components parameters to all services
     * deployed in the store.
     */
    public synchronized int
        createChangeGlobalComponentsParamsPlan(String planName,
                                               ParameterMap newParams) {
            final Plan plan =
                new ChangeGlobalComponentsParamsPlan(planIdGenerator,
                                                     planName, this,
                                                     admin.getCurrentTopology(),
                                                     newParams);
            register(plan);
            return admin.saveCreatePlan(plan);
        }

    /**
     * Creates a plan to apply new global security parameters to all services
     * deployed in the store.
     */
    public synchronized int
        createChangeGlobalSecurityParamsPlan(String planName,
                                             ParameterMap newParams) {
            final Plan plan =
                new ChangeGlobalSecurityParamsPlan(planIdGenerator,
                                                   planName, this,
                                                   admin.getCurrentTopology(),
                                                   newParams);
            register(plan);
            return admin.saveCreatePlan(plan);
        }

    /**
     * Creates a user and add it to the kvstore. Keep for compatible with
     * clients earlier than R3.3.
     */
    @Deprecated
    public synchronized int
        createCreateUserPlan(String planName,
                             String userName,
                             boolean isEnabled,
                             boolean isAdmin,
                             char[] plainPassword) {

        return createCreateUserPlan(planName, userName, isEnabled, isAdmin,
                                    plainPassword, null /* pwdLifetime*/ );
    }

    /**
     * Creates a user and add it to the kvstore.
     */
    public synchronized int
        createCreateUserPlan(String planName,
                             String userName,
                             boolean isEnabled,
                             boolean isAdmin,
                             char[] plainPassword,
                             Long pwdLifetime) {

            final SecurityMetadataPlan plan =
                SecurityMetadataPlan.createCreateUserPlan(
                    planIdGenerator, planName, this, userName, isEnabled,
                    isAdmin, plainPassword, pwdLifetime);
            register(plan);
            return admin.saveCreatePlan(plan);
        }

    /**
     * Creates an external user and add it to the kvstore.
     */
    public synchronized int
        createCreateExternalUserPlan(String planName,
                                     String userName,
                                     boolean isEnabled,
                                     boolean isAdmin) {

            final SecurityMetadataPlan plan =
                SecurityMetadataPlan.createCreateExternalUserPlan(
                    planIdGenerator, planName, this, userName, isEnabled,
                    isAdmin);
            register(plan);
            return admin.saveCreatePlan(plan);
        }

    /**
     * Changes the information of a kvstore user.
     */
    public synchronized int
        createChangeUserPlan(String planName,
                             String userName,
                             Boolean isEnabled,
                             char[] plainPassword,
                             boolean retainPassword,
                             boolean clearRetainedPassword,
                             Long pwdLifetime) {

            final SecurityMetadataPlan plan =
                SecurityMetadataPlan.createChangeUserPlan(
                    planIdGenerator, planName, this, userName, isEnabled,
                    plainPassword, retainPassword, clearRetainedPassword,
                    pwdLifetime);
            register(plan);
            return admin.saveCreatePlan(plan);
        }

    /**
     * Changes the information of a kvstore user. Keep for compatible with
     * clients earlier than R3.3.
     */
    @Deprecated
    public synchronized int
        createChangeUserPlan(String planName,
                             String userName,
                             Boolean isEnabled,
                             char[] plainPassword,
                             boolean retainPassword,
                             boolean clearRetainedPassword) {

        return createChangeUserPlan(
            planName, userName, isEnabled, plainPassword, retainPassword,
            clearRetainedPassword, null /* pwdLifeTime */);
    }

    /**
     * Removes a user with the specified name from the store.  Keep for
     * compatible with clients earlier than R3.3.
     */
    @Deprecated
    public synchronized int
        createDropUserPlan(String planName, String userName) {
        return createDropUserPlan(planName, userName, false /* cascade */);
    }

    /**
     * Removes a user with the specified name from the store.
     */
    public synchronized int
        createDropUserPlan(String planName, String userName, boolean cascade) {

        final AbstractPlan plan = SecurityMetadataPlan.createDropUserPlan(
            planIdGenerator, planName, this, userName, cascade);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * Creates a user-defined role and add it to the kvstore.
     */
    public synchronized int createCreateRolePlan(String planName,
                                                 String roleName) {

        final SecurityMetadataPlan plan =
            SecurityMetadataPlan.createCreateRolePlan(planIdGenerator, planName,
                                                      this, roleName);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * Removes a user-defined role with the specified name from the store.
     */
    public synchronized int createDropRolePlan(String planName,
                                               String roleName) {

        final SecurityMetadataPlan plan =
            SecurityMetadataPlan.createDropRolePlan(planIdGenerator, planName,
                                                    this, roleName);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * Grant given roles to user in the store.
     */
    public synchronized int
        createGrantPlan(String planName, String grantee, Set<String> roles) {

        final SecurityMetadataPlan plan =
            SecurityMetadataPlan.createGrantPlan(planIdGenerator, planName,
                                                 this, grantee, roles);

        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * Grant given roles to another role.
     */
    public synchronized int
        createGrantRolesToRolePlan(String planName,
                                   String grantee,
                                   Set<String> roles) {

        final SecurityMetadataPlan plan =
            SecurityMetadataPlan.createGrantRolesToRolePlan(
                planIdGenerator, planName, this, grantee, roles);

        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * Revoke given roles from user in the store.
     */
    public synchronized int
        createRevokePlan(String planName, String revokee, Set<String> roles) {

        final SecurityMetadataPlan plan =
            SecurityMetadataPlan.createRevokePlan(planIdGenerator, planName,
                                                  this, revokee, roles);

        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * Grant given roles to another role.
     */
    public synchronized int
        createRevokeRolesFromRolePlan(String planName,
                                      String revokee,
                                      Set<String> roles) {

        final SecurityMetadataPlan plan =
            SecurityMetadataPlan.createRevokeRolesFromRolePlan(
                planIdGenerator, planName, this, revokee, roles);

        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * Creates a plan that grants a set of privileges on, optionally, a table
     * to a role.
     *
     * @param planName plan name
     * @param roleName role name
     * @param tableName table name, null if granting only system privileges
     * @param privs privileges to grant
     * @return planId
     */
    public synchronized int
        createGrantPrivilegePlan(String planName,
                                 String roleName,
                                 String tableName,
                                 Set<String> privs) {
        final SecurityMetadataPlan plan =
            SecurityMetadataPlan.createGrantPrivsPlan(planIdGenerator, planName,
                                                      this, roleName, tableName,
                                                      privs);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * Creates a plan that revokes a set of privileges on, optionally, a table
     * from a role.
     *
     * @param planName plan name
     * @param roleName role name
     * @param tableName table name, null if revoking only system privileges
     * @param privs privileges to revoke
     * @return planId
     */
    public synchronized int
        createRevokePrivilegePlan(String planName,
                                  String roleName,
                                  String tableName,
                                  Set<String> privs) {
        final SecurityMetadataPlan plan =
            SecurityMetadataPlan.createRevokePrivsPlan(
                planIdGenerator, planName, this, roleName, tableName, privs);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * Creates a plan that verifies and repairs the store topology.
     */
    public synchronized int createRepairPlan(String planName) {

        final RepairPlan plan = new RepairPlan(planIdGenerator,
                                               planName,
                                               this);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    /**
     * Submits a plan for asynchronous execution. If a previous plan is still
     * executing, we will currently throw an exception. In the future, plans
     * may be queued, but queuing would require policies and mechanism to
     * determine what should happen to the rest of the queue if a plan fails.
     * For example, should we "run the next plan, but only if the current
     * succeeds" or ".. regardless of if the current succeeds", etc.
     *
     * Plan execution can be repeated, in order to retry a plan.
     * @throws PlanLocksHeldException
     */
    public PlanRun executePlan(Plan plan, boolean force)
        throws PlanLocksHeldException {

        /* For now, a Planner will only execute an AbstractPlan. */
        final AbstractPlan targetPlan;
        if (plan instanceof AbstractPlan) {
            targetPlan = (AbstractPlan) plan;
        } else {
            throw new NonfatalAssertionException
                ("Unknown Plan type: " + plan.getClass() +
                 " cannot be executed");
        }

        /* Check any preconditions for running the plan */
        targetPlan.validateStartOfRun();

        /* Check that the catalog's rules for running this plan are ok */
        catalog.validateStart(plan);
        PlanRun planRun = null;
        try {
            /*
             * Make sure we can get any plan-exclusion locks we need. Lock
             * before doing checks, to make sure that the topology does not
             * change.
             */
            plan.getCatalogLocks();

            /* Validate that this plan can run */
            plan.preExecuteCheck(force, logger);

            /*
             * Executing a plan equates to executing each of its tasks and
             * monitoring their state.  We'll kick off this process by running a
             * PlanExecutor in another thread.
             */
            planRun = targetPlan.startNewRun();
            final PlanExecutor planExec = new PlanExecutor(admin, this,
                                                           targetPlan,
                                                           planRun, logger);

            final Future<Plan.State> future = executor.submit(planExec);

            /*
             * Note that Catalog.addPlanFuture guards against the possibility
             * that the execute thread has finished before the future is added
             * to the catalog.
             */
            catalog.addPlanFuture(targetPlan, future);
        } catch (RejectedExecutionException e) {
            final String problem =
                "Plan did not start, insufficient resources for " +
                "executing a plan";
            if (planRun != null) {
                plan.saveFailure(planRun, e, problem, ErrorMessage.NOSQL_5400,
                                 CommandResult.PLAN_CANCEL, logger);
            }
            planFinished(targetPlan);
            throw new CommandFaultException(
                problem, new OperationFaultException(problem, e),
                ErrorMessage.NOSQL_5400, CommandResult.PLAN_CANCEL);
        }

        return planRun;
    }

    /**
     * Used by the PlanExecutor to indicate that it's finished execution.
     */
    void planFinished(Plan plan) {
        catalog.clearLocks(plan.getId());
        catalog.clearPlan(plan);
    }

    public Admin getAdmin() {
        return admin;
    }

    public void lockElasticity(int planId, String planName)
        throws PlanLocksHeldException {
        catalog.lockElasticityChange(planId, planName);
    }

    public void lock(int planId, String planName, ResourceId rnId)
        throws PlanLocksHeldException {
        catalog.lock(planId, planName, rnId);
    }

    public void lockShard(int planId, String planName, RepGroupId rgId)
        throws PlanLocksHeldException {
        catalog.lockShard(planId, planName, rgId);
    }

    /**
     * A collection of non-finished plans. It is used to enforce runtime
     * constraints such as:
     * - only one exclusive plan can run at a time
     * - rep nodes can't be targeted by more than one plan at a time.
     * - execution time locks taken. Locks must be acquire for each execution,
     * because the locks are transient and only last of the life of the
     * Planner instance.
     *
     * The catalog lets the Admin query plan status, and interrupt running
     * plans.
     * TODO: do we need the notion of exclusivity, if locks are now taken?
     */
    private static class Catalog {
        private Plan currentExclusivePlan;
        private Plan currentExecutingPlan;
        private final Map<Integer, Future<Plan.State>> futures;
        private final Map<Integer, Plan> planMap;

        /*
         * Logical locks are used to govern which plans can run concurrently. A
         * single high level elasticity lock is used to ensure that only a
         * single elasticity plan can run at a time. Shard and RN locks
         * coordinate between concurrent elasticity and repair plans. Repair
         * plans are those that might move a single RN or migrate all the
         * components on a single SN.
         *
         * Locking a shard requires first checking for locks against any RNs or
         * ANs in the shard. The same applies in reverse; locking a RN/AN is
         * only possible if there is no shard lock. For simplicity, and
         * because this does not need to be a performant action, all
         * serialization is accomplished simply be synchronizing
         * on elasticityLock.
         */
        private final TopoLock elasticityLock;
        private final Map<ResourceId, TopoLock> rnAnLocks;
        private final Map<RepGroupId, TopoLock> rgLocks;

        Catalog() {
            planMap = new HashMap<>();
            futures = new HashMap<>();

            elasticityLock = new TopoLock();
            rnAnLocks = new HashMap<>();
            rgLocks = new HashMap<>();
        }

        public void lockElasticityChange(int planId, String planName)
            throws PlanLocksHeldException {
            if (!elasticityLock.get(planId, planName)) {
                throw cantLock(planId, planName, elasticityLock.lockingPlanId,
                               elasticityLock.lockingPlanName);
            }
        }

        /**
         * Check the shard locks before locking the RN.
         * @throws PlanLocksHeldException
         */
        public void lock(int planId, String planName, ResourceId resId)
            throws PlanLocksHeldException {
            synchronized (elasticityLock) {
                final TopoLock rgl =
                    rgLocks.get(Utils.getRepGroupId(resId));
                if (rgl != null) {
                    if (rgl.lockingPlanId != planId) {
                        throw cantLock(planId, planName, rgl.lockingPlanId,
                                       rgl.lockingPlanName);
                    }
                }

                TopoLock rnl = rnAnLocks.get(resId);
                if (rnl == null) {
                    rnl = new TopoLock();
                    rnAnLocks.put(resId, rnl);
                }
                if (!rnl.get(planId, planName)) {
                    throw cantLock(planId, planName, rnl.lockingPlanId,
                                   rnl.lockingPlanName);
                }
            }
        }

        private PlanLocksHeldException cantLock(int planId,
                                                 String planName,
                                                 int lockingId,
                                                 String lockingName) {
            return new PlanLocksHeldException
                ("Couldn't execute " + planId + "/" + planName + " because " +
                 lockingId + "/" + lockingName + " is running. " +
                 "Wait until that plan is finished or interrupted",
                 lockingId);
        }

        /**
         * Check the RN locks as well as the RN
         * @throws PlanLocksHeldException
         */
        public void lockShard(int planId, String planName, RepGroupId rgId)
            throws PlanLocksHeldException {
            synchronized (elasticityLock) {
                TopoLock rgl = rgLocks.get(rgId);
                if (rgl != null && rgl.locked) {
                    if (rgl.lockingPlanId == planId) {
                        return;
                    }
                    throw cantLock(planId, planName, rgl.lockingPlanId,
                                   rgl.lockingPlanName);
                }

                /* check RNs/ANs first */
                for (Map.Entry<ResourceId, TopoLock> entry:
                         rnAnLocks.entrySet()) {
                    if (rgId.equals(Utils.getRepGroupId(entry.getKey()))) {
                        final TopoLock l = entry.getValue();
                        if ((l.lockingPlanId != planId) && l.locked) {
                            throw cantLock(planId, planName, l.lockingPlanId,
                                           l.lockingPlanName);
                        }
                    }
                }

                if (rgl == null) {
                    rgl = new TopoLock();
                    rgLocks.put(rgId, rgl);
                }

                rgl.get(planId, planName);
            }
        }

        public void clearLocks(int planId) {
            /* Remove all locks that pertain to this plan */
            synchronized (elasticityLock) {
                Iterator<TopoLock> iter = rnAnLocks.values().iterator();
                while (iter.hasNext()) {
                    final TopoLock tl = iter.next();
                    if (tl.lockingPlanId == planId) {
                        iter.remove();
                    }
                }

                iter = rgLocks.values().iterator();
                while (iter.hasNext()) {
                    final TopoLock tl = iter.next();
                    if (tl.lockingPlanId == planId) {
                        iter.remove();
                    }
                }
            }

            elasticityLock.releaseForPlanId(planId);
        }

        /**
         * A lock to coordinate topology related operations.
         */
        private class TopoLock {
            boolean locked;
            int lockingPlanId;
            String lockingPlanName;

            synchronized boolean get(int planId, String planName) {
                if (locked && (lockingPlanId != planId)) {
                    return false;
                }
                locked = true;
                lockingPlanId = planId;
                lockingPlanName = planName;
                return true;
            }

            /**
             * Release the component if this plan owns it.
             */
            synchronized void releaseForPlanId(int planId) {
                if (!locked) {
                    return;
                }

                if (lockingPlanId == planId) {
                    locked = false;
                    lockingPlanId = 0;
                    lockingPlanName = null;
                }
            }
        }

        /**
         * Enforce exclusivity at plan creation time.
         *
         *             no pending or  1 or more pending   1 pending/approved
         *             approved       or approved non-ex   exclusive plan
         *              plans         plans
         *
         * New plan is   ok             ok                  throw exception
         * exclusive
         *
         * New plan is   ok             ok                  ok
         * not
         * exclusive
         */
        synchronized void addNewPlan(Plan plan) {

            /* No need to enforce exclusivity. */
            if (!plan.isExclusive()) {
                planMap.put(plan.getId(), plan);
                return;
            }

            if (currentExclusivePlan == null) {
                currentExclusivePlan = plan;
                planMap.put(plan.getId(), plan);
                return;
            }

            throw new IllegalCommandException
                (plan + " is an exclusive type plan, and cannot be created " +
                 " because " + currentExclusivePlan +
                 " is active. Consider canceling " +
                 currentExclusivePlan + ".",
                 ErrorMessage.NOSQL_5300, CommandResult.NO_CLEANUP_JOBS);
        }

        synchronized void addPlanFuture(Plan plan, Future<Plan.State> future) {

            /*
             * There is the small possibility that the plan execution thread
             * will have executed and finished the plan before we save its
             * future. Check so that we don't needlessly add it.
             */
            if (!plan.getState().isTerminal()) {
                futures.put(plan.getId(), future);
            }
        }

        synchronized void validateStart(Plan plan) {

            /*
             * Plans that were created through Planner should be
             * registered. This is really an assertion check for internal
             * testing plans.
             */
            if (getPlan(plan.getId()) == null) {
                throw new NonfatalAssertionException
                    (plan + " must be registered.");
            }

            /* A non-exclusive plan has no restrictions on when it can run.*/
            if (!plan.isExclusive()) {
                return;
            }

            if (currentExecutingPlan != null) {
                if (currentExecutingPlan.equals(plan)) {
                    throw new IllegalCommandException
                        (plan + " is already running", ErrorMessage.NOSQL_5300,
                         CommandResult.NO_CLEANUP_JOBS);
                }

                throw new IllegalCommandException
                    (currentExecutingPlan + " is running, can't start " + plan,
                     ErrorMessage.NOSQL_5300, CommandResult.NO_CLEANUP_JOBS);
            }
            currentExecutingPlan = plan;
        }

        synchronized void clearPlan(Plan plan) {
            futures.remove(plan.getId());

            if (currentExecutingPlan == plan) {
                currentExecutingPlan = null;
            }

            if (!plan.getState().isTerminal()) {
                return;
            }

            planMap.remove(plan.getId());

            if (currentExclusivePlan == plan) {
                currentExclusivePlan = null;
            }
        }

        Plan getPlan(int planId) {
            return planMap.get(planId);
        }
    }

    /**
     * Returns the specified plan from the cache of active Plans.
     */
    public Plan getCachedPlan(int planId) {
        return catalog.getPlan(planId);
    }

    private Plan getFromCatalog(int planId) {

        final Plan plan = getCachedPlan(planId);

        if (plan == null) {
            /*
             * Plan ids may be specified via the CLI, so a user specified
             * invalid id may result in this problem, and this should be an
             * IllegalCommandException.
             */
            throw new IllegalCommandException("Plan " + planId +
                                              " is not an active plan");
        }
        return plan;
    }

    /**
     */
    public void approvePlan(int planId) {
        final Plan plan = getFromCatalog(planId);
        try {
            ((AbstractPlan) plan).requestApproval();
        } catch (IllegalStateException e) {

            /*
             * convert this to IllegalCommandException, since this is a
             * user initiated action.
             */
            throw new IllegalCommandException(e.getMessage());
        }
    }

    /**
     * Cancels a PENDING or APPROVED plan.
     */
    public void cancelPlan(int planId) {
        final AbstractPlan plan = (AbstractPlan) getFromCatalog(planId);
        try {
            plan.requestCancellation();
            planFinished(plan);
        } catch (IllegalStateException e) {

            /*
             * convert this to IllegalCommandException, since this is a
             * user initiated action.
             */
            throw new IllegalCommandException(e.getMessage(),
                                              ErrorMessage.NOSQL_5200,
                                              CommandResult.NO_CLEANUP_JOBS);
        }
    }

    /**
     * Interrupt a RUNNING plan.  Users must retry or rollback interrupted
     * plans.
     */
    public void interruptPlan(int planId) {
        final Plan plan = getFromCatalog(planId);
        final AbstractPlan aplan = (AbstractPlan) plan;
        if (aplan.cancelIfNotStarted()) {

            /*
             * If the plan isn't even running, just change state to CANCEL, no
             * need to do any interrupt processing.
             */
            return;
        }

        if (!(plan.getState().checkTransition
              (Plan.State.INTERRUPT_REQUESTED))) {
            throw new IllegalCommandException
                ("Can't interrupt plan " + plan + " in state " +
                 plan.getState());
        }

        logger.info("User requesting interrupt of " + plan);
        aplan.requestInterrupt();
    }

    /**
     * Returns the logger for this planner.
     *
     * @return the logger
     */
    Logger getLogger() {
        return logger;
    }

    /* For unit test support */
    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    /**
     * Registers an externally managed Elasticsearch cluster and updates
     * SNA Parameters to reflect it.
     */
    public Integer createRegisterESClusterPlan(String planName,
                                               String clusterName,
                                               String transportHp,
                                               boolean forceClear) {
        final Plan plan =
            new RegisterESPlan(planIdGenerator, planName, this,
                               clusterName, transportHp,
                               forceClear);
        register(plan);
        return admin.saveCreatePlan(plan);
    }

    public Integer createDeregisterESClusterPlan(String planName) {
        final Plan plan =
            new DeregisterESPlan(planIdGenerator, planName, this);
        register(plan);
        return admin.saveCreatePlan(plan);
    }
}
