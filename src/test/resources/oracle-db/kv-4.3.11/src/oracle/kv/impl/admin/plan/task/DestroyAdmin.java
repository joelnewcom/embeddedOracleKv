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
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.sna.StorageNodeAgentAPI;
import oracle.kv.impl.topo.AdminId;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.util.registry.RegistryUtils;

import com.sleepycat.persist.model.Persistent;

/**
 * A task for asking a storage node to destroy an Admin that it hosts.
 */
@Persistent(version=0)
public class DestroyAdmin extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private AbstractPlan plan;
    private StorageNodeId snid;
    private AdminId victim;

    public DestroyAdmin() {
    }

    public DestroyAdmin(AbstractPlan plan,
                        StorageNodeId snid,
                        AdminId victim) {
        this.plan = plan;
        this.snid = snid;
        this.victim = victim;
    }

    @Override
    public State doWork()
        throws Exception {

        Admin admin = plan.getAdmin();
        Parameters parameters = admin.getCurrentParameters();

        StorageNodeAgentAPI sna =
            RegistryUtils.getStorageNodeAgent(parameters, snid,
                                              plan.getLoginManager());

        sna.destroyAdmin(victim, true);

        return State.SUCCEEDED;
    }

    @Override
    public boolean continuePastError() {
        return false;
    }

    @Override
    public String toString() {
       return super.toString() +
           " Destroy the Admin replica " + victim;
    }
}
