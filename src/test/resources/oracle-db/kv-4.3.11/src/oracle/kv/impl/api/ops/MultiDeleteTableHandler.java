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

import static oracle.kv.impl.api.ops.OperationHandler.CURSOR_READ_COMMITTED;

import java.util.Collections;
import java.util.List;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.Transaction;

import oracle.kv.Direction;
import oracle.kv.impl.api.ops.InternalOperation.OpCode;
import oracle.kv.impl.rep.migration.MigrationStreamHandle;
import oracle.kv.impl.security.KVStorePrivilege;
import oracle.kv.impl.security.TablePrivilege;
import oracle.kv.impl.topo.PartitionId;

/**
 * Server handler for {@link MultiDeleteTable}.
 */
public class MultiDeleteTableHandler
        extends MultiTableOperationHandler<MultiDeleteTable> {

    public MultiDeleteTableHandler(OperationHandler handler) {
        super(handler, OpCode.MULTI_DELETE_TABLE, MultiDeleteTable.class);
    }

    @Override
    public Result execute(MultiDeleteTable op,
                          Transaction txn,
                          PartitionId partitionId) {

        verifyTableAccess(op);

        int nDeletions = 0;
        final OperationTableInfo tableInfo = new OperationTableInfo();
        Scanner scanner = getScanner(op,
                                     tableInfo,
                                     txn,
                                     partitionId,
                                     op.getMajorPathComplete(),
                                     Direction.FORWARD,
                                     op.getResumeKey(),
                                     CURSOR_READ_COMMITTED,
                                     LockMode.READ_UNCOMMITTED_ALL,
                                     true); // set keyOnly. Handle fetch here

        DatabaseEntry keyEntry = scanner.getKey();
        DatabaseEntry dataEntry = scanner.getData();
        Cursor cursor = scanner.getCursor();
        boolean moreElements;
        try {
            while ((moreElements = scanner.next()) == true) {

                int match = keyInTargetTable(op,
                                             tableInfo,
                                             keyEntry,
                                             dataEntry,
                                             cursor);
                if (match > 0) {
                    op.setLastDeleted(keyEntry.getData());

                    /*
                     * There is no need to get the record to lock it
                     * in the delete path.  If the record is gone the
                     * delete below will fail.
                     */
                    if (cursor.delete(null) != null) {
                        int num = 1;
                        /*
                         * If this is a client-driven operation the
                         * migration stream needs to be considered.
                         */
                        if (isClientOperation(op)) {
                            MigrationStreamHandle.get().
                                addDelete(keyEntry, cursor);

                            num += tableInfo.deleteAncestorKeys(cursor,
                                                                keyEntry);
                        }
                        nDeletions += num;
                    }
                } else if (match < 0) {
                    moreElements = false;
                    break;
                }
                if (op.getBatchSize() > 0 && nDeletions >= op.getBatchSize()) {
                    break;
                }
            }
        } finally {
            scanner.close();
        }
        assert (!moreElements || op.getBatchSize() > 0);
        return new Result.MultiDeleteResult(getOpCode(), nDeletions);
    }

    /*
     * The internal table removal code will set a non-zero batchSize.
     */
    private boolean isClientOperation(MultiDeleteTable op) {
        return op.getBatchSize() == 0;
    }

    @Override
    public List<? extends KVStorePrivilege>
        tableAccessPrivileges(long tableId) {
        return Collections.singletonList(
            new TablePrivilege.DeleteTable(tableId));
    }
}
