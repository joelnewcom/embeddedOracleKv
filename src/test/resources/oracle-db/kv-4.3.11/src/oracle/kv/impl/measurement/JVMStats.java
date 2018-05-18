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
package oracle.kv.impl.measurement;

import java.io.Serializable;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import com.sleepycat.je.utilint.JVMSystemUtils;

/**
 * Dump of Java Virtual Machine information.
 */
public class JVMStats implements ConciseStats, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Container class for garbage collector information.
     */
    static private class CollectorInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The name of the garbage collector this object represents.
         */
        private final String name;

        /**
         * The total number of collections that have occurred. This field is set
         * to the value returned by
         * {@link java.lang.management.GarbageCollectorMXBean#getCollectionCount
         * GarbageCollectorMXBean.getCollectionCount} at the time this object is
         * constructed.
         *
         * @see java.lang.management.GarbageCollectorMXBean#getCollectionCount
         * GarbageCollectorMXBean.getCollectionCount
         */
        private final long count;

        /**
         * The approximate accumulated collection elapsed time in milliseconds.
         * This field is set to the value returned by
         * {@link java.lang.management.GarbageCollectorMXBean#getCollectionTime
         * GarbageCollectorMXBean.getCollectionTime} at the time this object is
         * constructed.
         *
         * @see java.lang.management.GarbageCollectorMXBean#getCollectionTime
         * GarbageCollectorMXBean.getCollectionTime
         */
        private final long time;

        private CollectorInfo(GarbageCollectorMXBean gc) {
            name = gc.getName();
            count = gc.getCollectionCount();
            time = gc.getCollectionTime();
        }
    }

    private final long start;
    private final long end;

    /**
     * The amount of free memory in the Java Virtual Machine. This field is set
     * to the value returned by
     * {@link java.lang.Runtime#freeMemory() Runtime.freeMemory} at the time
     * this object is constructed.
     *
     * @see java.lang.Runtime#freeMemory Runtime.freeMemory
     */
    private final long freeMemory;

    /**
     * The maximum amount of memory that the Java virtual machine will attempt
     * to use. This field is set to the value returned by
     * {@link java.lang.Runtime#maxMemory() Runtime.maxMemory} (or for Zing,
     * a JVM-specific method) at the time this object is constructed.
     *
     * @see java.lang.Runtime#maxMemory() Runtime.maxMemory
     */
    private final long maxMemory;

    /**
     * The total amount of memory in the Java virtual machine. This field is set
     * to the value returned by
     * {@link java.lang.Runtime#totalMemory() Runtime.totalMemory} at the time
     * this object is constructed.
     *
     * @see java.lang.Runtime#totalMemory() Runtime.totalMemory
     */
    private final long totalMemory;

    /**
     * Garbage collectors operating in the Java virtual machine.
     */
    private final List<CollectorInfo> collectors;

    /**
     * Constructor. The JVM information contained in this object is collected
     * at construction time.
     */
    public JVMStats(long start, long end) {
    	this.start = start;
    	this.end = end;
        Runtime rt = Runtime.getRuntime();
        this.freeMemory = rt.freeMemory();
        this.maxMemory = JVMSystemUtils.getRuntimeMaxMemory();
        this.totalMemory = rt.totalMemory();

        List<GarbageCollectorMXBean> gcBeans =
                ManagementFactory.getGarbageCollectorMXBeans();

        collectors = new ArrayList<CollectorInfo>(gcBeans.size());

        for(GarbageCollectorMXBean gc : gcBeans) {
            collectors.add(new CollectorInfo(gc));
        }
    }

    /* -- From ConciseStats -- */

    @Override
    public long getStart() {
        return start;
    }

    @Override
    public long getEnd() {
        return end;
    }

    @Override
    public String getFormattedStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("Memory");
        sb.append("\n\tfreeMemory=");
        sb.append(freeMemory);
        sb.append("\n\tmaxMemory=");
        sb.append(maxMemory);
        sb.append("\n\ttotalMemory=");
        sb.append(totalMemory);
        for (CollectorInfo gc : collectors) {
            sb.append("\n");
            sb.append(gc.name);
            sb.append("\n\tcount=");
            sb.append(gc.count);
            sb.append("\n\ttime=");
            sb.append(gc.time);
        }
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "JVMStats[" + getFormattedStats() + "]";
    }
}
