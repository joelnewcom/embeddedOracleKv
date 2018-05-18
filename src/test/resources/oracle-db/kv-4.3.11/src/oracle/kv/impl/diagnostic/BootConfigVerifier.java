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

package oracle.kv.impl.diagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import oracle.kv.impl.admin.param.BootstrapParams;
import oracle.kv.impl.param.Parameter;
import oracle.kv.impl.param.ParameterMap;
import oracle.kv.impl.param.ParameterState;
import oracle.kv.impl.param.SizeParameter;

/**
 * Verify the parameters of boot configuration are valid or not. It is subclass
 * of DiagnosticVerifier.
 *
 * The parameters can be categorized as four types: ports parameters,
 * host parameter, files and directories parameters and system info parameters.
 * BootConfigVerifier uses four different checkers to check the four types of
 * parameters. The four checkers are DirectoryChecker, PortChecker, HostChecker,
 * and SystemInfoChecker.
 *
 * To improve performance, the four checker run in four different threads
 * and they execute in parallel.
 */
public class BootConfigVerifier extends DiagnosticVerifier {
    private int THREADS_NUM = 4;

    private BootstrapParams parameters;

    public BootConfigVerifier(BootstrapParams parameters,
                              boolean returnOnError) {
        super(returnOnError);
        this.parameters = parameters;
    }

    @Override
    public boolean doWork() {

        ThreadPoolExecutor checkersExecutor =
                new ThreadPoolExecutor(THREADS_NUM, THREADS_NUM, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

        List<Future<String>> checkResults = new ArrayList<Future<String>>();

        /* Start to check ports */
        PortChecker portChecker = new PortChecker(parameters, returnOnError);
        checkResults.add(checkersExecutor.submit(portChecker));

        /* Start to check directories */
        DirectoryChecker directoryChecker = new DirectoryChecker
                (parameters, returnOnError);
        checkResults.add(checkersExecutor.submit(directoryChecker));

        /* Start to check hosts */
        HostChecker hostChecker = new HostChecker(parameters, returnOnError);
        checkResults.add(checkersExecutor.submit(hostChecker));

        /* Start to check system info */
        SystemInfoChecker systemInfoChecker = new SystemInfoChecker(parameters,
                returnOnError);
        checkResults.add(checkersExecutor.submit(systemInfoChecker));
        try {
            for (Future<String> fs:checkResults) {
                try {
                    String retMsg = fs.get();

                    /*
                     * some invalid parameters are found, when retMsg
                     * is not null
                     */
                    if (retMsg != null && !retMsg.isEmpty()){
                        printMessage(retMsg);

                        if (returnOnError) {
                            return false;
                        }
                    }
                } catch (InterruptedException ex) {
                    printMessage(ex.getMessage());
                    if (returnOnError) {
                        return false;
                    }
                } catch (ExecutionException ex) {
                    printMessage(ex.getMessage());
                    if (returnOnError) {
                        return false;
                    }
                }
            }
        } finally {
            /* Ensure shutdown always be called before the method returns */
            checkersExecutor.shutdown();
        }

        return true;
    }

    /**
     * Check ports in boot configuration are available or not.
     */
    private class PortChecker extends Checker {

        private final int registryPort;
        private final String HAPortRange;
        private final String servicePortRange;
        private final int trapPort;
        private final int pollPort;
        private final int capacity;

        public PortChecker(BootstrapParams parameters,
                           boolean returnImmediate) {
            super(returnImmediate);
            this.registryPort = parameters.getRegistryPort();
            this.HAPortRange = parameters.getHAPortRange();
            this.servicePortRange = parameters.getServicePortRange();
            this.trapPort = parameters.getMgmtTrapPort();
            this.pollPort = parameters.getMgmtPollingPort();
            this.capacity = parameters.getCapacity();
        }

        @Override
        public String check() throws Exception {
            String retMsg;

            PortConflictValidator validator = new PortConflictValidator();

            /* Check registry port is available or not */
            retMsg = ParametersValidator.
                    checkPort(ParameterState.COMMON_REGISTRY_PORT,
                    registryPort);
            aggregateMessage(retMsg);
            if (shouldStopCheck(retMsg)) {
                return getMessage();
            }

            /* Check registry port is already assigned or not */
            retMsg = validator.check(ParameterState.COMMON_REGISTRY_PORT,
                    registryPort);
            aggregateMessage(retMsg);
            if (shouldStopCheck(retMsg)) {
                return getMessage();
            }

            /* Check HA range ports are available or not */
            retMsg = ParametersValidator.
                    checkRangePorts(ParameterState.COMMON_PORTRANGE,
                    HAPortRange);
            aggregateMessage(retMsg);
            if (shouldStopCheck(retMsg)) {
                return getMessage();
            }

            /* Check HA range ports are already assigned or not */
            retMsg = validator.check(ParameterState.COMMON_PORTRANGE,
                    HAPortRange);
            aggregateMessage(retMsg);
            if (shouldStopCheck(retMsg)) {
                return getMessage();
            }

            /* Check capacity of SNA is greater than 0 or not */
            retMsg = ParametersValidator.
                    checkPositiveInteger(ParameterState.COMMON_CAPACITY,
                    capacity);
            aggregateMessage(retMsg);
            if (shouldStopCheck(retMsg)) {
                return getMessage();
            }

            /* Check the number of ports in a range is enough */
            retMsg = ParametersValidator.
                checkRangePortsNumber(ParameterState.
                                      COMMON_PORTRANGE,
                                      HAPortRange, capacity);
            aggregateMessage(retMsg);
            if (shouldStopCheck(retMsg)) {
                return getMessage();
            }

            if (servicePortRange != null && !servicePortRange.isEmpty()) {
                /* Check service range ports are available or not */
                retMsg = ParametersValidator.
                        checkRangePorts(ParameterState.COMMON_SERVICE_PORTRANGE,
                        servicePortRange);
                aggregateMessage(retMsg);
                if (shouldStopCheck(retMsg)) {
                    return getMessage();
                }

                /* Check service range ports are already assigned or not */
                retMsg = validator.
                        check(ParameterState.COMMON_SERVICE_PORTRANGE,
                        servicePortRange);
                aggregateMessage(retMsg);
                if (shouldStopCheck(retMsg)) {
                    return getMessage();
                }
            }

            /* Check trap port is greater than 0 or not*/
            retMsg = ParametersValidator.
                    checkPositiveInteger(ParameterState.COMMON_MGMT_TRAP_PORT,
                    trapPort);
            aggregateMessage(retMsg);
            if (shouldStopCheck(retMsg)) {
                return getMessage();
            }

            /* Check trap port is available or not */
            retMsg = ParametersValidator.
                    checkPort(ParameterState.COMMON_MGMT_TRAP_PORT,
                    trapPort);
            aggregateMessage(retMsg);
            if (shouldStopCheck(retMsg)) {
                return getMessage();
            }

            /* Check trap port is already assigned or not */
            retMsg = validator.check(ParameterState.COMMON_MGMT_TRAP_PORT,
                    trapPort);
            aggregateMessage(retMsg);
            if (shouldStopCheck(retMsg)) {
                return getMessage();
            }

            /* Check poll port is greater than 0 or not */
            retMsg = ParametersValidator.
                    checkPositiveInteger(ParameterState.COMMON_MGMT_POLL_PORT,
                    pollPort);
            aggregateMessage(retMsg);
            if (shouldStopCheck(retMsg)) {
                return getMessage();
            }

            /* Check poll port is available or not */
            retMsg = ParametersValidator.
                    checkPort(ParameterState.COMMON_MGMT_POLL_PORT,
                    pollPort);
            aggregateMessage(retMsg);
            if (shouldStopCheck(retMsg)) {
                return getMessage();
            }

            /* Check poll port is already assigned or not */
            retMsg = validator.check(ParameterState.COMMON_MGMT_POLL_PORT,
                    pollPort);
            aggregateMessage(retMsg);
            if (shouldStopCheck(retMsg)) {
                return getMessage();
            }

            return getMessage();
        }
    }

    /**
     * Check directories in boot configuration exist or not.
     */
    private class DirectoryChecker extends Checker {
        private final String rootDir;
        private final ParameterMap storageDirMap;

        public DirectoryChecker(BootstrapParams parameters,
                           boolean returnImmediate) {
            super(returnImmediate);
            this.rootDir = parameters.getRootdir();
            this.storageDirMap = parameters.getStorageDirMap();
        }

        @Override
        public String check() throws Exception {
            String retMsg;

            /* Check root directory exists or not */
            retMsg =
                   ParametersValidator.checkDirectory(ParameterState.BP_ROOTDIR,
                                                      rootDir, 0L);   // TODO ... can rootDir have size?
            aggregateMessage(retMsg);
            if (shouldStopCheck(retMsg)) {
                return getMessage();
            }

            /* Check all directories in storageDirMap exist or not */
            if (storageDirMap != null && !storageDirMap.isEmpty()) {
                for (Parameter storageDir : storageDirMap) {
                    retMsg = ParametersValidator.checkDirectory
                            (ParameterState.RN_MOUNT_POINT,
                             storageDir.getName(),
                             SizeParameter.getSize(storageDir));
                    aggregateMessage(retMsg);
                    if (shouldStopCheck(retMsg)) {
                        return getMessage();
                    }
                }
            }

            return getMessage();
        }
    }

    /**
     * Check hosts in boot configuration are reachable or not.
     */
    private class HostChecker extends Checker {
        private String host = null;
        private String HAHostname = null;
        private String trapHost = null;

        public HostChecker(BootstrapParams parameters,
                           boolean returnImmediate) {
            super(returnImmediate);
            this.host = parameters.getHostname();
            this.HAHostname = parameters.getHAHostname();
            this.trapHost = parameters.getMgmtTrapHost();
        }

        @Override
        public String check() throws Exception {
            String retMsg;

            /* Check host is reachable or not */
            retMsg = ParametersValidator.
                    checkHostname(ParameterState.COMMON_HOSTNAME, host);
            aggregateMessage(retMsg);
            if (shouldStopCheck(retMsg)) {
                return getMessage();
            }

            /* Check HA host is reachable or not */
            if (HAHostname != null && !HAHostname.isEmpty()) {
                retMsg = ParametersValidator.
                        checkHostname(ParameterState.COMMON_HA_HOSTNAME,
                        HAHostname);
                aggregateMessage(retMsg);
                if (shouldStopCheck(retMsg)) {
                    return getMessage();
                }
            }

            /* Check trap host is reachable or not */
            if (trapHost != null && trapHost.isEmpty()) {
                retMsg = ParametersValidator.
                        checkHostname(ParameterState.COMMON_MGMT_TRAP_HOST,
                        trapHost);
                aggregateMessage(retMsg);
                if (shouldStopCheck(retMsg)) {
                    return getMessage();
                }
            }

            /* Check the local host name is resolvable or not */
            retMsg = ParametersValidator.checkLocalHostname();
            aggregateMessage(retMsg);
            if (shouldStopCheck(retMsg)) {
                return getMessage();
            }

            return getMessage();
        }
    }

    /**
     * Check system info in boot configuration are valid or not.
     */
    private class SystemInfoChecker extends Checker {
        private int cpu_nums = 0;
        private int memory_mb = 0;
        private int capacity = 0;

        public SystemInfoChecker(BootstrapParams parameters,
                           boolean returnImmediate) {
            super(returnImmediate);
            this.cpu_nums = parameters.getNumCPUs();
            this.memory_mb = parameters.getMemoryMB();
            this.capacity = parameters.getCapacity();
        }

        @Override
        public String check() throws Exception {
            String retMsg;

            /* Check cpu number is greater than 0 or not */
            retMsg = ParametersValidator.
                    checkPositiveInteger(ParameterState.COMMON_NUMCPUS,
                    cpu_nums);
            aggregateMessage(retMsg);
            if (shouldStopCheck(retMsg)) {
                return getMessage();
            }

            /*
             * Check the expected number of CPU is greater than the real
             * number of CPU
             */
            retMsg = ParametersValidator.
                    checkCPUNumber(ParameterState.COMMON_NUMCPUS,
                    cpu_nums);
            aggregateMessage(retMsg);
            if (shouldStopCheck(retMsg)) {
                return getMessage();
            }

            /* Check memory size is greater than 0 or not */
            retMsg = ParametersValidator.
                    checkPositiveInteger(ParameterState.COMMON_MEMORY_MB,
                    memory_mb);
            aggregateMessage(retMsg);
            if (shouldStopCheck(retMsg)) {
                return getMessage();
            }

            /*
             * Check the expected memory size is greater than the real
             * memory size
             */
            retMsg = ParametersValidator.
                    checkMemorySize(ParameterState.COMMON_MEMORY_MB,
                    memory_mb, capacity);
            aggregateMessage(retMsg);
            if (shouldStopCheck(retMsg)) {
                return getMessage();
            }

            return getMessage();
        }
    }
}
