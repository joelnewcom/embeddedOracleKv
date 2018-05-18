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

import oracle.kv.impl.admin.param.StorageNodeParams;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.rep.admin.RepNodeAdminAPI;
import oracle.kv.impl.topo.RepNodeId;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.util.registry.RegistryUtils;

import com.sleepycat.persist.model.Persistent;

/**
 * Send a simple newParameters call to the RepNodeAdminAPI to refresh its
 * parameters without a restart.
 *
 * version 0: original.
 * version 1: Changed inheritance chain.
 */
@Persistent(version=1)
public class NewRepNodeParameters extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private StorageNodeId hostSNId; /* TODO: deprecate */
    private RepNodeId targetNodeId;
    private AbstractPlan plan;

    public NewRepNodeParameters() {
    }

    public NewRepNodeParameters(AbstractPlan plan,
                                RepNodeId targetNodeId) {
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
            RepNodeAdminAPI rnAdmin = registry.getRepNodeAdmin(targetNodeId);
            rnAdmin.newParameters();
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
        StorageNodeParams snp =
            (plan.getAdmin() != null ?
             plan.getAdmin().getStorageNodeParams(hostSNId) : null);
        String retval = super.toString() + " cause " + targetNodeId;
        if (snp != null) {
            retval += " on " + snp.displaySNIdAndHost();
        }
        return retval + " to refresh its parameter state without restarting";
    }

    @Override
    public boolean continuePastError() {
        return false;
    }
}
