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
import oracle.kv.impl.admin.param.GlobalParams;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.param.ParameterMap;
import oracle.kv.impl.security.login.LoginManager;
import oracle.kv.impl.sna.StorageNodeAgentAPI;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.topo.Topology;
import oracle.kv.impl.util.registry.RegistryUtils;

import com.sleepycat.persist.model.Persistent;

/**
 * A task for asking a storage node to write the new global parameters to
 * configuration file.
 */
@Persistent
public class WriteNewGlobalParams extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private boolean continuePastError;
    private AbstractPlan plan;
    private ParameterMap newParamMap;
    private StorageNodeId targetSNId;

    public WriteNewGlobalParams(AbstractPlan plan,
                                ParameterMap newParams,
                                StorageNodeId targetSNId,
                                boolean continuePastError) {
        this.plan = plan;
        this.newParamMap = newParams;
        this.continuePastError = continuePastError;
        this.targetSNId = targetSNId;
    }

    /* No-arg ctor for DPL */
    @SuppressWarnings("unused")
    private WriteNewGlobalParams() {
    }

    @Override
    public boolean continuePastError() {
        return continuePastError;
    }

    @Override
    public State doWork() throws Exception {
        final Admin admin = plan.getAdmin();
        final GlobalParams gp = admin.getCurrentParameters().getGlobalParams();
        final ParameterMap gpMap = gp.getMap();

        final ParameterMap diff =
            gpMap.diff(newParamMap, true /* notReadOnly */);
        plan.getLogger().info("Changing Global params for " + targetSNId +
                              ": " + diff);

        final Topology topology = admin.getCurrentTopology();
        final LoginManager loginMgr = admin.getLoginManager();
        final RegistryUtils registryUtils =
                new RegistryUtils(topology, loginMgr);
        final StorageNodeAgentAPI sna =
                registryUtils.getStorageNodeAgent(targetSNId);

        /*
         * Merge and store the changed global params in the admin db, and then
         * send them to the SNA.
         * Note that mergeCount is only an indicator of whether the adminDB
         * contains these parameter values. MergeCount doesn't tell us whether
         * the target SN already has these parameter settings. For example, the
         * adminDB may be updated from an earlier call to write new global
         * params for a different SN, and mergeCount would be 0, but this
         * target SN might still need an update.
         */
        int mergedCount = gpMap.merge(newParamMap, true /* notReadOnly */);
        try {
            /* Check the parameters prior to writing them to the DB. */
            sna.checkParameters(gpMap, null);
        } catch (UnsupportedOperationException ignore) {
            /*
             * If UOE, the SN is not yet upgraded to a version that
             * supports this check, so just ignore
             */
        }

        /* The AdminDB needs updating */
        if (mergedCount > 0) {
            admin.updateParams(gp);
        }

        /*
         * Send new parameters to the SNA, ask it to write a new configuration
         * file. 
         */
        sna.newGlobalParameters(gpMap);

        return State.SUCCEEDED;
    }

    @Override
    public String toString() {
       return super.toString() +
           " write new global parameters into the Admin database and SN " +
           targetSNId + "'s configuration file : " +
           newParamMap.showContents();
    }
}
