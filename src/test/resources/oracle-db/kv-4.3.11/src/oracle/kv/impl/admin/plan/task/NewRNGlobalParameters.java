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

import java.rmi.RemoteException;
import java.util.logging.Level;

import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.rep.admin.RepNodeAdminAPI;
import oracle.kv.impl.topo.RepNodeId;
import oracle.kv.impl.util.registry.RegistryUtils;

import com.sleepycat.persist.model.Persistent;

/**
 * Send a simple newGlobalParameters call to the RepNodeAdminAPI to refresh its
 * global parameters without a restart.
 */
@Persistent
public class NewRNGlobalParameters extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private RepNodeId targetNodeId;
    private AbstractPlan plan;

    public NewRNGlobalParameters(AbstractPlan plan,
                                 RepNodeId targetNodeId) {
        this.plan = plan;
        this.targetNodeId = targetNodeId;
    }

    /* For DPL */
    @SuppressWarnings("unused")
    private NewRNGlobalParameters() {
    }

    @Override
    public boolean continuePastError() {
        return false;
    }

    @Override
    public State doWork() throws Exception {
        plan.getLogger().log(Level.FINE,
            "Sending newGlobalParameters to {0}", targetNodeId);

        try {
            final RegistryUtils registry =
                new RegistryUtils(plan.getAdmin().getCurrentTopology(),
                                  plan.getAdmin().getLoginManager());
            final RepNodeAdminAPI rnAdmin =
                registry.getRepNodeAdmin(targetNodeId);
            rnAdmin.newGlobalParameters();
        } catch (java.rmi.NotBoundException notbound) {
            plan.getLogger().info(targetNodeId +
                " cannot be contacted when updating its global parameters: " +
                notbound);
            throw notbound;
        } catch (RemoteException e) {
            plan.getLogger().severe(
                "Attempting to update global parameters for targetNodeId:" +
                e);
            throw e;
        }
        return State.SUCCEEDED;
    }

    @Override
    public String toString() {
        return super.toString() + targetNodeId +
               " to refresh its global parameter state without restarting";
    }
}
