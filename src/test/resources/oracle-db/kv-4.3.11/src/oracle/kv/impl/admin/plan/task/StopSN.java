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

import java.rmi.ConnectException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.UnknownHostException;

import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.plan.TopologyPlan;
import oracle.kv.impl.sna.StorageNodeAgentAPI;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.topo.Topology;
import oracle.kv.impl.util.registry.RegistryUtils;

/**
 * Stop Storage Node
 */
public class StopSN extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private StorageNodeId target;
    private TopologyPlan plan;

    public StopSN(TopologyPlan plan,
                  StorageNodeId target) {

        super();
        this.plan = plan;
        this.target = target;
    }

    @Override
    public State doWork()
        throws Exception {

        /* Get StorageNodeAgentAPI and shut down the Storage Node directly */
        Admin admin = plan.getAdmin();
        Topology topology = admin.getCurrentTopology();
        RegistryUtils registryUtils =
            new RegistryUtils(topology, admin.getLoginManager());
        try {
            StorageNodeAgentAPI sna = registryUtils.getStorageNodeAgent(target);
            sna.shutdown(true, false);
        } catch (NoSuchObjectException |
                 UnknownHostException |
                 NotBoundException |
                 ConnectException exception) {
            plan.getLogger().fine("The SN " + target +
                                  " cannot be found by RMI");
        }
        return Task.State.SUCCEEDED;
    }

    @Override
    public boolean continuePastError() {
        return false;
    }
}
