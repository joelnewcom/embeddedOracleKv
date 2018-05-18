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

package oracle.kv.impl.admin;

import java.io.PrintStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import oracle.kv.impl.admin.param.AdminParams;
import oracle.kv.impl.admin.param.Parameters;
import oracle.kv.impl.topo.AdminId;
import oracle.kv.impl.topo.DatacenterId;
import oracle.kv.impl.topo.RepGroup;
import oracle.kv.impl.topo.RepGroupId;
import oracle.kv.impl.topo.RepGroupMap;
import oracle.kv.impl.topo.RepNode;
import oracle.kv.impl.topo.RepNodeId;
import oracle.kv.impl.topo.ResourceId;
import oracle.kv.impl.topo.StorageNode;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.topo.Topology;
import oracle.kv.impl.util.KVThreadFactory;
import oracle.kv.impl.util.server.LoggerUtils;

/**
 * A class to encapsulate store-wide snapshot creation and management.
 * Model for snapshots:
 * -- Snapshots are run for all nodes, masters and replicas
 * -- Snapshots will fail for disabled or not-running services
 * -- Snapshots are done in parallel, each in its own thread
 *
 * Off-node backups of snapshots is done via direct administrative copies of
 * files associated with a particular snapshot.  In general the snapshot from
 * the nodes acting as master at the time of the snapshot should be used.
 *
 * Operations supported include:
 * createSnapshot(name)
 * listSnapshots()
 * removeSnapshot(name)
 * removeAllSnapshots()
 *
 * The methods, with the exception of createSnapshot() are void.  Results are
 * determined by calling methods after the operation returns.  The general
 * pattern is:
 *   Snapshot snapshot = new Snapshot(...);
 *   snapshot.operation();
 *   if (!snapshot.succeeded()) {
 *     ...problems...
 *     List<SnapResult> failures = snapshot.getFailures();
 *     ... process failures ...
 *   }
 *
 * The SnapResult object contains the ResourceId of the failed node along with
 * any exception or additional information that may be useful.  It is also
 * possible to get a list of successes.
 *
 * The name passed into createSnapshot() will be appended to the current date
 * and time to make the actual snapshot name used, which is the one returned
 * by listSnapshots() and used for removeSnapshot().
 *
 */

public class Snapshot {

    Topology topo;
    CommandServiceAPI cs;
    ExecutorService threadPool;
    boolean verboseOutput;
    PrintStream output;
    SnapshotOperation op;

    /**
     * Results handling
     */
    private List<SnapResult> success;
    private List<SnapResult> failure;
    private boolean allSucceeded;
    private boolean quorumSucceeded;

    public enum SnapshotOperation {
        CREATE, REMOVE, REMOVEALL
    }

    public Snapshot(CommandServiceAPI cs,
                    boolean verboseOutput,
                    PrintStream output)
        throws RemoteException {
        this.cs = cs;
        this.verboseOutput = verboseOutput;
        this.output = output;
        allSucceeded = true;
        quorumSucceeded = true;
        topo = cs.getTopology();
    }

    /**
     * Results handling.
     */

    /**
     * Indicates whether the operation succeeded on all nodes in the Topology.
     */
    public boolean succeeded() {
        return allSucceeded;
    }

    /**
     * Return true if at least one node in each replication group (including
     * the admin) returned success.  If there was a failure the caller must
     * use other methods to report the problems.
     */
    public boolean getQuorumSucceeded() {
        return quorumSucceeded;
    }

    /**
     * Return the list of SnapResult objects for which the operation succeeded
     */
    public List<SnapResult> getSuccesses() {
        return success;
    }

    /**
     * Return the list of SnapResult objects for which the operation failed
     */
    public List<SnapResult> getFailures() {
        return failure;
    }

    public SnapshotOperation getOperation() {
        return op;
    }

    /**
     * Create a snapshot on all Storage Nodes in the topology.  The actual
     * snapshot name is the current time/date concatenated with the name
     * parameter.
     *
     * @param name the suffix to use for the snapshot name
     *
     * @return the generated snapshot name
     */
    public String createSnapshot(String name)
        throws Exception {
        return createSnapshot(name, null);
    }

    /**
     * Create a snapshot on all Storage Nodes in the specified zone.
     * The actual snapshot name is the current time/date concatenated with the
     * name parameter.
     * Operation applies to all zones if dcId is null.
     *
     * @param name the suffix to use for the snapshot name
     * @param dcId the datacenterId
     *
     * @return the generated snapshot name
     */
    public String createSnapshot(String name, DatacenterId dcId)
        throws Exception {

        String snapName = makeSnapshotName(name);

        verbose("Start create snapshot " + snapName);
        resetOperation(SnapshotOperation.CREATE);
        makeSnapshotTasks(snapName, dcId);
        verbose("Complete create snapshot " + snapName);
        return snapName;
    }

    /**
     * Remove a snapshot from all Storage Nodes.  Each Storage Node will remove
     * it from any managed services.
     *
     * @param name the full name of the snapshot, including date time that was
     * generated by createSnapshot
     *
     */
    public void removeSnapshot(String name)
        throws Exception {
        removeSnapshot(name, null);
    }

    /**
     * Remove a snapshot from all Storage Nodes belonging to the given zone.
     * Each Storage Node will remove it from any managed services.
     * Operation applies to all zones if dcId is null.
     *
     * @param name the full name of the snapshot, including date time that was
     * generated by createSnapshot
     * @param dcId the datacenterId
     */
    public void removeSnapshot(String name, DatacenterId dcId)
        throws Exception {

        verbose("Start remove snapshot " + name);
        resetOperation(SnapshotOperation.REMOVE);
        makeSnapshotTasks(name, dcId);
        verbose("Complete remove snapshot " + name);
    }

    /**
     * Remove all known snapshots unconditionally.
     */
    public void removeAllSnapshots()
        throws Exception {

        removeAllSnapshots(null);
    }

    /**
     * Remove all known snapshots in a given zone. If null is supplied for dcId
     * all the known spanshots are removed unconditionally.
     */
    public void removeAllSnapshots(DatacenterId dcId)
        throws Exception {

        verbose("Start remove all snapshots");
        resetOperation(SnapshotOperation.REMOVEALL);
        makeSnapshotTasks(null, dcId);
        verbose("Complete remove all snapshots");
    }

    /**
     * Return an array of names of snapshots.  For now this will choose an
     * arbitrary Storage Node and ask it for its list under the assumption
     * that each SN should have the same snapshots.  Try all storage nodes
     * if any are not available.
     */
    public String[] listSnapshots()
        throws RemoteException {

        return cs.listSnapshots(null /* StorageNodeId */);
    }

    /**
     * A variant of listSnapshots that takes a specific StorageNodeId as the
     * target node.
     */
    public String[] listSnapshots(StorageNodeId snid)
        throws RemoteException {

        StorageNode sn = topo.get(snid);
        if (sn == null) {
            throw new IllegalArgumentException
                ("No Storage Node found with id " + snid);
        }
        verbose("Listing snapshots from Storage Node: " + snid);
        return cs.listSnapshots(snid);
    }

    private void resetOperation(SnapshotOperation newOp) {
        op = newOp;
        allSucceeded = true;
        quorumSucceeded = true;
        success = null;
        failure = null;
    }

    /**
     * Common code to perform the same task on all services and wait for
     * results.  Because storage nodes can be down put the possibility of
     * connection-related exceptions inside the task execution vs creation.
     */
    private void makeSnapshotTasks(String name, DatacenterId dcId)
        throws Exception {

        List<RepNode> repNodes = topo.getSortedRepNodes();
        createThreadPool();
        List<Future<SnapResult>> taskResults = createFutureList();

        /**
         * Do admins first
         */
        Parameters p = cs.getParameters();
        for (AdminId id : p.getAdminIds()) {
            AdminParams ap = p.get(id);
            StorageNode sn = topo.get(ap.getStorageNodeId());
            if (dcId != null && !sn.getDatacenterId().equals(dcId)) {
                continue;
            }
            verbose("Creating task (" + op + ") for " + id);
            taskResults.add
                (threadPool.submit
                    (new SnapshotTask(name, sn.getResourceId(), id)));
        }

        for (RepNode rn : repNodes) {
            StorageNode sn = topo.get(rn.getStorageNodeId());
            if (dcId != null && !sn.getDatacenterId().equals(dcId)) {
                continue;
            }
            verbose("Creating task (" + op + ") for " + rn);
            taskResults.add
                (threadPool.submit
                 (new SnapshotTask
                  (name, sn.getResourceId(), rn.getResourceId())));
        }
        waitForResults(taskResults);
    }

    private String makeSnapshotName(String name) {
        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyMMdd-HHmmss");
        return format.format(now) + "-" + name;
    }

    private void message(String msg) {
        if (output != null) {
            output.println(msg);
        }
    }

    private void verbose(String msg) {
        if (verboseOutput) {
            message(msg);
        }
    }

    private void createThreadPool() {
        if (threadPool == null) {
            threadPool = Executors.newCachedThreadPool
                (new KVThreadFactory("Snapshot", null));
        }
    }

    /**
     * Encapsulate the results of a distributed snapshot operation.
     */
    public static class SnapResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private final boolean result;
        private final ResourceId service;
        private final Exception ex;
        private final String message;
        private final SnapshotOperation op;

        protected SnapResult(SnapshotOperation op,
                             boolean result,
                             ResourceId service,
                             Exception ex,
                             String message) {
            this.op = op;
            this.result = result;
            this.service = service;
            this.ex = ex;

            /**
             * TODO: should message include stack trace in the exception case?
             */
            if (ex != null) {
                this.message = message + ", Exception: " + ex;
            } else {
                this.message = message;
            }
        }

        public boolean getSucceeded() {
            return result;
        }

        public ResourceId getService() {
            return service;
        }

        public String getMessage() {
            return message;
        }

        public Exception getException() {
            return ex;
        }

        public String getExceptionStackTrace() {
            if (ex != null) {
                return LoggerUtils.getStackTrace(ex);
            }
            return "";
        }

        @Override
        public String toString() {
            return "Operation " + op + " on " + service +
                (result ? " succeeded" : " failed") + ": " + message;
        }
    }

    private List<Future<SnapResult>> createFutureList() {
        return new ArrayList<Future<SnapResult>>();
    }

    /**
     * Walk the list of tasks aggregating results.  Because the SnapshotTask
     * will not throw an exception it should not be possible to get an
     * exception from the Future other than InterruptedException, but it needs
     * to be handled because of the Future.get() contract.
     */
    private void waitForResults(List<Future<SnapResult>> taskResults) {

        success = new ArrayList<SnapResult>();
        failure = new ArrayList<SnapResult>();

        for (Future<SnapResult> result : taskResults) {
            SnapResult snapResult = null;
            try {
                snapResult = result.get();
            } catch (InterruptedException e) {
                snapResult = new SnapResult(op, false, null, e, "Interrupted");
            } catch (ExecutionException e) {
                snapResult = new SnapResult(op, false, null, e, "Fail");
            }
            verbose("Task result (" + op + "): " + snapResult);
            if (snapResult.getSucceeded()) {
                success.add(snapResult);
            } else {
                allSucceeded = false;
                failure.add(snapResult);
            }
        }

        verbose("Operation " + op + ", Successful nodes: " + success.size() +
                ", failed nodes: " + failure.size());

        /**
         * Perform additional analysis.
         */
        processResults();
    }

    /**
     * Figure out if the operation was overall a success or failure based on
     * getting valid returns from enough nodes.  This isn't the most efficient
     * algorithm but it's simple.
     * o create a HashSet<RepGroupId> and populate it from the successes
     * o walk the topology and make sure that there is an entry in the set for
     * each RepGroup.
     * o note the failures.
     */
    private void processResults() {
        if (allSucceeded) {
            verbose("Operation " + op + " succeeded for all nodes");
            quorumSucceeded = true;
            return;
        }

        Set<RepGroupId> repGroups = new HashSet<RepGroupId>();
        boolean adminOK = false;
        for (SnapResult res : success) {
            ResourceId rid = res.getService();
            if (rid != null) {
                if (rid instanceof RepNodeId) {
                    RepGroupId rgid =
                        new RepGroupId(((RepNodeId)rid).getGroupId());
                    repGroups.add(rgid);
                } else {
                    adminOK = true;
                }
            }
        }

        if (!adminOK) {
            quorumSucceeded = false;
        }
        RepGroupMap rgm = topo.getRepGroupMap();
        for (RepGroup rg : rgm.getAll()) {
            if (!repGroups.contains(rg.getResourceId())) {
                message("Operation " + op + " did not succeed for shard "
                        + rg);
                quorumSucceeded = false;
            } else {
                verbose("Operation " + op + " succeeded for shard " + rg);
            }
        }
    }

    /**
     * Encapsulate a task to snapshot a single managed service, either RepNode
     * or Admin.
     */
    private class SnapshotTask implements Callable<SnapResult> {
        String name;
        StorageNodeId snid;
        ResourceId rid;

        public SnapshotTask(String name,
                            StorageNodeId snid,
                            ResourceId rid) {
            this.name = name;
            this.snid = snid;
            this.rid = rid;
        }

        @Override
        public SnapResult call() {
            try {
                return cs.executeSnapshotOperation(op, snid, rid, name);
            } catch (Exception e) {
                return new SnapResult(op, false, rid, e, "Failed");
            }
        }
    }
}
