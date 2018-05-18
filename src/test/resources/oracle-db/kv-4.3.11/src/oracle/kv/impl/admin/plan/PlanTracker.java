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

package oracle.kv.impl.admin.plan;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.kv.impl.admin.AdminServiceParams;
import oracle.kv.impl.admin.plan.task.Task;
import oracle.kv.impl.security.ExecutionContext;
import oracle.kv.impl.security.KVStorePrivilege;
import oracle.kv.impl.security.KVStoreUserPrincipal;
import oracle.kv.impl.security.SystemPrivilege;
import oracle.kv.impl.util.server.LoggerUtils;
import oracle.kv.impl.util.server.LoggerUtils.SecurityLevel;

/**
 * Logs information about plan execution.
 */
public class PlanTracker implements ExecutionListener {

    /*
     * Plans requiring any of privileges in this set need to be recored in
     * audit logging.
     */
    private static final Set<KVStorePrivilege> privsRequireAudit =
        new HashSet<>();

    static {
        privsRequireAudit.add(SystemPrivilege.SYSOPER);
        privsRequireAudit.add(SystemPrivilege.SYSDBA);
    }
    private final Logger logger;

    public PlanTracker(AdminServiceParams adminParams) {
        logger = LoggerUtils.getLogger(this.getClass(), adminParams);
    }

    private String header(Plan plan) {
        return plan + ":";
    }

    /**
     * Only log execution state of plans that require SYSOPER and SYSDBA
     * privileges as well as all table DDL-related plans.
     */
    private boolean needAuditLogging(Plan plan) {
        if (plan instanceof DeployTableMetadataPlan) {
            return true;
        }
        final List<? extends KVStorePrivilege> requiredPrivs =
            plan.getRequiredPrivileges();

        for (KVStorePrivilege priv : requiredPrivs) {
            if (privsRequireAudit.contains(priv)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void planStart(Plan plan) {
        int numTasks = plan.getTotalTaskCount();
        logger.log(Level.INFO, "{0} started, {1} tasks",
                   new Object[]{header(plan), numTasks});
    }

    @Override
    public void planEnd(Plan plan) {
        final ExecutionContext exeCtx = ExecutionContext.getCurrent();
        if (exeCtx == null || !needAuditLogging(plan)) {
            logger.log(Level.INFO, "{0} ended, state={1}",
                       new Object[]{header(plan), plan.getState()});
        } else {
            final KVStoreUserPrincipal user =
                ExecutionContext.getSubjectUserPrincipal(
                    exeCtx.requestorSubject());
            logger.log(SecurityLevel.SEC_INFO, "KVAuditInfo [{0}," +
                " owned by {1}, executed by {2} from {3} ended, state={4}]",
                new Object[]{plan,
                (plan.getOwner() == null) ? "" : plan.getOwner(),
                (user == null) ? "" : user.getName(),
                exeCtx.requestorHost(), plan.getState()});
        }
    }

    @Override
    public void taskStart(Plan plan,
                          Task task,
                          int taskNum,
                          int totalTasks) {
        logger.log(Level.INFO, "{0} task {1} [{2}] started",
                   new Object[]{header(plan), taskNum, task});
    }

    @Override
    public void taskEnd(Plan plan,
                        Task task,
                        TaskRun taskRun,
                        int taskNum,
                        int totalTasks) {
        logger.log(Level.INFO, "{0} task {1} [{2}] ended, state={3}",
                   new Object[]{header(plan), taskNum, task,
                                taskRun.getState()});
    }
}
