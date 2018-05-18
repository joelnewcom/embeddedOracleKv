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
import java.util.concurrent.Callable;

import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.param.AdminParams;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.admin.plan.PlanExecutor.ParallelTaskRunner;
import oracle.kv.impl.sna.StorageNodeAgentAPI;
import oracle.kv.impl.topo.Partition;
import oracle.kv.impl.topo.RepGroup;
import oracle.kv.impl.topo.RepGroupId;
import oracle.kv.impl.topo.RepNode;
import oracle.kv.impl.topo.RepNodeId;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.topo.Topology;
import oracle.kv.impl.util.registry.RegistryUtils;

/**
 * A task for removing a RepNode from topology.
 * Step 1: Stop victim RepNode.
 * Step 2: Destroy the stopped victim from the SN which host it.
 * Step 3: Step 3: Remove RepNode and save changed plan.
 */
public class RemoveRepNode extends AbstractTask {

    private static final long serialVersionUID = 1L;

    private AbstractPlan plan;
    private RepNodeId victim;

    public RemoveRepNode(AbstractPlan plan,
                         RepNodeId victim) {
        this.plan = plan;
        this.victim = victim;
    }

    @Override
    public Callable<Task.State> getFirstJob(int taskId,
                                            ParallelTaskRunner runner) {
        return removeRepNodeJob(taskId, runner);
    }

    /**
     * @return a wrapper that will invoke to remove a RepNode.
     */
    private JobWrapper removeRepNodeJob(final int taskId,
                                        final ParallelTaskRunner runner) {
        return new JobWrapper(taskId, runner, "request removing a repnode") {
            @Override
            public NextJob doJob() {
                Topology sourceTopo = plan.getAdmin().getCurrentTopology();
                RepGroupId rgId = new RepGroupId(victim.getGroupId());
                RepGroup rg = sourceTopo.get(rgId);
                if (rg == null) {
                    /*
                     * This would happen if the plan was interrupted and
                     * re-executed.
                     */
                    plan.getLogger().fine(rg + " does no exist");
                    return NextJob.END_WITH_SUCCESS;
                }

                /* Check whether the to-be-removed shard has partitions. */
                for (Partition partition :
                        sourceTopo.getPartitionMap().getAll()) {
                    if (partition.getRepGroupId().equals(rgId)) {
                        throw new IllegalStateException
                            ("Error removing " + victim +
                             ", shard is not empty of user data (partitions)");
                    }
                }

                return removeRepNode(taskId, runner);
            }
        };
    }

    /**
     * Remove a RepNode from topology and shard
     */
    private NextJob removeRepNode(int taskId, ParallelTaskRunner runner) {
        final Admin admin = plan.getAdmin();
        final AdminParams ap = admin.getParams().getAdminParams();

        /*
         * Step 1: Stop victim RepNode.
         * Step 2: Destroy the stopped victim from the SN which host it.
         * Step 3: Remove RepNode and save changed plan.
         */
        Topology topo = admin.getCurrentTopology();
        RepNode rn = topo.get(victim);
        StorageNodeId snId = rn.getStorageNodeId();
        try {
            /* Step 1: Transfer master to another RepNode removeId. */
            Utils.stopRN(plan, snId, victim, false);
        } catch (RemoteException | NotBoundException e) {
            /* RMI problem, try again later. */
            return new NextJob(Task.State.RUNNING,
                               removeRepNodeJob(taskId, runner),
                               ap.getServiceUnreachablePeriod());
        }

        try {
            /* Step 2: Destroy the stopped victim from the SN which host it. */
            RegistryUtils registry =
                    new RegistryUtils(topo, admin.getLoginManager());
            StorageNodeAgentAPI oldSna = registry.getStorageNodeAgent(snId);
            oldSna.destroyRepNode(victim, true /* deleteData */);
        } catch (RemoteException | NotBoundException e) {
            /* RMI problem, try again later. */
            return new NextJob(Task.State.RUNNING,
                               removeRepNodeJob(taskId, runner),
                               ap.getServiceUnreachablePeriod());
        }

        /* Step 3: Remove RepNode and save changed plan. */
        admin.removeRepNodeAndSaveTopo(victim,
                                       plan.getDeployedInfo(),
                                       plan);

        return NextJob.END_WITH_SUCCESS;
    }

    @Override
    public boolean continuePastError() {
        return false;
    }

    @Override
    public String toString() {
       return super.toString() +
           " Remove RepNode " + victim;
    }
}
