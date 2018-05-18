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

import static com.sleepycat.je.Get.SEARCH;
import static oracle.kv.impl.api.ops.OperationHandler.CURSOR_DEFAULT;

import java.util.Collections;
import java.util.List;

import oracle.kv.impl.api.ops.InternalOperation.OpCode;
import oracle.kv.impl.security.KVStorePrivilege;
import oracle.kv.impl.security.SystemPrivilege;
import oracle.kv.impl.security.TablePrivilege;
import oracle.kv.impl.topo.PartitionId;
import oracle.kv.impl.util.TxnUtil;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationResult;
import com.sleepycat.je.Transaction;

/**
 * Server handler for {@link Get}.
 */
class GetHandler extends SingleKeyOperationHandler<Get> {

    GetHandler(OperationHandler operationHandler) {
        super(operationHandler, OpCode.GET, Get.class);
    }

    @Override
    Result execute(Get op, Transaction txn, PartitionId partitionId) {

        verifyDataAccess(op);

        final ResultValueVersion resultValueVersion =
            get(txn, partitionId, op.getKeyBytes());

        return new Result.GetResult(getOpCode(), resultValueVersion);
    }

    /**
     * Gets the value associated with the key.
     */
    private ResultValueVersion get(Transaction txn,
                                   PartitionId partitionId,
                                   byte[] keyBytes) {

        assert (keyBytes != null);

        final Database db = getRepNode().getPartitionDB(partitionId);
        final DatabaseEntry dataEntry = new DatabaseEntry();
        final DatabaseEntry keyEntry = new DatabaseEntry(keyBytes);
        final Cursor cursor = db.openCursor(txn, CURSOR_DEFAULT);
        try {
            final OperationResult result =
                cursor.get(keyEntry, dataEntry,
                           SEARCH, LockMode.DEFAULT.toReadOptions());
            if (result == null) {
                return null;
            }
            return makeValueVersion(cursor, dataEntry, result);

        } finally {
            TxnUtil.close(cursor);
        }
    }

    @Override
    List<? extends KVStorePrivilege> schemaAccessPrivileges() {
        return SystemPrivilege.schemaReadPrivList;
    }

    @Override
    List<? extends KVStorePrivilege> generalAccessPrivileges() {
        return SystemPrivilege.readOnlyPrivList;
    }

    @Override
    public List<? extends KVStorePrivilege>
        tableAccessPrivileges(long tableId) {
        return Collections.singletonList(
            new TablePrivilege.ReadTable(tableId));
    }
}
