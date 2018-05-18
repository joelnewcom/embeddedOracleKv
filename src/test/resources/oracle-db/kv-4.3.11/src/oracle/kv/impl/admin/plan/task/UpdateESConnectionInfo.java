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

import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.param.Parameters;
import oracle.kv.impl.admin.param.StorageNodeParams;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.sna.StorageNodeAgentAPI;
import oracle.kv.impl.test.TestHook;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.util.registry.RegistryUtils;

/**
 * A task for creating and starting an instance of an Admin service on a
 * StorageNode.
 */
public class UpdateESConnectionInfo extends SingleJobTask {
    private static final long serialVersionUID = 1L;

    /* Hook to inject failures at different points in task execution */
    public static TestHook<Integer> FAULT_HOOK;

    private final AbstractPlan plan;
    private final StorageNodeId snid;
    private final String clusterName;
    private final String allTransports;

    public UpdateESConnectionInfo(AbstractPlan plan,
                                  StorageNodeId snid,
                                  String clusterName,
                                  String allTransports) {
        super();
        this.plan = plan;
        this.snid = snid;
        this.clusterName = clusterName;
        this.allTransports = allTransports;
    }

    @Override
    public State doWork()
        throws Exception {

        Admin admin = plan.getAdmin();
        Parameters p = admin.getCurrentParameters();
        StorageNodeParams snp = p.get(snid);
        snp.setSearchClusterMembers(allTransports);
        snp.setSearchClusterName(clusterName);
        admin.updateParams(snp, null);

        plan.getLogger().info("Changed searchClusterMembers for " + snid +
                              " to " + allTransports);

        /* Tell the SNA about it. */
        StorageNodeAgentAPI sna =
            RegistryUtils.getStorageNodeAgent(p, snid,
                                              plan.getLoginManager());
        sna.newStorageNodeParameters(snp.getMap());

        return State.SUCCEEDED;
    }

    @Override
    public boolean continuePastError() {
        return false;
    }

    @Override
    public String toString() {
        return super.toString() + " update clusterMembers for " + snid;
    }
}
