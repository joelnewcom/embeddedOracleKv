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

import java.io.Serializable;
import java.util.Map;

import oracle.kv.impl.admin.CommandResult;
import oracle.kv.impl.admin.NonfatalAssertionException;
import oracle.kv.impl.admin.PlanLocksHeldException;
import oracle.kv.impl.admin.plan.Planner;

import com.sleepycat.persist.model.Persistent;

/**
 * A common base class for implementations of the {@link Task} interface.
 */
@Persistent
public abstract class AbstractTask implements Task, Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * If a task cleanup job fails, keep retrying periodically, until it either
     * succeeds or the user interrupts the plan again.
     */
    static final int CLEANUP_RETRY_MILLIS = 120000;

    private transient CommandResult taskResult;

    public AbstractTask() {
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public TaskList getNestedTasks() {
        /* A task list is only returned for parallel tasks. */
        return null;
    }

    /**
     * AbstractTasks are assumed to not have any nested tasks.
     */
    @Override
    public int getTotalTaskCount() {
        return 1;
    }

    /**
     * Most tasks have no cleanup to do. Whatever changes they have executed
     * can be left untouched.
     */
    @Override
    public Runnable getCleanupJob() {
        return null;
    }

    /**
     * Obtain any required topo locks before plan execution, to avoid conflicts
     * in concurrent plans.
     * @throws PlanLocksHeldException
     */
    @Override
    public void lockTopoComponents(Planner planner)
        throws PlanLocksHeldException {
        /* default: no locks needed */
    }

    /*
     * Format any detailed information collected about the task in a way
     * that's usable for plan reporting. Should be overridden by tasks to
     * provide customized status reports.
     *
     * @return null if there are no details.
     */
    @Override
    public String displayExecutionDetails(Map<String, String> details,
                                          String displayPrefix) {
        if (details.isEmpty()) {
            return null;
        }

        return details.toString();
    }

    @Override
    public boolean logicalCompare(Task otherTask) {
        throw new NonfatalAssertionException(getName() +
            ": logical comparison is only supported for " +
            "table and security related tasks");
    }

    @Override
    public boolean restartOnInterrupted() {
        return false;
    }

    @Override
    public CommandResult getTaskResult() {
        return taskResult;
    }

    @Override
    public void setTaskResult(CommandResult taskResult) {
        this.taskResult = taskResult;
    }
}
