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

import static oracle.kv.impl.api.ops.OperationHandler.CURSOR_DEFAULT;

import oracle.kv.impl.api.ops.InternalOperation.OpCode;
import oracle.kv.impl.rep.migration.MigrationStreamHandle;
import oracle.kv.impl.topo.PartitionId;
import oracle.kv.impl.util.TxnUtil;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Get;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationResult;
import com.sleepycat.je.Transaction;

/**
 * Server handler for {@link Delete}.
 */
class DeleteHandler extends BasicDeleteHandler<Delete> {

    DeleteHandler(OperationHandler handler) {
        super(handler, OpCode.DELETE, Delete.class);
    }

    @Override
    Result execute(Delete op, Transaction txn, PartitionId partitionId) {
        verifyDataAccess(op);

        final ReturnResultValueVersion prevVal =
            new ReturnResultValueVersion(op.getReturnValueVersionChoice());

        final boolean result =
            delete(txn, partitionId, op.getKeyBytes(), prevVal);

        return new Result.DeleteResult(getOpCode(), prevVal.getValueVersion(),
                                       result);
    }

    /**
     * Delete the key/value pair associated with the key.
     */
    private boolean delete(Transaction txn,
                           PartitionId partitionId,
                           byte[] keyBytes,
                           ReturnResultValueVersion prevValue) {

        assert (keyBytes != null);

        final Database db = getRepNode().getPartitionDB(partitionId);
        final DatabaseEntry keyEntry = new DatabaseEntry(keyBytes);

        /* Simple case: previous version and value are not returned. */
        if (!prevValue.getReturnChoice().needValueOrVersion()) {

            final OperationResult result = db.delete(txn, keyEntry, null);
            if (result != null) {
                MigrationStreamHandle.get().addDelete(keyEntry, null);
            }
            return (result != null);
        }

        /*
         * To return previous value/version, we must first position on the
         * existing record and then delete it.
         */
        final Cursor cursor = db.openCursor(txn, CURSOR_DEFAULT);
        try {
            final DatabaseEntry prevData =
                prevValue.getReturnChoice().needValue() ?
                new DatabaseEntry() :
                NO_DATA;
            final OperationResult result =
                cursor.get(keyEntry, prevData,
                           Get.SEARCH, LockMode.RMW.toReadOptions());
            if (result == null) {
                return false;
            }
            getPrevValueVersion(cursor, prevData, prevValue, result);
            cursor.delete(null);
            MigrationStreamHandle.get().addDelete(keyEntry, cursor);
            return true;
        } finally {
            TxnUtil.close(cursor);
        }
    }
}
