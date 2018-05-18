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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.kv.KVVersion;
import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.CommandResult;
import oracle.kv.impl.admin.NonfatalAssertionException;
import oracle.kv.impl.admin.PlanLocksHeldException;
import oracle.kv.impl.admin.TopologyCheck;
import oracle.kv.impl.admin.TopologyCheck.Remedy;
import oracle.kv.impl.admin.TopologyCheckUtils;
import oracle.kv.impl.admin.param.AdminParams;
import oracle.kv.impl.admin.param.ArbNodeParams;
import oracle.kv.impl.admin.param.Parameters;
import oracle.kv.impl.admin.param.RepNodeParams;
import oracle.kv.impl.admin.param.StorageNodeParams;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.admin.plan.Planner;
import oracle.kv.impl.admin.plan.PortTracker;
import oracle.kv.impl.arb.admin.ArbNodeAdminAPI;
import oracle.kv.impl.fault.CommandFaultException;
import oracle.kv.impl.fault.OperationFaultException;
import oracle.kv.impl.security.login.LoginManager;
import oracle.kv.impl.sna.StorageNodeAgentAPI;
import oracle.kv.impl.test.TestHook;
import oracle.kv.impl.test.TestHookExecute;
import oracle.kv.impl.topo.ArbNode;
import oracle.kv.impl.topo.ArbNodeId;
import oracle.kv.impl.topo.RepGroup;
import oracle.kv.impl.topo.RepGroupId;
import oracle.kv.impl.topo.RepNode;
import oracle.kv.impl.topo.RepNodeId;
import oracle.kv.impl.topo.StorageNode;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.topo.Topology;
import oracle.kv.impl.util.VersionUtil;
import oracle.kv.impl.util.ConfigurableService.ServiceStatus;
import oracle.kv.impl.util.registry.RegistryUtils;
import oracle.kv.impl.util.server.LoggerUtils;
import oracle.kv.util.ErrorMessage;

/**
 * Move a single AN to a new storage node.
 * 1. stop/disable AN
 * 2. change params and topo
 * 3. update the other members of the rep group.
 * 4. broadcast the topo changes
 * 5. turn off the disable bit and tell the new SN to deploy the AN
 * 6. wait for the new AN to come up then delete files of the old AN.
 */
public class RelocateAN extends SingleJobTask {
    private static final long serialVersionUID = 1L;

    private final ArbNodeId anId;
    private final StorageNodeId oldSN;
    private final StorageNodeId newSN;
    private final AbstractPlan plan;

    /* Hook to inject failures at different points in task execution */
    public static TestHook<Integer> FAULT_HOOK;

    public RelocateAN(AbstractPlan plan,
                      StorageNodeId oldSN,
                      StorageNodeId newSN,
                      ArbNodeId anId) {

        super();
        this.oldSN = oldSN;
        this.newSN = newSN;
        this.plan = plan;
        this.anId = anId;

        /*
         * This task does not support moving an AN within the same SN.  Also
         * more safeguards should be added when deleting the old AN.
         */
        if (oldSN.equals(newSN)) {
            throw new NonfatalAssertionException("The RelocateAN task does " +
                                                 "not support relocating to " +
                                                 "the same Storage Node");
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
            checker.checkLocation(admin, newSN, anId,
                                  false /* calledByDeployNewRN */,
                                  true /* makeRNEnabled */,
                                  oldSN);
        if (!remedy.isOkay()) {
            logger.info("RelocateAN check of newSN: " + remedy);
        }

        boolean newDone = checker.applyRemedy(remedy, plan);

        remedy =
            checker.checkLocation(admin, oldSN, anId,
                                  false /* calledByDeployNewRN */,
                                  true /* makeRNEnabled */,
                                  oldSN);
        if (!remedy.isOkay()) {
            logger.info("RelocateAN check of oldSN: " + remedy);
        }

        boolean oldDone = checker.applyRemedy(remedy, plan);

        return newDone && oldDone;
    }

    @Override
    public State doWork()
        throws Exception {

        final Admin admin = plan.getAdmin();
        final Logger logger = plan.getLogger();

        /*
         * Prevent the inadvertent downgrade of a AN version by checking
         * that the destination SN is a version that is >= source SN.
         */
        try {
            checkVersions();
        } catch (OperationFaultException e) {
            throw new CommandFaultException(e.getMessage(), e,
                                            ErrorMessage.NOSQL_5200,
                                            CommandResult.NO_CLEANUP_JOBS);
        }

        /*
         * Before doing any work, make sure that the topology, params,
         * SN config files, and JE HA rep group are consistent. This is
         * most definitive if the JE HA repGroupDB can be read, which
         * is only possible if there is a master of the group. The correct
         * location can be deduced in some other limited cases too.
         */
        boolean done = checkAndRepairLocation();

        /* Check the topology after any fixes */
        final Topology current = admin.getCurrentTopology();
        ArbNode an = current.get(anId);

        if (done && an.getStorageNodeId().equals(newSN)) {
            /*
             * The check has been done, any small repairs needed were done, all
             * is consistent, and the AN is already living on the new
             * SN. Nothing more to be done with the topology and params.
             */
            logger.info(anId + " is already on " + newSN +
                        ", no additional metadata changes needed." );
        } else {

            /*
             * There's work to do to update the topology, params, and JE HA
             * repGroupDB. Make sure both old and new SNs are up.
             */
            final LoginManager loginMgr = admin.getLoginManager();
            try {
                Utils.confirmSNStatus(current,
                                      loginMgr,
                                      oldSN,
                                      true,
                                      "Please ensure that " + oldSN +
                                      " is deployed and running before " +
                                      "attempting a relocate " + anId + ".");
                Utils.confirmSNStatus(current,
                                      loginMgr,
                                      newSN,
                                      true,
                                      "Please ensure that " + newSN +
                                      " is deployed and running before " +
                                      "attempting a relocate " + anId + ".");
            } catch (OperationFaultException e) {
                throw new CommandFaultException(e.getMessage(), e,
                                                ErrorMessage.NOSQL_5200,
                                                CommandResult.NO_CLEANUP_JOBS);
            }

            /*
             * Relocation requires bringing down one AN in the shard. Before any
             * work is done, check if the shard is going to be resilient enough.
             * Does it currently have a master? If the target node is brought
             * down, will the shard have a quorum? If at all possible, avoid
             * changing admin metadata if it is very likely that the shard can't
             * update its own JE HA group membership.
             */
            final RepGroupId rgId = current.get(anId).getRepGroupId();
            try {
                Utils.verifyShardHealth(admin.getCurrentParameters(),
                                        current, admin, anId, oldSN, newSN,
                                        plan.getLogger());
            } catch (OperationFaultException e) {
                throw new CommandFaultException(e.getMessage(), e,
                                                ErrorMessage.NOSQL_5400,
                                                CommandResult.PLAN_CANCEL);
            }

            assert TestHookExecute.doHookIfSet(FAULT_HOOK, 1);

            /* Step 1. Stop and disable the AN. */
            try {
                Utils.stopAN(plan, oldSN, anId);
            } catch (Exception e) {
                throw new CommandFaultException(
                    e.getMessage(), e, ErrorMessage.NOSQL_5400,
                    CommandResult.TOPO_PLAN_REPAIR);
            }

            /*
             * Assert that the AN's disable bit is set, because the task cleanup
             * implementation uses that as an indication that step 5 executed.
             */
            ArbNodeParams anp = admin.getArbNodeParams(anId);
            if (!anp.isDisabled()) {
                final String msg = "Expected disabled bit to be set "+
                    "for " + anId  +  ": " + anp;
                throw new CommandFaultException(msg,
                                                new IllegalStateException(msg),
                                                ErrorMessage.NOSQL_5400,
                                                CommandResult.TOPO_PLAN_REPAIR);
            }

            assert TestHookExecute.doHookIfSet(FAULT_HOOK, 2);

            /* Step 2. Change params and topo, as one transaction. */
            changeParamsAndTopo(oldSN, newSN, rgId);

            assert TestHookExecute.doHookIfSet(FAULT_HOOK, 3);

            /*
             * Step 3. Tell the HA group about the new location of this
             * node. This requires a quorum to update the HA group db, and may
             * take some retrying, as step 1 might have actually shut down the
             * master of the HA group.
             */
            try {
                Utils.changeHAAddress(admin.getCurrentTopology(),
                                      admin.getCurrentParameters(),
                                      admin.getParams().getAdminParams(),
                                      anId, oldSN, newSN, plan, logger);
            } catch (OperationFaultException e) {
                throw new CommandFaultException(e.getMessage(), e,
                                                ErrorMessage.NOSQL_5400,
                                                CommandResult.TOPO_PLAN_REPAIR);
            }

            assert TestHookExecute.doHookIfSet(FAULT_HOOK, 4);

            /*
             * Step 4. Send topology change to RNs.
             */
            Topology topo = admin.getCurrentTopology();
            if (!Utils.broadcastTopoChangesToRNs
                (logger, topo,
                 "relocate " + anId + " from " + oldSN + " to " + newSN,
                 admin.getParams().getAdminParams(), plan)) {

                /*
                 * The plan is interrupted before enough nodes saw the new
                 * topology.
                 */
                return State.INTERRUPTED;
            }

            /* Send the updated params to the RN's peers */
            Utils.refreshParamsOnPeers(plan, anId);

            assert TestHookExecute.doHookIfSet(FAULT_HOOK, 5);

            /*
             * Step 5. Remove the disable flag for this AN, and deploy the AN on
             * the new SN.
             */
            startAN(plan, newSN, anId);

            assert TestHookExecute.doHookIfSet(FAULT_HOOK, 6);
        }

        /*
         * Step 6: Destroy the old AN. Make sure the new AN is up and is current
         * with its master. The ANLocationCheck repair does not do this step,
         * so check if it's needed at this time.
         */
        return destroyArbNode();
    }

    /**
     * Complain if the new SN is at an older version than the old SN.
     */
    private void checkVersions() {
        final Admin admin = plan.getAdmin();
        final RegistryUtils regUtils =
            new RegistryUtils(admin.getCurrentTopology(),
                              admin.getLoginManager());

        String errorMsg =  " cannot be contacted. Please ensure that it " +
            "is deployed and running before attempting to deploy " +
            "this topology";

        KVVersion oldVersion = null;
        KVVersion newVersion = null;
        try {
            StorageNodeAgentAPI oldSNA = regUtils.getStorageNodeAgent(oldSN);
            oldVersion = oldSNA.ping().getKVVersion();
        } catch (RemoteException | NotBoundException e) {
            throw new OperationFaultException(oldSN + errorMsg);
        }

        try {
            StorageNodeAgentAPI newSNA = regUtils.getStorageNodeAgent(newSN);
            newVersion = newSNA.ping().getKVVersion();
        } catch (RemoteException | NotBoundException e) {
            throw new OperationFaultException(newSN + errorMsg);
        }

        if (VersionUtil.compareMinorVersion(oldVersion, newVersion) > 0) {
            throw new OperationFaultException
                (anId + " cannot be moved from " +  oldSN + " to " + newSN +
                 " because " + oldSN + " is at version " + oldVersion +
                 " and " + newSN + " is at older version " + newVersion +
                 ". Please upgrade " + newSN +
                 " to a version that is equal or greater than " + oldVersion);
        }
    }

    /**
     * Deletes the old AN on the original SN. Returns SUCCESS if the delete was
     * successful. This method calls awaitCOnsistency() on the new node
     * to make sure it is up and healthy before deleting the old node.
     *
     * @return SUCCESS if the old AN was deleted
     */
    private State destroyArbNode() {
        try {
            if (Utils.destroyArbNode(plan.getAdmin(),
                               plan.getLogger(), oldSN, anId)) {
                return State.SUCCEEDED;
            }
        } catch (InterruptedException ie) {
            return State.INTERRUPTED;
        }
        final String msg = "Time out while waiting for " + anId +
            " to come up on " + newSN +
            " before deleting the ArbNode from its old home on " + oldSN;
        throw new CommandFaultException(msg, new RuntimeException(msg),
                                        ErrorMessage.NOSQL_5400,
                                        CommandResult.TOPO_PLAN_REPAIR);
    }

    /**
     * Start the AN, update its params.
     * @throws RemoteException
     * @throws NotBoundException
     */
    static public void startAN(AbstractPlan plan,
                               StorageNodeId targetSNId,
                               ArbNodeId targetANId)
        throws RemoteException, NotBoundException {

        Admin admin = plan.getAdmin();

        /*
         * Update the SN after any AdminDB param changes are done. Refetch
         * the params and topo because they might have been updated.
         */
        Topology topo = admin.getCurrentTopology();
        ArbNodeParams anp =
            new ArbNodeParams(admin.getArbNodeParams(targetANId));
        if (anp.isDisabled()) {
            anp.setDisabled(false);
            admin.updateParams(anp);
        }
        plan.getLogger().log(Level.INFO,
                             "Starting up {0} on {1} with  {2}",
                             new Object[]{targetANId, targetSNId, anp});

        RegistryUtils regUtils = new RegistryUtils(topo,
                                                   admin.getLoginManager());
        StorageNodeAgentAPI sna =  regUtils.getStorageNodeAgent(targetSNId);

        /*
         * Update the AN's configuration file if the AN is present, since
         * createArbNode only updates the parameters for a new node
         */
        final boolean anExists = sna.arbNodeExists(targetANId);
        if (anExists) {
            sna.newArbNodeParameters(anp.getMap());
        }

        /* Start or create the AN */
        try {
            sna.createArbNode(anp.getMap());
        } catch (IllegalStateException e) {
            throw new CommandFaultException(e.getMessage(), e,
                                            ErrorMessage.NOSQL_5200,
                                            CommandResult.NO_CLEANUP_JOBS);
        }

        /*
         * Refresh the arbNodeAdmin parameters for an existing node in case it
         * was already running, since the start or create will be a no-op if
         * the AN was already up
         */
        if (anExists) {
            try {
                Utils.waitForNodeState(plan, targetANId, ServiceStatus.RUNNING);
            } catch (Exception e) {
                throw new CommandFaultException(e.getMessage(), e,
                                                ErrorMessage.NOSQL_5400,
                                                CommandResult.TOPO_PLAN_REPAIR);
            }
            ArbNodeAdminAPI anAdmin = regUtils.getArbNodeAdmin(targetANId);
            anAdmin.newParameters();
        }

        /* Register this arbNode with the monitor. */
        StorageNode sn = topo.get(targetSNId);
        admin.getMonitor().registerAgent(sn.getHostname(),
                                         sn.getRegistryPort(),
                                         targetANId);
    }

    /**
     * Update and persist the params and topo to make the AN refer to the new
     * SN. Check to see if this has already occurred, to make the work
     * idempotent.
     */
    private void changeParamsAndTopo(StorageNodeId before,
                                     StorageNodeId after,
                                     RepGroupId rgId) {

        Parameters parameters = plan.getAdmin().getCurrentParameters();
        Topology topo = plan.getAdmin().getCurrentTopology();
        PortTracker portTracker = new PortTracker(topo, parameters, after);

        /* Modify pertinent params and topo */
        StorageNodeId origParamsSN = parameters.get(anId).getStorageNodeId();
        StorageNodeId origTopoSN = topo.get(anId).getStorageNodeId();
        ChangedParams changedParams = transferANParams
            (parameters, portTracker, topo, before, after, rgId);
        Set<ArbNodeParams> changedANParams = changedParams.getANP();
        Set<RepNodeParams> changedRNParams = changedParams.getRNP();
        boolean topoChanged = transferTopo(topo, before, after);

        /*
         * Sanity check that params and topo are in sync, both should be
         * either unchanged or changed
         */
        if (!changedANParams.isEmpty() != topoChanged) {
            final String msg =
                anId + " params and topo out of sync. Original params SN=" +
                origParamsSN + ", orignal topo SN=" + origTopoSN +
                " source SN=" + before + " destination SN=" + after;
            throw new CommandFaultException(msg,
                                            new IllegalStateException(msg),
                                            ErrorMessage.NOSQL_5200,
                                            CommandResult.NO_CLEANUP_JOBS);
        }

        /* Only do the update if there has been a change */
        Logger logger = plan.getLogger();
        if (!(topoChanged || !changedRNParams.isEmpty())) {
            logger.log(Level.INFO,
                       "No change to params or topology, no need to update " +
                       "in order to move {0} from {1} to {2}",
                       new Object[]{anId, before, after});
            return;
        }

        plan.getAdmin().saveTopoAndParams(topo,
                                          plan.getDeployedInfo(),
                                          changedRNParams,
                                          Collections.<AdminParams>emptySet(),
                                          changedANParams,
                                          plan);
        logger.log(Level.INFO,
                   "Updating params and topo for move of {0} from " +
                   "{1} to {2}: {3}",
                   new Object[]{anId, before, after, changedANParams});

    }

    /**
     * The params fields that have to be updated are:
     * For the AN that is to be moved:
     *   a. new JE HA nodehostport value
     *   b. new mount point
     *   c. new storage node id
     *   d. calculate JE cache size, which may change due to the capacity
     *      and memory values of the destination storage node.
     * For the other RNs in this shard:
     *   a. new helper host values, that point to this new location for our
     *      relocated RN
     */
    private ChangedParams transferANParams(Parameters parameters,
                                           PortTracker portTracker,
                                           Topology topo,
                                           StorageNodeId before,
                                           StorageNodeId after,
                                           RepGroupId rgId) {

        Set<ArbNodeParams> changedAnp = new HashSet<>();
        Set<RepNodeParams> changedRnp = new HashSet<>();

        ArbNodeParams anp = parameters.get(anId);

        if (anp.getStorageNodeId().equals(after)) {

            /* We're done, this task ran previously. */
            plan.getLogger().info(anId + " already transferred to " + after);
            return new ChangedParams(changedAnp, changedRnp);
        }

        /*
         * Sanity check -- this RNP should be pointing to the before SN, not
         * to some third party SN!
         */
        if (!anp.getStorageNodeId().equals(before)) {
            final String msg =
                "Attempted to transfer " + anId + " from " + before + " to " +
                after + " but unexpectedly found it residing on " +
                anp.getStorageNodeId();
                throw new CommandFaultException(msg,
                                                new IllegalStateException(msg),
                                                ErrorMessage.NOSQL_5200,
                                                CommandResult.NO_CLEANUP_JOBS);
        }

        /*
         * Change the SN, helper hosts, nodeHostPort
         */
        int haPort = portTracker.getNextPort(after);

        String newSNHAHostname = parameters.get(after).getHAHostname();
        String oldNodeHostPort = anp.getJENodeHostPort();
        String nodeHostPort = newSNHAHostname + ":" + haPort;
        plan.getLogger().log(Level.INFO,
                             "transferring HA port for {0} from {1} to {2}",
                             new Object[]{anp.getArbNodeId(), oldNodeHostPort,
                                          nodeHostPort});

        anp.setStorageNodeId(after);
        anp.setJENodeHostPort(nodeHostPort);

        /*
         * Setting the helper hosts is not strictly necessary, as it should
         * not have changed, but take this opportunity to update the helper
         * list in case a previous param change had been interrupted.
         */
        anp.setJEHelperHosts(
            TopologyCheckUtils.findPeerHelpers(anId, parameters, topo));

        changedAnp.add(anp);

        /* Change the helper hosts for other RNs in the group. */
        for (RepNode peer : topo.get(rgId).getRepNodes()) {
            RepNodeId peerId = peer.getResourceId();
            if (peerId.equals(anId)) {
                continue;
            }

            RepNodeParams peerParam = parameters.get(peerId);
            String oldHelper = peerParam.getJEHelperHosts();
            String newHelpers = oldHelper.replace(oldNodeHostPort,
                                                  nodeHostPort);
            peerParam.setJEHelperHosts(newHelpers);
            changedRnp.add(peerParam);
        }
        return new ChangedParams(changedAnp, changedRnp);
    }

    /**
     * Find all RepNodes that refer to the old node, and update the topology to
     * refer to the new node.
     * @return true if a change has been made, return false if the AN is already
     * on the new SN.
     */
    private boolean transferTopo(Topology topo, StorageNodeId before,
                                 StorageNodeId after) {

        ArbNode an = topo.get(anId);
        StorageNodeId inUseSNId = an.getStorageNodeId();
        if (inUseSNId.equals(before)) {
            ArbNode updatedAN = new ArbNode(after);
            RepGroup rg = topo.get(an.getRepGroupId());
            rg.update(an.getResourceId(),updatedAN);
            return true;
        }

        if (inUseSNId.equals(after)) {
            return false;
        }

        final String msg =
            an + " expected to be on old SN " + before + " or new SN " + after +
            " but instead is on " + inUseSNId;
        throw new CommandFaultException(msg, new RuntimeException(msg),
                                        ErrorMessage.NOSQL_5200,
                                        CommandResult.NO_CLEANUP_JOBS);
    }

    @Override
    public boolean continuePastError() {
        return false;
    }

    @Override
    public Runnable getCleanupJob() {
        return new Runnable() {
           @Override
               public void run() {
               try {
                   cleanupRelocation();
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
    private void cleanupRelocation()
        throws RemoteException, NotBoundException {

        assert TestHookExecute.doHookIfSet(FAULT_HOOK, 7);

        boolean done = checkAndRepairLocation();
        final Topology current = plan.getAdmin().getCurrentTopology();
        ArbNode an = current.get(anId);

        if (done) {
            if (an.getStorageNodeId().equals(newSN)) {
                plan.getLogger().info("In RelocateAN cleanup, shard is " +
                                      " consistent, " + anId +
                                      " is on the target " + newSN);

                /* attempt to delete the old AN */
                destroyArbNode();
            }
            plan.getLogger().info("In RelocateAN cleanup, shard is " +
                                  "consistent, " + anId + " is on " +
                                  an.getStorageNodeId());
        } else {
            plan.getLogger().info("In RelocateAN cleanup, shard did not have " +
                                  "master, no cleanup attempted since " +
                                  "authoritative information is lacking.");
        }
    }

    /**
     * This is the older style cleanup, which attempts to reason about how far
     * the task proceeded, and then attempts to revert to the previous state.
     */
    @SuppressWarnings("unused")
    private boolean checkLocationConsistency()
        throws InterruptedException, RemoteException, NotBoundException {

        Admin admin = plan.getAdmin();
        assert TestHookExecute.doHookIfSet(FAULT_HOOK, 7);

        /*
         * If step 5 occurred (enable bit on, AN pointing to new SN, then the HA
         * group and the params/topo are consistent, so attempt to delete the
         * old RN.
         */
        ArbNodeParams anp = admin.getArbNodeParams(anId);
        if ((anp.getStorageNodeId().equals(newSN)) &&
            !anp.isDisabled()) {
            return
                destroyArbNode() == State.SUCCEEDED;
        }

        /*
         * If the ArbNodeParams still point at the old SN, steps 2 and 3 did
         * not occur, nothing to clean up
         */
        if (anp.getStorageNodeId().equals(oldSN)) {
            /*
             * If the original AN was disabled, attempt to re-enable it. Note
             * that this may enable a node which was disabled before the plan
             * run.
             */
            if (anp.isDisabled()) {
                Utils.startAN(plan, oldSN, anId);
            }
            return true;
        }

        /*
         * We are somewhere between steps 1 and 5. Revert both of the kvstore
         * params and topo, and the JE HA update, and the peer RNs helper
         * hosts.
         */
        Topology topo = admin.getCurrentTopology();
        changeParamsAndTopo(newSN, oldSN, topo.get(anId).getRepGroupId());
        Utils.refreshParamsOnPeers(plan, anId);

        Utils.changeHAAddress(topo,
                              admin.getCurrentParameters(),
                              admin.getParams().getAdminParams(),
                              anId, newSN, oldSN, plan, plan.getLogger());

        /* refresh the topo, it's been updated */
        topo = admin.getCurrentTopology();
        if (!Utils.broadcastTopoChangesToRNs(plan.getLogger(),
                                             topo,
                                            "revert relocation of  " + anId +
                                             " and move back from " +
                                             newSN + " to " + oldSN,
                                             admin.getParams().getAdminParams(),
                                             plan)) {
            /*
             * The plan is interrupted before enough nodes saw the new
             * topology.
             */
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StorageNodeParams snpOld =
            (plan.getAdmin() != null ?
             plan.getAdmin().getStorageNodeParams(oldSN) : null);
        StorageNodeParams snpNew =
            (plan.getAdmin() != null ?
             plan.getAdmin().getStorageNodeParams(newSN) : null);
        return super.toString() + " move " + anId + " from " +
            (snpOld != null ? snpOld.displaySNIdAndHost() : oldSN) +
            " to " +
            (snpNew != null ? snpNew.displaySNIdAndHost() : newSN);
    }

    @Override
    public void lockTopoComponents(Planner planner)
        throws PlanLocksHeldException {
        planner.lockShard(plan.getId(), plan.getName(),
                          new RepGroupId(anId.getGroupId()));
    }

    class ChangedParams {
        private final Set<ArbNodeParams> anParams;
        private final Set<RepNodeParams> rnParams;
        ChangedParams(Set<ArbNodeParams> anp, Set<RepNodeParams> rnp) {
            anParams = anp;
            rnParams = rnp;
        }
        Set<ArbNodeParams> getANP() {
            return anParams;
        }
        Set<RepNodeParams> getRNP() {
            return rnParams;
        }

    }
}
