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

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.CommandResult;
import oracle.kv.impl.admin.TopologyCheck;
import oracle.kv.impl.admin.TopologyCheck.Remedy;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.fault.CommandFaultException;
import oracle.kv.impl.fault.OperationFaultException;
import oracle.kv.impl.sna.StorageNodeAgentAPI;
import oracle.kv.impl.test.TestHook;
import oracle.kv.impl.test.TestHookExecute;
import oracle.kv.impl.topo.ArbNode;
import oracle.kv.impl.topo.ArbNodeId;
import oracle.kv.impl.topo.RepGroupId;
import oracle.kv.impl.topo.RepNode;
import oracle.kv.impl.topo.RepNodeId;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.topo.Topology;
import oracle.kv.impl.util.registry.RegistryUtils;
import oracle.kv.impl.util.server.LoggerUtils;
import oracle.kv.util.ErrorMessage;

/**
 * Remove a single ArbNode. This
 * requires:
 *
 * 1. Insure the RNs in the group are alive and current.
 * 2. Disable and shut down AN. Change parameters to indicate disabled.
 * 3. Remove entry from JEHA
 * 4. Removing AN from SN.
 * 5. Remove AN from topology and parameters.
 * 6. Notify members of change.
 *
 * The above order of operations is implicitly tied into how repair (Remedy)
 * work. The TopologyCheck.checkLocation() method relies on this order
 * to generate the Remedy.
 */
public class RemoveAN extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private final AbstractPlan plan;
    private final ArbNodeId anId;
    private final StorageNodeId snId;

    /* Hook to inject failures at different points in task execution */
    public static TestHook<String> FAULT_HOOK;

    public RemoveAN(AbstractPlan plan,
                    ArbNodeId anId) {

        super();
        this.plan = plan;
        this.anId = anId;
        final Admin admin = plan.getAdmin();
        final Topology current = admin.getCurrentTopology();
        ArbNode an = current.get(anId);
        snId = an.getStorageNodeId();
    }

    @Override
    public State doWork()
        throws Exception {
        Logger logger = plan.getLogger();
        final Admin admin = plan.getAdmin();
        final Topology current = admin.getCurrentTopology();
        ArbNode an = current.get(anId);
        if (an == null) {
            /* Already removed from topo. */
            return Task.State.SUCCEEDED;
        }
        RepGroupId rgId = an.getRepGroupId();
        String helperHosts =
            admin.getCurrentParameters().get(anId).getJEHelperHosts();

        logger.info("Removing AN " + anId);
        /* If AN is part of topo disable and stop it. */
        if (!waitForGroup(an.getRepGroupId())) {
            return Task.State.INTERRUPTED;
        }
        try {
            Utils.stopAN(plan, an.getStorageNodeId(), anId);
        } catch (Exception e) {
            throw new CommandFaultException(
                e.getMessage(), e, ErrorMessage.NOSQL_5400,
                CommandResult.TOPO_PLAN_REPAIR);
        }

        assert TestHookExecute.doHookIfSet(FAULT_HOOK, makeHookTag("1"));

        /* Remove AN from JEHA group */
        try {
            Utils.removeHAAddress(admin.getCurrentTopology(),
                                  admin.getParams().getAdminParams(),
                                  anId, an.getStorageNodeId(), plan,
                                  rgId, helperHosts, logger);
        } catch (OperationFaultException e) {
            throw new CommandFaultException(e.getMessage(), e,
                                            ErrorMessage.NOSQL_5400,
                                            CommandResult.TOPO_PLAN_REPAIR);
        }

        assert TestHookExecute.doHookIfSet(FAULT_HOOK, makeHookTag("2"));

        logger.log(Level.INFO,
                   "Attempting to delete {0} from {1}",
                   new Object[]{anId, an.getStorageNodeId()});
        Topology useTopo = admin.getCurrentTopology();
        RegistryUtils registry =
            new RegistryUtils(useTopo, admin.getLoginManager());
        StorageNodeAgentAPI sna =
            registry.getStorageNodeAgent(an.getStorageNodeId());
        try {
            sna.destroyArbNode(anId, true /* deleteData */);
        } catch (Exception e) {
            throw new CommandFaultException(e.getMessage(), e,
                                            ErrorMessage.NOSQL_5400,
                                            CommandResult.TOPO_PLAN_REPAIR);
        }

        assert TestHookExecute.doHookIfSet(FAULT_HOOK, makeHookTag("3"));

        current.remove(anId);
        plan.getAdmin().saveTopoAndRemoveAN(current,
                                            plan.getDeployedInfo(),
                                            anId, plan);

        assert TestHookExecute.doHookIfSet(FAULT_HOOK, makeHookTag("4"));

        /* Send topology changes to all nodes.*/
        if (!Utils.broadcastTopoChangesToRNs(plan.getLogger(),
                                             current,
                                             "remove AN " + anId,
                                             admin.getParams().
                                             getAdminParams(),
                                             plan)) {
            return Task.State.INTERRUPTED;
        }

        logger.info("Removed AN " + anId);
        return Task.State.SUCCEEDED;
    }

    @Override
    public boolean continuePastError() {
        return false;
    }

    @Override
    public String toString() {
        if (anId == null) {
            return super.toString();
        }
        return super.toString() + " " + anId;
    }

    @Override
    public String getName() {
        if (anId == null) {
            return super.getName();
        }
        return super.getName() + " " + anId;
    }


    @Override
    public Runnable getCleanupJob() {
        return new Runnable() {
           @Override
           public void run() {
               try {
                   cleanup();
               } catch (Exception e) {
                   plan.getLogger().log
                       (Level.SEVERE,
                        "{0}: problem when cancelling relocation {1}",
                        new Object[] {this, LoggerUtils.getStackTrace(e)});

                   /*
                    * Don't try to continue with cleanup; a problem has
                    * occurred. Future, additional invocations of the plan
                    * will have to figure out the context and do cleanup.
                    */
                   throw new RuntimeException(e);
               }
           }
        };
    }

    /**
     * This method calls awaitConsistency() on the RN nodes
     * in the rep group
     * to make sure all are up and healthy before deleting the old node.
     *
     * @return SUCCESS if the old RN was deleted
     * @throws InterruptedException
     */
    private boolean waitForGroup(RepGroupId rgId)
        throws InterruptedException {

        final Admin admin = plan.getAdmin();
        Topology current = admin.getCurrentTopology();

        for (RepNode rn : current.get(rgId).getRepNodes()) {
            RepNodeId rnId = rn.getResourceId();
            if (!Utils.waitForRepNode(plan, rnId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Do the minimum cleanup : when this task ends, check
     *  - the kvstore metadata as known by the admin (params, topo)
     *  - the configuration information, including helper hosts, as stored in
     *  the SN config file
     *  - the JE HA groupdb
     * and attempt to leave it all consistent. Do not necessarily try to revert
     * to the topology before the task.
     * @throws NotBoundException
     * @throws RemoteException
     */
    private void cleanup()
        throws RemoteException, NotBoundException {

        assert TestHookExecute.doHookIfSet(FAULT_HOOK, makeHookTag("5"));

        boolean done = checkAndRepairLocation();
        if (!done) {
            plan.getLogger().info("In RemoveAN cleanup, shard did not have " +
                                  "master, no cleanup attempted since " +
                                  "authoritative information is lacking.");
        }
    }


    /**
     * Use the RNLocationCheck and the current state of the JE HA repGroupDB to
     * repair any inconsistencies between the AdminDB, the SNA config files,
     * and the JE HA repGroupDB.
     * @throws NotBoundException
     * @throws RemoteException
     */
    private boolean checkAndRepairLocation()
        throws RemoteException, NotBoundException {

        Admin admin = plan.getAdmin();
        Logger logger = plan.getLogger();
        TopologyCheck checker =
            new TopologyCheck(logger,
                              admin.getCurrentTopology(),
                              admin.getCurrentParameters());

        /* ApplyRemedy will throw an exception if there is a problem */
        Remedy remedy =
            checker.checkLocation(admin, snId, anId,
                                  false /* calledByDeployNewRN */,
                                  true /* mustReeanableRN */,
                                  null /* oldSN */);
        if (!remedy.isOkay()) {
            logger.info("RemoveAN check of SN: " + remedy);
        }

        return  checker.applyRemedy(remedy, plan);
    }

    /**
     * For unit test support -- make a string that uniquely identifies when
     * this task executes on a given SN
     */
    private String makeHookTag(String pointName) {
        return "RemoveAN/" + snId + "_pt" + pointName;
    }
}
