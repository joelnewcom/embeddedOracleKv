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

import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.CommandResult;
import oracle.kv.impl.admin.CommandResult.CommandFails;
import oracle.kv.impl.admin.VerifyConfiguration;
import oracle.kv.impl.admin.VerifyConfiguration.CompareParamsResult;
import oracle.kv.impl.admin.param.Parameters;
import oracle.kv.impl.admin.param.RepNodeParams;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.fault.CommandFaultException;
import oracle.kv.impl.fault.OperationFaultException;
import oracle.kv.impl.param.LoadParameters;
import oracle.kv.impl.param.ParameterState;
import oracle.kv.impl.rep.admin.RepNodeAdminAPI;
import oracle.kv.impl.sna.StorageNodeAgentAPI;
import oracle.kv.impl.topo.Datacenter;
import oracle.kv.impl.topo.RepNode;
import oracle.kv.impl.topo.RepNodeId;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.topo.Topology;
import oracle.kv.impl.util.ConfigurableService.ServiceStatus;
import oracle.kv.impl.util.registry.RegistryUtils;
import oracle.kv.util.ErrorMessage;

import com.sleepycat.je.rep.NodeType;

/**
 * A task, with associated utility methods, to update parameters for an RN so
 * that the values in the admin database, the SN configuration file, and the
 * in-memory values in the RN service all agree.  Only modifies the
 * configuration file if it is specifically the RN parameters that are
 * different; does not make modifications for global or SN parameters that also
 * apply to RNs.
 */
public class UpdateRepNodeParams extends BasicUpdateParams {
    private static final long serialVersionUID = 1L;

    private final RepNodeId repNodeId;
    private final boolean zoneIsOffline;

    public UpdateRepNodeParams(AbstractPlan plan,
                               RepNodeId repNodeId,
                               boolean zoneIsOffline) {
        super(plan);
        this.repNodeId = repNodeId;
        this.zoneIsOffline = zoneIsOffline;
    }

    @Override
    public State doWork() throws Exception {
        return update(plan, this, repNodeId, zoneIsOffline);
    }

    /**
     * Updates the admin database as needed so that the type of the specified
     * RN matches the type of the node's zone, and performs the necessary
     * updates so that SN configuration parameters and the in-memory parameters
     * for the RN match the values stored in the admin database.  Does nothing
     * if no changes are needed.
     *
     * @param plan the containing plan
     * @param task the working task to set command result. Use null if no task
     * to set command result.
     * @param rnId the ID of the RN to update
     */
    public static State update(AbstractPlan plan, Task task, RepNodeId rnId)
        throws Exception {

        return update(plan, task, rnId, false);
    }

    /*
     * Suppress null warnings, since, although the code makes the correct
     * checks for nulls, there seems to be no other way to make Eclipse happy
     */
    @SuppressWarnings("null")
    private static State update(AbstractPlan plan,
                                Task task,
                                RepNodeId rnId,
                                boolean zoneIsOffline)
        throws Exception {

        /*
         * In most cases, this code modifies parameters in the following order:
         *
         * 1. Remove electable node from the JE rep group DB, when changing the
         * type of an electable node to something else.
         *
         * 2. Modify the parameters in the admin DB.
         *
         * 3. Modify the configuration file stored by the SN.
         *
         * 4. Modify the parameters of the running service, either by notifying
         * the service of the change or, if needed, restarting it.
         *
         * Making the JE rep group DB modification before making any other
         * changes is important because having a node come up as non-electable
         * if the rep group DB says it is electable can result in type conflict
         * errors in JE that will prevent the node from starting.  It is
         * harmless for a node to start up as an electable node and not be
         * present in the JE rep group DB: the node will be added to the JE rep
         * group DB automatically in that case.
         *
         * Note that the only way to provide changed parameters to the service
         * is through the SN configuration file.
         *
         * One exception to the specified order of modifications is made when
         * setting the electable group size override.  In that case, because we
         * want the setting to be temporary, we only modify the SN
         * configuration file and notify the service, but do not make the
         * change to the admin DB.  (This change does not need to be reflected
         * in the JE rep group DB.)  Not making this change in the admin DB
         * means that the change will be reverted automatically by this code if
         * an earlier invocation that has set it fails before it can complete.
         *
         * Another exception is that only the admin DB is modified for nodes in
         * an offline zone.  The RN and the SN for an offline zone are not
         * accessible, so it isn't possible to update their values.  The
         * assumption is that the SN's services will be disabled before it is
         * brought online so that the parameters can be updated before the RN
         * is restarted.
         *
         * Although this code makes modifications in the order described here,
         * we cannot assume that all modifications will occur in that order.
         * For example, failures can mean that only some of the modifications
         * in the prescribed order will have been made.  Canceling a command
         * that has failed and following it with a command the reverts the
         * original change can also change the pattern of changes encountered,
         * as could outside modifications to SN configuration files made by
         * users restoring old file contents after a hardware failure.  This
         * code is intended to be robust in the face of these factors, but does
         * not attempt to handle cases where users have modified individual
         * hidden or sensitive parameters themselves.  For example, by-hand
         * modifications to the electable group size override could cause
         * unexpected problems.
         */

        /* Get admin DB parameters */
        final Admin admin = plan.getAdmin();
        final Parameters dbParams = admin.getCurrentParameters();
        final RepNodeParams rnDbParams = dbParams.get(rnId);
        final Topology topo = admin.getCurrentTopology();
        final RepNode thisRn = topo.get(rnId);
        final StorageNodeId snId = thisRn.getStorageNodeId();
        final Datacenter dc = topo.getDatacenter(snId);
        final NodeType expectedNodeType =
            Datacenter.ServerUtil.getDefaultRepNodeType(dc);
        final NodeType dbNodeType = rnDbParams.getNodeType();
        final boolean updateDbNodeType = !expectedNodeType.equals(dbNodeType);

        /* If zone is offline, just update the admin DB if needed */
        if (zoneIsOffline) {
            if (updateDbNodeType) {
                plan.getLogger().fine("Updating node type in admin DB");
                rnDbParams.setNodeType(expectedNodeType);
                admin.updateParams(rnDbParams);
            }
            return State.SUCCEEDED;
        }

        /*
         * Update the node type locally before performing comparisons, but
         * don't make the change persistent, so we can coordinate that change
         * with other changes.
         */
        if (updateDbNodeType) {
            rnDbParams.setNodeType(expectedNodeType);
        }

        /* Get SN configuration parameters */
        final RegistryUtils regUtils =
            new RegistryUtils(topo, plan.getLoginManager());
        final StorageNodeAgentAPI sna = regUtils.getStorageNodeAgent(snId);
        final LoadParameters configParams = sna.getParams();
        final CompareParamsResult snCompare =
            VerifyConfiguration.compareParams(configParams,
                                              rnDbParams.getMap());

        /* Get in-memory parameters from the RN */
        LoadParameters serviceParams = null;
        try {
            final RepNodeAdminAPI rna = regUtils.getRepNodeAdmin(rnId);
            serviceParams = rna.getParams();
        } catch (RemoteException | NotBoundException e) {
            plan.getLogger().info("Problem calling " + rnId + ": " + e);
        }

        /*
         * Check if parameters file needs to be updated, if the RN needs to
         * read them, and if the RN needs to be restarted.
         */
        final CompareParamsResult serviceCompare;
        final CompareParamsResult combinedCompare;
        if (serviceParams == null) {
            serviceCompare = CompareParamsResult.NO_DIFFS;
            combinedCompare = snCompare;
        } else {
            serviceCompare = VerifyConfiguration.compareServiceParams(
                snId, rnId, serviceParams, dbParams);
            combinedCompare = VerifyConfiguration.combineCompareParamsResults(
                snCompare, serviceCompare);
        }
        if (combinedCompare == CompareParamsResult.MISSING) {
            final String msg = "some parameters were missing";
            logError(plan, rnId, msg);
            if (task != null) {
                final CommandResult taskResult =new CommandFails(
                    msg, ErrorMessage.NOSQL_5400,
                    CommandResult.TOPO_PLAN_REPAIR);
                task.setTaskResult(taskResult);
            }
            return State.ERROR;
        }

        /* No diffs, just update admin DB if needed */
        if (combinedCompare == CompareParamsResult.NO_DIFFS) {
            if (updateDbNodeType) {
                plan.getLogger().fine("Updating node type in admin DB");
                admin.updateParams(rnDbParams);
            }
            return State.SUCCEEDED;
        }

        /* Check if deleting an electable node */
        final boolean deleteElectable;
        if (expectedNodeType.isElectable()) {
            deleteElectable = false;
        } else if (serviceParams == null) {

            /*
             * Since the node isn't up, better do the check to be on the safe
             * side
             */
            deleteElectable = true;
        } else {
            final RepNodeParams rnServiceParams = new RepNodeParams(
                serviceParams.getMap(rnId.getFullName(),
                                     ParameterState.REPNODE_TYPE));
            deleteElectable = rnServiceParams.getNodeType().isElectable();
        }

        /* Not deleting electable node, so no need to reduce quorum */
        if (!deleteElectable) {
            if (updateDbNodeType) {
                plan.getLogger().fine("Updating node type in admin DB");
                admin.updateParams(rnDbParams);
            }
            if (snCompare != CompareParamsResult.NO_DIFFS) {
                plan.getLogger().fine("Updating RN config parameters");
                sna.newRepNodeParameters(rnDbParams.getMap());
            }
            if (serviceCompare == CompareParamsResult.DIFFS) {
                plan.getLogger().fine("Notify RN of new parameters");
                regUtils.getRepNodeAdmin(rnId).newParameters();
            } else {

                /* Stop running node in preparation for restarting it */
                if (serviceCompare == CompareParamsResult.DIFFS_RESTART) {

                    /*
                     * If stopping a primary node, make sure the remaining
                     * members of the group are caughtup.
                     * If changing a secondary to a primary, wait for it to
                     * catch up before stopping. Timing out is not fatal.
                     * In either case do not call stopRN(..., true) as it gets
                     * information to wait from the parameters in the Admin DB
                     * which, at this point, may not match the reality of the
                     * RN.
                     * Use the service parameters if available to determine the
                     * RN type, ortherwise go with original DB info.
                     */
                    final boolean stopPrimary;
                    if (serviceParams == null) {
                        stopPrimary = dbNodeType.isElectable();
                    } else {
                        final RepNodeParams rnServiceParams = new RepNodeParams(
                             serviceParams.getMap(rnId.getFullName(),
                                                  ParameterState.REPNODE_TYPE));
                        stopPrimary =
                                    rnServiceParams.getNodeType().isElectable();
                    }
                    try {
                        if (stopPrimary) {
                            Utils.awaitGroupConsistent(
                                plan, rnId,
                                Utils.getElectableRF(dbParams,
                                                     rnId.getGroupId()));
                        } else if (updateDbNodeType) {
                            Utils.awaitConsistent(plan, rnId);
                        }
                        Utils.stopRN(plan, snId, rnId, false);
                    } catch (OperationFaultException e) {
                        throw new CommandFaultException(
                            e.getMessage(), e, ErrorMessage.NOSQL_5400,
                            CommandResult.PLAN_CANCEL);
                    }
                }

                /*
                 * Restart the node, or start it if it was not running and is
                 * not disabled
                 */
                if ((serviceCompare == CompareParamsResult.DIFFS_RESTART) ||
                    ((serviceParams == null) && !rnDbParams.isDisabled())) {
                    try {
                        Utils.startRN(plan, snId, rnId);
                        Utils.waitForNodeState(plan, rnId,
                                                  ServiceStatus.RUNNING);
                    } catch (Exception e) {
                        throw new CommandFaultException(
                            e.getMessage(), e, ErrorMessage.NOSQL_5400,
                            CommandResult.PLAN_CANCEL); 
                    }
                }
            }
            return State.SUCCEEDED;
        }

        /*
         * Compute the primary RF and check if stopping the node will cause a
         * loss of quorum for RF > 2
         */
        final int primaryRepFactor;
        try {
            primaryRepFactor = Utils.verifyShardHealth(
                dbParams, topo, admin, rnId, plan.getLogger());
        }
        catch (OperationFaultException e) {
            throw new CommandFaultException(
                e.getMessage(), e, ErrorMessage.NOSQL_5400,
                CommandResult.PLAN_CANCEL);
        }
        if ((primaryRepFactor == 1) && (serviceParams != null)) {
            final String msg =
                "changing the type of an electable node is not" +
                " supported for RF=1";
            logError(plan, rnId, msg);
            if (task != null) {
                final CommandResult taskResult =new CommandFails(
                    msg, ErrorMessage.NOSQL_5100,
                    CommandResult.NO_CLEANUP_JOBS);
                task.setTaskResult(taskResult);
            }
            return State.ERROR;
        }

        /* Set electable group size to maintain quorum with RF=2. */
        boolean found = false;
        RepNodeParams otherRnDbParams = null;
        StorageNodeAgentAPI otherSNA = null;
        RepNodeAdminAPI otherRNA = null;
        if (primaryRepFactor == 2) {

            /* Find the other electable RN that will become master */
            for (final RepNodeId anRnId :
                     topo.getSortedRepNodeIds(thisRn.getRepGroupId())) {
                if (rnId.equals(anRnId)) {
                    continue;
                }
                otherRnDbParams = dbParams.get(anRnId);
                try {
                    otherRNA = regUtils.getRepNodeAdmin(anRnId);
                } catch (RemoteException | NotBoundException e) {
                    continue;
                }
                final RepNodeParams otherRnServiceParams = new RepNodeParams(
                    otherRNA.getParams().getMap(anRnId.getFullName(),
                                                ParameterState.REPNODE_TYPE));
                if (!otherRnServiceParams.getNodeType().isElectable()) {
                    continue;
                }
                otherSNA = regUtils.getStorageNodeAgent(
                    otherRnDbParams.getStorageNodeId());
                found = true;
                break;
            }
            if (!found) {
                final String msg = "other electable RN was not found";
                logError(plan, rnId, msg);
                if (task != null) {
                    final CommandResult taskResult =new CommandFails(
                        msg, ErrorMessage.NOSQL_5400,
                        CommandResult.PLAN_CANCEL);
                    task.setTaskResult(taskResult);
                }
                return State.ERROR;
            }

            plan.getLogger().fine("Setting group size override to 1");
            otherRnDbParams.setElectableGroupSizeOverride(1);
            otherSNA.newRepNodeParameters(otherRnDbParams.getMap());
            otherRNA.newParameters();
        }

        try {

            /* Stop node if it is running */
            if (serviceParams != null) {
                /*
                 * Do not call stopRN(..., true) as it gets information to wait
                 * from the parameters in the Admin DB which, at this point,
                 * may not match the reality of the RN.
                 */
                try {
                    Utils.awaitGroupConsistent(plan, rnId, primaryRepFactor);
                    Utils.stopRN(plan, snId, rnId, false);
                } catch (OperationFaultException e) {
                    throw new CommandFaultException(
                        e.getMessage(), e, ErrorMessage.NOSQL_5400,
                        CommandResult.PLAN_CANCEL);
                }
            }

            /*
             * Remove the node from the JE HA rep group because we're changing
             * its type to be not primary
             */
            if (!deleteMember(plan, admin.getReplicationGroupAdmin(rnId),
                              rnId.getFullName(), rnId)) {

                /* Failed -- restart if we stopped it */
                if (serviceParams != null) {
                    Utils.startRN(plan, snId, rnId);
                }
                if (task != null) {
                    final CommandResult taskResult =new CommandFails(
                        "Failed to delete a node from the JE replication group",
                        ErrorMessage.NOSQL_5400, CommandResult.PLAN_CANCEL);
                    task.setTaskResult(taskResult);
                }
                return State.ERROR;
            }
        } finally {
            if (primaryRepFactor == 2) {
                plan.getLogger().fine("Clearing group size override");
                otherRnDbParams.setElectableGroupSizeOverride(0);
                otherSNA.newRepNodeParameters(otherRnDbParams.getMap());
                otherRNA.newParameters();
            }
        }

        if (updateDbNodeType) {
            plan.getLogger().fine("Updating node type in admin DB");
            admin.updateParams(rnDbParams);
        }
        if (snCompare != CompareParamsResult.NO_DIFFS) {
            plan.getLogger().fine("Updating RN config parameters");
            sna.newRepNodeParameters(rnDbParams.getMap());
        }

        /* Start unless stopped because disabled */
        if (!rnDbParams.isDisabled()) {
            try {
                Utils.startRN(plan, snId, rnId);
                Utils.waitForNodeState(plan, rnId, ServiceStatus.RUNNING);
            } catch (Exception e) {
                throw new CommandFaultException(
                    e.getMessage(), e, ErrorMessage.NOSQL_5400,
                    CommandResult.PLAN_CANCEL); 
            }
        }
        return State.SUCCEEDED;
    }

    @Override
    public boolean continuePastError() {
        return false;
    }

    @Override
    public String getName() {
        return super.getName() + " " + repNodeId;
    }

    @Override
    public String toString() {
        return getName();
    }
}
