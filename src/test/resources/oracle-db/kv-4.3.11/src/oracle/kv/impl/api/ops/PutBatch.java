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

import static oracle.kv.impl.util.SerialVersion.TTL_SERIAL_VERSION;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import oracle.kv.impl.api.bulk.BulkPut.KVPair;

/**
 * A put-batch operation.
 */
public class PutBatch extends MultiKeyOperation {

    private final List<KVPair> kvPairs;
    private final long[] tableIds;

    public PutBatch(List<KVPair> le, long[] tableIds) {
        super(OpCode.PUT_BATCH, null, null, null);
        this.kvPairs = le;
        this.tableIds = tableIds;
    }

    PutBatch(DataInput in, short serialVersion) throws IOException {

        super(OpCode.PUT_BATCH, in, serialVersion);
        final int kvPairCount = in.readInt();

        kvPairs = new ArrayList<KVPair>(kvPairCount);

        for (int i = 0; i < kvPairCount; i++) {

            final int keySize = in.readInt();

            final byte[] key = new byte[keySize];
            in.readFully(key);

            final int valueSize = in.readInt();

            final byte[] value = new byte[valueSize];
            in.readFully(value);

            final KVPair kv;
            if (serialVersion >= TTL_SERIAL_VERSION) {
                final int ttlVal = in.readInt();
                final byte ttlUnitOrdinal = (ttlVal != 0) ? in.readByte() : 0;
                kv = new KVPair(key, value, ttlVal, ttlUnitOrdinal);
            } else {
                kv = new KVPair(key, value);
            }
            kvPairs.add(kv);
        }

        final int tableIdCount = in.readInt();
        if (tableIdCount == -1) {
            tableIds = null;
        } else {
            tableIds = new long[tableIdCount];
            for (int i = 0; i < tableIdCount; i++) {
                tableIds[i] = in.readLong();
            }
        }
    }

    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
        throws IOException {

        super.writeFastExternal(out, serialVersion);

        out.writeInt(kvPairs.size());

        for (KVPair e : kvPairs) {

            final byte[] key = e.getKey();

            out.writeInt(key.length);
            out.write(key);

            final byte[] value = e.getValue();
            out.writeInt(value.length);
            out.write(value);

            int ttlVal = e.getTTLVal();
            if (serialVersion >= TTL_SERIAL_VERSION) {
                out.writeInt(ttlVal);
                if (ttlVal != 0) {
                    out.writeByte(e.getTTLUnitOrdinal());
                }
            } else if (ttlVal != 0) {
                /*
                 * Throw an exception so that TTL information is not
                 * transparently dropped when writing to older servers.
                 */
                throwVersionRequired(serialVersion, TTL_SERIAL_VERSION);
            }
        }

        if (tableIds != null) {
            out.writeInt(tableIds.length);
            for (Long tableId : tableIds) {
                out.writeLong(tableId);
            }
        } else {
            out.writeInt(-1);
        }
    }

    List<KVPair> getKvPairs() {
        return kvPairs;
    }

    long[] getTableIds() {
        return tableIds;
    }
}
