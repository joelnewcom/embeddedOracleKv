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

import java.util.logging.Logger;

import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.plan.DeployTopoPlan;
import oracle.kv.impl.topo.RepGroup;
import oracle.kv.impl.topo.RepGroupId;
import oracle.kv.impl.topo.Topology;

import com.sleepycat.persist.model.Persistent;

/**
 * Add a new rep group (shard) to the topology. This is purely an update to the
 * topology stored in the administrative db and does not require any remote
 * calls.
 */
@Persistent
public class DeployShard extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    protected DeployTopoPlan plan;
    private int planShardIdx;

    /*
     * For debugging a description of the SNs that host the shard. This
     * is provided because the shard id only may not mean much to the user;
     * SNs host/ports and ids will have more significance.
     */
    private String snDescList;

    /**
     */
    public DeployShard(DeployTopoPlan plan,
                       int planShardIdx,
                       String snDescList) {
        this.plan = plan;
        this.planShardIdx = planShardIdx;
        this.snDescList = snDescList;
    }

    /*
     * No-arg ctor for use by DPL.
     */
    DeployShard() {
    }

    @Override
    public State doWork()
        throws Exception {

        Logger logger = plan.getLogger();

        Admin plannerAdmin = plan.getAdmin();
        Topology current = plannerAdmin.getCurrentTopology();

        /* The shard has been created from a previous invocation of the plan. */
        RepGroupId shardId = plan.getShardId(planShardIdx);
        if (shardId != null) {
            RepGroup rg = current.get(shardId);
            if (rg != null) {
                logger.info(shardId + " was previously created.");
                return State.SUCCEEDED;
            }

            /*
             * Clear the shard id, the plan was saved, but the current topology
             * doesn't have this shard.
             */
            plan.setNewShardId(planShardIdx, null);
            logger.fine(shardId + " not present in topology, although saved "+
                        "in plan, reset and repeat creation.");
        }

        /*
         * Create the shard (RepGroup) and partitions and persist its presence
         * in the topology. Note that the plan associates the RepGroupId with a
         * shard index, and this mapping will be preserved when the plan is
         * saved after each task.
         */
        RepGroup repGroup = new RepGroup();
        current.add(repGroup);
        plan.setNewShardId(planShardIdx, repGroup.getResourceId());
        plannerAdmin.saveTopo(current, plan.getDeployedInfo(), plan);
        return State.SUCCEEDED;
    }

    /**
     * If this task does not succeed, the following tasks of creating RNs
     * cannot continue.
     */
    @Override
    public boolean continuePastError() {
        return false;
    }

    @Override
    public String toString() {
        RepGroupId shardId = plan.getShardId(planShardIdx);
        if (shardId != null) {
            return super.toString() +  " " + shardId + " on " +  snDescList;
        }

        return super.toString() +  " on " +  snDescList;
    }
}
