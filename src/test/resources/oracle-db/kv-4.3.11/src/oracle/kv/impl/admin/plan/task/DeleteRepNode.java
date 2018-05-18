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

import oracle.kv.impl.admin.PlanLocksHeldException;
import oracle.kv.impl.admin.param.StorageNodeParams;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.admin.plan.Planner;
import oracle.kv.impl.sna.StorageNodeAgentAPI;
import oracle.kv.impl.topo.RepNodeId;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.util.registry.RegistryUtils;

import com.sleepycat.persist.model.Persistent;

/**
 * Tell the SN to delete this RN from its configuration file, and from its set
 * of managed processes.
 */
@Persistent
public class DeleteRepNode extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private StorageNodeId snId;
    private RepNodeId rnId;
    private AbstractPlan plan;

    public DeleteRepNode() {
    }

    public DeleteRepNode(AbstractPlan plan,
                         StorageNodeId snId,
                         RepNodeId rnId) {
        this.plan = plan;
        this.snId = snId;
        this.rnId = rnId;
    }

    @Override
    public State doWork()
        throws Exception {

        try {
            RegistryUtils registry =
                new RegistryUtils(plan.getAdmin().getCurrentTopology(),
                                  plan.getAdmin().getLoginManager());
            StorageNodeAgentAPI sna = registry.getStorageNodeAgent(snId);
            sna.destroyRepNode(rnId, true /* deleteData */);
            return State.SUCCEEDED;

        } catch (java.rmi.NotBoundException notbound) {
            plan.getLogger().info
                (snId + " cannot be contacted to delete " + rnId + ":" +
                 notbound);
        } catch (RemoteException e) {
            plan.getLogger().severe
                         ("Attempting to delete " + rnId + " from " + snId +
                          ": " +  e);
        }
        return State.ERROR;
    }

    @Override
    public String toString() {
        StorageNodeParams snp =
            (plan.getAdmin() != null ?
             plan.getAdmin().getStorageNodeParams(snId) : null);
        return super.toString() + " remove " + rnId + " from " +
            (snp != null ? snp.displaySNIdAndHost() : snId);
    }

    @Override
    public boolean continuePastError() {
        return false;
    }

    /* Lock the target RN */
    @Override
    public void lockTopoComponents(Planner planner)
        throws PlanLocksHeldException {
        planner.lock(plan.getId(), plan.getName(), rnId);
    }
}
