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

import static oracle.kv.impl.util.ObjectUtil.checkNull;

import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.IllegalCommandException;
import oracle.kv.impl.admin.param.AdminParams;
import oracle.kv.impl.admin.param.ArbNodeParams;
import oracle.kv.impl.admin.param.Parameters;
import oracle.kv.impl.admin.param.RepNodeParams;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.arb.admin.ArbNodeAdminAPI;
import oracle.kv.impl.param.LoadParameters;
import oracle.kv.impl.param.ParameterState;
import oracle.kv.impl.param.ParameterUtils;
import oracle.kv.impl.rep.admin.RepNodeAdminAPI;
import oracle.kv.impl.security.login.LoginManager;
import oracle.kv.impl.topo.AdminId;
import oracle.kv.impl.topo.ArbNode;
import oracle.kv.impl.topo.ArbNodeId;
import oracle.kv.impl.topo.Datacenter;
import oracle.kv.impl.topo.DatacenterId;
import oracle.kv.impl.topo.RepGroup;
import oracle.kv.impl.topo.RepGroupId;
import oracle.kv.impl.topo.RepNode;
import oracle.kv.impl.topo.RepNodeId;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.topo.Topology;
import oracle.kv.impl.util.ConfigurableService.ServiceStatus;
import oracle.kv.impl.util.registry.RegistryUtils;

import com.sleepycat.je.OperationFailureException;
import com.sleepycat.je.rep.MasterStateException;
import com.sleepycat.je.rep.MemberActiveException;
import com.sleepycat.je.rep.MemberNotFoundException;
import com.sleepycat.je.rep.NodeType;
import com.sleepycat.je.rep.ReplicationGroup;
import com.sleepycat.je.rep.ReplicationNode;
import com.sleepycat.je.rep.UnknownMasterException;
import com.sleepycat.je.rep.util.ReplicationGroupAdmin;
import com.sleepycat.je.rep.utilint.HostPortPair;

/**
 * A task, and associated utility methods, for repairing quorum for a shard.
 */
public class RepairShardQuorum extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private final AbstractPlan plan;
    private final RepGroupId rgId;
    private final Set<DatacenterId> allPrimaryZones;
    private final Set<DatacenterId> offlineZones;

    /**
     * Creates the task.  The primary and offline zones must not overlap and
     * must include all primary zones in the current topology.
     *
     * @param plan the associated plan
     * @param rgId the ID of the shard to repair
     * @param allPrimaryZones IDs of all primary zones
     * @param offlineZones IDs of offline zones
     */
    public RepairShardQuorum(AbstractPlan plan,
                             RepGroupId rgId,
                             Set<DatacenterId> allPrimaryZones,
                             Set<DatacenterId> offlineZones) {
        this.plan = plan;
        this.rgId = rgId;
        this.allPrimaryZones = allPrimaryZones;
        this.offlineZones = offlineZones;
    }

    @Override
    public State doWork() throws Exception {
        if (!repairQuorum(plan, rgId, allPrimaryZones, offlineZones)) {
            return State.ERROR;
        }
        return State.SUCCEEDED;
    }

    /**
     * Verify that there are no active RNs in the specified offline zones, and
     * that a quorum of RNs is active in each shard in the specified primary
     * zones.  The primary and offline zones must not overlap and must include
     * all primary zones in the current topology.
     *
     * @param plan the running plan
     * @param allPrimaryZones the IDs of all proposed primary zones
     * @param offlineZones the IDs of all zones believed to be offline
     * @param topo the topology to use for the check
     * @param params the parameters to use for the check
     * @param loginManager the login manager
     * @param logger the logger to use for logging
     * @throws IllegalCommandException if any RNs in the shard in offline zones
     * are found to be active, or if the number of active RNs per shard in
     * primary zones is less than the quorum
     */
    public static void verify(AbstractPlan plan,
                              Set<DatacenterId> allPrimaryZones,
                              Set<DatacenterId> offlineZones,
                              Topology topo,
                              Parameters params,
                              LoginManager loginManager,
                              Logger logger) {
        verify(plan, null, allPrimaryZones, offlineZones,
               getRNInfo(null, topo, loginManager, logger), topo, params);
    }

    /**
     * Verify a single shard using the specified RNInfo, or all shards if
     * rgId is null.
     */
    private static void verify(AbstractPlan plan,
                               RepGroupId rgId,
                               Set<DatacenterId> allPrimaryZones,
                               Set<DatacenterId> offlineZones,
                               Map<RepNodeId, RNInfo> rnInfoMap,
                               Topology topo,
                               Parameters params) {
        assert Collections.disjoint(allPrimaryZones, offlineZones);
        int primaryRepFactor = 0;
        for (final Datacenter dc : topo.getDatacenterMap().getAll()) {
            final DatacenterId dcId = dc.getResourceId();
            assert !dc.getDatacenterType().isPrimary() ||
                offlineZones.contains(dcId) ||
                allPrimaryZones.contains(dcId)
                : "Existing primary zones should be specified in offline" +
                " or primary zones sets";
            if (allPrimaryZones.contains(dcId)) {
                primaryRepFactor += dc.getRepFactor();
            }
        }
        final int quorum = (primaryRepFactor + 1) / 2;
        final StringBuilder errorMessage = new StringBuilder();
        Collection<RepGroup> repGroups = (rgId != null) ?
            Collections.singleton(topo.get(rgId)) :
            topo.getRepGroupMap().getAll();
        for (final RepGroup rg : repGroups) {
            int active = 0;
            for (final RepNode rn : rg.getRepNodes()) {
                final RepNodeId rnId = rn.getResourceId();
                final DatacenterId dcId = topo.getDatacenterId(rnId);
                final RNInfo rnInfo = rnInfoMap.get(rnId);
                if (rnInfo == null) {
                    continue;
                }
                if (offlineZones.contains(dcId)) {
                    errorMessage.append("\n  Node ").append(rnId)
                                .append(" was found active in offline zone ")
                                .append(topo.get(dcId));
                }
                if (allPrimaryZones.contains(dcId)) {
                    active++;
                }
            }
            if (active < quorum) {
                errorMessage.append("\n  Insufficient active nodes in shard ")
                            .append(rg.getResourceId())
                            .append(". Found ").append(active)
                            .append(" active RNs, but require ").append(quorum)
                            .append(" for quorum");
            }
        }

        /* Check for Admins still alive in the offline zone */
        final Map<AdminId, ServiceStatus> status =
                Utils.getAdminStatus(plan, params);
        for (Entry<AdminId, ServiceStatus> adminStatus : status.entrySet()) {
            final AdminId adminId = adminStatus.getKey();
            final AdminParams adminParams = params.get(adminId);
            final StorageNodeId snId = adminParams.getStorageNodeId();
            final DatacenterId dcId = topo.getDatacenter(snId).getResourceId();
            if (offlineZones.contains(dcId) &&
                adminStatus.getValue().isAlive()) {
                errorMessage.append("\n  Admin ").append(adminId)
                            .append(" was found active in offline zone ")
                            .append(topo.get(dcId));
            }
        }

        if (errorMessage.length() > 0) {
            throw new IllegalCommandException(
                "Verification for failover failed:" + errorMessage);
        }
    }

    /**
     * Repairs the shard quorum by updating the JE HA rep group membership to
     * match the currently available RNs in the specified primary zones,
     * setting an electable group size override on each RN to obtain quorum,
     * and converting nodes to primary nodes as needed.  The primary and
     * offline zones must not overlap and must include all primary zones in the
     * current topology.  If there are no existing primary nodes, establishes
     * one secondary as the initial master by resetting the JE replication
     * group, which updates the group membership and allows the other secondary
     * nodes to join.
     *
     * <p>First checks that a quorum of the RNs in the shard in the specified
     * primary zones is available, and that no RNs from the shard, or admins,
     * in the offline zones are available.
     *
     * <p>Then attempts to modify RN parameters and adjust the JE rep group
     * membership.  The command fails if any of those operations fail.
     *
     * @param plan the running plan
     * @param rgId the ID of the shard
     * @param allPrimaryZones the IDs of all zones that will be primary zones
     * @param offlineZones the IDs of offline zones
     * @return whether the operation succeeded
     * @throws IllegalCommandException if any RNs in the shard in offline zones
     * are found to be active, or if the number of active RNs per shard in
     * primary zones is less than the quorum
     * @throws IllegalStateException if the JE HA replication group membership
     * cannot be obtained
     */
    private static boolean repairQuorum(AbstractPlan plan,
                                        RepGroupId rgId,
                                        Set<DatacenterId> allPrimaryZones,
                                        Set<DatacenterId> offlineZones) {

        checkNull("rgId", rgId);

        /* Get information from the admin DB */
        final Admin admin = plan.getAdmin();
        final Topology topo = admin.getCurrentTopology();
        final Map<RepNodeId, RNInfo> rnInfoMap =
            getRNInfo(rgId, topo, admin.getLoginManager(), plan.getLogger());

        final Map<ArbNodeId, ANInfo> anInfoMap =
            getANInfo(rgId, topo, admin.getLoginManager(), plan.getLogger());

        verify(plan, rgId, allPrimaryZones, offlineZones, rnInfoMap,
               topo, admin.getCurrentParameters());

        int existingPrimaries = 0;
        for (final RNInfo rnInfo : rnInfoMap.values()) {
            if (rnInfo.params.getNodeType().isElectable()) {
                existingPrimaries++;
            }
        }

        /*
         * If there are no existing primary nodes, then establish the initial
         * master by using the first node in the set of nodes that should be
         * primary to reset the JE replication group.  This operation resets
         * the group membership, so there is no need to delete members
         * explicitly or set the electable group size override.
         */
        if (existingPrimaries == 0) {
            plan.getLogger().info(
                "Repair shard quorum: no existing primaries");
            boolean nextIsFirst = true;
            for (RNInfo rnInfo : rnInfoMap.values()) {
                final boolean first = nextIsFirst;
                nextIsFirst = false;
                final DatacenterId dcId = topo.getDatacenterId(rnInfo.rn);
                if (!allPrimaryZones.contains(dcId)) {
                    continue;
                }
                if (!repairNodeParams(plan, rgId, rnInfo, 0, first)) {
                    return false;
                }

                /*
                 * Clear the reset rep group flag for the first node, after
                 * getting its updated info
                 */
                if (first) {
                    rnInfo = getRNInfo(
                        rnInfo.rn.getResourceId(), topo,
                        new RegistryUtils(topo, admin.getLoginManager()),
                        plan.getLogger());
                    if ((rnInfo == null) ||
                        !repairNodeParams(plan, rgId, rnInfo, 0, false)) {
                        return false;
                    }
                }
            }
            return true;
        }

        /*
         * Update the electable group size override on existing primary nodes
         * to establish quorum.  Although we could use reset rep group to do
         * this, it is probably safer to do it by modifying the electable group
         * size override, since that allows the nodes to perform an election.
         */
        for (final RNInfo rnInfo : rnInfoMap.values()) {
            if (rnInfo.params.getNodeType().isElectable()) {
                if (!repairNodeParams(plan, rgId, rnInfo, existingPrimaries,
                                      false)) {
                    return false;
                }
            }
        }

        /*
         * Update the JE HA group membership information, if needed, to
         * remove nodes that are not in the requested primary set.
         */
        plan.getLogger().info("Get JE rep group membership");
        final Set<InetSocketAddress> primarySockets = new HashSet<>();
        final StringBuilder helperHosts = new StringBuilder();
        for (final RNInfo rnInfo : rnInfoMap.values()) {
            final String hostPort = rnInfo.params.getJENodeHostPort();
            final DatacenterId dcId = topo.getDatacenterId(rnInfo.rn);
            if (allPrimaryZones.contains(dcId)) {
                primarySockets.add(HostPortPair.getSocket(hostPort));
            }
            if (helperHosts.length() != 0) {
                helperHosts.append(ParameterUtils.HELPER_HOST_SEPARATOR);
            }
            helperHosts.append(hostPort);
        }

        for (final ANInfo anInfo : anInfoMap.values()) {
            final String hostPort = anInfo.params.getJENodeHostPort();
            final DatacenterId dcId =
                topo.getDatacenterId(anInfo.an.getResourceId());
            if (allPrimaryZones.contains(dcId)) {
                primarySockets.add(HostPortPair.getSocket(hostPort));
            }
        }

        final ReplicationGroupAdmin rga =
            admin.getReplicationGroupAdmin(rgId.getGroupName(),
                                           helperHosts.toString());
        final ReplicationGroup rg = Admin.getReplicationGroup(rga);
        Set<ReplicationNode> electableNodes = rg.getElectableNodes();
        for (final ReplicationNode jeRN : electableNodes) {
            if (!primarySockets.contains(jeRN.getSocketAddress())) {
                    deleteMember(plan, rga, rgId, jeRN);
            }
        }

        /* Clear special parameters on the primary nodes */
        for (final RNInfo rnInfo : rnInfoMap.values()) {
            if (rnInfo.params.getNodeType().isElectable()) {
                if (!repairNodeParams(plan, rgId, rnInfo, 0, false)) {
                    return false;
                }
            }
        }

        /*
         * Convert any secondary nodes that should be primary nodes after
         * quorum has been established so that they can join the existing
         * group.
         */
        if (existingPrimaries < rnInfoMap.size()) {
            for (final RNInfo rnInfo : rnInfoMap.values()) {
                if (rnInfo.params.getNodeType().isElectable()) {
                    continue;
                }
                final DatacenterId dcId = topo.getDatacenterId(rnInfo.rn);
                if (!allPrimaryZones.contains(dcId)) {
                    continue;
                }
                if (!repairNodeParams(plan, rgId, rnInfo, 0, false)) {
                    return false;
                }
            }
        }

        return true;
    }

    /** Stores information about an RN. */
    private static class RNInfo {
        final RepNode rn;
        final RepNodeAdminAPI rna;
        final RepNodeParams params;
        RNInfo(RepNode rn, RepNodeAdminAPI rna, LoadParameters params) {
            this.rn = rn;
            this.rna = rna;
            this.params = (params == null) ? null :
                new RepNodeParams(
                    params.getMapByType(ParameterState.REPNODE_TYPE));
        }
    }

    /**
     * Returns information about all active RNs in a shard, or in all shards if
     * rgId is null.
     */
    private static Map<RepNodeId, RNInfo> getRNInfo(RepGroupId rgId,
                                                    Topology topo,
                                                    LoginManager loginManager,
                                                    Logger logger) {
        final RegistryUtils regUtils = new RegistryUtils(topo, loginManager);
        final Map<RepNodeId, RNInfo> rnInfoMap = new HashMap<>();
        final Collection<RepNodeId> repNodeIds = (rgId != null) ?
            topo.getSortedRepNodeIds(rgId) :
            topo.getRepNodeIds();
        for (final RepNodeId rnId : repNodeIds) {
            final RNInfo rnInfo = getRNInfo(rnId, topo, regUtils, logger);
            if (rnInfo != null) {
                rnInfoMap.put(rnId, rnInfo);
            }
        }
        return rnInfoMap;
    }

    /** Return information about a single RN, or null if not available. */
    private static RNInfo getRNInfo(RepNodeId rnId,
                                    Topology topo,
                                    RegistryUtils regUtils,
                                    Logger logger) {
        final RepNode rn = topo.get(rnId);
        try {
            final RepNodeAdminAPI rna = regUtils.getRepNodeAdmin(rnId);
            final LoadParameters params = rna.getParams();
            return new RNInfo(rn, rna, params);
        } catch (RemoteException | NotBoundException re) {
            logger.info("Unable to reach " + rnId + ": " + re);
            return null;
        }
    }

    /** Stores information about an AN. */
    private static class ANInfo {
        final ArbNode an;
        final ArbNodeParams params;
        ANInfo(ArbNode an, LoadParameters params) {
            this.an = an;
            this.params = (params == null) ? null :
                new ArbNodeParams(
                    params.getMapByType(ParameterState.ARBNODE_TYPE));
        }
    }

    /**
     * Returns information about all active ANs in a shard, or in all shards if
     * rgId is null.
     */
    private static Map<ArbNodeId, ANInfo> getANInfo(RepGroupId rgId,
                                                    Topology topo,
                                                    LoginManager loginManager,
                                                    Logger logger) {
        final RegistryUtils regUtils = new RegistryUtils(topo, loginManager);
        final Map<ArbNodeId, ANInfo> anInfoMap = new HashMap<>();
        final Collection<ArbNodeId> arbNodeIds = (rgId != null) ?
            topo.getSortedArbNodeIds(rgId) :
            topo.getArbNodeIds();
        for (final ArbNodeId anId : arbNodeIds) {
            final ANInfo anInfo = getANInfo(anId, topo, regUtils, logger);
            if (anInfo != null) {
                anInfoMap.put(anId, anInfo);
            }
        }
        return anInfoMap;
    }

    /** Return information about a single RN, or null if not available. */
    private static ANInfo getANInfo(ArbNodeId anId,
                                    Topology topo,
                                    RegistryUtils regUtils,
                                    Logger logger) {
        final ArbNode an = topo.get(anId);
        try {
            final ArbNodeAdminAPI ana = regUtils.getArbNodeAdmin(anId);
            final LoadParameters params = ana.getParams();
            return new ANInfo(an, params);
        } catch (RemoteException | NotBoundException re) {
            logger.info("Unable to reach " + anId + ": " + re);
            return null;
        }
    }


    private static boolean deleteMember(AbstractPlan plan,
                                        ReplicationGroupAdmin jeAdmin,
                                        RepGroupId rgId,
                                        ReplicationNode jeRN) {
        final String name = jeRN.getName();
        plan.getLogger().info("Repair shard quorum: delete member: " + name);
        try {
            jeAdmin.deleteMember(name);
            return true;
        } catch (IllegalArgumentException iae) {
            /* Already a secondary, ignore */
            return true;
        } catch (UnknownMasterException ume) {
            logError(plan, rgId, "the master was not found");
            return false;
        } catch (MemberActiveException mae) {
            /* This is unlikely since the node was offline */
            logError(plan, rgId, jeRN + " is active");
            return false;
        } catch (MemberNotFoundException mnfe) {
            logError(plan, rgId, jeRN + " was not found");
            return false;
        } catch (MasterStateException mse) {
            logError(plan, rgId, jeRN + " is currently the master");
            return false;
        } catch (OperationFailureException ofe) {
            logError(plan, rgId, "unexpected exception: " + ofe);
            return false;
        }
    }

    private static void logError(AbstractPlan plan,
                                 RepGroupId rgId,
                                 String cause) {
        plan.getLogger().log(
            Level.INFO, "Couldn''t repair quorum for {0} because {1}",
            new Object[] { rgId, cause });
    }

    @Override
    public boolean continuePastError() {
        return false;
    }

    /**
     * Update the node parameters as needed to make it a primary node, and have
     * the requested electable group size override and reset rep group
     * settings.  Returns whether the update was successful.
     */
    private static boolean repairNodeParams(AbstractPlan plan,
                                            RepGroupId rgId,
                                            RNInfo rnInfo,
                                            int groupSizeOverride,
                                            boolean resetRepGroup) {
        final RepNodeId rnId = rnInfo.rn.getResourceId();
        plan.getLogger().info("Repair node params: " + rnId +
                              ", groupSizeOverride: " + groupSizeOverride +
                              ", resetRepGroup: " + resetRepGroup);
        if (rnInfo.rna == null) {
            logError(plan, rgId, rnId + " is not running");
            return false;
        }

        final StorageNodeId snId = rnInfo.rn.getStorageNodeId();
        final RepNodeParams rnParams = rnInfo.params;
        final boolean currentIsPrimary = rnParams.getNodeType().isElectable();
        final int currentGroupSizeOverride =
            rnParams.getElectableGroupSizeOverride();
        final boolean currentResetRepGroup = rnParams.getResetRepGroup();

        assert !resetRepGroup || !currentIsPrimary
            : "Only reset replication group for secondary node";

        /* Check if node is OK as is */
        if (currentIsPrimary &&
            (currentGroupSizeOverride == groupSizeOverride) &&
            (currentResetRepGroup == resetRepGroup)) {
            plan.getLogger().info("Repair node params: OK: " + rnId);
            return true;
        }

        rnParams.setNodeType(NodeType.ELECTABLE);
        rnParams.setElectableGroupSizeOverride(groupSizeOverride);
        rnParams.setResetRepGroup(resetRepGroup);

        try {
            WriteNewParams.writeNewParams(plan, rnParams.getMap(), rnId, snId);
            if (currentIsPrimary) {
                plan.getLogger().info("Repair node params: no restart: " +
                                      rnId);
                rnInfo.rna.newParameters();
                return true;
            }
            plan.getLogger().info("Repair node params: restart: " + rnId);

            /*
             * We do not wait for this or other nodes to be consistent,
             * stopRN(..., false), because this method is called when shard
             * quorum has been lost, so there is reason to think that there may
             * not be a master yet.  In particular, an election probably won't
             * work until we have set the electable group size override on
             * enough nodes that one of them ends up becoming the master.
             */
            Utils.stopRN(plan, snId, rnId, false);
            Utils.startRN(plan, snId, rnId);
            Utils.waitForNodeState(plan, rnId, ServiceStatus.RUNNING);
            return true;
        } catch (Exception e) {
            plan.getLogger().log(
                Level.INFO,
                "Problem attempting to update the quorum for RN: " + rnId +
                ", SN ID: " + snId,
                e);
            return false;
        }
    }

    @Override
    public String getName() {
        return super.getName() + ' ' + rgId;
    }

    @Override
    public String toString() {
        return getName();
    }
}
