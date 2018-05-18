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

package oracle.kv.impl.rep.monitor;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import oracle.kv.impl.measurement.ConciseStats;
import oracle.kv.impl.measurement.EnvStats;
import oracle.kv.impl.measurement.LatencyInfo;
import oracle.kv.impl.measurement.Measurement;
import oracle.kv.impl.measurement.PerfStatType;
import oracle.kv.impl.measurement.RepEnvStats;
import oracle.kv.impl.monitor.Metrics;
import oracle.kv.impl.util.FormatUtils;
import oracle.kv.impl.util.JsonUtils;

import com.sleepycat.je.utilint.Stat;
import com.sleepycat.je.utilint.StatDefinition;
import com.sleepycat.je.utilint.StatGroup;
import com.sleepycat.utilint.Latency;

import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

/**
 * A set of stats from a single measurement period.
 * 
 * The RepNodes keep stats per type of API operation, as defined by
 * oracle.kv.impl.api.ops.InternalOperation. These are the base, detailed
 * interval stats. These stats can then be aggregated in two dimensions: (a)
 * over time to create cumulative stats, which cover the duration of this
 * repNode's uptime and (b) by type, so that several types of operations are
 * combined.
 * 
 * Currently we summarize all user operations by interval and cumulative. To
 * illustrate, suppose there are these interval collections:
 * 
 * interval 1: base stats collected for get, deleteIfVersion.
 *    summarized cumulative stats encompass interval 1, all operations
 *    summarized interval stats encompass interval 1, all operations
 * interval 2: base stats collected for get, multiget
 *    summarized cumulative stats encompass interval 1 & 2, all operations
 *    summarized interval stats encompass interval 2, all operation.
 * interval 3: base stats collected for putIfAbsent, putIfVersion
 *    summarized cumulative stats encompass interval 1, 2, 3, all operations
 *    summarized interval stats encompass interval 3, all operations
 *
 * The summarized cumulative stats must be calculated on the RepNode, so that
 * they reflect the lifetime of the RepNode instance. The interval stats
 * could be calculated either on the RepNode or in the Admin/Monitor, but are
 * calculated in the RepNode for code consistency.
 *
 * The summarized stats are the most commonly used. The base stats are also 
 * shipped across the wire to the Admin/Monitor for use in the CSVView, which 
 * has more limited utility. They may also be used in the future for other
 * analysis.
 * 
 * These stats are also shipped to SN monitoring and management agent.
 */
public class StatsPacket implements Measurement, Serializable {

    private static final long serialVersionUID = 1L;
    private final Map<Integer, LatencyInfo> latencies;
    private EnvStats envStats;
    private RepEnvStats repEnvStats;
    private final long start;
    private final long end;
    private final Map<String, Integer> exceptionStats;
    private int activeRequests;
    private int totalRequests;

    /* To act as tags for aggregating */
    private final String resource;
    private final String shard;

    /* Since R2 */
    private List<ConciseStats> otherStats = null;

    public StatsPacket(long start, long end, String resource, String shard) {
        this.start = start;
        this.end = end;
        latencies = new HashMap<Integer, LatencyInfo>();
        exceptionStats = new HashMap<String, Integer>();
        this.resource = resource;
        this.shard = shard;
    }

    public void add(LatencyInfo m) {
        latencies.put(m.getPerfStatId(), m);
    }

    public void add(EnvStats stats) {
        this.envStats = stats;
    }

    public void add(RepEnvStats stats) {
        this.repEnvStats = stats;
    }

    public void add(ConciseStats stats) {
        if (otherStats == null) {
            otherStats = new ArrayList<ConciseStats>();
        }
        otherStats.add(stats);
    }

    public void add(String e, int count) {
        exceptionStats.put(e, count);
    }

    public Map<String, Integer> getExceptionStats() {
        return exceptionStats;
    }

    public String getResource() {
        return resource;
    }
    public String getShard() {
        return shard;
    }

    public int getActiveRequests() {
        return activeRequests;
    }

    public void setActiveRequests(int activeRequests) {
        this.activeRequests = activeRequests;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
    }

    public LatencyInfo get(PerfStatType perfType) {
        return latencies.get(perfType.getId());
    }

    @Override
    public long getStart() {
        return start;
    }

    @Override
    public long getEnd() {
        return end;
    }

    @Override
    public int getId() {
        return Metrics.RNSTATS.getId();
    }

    /**
     * WriteCSVHeader would ideally be a static, but we use the presence of an
     * envStat and repEnvStat to determine whether env stat dumping is enabled.
     */
    public void writeCSVHeader(PrintStream out, 
                               PerfStatType[] headerList,
                               Map<String, Long> sortedEnvStats) {
        out.print("Date,");

        for (PerfStatType perfType : headerList) {
            out.print(LatencyInfo.getCSVHeader(perfType.toString()) + ",");
        }

        for (String name : sortedEnvStats.keySet()) {
            out.print(name + ",");
        }
    }

    /** StatsPackets know how to record themselves into a .csv file. */
    public void writeStats(PrintStream out, 
                           PerfStatType[] statList,
                           Map<String, Long> sortedEnvStats) {

        out.print(getFormattedDate() + ",");

        for (PerfStatType statType : statList) {
            LatencyInfo lm = latencies.get(statType.getId());
            if (lm == null) {
                out.print(LatencyInfo.ZEROS);
            } else {
                out.print(lm.getCSVStats());
            }
            out.print(",");
        }

        for (Long value : sortedEnvStats.values()) {
            out.print(value + ",");
        }
        out.println("");
    }

    private String getFormattedDate() {
        return FormatUtils.formatDateAndTime(end);
    }

    /**
     * Sort env and rep env stats by name, for use in the .csv file.  Could be
     * made more efficient, so the sorting need not be done each collection
     * time, if the .csv file generation feature becomes more commonly used.
     */
    public Map<String, Long> sortEnvStats() {

        final Collection<StatGroup> groups = new ArrayList<StatGroup>();

        if (repEnvStats != null) {
            groups.addAll(repEnvStats.getStats().getStatGroups());
        }

        if (envStats != null) {
            groups.addAll(envStats.getStats().getStatGroups());
        }

        Map<String, Long> sortedVals = new TreeMap<String, Long>();
        for (StatGroup sg : groups) {
            for (Map.Entry<StatDefinition, Stat<?>> e : 
                     sg.getStats().entrySet()) {

                String name = ('"' + sg.getName() + "\n" +
                               e.getKey().getName() + '"').intern();
                Object val = e.getValue().get();
                if (val instanceof Number) {
                    sortedVals.put(name,((Number) val).longValue());
                }
            }
        }
        return sortedVals;
    }

    /**
     * Rollup the stats contained within this packet by the summary list
     * provided as an argument. 
     */
    public Map<PerfStatType, LatencyInfo> 
        summarizeLatencies(PerfStatType[] summaryList) {

        /* Setup a map to hold summary rollups for each summary stat */
        Map<PerfStatType, LatencyInfo> rollupValues = 
            new HashMap<PerfStatType, LatencyInfo>();

        /* 
         * If this latency is a child of one of the summary stats, add the
         * child into the summary.
         */
        for (LatencyInfo m: latencies.values()) {
            for (PerfStatType root : summaryList) {
                if (PerfStatType.getType(m.getPerfStatId()).getParent() == 
                    root) {
                    LatencyInfo existing = rollupValues.get(root);

                    if (existing == null) {
                        rollupValues.put
                            (root, new LatencyInfo(root, m));
                    } else {
                        existing.rollup(m);
                    }
                }
            }
        }

        return rollupValues;
    }

    /**
     * Rollup the stats contained within this packet by the summary list
     * provided as an argument. The rolledup values are written to the out
     * stream, but are also returned so that unit tests can check the values.
     */
    public Map<PerfStatType, LatencyInfo>
        summarizeAndWriteStats(PrintStream out, 
                               PerfStatType[] summaryList,
                               Map<String, Long> sortedEnvStats) {

        out.print(getFormattedDate() + ",");

        /* Setup a map to hold summary rollups for each summary stat */
        Map<PerfStatType, LatencyInfo> rollupValues = 
            summarizeLatencies(summaryList);

        /* Dump the stats. */
        for (PerfStatType root : summaryList) {
            LatencyInfo m = rollupValues.get(root);
            if (m == null) {
                m = LatencyInfo.ZERO_MEASUREMENT;
            }
            out.print(m.getCSVStats() + ",");
        }

        for (Long value : sortedEnvStats.values()) {
            out.print(value + ",");
        }

        out.println("");
        return rollupValues;
    }
    
    public EnvStats getEnvStats() {
        return envStats;
    }

    public RepEnvStats getRepEnvStats() {
        return repEnvStats;
    }

    /**
     * Returns the list of other stats or {@code null}. The returned value may
     * be {@code null} if no stats were added, or due to receiving a
     * {@code StatPacket} from an older version node.
     * 
     * @return the list of other stats or null
     */
    public List<ConciseStats> getOtherStats() {
        return otherStats;
    }

    /* For unit test support */
    public List<Latency> getLatencies() {
        List<Latency> latencyList = new ArrayList<Latency>();
        for (LatencyInfo lm : latencies.values()) {
            latencyList.add(lm.getLatency());
        }
        return latencyList;
    }

    @Override
        public String toString() {
        StringBuilder sb = new StringBuilder();
        for (LatencyInfo m: latencies.values()) {
            sb.append(m).append("\n");
        }
        return sb.toString();
    }

    public String toOpJsonString() {
        try {
            ObjectNode jsonRoot = JsonUtils.createObjectNode();
            jsonRoot.put("resource", getResource());
            jsonRoot.put("shard", getShard());
            jsonRoot.put("datetime", getEnd());
            int total = 0;
            for (Entry<String, Integer> entry :
                getExceptionStats().entrySet()) {
                    total += entry.getValue();
            }
            jsonRoot.put("Exception_Total_Count", total);
            for (PerfStatType type : PerfStatType.values()) {
                LatencyInfo latencyInfo = get(type);
                if (latencyInfo == null) {
                    continue;
                }
                Latency latency = latencyInfo.getLatency();
                jsonRoot.put(type.toString() + "_TotalOps",
                    latency.getTotalOps());
                jsonRoot.put(type.toString() + "_TotalReq",
                    latency.getTotalRequests());
                jsonRoot.put(type.toString() + "_PerSec",
                    latencyInfo.getThroughputPerSec());
                jsonRoot.put(type.toString() + "_Min",
                    latency.getMin());
                jsonRoot.put(type.toString() + "_Max",
                    latency.getMax());
                jsonRoot.put(type.toString() + "_Avg",
                    latency.getAvg());
                jsonRoot.put(type.toString() + "_95th",
                    latency.get95thPercent());
                jsonRoot.put(type.toString() + "_99th",
                    latency.get99thPercent());
            }
            jsonRoot.put("Active_Requests", getActiveRequests());
            jsonRoot.put("Total_Requests", getTotalRequests());
            ObjectWriter writer = JsonUtils.createWriter(false);
            return writer.writeValueAsString(jsonRoot);
        } catch (Exception e) {
            return "";
        }
    }

    public String toExceptionsJsonString() {
        try {
            ObjectNode jsonRoot = JsonUtils.createObjectNode();
            jsonRoot.put("resource", getResource());
            jsonRoot.put("shard", getShard());
            jsonRoot.put("datetime", getEnd());
            ArrayNode exceptionArray = jsonRoot.putArray("Exceptions");
            int total = 0;
            for (Entry<String, Integer> entry :
                getExceptionStats().entrySet()) {

                if (entry.getValue() > 0) {
                    total += entry.getValue();
                    ObjectNode exceptionNode = JsonUtils.createObjectNode();
                    exceptionNode.put("Exception_Name", entry.getKey());
                    exceptionNode.put("Exception_Count", entry.getValue());
                    exceptionArray.add(exceptionNode);
                }
            }
            if (total == 0) {
                return "";
            }
            ObjectWriter writer = JsonUtils.createWriter(false);
            return writer.writeValueAsString(jsonRoot);
        } catch (Exception e) {
            return "";
        }
    }

    public String toEnvJsonString() {
        final Collection<StatGroup> groups = new ArrayList<StatGroup>();
        if (repEnvStats != null) {
            groups.addAll(repEnvStats.getStats().getStatGroups());
        }
        if (envStats != null) {
            groups.addAll(envStats.getStats().getStatGroups());
        }
        if (groups.size() == 0) {
            return "";
        }
        try {
            ObjectNode jsonRoot = JsonUtils.createObjectNode();
            jsonRoot.put("resource", getResource());
            jsonRoot.put("shard", getShard());
            jsonRoot.put("datetime", getEnd());

            for (StatGroup sg : groups) {
                if (sg == null) {
                    continue;
                }
                for (Map.Entry<StatDefinition, Stat<?>> e :
                    sg.getStats().entrySet()) {

                    String name = (sg.getName() + "_" +
                                   e.getKey().getName()).intern();
                    Object val = e.getValue().get();
                    if (val instanceof Number) {
                        jsonRoot.put(name, ((Number) val).longValue());
                    }
                    if (val instanceof String) {
                        jsonRoot.put(name, (String) val);
                    }
                }
            }
            ObjectWriter writer = JsonUtils.createWriter(false);
            return writer.writeValueAsString(jsonRoot);
        } catch (Exception e) {
            return "";
        }
    }
}
