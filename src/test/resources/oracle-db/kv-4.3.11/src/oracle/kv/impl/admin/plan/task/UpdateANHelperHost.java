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

import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.topo.ArbNodeId;
import oracle.kv.impl.topo.RepGroupId;

/**
 * A task for asking a ArbNode to update its helper hosts to include all its
 * peers.
 */
public class UpdateANHelperHost extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private final AbstractPlan plan;
    private final ArbNodeId anId;
    private final RepGroupId rgId;

    public UpdateANHelperHost(AbstractPlan plan,
                              ArbNodeId anId,
                              RepGroupId rgId) {

        super();
        this.plan = plan;
        this.anId = anId;
        this.rgId = rgId;
    }

    /**
     */
    @Override
    public State doWork()
        throws Exception {
        Utils.updateHelperHost(plan.getAdmin(),
                               plan.getAdmin().getCurrentTopology(),
                               rgId,
                               anId,
                               plan.getLogger());
        return State.SUCCEEDED;
    }

    @Override
    public boolean continuePastError() {
        return false;
    }
}
