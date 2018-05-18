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

import oracle.kv.Version;
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
 * Server handler for {@link DeleteIfVersion}.
 */
class DeleteIfVersionHandler extends BasicDeleteHandler<DeleteIfVersion> {

    DeleteIfVersionHandler(OperationHandler handler) {
        super(handler, OpCode.DELETE_IF_VERSION, DeleteIfVersion.class);
    }

    @Override
    Result execute(DeleteIfVersion op,
                   Transaction txn,
                   PartitionId partitionId) {

        verifyDataAccess(op);

        final ReturnResultValueVersion prevVal =
            new ReturnResultValueVersion(op.getReturnValueVersionChoice());

        final boolean result = deleteIfVersion(
            txn, partitionId, op.getKeyBytes(), op.getMatchVersion(), prevVal);

        return new Result.DeleteResult(getOpCode(), prevVal.getValueVersion(),
                                       result);
    }

    /**
     * Delete a key/value pair, if the existing record has the given version.
     */
    private boolean deleteIfVersion(Transaction txn,
                                    PartitionId partitionId,
                                    byte[] keyBytes,
                                    Version matchVersion,
                                    ReturnResultValueVersion prevValue) {

        assert (keyBytes != null) && (matchVersion != null);

        final Database db = getRepNode().getPartitionDB(partitionId);
        final DatabaseEntry keyEntry = new DatabaseEntry(keyBytes);
        final Cursor cursor = db.openCursor(txn, CURSOR_DEFAULT);
        try {
            OperationResult result =
                cursor.get(keyEntry, NO_DATA,
                           Get.SEARCH, LockMode.RMW.toReadOptions());
            if (result == null) {
                /* Not present, return false. */
                return false;
            }
            if (versionMatches(cursor, matchVersion)) {
                /* Version matches, delete and return true. */
                cursor.delete(null);
                MigrationStreamHandle.get().addDelete(keyEntry, cursor);
                return true;
            }
            /* No match, get previous value/version and return false. */
            final DatabaseEntry prevData;
            if (prevValue.getReturnChoice().needValue()) {
                prevData = new DatabaseEntry();
                result = cursor.get(keyEntry, prevData,
                                    Get.CURRENT, LockMode.RMW.toReadOptions());
            } else {
                prevData = NO_DATA;
            }
            getPrevValueVersion(cursor, prevData, prevValue, result);
            return false;
        } finally {
            TxnUtil.close(cursor);
        }
    }
}
