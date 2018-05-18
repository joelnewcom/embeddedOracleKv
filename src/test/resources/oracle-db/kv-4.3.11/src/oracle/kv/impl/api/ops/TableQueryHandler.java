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

package oracle.kv.impl.api.ops;

import java.util.ArrayList;
import java.util.List;

import com.sleepycat.je.Transaction;

import oracle.kv.impl.api.ops.InternalOperation.OpCode;
import oracle.kv.impl.api.table.FieldValueImpl;
import oracle.kv.impl.api.table.TableMetadata;
import oracle.kv.impl.api.table.TupleValue;
import oracle.kv.impl.fault.WrappedClientException;
import oracle.kv.impl.metadata.Metadata.MetadataType;
import oracle.kv.impl.query.QueryException;
import oracle.kv.impl.query.QueryStateException;
import oracle.kv.impl.query.runtime.PlanIter;
import oracle.kv.impl.query.runtime.RuntimeControlBlock;
import oracle.kv.impl.query.runtime.server.ServerTableIter.ServerTableIterFactory;
import oracle.kv.impl.security.KVStorePrivilege;
import oracle.kv.impl.security.SystemPrivilege;
import oracle.kv.impl.topo.PartitionId;
import oracle.kv.query.ExecuteOptions;

/**
 * Server handler for {@link TableQuery}.
 */
class TableQueryHandler extends InternalOperationHandler<TableQuery> {

    TableQueryHandler(OperationHandler handler, OpCode opCode) {
        super(handler, opCode, TableQuery.class);
    }

    @Override
    List<? extends KVStorePrivilege> getRequiredPrivileges(TableQuery op) {
        /*
         * Checks the basic privilege for authentication here, and leave the
         * keyspace checking and the table access checking in
         * {@code verifyTableAccess()}.
         */
        return SystemPrivilege.usrviewPrivList;
    }

    @Override
    Result execute(TableQuery op,
                   Transaction txn,
                   PartitionId partitionId) {

        final int batchSize = op.getBatchSize();

        final List<FieldValueImpl> results =
            new ArrayList<FieldValueImpl>(batchSize);

        TableMetadata md = getMetadata();

        ExecuteOptions options = new ExecuteOptions();
        options.setResultsBatchSize(batchSize);
        options.setTraceLevel(op.getTraceLevel());

        RuntimeControlBlock rcb = new RuntimeControlBlock(
            getLogger(),
            op.getQueryPlan(),
            op.getNumIterators(),
            op.getNumRegisters(),
            md,
            options,
            op.getExternalVars(),
            op.getPrimaryResumeKey(),
            op.getSecondaryResumeKey(),
            op.getNumResultsComputed(),
            new ServerTableIterFactory(txn, partitionId, operationHandler),
            null); /* KVStoreImpl not needed and not available */

        rcb.trace("Executing query on partition " + partitionId + "\n", 1);

        executeQueryPlan(op, rcb, results, partitionId);

        rcb.trace("Produced a batch of " + results.size() +
                  " results on partition " + partitionId, 1);

        /*
         * Resume key is both input and output parameter for RCB. If set on
         * output there are more keys to be found in this iteration.
         */
        byte[] newPrimaryResumeKey = rcb.getPrimaryResumeKey();
        byte[] newSecondaryResumeKey = rcb.getSecondaryResumeKey();
        boolean more = (results.size() == batchSize &&
                        (newPrimaryResumeKey != null ||
                         newSecondaryResumeKey != null));

        return new Result.QueryResult(getOpCode(),
                                      results,
                                      op.getResultDef(),
                                      op.mayReturnNULL(),
                                      more,
                                      newPrimaryResumeKey,
                                      newSecondaryResumeKey);
    }

    /**
     * Returns a TableMetadata instance available on this node.
     */
    private TableMetadata getMetadata() {
        return (TableMetadata) getRepNode().getMetadata(MetadataType.TABLE);
    }

    private void executeQueryPlan(
        TableQuery op,
        RuntimeControlBlock rcb,
        List<FieldValueImpl> results,
        PartitionId pid) {

        final int batchSize = op.getBatchSize();
        final PlanIter queryPlan = op.getQueryPlan();

        try {
            queryPlan.open(rcb);

            while (results.size() < batchSize && queryPlan.next(rcb)) {

                FieldValueImpl res = rcb.getRegVal(queryPlan.getResultReg());
                if (res.isTuple()) {
                    res = ((TupleValue)res).toRecord();
                }

                rcb.trace("Produced result on partition" + pid + " :\n" + res,
                          1);

                results.add(res);
            }
        } catch (QueryException qe) {

            /*
             * For debugging and tracing this can temporarily use level INFO
             */
            getLogger().fine("Query execution failed: " + qe);

            /*
             * Turn this into a wrapped IllegalArgumentException so that it can
             * be passed to the client.
             */
            throw qe.getWrappedIllegalArgument();
        } catch (QueryStateException qse) {

            /*
             * This exception indicates a bug or problem in the engine. Log
h             * it. It will be thrown through to the client side.
             */
            getLogger().warning(qse.toString());

            /*
             * Wrap this exception into one that can be thrown to the client.
             */
            qse.throwClientException();

        } catch (IllegalArgumentException e) {
            throw new WrappedClientException(e);

        } finally {
            queryPlan.close(rcb);
        }
    }
}
