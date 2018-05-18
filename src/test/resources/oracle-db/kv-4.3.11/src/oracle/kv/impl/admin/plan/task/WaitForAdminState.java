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

import oracle.kv.impl.admin.CommandResult;
import oracle.kv.impl.admin.param.AdminParams;
import oracle.kv.impl.admin.param.Parameters;
import oracle.kv.impl.admin.param.StorageNodeParams;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.fault.CommandFaultException;
import oracle.kv.impl.topo.AdminId;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.util.ConfigurableService.ServiceStatus;
import oracle.kv.impl.util.ServiceUtils;
import oracle.kv.util.ErrorMessage;

import com.sleepycat.persist.model.Persistent;

/**
 * Monitors the state of an Admin, blocking until a certain state has been
 * reached.
 *
 * version 0: original.
 * version 1: Changed inheritance chain.
 */
@Persistent(version=1)
public class WaitForAdminState extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    /**
     * The node that is to be monitored
     */
    private AdminId targetAdminId;

    /**
     * The state the node must be in before finishing this task
     */
    private ServiceStatus targetState;
    private AbstractPlan plan;
    private StorageNodeId snId;

    public WaitForAdminState() {
    }

    /**
     * Creates a task that will block until a given Admin has reached
     * a given state.
     *
     * @param desiredState the state to wait for
     */
    public WaitForAdminState(AbstractPlan plan,
                             StorageNodeId snId,
                             AdminId targetAdminId,
                             ServiceStatus desiredState) {
        this.plan = plan;
        this.targetAdminId = targetAdminId;
        this.snId = snId;
        this.targetState = desiredState;
    }

    @Override
    public State doWork()
        throws Exception {

        Parameters parameters = plan.getAdmin().getCurrentParameters();
        StorageNodeParams snp = parameters.get(snId);

        /* Get the timeout from the currently running Admin's myParams. */
        AdminParams ap = plan.getAdmin().getParams().getAdminParams();
        long waitSeconds =
            ap.getWaitTimeoutUnit().toSeconds(ap.getWaitTimeout());

        String msg =
            "Waiting " + waitSeconds + " seconds for Admin" + targetAdminId +
            " to reach " + targetState;

        plan.getLogger().fine(msg);

        try {
            ServiceUtils.waitForAdmin(snp.getHostname(), snp.getRegistryPort(),
                                      plan.getLoginManager(),
                                      waitSeconds, targetState);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                throw new CommandFaultException(e.getMessage(), e,
                                                ErrorMessage.NOSQL_5400,
                                                CommandResult.PLAN_CANCEL);
            }

            plan.getLogger().info("Timed out while " + msg);
            throw new CommandFaultException(e.getMessage(),
                                            ErrorMessage.NOSQL_5400,
                                            CommandResult.PLAN_CANCEL);
        }

        return State.SUCCEEDED;
    }

    @Override
    public String toString() {
       return super.toString() + " waits for Admin " + targetAdminId +
           " to reach " + targetState + " state";
    }

    @Override
    public boolean continuePastError() {
        return true;
    }
}
