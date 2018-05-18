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

package oracle.kv.impl.admin.plan.task;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.CommandResult;
import oracle.kv.impl.admin.TopologyCheck;
import oracle.kv.impl.admin.TopologyCheck.Remedy;
import oracle.kv.impl.admin.param.Parameters;
import oracle.kv.impl.admin.param.ArbNodeParams;
import oracle.kv.impl.admin.param.StorageNodeParams;
import oracle.kv.impl.admin.plan.DeployTopoPlan;
import oracle.kv.impl.admin.plan.PortTracker;
import oracle.kv.impl.fault.CommandFaultException;
import oracle.kv.impl.param.ParameterMap;
import oracle.kv.impl.security.login.LoginManager;
import oracle.kv.impl.sna.StorageNodeAgentAPI;
import oracle.kv.impl.test.TestHook;
import oracle.kv.impl.test.TestHookExecute;
import oracle.kv.impl.topo.RepGroup;
import oracle.kv.impl.topo.RepGroupId;
import oracle.kv.impl.topo.ArbNode;
import oracle.kv.impl.topo.ArbNodeId;
import oracle.kv.impl.topo.RepNode;
import oracle.kv.impl.topo.RepNodeId;
import oracle.kv.impl.topo.StorageNode;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.topo.Topology;
import oracle.kv.impl.util.TopologyPrinter;
import oracle.kv.impl.util.registry.RegistryUtils;
import oracle.kv.impl.util.server.LoggerUtils;
import oracle.kv.util.ErrorMessage;

/**
 * Create and start a single ArbNode on a particular Storage Node. This
 * requires:
 *
 * 1. adding the new ARB to the topology
 * 2. adding appropriate param entries to the AdminDB for this ARB.
 * 3. contacting the owning SN to invoke the ARB creation.
 *
 * Note that since the Admin DB has the authoritative copy of the topology and
 * metadata, (1) and (2) must be done before the remote request to the SN is
 * made. The task must take care to be idempotent. Topology changes should not
 * be made needlessly, because unnecessary versions merely need pruning later.
 */

public class DeployNewARB extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private final DeployTopoPlan plan;
    private final StorageNodeId snId;
    private final String snDescriptor;

    /*
     * Since the ArbNodeId is only calculated when the task executes, this
     * field may be null. However, go to the effort to hang onto this
     * when it's available, because it's very useful for logging.
     */
    private ArbNodeId displayARBId;

    /*
     * Only one of these fields will be set. If the AN is being made for a
     * brand new shard, the RepGroupId hasn't been allocated at the time when
     * the task is constructed. In that case, we use the planShardIdx. If
     * the AN is being added to a shard that already exists, the specified
     * shard field will be set.
     */
    final private int planShardIdx;
    final private RepGroupId specifiedShard;

    /* Hook to inject failures at different points in task execution */
    public static TestHook<String> FAULT_HOOK;

    /**
     * Creates a task for creating and starting a new ArbNode for a brand
     * new shard, when we don't yet know the shard's id
     */
    public DeployNewARB(DeployTopoPlan plan,
                        StorageNodeId snId,
                        int planShardIdx) {

        this(plan, snId, planShardIdx, null);
    }

    /**
     * Creates a task for creating and starting a new ArbNode for a shard
     * that already exists and has a repGroupId.
     */
    public DeployNewARB(DeployTopoPlan plan,
                        StorageNodeId snId,
                        RepGroupId specifiedShard) {

        this(plan, snId, 0, specifiedShard);
    }

    private DeployNewARB(DeployTopoPlan plan, StorageNodeId snId,
                         int planShardIdx, RepGroupId specifiedShard) {
        this.plan = plan;
        this.snId = snId;
        this.planShardIdx = planShardIdx;
        this.specifiedShard = specifiedShard;

        /* A more descriptive label used for error messages, etc. */
        StorageNodeParams snp = plan.getAdmin().getStorageNodeParams(snId);
        snDescriptor = snp.displaySNIdAndHost();
    }

    /**
     * TODO: refactor change port tracker so it generates helper hosts and
     * works on a single SN. Correct now, just would be nicer to share the code.
     */
    private ArbNodeParams makeArbNodeParams(Topology current,
                                            RepGroupId rgId,
                                            ArbNodeId arbId) {
        /*
         * The ArbNodeParams has everything needed to start up the new ArbNode.
         */
        Parameters params = plan.getAdmin().getCurrentParameters();
        ParameterMap pMap = params.copyPolicies();

        /* Set JE HA host name */
        String haHostname = params.get(snId).getHAHostname();

        /* Find helper hosts for JE HA */
        PortTracker portTracker = new PortTracker(current, params, snId);
        int haPort = portTracker.getNextPort(snId);
        String otherHelpers = findHelperHosts(current.get(rgId), params);
        String helperHosts;
        if (otherHelpers.length() == 0) {
            helperHosts = haHostname + ":" + haPort;
        } else {
            helperHosts = otherHelpers;
        }

        ArbNodeParams arbp =
            new ArbNodeParams(pMap, snId, arbId,
                              false /* disabled */,
                              haHostname, haPort, helperHosts);
        /*
         * If the storage node has a memory setting, set an explicit JE heap
         * and cache size. The new AN has already been added to the topology,
         * so it will be accounted for by current.getHostedArbNodeIds().
         */
        StorageNodeParams snp = params.get(snId);

        long heapMB = StorageNodeParams.calculateANHeapSizeMB();

        /*
         * If the storage node has a num cpus setting, set an explicit
         * -XX:ParallelGCThreads value
         */
        int gcThreads = snp.calcGCThreads();

        plan.getLogger().log
            (Level.INFO,
             "Creating {0} on {1} haPort={2}:{3} helpers={4} " +
             "heapMB={5} -XX:ParallelGCThreads={6}",
             new Object[] {arbId, snId, haHostname, haPort, helperHosts,
                           (heapMB == 0) ? "unspecified" : heapMB,
                           (gcThreads==0) ? "unspecified" : gcThreads});

        /*
         * Set the JVM heap, JE cache, and -XX:ParallelTCThreads.
         */
        arbp.setARBHeap(heapMB);
        arbp.setParallelGCThreads(gcThreads);

        return arbp;
    }

    /**
     * Look at the current topology and parameters, as stored in the AdminDB,
     * and generate a set of helpers composed of all the hahost values for the
     * RN members of the group.  Returns an empty string
     * if there are no other nodes to use as helpers.  In that case, the caller
     * should use the node's hostname and port to make it self-electing.
     */
    private String findHelperHosts(RepGroup shard,
                                   Parameters params) {

        StringBuilder helperHosts = new StringBuilder();

        for (RepNode rn : shard.getRepNodes()) {
            RepNodeId rId = rn.getResourceId();

            if (helperHosts.length() != 0) {
                helperHosts.append(",");
            }

            helperHosts.append(params.get(rId).getJENodeHostPort());
        }
        return helperHosts.toString();
    }

    @Override
    public State doWork()
        throws Exception {
        Logger logger = plan.getLogger();

        /* Create and save a topology and params that represent the new AN. */
        final RepGroupId shardId;
        if (specifiedShard == null) {
            shardId = plan.getShardId(planShardIdx);
        } else {
            shardId = specifiedShard;
        }

        Admin admin = plan.getAdmin();
        Topology current = admin.getCurrentTopology();

        RepGroup rg = current.get(shardId);
        if (rg == null) {
            /*
             * This is really an assert, intended to provide better debugging
             * information than the resulting NPE.
             */
            final String msg = "Unexpectedly can't find shard " + shardId +
                " current topology=" + TopologyPrinter.printTopology(current);
            throw new CommandFaultException(msg, new IllegalStateException(msg),
                                            ErrorMessage.NOSQL_5500,
                                            CommandResult.NO_CLEANUP_JOBS);
        }
        ArbNode arbn = null;
        ArbNodeParams arbp = null;

        /*
         * If this shard already has a ARB on this SN, then this task already
         * executed and this is a retry of the plan. We should use the ARB
         * that is there. This assumes we will never try to create the two ARBs
         * in the same shard.
         */
        for (ArbNode existing : rg.getArbNodes()) {
            if (existing.getStorageNodeId().equals(snId)) {
                arbn = existing;
                arbp = admin.getArbNodeParams(arbn.getResourceId());
            }
        }

        if (arbn == null) {
            arbn = new ArbNode(snId);
            rg.add(arbn);
            displayARBId = arbn.getResourceId();
            arbp =  makeArbNodeParams(current,
                                      rg.getResourceId(),
                                      arbn.getResourceId());
            admin.saveTopoAndARBParam(current,
                                      plan.getDeployedInfo(),
                                      arbp, plan);
        } else {
            displayARBId = arbn.getResourceId();
        }

        logger.info("Creating AN " + displayARBId);

        assert TestHookExecute.doHookIfSet(FAULT_HOOK, makeHookTag("1"));

        /*
         * Invoke the creation of the AN after the metadata is safely stored.
         * in the Admin DB.
         */
        LoginManager loginMgr = admin.getLoginManager();
        RegistryUtils regUtils = new RegistryUtils(current, loginMgr);
        StorageNodeAgentAPI sna = regUtils.getStorageNodeAgent(snId);

        if (arbp == null) {
            final String msg = "ArbNodeParams null for " + arbn;
            throw new CommandFaultException(msg, new IllegalStateException(msg),
                                            ErrorMessage.NOSQL_5400,
                                            CommandResult.PLAN_CANCEL);
        }

        try {
            sna.createArbNode(arbp.getMap());
        } catch (IllegalStateException e) {
            throw new CommandFaultException(e.getMessage(), e,
                                            ErrorMessage.NOSQL_5200,
                                            CommandResult.NO_CLEANUP_JOBS);
        }

        /* Register this ArbNode with the monitor. */
        StorageNode sn = current.get(snId);
        admin.getMonitor().registerAgent(sn.getHostname(),
                                         sn.getRegistryPort(),
                                         arbn.getResourceId());

        /*
         * At this point, we've succeeded. The user will have to rely on ping
         * and on monitoring to wait for the arbiter node to come up.
         */
        return State.SUCCEEDED;
    }

    @Override
    public boolean continuePastError() {
        return false;
    }

    @Override
    public String toString() {
        if (displayARBId == null) {
            return super.toString() + " on " +  snDescriptor;
        }
        return super.toString() + " " + displayARBId + " on " +
            snDescriptor;
    }

    @Override
    public String getName() {
        if (displayARBId == null) {
            return super.getName() + " on " +  snDescriptor;
        }
        return super.getName() + " " + displayARBId + " on " +  snDescriptor;
    }

    @Override
    public Runnable getCleanupJob() {
        return new Runnable() {
        @Override
        public void run(){
            boolean done = false;
            int numAttempts = 0;
            while (!done && !plan.cleanupInterrupted()) {
                try {
                    done = cleanupAllocation();
                    numAttempts++;
                } catch (Exception e) {
                    plan.getLogger().log
                        (Level.SEVERE,
                         "{0}: problem when cancelling deployment of AN {1}",
                         new Object[] {this, LoggerUtils.getStackTrace(e)});
                    /*
                     * Don't try to continue with cleanup; a problem has
                     * occurred. Future, additional invocations of the plan
                     * will have to figure out the context and do cleanup.
                     */
                    throw new RuntimeException(e);
                }

                if (!done) {
                    /*
                     * Arbitrarily limit number of tries to 5. TODO: would be
                     * nicer if this was based on time.
                     */
                    if (numAttempts > 5) {
                        return;
                    }

                    /*
                     * TODO: would be nicer to schedule a job, rather
                     * than sleep.
                     */
                    try {
                        Thread.sleep(AbstractTask.CLEANUP_RETRY_MILLIS);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }
        };
    }

    private boolean cleanupAllocation()
        throws RemoteException, NotBoundException {
        Logger logger = plan.getLogger();

        assert TestHookExecute.doHookIfSet(FAULT_HOOK,
                                           makeHookTag("cleanup"));
        /* AN wasn't created, nothing to do */
        if (displayARBId == null) {
            logger.info("DeployNewARB cleanup: AN not created.");
            return true;
        }

        Admin admin = plan.getAdmin();
        TopologyCheck checker =
            new TopologyCheck(logger,
                              admin.getCurrentTopology(),
                              admin.getCurrentParameters());

        Remedy remedy =
            checker.checkLocation(admin, snId, displayARBId,
                                  true /* calledByDeployNewRN */,
                                  true /* makeRNEnable */,
                                  null /* oldSNId */);

        logger.info("DeployNewARB cleanup: " + remedy);

        return checker.applyRemedy(remedy, plan);
    }

    /**
     * For unit test support -- make a string that uniquely identifies when
     * this task executes on a given SN
     */
    private String makeHookTag(String pointName) {
        return "DeployNewARB/" + snId + "_pt" + pointName;
    }
}
