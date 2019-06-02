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

package oracle.kv.impl.fault;

import java.util.logging.Logger;

import oracle.kv.FaultException;
import oracle.kv.KVSecurityException;
import oracle.kv.MetadataNotFoundException;
import oracle.kv.impl.api.table.TableVersionException;
import oracle.kv.impl.query.QueryRuntimeException;
import oracle.kv.impl.rep.EnvironmentFailureRetryException;
import oracle.kv.impl.security.SessionAccessException;
import oracle.kv.impl.test.TestStatus;
import oracle.kv.impl.util.ConfigurableService;
import oracle.kv.impl.util.ConfigurableService.ServiceStatus;
import oracle.kv.impl.util.server.LoggerUtils;

import com.sleepycat.je.EnvironmentFailureException;

/**
 * The fault handler for the RepNodeService or ArbNodeService
 */
public class ServiceFaultHandler extends ProcessFaultHandler {

    private final ConfigurableService service;

    /** The fault that results in the process shutting down. */
    private Throwable shutdownFault;

    /**
     * Creates a fault handler for the RepNodeService or ArbNodeService
     *
     * @param service the service whose faults are being handled
     * @param logger the logger associated with the service
     * @param defaultExitCode the default exit code to use on a process exit
     */
    public ServiceFaultHandler(ConfigurableService service,
                               Logger logger,
                               ProcessExitCode defaultExitCode) {
        super(logger, defaultExitCode);
        this.service = service;
    }

    /**
     * Initiates an async shutdown in a separate thread. Note that there is no
     * way to really guarantee, that the process does not exit before the call
     * completes. From a practical standpoint this is a very unlikely event.
     */
    @Override
    protected void queueShutdownInternal(Throwable fault,
                                         ProcessExitCode exitCode) {
        assert (fault != null);

        shutdownFault = fault;
        if (TestStatus.isActive()) {
            new AsyncTestShutdownThread().start();
        } else {
            new AsyncShutdownThread(exitCode).start();
        }
    }

    /**
     * Wrap it inside a FaultException, if it isn't one already.
     * <p>
     * FaultExceptions in the service originate from "forwarding" operations
     * and are just passed through.
     */
    @Override
    protected RuntimeException
        getThrowException(RuntimeException requestException) {

        if ((requestException instanceof RNUnavailableException) ||
            (requestException instanceof WrappedClientException) ||
            (requestException instanceof FaultException)) {
            return requestException;
        }

        /*
         * Pass this directly back to client. NOTE: this is not passed
         * through RequestHandler and RequestDispatcher. It's passed
         * directly via its own RMI interface (UserLoginAPI).
         */
        if (requestException instanceof SessionAccessException) {
            return requestException;
        }

        /*
         * These exceptions need to be wrapped and passed to the client
         * as-is
         */
        if (requestException instanceof KVSecurityException ||
            requestException instanceof MetadataNotFoundException) {

            return new WrappedClientException(requestException);
        }

        if (requestException instanceof ClientAccessException) {
            /*
             * This is a security exception generated by the client.
             * Unwrap it so that the client sees it in its orginal form.
             */
            return ((ClientAccessException) requestException).getCause();
        }
        if (requestException instanceof EnvironmentFailureRetryException) {

            /*
             * Map it to a RNUnavailableException so that the request can be
             * retried at a different node, while this node is (typically)
             * restarted by the SN to clear out the state
             */
            return new RNUnavailableException(requestException.getMessage());
        }

        if (requestException instanceof QueryRuntimeException) {
            return getThrowException((RuntimeException)requestException.getCause());
        }
        
        if (requestException instanceof TableVersionException) {
            /*
             * Operation caught window during a table metadata update. Retry
             * should resolve.
             */
            return new FaultException("Store is updating table schema" +
                                      " information; operation should be" +
                                      " retried", true /* isRemote */);
        }

        return new FaultException(requestException, true /* isRemote */);
    }

    /**
     * Treat a <code>FaultException</code> like an operation failure, that is,
     * don't exit the process.
     */
    @Override
    public ProcessExitCode getExitCode(RuntimeException requestException,
                                       ProcessExitCode exitCode) {

        if (requestException instanceof EnvironmentFailureException &&
            ((EnvironmentFailureException)requestException).isCorrupted()) {

            /* Do not restart if environment is corrupted */
            return ProcessExitCode.NO_RESTART;
        } else if ((requestException instanceof RNUnavailableException) ||
                   (requestException instanceof WrappedClientException) ||
                   (requestException instanceof FaultException)) {
            return null;
        }

        return exitCode ;
    }

    /**
     * Returns the fault that resulted in the shutdown.
     */
    public Throwable getShutdownFault() {
        return shutdownFault;
    }

    /**
     * Thread used for shutdown in a test environment. The run method here
     * ensures that service.stop is invoked, so that subsequent unit
     * tests can start with a clean state.
     */
    private class AsyncTestShutdownThread extends Thread {

        AsyncTestShutdownThread() {
            super();
            assert service.getUsingThreads();
            /* Don't hold up the exit. */
            setDaemon(true);
        }

        @Override
        public void run() {

            /*
             * Test environment. Make it a clean stop, so that the
             * next unit test can start with a clean state.
             */
            try {

                /*
                 * Make it a force stop, placing it in the terminal STOPPED
                 * state.
                 */
                service.stop(true);
            } catch (Exception e) {

                /*
                 * Best faith effort, we are on our way to the exits, Ignore
                 * final exceptions. Don't log, the logger may be in an unknown
                 * state.
                 */
                System.err.println("Exception during exit: " + e);
            }
        }
    }

    /**
     * Thread for shutdown in a real RN or AN environment. This thread is
     * also used in KVLite where the registry, RN, AN and admin may all be
     * configured to run in the same process. In such a configuration,
     * the KVLite process will itself exit if the RN or AN fault handling
     * requires that the RN or AN exit.
     */
    private class AsyncShutdownThread extends Thread {
        final ProcessExitCode exitCode;

        AsyncShutdownThread(ProcessExitCode exitCode) {
            super();
            this.exitCode = exitCode;
            /* Don't hold up the exit. */
            setDaemon(true);
        }

        @Override
        public void run() {
            Thread.yield();
            try {
                /*
                 * The exception should already have been logged, but print it
                 * here to stderr as a last resource in case the logging system
                 * has itself failed, say because we have run out of disk
                 * space.
                 */
                System.err.println("Process exiting due to fault");
                if (shutdownFault != null) {
                    shutdownFault.printStackTrace(System.err);
                }

                /*
                 * The service is going to exit and services will be stopped
                 * perhaps abruptly as a result.
                 */
                final ServiceStatus status =
                    ProcessExitCode.NO_RESTART.getValue() ==
                    exitCode.getValue() ?
                        ServiceStatus.ERROR_NO_RESTART :
                            ServiceStatus.ERROR_RESTARTING;
                service.update(status);

                /*
                 * Flush logs and stderr.
                 */
                LoggerUtils.closeAllHandlers();
                System.err.flush();

            } finally {
                /* Exit for sure even if there were exceptions */
                System.exit(exitCode.getValue());
            }
        }
    }
}
