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

import static com.sleepycat.je.Put.CURRENT;
import static com.sleepycat.je.Put.NO_OVERWRITE;
import static com.sleepycat.je.Put.OVERWRITE;
import static oracle.kv.impl.api.ops.OperationHandler.CURSOR_DEFAULT;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Get;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationResult;
import com.sleepycat.je.Transaction;

import oracle.kv.impl.api.ops.InternalOperation.OpCode;
import oracle.kv.impl.rep.migration.MigrationStreamHandle;
import oracle.kv.impl.topo.PartitionId;
import oracle.kv.impl.util.TxnUtil;
import oracle.kv.table.TimeToLive;

/**
 * Server handler for {@link Put}.
 */
class PutHandler extends BasicPutHandler<Put> {

    PutHandler(OperationHandler handler) {
        super(handler, OpCode.PUT, Put.class);
    }

    @Override
    Result execute(Put op, Transaction txn, PartitionId partitionId) {

        verifyDataAccess(op);

        final ReturnResultValueVersion prevVal =
            new ReturnResultValueVersion(op.getReturnValueVersionChoice());

        final VersionAndExpiration result = put(
            txn, partitionId, op.getKeyBytes(), op.getValueBytes(), prevVal,
            op.getTTL(), op.getUpdateTTL());
        return new Result.PutResult(getOpCode(),
                                    prevVal.getValueVersion(),
                                    result);
    }

    /**
     * Put a key/value pair. If the key exists, the associated value is
     * overwritten.
     */
    private VersionAndExpiration put(Transaction txn,
                                     PartitionId partitionId,
                                     byte[] keyBytes,
                                     byte[] valueBytes,
                                     ReturnResultValueVersion prevValue,
                                     TimeToLive ttl,
                                     boolean updateTTL) {

        assert (keyBytes != null) && (valueBytes != null);
        final Database db = getRepNode().getPartitionDB(partitionId);
        final DatabaseEntry keyEntry = new DatabaseEntry(keyBytes);
        final DatabaseEntry dataEntry = valueDatabaseEntry(valueBytes);

        final com.sleepycat.je.WriteOptions jeOptions =
                makeOption(ttl, updateTTL);
        /* Simple case: previous version and value are not returned. */
        if (!prevValue.getReturnChoice().needValueOrVersion()) {
            final Cursor cursor = db.openCursor(txn, CURSOR_DEFAULT);
            try {
                final OperationResult result = cursor.put(keyEntry, dataEntry,
                        OVERWRITE, jeOptions);
                final VersionAndExpiration v =
                    new VersionAndExpiration(getVersion(cursor), result);
                MigrationStreamHandle.get().addPut(keyEntry,
                                                   dataEntry,
                                                   v.getVersion().getVLSN(),
                                                   result.getExpirationTime());
                return v;
            } finally {
                TxnUtil.close(cursor);
            }
        }

        /*
         * To return previous value/version, we have to either position on the
         * existing record and update it, or insert without overwriting.
         */
        final Cursor cursor = db.openCursor(txn, CURSOR_DEFAULT);
        try {
            while (true) {
                OperationResult result = cursor.put(keyEntry, dataEntry,
                        NO_OVERWRITE, jeOptions);
                if (result != null) {
                    final VersionAndExpiration v =
                        new VersionAndExpiration(getVersion(cursor), result);
                    MigrationStreamHandle.get().
                        addPut(keyEntry, dataEntry,
                               v.getVersion().getVLSN(),
                               result.getExpirationTime());
                    return v;
                }
                final DatabaseEntry prevData =
                    prevValue.getReturnChoice().needValue() ?
                    new DatabaseEntry() :
                    NO_DATA;
                result = cursor.get(keyEntry, prevData,
                                    Get.SEARCH, LockMode.RMW.toReadOptions());
                if (result != null) {
                    getPrevValueVersion(cursor, prevData, prevValue, result);
                    result = cursor.put(null, dataEntry, CURRENT, jeOptions);
                    final VersionAndExpiration v =
                        new VersionAndExpiration(getVersion(cursor), result);
                    MigrationStreamHandle.get().
                        addPut(keyEntry, dataEntry,
                               v.getVersion().getVLSN(),
                               result.getExpirationTime());
                    return v;
                }
                /* Another thread deleted the record.  Continue. */
            }
        } finally {
            TxnUtil.close(cursor);
        }
    }
}
