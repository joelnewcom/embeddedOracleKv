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

package oracle.kv.impl.rep;

import static com.sleepycat.je.rep.NoConsistencyRequiredPolicy.NO_CONSISTENCY;
import static com.sleepycat.je.rep.QuorumPolicy.SIMPLE_MAJORITY;
import static com.sleepycat.je.rep.impl.RepParams.REPLAY_MAX_OPEN_DB_HANDLES;

import java.io.File;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.kv.KVVersion;
import oracle.kv.impl.admin.param.RepNodeParams;
import oracle.kv.impl.admin.param.StorageNodeParams;
import oracle.kv.impl.fault.ProcessExitCode;
import oracle.kv.impl.param.ParameterListener;
import oracle.kv.impl.param.ParameterMap;
import oracle.kv.impl.param.ParameterState;
import oracle.kv.impl.param.ParameterUtils;
import oracle.kv.impl.rep.RepNodeService.Params;
import oracle.kv.impl.test.TestHook;
import oracle.kv.impl.test.TestHookExecute;
import oracle.kv.impl.test.TestStatus;
import oracle.kv.impl.util.DatabaseUtils;
import oracle.kv.impl.util.FileNames;
import oracle.kv.impl.util.server.JENotifyHooks.LogRewriteListener;
import oracle.kv.impl.util.server.JENotifyHooks.RecoveryListener;
import oracle.kv.impl.util.server.JENotifyHooks.RedirectHandler;
import oracle.kv.impl.util.server.JENotifyHooks.SyncupListener;
import oracle.kv.impl.util.server.LoggerUtils;

import com.sleepycat.je.CacheMode;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentFailureException;
import com.sleepycat.je.EnvironmentMutableConfig;
import com.sleepycat.je.JEVersion;
import com.sleepycat.je.RecoveryProgress;
import com.sleepycat.je.rep.InsufficientLogException;
import com.sleepycat.je.rep.NetworkRestore;
import com.sleepycat.je.rep.NetworkRestoreConfig;
import com.sleepycat.je.rep.NodeType;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.ReplicationConfig;
import com.sleepycat.je.rep.ReplicationMutableConfig;
import com.sleepycat.je.rep.ReplicationNetworkConfig;
import com.sleepycat.je.rep.RestartRequiredException;
import com.sleepycat.je.rep.RollbackException;
import com.sleepycat.je.rep.StateChangeEvent;
import com.sleepycat.je.rep.StateChangeListener;
import com.sleepycat.je.rep.UnknownMasterException;
import com.sleepycat.je.rep.impl.RepParams;
import com.sleepycat.je.rep.impl.networkRestore.NetworkBackupStats;
import com.sleepycat.je.utilint.JVMSystemUtils;
import com.sleepycat.je.utilint.StoppableThread;

/**
 * RepEnvManager is responsible for managing the handle to the replicated
 * environment and the database handles associated with the environment.
 * <p>
 * This class is effectively a component of RepNode and is used exclusively
 * by it.
 */
public class RepEnvHandleManager implements ParameterListener {

    /**
     * A test hook for calls to the recovery listener's progress method,
     * passing the RecoveryProgress phase.
     */
    public static volatile TestHook<RecoveryProgress> recoveryProgressTestHook;

    /**
     * A test hook called while holding the semaphore used to control opening
     * the JE HA environment, passing the service parameters.
     */
    public static volatile TestHook<RepNodeService.Params> openEnvTestHook;

    private final RepNode repNode;
    private final RepNodeService.Params repServiceParams;
    private final RepNodeService repNodeService;

    /* The parameters needed to configure the replicated environment. */
    private final File envDir;
    private final File snapshotDir;
    private final EnvironmentConfig envConfig;
    private final ReplicationConfig renvConfig;
    private final VersionManager versionManager;

    /**
     * Listener factory used to create new listener instances each time a
     * replicated environment is reopened.
     */
    private final StateChangeListenerFactory listenerFactory;

    /*
     * Semaphore to ensure that only one thread updates the environment handle.
     */
    private final Semaphore renewRepEnvSemaphore = new Semaphore(1);
    /* Lock to synchronize access to the environment handle. */
    private final ReentrantReadWriteLock envLock;

    /* The handle being managed. */
    private ReplicatedEnvironment repEnv;

    private final Logger logger;

    /**
     * The object currently performing a network restore, or null if no network
     * restore is in progress.  Used to get network restore statistics.
     */
    private volatile NetworkRestore networkRestore = null;

    public RepEnvHandleManager(RepNode repNode,
                               StateChangeListenerFactory listenerFactory,
                               RepNodeService.Params params,
                               RepNodeService repNodeService) {

        assert listenerFactory != null;

        this.repNode = repNode;

        logger = LoggerUtils.getLogger(this.getClass(), params);
        repServiceParams = params;

        ParameterUtils pu =
            new ParameterUtils(repNode.getRepNodeParams().getMap());
        envConfig = pu.getEnvConfig();
        /*
         * Use EVICT_LN as the default cache mode to ensure that LNS in
         * partition dbs are explicitly evicted during any syncup operations
         * which replay these LNs.
         */
        envConfig.setCacheMode(CacheMode.EVICT_LN);

        final long rnMaxOffHeap = params.
            getRepNodeParams().getRNMaxOffHeap(params.getStorageNodeParams());
        envConfig.setOffHeapCacheSize(rnMaxOffHeap);

        renvConfig = pu.getRNRepEnvConfig();

        logger.info("NoSQL version:" + KVVersion.CURRENT_VERSION +
                    " JE version:" + JEVersion.CURRENT_VERSION +
                    " Java version:" + System.getProperty("java.version"));

        logger.info(String.format("JVM Runtime maxMemory:%,d kB" +
                                  " JE cache size:%,d kB" +
                                  " Max Offheap cache size:%,d kB",
                                  JVMSystemUtils.getRuntimeMaxMemory() / 1024,
                                  envConfig.getCacheSize() / 1024,
                                  rnMaxOffHeap / 1024));
        if (rnMaxOffHeap > 0) {
            logger.info("Environment variable MALLOC_ARENA_MAX: " +
                         System.getenv("MALLOC_ARENA_MAX"));
        }

        logger.info("Non-default JE properties for environment: " +
                    pu.createProperties(false, false));

        renvConfig.setGroupName(repNode.getRepNodeId().getGroupName());
        renvConfig.setNodeName(repNode.getRepNodeId().getFullName());
        renvConfig.setNodeType(repNode.getRepNodeParams().getNodeType());
        renvConfig.setElectableGroupSizeOverride(
            repNode.getRepNodeParams().getElectableGroupSizeOverride());

        if (TestStatus.isActive()) {
            renvConfig.setConfigParam(RepParams.SO_REUSEADDR.getName(),
                                      "true");

            renvConfig.setConfigParam(RepParams.SO_BIND_WAIT_MS.getName(),
                                      "120000");
        }

        /* Configure the JE HA communication mechanism */
        if (params.getSecurityParams() != null) {
            final Properties haProps =
                params.getSecurityParams().getJEHAProperties();
            logger.info("DataChannelFactory: " +
                        haProps.getProperty(
                            ReplicationNetworkConfig.CHANNEL_TYPE));
            renvConfig.setRepNetConfig(
                ReplicationNetworkConfig.create(haProps));
        }

        StorageNodeParams snParams = params.getStorageNodeParams();
        envDir = FileNames.getEnvDir(snParams.getRootDirPath(),
                                     params.getGlobalParams().getKVStoreName(),
                                     repNode.getRepNodeParams().
                                                    getStorageDirectoryFile(),
                                     snParams.getStorageNodeId(),
                                     repNode.getRepNodeId());
        snapshotDir =
            FileNames.getSnapshotDir(snParams.getRootDirPath(),
                                     params.getGlobalParams().getKVStoreName(),
                                     repNode.getRepNodeParams().
                                                    getStorageDirectoryFile(),
                                     snParams.getStorageNodeId(),
                                     repNode.getRepNodeId());
        this.listenerFactory = listenerFactory;

        envLock = new ReentrantReadWriteLock();

        if (FileNames.makeDir(envDir)) {
            logger.info("Created new environment dir: " + envDir);
        }
        versionManager = new VersionManager(logger, repNode);
        this.repNodeService = repNodeService;
    }

    /**
     * Used to keep track of the number of partitions in this environment and
     * update the replica db handle cache size. The cache must be large enough
     * so that it can maintain one handle per partition. The cache exists to
     * avoid repeated closing and opening database handles, which are expensive
     * operations, during the replay of the replication stream.
     *
     * @param rnPartitions the number of partitions associated with this RN
     */
    public void updateRNPartitions(int rnPartitions) {
        final int configHandles = Integer.parseInt(renvConfig.
               getConfigParam(REPLAY_MAX_OPEN_DB_HANDLES.getName()));

        final int maxOpenHandles =
            Math.max(configHandles, (rnPartitions + 1 /* member db */));

        renvConfig.setConfigParam(REPLAY_MAX_OPEN_DB_HANDLES.getName(),
                                  Integer.toString(maxOpenHandles));

        /*
         * Check the cached setting of REPLAY_MAX_OPEN_DB_HANDLES in
         * renvConfig against the actual config used by the open environment
         * and correct it if necessary.
         */
        final ReplicatedEnvironment configEnv = getEnv(1);

        if ((configEnv == null) || !configEnv.isValid()) {
            /*
             * Environment is not available, renvConfig will take effect when
             * the environment is next opened.
             */
            return;
        }

        try {
            final int actualHandles =
                Integer.parseInt(configEnv.getMutableConfig().
                                 getConfigParam(REPLAY_MAX_OPEN_DB_HANDLES.
                                                getName()));
            if (actualHandles != maxOpenHandles) {
                /* It's different reset it. */
                configEnv.setRepMutableConfig(renvConfig);
                logger.info("Hosted partitions: " + rnPartitions +
                            ". Dynamically changed replay handles from: " +
                            actualHandles + " to: " + maxOpenHandles);
            }
        } catch (IllegalStateException e) {
            /* Ignore it, environment was subsequently closed. */
            return;
        } catch (EnvironmentFailureException ife) {
            /* Ignore it, environment was subsequently invalidated. */
            return;
        }
    }

    /**
     * Return the replicated environment handle waiting if necessary for
     * one to be established in the face of
     * {@link RestartRequiredException}s.
     *
     * @param timeoutMs the time to wait
     *
     * @return the replicated environment handle, or null if no handle was
     * established in the timeout window.
     */
     ReplicatedEnvironment getEnv(long timeoutMs) {
         if (timeoutMs == 0) {
             /* Avoids throw of interrupts in context that don't allow it. */
             return repEnv;
         }

        /* Wait for an environment to be established. */
        boolean lockAcquired;
        try {
            lockAcquired =
                envLock.readLock().tryLock(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Unexpected interrupt", e);
        }

        if (!lockAcquired) {
            return null;
        }

        try {
            return repEnv;
        } finally {
            envLock.readLock().unlock();
        }
    }

    /**
     * Determines whether a state change event warrants env handle maint
     * actions.
     *
     * If the state has transitioned to DETACHED as a result of an exception in
     * a daemon thread, then the handle is re-established asynchronously.
     *
     * Note that the transition of a node to the DETACHED state due to some
     * EnvironmentFailureException could be detected concurrently in threads
     * processing requests as well. This mechanism serves as a backup when an
     * environment is quiescent from an application's viewpoint but some
     * administrative daemon (e.g. replica replay, cleaning, etc.) causes the
     * environment to be invalidated.
     *
     * @param env the non-null environment associated with the event
     *
     * @param stateChangeEvent the event
     */
    public void noteStateChange(ReplicatedEnvironment env,
                                StateChangeEvent stateChangeEvent) {
        if (!stateChangeEvent.getState().isDetached()) {
            return;
        }

        final EnvironmentFailureException efe = env.getInvalidatingException();

        if (efe == null) {
            logger.info(
                "Node in detached state. Handle is " +
                (env.isClosed() ? "closed." : "open."));
            return;
        }

        if (efe.isCorrupted()) {
            /*
             * Environment is corrupted, exit the process without restart.
             */
            logger.info("Exiting process. " +
                        " Node in detached state, environment corrupted." +
                        " Exception class: " + efe.getClass().getName() +
                        " Exception message: " + efe.getMessage());
            repNodeService.getFaultHandler().
                queueShutdown(efe, ProcessExitCode.NO_RESTART);
            return;
        }

        try {
            /* Provoke the exception that resulted in it becoming invalid. */
            throw efe;
        } catch (RollbackException rbe) {
            logger.info("Node in detached state. " +
                         "Handled being re-established.");
            /* asyncRenewRepEnv() will reopen the env. */
            asyncRenewRepEnv(env, rbe);
        } catch (InsufficientLogException ile) {
            asyncRenewRepEnv(env, ile);
        } catch (DatabaseException dbe) {
            /*
             * Something unanticipated with the environment, exit the process.
             * The SNA will restart this process.
             */
            logger.info("Exiting process. " +
                        " Node in detached state, environment invalid." +
                        " Exception class: " + dbe.getClass().getName() +
                        " Exception message: " + dbe.getMessage());
            repNodeService.getFaultHandler().
                 queueShutdown(dbe, ProcessExitCode.RESTART);
        }
    }

    /**
     * Recreates the replicated environment handle used to service all
     * requests directed at this node, performing the operation asynchronously
     * in a separate thread unless it is already underway.
     *
     * @param prevRepEnv the previous environment handle that is being
     * re-established. It's null if the handle is being created for the first
     * time; in this case the restartException argument must be null as well.
     *
     * @param restartException the exception that provoked the
     * re-establishment of the environment handle. It's null if prevRepEnv is
     * null. If non-null, it must be a "renewable" exception, that is one for
     * which isRenewable() returns true.
     */
    void asyncRenewRepEnv(ReplicatedEnvironment prevRepEnv,
                          DatabaseException restartException) {
        new AsyncRenewRepEnv(prevRepEnv, restartException).start();
    }

    /**
     * Recreates the replicated environment handle used to service all
     * requests directed at this node.
     * <p>
     * Multiple threads may simultaneously discover that the environment
     * has been invalidated and that new JE handles need to be established.
     * This method synchronizes the restart of the environment by ensuring
     * that at most one thread re-initiates the establishment of the
     * handles and any associated re-initializations, while the other
     * threads wait so they can use the newly established handle.
     *
     * @param prevRepEnv the previous environment handle that is being
     * re-established. It's null if the handle is being created for the first
     * time; in this case the restartException argument must be null as well.
     *
     * @param restartException the exception that provoked the
     * re-establishment of the environment handle. It's null if prevRepEnv is
     * null. If non-null, it must be a "renewable" exception, that is one for
     * which isRenewable() returns true.
     *
     * @return true if the environment handle was recreated by the call,
     * false if a creation was already in progress.
     */
    boolean renewRepEnv(ReplicatedEnvironment prevRepEnv,
                        DatabaseException restartException) {

        assert(((prevRepEnv == null) && (restartException == null)) ||
               ((prevRepEnv != null) && (restartException != null)));

        /*
         * The exception requiring a restart must satisfy isRenewable()
         * All others should result in a process exit.
         */
        assert((restartException == null) || isRenewable(restartException));

        if (!renewRepEnvSemaphore.tryAcquire()) {
            return false;
        }

        /* This thread is the one that will reopen the env handle. */
        try {
            envLock.writeLock().lockInterruptibly();
        } catch (InterruptedException ie) {
            renewRepEnvSemaphore.release();
            return false;
        }

        try {
            if ((repEnv != null) && (repEnv != prevRepEnv)) {
                /* It's already been renewed by some other thread. */
                return true;
            }

            /* Clean up previous environment. */
            if (prevRepEnv != null) {
                cleanupPrevEnv(prevRepEnv, restartException);
            }

            maybeResetRepGroup();
            repEnv = openEnv();
            if (prevRepEnv == null) {
                /*
                 * Initial startup. Perform this check before doing anything
                 * else in case local databases need to be updated.
                 */
                versionManager.checkCompatibility(repEnv);
            }
            repEnv.setStateChangeListener(listenerFactory.create(repEnv));
            repNode.updateDbHandles(repEnv);
            /* Re-establish database handles. */
            logger.info("Replicated environment handle " +
                        ((prevRepEnv == null) ? "" : "re-") + "established." +
                        " Cache size: " + repEnv.getConfig().getCacheSize() +
                        ", State: " + repEnv.getState());
            return true;
        } finally {

            /*
             * Free the readers, so they can proceed with the new
             * environment handle.
             */
            envLock.writeLock().unlock();
            renewRepEnvSemaphore.release();
        }
    }

    /**
     * Predicate to determine whether the environment handle should be renewed
     * without restarting the process as a result of the exception.
     *
     * Only the InsufficientLogException and RollbackException subclasses of
     * EFE result in the handle actually being renewed in the process. All
     * other EFEs result in a process exit.
     */
    private boolean isRenewable(DatabaseException exception) {
        return exception instanceof InsufficientLogException ||
               exception instanceof RollbackException;
    }

    /**
     * Implement cleanup appropriate for the exception to ensure that the
     * environment is closed cleanly and can be reopened if necessary.
     *
     * @param prevRepEnv the env that needs to be cleaned up
     *
     * @param restartException the exception that provoked the close of the
     * environment
     */
    private void cleanupPrevEnv(ReplicatedEnvironment prevRepEnv,
                                DatabaseException restartException) {

        if (restartException instanceof InsufficientLogException) {
            /*
             * Restore the log files, so that the environment can be reopened.
             */
            networkRestore((InsufficientLogException) restartException);
        } else {
            logger.log(Level.INFO,
                       "Closing environment handle in response to exception",
                       restartException);
        }

        repNode.closeDbHandles(false);

        try {
            prevRepEnv.close();
        } catch (DatabaseException e) {
            /* Ignore the exception, but log it. */
            logger.log(Level.INFO, "Exception closing environment", e);
        }
    }

    /**
     * Close the environment.
     */
    void closeEnv() {
        /* Wait for readers to exit. */
        envLock.writeLock().lock();
        try {
            if (repEnv != null) {
                try {
                    repEnv.close();
                } catch (IllegalStateException ise) {
                    /*
                     * Log complaints about unclosed handles, etc. during
                     * the environment close and keep going.
                     */
                    logger.info("IllegalStateException during env close. " +
                                ise.getMessage());
                } catch (EnvironmentFailureException efe) {
                    /*
                     * Open database handles during a environment close on a
                     * replica can sometimes result in this exception. Ignore
                     * it and proceed as for ISE. Remove when JE SR22023 is
                     * addressed.
                     */
                    logger.info("Environment failure during close. " +
                                efe.getMessage());
                }
            }
        } finally {
            envLock.writeLock().unlock();
        }
    }

    ReplicationNetworkConfig getRepNetConfig() {
        return renvConfig.getRepNetConfig();
    }

    long getReplicaAckTimeout(TimeUnit unit) {
        return renvConfig.getReplicaAckTimeout(unit);
    }

    /** Reset the JE replication group, if requested. */
    private void maybeResetRepGroup() {
        final RepNodeParams rnParams = repNode.getRepNodeParams();
        if (!rnParams.getResetRepGroup()) {
            return;
        }

        logger.info("Resetting replication group");
        final EnvironmentConfig resetEnvConfig = envConfig.clone();
        final ReplicationConfig resetRepConfig = renvConfig.clone();

        /*
         * Make the reset retain the current rep group UUID so that existing
         * nodes can rejoin the group when they restart
         */
        resetRepConfig.setConfigParam(
            RepParams.RESET_REP_GROUP_RETAIN_UUID.getName(), "true");

        DatabaseUtils.resetRepGroup(envDir, resetEnvConfig, resetRepConfig);
    }

    /**
     * Returns a replicated environment handle, dealing with any recoverable
     * exceptions in the process.
     *
     * @return the replicated environment handle. The handle may be in the
     * Master, Replica, or Unknown state.
     */
    private ReplicatedEnvironment openEnv() {

        boolean networkRestoreDone = false;

        assert TestHookExecute.doHookIfSet(openEnvTestHook, repServiceParams);

        /*
         * Plumb JE environment logging and progress listening output to
         * KVStore monitoring.
         */
        envConfig.setLoggingHandler
            (new RepEnvRedirectHandler(repServiceParams));
        envConfig.setRecoveryProgressListener
            (new RepEnvRecoveryListener(repServiceParams));
        renvConfig.setSyncupProgressListener
            (new RepEnvSyncupListener(repServiceParams));
        renvConfig.setLogFileRewriteListener
            (new RepEnvLogRewriteListener(snapshotDir, repServiceParams));

        while (true) {
            try {
                final ReplicatedEnvironment renv =
                        new ReplicatedEnvironment(envDir,
                                                  renvConfig,
                                                  envConfig,
                                                  NO_CONSISTENCY,
                                                  SIMPLE_MAJORITY);

                logger.info(String.format(
                                "Opened JE environment: " +
                                JEVersion.CURRENT_VERSION.getVersionString() +
                                " JVM max heap: %,d JE properties: %s",
                                JVMSystemUtils.getRuntimeMaxMemory(),
                                renv.getConfig()));
                return renv;
            } catch (UnknownMasterException unknownMaster) {

                /*
                 * Assuming that timeouts are correctly configured there
                 * isn't much point in retrying, just rethrow the
                 * exception.
                 */
                throw unknownMaster;
            } catch (InsufficientLogException ile) {
                if (networkRestoreDone) {

                    /*
                     * Should have made progress after the earlier network
                     * restore, propagate the exception to the caller so it
                     * can be logged and propagated back to the client.
                     */
                    throw ile;
                }
                networkRestore(ile);
                continue;
            } catch (RollbackException rbe) {
                Long time = rbe.getEarliestTransactionCommitTime();
                logger.info("Rollback exception retrying: " +
                            rbe.getMessage() +
                            ((time ==  null) ?
                             "" :
                             " Rolling back to: " + new Date(time)));
                continue;
            }
        }
    }

    /**
     * Configures and initiates a network restore operation in response to
     * an ILE
     */
    private void networkRestore(InsufficientLogException ile) {
        final NetworkRestoreConfig config = new NetworkRestoreConfig();

        // TODO: sort the ile.getLogProviders() list based upon
        // datacenter proximity and pass it in as the arg below.
        config.setLogProviders(null);
        final boolean nrConfigRetainLogFiles =
            repNode.getRepNodeParams().getNRConfigRetainLogFiles();
        config.setRetainLogFiles(nrConfigRetainLogFiles);

        networkRestore = new NetworkRestore();
        try {
            networkRestore.execute(ile, config);
        } finally {
            networkRestore = null;
        }
    }

    /**
     * Returns network backup statistics for a network restore that is
     * currently underway, or null if there is no network restore in progress
     * or the statistics are otherwise not available.
     */
    NetworkBackupStats getNetworkRestoreStats() {
        final NetworkRestore networkRestoreSnapshot = networkRestore;
        if (networkRestoreSnapshot != null) {
            return networkRestoreSnapshot.getNetworkBackupStats();
        }
        return null;
    }

    /**
     * A custom Handler for JE log handling.  This class provides a unique
     * class scope for the purpose of logger creation.
     */
    private static class RepEnvRedirectHandler extends RedirectHandler {
        RepEnvRedirectHandler(Params repServiceParams) {
            super(LoggerUtils.getLogger(RepEnvRedirectHandler.class,
                                        repServiceParams));
        }
    }

    /**
     * A custom Handler for JE recovery recovery notification.  This class
     * provides a unique class scope for the purpose of logger creation.
     */
    private static class RepEnvRecoveryListener extends RecoveryListener {
        RepEnvRecoveryListener(Params repServiceParams) {
            super(LoggerUtils.getLogger(RepEnvRecoveryListener.class,
                                        repServiceParams));
        }
        /** Add a test hook */
        @Override
        public boolean progress(RecoveryProgress phase, long n, long total) {
            assert TestHookExecute.doHookIfSet(recoveryProgressTestHook,
                                               phase);
            return super.progress(phase, n, total);
        }
    }

    /**
     * A custom Handler for JE syncup progress notification.  This class
     * provides a unique class scope for the purpose of logger creation.
     */
    private static class RepEnvSyncupListener extends SyncupListener {
        RepEnvSyncupListener(Params repServiceParams) {
            super(LoggerUtils.getLogger(RepEnvSyncupListener.class,
                                        repServiceParams));
        }
    }

    /**
     * A custom Handler for JE log rewrite notification.  This class provides
     * a unique class scope for the purpose of logger creation.
     */
    private static class RepEnvLogRewriteListener extends LogRewriteListener {
        RepEnvLogRewriteListener(File snapshotDir, Params repServiceParams) {
            super(snapshotDir,
                  LoggerUtils.getLogger(RepEnvLogRewriteListener.class,
                                        repServiceParams));
        }
    }

    /**
     * Implementation of ParameterListener.
     */
    @Override
    public void newParameters(ParameterMap oldMap, ParameterMap newMap) {
        final ReplicatedEnvironment env = getEnv(1);

        if ((env == null) || !env.isValid()) {
            /*
             * The parameters will be set on the env when the handle is created.
             */
            return;
        }

        final EnvironmentMutableConfig mutableConfig = env.getMutableConfig();
        long oldSize = mutableConfig.getCacheSize();
        int oldPercent = mutableConfig.getCachePercent();
        long newSize = newMap.getOrZeroLong(ParameterState.JE_CACHE_SIZE);
        int newPercent = newMap.get(ParameterState.RN_CACHE_PERCENT).asInt();
        if (oldSize != newSize || oldPercent != newPercent) {
            mutableConfig.setCacheSize(newSize);
            mutableConfig.setCachePercent(newPercent);
            env.setMutableConfig(mutableConfig);
        }

        final ReplicationMutableConfig repMutableConfig =
            env.getRepMutableConfig();
        final int oldElectableGroupSizeOverride =
            repMutableConfig.getElectableGroupSizeOverride();
        final RepNodeParams newParams = new RepNodeParams(newMap);
        final int newElectableGroupSizeOverride =
            newParams.getElectableGroupSizeOverride();
        if (oldElectableGroupSizeOverride != newElectableGroupSizeOverride) {
            repMutableConfig.setElectableGroupSizeOverride(
                newElectableGroupSizeOverride);
            env.setRepMutableConfig(repMutableConfig);
        }

        /*
         * Check whether the new parameters specify a new node type that is not
         * reflected by the environment.  This could happen if, because of a
         * bug, we failed to restart the environment after updating the
         * parameter.
         */
        final NodeType envNodeType = env.getRepConfig().getNodeType();
        final NodeType newNodeType = newParams.getNodeType();
        if (!envNodeType.equals(newNodeType)) {
            throw new IllegalStateException(
                "Environment for " + repNode.getRepNodeId() +
                " has wrong node type: expected " + newNodeType +
                ", found " + envNodeType);
        }
    }

    /**
     * The factory interface used to create a new SCL each time an environment
     * is opened.
     */
    public interface StateChangeListenerFactory {
        public StateChangeListener create(ReplicatedEnvironment repEnv);
    }

    /**
     * Thread to perform an asynchronous renewal of the replicated environment
     * handle. This short-lived thread is used when an established thread of
     * control cannot be stalled for a potentially long-running operation like
     * the establishment of the environment handle.
     */
    private class AsyncRenewRepEnv extends StoppableThread {
        /* The environment provoking the exception. */
        ReplicatedEnvironment environment;

        /* Rollback exception in the replica thread. */
        DatabaseException exception;

        @Override
        public void run() {
            try {
                renewRepEnv(environment, exception);
            } finally {
                /* Remove references to avoid OOME. [#25649] */
                environment = null;
                exception = null;
            }
        }

        /* (non-Javadoc)
         * @see com.sleepycat.je.utilint.StoppableThread#getLogger()
         */
        @Override
        protected Logger getLogger() {
            return logger;
        }

        AsyncRenewRepEnv(ReplicatedEnvironment environment,
                         DatabaseException exception) {
            super("AsncRenewRepEnvThread");
            if (!isRenewable(exception)) {
                throw new IllegalArgumentException("Unexpected exception: " +
                                                    exception);
            }
            this.environment = environment;
            this.exception = exception;
        }
    }
}
