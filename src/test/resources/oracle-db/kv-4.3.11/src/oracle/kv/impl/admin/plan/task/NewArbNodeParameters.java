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
import oracle.kv.impl.arb.admin.ArbNodeAdminAPI;
import oracle.kv.impl.topo.ArbNodeId;
import oracle.kv.impl.util.registry.RegistryUtils;

/**
 * Send a simple newParameters call to the ArbNodeAdminAPI to refresh its
 * parameters without a restart.
 */
public class NewArbNodeParameters extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private final ArbNodeId targetNodeId;
    private final AbstractPlan plan;

    public NewArbNodeParameters(AbstractPlan plan,
                                ArbNodeId targetNodeId) {
        this.plan = plan;
        this.targetNodeId = targetNodeId;
    }

    @Override
    public State doWork()
        throws Exception {

        plan.getLogger().log(Level.FINE,
                             "Sending newParameters to {0}", targetNodeId);

        try {
            RegistryUtils registry =
                new RegistryUtils(plan.getAdmin().getCurrentTopology(),
                                  plan.getAdmin().getLoginManager());
            ArbNodeAdminAPI anAdmin = registry.getArbNodeAdmin(targetNodeId);
            anAdmin.newParameters();
        } catch (java.rmi.NotBoundException notbound) {
            plan.getLogger().info(targetNodeId +
                        " cannot be contacted when updating its parameters: " +
                        notbound);
            throw notbound;
        } catch (RemoteException e) {
            plan.getLogger().severe
                         ("Attempting to update parameters for targetNodeId:" +
                          e);
            throw e;
        }
        return State.SUCCEEDED;
    }

    @Override
    public String toString() {
        String retval = super.toString() + " cause " + targetNodeId;
        return retval + " to refresh its parameter state without restarting";
    }

    @Override
    public boolean continuePastError() {
        return false;
    }
}
