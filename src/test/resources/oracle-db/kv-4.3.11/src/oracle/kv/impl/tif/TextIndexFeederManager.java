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

package oracle.kv.impl.tif;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;

import com.sleepycat.je.rep.StateChangeEvent;
import com.sleepycat.je.utilint.StoppableThread;

import oracle.kv.impl.admin.param.GlobalParams;
import oracle.kv.impl.admin.param.RepNodeParams;
import oracle.kv.impl.admin.param.StorageNodeParams;
import oracle.kv.impl.api.table.TableMetadata;
import oracle.kv.impl.param.ParameterListener;
import oracle.kv.impl.param.ParameterMap;
import oracle.kv.impl.param.ParameterState;
import oracle.kv.impl.param.ParameterState.Info;
import oracle.kv.impl.rep.RepNode;
import oracle.kv.impl.rep.RepNodeService.Params;
import oracle.kv.impl.util.ConfigUtils;
import oracle.kv.impl.util.FileNames;
import oracle.kv.impl.util.StateTracker;
import oracle.kv.impl.util.server.Log4j2julAppender;
import oracle.kv.impl.util.server.LoggerUtils;
import oracle.kv.table.Index;

/**
 * Object of TextIndexFeeder manager that lives in RN. The manager
 * has a state tracker to track RN state change and is responsible for
 * starting/stopping TIF according to RN state.
 */
public class TextIndexFeederManager implements ParameterListener {

    private static final String TIF_NODE_PREFIX = "TextIndexFeeder-";
    private static final int TIF_START_RETRY_WAIT_MS = 30*1000; 
    private final RepNode sourceRepNode;
    private final RepNode hostRepNode;
    private final String tifNodeName;
    private final String kvstore;
    private final File configFile;
    private final Logger logger;
    private final TextIndexFeederStateTracker stateTracker;

    private TextIndexFeeder textIndexFeeder;
    private Node esClientNode;
    /* if the manager is on master RN */
    private volatile boolean isOnMaster;
    /* true if store is a secure store */
    private final boolean isSecureStore;

    /**
     * Constructor for TIF manager that co-locates with source node
     *
     * @param repNode RN that TIF co-locates with source
     * @param params  parameters
     */
    public TextIndexFeederManager(RepNode repNode,
                                  Params params) {

        this(repNode, repNode, params);
    }

    /**
     * Constructor for TIF manager
     *
     * @param sourceRepNode RN of source node to stream from
     * @param hostRepNode   RN on which TIF is running
     * @param params        RN parameters
     */
    public TextIndexFeederManager(RepNode sourceRepNode,
                                  RepNode hostRepNode,
                                  Params params) {

        this.sourceRepNode = sourceRepNode;
        this.hostRepNode = hostRepNode;

        final StorageNodeParams snp = params.getStorageNodeParams();
        final GlobalParams gp = params.getGlobalParams();
        isSecureStore = params.getSecurityParams().isSecure();
        configFile =
            FileNames.getSNAConfigFile(snp.getRootDirPath(),
                                       gp.getKVStoreName(),
                                       snp.getStorageNodeId());

        kvstore = gp.getKVStoreName();

        /* TIF node must start with the prefix */
        tifNodeName = TIF_NODE_PREFIX +
                      hostRepNode.getRepNodeId().getFullName() + "-" +
                      UUID.randomUUID();
        logger = LoggerUtils.getLogger(this.getClass(), params);

        stateTracker = new TextIndexFeederStateTracker(sourceRepNode, logger);
        textIndexFeeder = null;
        esClientNode = null;

        /*
         * Set up log4j to route messages to our j.u.l-based logging
         * subsystem, so that ES messages are recorded in the RepNode log.
         */
        org.apache.log4j.Logger log4jRoot =
            org.apache.log4j.Logger.getRootLogger();
        log4jRoot.removeAllAppenders();  /* There should be none. */
        Log4j2julAppender kvServiceAppender =
        	Log4j2julAppender.getAppender4Logger(logger, "[tif][es]");
        log4jRoot.addAppender(kvServiceAppender);
    }

    /**
     * Checks if a nodeName a TIF node
     *
     * @param nodeName  name of node
     *
     * @return true if the nodeName is a TIF node, false otherwise
     */
    public static boolean isTIFNode(final String nodeName) {
        return nodeName.startsWith(TIF_NODE_PREFIX);
    }

    /**
     * Return true if TIF is actively running.  Called only by tests.
     *
     * @return true if TIF is actively running
     */
    public boolean isTIFRunning() {
        if (textIndexFeeder == null) {
            return false;
        }

        SubscriptionState state = textIndexFeeder.getSubscriptionState();
        return (state == SubscriptionState.PARTITION_TRANSFER ||
                state == SubscriptionState.REPLICATION_STREAM);
    }

    /**
     * Returns true if the manager is on a master RN, false otherwise.
     *
     * @return true if the manager is on a master RN, false otherwise.
     */
    public boolean isOnMaster() {
        return isOnMaster;
    }

    /**
     * Starts the state tracker
     */
    public void startTracker() {
        /* only start state tracker for non-secure store */
        if (!isSecureStore) {
            stateTracker.start();
        }
    }

    /**
     * Gets TIF created by the manager, null if no TIF has been created
     *
     * @return TIF created by the manager, null if no TIF has been created
     */
    public TextIndexFeeder getTextIndexFeeder() {
        return textIndexFeeder;
    }

    /**
     * Notes a state change in the replicated environment. The actual
     * work to change state is made asynchronously to allow a quick return.
     */
    public void noteStateChange(StateChangeEvent stateChangeEvent) {
        stateTracker.noteStateChange(stateChangeEvent);
    }

    /**
     * Shuts down TIF manager and its tracker and TIF
     *
     * @param deleteESIndex true if keep the ES index, false otherwise
     */
    public void shutdown(boolean deleteESIndex) {

        stopTif(deleteESIndex, false);

        logger.info("TIF manager at " +
                    hostRepNode.getRepNodeId().getFullName() +
                    " has shutdown.");
    }

    /**
     * TIFM acts on the new table metadata when the table manager is updated.
     * Failure results in a runtime exception's being thrown.
     *
     * @param oldMetadata old table metadata
     * @param newMetadata new table metadata
     */
    public void newTableMetadata(TableMetadata oldMetadata,
                                 TableMetadata newMetadata) {

    	if (! isOnMaster()) {
            return;
    	}

        Set<String> esIndicesToAdd = null;
        Set<String> esIndicesToDrop = null;
    	if (textIndexFeeder == null ) {
            /*
             * Without the feeder running, we can't tell whether a new index
             * has been added, but if there are any text indexes present in the
             * new metadata, we need to get the feeder cranked up.  This will
             * commonly happen when the very first text index is created.  If
             * it happens at other times, the TIF should already be running,
             * and starting here would be a remedial operation.
             */
            final List<Index> textIndices = newMetadata.getTextIndexes();
            if (!textIndices.isEmpty()) {
                logger.info("TIFM starting TIF, " +
                            "triggered by new table metadata including text " +
                            "indexes: " +
                            Arrays.toString(textIndices.toArray()));
                esIndicesToAdd =
                    TextIndexFeeder.buildESIndicesNames(kvstore, textIndices);
            } else {
                /* ignore if no TIF and no text index */
                logger.finest("There is no running TIF and no text index " +
                              "created, ignore.");
            }
    	} else {
            /* we have a running TIF */
            textIndexFeeder.setTableMetadata(newMetadata);
            /* compute es indices to add and drop */
            esIndicesToAdd = textIndexFeeder.esIndicesToAdd(oldMetadata);
            esIndicesToDrop = textIndexFeeder.esIndicesToDrop(oldMetadata);
    	}

        /* first drop es index if any, log any error in deleting ES index */
        dropESIndices(esIndicesToDrop);

        /*
         * add es index by restart TIF, note all existing ES indices will be
         * deleted and recreated from scratch. If fail to start TIF,
         * throw an exception.
         */
        if (esIndicesToAdd == null || esIndicesToAdd.isEmpty()) {
            /* nothing to add, return true to updater */
            logger.fine("Nothing to add, TIFM does not need to start TIF");
        } else {
            /* If any text indexes were added, then we have to start over. */
            logger.info("TIFM restarts TIF due to added text indices." +
                        "\nAll text indices to be recreated: " +
                        Arrays.toString(
                            newMetadata.getTextIndexes().toArray()) +
                        "\nAll new ES indices to add: " +
                        Arrays.toString(esIndicesToAdd.toArray()));

            /*
             * Keep the old indexes around, but remove the checkpoint
             * for this rep group so that index population will start
             * over from the beginning when the TIF is restarted.
             */
            restartTif(true);
        }
    }

    /**
     * If there is any change in TIF parameters, restart the TIF.
     */
    @Override
    public void newParameters(ParameterMap oldMap, ParameterMap newMap) {
    
    	if (! isOnMaster()) {
            return;
    	}

        if (oldMap.filter(Info.TIF, true).
            equals(newMap.filter(Info.TIF, true))) {

            return;
        }

        logger.info("Restarting TIF due to parameter change.");

        restartTif(false);
    }

    /*---------------------*/
    /*- private functions -*/
    /*---------------------*/

    /* Drops a list of ES indices */
    private void dropESIndices(Set<String> esIndicesToDelete) {

        if (esIndicesToDelete == null || esIndicesToDelete.isEmpty()) {
            /* nothing to drop, return true to updater */
            return;
        }

        logger.info("Drop text indices, all corresponding ES indices will be " +
                    "deleted. ES indices to be dropped: " +
                    Arrays.toString(esIndicesToDelete.toArray()));

        for (String esIndexName : esIndicesToDelete) {
            try {
                textIndexFeeder.dropIndex(esIndexName);
            } catch (Exception e) {

                /*
                 * if unable to drop an ES index, continue to avoid blocking
                 * the table manager. the ES index would be left in ES cluster
                 */
                logger.warning("Unable to drop ES index " + esIndexName +
                               " reason: " + e.getMessage());
            }
        }
    }

    /* Creates an ES client node through which TIF can access the ES cluster */
    private void buildESClientNode(StorageNodeParams snp)
        throws IllegalStateException {

        /* Should NOT ever build more than one ES node at a time. */
        if (null != esClientNode) {
            throw new IllegalStateException
                ("Building a second ES node while a first one already exists.");
        }

        final String clusterName = snp.getSearchClusterName();
        final String esMembers = snp.getSearchClusterMembers();
        final String hostName = snp.getHostname();

        esClientNode = ElasticsearchHandler.buildESClientNode(clusterName,
                                                              esMembers,
                                                              hostName);
        logger.info("ES client node created for cluster members: " +
                    clusterName + "/" + esMembers);
    }

    private void stopEsClientNode() {
        if (null != esClientNode) {
            logger.info("Closing ES client node");
            esClientNode.close();
            esClientNode = null;
        }
    }

    /* Checks if any text index defined on the source node */
    private String getTextIndices(TableMetadata tableMetadata) {

        String indexNames = "";
        final List<Index> indices = tableMetadata.getTextIndexes();
        for (int i = 0; i < indices.size(); i++) {
            if (i > 0) {
                indexNames += ", ";
            }
            indexNames = indexNames + indices.get(i).getName();
        }
        return "[" + indexNames + "]";
    }

    /*
     * Starts the Text Index Feeder
     *
     * @return true if starts TIF successfully, false otherwise.
     */
    private boolean activateTIF() {

        final SourceRepNode source = new SourceRepNode(kvstore, sourceRepNode);
        final HostRepNode host = new HostRepNode(tifNodeName, hostRepNode);

        /*
         * Get the most up-to-date version of parameters directly from the
         * config file.
         */
        StorageNodeParams snp =
            ConfigUtils.getStorageNodeParams(configFile, logger);
        RepNodeParams rnp =
            ConfigUtils.getRepNodeParams(configFile,
                                         hostRepNode.getRepNodeId(),
                                         logger);

        ParameterMap tuningParams = rnp.getMap().filter(Info.TIF, true);

        final boolean tifIsNodeClient = tuningParams.getOrDefault
            (ParameterState.RN_TIF_IS_NODE_CLIENT).asBoolean();

        logger.info("Ready to start TIF to stream data from source:\n" +
                    source + "\nto host node:\n" + host);

        boolean succ;

        /*
         * all ES exception during startup of TIF should be captured here
         * instead of being propagated to RN
         */
        try {
            /*
             * Ownership of the ES client node (if there is one) remains with
             * the TIF manager, while the ES client itself belongs to the ES
             * handler.  Upon closing, the ES handler should close the client,
             * but the TIF manager must close the node.  By keeping knowledge
             * of the node with the manager, we allow the TIF and ES handler to
             * remain ignorant about whether they are working with a node
             * client or a transport client.
             */
            Client esClient = null;
            if (tifIsNodeClient) {
                buildESClientNode(snp);
                esClient = esClientNode.client();
            } else {
                final String clusterName = snp.getSearchClusterName();
                final String esMembers = snp.getSearchClusterMembers();
                esClient = ElasticsearchHandler.getTransportClient
                    (clusterName, esMembers,
                     null /* don't set log4j appender (done in TIFM ctor) */);
            }

            final ElasticsearchHandler esHandler =
                new ElasticsearchHandler(esClient, logger);
            /* create TIF */
            textIndexFeeder = new TextIndexFeeder(source, host, esHandler,
                                                  tuningParams, logger);

            /* ensure ES index and mapping exists for each text index */
            textIndexFeeder.ensureESIndexAndMapping();

            /* now start stream data! */
            textIndexFeeder.startFeeder();
            logger.info("TIF " + host.getTifNodeName() +
                        " started to stream data on " +
                        hostRepNode.getRepNodeId().getFullName());
            succ = true;
        } catch (Exception e) {
            /* fail to prepare ES index and mapping */
            logger.log(Level.WARNING,
                       "Unable to ensure ES index or mapping due to " +
                       "error: " + e.getMessage(), e);
            succ = false;
        }

        return succ;
    }

    /* Thread to manage replicated environment state changes. */
    private class TextIndexFeederStateTracker extends StateTracker {

        TextIndexFeederStateTracker(RepNode repNode, Logger logger) {

            super(TextIndexFeederStateTracker.class.getSimpleName(),
                  repNode.getRepNodeId(), logger,
                  repNode.getExceptionHandler());

        }

        @Override
        protected void doNotify(StateChangeEvent sce) {
            if (sourceRepNode.getEnv(1) == null) {
                return;
            }

            if (shutdown.get()) {
                /* if tracker shutdown, stop TIF if running */
                stopTif(false, false);
                return;
            }

            logger.info("received state change " + sce.getState());

            if (sce.getState().isMaster()) {
                /* if a master */
                isOnMaster = true;

                final TableMetadata tableMetadata =
                    sourceRepNode.getTableManager().getTableMetadata();
                if (tableMetadata == null ||
                    tableMetadata.getTextIndexes().size() == 0) {
                    logger.info("no text index found, skip starting TIF.");
                } else {
                    logger.info("found text indices: " +
                                getTextIndices(tableMetadata) +
                                ", now start TIF.");
                    startTif();
                }
            } else {
                /* not a master */
                isOnMaster = false;
                stopTif(false, false);
            }
        }
    }

    /* The start/stop protocol is implemented here. */

    private enum TifState { STOPPED, STARTING, RUNNING }
    private TifState tifState = TifState.STOPPED;

    private synchronized void startTif() {
        if (tifState != TifState.STOPPED) {
            /* Either it's running or some other thread is trying to start it */
            logger.info("Request to start TIF, but it is already " + tifState);
            return;
        }

        logger.info("Starting TIF");
        tifState = TifState.STARTING;

        new StoppableThread("StartTIFThread") {
            @Override
            public void run() {
                /* Keep trying to start, as long as the state is STARTING. */
                while (true) {
                    synchronized(TextIndexFeederManager.this) {
                        if (tifState != TifState.STARTING) {
                            return;
                        }
                        if (activateTIF()) {
                            tifState = TifState.RUNNING;
                            return;
                        }
                        logger.info("Attempt to start TIF failed; will retry.");
                    }
                    try {
                        Thread.sleep(TIF_START_RETRY_WAIT_MS);
                    } catch(Exception e) {
                        /* Just go around the loop again. */
                    }
                }
            }
                        
            @Override
            protected Logger getLogger() {
                return logger;
            }
        }.start();
    }

    /* Shut down the TIF and its ES client. */
    private synchronized void stopTif(boolean deleteIndexes,
                                      boolean removeCheckpoint) {
        if (tifState == TifState.STOPPED) {
            return;
        }
        if (textIndexFeeder != null) {
            textIndexFeeder.stopFeeder(deleteIndexes, removeCheckpoint);
            textIndexFeeder = null;
        }
        stopEsClientNode();
        tifState = TifState.STOPPED;
    }

    /* Stop and start the TIF without allowing a stop to be inserted between. */
    private synchronized void restartTif(boolean removeCheckpoint) {
        stopTif(false, removeCheckpoint);
        startTif();
    }
}
