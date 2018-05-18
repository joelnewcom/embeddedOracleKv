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

import java.util.HashSet;
import java.util.Set;

import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.PlanLocksHeldException;
import oracle.kv.impl.admin.param.ArbNodeParams;
import oracle.kv.impl.admin.param.Parameters;
import oracle.kv.impl.admin.param.RepNodeParams;
import oracle.kv.impl.admin.param.StorageNodeParams;
import oracle.kv.impl.admin.param.StorageNodeParams.RNHeapAndCacheSize;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.admin.plan.Planner;
import oracle.kv.impl.metadata.Metadata;
import oracle.kv.impl.metadata.MetadataInfo;
import oracle.kv.impl.param.ParameterMap;
import oracle.kv.impl.security.login.LoginManager;
import oracle.kv.impl.sna.StorageNodeAgentAPI;
import oracle.kv.impl.topo.ArbNodeId;
import oracle.kv.impl.topo.RepGroupId;
import oracle.kv.impl.topo.RepNodeId;
import oracle.kv.impl.topo.StorageNode;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.topo.Topology;
import oracle.kv.impl.util.registry.RegistryUtils;

import com.sleepycat.persist.model.Persistent;

/**
 * Task for creating and starting all RepNodes and ArbNodes which are
 * housed on a particular Storage Node.
 *
 * version 0: original.
 */
@Persistent(version=0)
public class DeployMultipleRNs extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    protected AbstractPlan plan;
    protected StorageNodeId snId;
    protected String snDescriptor;

    /**
     * Creates a task for creating and starting a new RepNode.
     */
    public DeployMultipleRNs(AbstractPlan plan,
                             StorageNodeId snId) {
        super();
        this.plan = plan;
        this.snId = snId;

        /* A more descriptive label used for error messages, etc. */
        StorageNodeParams snp = plan.getAdmin().getStorageNodeParams(snId);
        snDescriptor = snp.displaySNIdAndHost();
    }

    /*
     * No-arg ctor for use by DPL.
     */
    DeployMultipleRNs() {
    }

    private Set<RepNodeId> getTargets() {
        Parameters parameters = plan.getAdmin().getCurrentParameters();
        Set<RepNodeId> targetSet = new HashSet<RepNodeId>();

        for (RepNodeParams rnp: parameters.getRepNodeParams()) {
            if (rnp.getStorageNodeId().equals(snId)) {
                targetSet.add(rnp.getRepNodeId());
            }
        }
        return targetSet;
    }

    private Set<ArbNodeId> getANTargets() {
        Parameters parameters = plan.getAdmin().getCurrentParameters();
        Set<ArbNodeId> targetSet = new HashSet<ArbNodeId>();

        for (ArbNodeParams anp: parameters.getArbNodeParams()) {
            if (anp.getStorageNodeId().equals(snId)) {
                targetSet.add(anp.getArbNodeId());
            }
        }
        return targetSet;
    }

    @Override
    public State doWork()
        throws Exception {
        Admin admin = plan.getAdmin();

        /*
         * Attempt to deploy repNodes. Note that one or more repNodes may have
         * been previously deployed. The task is successful if the repNodes are
         * created, or are on their way to coming up.
         */
        Topology topo = plan.getAdmin().getCurrentTopology();
        ParameterMap policyMap =
                plan.getAdmin().getCurrentParameters().copyPolicies();
        LoginManager loginMgr = admin.getLoginManager();
        RegistryUtils regUtils = new RegistryUtils(topo, loginMgr);
        StorageNodeAgentAPI sna = regUtils.getStorageNodeAgent(snId);
        StorageNodeParams snp = plan.getAdmin().getCurrentParameters().get(snId);
        int gcThreads = snp.calcGCThreads();
        Set<RepNodeId> targetRNIds = getTargets();
        int numRNsOnSN = targetRNIds.size();
        int numANsOnSN = topo.getHostedArbNodeIds(snId).size();


        final Set<Metadata<? extends MetadataInfo>> metadataSet =
                                            Utils.getMetadataSet(topo, plan);

        for (RepNodeId rnId : targetRNIds) {
            RepNodeParams rnp = admin.getRepNodeParams(rnId);
            RNHeapAndCacheSize heapAndCache =
                snp.calculateRNHeapAndCache(policyMap,
                                            numRNsOnSN,
                                            rnp.getRNCachePercent(),
                                            numANsOnSN);
            rnp.setRNHeapAndJECache(heapAndCache);
            rnp.setParallelGCThreads(gcThreads);
            sna.createRepNode(rnp.getMap(), metadataSet);

            /* Register this repNode with the monitor. */
            StorageNode sn = topo.get(snId);
            admin.getMonitor().registerAgent(sn.getHostname(),
                                             sn.getRegistryPort(),
                                             rnId);
        }

        Set<ArbNodeId> targetANIds = getANTargets();
        for (ArbNodeId anId : targetANIds) {
            ArbNodeParams anp = admin.getArbNodeParams(anId);
            sna.createArbNode(anp.getMap());

            /* Register this arbNode with the monitor. */
            StorageNode sn = topo.get(snId);
            admin.getMonitor().registerAgent(sn.getHostname(),
                                             sn.getRegistryPort(),
                                             anId);
        }


        /*
         * At this point, we've succeeded. The user will have to rely on ping
         * and on monitoring to wait for all the rep nodes to come up.
         */
        return State.SUCCEEDED;
    }

    @Override
    public boolean continuePastError() {
        return false;
    }

    @Override
    public void lockTopoComponents(Planner planner)
        throws PlanLocksHeldException {
        Set<RepNodeId> targets = getTargets();

        /*
         * Find the set of shard ids, we only want to get the shard lock
         * once, because the locking mechanism is fairly simplistic.
         */
        Set<RepGroupId> shards = new HashSet<RepGroupId>();
        for (RepNodeId rnId : targets) {
            shards.add(new RepGroupId(rnId.getGroupId()));
        }

        for (RepGroupId rgId : shards) {
            planner.lockShard(plan.getId(), plan.getName(), rgId);
        }
    }
}
