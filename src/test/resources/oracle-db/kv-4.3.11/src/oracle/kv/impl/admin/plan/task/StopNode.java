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

import oracle.kv.impl.admin.PlanLocksHeldException;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.admin.plan.Planner;
import oracle.kv.impl.topo.ArbNodeId;
import oracle.kv.impl.topo.RepNodeId;
import oracle.kv.impl.topo.ResourceId;
import oracle.kv.impl.topo.StorageNodeId;

/**
 * A task for stopping a given RepNode or ArbNode
 */
public class StopNode extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private final AbstractPlan plan;
    private final StorageNodeId snId;
    private final ResourceId resId;
    private final boolean continuePastError;

    /**
     * We expect that the target Node exists before StopNode is
     * executed.
     * @param continuePastError if true, if this task fails, the plan
     * will stop.
     */
    public StopNode(AbstractPlan plan,
                    StorageNodeId snId,
                    ResourceId resId,
                    boolean continuePastError) {
        super();
        this.plan = plan;
        this.snId = snId;
        this.resId = resId;
        this.continuePastError = continuePastError;
    }

    @Override
    public State doWork()
        throws Exception {

        if (resId.getType().isRepNode()) {
            // TODO - Survey usages of this task to see if it should wait for
            // nodes to be consistent, stopRN(..., true).
            Utils.stopRN(plan, snId, (RepNodeId)resId, false);
        } else {
            Utils.stopAN(plan, snId, (ArbNodeId)resId);
        }

        return State.SUCCEEDED;
    }

    @Override
    public boolean continuePastError() {
        return continuePastError;
    }

    @Override
    public String getName() {
        return super.getName() + " " + resId;
    }

    @Override
    public String toString() {
        return super.toString() + " " + resId;
    }

    @Override
    public void lockTopoComponents(Planner planner)
        throws PlanLocksHeldException {
        planner.lock(plan.getId(), plan.getName(), resId);
    }
}
