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
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.param.AdminParams;
import oracle.kv.impl.admin.plan.PlanExecutor.ParallelTaskRunner;
import oracle.kv.impl.admin.plan.MetadataPlan;
import oracle.kv.impl.admin.plan.TablePlanGenerator;
import oracle.kv.impl.api.table.TableMetadata;
import oracle.kv.impl.param.DurationParameter;
import oracle.kv.impl.rep.admin.RepNodeAdminAPI;
import oracle.kv.impl.topo.RepGroupId;
import oracle.kv.impl.topo.RepNode;
import oracle.kv.impl.topo.Topology;
import oracle.kv.impl.util.registry.RegistryUtils;
import oracle.kv.util.PingCollector;

import com.sleepycat.persist.model.Persistent;

/**
 * Wait for an add a new index on a RepNode. Each node will populate the
 * new index for its shard.
 */
@Persistent(version=1)
public class WaitForAddIndex extends AbstractTask {

    private static final long serialVersionUID = 1L;

    private /*final*/ MetadataPlan<TableMetadata> plan;
    private /*final*/ RepGroupId groupId;

    private /*final*/ String indexName;
    private /*final*/ String tableName;

    private static final long START_WAIT_TIME_MS = 500;

    /*
     * Wait time between check calls. Start at 500ms increasing
     * to getCheckAddIndexPeriod()
     */
    private long waitTimeMS = START_WAIT_TIME_MS;

    /**
     *
     * @param plan
     * @param groupId ID of the current rep group of the partition
     * will stop.
     */
    public WaitForAddIndex(MetadataPlan<TableMetadata> plan,
                           RepGroupId groupId,
                           String indexName,
                           String tableName) {
        this.plan = plan;
        this.groupId = groupId;
        this.indexName = indexName;
        this.tableName = tableName;
    }

    /* DPL */
    protected WaitForAddIndex() {
    }

    @Override
    public boolean continuePastError() {
        return true;
    }

    @Override
    public Callable<State> getFirstJob(int taskId, ParallelTaskRunner runner) {
        return makeWaitForAddIndexJob(taskId, runner);
    }

    /**
     * @return a wrapper that will invoke a add index job.
     */
    private JobWrapper makeWaitForAddIndexJob(final int taskId,
                                              final ParallelTaskRunner runner) {

        return new JobWrapper(taskId, runner, "add index") {

            @Override
            public NextJob doJob() {
                return waitForAddIndex(taskId, runner);
            }
        };
    }

    /**
     * Query for add index to complete
     */
    private NextJob waitForAddIndex(int taskId, ParallelTaskRunner runner) {
        final AdminParams ap = plan.getAdmin().getParams().getAdminParams();
        RepNodeAdminAPI masterRN = null;
        try {
            masterRN = getMaster();
            if (masterRN == null) {
                /* No master available, try again later. */
                return new NextJob(Task.State.RUNNING,
                                   makeWaitForAddIndexJob(taskId, runner),
                                   ap.getRNFailoverPeriod());
            }

            plan.getLogger().log(Level.INFO,
                                 "Wait for add index {0} for {1}",
                                 new Object[] {indexName, groupId});

            return queryForDone(taskId, runner, ap);

        } catch (RemoteException | NotBoundException e) {
            /* RMI problem, try step 1 again later. */
            return new NextJob(Task.State.RUNNING,
                               makeWaitForAddIndexJob(taskId, runner),
                               ap.getServiceUnreachablePeriod());

        }
    }

    /**
     * Find the master of the RepGroup.
     * @return null if no master can be found, otherwise the admin interface
     * for the master RN.
     *
     * @throws NotBoundException
     * @throws RemoteException
     */
    private RepNodeAdminAPI getMaster()
        throws RemoteException, NotBoundException {

        final Admin admin = plan.getAdmin();
        Topology topology = admin.getCurrentTopology();
        PingCollector collector = new PingCollector(topology);
        RepNode masterRN = collector.getMaster(groupId);
        if (masterRN == null) {
            return null;
        }

        RegistryUtils registryUtils =
                new RegistryUtils(topology, admin.getLoginManager());
        return registryUtils.getRepNodeAdmin(masterRN.getResourceId());
    }

    private NextJob queryForDone(int taskId,
                                 ParallelTaskRunner runner,
                                 AdminParams ap) {

        try {
            RepNodeAdminAPI masterRN = getMaster();
            if (masterRN == null) {
                /* No master to talk to, repeat step2 later. */
                return new NextJob(Task.State.RUNNING,
                                   makeDoneQueryJob(taskId, runner, ap),
                                   ap.getRNFailoverPeriod());
            }

            final boolean done = masterRN.addIndexComplete(indexName,
                                                           tableName);

            plan.getLogger().log(Level.INFO,
                                 "Add index {0} on {1} done={2}",
                                 new Object[] {indexName,
                                               groupId, done});

            return done ? NextJob.END_WITH_SUCCESS :
                          new NextJob(Task.State.RUNNING,
                                      makeDoneQueryJob(taskId, runner, ap),
                                      getCheckIndexTime(ap));
        } catch (RemoteException | NotBoundException e) {
            /* RMI problem, try again later. */
            return new NextJob(Task.State.RUNNING,
                               makeDoneQueryJob(taskId, runner, ap),
                               ap.getServiceUnreachablePeriod());
        }
    }

    /**
     * @return a wrapper that will invoke a done query.
     */
    private JobWrapper makeDoneQueryJob(final int taskId,
                                        final ParallelTaskRunner runner,
                                        final AdminParams ap) {
        return new JobWrapper(taskId, runner, "query add index done") {
            @Override
            public NextJob doJob() {
                return queryForDone(taskId, runner, ap);
            }
        };
    }

    private DurationParameter getCheckIndexTime(AdminParams ap) {
        DurationParameter dp = ap.getCheckAddIndexPeriod();

        /* May be zero due to upgrade */
        if (waitTimeMS == 0) {
            waitTimeMS = START_WAIT_TIME_MS;
        }
        if ((dp != null) && (waitTimeMS < dp.toMillis())) {
            dp = new DurationParameter("", TimeUnit.MILLISECONDS, waitTimeMS);
            waitTimeMS += waitTimeMS;
        }
        return dp;
    }

    @Override
    public String toString() {
        return TablePlanGenerator.makeName("WaitForAddIndex", tableName,
                                           indexName, groupId.getGroupName());
    }

    /**
     * No true impact on table or index creation, no need to compare.
     */
    @Override
    public boolean logicalCompare(Task t) {
        return true;
    }
}
