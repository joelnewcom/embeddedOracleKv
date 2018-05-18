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

import oracle.kv.impl.admin.CommandServiceAPI;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.topo.AdminId;
import oracle.kv.impl.util.ServiceUtils;
import oracle.kv.impl.util.ConfigurableService.ServiceStatus;

/**
 * Send a simple newGlobalParameters call to the Admin to refresh its global
 * parameters without a restart.
 */
@Persistent
public class NewAdminGlobalParameters extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private AdminId targetAdminId;
    private AbstractPlan plan;
    private String hostname;
    private int registryPort;

    /* For DPL */
    @SuppressWarnings("unused")
    private NewAdminGlobalParameters() {
    }

    public NewAdminGlobalParameters(AbstractPlan plan,
                                    String hostname,
                                    int registryPort,
                                    AdminId targetAdminId) {
        this.plan = plan;
        this.hostname = hostname;
        this.registryPort = registryPort;
        this.targetAdminId = targetAdminId;
    }

    @Override
    public boolean continuePastError() {
        return false;
    }

    @Override
    public State doWork() throws Exception {
        plan.getLogger().fine(
            "Sending newGlobalParameters to Admin " + targetAdminId);

        final CommandServiceAPI cs = ServiceUtils.waitForAdmin(
            hostname, registryPort, plan.getLoginManager(),
            40, ServiceStatus.RUNNING);

        cs.newGlobalParameters();
        return State.SUCCEEDED;
    }

    @Override
    public String toString() {
       return super.toString() + " cause Admin " + targetAdminId +
           " to refresh its global parameter state without restarting";
    }
}
