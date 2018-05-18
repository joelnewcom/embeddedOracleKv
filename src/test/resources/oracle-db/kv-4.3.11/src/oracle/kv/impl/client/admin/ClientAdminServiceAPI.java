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
package oracle.kv.impl.client.admin;

import java.io.IOException;
import java.net.URI;
import java.rmi.RemoteException;

import oracle.kv.impl.admin.DdlResultsReport;
import oracle.kv.impl.security.AuthContext;
import oracle.kv.impl.security.ContextProxy;
import oracle.kv.impl.security.login.LoginHandle;
import oracle.kv.impl.test.ExceptionTestHook;
import oracle.kv.impl.test.ExceptionTestHookExecute;
import oracle.kv.impl.util.JsonUtils;
import oracle.kv.impl.util.SerialVersion;
import oracle.kv.impl.util.registry.RemoteAPI;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;

/**
 * Defines the RMI interface used by the kvclient to asynchronously submit
 * DDL statements, which will be executed by the Admin service.
 */
public class ClientAdminServiceAPI extends RemoteAPI {

    /** 
     * StatementResult.getResult() added in V7. Output that was previously
     * sent to getInfo() is now diverted to getResult() if the statement
     * is a show or a describe and has a result, rather than just execution
     * status.
     */
    public static final short STATEMENT_RESULT_VERSION = SerialVersion.V7;

    private static final AuthContext NULL_CTX = null;

    private final ClientAdminService proxyRemote;

    /* For testing only, to mimic network problems */
    public static ExceptionTestHook<String, RemoteException> REMOTE_FAULT_HOOK;

    private ClientAdminServiceAPI(ClientAdminService remote,
                                  LoginHandle loginHdl)
        throws RemoteException {

        super(remote);
        this.proxyRemote =
            ContextProxy.create(remote, loginHdl, getSerialVersion());
    }

    public static ClientAdminServiceAPI wrap(ClientAdminService remote,
                                             LoginHandle loginHdl)
        throws RemoteException {

        return new ClientAdminServiceAPI(remote, loginHdl);
    }

    /**
     * Submit a DDL statement for asynchronous execution and return status
     * about the corresponding plan.
     *
     * @param statement - a DDL statement
     * @return information about the current execution state of the plan
     * @throws RemoteException
     */
    public ExecutionInfo execute(String statement)
        throws RemoteException {

        assert ExceptionTestHookExecute.doHookIfSet(REMOTE_FAULT_HOOK,
                                                    "execute");
        ExecutionInfo execInfo = 
            proxyRemote.execute(statement, NULL_CTX, getSerialVersion());
        return convertInfo(execInfo);
    }

    /**
     * Get current status for the specified plan
     * @param planId
     * @return detailed plan status
     */
    public ExecutionInfo getExecutionStatus(int planId)
        throws RemoteException {

        assert ExceptionTestHookExecute.doHookIfSet(REMOTE_FAULT_HOOK,
                                                    "getExecutionStatus");
        /* 
         * Note that the STATEMENT_RESULT_VERSION conversion does not need to
         * happen for plan statements, and this entry point only applies to
         * plan statements.
         */
        return proxyRemote.getExecutionStatus(planId, NULL_CTX,
                                              getSerialVersion());
    }

    /**
     * Return true if this Admin can handle DDL operations. That currently
     * equates to whether the Admin is a master or not.
     */
    public boolean canHandleDDL() throws RemoteException {

        return proxyRemote.canHandleDDL(NULL_CTX, getSerialVersion());
    }

    /**
     * Return the address of the master Admin. If this Admin doesn't know that,
     * return null.
     *
     * @throws RemoteException
     */
    public URI getMasterRmiAddress() throws RemoteException {
        return proxyRemote.getMasterRmiAddress(NULL_CTX, getSerialVersion());
    }

    /**
     * Initiate a plan cancellation.
     */
    public ExecutionInfo interruptAndCancel(int planId) throws RemoteException {

        assert ExceptionTestHookExecute.doHookIfSet(REMOTE_FAULT_HOOK,
                                                    "interruptAndCancel");
        /* 
         * Note that the STATEMENT_RESULT_VERSION conversion does not need to
         * happen for plan statements, and this entry point only applies to
         * plan statements.
         */
        return proxyRemote.interruptAndCancel(planId,
                                              NULL_CTX,
                                              getSerialVersion());
    }

    /**
     * Manage any protocol upgrades needed to the ExecutionInfo class 
     */
    private ExecutionInfo convertInfo(ExecutionInfo execInfo) {

        if (getSerialVersion() < STATEMENT_RESULT_VERSION) {

            /*
             * In versions of the server before STATEMENT_RESULT_VERSION, the
             * results for the show or describe statements were incorrectly put
             * within the info/infoAsJson fields instead of the result
             * field. Since this is a new client talking to an old server, copy
             * the infoAsJson value over the the result field if the statement
             * was a show or describe. This will make the show tables statement
             * have a JSON result instead of a text result, but this is such an
             * infrequent case that it is not worth dealing with.
             */
            String jsonInfo = execInfo.getJSONInfo();
            final JsonNode json;
            boolean showResult = false;
            try {
                @SuppressWarnings("deprecation")
                final JsonParser parser =
                    JsonUtils.createJsonParser
                    (new java.io.StringBufferInputStream(jsonInfo));
                json = parser.readValueAsTree();
                String type = JsonUtils.getAsText(json, "type");
                if ((type != null) && 
                    (type.equals("show") || type.equals("describe"))) {
                    showResult = true;
                }
            } catch (IOException ignore) {
                /* 
                 * The json isn't valid, so err on the side of showing too 
                 * much information.
                 */
                showResult = true;
            }
            
            if (showResult) {
                return new ExecutionInfoImpl
                        (execInfo.getPlanId(),
                         execInfo.isTerminated(),
                         DdlResultsReport.STATEMENT_COMPLETED,
                         DdlResultsReport.STATEMENT_COMPLETED_JSON,
                         execInfo.isSuccess(),
                         execInfo.isCancelled(),
                         execInfo.getErrorMessage(),
                         execInfo.needsTermination(),
                         execInfo.getJSONInfo()); // result
            }
        }
    
        /* Nothing to do */
        return execInfo;
    }
}
