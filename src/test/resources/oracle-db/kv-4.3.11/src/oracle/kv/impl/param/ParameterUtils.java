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

package oracle.kv.impl.param;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import oracle.kv.RequestLimitConfig;
import oracle.kv.impl.admin.IllegalCommandException;
import oracle.kv.impl.util.ConfigUtils;

import com.sleepycat.je.CacheMode;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.config.EnvironmentParams;
import com.sleepycat.je.rep.ReplicationConfig;
import com.sleepycat.je.rep.ReplicationMutableConfig;
import com.sleepycat.je.rep.arbiter.ArbiterConfig;
import com.sleepycat.je.rep.impl.RepParams;
import com.sleepycat.je.utilint.JVMSystemUtils;

/**
 * See header comments in Parameter.java for an overview of Parameters.
 *
 * This is a utility class with some useful methods for handling Parameters.
 */
public class ParameterUtils {

    public final static String HELPER_HOST_SEPARATOR = ",";

    private final ParameterMap map;

    public ParameterUtils(ParameterMap map) {
        this.map = map;
    }

    /**
     * Default values for JE EnvironmentConfig, ReplicationConfig
     */
    private static final String DEFAULT_CONFIG_PROPERTIES=
        EnvironmentConfig.TXN_DURABILITY + "=" +
        "write_no_sync,write_no_sync,simple_majority;" +
        EnvironmentConfig.NODE_MAX_ENTRIES + "=128;" +
        EnvironmentConfig.CLEANER_THREADS + "=2;" +
        EnvironmentConfig.LOG_FILE_CACHE_SIZE + "=2000;" +
        EnvironmentConfig.CLEANER_READ_SIZE + "=1048576;" +
        EnvironmentConfig.CHECKPOINTER_BYTES_INTERVAL + "=500000000;" +
        EnvironmentConfig.ENV_RUN_EVICTOR + "=true;" +
        EnvironmentConfig.LOG_WRITE_QUEUE_SIZE + "=2097152;" +
        EnvironmentConfig.LOG_NUM_BUFFERS + "=16;" +
        EnvironmentConfig.LOG_FILE_MAX + "=1073741824;" +
        EnvironmentConfig.CLEANER_MIN_UTILIZATION + "=40;" +
        EnvironmentConfig.LOG_FAULT_READ_SIZE + "=4096;" +
        EnvironmentConfig.LOG_ITERATOR_READ_SIZE + "=1048576;" +
        EnvironmentConfig.LOCK_N_LOCK_TABLES + "=97;" +
        EnvironmentConfig.LOCK_TIMEOUT + "=10 s;" +

        /* Replication.  Not all of these are documented publicly */
        ReplicationConfig.ENV_UNKNOWN_STATE_TIMEOUT + "=10 s;" +
        ReplicationConfig.TXN_ROLLBACK_LIMIT + "=10;" +
        ReplicationConfig.REPLICA_ACK_TIMEOUT + "=5 s;" +
        ReplicationConfig.CONSISTENCY_POLICY + "=NoConsistencyRequiredPolicy;" +
        ReplicationMutableConfig.REPLAY_MAX_OPEN_DB_HANDLES + "=100;" +

        /*
         * Use timeouts shorter than the default 30sec to speed up failover
         * in network hardware level failure situations.
         */
        ReplicationConfig.FEEDER_TIMEOUT + "=10 s;" +
        ReplicationConfig.REPLICA_TIMEOUT + "=10 s;" +

        /* Ignore old primary node ID when restarting as secondary */
        RepParams.IGNORE_SECONDARY_NODE_ID.getName() + "=true;" +

        EnvironmentParams.REP_PARAM_PREFIX +
        "preHeartbeatTimeoutMs=5000000000;" +
        EnvironmentParams.REP_PARAM_PREFIX +
        "vlsn.distance=1000000;" +
        EnvironmentParams.REP_PARAM_PREFIX +
        "vlsn.logCacheSize=128;" +
        EnvironmentConfig.EVICTOR_CRITICAL_PERCENTAGE + "=20;";

    /**
     * Extra default RepEnv properties for Admin.
     */
    private static final String EXTRA_ADMIN_CONFIG_PROPERTIES=
        /* Persuade the Admin to listen on all interfaces, all the time. */
        ReplicationConfig.BIND_INADDR_ANY + "=true;" +

        /* Ignore old primary node ID when restarting as secondary */
        RepParams.IGNORE_SECONDARY_NODE_ID.getName() + "=true;" +

        /*
         * Override and reduce the default logCacheSize given in 
         * DEFAULT_CONFIG_PROPERTIES.  See [#25435].
         */
        EnvironmentParams.REP_PARAM_PREFIX + "vlsn.logCacheSize=32;";

    private static final String DEFAULT_ARB_CONFIG_PROPERTIES =
        ReplicationConfig.ENV_UNKNOWN_STATE_TIMEOUT + "=10 s;" +
        ReplicationConfig.FEEDER_TIMEOUT + "=10 s;" +
        ReplicationConfig.REPLICA_TIMEOUT + "=10 s;" +
        RepParams.MAX_CLOCK_DELTA + "=1 min";
    /**
     * Return an EnvironmentConfig set with the relevant parameters in this
     * object.
     */
    public EnvironmentConfig getEnvConfig() {
        EnvironmentConfig ec;
        ec = new EnvironmentConfig(createProperties(true, false));
        ec.setAllowCreate(true);
        ec.setTransactional(true);
        if (map.exists(ParameterState.RN_CACHE_PERCENT)) {
            ec.setCachePercent(
                map.get(ParameterState.RN_CACHE_PERCENT).asInt());
        }
        if (map.exists(ParameterState.JE_CACHE_SIZE)) {
            ec.setCacheSize(map.get(ParameterState.JE_CACHE_SIZE).asLong());
        }
        return ec;
    }

    /**
     * Return ArbiterConfig set with the relevant parameters in this
     * object.
     */
    public ArbiterConfig getArbConfig(String arbHome) {
        return getArbConfig(arbHome, false);
    }

    private ArbiterConfig getArbConfig(String arbHome, boolean validateOnly) {
        ArbiterConfig arbcfg;
        arbcfg = new ArbiterConfig(createArbProperties());
        if (arbHome != null) {
            arbcfg.setArbiterHome(arbHome);
        }
        if (map.exists(ParameterState.JE_HELPER_HOSTS)) {
            arbcfg.setHelperHosts(
                map.get(ParameterState.JE_HELPER_HOSTS).asString());
        }
        if (map.exists(ParameterState.JE_HOST_PORT)) {

            /*
             * If in validating mode, setting the nodeHostPort param will not
             * work correctly because in that case we are running on the admin
             * host and not the target host.  If validating, treat the
             * host:port as a single-value helper hosts string and validate
             * directly.
             */
            String nhp = map.get(ParameterState.JE_HOST_PORT).asString();
            if (validateOnly) {
                RepParams.HELPER_HOSTS.validateValue(nhp);
            } else {
                arbcfg.setNodeHostPort(nhp);
            }
            arbcfg.setNodeName(map.get(ParameterState.AP_AN_ID).asString());
        }
        return arbcfg;

    }

    /**
     * Return a ReplicationConfig set with RN-specific parameters.
     */
    public ReplicationConfig getRNRepEnvConfig() {
        return getRepEnvConfig(null, false, false);
    }

    /**
     * Return a ReplicationConfig set with Admin-specific parameters.
     */
    public ReplicationConfig getAdminRepEnvConfig() {
        return getRepEnvConfig(null, false, true);
    }

    /**
     * Check for incorrect JE parameters.
     */
    public static void validateParams(ParameterMap map) {

        /* Check for incorrect JE params. */
        try {
            ParameterUtils pu = new ParameterUtils(map);
            pu.getEnvConfig();
            pu.getRepEnvConfig(null, true, false);
        } catch (Exception e) {
            throw new IllegalCommandException("Incorrect parameters: " +
                                              e.getMessage(), e);
        }
    }

    public static void validateArbParams(ParameterMap map) {
        /* Check for incorrect JE params. */
        try {
            ParameterUtils pu = new ParameterUtils(map);
            pu.getArbConfig(null, true);
        } catch (Exception e) {
            throw new IllegalCommandException("Incorrect parameters: " +
                                              e.getMessage(), e);
        }
    }

    /* TODO: The method is not used; why is it here? */
    @SuppressWarnings("unused")
    private ReplicationConfig getRepEnvConfig(Properties securityProps) {
        return getRepEnvConfig(securityProps, false, false);
    }

    private ReplicationConfig getRepEnvConfig(Properties securityProps,
                                              boolean validating,
                                              boolean forAdmin) {
        ReplicationConfig rc;
        Properties allProps = createProperties(false, forAdmin);
        mergeProps(allProps, securityProps);
        rc = new ReplicationConfig(allProps);
        rc.setConfigParam("je.rep.preserveRecordVersion", "true");

        if (map.exists(ParameterState.JE_HELPER_HOSTS)) {
            rc.setHelperHosts(map.get(ParameterState.JE_HELPER_HOSTS).asString());
        }
        if (map.exists(ParameterState.JE_HOST_PORT)) {

            /*
             * If in validating mode, setting the nodeHostPort param will not
             * work correctly because in that case we are running on the admin
             * host and not the target host.  If validating, treat the
             * host:port as a single-value helper hosts string and validate
             * directly.
             */
            String nhp = map.get(ParameterState.JE_HOST_PORT).asString();
            if (validating) {
                RepParams.HELPER_HOSTS.validateValue(nhp);
            } else {
                rc.setNodeHostPort(nhp);
            }
        }
        return rc;
    }

    private void removeRep(Properties props) {
        Iterator<?> it = props.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            if (key.indexOf(EnvironmentParams.REP_PARAM_PREFIX) != -1) {
                it.remove();
            }
        }
    }

    private void mergeProps(Properties baseProps,
                            Properties mergeProps) {
        if (mergeProps == null) {
            return;
        }
        for (Object propKey : mergeProps.keySet()) {
            String propSKey = (String) propKey;
            String propVal = mergeProps.getProperty(propSKey);
            baseProps.setProperty(propSKey, propVal);
        }
    }

    /**
     * Create a Properties object from the DEFAULT_CONFIG_PROPERTIES String
     * and if present, the configProperties String.  Priority is given to the
     * configProperties (last property set wins).
     */
    public Properties createProperties(boolean removeReplication,
                                       boolean forAdmin) {
        Properties props = new Properties();
        String propertyString = DEFAULT_CONFIG_PROPERTIES;
        if (forAdmin) {
            propertyString += EXTRA_ADMIN_CONFIG_PROPERTIES;
        }
        String configProperties = map.get(ParameterState.JE_MISC).asString();
        if (configProperties != null) {
            propertyString = propertyString + ";" + configProperties;
        }
        try {
            props.load(ConfigUtils.getPropertiesStream(propertyString));
            if (removeReplication) {
                removeRep(props);
            }
        } catch (Exception e) {
            /* TODO: do something about this? */
        }
        return props;
    }


    public Properties createArbProperties() {
        Properties props = new Properties();
        String propertyString = DEFAULT_ARB_CONFIG_PROPERTIES;
        String configProperties = map.get(ParameterState.JE_MISC).asString();
        if (configProperties != null) {
            propertyString = propertyString + ";" + configProperties;
        }
        try {
            props.load(ConfigUtils.getPropertiesStream(propertyString));
        } catch (Exception e) {
            /* TODO: do something about this? */
        }
        return props;
    }

    public static CacheMode getCacheMode(ParameterMap map) {
        CacheModeParameter cmp =
            (CacheModeParameter) map.get(ParameterState.KV_CACHE_MODE);
        if (cmp != null) {
            return cmp.asCacheMode();
        }
        return null;
    }

    public static long getRequestQuiesceTime(ParameterMap map)  {
        return getDurationMillis(map, ParameterState.REQUEST_QUIESCE_TIME);
    }

    public static RequestLimitConfig getRequestLimitConfig(ParameterMap map) {
        final int maxActiveRequests =
            map.get(ParameterState.RN_MAX_ACTIVE_REQUESTS).asInt();
        final int requestThresholdPercent =
            map.get(ParameterState.RN_REQUEST_THRESHOLD_PERCENT).asInt();
        final int nodeLimitPercent =
            map.get(ParameterState.RN_NODE_LIMIT_PERCENT).asInt();

        return new RequestLimitConfig(maxActiveRequests,
                                      requestThresholdPercent,
                                      nodeLimitPercent);
    }

    public static long getThreadDumpIntervalMillis(ParameterMap map)  {
        return getDurationMillis(map, ParameterState.SP_DUMP_INTERVAL);
    }

    public static int getMaxTrackedLatencyMillis(ParameterMap map)  {
        return (int) getDurationMillis(map, ParameterState.SP_MAX_LATENCY);
    }

    public static long getDurationMillis(ParameterMap map, String parameter) {
        DurationParameter dp = (DurationParameter) map.getOrDefault(parameter);
        return dp.toMillis();
    }

    /* Parse the helper host string and return a list */
    public static List<String> helpersAsList(String helperHosts) {
        List<String> helpers = new ArrayList<String>();
        if ((helperHosts == null) || (helperHosts.length() == 0)) {
            return helpers;
        }

        String[] split = helperHosts.split(HELPER_HOST_SEPARATOR);
        for (String element : split) {
            helpers.add(element.trim());
        }

        return helpers;
    }

    /**
     * Given a desired heap size in MB, return the size that will actually be
     * allocated by the JVM when this is passed via -Xmx. Zing will bump the
     * heap up to 1 GB if -Xmx is smaller.
     *
     * It is best to specify the returned value as -Xmx so the Zing warning
     * message is avoided. Also the actual value may be needed by the caller to
     * calculate memory actually used, etc.
     */
    public static int applyMinHeapMB(int heapMB) {
        return Math.max(heapMB, JVMSystemUtils.MIN_HEAP_MB);
    }
}
