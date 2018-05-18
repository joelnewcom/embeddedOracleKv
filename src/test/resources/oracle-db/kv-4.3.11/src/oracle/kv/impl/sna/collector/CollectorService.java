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

package oracle.kv.impl.sna.collector;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import oracle.kv.impl.admin.param.GlobalParams;
import oracle.kv.impl.admin.param.SecurityParams;
import oracle.kv.impl.admin.param.StorageNodeParams;
import oracle.kv.impl.param.ParameterListener;
import oracle.kv.impl.param.ParameterMap;
import oracle.kv.impl.security.login.LoginManager;

/**
 * SN collector service that will start all agents to collect monitor data.
 */
public class CollectorService {

    private List<CollectorAgent> agents;
    private boolean shutdown;
    private GlobalParamsListener globalParamsListener;
    private SNParamsListener snParamsListener;
    private Logger snaLogger;

    public CollectorService(StorageNodeParams snp,
                            GlobalParams gp,
                            SecurityParams sp,
                            LoginManager loginManager,
                            Logger snaLogger) {
        /* register all agents */
        FileCollectorRecorder collectorRecorder =
            new FileCollectorRecorder(snp, gp);
        agents = new ArrayList<CollectorAgent>();
        PingCollectorAgent pingAgent =
            new PingCollectorAgent(gp, snp, loginManager, collectorRecorder,
                                   snaLogger);
        JMXCollectorAgent metricAgent =
            new JMXCollectorAgent(snp, sp, collectorRecorder, snaLogger);
        agents.add(pingAgent);
        agents.add(metricAgent);
        if (gp.getCollectorEnabled()) {
            for (CollectorAgent agent : agents) {
                agent.start();
            }
        }
        shutdown = false;
        globalParamsListener = new GlobalParamsListener();
        snParamsListener = new SNParamsListener();
        this.snaLogger = snaLogger;
    }

    public synchronized void updateParams(GlobalParams newGlobalParams,
                                          StorageNodeParams newSNParams) {
        if (shutdown) {
            /* Collector service has been shutdown, don't update parameters. */
            return;
        }
        if (newGlobalParams != null) {
            snaLogger.fine("Collector service: new global params: " +
                newGlobalParams.getMap().toString());
        }
        if (newSNParams != null) {
            snaLogger.fine("Collector service: new SN params: " +
                newSNParams.getMap().toString());
        }
        for (CollectorAgent agent : agents) {
            agent.updateParams(newGlobalParams, newSNParams);
        }
    }

    public synchronized void shutdown() {
        shutdown = true;
        for (CollectorAgent agent : agents) {
            agent.stop();
        }
    }

    public ParameterListener getGlobalParamsListener() {
        return globalParamsListener;
    }

    public ParameterListener getSNParamsListener() {
        return snParamsListener;
    }

    /**
     * Global parameter change listener.
     */
    private class GlobalParamsListener implements ParameterListener {

        @Override
        public void newParameters(ParameterMap oldMap, ParameterMap newMap) {
            GlobalParams newGlobalParams = new GlobalParams(newMap);
            updateParams(newGlobalParams, null /* newSNParams */);
        }
    }

    /**
     * StorageNode parameter change listener.
     */
    private class SNParamsListener implements ParameterListener {

        @Override
        public void newParameters(ParameterMap oldMap, ParameterMap newMap) {
            StorageNodeParams newSNParams = new StorageNodeParams(newMap);
            updateParams(null /* newGlobalParams */, newSNParams);
        }
    }
}
