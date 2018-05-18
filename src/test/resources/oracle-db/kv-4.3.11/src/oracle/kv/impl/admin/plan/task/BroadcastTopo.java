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


import com.sleepycat.persist.model.Persistent;
import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.plan.DeployTopoPlan;


/**
 * Broadcast the current topology to all RNs. This is typically done after a
 * series of tasks that modify the topology.
 */
@Persistent
public class BroadcastTopo extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    protected DeployTopoPlan plan;

    /**
     * Constructor.
     * @param plan the plan
     */
    public BroadcastTopo(DeployTopoPlan plan) {
        this.plan = plan;
    }

    /*
     * No-arg ctor for use by DPL.
     */
    BroadcastTopo() {
    }

    @Override
    public State doWork() throws Exception {
        final Admin admin = plan.getAdmin();
        if (!Utils.broadcastTopoChangesToRNs(plan.getLogger(),
                                             admin.getCurrentTopology(),
                                             toString(),
                                             admin.getParams().getAdminParams(),
                                             plan,
                                             plan.getOfflineZones())) {
            return State.INTERRUPTED;
        }
        return State.SUCCEEDED;
    }

    /**
     * Stop the plan if this task fails. Although there are other mechanisms
     * that will let the topology trickle down to the node, the barrier for
     * broadcast success is low (only a small percent of the RNs need to
     * acknowledge the topology), and plan execution will be easier to
     * understand without failures.
     */
    @Override
    public boolean continuePastError() {
        return false;
    }
}
