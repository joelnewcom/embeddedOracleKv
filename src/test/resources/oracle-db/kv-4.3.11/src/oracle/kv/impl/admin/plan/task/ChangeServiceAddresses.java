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

import oracle.kv.impl.admin.CommandServiceAPI;
import oracle.kv.impl.admin.param.AdminParams;
import oracle.kv.impl.admin.param.ArbNodeParams;
import oracle.kv.impl.admin.param.Parameters;
import oracle.kv.impl.admin.param.RepNodeParams;
import oracle.kv.impl.admin.param.StorageNodeParams;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.admin.plan.TopologyPlan;
import oracle.kv.impl.fault.OperationFaultException;
import oracle.kv.impl.topo.AdminId;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.util.registry.RegistryUtils;

import com.sleepycat.persist.model.Persistent;

/**
 * Change the JE HA rep node address for the target rep nodes and admins by
 * using JE HA utilities to inform rep group members of the change.
 *
 * version 0: original.
 * version 1: Changed inheritance chain.
 */
@Persistent(version=1)
public class ChangeServiceAddresses extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private StorageNodeId oldNode;
    private StorageNodeId newNode;
    private TopologyPlan plan;

    public ChangeServiceAddresses(TopologyPlan plan,
                                  StorageNodeId oldNode,
                                  StorageNodeId newNode) {
        super();
        this.oldNode = oldNode;
        this.newNode = newNode;
        this.plan = plan;
    }

    /*
     * No-arg ctor for use by DPL.
     */
    @SuppressWarnings("unused")
    private ChangeServiceAddresses() {
    }


    @Override
    public State doWork()
        throws Exception {

        Parameters params = plan.getAdmin().getCurrentParameters();

        for (AdminParams ap: params.getAdminParams()) {
            if (ap.getStorageNodeId().equals(newNode)) {
                changeAdminHAAddress(params, ap.getAdminId());
            }
        }

        /* Find the admin params for this particular admin instance. */
        AdminParams adminParams = plan.getAdmin().getParams().getAdminParams();
        for (RepNodeParams rnp: params.getRepNodeParams()) {
            if (rnp.getStorageNodeId().equals(newNode)) {
                Utils.changeHAAddress(plan.getTopology(), params, adminParams,
                                      rnp.getRepNodeId(), oldNode, newNode,
                                      plan, plan.getLogger());
            }
        }
        for (ArbNodeParams anp: params.getArbNodeParams()) {
            if (anp.getStorageNodeId().equals(newNode)) {
                Utils.changeHAAddress(plan.getTopology(), params, adminParams,
                                      anp.getArbNodeId(), oldNode, newNode,
                                      plan, plan.getLogger());
            }
        }

        return Task.State.SUCCEEDED;
    }

    /**
     * Find a fellow member of this admin's rep group, and ask it to change HA
     * addresses for the target RN. Try all members until someone succeeds.
     * Throws exception if no-one can successfully change the address.
     */
    private void changeAdminHAAddress(Parameters parameters,
                                      AdminId targetId) {
        String problemMsg = " for HA address update of " + targetId +
                " while replacing " + oldNode + " with " + newNode + " :";
        changeAdminHAAddress(plan, problemMsg, parameters, targetId);

    }

    /* For use by this task, and by repairs */
    public static void changeAdminHAAddress(AbstractPlan plan,
                                            String problemMsg,
                                            Parameters parameters,
                                            AdminId targetId) {

        /*
         * Find the an admin that is not the target admin, and ask it
         * to update HA addresses.
         */
        boolean done = false;

        AdminParams targetAP = parameters.get(targetId);
        String targetHelperHosts = targetAP.getHelperHosts();
        String targetNodeHostPort = targetAP.getNodeHostPort();

        for (AdminParams peerAP: parameters.getAdminParams()) {

            AdminId peerId = peerAP.getAdminId();
            if (peerId.equals(targetId)) {
                continue;
            }

            /*
             * Found a peer admin. Try all members of the group until one
             * permits us to make the change.
             */
            StorageNodeParams peerSNP =
                parameters.get(peerAP.getStorageNodeId());

            try {
                CommandServiceAPI cs =
                    RegistryUtils.getAdmin(peerSNP.getHostname(),
                                           peerSNP.getRegistryPort(),
                                           plan.getLoginManager());
                cs.updateMemberHAAddress(targetId, targetHelperHosts,
                                         targetNodeHostPort);

                done = true;
                break;
            } catch (java.rmi.NotBoundException notbound) {
                plan.getLogger().info
                    (peerId + " cannot be contacted" + problemMsg + notbound);
            } catch (RemoteException e) {
                plan.getLogger().severe(peerId + " exception" + problemMsg + e);
            }
        }

        if (!done) {
            throw new OperationFaultException
                ("Couldn't change HA address for " + targetId + " to " +
                 targetNodeHostPort + " " + problemMsg);
        }
    }

    @Override
    public boolean continuePastError() {
        return false;
    }
}
