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

import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.param.ArbNodeParams;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.param.ParameterMap;
import oracle.kv.impl.security.login.LoginManager;
import oracle.kv.impl.sna.StorageNodeAgentAPI;
import oracle.kv.impl.topo.ArbNodeId;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.topo.Topology;
import oracle.kv.impl.util.registry.RegistryUtils;

/**
 * A task for asking a storage node to write a new configuration file.
 */
public class WriteNewANParams extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private final AbstractPlan plan;
    private final ParameterMap newParams;
    private final StorageNodeId targetSNId;
    private final ArbNodeId anid;
    private final boolean continuePastError;

    public WriteNewANParams(AbstractPlan plan,
                            ParameterMap newParams,
                            ArbNodeId anid,
                            StorageNodeId targetSNId,
                            boolean continuePastError) {
        super();
        this.plan = plan;
        this.newParams = newParams;
        this.anid = anid;
        this.targetSNId = targetSNId;
        this.continuePastError = continuePastError;
    }

    /**
     */
    @Override
    public State doWork()
        throws Exception {

        if (!writeNewANParams(plan, newParams, anid, targetSNId)) {
            return State.ERROR;
        }
        return State.SUCCEEDED;
    }

    public static boolean writeNewANParams(AbstractPlan plan,
                                         ParameterMap newParams,
                                         ArbNodeId anid,
                                         StorageNodeId targetSNId)
        throws Exception {

        Admin admin = plan.getAdmin();
        ArbNodeParams anp = admin.getArbNodeParams(anid);
        ParameterMap anMap = anp.getMap();
        ArbNodeParams newAnp = new ArbNodeParams(newParams);
        newAnp.setArbNodeId(anid);
        ParameterMap diff = anMap.diff(newParams, true);
        plan.getLogger().info("Changing params for " + anid + ": " + diff);

        /*
         * Merge and store the changed rep node params in the admin db before
         * sending them to the SNA.
         */
        anMap.merge(newParams, true);
        admin.updateParams(anp);

        /* Ask the SNA to write a new configuration file. */
        Topology topology = admin.getCurrentTopology();
        LoginManager loginMgr = admin.getLoginManager();
        RegistryUtils registryUtils = new RegistryUtils(topology, loginMgr);

        StorageNodeAgentAPI sna =
            registryUtils.getStorageNodeAgent(targetSNId);
        sna.newArbNodeParameters(anMap);

        return true;
    }

    @Override
    public boolean continuePastError() {
        return continuePastError;
    }

    @Override
    public String toString() {
       return super.toString() +
           " write new " + anid + " parameters into the Admin database: " +
           newParams.showContents();
    }
}
