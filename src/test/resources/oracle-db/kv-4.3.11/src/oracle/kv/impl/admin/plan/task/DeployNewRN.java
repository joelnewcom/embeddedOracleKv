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
import oracle.kv.impl.admin.param.RepNodeParams;
import oracle.kv.impl.admin.param.StorageNodeParams;
import oracle.kv.impl.admin.param.StorageNodeParams.RNHeapAndCacheSize;
import oracle.kv.impl.admin.plan.DeployTopoPlan;
import oracle.kv.impl.admin.plan.PortTracker;
import oracle.kv.impl.admin.topo.StorageDirectory;
import oracle.kv.impl.fault.CommandFaultException;
import oracle.kv.impl.param.ParameterMap;
import oracle.kv.impl.param.ParameterState;
import oracle.kv.impl.param.ParameterUtils;
import oracle.kv.impl.security.login.LoginManager;
import oracle.kv.impl.sna.StorageNodeAgentAPI;
import oracle.kv.impl.test.TestHook;
import oracle.kv.impl.test.TestHookExecute;
import oracle.kv.impl.topo.Datacenter;
import oracle.kv.impl.topo.RepGroup;
import oracle.kv.impl.topo.RepGroupId;
import oracle.kv.impl.topo.RepNode;
import oracle.kv.impl.topo.RepNodeId;
import oracle.kv.impl.topo.StorageNode;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.topo.Topology;
import oracle.kv.impl.util.TopologyPrinter;
import oracle.kv.impl.util.registry.RegistryUtils;
import oracle.kv.impl.util.server.LoggerUtils;
import oracle.kv.util.ErrorMessage;

import com.sleepycat.je.rep.NodeType;
import com.sleepycat.persist.model.Persistent;

/**
 * Create and start a single RepNode on a particular Storage Node. This
 * requires:
 *
 * 1. adding the new RN to the topology
 * 2. adding appropriate param entries to the AdminDB for this RN.
 * 3. contacting the owning SN to invoke the RN creation.
 *
 * Note that since the Admin DB has the authoritative copy of the topology and
 * metadata, (1) and (2) must be done before the remote request to the SN is
 * made. The task must take care to be idempotent. Topology changes should not
 * be made needlessly, because unnecessary versions merely need pruning later.
 *
 * version 0: original
 * version 1: add storageDirectorySize
 */
@Persistent(version=1)
public class DeployNewRN extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private DeployTopoPlan plan;
    private StorageNodeId snId;
    private String snDescriptor;

    /*
     * Note that we now use "storage directory" instead of "mount point", but
     * since the field is serialized it would be a pain to change.
     */
    private String mountPoint;

    /*
     * If deserializing from an old version storageDirectorySize will
     * be 0. It is assumed that checks have already been made to prevent
     * a non-zero size before the Admins are upgraded.
     */
    private long storageDirectorySize;

    /*
     * Since the RepNodeId is only calculated when the task executes, this
     * field may be null. However, go to the effort to hang onto this
     * when it's available, because it's very useful for logging.
     */
    private RepNodeId displayRNId;

    /*
     * Only one of these fields will be set. If the RN is being made for a
     * brand new shard, the RepGroupId hasn't been allocated at the time when
     * the task is constructed. In that case, we use the planShardIdx. If
     * the RN is being added to a shard that already exists, the specified
     * shard field will be set.
     */
    private int planShardIdx;
    private RepGroupId specifiedShard;

    /* Hook to inject failures at different points in task execution */
    public static TestHook<String> FAULT_HOOK;

    /**
     * Creates a task for creating and starting a new RepNode for a brand
     * new shard, when we don't yet know the shard's id
     * @param storageDirectory if null, put RN in the SN's root directory
     */
    public DeployNewRN(DeployTopoPlan plan,
                       StorageNodeId snId,
                       int planShardIdx,
                       StorageDirectory storageDirectory) {
        this(plan, snId, storageDirectory);
        this.planShardIdx = planShardIdx;
    }

    /**
     * Creates a task for creating and starting a new RepNode for a shard
     * that already exists and has a repGroupId.
     * @param storageDirectory if null, put RN in the SN's root directory
     */
    public DeployNewRN(DeployTopoPlan plan,
                       StorageNodeId snId,
                       RepGroupId specifiedShard,
                       StorageDirectory storageDirectory) {
        this(plan, snId, storageDirectory);
        this.specifiedShard = specifiedShard;
    }

    private DeployNewRN(DeployTopoPlan plan,
                      StorageNodeId snId,
                      StorageDirectory storageDirectory) {
        super();
        this.plan = plan;
        this.snId = snId;
        if (storageDirectory == null) {
            mountPoint = null;
            storageDirectorySize = 0L;
        } else {
            mountPoint = storageDirectory.getPath();
            storageDirectorySize = storageDirectory.getSize();
        }

        /* A more descriptive label used for error messages, etc. */
        StorageNodeParams snp = plan.getAdmin().getStorageNodeParams(snId);
        snDescriptor = snp.displaySNIdAndHost();
    }

    /*
     * No-arg ctor for use by DPL.
     */
    DeployNewRN() {
    }

    /**
     * TODO: refactor change port tracker so it generates helper hosts and
     * works on a single SN. Correct now, just would be nicer to share the code.
     */
    private RepNodeParams makeRepNodeParams(Topology current,
                                            RepGroupId rgId,
                                            RepNodeId rnId) {
        /*
         * The RepNodeParams has everything needed to start up the new RepNode.
         */
        Parameters params = plan.getAdmin().getCurrentParameters();
        ParameterMap pMap = params.copyPolicies();

        /* Set JE HA host name */
        String haHostname = params.get(snId).getHAHostname();

        /* Find helper hosts for JE HA */
        PortTracker portTracker = new PortTracker(current, params, snId);
        int haPort = portTracker.getNextPort(snId);
        String otherHelpers = findHelperHosts(current.get(rgId), rnId, params);
        final NodeType nodeType = computeNodeType(current);
        String helperHosts;
        if (otherHelpers.length() == 0) {
            if (!nodeType.isElectable()) {
                final String msg = "The self-electing node must be electable";
                throw new CommandFaultException(
                    msg, new IllegalStateException(msg),
                    ErrorMessage.NOSQL_5500, CommandResult.NO_CLEANUP_JOBS);
            }
            helperHosts = haHostname + ":" + haPort;
        } else {
            helperHosts = otherHelpers;
        }

        RepNodeParams rnp =
            new RepNodeParams(pMap, snId, rnId,
                              false /* disabled */,
                              haHostname, haPort, helperHosts,
                              mountPoint, storageDirectorySize,
                              nodeType);
        /*
         * If the storage node has a memory setting, set an explicit JE heap
         * and cache size. The new RN has already been added to the topology,
         * so it will be accounted for by current.getHostedRepNodeIds().
         */
        StorageNodeParams snp = params.get(snId);
        int numRNsOnSN = current.getHostedRepNodeIds(snId).size();
        int numANsOnSN = current.getHostedArbNodeIds(snId).size();
        RNHeapAndCacheSize heapAndCache =
            snp.calculateRNHeapAndCache(pMap, numRNsOnSN,
                                        rnp.getRNCachePercent(), numANsOnSN);
        long heapMB = heapAndCache.getHeapMB();
        long cacheBytes = heapAndCache.getCacheBytes();
        int cachePercent = heapAndCache.getCachePercent();

        /*
         * If the storage node has a num cpus setting, set an explicit
         * -XX:ParallelGCThreads value
         */
        int gcThreads = snp.calcGCThreads();

        plan.getLogger().log
            (Level.INFO,
             "Creating {0} on {1} haPort={2}:{3} helpers={4} " +
             "storage directory path={5} directory size={6} " +
             "heapMB={7} cachePercent={8} cacheSize={9} " +
             "-XX:ParallelGCThreads={10}",
             new Object[] {rnId, snId, haHostname, haPort, helperHosts,
                           mountPoint, storageDirectorySize,
                           (heapMB == 0) ? "unspecified" : heapMB,
                           (cachePercent==0) ? "unspecified" : cachePercent,
                           (cacheBytes==0) ? "unspecified" : cacheBytes,
                           (gcThreads==0) ? "unspecified" : gcThreads});

        /*
         * Set the JVM heap, JE cache, and -XX:ParallelTCThreads.
         */
        rnp.setRNHeapAndJECache(heapAndCache);
        rnp.setParallelGCThreads(gcThreads);

        return rnp;
    }

    /**
     * Look at the current topology and parameters, as stored in the AdminDB,
     * and generate a set of helpers composed of all the hahost values for the
     * members of the group, other than the target RN.  Returns an empty string
     * if there are no other nodes to use as helpers.  In that case, the caller
     * should use the node's hostname and port to make it self-electing.
     */
    private String findHelperHosts(RepGroup shard,
                                   RepNodeId targetRNId,
                                   Parameters params) {

        StringBuilder helperHosts = new StringBuilder();

        for (RepNode rn : shard.getRepNodes()) {
            RepNodeId rId = rn.getResourceId();
            if (rId.equals(targetRNId)) {
                continue;
            }

            if (helperHosts.length() != 0) {
                helperHosts.append(",");
            }

            helperHosts.append(params.get(rId).getJENodeHostPort());
        }
        return helperHosts.toString();
    }

    /** Returns the node type for creating an RN in the specified SN. */
    private NodeType computeNodeType(final Topology current) {
        final Datacenter datacenter = current.getDatacenter(snId);
        return Datacenter.ServerUtil.getDefaultRepNodeType(datacenter);
    }

    @Override
    public State doWork()
        throws Exception {

        /* Create and save a topology and params that represent the new RN. */
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
            final String msg = "Expectedly can't find shard " + shardId +
                " current topology=" + TopologyPrinter.printTopology(current);
            throw new CommandFaultException(msg, new IllegalStateException(msg),
                                            ErrorMessage.NOSQL_5500,
                                            CommandResult.NO_CLEANUP_JOBS);
        }
        RepNode rn = null;
        RepNodeParams rnp = null;

        /*
         * If this shard already has a RN on this SN, then this task already
         * executed and this is a retry of the plan. We should use the RN
         * that is there. This assume we will never try to create the two RNs
         * from the same shard on the same SN.
         */
        for (RepNode existing : rg.getRepNodes()) {
            if (existing.getStorageNodeId().equals(snId)) {
                rn = existing;
                rnp = admin.getRepNodeParams(rn.getResourceId());
            }
        }

        if (rn == null) {
            rn = new RepNode(snId);
            rg.add(rn);
            displayRNId = rn.getResourceId();
            rnp =  makeRepNodeParams(current,
                                     rg.getResourceId(),
                                     rn.getResourceId());
            admin.saveTopoAndRNParam(current,
                                     plan.getDeployedInfo(),
                                     rnp, plan);
        } else {
            displayRNId = rn.getResourceId();
        }

        assert TestHookExecute.doHookIfSet(FAULT_HOOK, makeHookTag("1"));

        /*
         * Invoke the creation of the RN after the metadata is safely stored.
         * in the Admin DB.
         */
        LoginManager loginMgr = admin.getLoginManager();
        RegistryUtils regUtils = new RegistryUtils(current, loginMgr);
        StorageNodeAgentAPI sna = regUtils.getStorageNodeAgent(snId);

        if (rnp == null) {
            final String msg = "RepNodeParams null for " + rn;
            throw new CommandFaultException(msg, new IllegalStateException(msg),
                                            ErrorMessage.NOSQL_5400,
                                            CommandResult.PLAN_CANCEL);
        }

        try {
            sna.createRepNode(rnp.getMap(),
                              Utils.getMetadataSet(current, plan));
        } catch (IllegalStateException e) {
            throw new CommandFaultException(e.getMessage(), e,
                                            ErrorMessage.NOSQL_5200,
                                            CommandResult.NO_CLEANUP_JOBS);
        }

        /* Register this repNode with the monitor. */
        StorageNode sn = current.get(snId);
        admin.getMonitor().registerAgent(sn.getHostname(),
                                         sn.getRegistryPort(),
                                         rn.getResourceId());

        /*
         * At this point, we've succeeded. The user will have to rely on ping
         * and on monitoring to wait for the rep node to come up.
         */
        return State.SUCCEEDED;
    }

    @Override
    public boolean continuePastError() {
        return false;
    }

    @Override
    public String toString() {
        if (displayRNId == null) {
            return super.toString() + " on " +  snDescriptor;
        }
        return super.toString() + " " + displayRNId + " on " +
            snDescriptor;
    }

    @Override
    public String getName() {
        if (displayRNId == null) {
            return super.getName() + " on " +  snDescriptor;
        }
        return super.getName() + " " + displayRNId + " on " +  snDescriptor;
    }

    @Override
    public Runnable getCleanupJob() {
        return new Runnable() {
        @Override
        public void run(){
            boolean done = false;
            final Admin admin = plan.getAdmin();
            final Parameters params = admin.getCurrentParameters();
            final long maxRetryTime = ParameterUtils.getDurationMillis(
                params.getPolicies(), ParameterState.AP_NEW_RN_RETRY_TIME);
            final long startTime = System.currentTimeMillis();
            while (!done && !plan.cleanupInterrupted()) {
                try {
                    done = cleanupAllocation();
                } catch (Exception e) {
                    plan.getLogger().log
                        (Level.SEVERE,
                         "{0}: problem when cancelling deployment of RN {1}",
                         new Object[] {this, LoggerUtils.getStackTrace(e)});
                    /*
                     * Don't try to continue with cleanup; a problem has
                     * occurred. Future, additional invocations of the plan
                     * will have to figure out the context and do cleanup.
                     */
                    throw new RuntimeException(e);
                }

                if (!done) {
                    if (System.currentTimeMillis() - startTime
                            > maxRetryTime) {
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
        /* RN wasn't created, nothing to do */
        if (displayRNId == null) {
            logger.info("DeployNewRN cleanup: RN not created.");
            return true;
        }

        Admin admin = plan.getAdmin();
        TopologyCheck checker =
            new TopologyCheck(logger,
                              admin.getCurrentTopology(),
                              admin.getCurrentParameters());

        Remedy remedy =
            checker.checkLocation(admin, snId, displayRNId,
                                  true /* calledByDeployNewRN */,
                                  true /* makeRNEnabled */,
                                  null /* oldSNId */,
                                  null /* storageDirectory */);
        logger.info("DeployNewRN cleanup: " + remedy);

        return checker.applyRemedy(remedy, plan);
    }

    /**
     * For unit test support -- make a string that uniquely identifies when
     * this task executes on a given SN
     */
    private String makeHookTag(String pointName) {
        return "DeployNewRN/" + snId + "_pt" + pointName;
    }
}
