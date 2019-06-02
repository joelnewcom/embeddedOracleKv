/*-
 * Copyright (C) 2011, 2018 Oracle and/or its affiliates. All rights reserved.
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

package oracle.kv.impl.monitor;

import java.util.LinkedList;
import java.util.List;

import oracle.kv.impl.measurement.Measurement;
import oracle.kv.impl.measurement.Pruned;
import oracle.kv.impl.measurement.ServiceStatusChange;
import oracle.kv.impl.topo.ResourceId;
import oracle.kv.impl.util.server.LoggerUtils;

/**
 * An AgentRepository stores measurements generated by a MonitorAgent, which
 * are periodically collected by the Monitor system. It's used by services that
 * are remote to the central Monitor.
 */
public class AgentRepository {

    private final int maxSize;
    private LinkedList<Measurement> current;

    // TODO: statusChanges may become unused if we use ping for the waitsFor
    // task.
    private int statusChanges;

    /*
     * Keep a placeholder for information that must be removed from the
     * repository to keep it within bounds.
     */
    private Pruned pruned;

    /*
     * Default size for the repository.  TODO: tune?
     */
    private static final int DEFAULT_SIZE = 10000;

    public AgentRepository(String kvName,
                           ResourceId resourceId) {
        this(kvName, resourceId, DEFAULT_SIZE);
    }

    public AgentRepository(String kvName,
                           ResourceId resourceId,
                           int maxSize) {
        current = new LinkedList<Measurement>();
        if (resourceId != null) {
            LoggerUtils.registerMonitorAgentBuffer(kvName, resourceId, this);
        }
        this.maxSize = maxSize;
        pruned = new Pruned();
        statusChanges = 0;
    }

    /* For unit tests. */
    AgentRepository(int maxSize) {
        this(null, null, maxSize);
    }

    /**
     * Add another measurement.
     */
    public synchronized void add(Measurement p) {
        current.addLast(p);

        if (p instanceof ServiceStatusChange) {
            statusChanges++;
        }

        /*
         * The repository is too full, delete the earliest one, and aggregate
         * it in the pruned field.
         */
        if (current.size() > maxSize) {
            Measurement target = current.getFirst();
            if (target instanceof ServiceStatusChange) {
                statusChanges--;
            }
            pruned.record(target);
            current.removeFirst();
        }
    }

    /**
     * Retrieve all measurements, and wipe the buffer clean. Note that this
     * method must be thread safe. Although the monitor polling task is
     * guaranteed not to execute concurrently, the monitor can make an
     * out-of-band collection call to gather data.
     */
    public synchronized Snapshot getAndReset() {
        LinkedList<Measurement> retrieved = current;
        current = new LinkedList<Measurement>();

        /*
         * If we pruned some away, add a placeholder to the beginning, earlier
         * end of the list.
         */
        if (pruned.exists()) {
            retrieved.addFirst(pruned);
            pruned = new Pruned();
        }

        Snapshot snapshot = new Snapshot(statusChanges, retrieved);
        statusChanges = 0;
        return snapshot;
    }

    /**
     * We intentionally don't consider the pruned items in the size. Size() is
     * used to report how many items remain in the buffer at stop time, and the
     * pruned items shouldn't be counted when we're trying to decided if
     * any coordination is needed between the monitor and monitor agent.
     */
    public int size() {
        return current.size();
    }

    public class Snapshot {
        public final List<Measurement> measurements;
        public int serviceStatusChanges;

        Snapshot(int serviceStatusChanges,
                 List<Measurement> measurements) {
            this.serviceStatusChanges = serviceStatusChanges;
            this.measurements = measurements;
        }
    }
}
