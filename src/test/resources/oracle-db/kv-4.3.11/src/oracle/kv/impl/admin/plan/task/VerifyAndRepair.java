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

import com.sleepycat.persist.model.Persistent;

import java.util.List;

import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.TopologyCheck;
import oracle.kv.impl.admin.TopologyCheck.Remedy;
import oracle.kv.impl.admin.VerifyConfiguration;
import oracle.kv.impl.admin.VerifyResults;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.topo.AdminId;

@Persistent
public class VerifyAndRepair extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private AbstractPlan plan;
    private boolean shouldContinuePastError;

    /* For DPL */
    VerifyAndRepair() {
    }

    public VerifyAndRepair(AbstractPlan plan, boolean continuePastError) {
        this.plan = plan;
        this.shouldContinuePastError = continuePastError;
    }

    @Override
    public String getName() {
        return "VerifyAndRepair";
    }

    @Override
    public boolean continuePastError() {
        return shouldContinuePastError;
    }

    /**
     * Run a verify on the current configuration and then attempt to repair
     * any problems that are found.
     */
    @Override
    public State doWork() throws Exception {

        Admin admin = plan.getAdmin();
        final VerifyConfiguration checker =
            new VerifyConfiguration(admin,
                                    false, /* showProgress */
                                    true, /* listAll */
                                    false, /* json */
                                    plan.getLogger());
        checker.verifyTopology();
        VerifyResults results = checker.getResults();

        TopologyCheck topoCheck = checker.getTopoChecker();
        AdminId masterAdminId =
            admin.getParams().getAdminParams().getAdminId();
        List<Remedy> remedies = checker.getRemedies(masterAdminId);
        plan.getLogger().info("Found repairs: " + remedies);
        topoCheck.applyRemedies(remedies, plan);
        topoCheck.repairInitialEmptyShards(results, plan);

        return Task.State.SUCCEEDED;
    }
}
