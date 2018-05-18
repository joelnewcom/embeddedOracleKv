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
import oracle.kv.impl.admin.param.RepNodeParams;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.param.ParameterMap;
import oracle.kv.impl.security.login.LoginManager;
import oracle.kv.impl.sna.StorageNodeAgentAPI;
import oracle.kv.impl.topo.RepNodeId;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.topo.Topology;
import oracle.kv.impl.util.registry.RegistryUtils;

import com.sleepycat.persist.model.Persistent;

/**
 * A task for asking a storage node to write a new configuration file.
 *
 * version 0: original.
 * version 1: Changed inheritance chain.
 */
@Persistent(version=1)
public class WriteNewParams extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private AbstractPlan plan;
    private ParameterMap newParams;
    private StorageNodeId targetSNId;
    private RepNodeId rnid;
    private boolean continuePastError;

    public WriteNewParams(AbstractPlan plan,
                          ParameterMap newParams,
                          RepNodeId rnid,
                          StorageNodeId targetSNId,
                          boolean continuePastError) {
        super();
        this.plan = plan;
        this.newParams = newParams;
        this.rnid = rnid;
        this.targetSNId = targetSNId;
        this.continuePastError = continuePastError;
    }

    /*
     * No-arg ctor for use by DPL.
     */
    @SuppressWarnings("unused")
    private WriteNewParams() {
    }

    /**
     */
    @Override
    public State doWork()
        throws Exception {

        if (!writeNewParams(plan, newParams, rnid, targetSNId)) {
            return State.ERROR;
        }
        return State.SUCCEEDED;
    }

    public static boolean writeNewParams(AbstractPlan plan,
                                         ParameterMap newParams,
                                         RepNodeId rnid,
                                         StorageNodeId targetSNId)
        throws Exception {

        final Admin admin = plan.getAdmin();
        final RepNodeParams rnp = admin.getRepNodeParams(rnid);
        final ParameterMap rnMap = rnp.getMap();
        final RepNodeParams newRnp = new RepNodeParams(newParams);
        newRnp.setRepNodeId(rnid);
        final ParameterMap diff = rnMap.diff(newParams, true);
        plan.getLogger().info("Changing params for " + rnid + ": " + diff);

        final Topology topology = admin.getCurrentTopology();
        final LoginManager loginMgr = admin.getLoginManager();
        final RegistryUtils registryUtils =
                new RegistryUtils(topology, loginMgr);
        final StorageNodeAgentAPI sna =
                registryUtils.getStorageNodeAgent(targetSNId);

        /*
         * Merge and store the changed rep node params in the admin db before
         * sending them to the SNA.
         */
        if (rnMap.merge(newParams, true) > 0) {
            try {
                /* Check the parameters prior to writing them to the DB. */
                sna.checkParameters(rnMap, rnid);
            } catch (UnsupportedOperationException ignore) {
                /*
                 * If UOE, the SN is not yet upgraded to a version that
                 * supports this check, so just ignore
                 */
            }
            admin.updateParams(rnp);
        }

        /* Ask the SNA to write a new configuration file. */
        sna.newRepNodeParameters(rnMap);

        return true;
    }

    @Override
    public boolean continuePastError() {
        return continuePastError;
    }

    @Override
    public String toString() {
       return super.toString() +
           " write new " + rnid + " parameters into the Admin database: " +
           newParams.showContents();
    }
}
