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

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Get;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationResult;
import com.sleepycat.je.Put;
import com.sleepycat.je.Transaction;
import com.sleepycat.je.WriteOptions;

import oracle.kv.Version;
import oracle.kv.impl.api.ops.InternalOperation.OpCode;
import oracle.kv.impl.rep.migration.MigrationStreamHandle;
import oracle.kv.impl.topo.PartitionId;
import oracle.kv.impl.util.TxnUtil;
import oracle.kv.table.TimeToLive;

/**
 * Server handler for {@link PutIfVersion}.
 */
class PutIfVersionHandler extends BasicPutHandler<PutIfVersion> {

    PutIfVersionHandler(OperationHandler handler) {
        super(handler, OpCode.PUT_IF_VERSION, PutIfVersion.class);
    }

    @Override
    Result execute(PutIfVersion op, Transaction txn, PartitionId partitionId) {

        verifyDataAccess(op);

        final ReturnResultValueVersion prevVal =
            new ReturnResultValueVersion(op.getReturnValueVersionChoice());

        final VersionAndExpiration result = putIfVersion(
            txn, partitionId, op.getKeyBytes(), op.getValueBytes(),
            op.getMatchVersion(), prevVal, op.getTTL(), op.getUpdateTTL());

        return new Result.PutResult(getOpCode(),
                                    prevVal.getValueVersion(),
                                    result);
    }

    /**
     * Update a key/value pair, if the existing record has the given version.
     */
    private VersionAndExpiration putIfVersion(
        Transaction txn,
        PartitionId partitionId,
        byte[] keyBytes,
        byte[] valueBytes,
        Version matchVersion,
        ReturnResultValueVersion prevValue,
        TimeToLive ttl,
        boolean updateTTL) {

        assert (keyBytes != null) && (valueBytes != null) &&
            (matchVersion != null);

        final Database db = getRepNode().getPartitionDB(partitionId);
        final DatabaseEntry keyEntry = new DatabaseEntry(keyBytes);
        final DatabaseEntry dataEntry = valueDatabaseEntry(valueBytes);
        final Cursor cursor = db.openCursor(txn, CURSOR_DEFAULT);
        try {
            OperationResult result =
                    cursor.get(keyEntry, NO_DATA,
                               Get.SEARCH, LockMode.RMW.toReadOptions());
            if (result == null) {
                /* Not present, return null. */
                return null;
            }
            if (versionMatches(cursor, matchVersion)) {
                WriteOptions options = makeOption(ttl, updateTTL);
                /* Version matches, update and return new version. */
                result = cursor.put(null, dataEntry, Put.CURRENT, options);
                final VersionAndExpiration v =
                    new VersionAndExpiration(getVersion(cursor), result);
                MigrationStreamHandle.get().
                    addPut(keyEntry, dataEntry,
                           v.getVersion().getVLSN(),
                           result.getExpirationTime());
                return v;
            }
            /* No match, get previous value/version and return null. */
            final DatabaseEntry prevData;
            if (prevValue.getReturnChoice().needValue()) {
                prevData = new DatabaseEntry();
                result = cursor.get(keyEntry,
                                    prevData,
                                    Get.CURRENT,
                                    LockMode.RMW.toReadOptions());
            } else {
                prevData = NO_DATA;
            }
            getPrevValueVersion(cursor, prevData, prevValue, result);
            return null;
        } finally {
            TxnUtil.close(cursor);
        }
    }
}
