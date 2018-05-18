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

import oracle.kv.impl.admin.NonfatalAssertionException;
import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.param.AdminParams;
import oracle.kv.impl.admin.param.Parameters;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.sna.StorageNodeAgentAPI;
import oracle.kv.impl.topo.AdminId;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.util.registry.RegistryUtils;

import com.sleepycat.persist.model.Persistent;

/**
 * Update the helper hosts on an Admin instance.
 */
@Persistent
public class UpdateAdminHelperHost extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private AbstractPlan plan;
    private AdminId aid;

    public UpdateAdminHelperHost(AbstractPlan plan, AdminId aid) {
        super();
        this.plan = plan;
        this.aid = aid;
    }

    /*
     * No-arg ctor for use by DPL.
     */
    @SuppressWarnings("unused")
    private UpdateAdminHelperHost() {
    }

    @Override
    public State doWork()
        throws Exception {

        Admin admin = plan.getAdmin();
        Parameters p = admin.getCurrentParameters();
        AdminParams ap = p.get(aid);

        if (ap == null) {
            throw new NonfatalAssertionException
                ("Can't find Admin " + aid + " in the Admin database.");
        }

        String helpers =
            Utils.findAdminHelpers(p, aid);

        if (helpers.length() == 0) {
            return State.SUCCEEDED;  /* Oh well. */
        }

        AdminParams newAp = new AdminParams(ap.getMap().copy());

        newAp.setHelperHost(helpers);
        admin.updateParams(newAp);
        plan.getLogger().info
            ("Changed helperHost for " + aid + " to " + helpers);

        /* Tell the SNA about it. */
        StorageNodeId snid = newAp.getStorageNodeId();
        StorageNodeAgentAPI sna =
            RegistryUtils.getStorageNodeAgent(p, snid,
                                              plan.getLoginManager());
        sna.newAdminParameters(newAp.getMap());

        return State.SUCCEEDED;
    }

    @Override
    public boolean continuePastError() {
        return false;
    }

    @Override
    public String toString() {
        return super.toString() + " update helper hosts for " + aid;
    }
}
