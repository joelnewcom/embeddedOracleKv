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

import static oracle.kv.impl.param.ParameterState.BOOTSTRAP_MOUNT_POINTS;
import static oracle.kv.impl.param.ParameterState.COMMON_SN_ID;

import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.param.StorageNodeParams;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.param.ParameterMap;
import oracle.kv.impl.security.login.LoginManager;
import oracle.kv.impl.sna.StorageNodeAgentAPI;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.topo.Topology;
import oracle.kv.impl.util.registry.RegistryUtils;

import com.sleepycat.persist.model.Persistent;
import java.util.logging.Level;

/**
 * A task for asking a storage node to write a new configuration file.
 *
 * version 0: original.
 * version 1: Changed inheritance chain.
 */
@Persistent(version=1)
public class WriteNewSNParams extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private AbstractPlan plan;
    private ParameterMap newParams;
    private StorageNodeId targetSNId;

    public WriteNewSNParams(AbstractPlan plan,
                            StorageNodeId targetSNId,
                            ParameterMap newParams) {
        super();
        this.plan = plan;
        this.newParams = newParams;
        this.targetSNId = targetSNId;
    }

    /*
     * No-arg ctor for use by DPL.
     */
    @SuppressWarnings("unused")
    private WriteNewSNParams() {
    }

    /**
     */
    @Override
    public State doWork()
        throws Exception {

        /*
         * Store the changed params in the admin db before sending them to the
         * SNA.  Merge rather than replace in this path.  If nothing is changed
         * return.
         */
        final Admin admin = plan.getAdmin();
        final StorageNodeParams snp = admin.getStorageNodeParams(targetSNId);
        ParameterMap snMap = snp.getMap();

        boolean updateAdminDB = true;
        final ParameterMap storageDirMap =
            newParams.getName().equals(BOOTSTRAP_MOUNT_POINTS) ? newParams :
            null;
        if (storageDirMap != null) {
            plan.getLogger().log(Level.INFO,
                                 "Changing storage directories for {0}: {1}",
                                 new Object[]{targetSNId, storageDirMap});
            /*
             * Snid may have been stored, remove it
             */
            storageDirMap.remove(COMMON_SN_ID);
            snp.setStorageDirMap(storageDirMap);
            snMap = null;
        } else {
            final ParameterMap diff = snMap.diff(newParams, true);

            updateAdminDB = snMap.merge(newParams, true) > 0;
            plan.getLogger().log(Level.INFO,
                                 "Changing these params for {0}: {1}",
                                 new Object[]{targetSNId, diff});
        }

        final Topology topology = admin.getCurrentTopology();
        final LoginManager loginMgr = admin.getLoginManager();
        final RegistryUtils registryUtils =
                new RegistryUtils(topology, loginMgr);
        final StorageNodeAgentAPI sna =
                registryUtils.getStorageNodeAgent(targetSNId);

        /* Only one or the other map will be non-null */
        final ParameterMap newMap = snMap != null ? snMap : storageDirMap;

        if (updateAdminDB) {
            try {
                /* Check the parameters prior to writing them to the DB. */
                sna.checkParameters(newMap, targetSNId);
            } catch (UnsupportedOperationException ignore) {
                /*
                 * If UOE, the SN is not yet upgraded to a version that
                 * supports this check, so just ignore
                 */
            }

            /* Update the admin DB */
            admin.updateParams(snp, null);
        }

        sna.newStorageNodeParameters(newMap);

        return State.SUCCEEDED;
    }

    @Override
    public boolean continuePastError() {
        return false;
    }

    @Override
    public String toString() {
       return super.toString() +
           " write new " + targetSNId +
           " parameters into the Admin database: " + newParams.showContents();
    }
}
