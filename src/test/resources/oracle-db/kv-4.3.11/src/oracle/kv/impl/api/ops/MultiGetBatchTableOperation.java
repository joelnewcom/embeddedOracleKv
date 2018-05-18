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

import java.io.IOException;
import java.io.DataInput;
import java.io.DataOutput;
import java.util.ArrayList;
import java.util.List;

import oracle.kv.KeyRange;
import oracle.kv.impl.api.table.TargetTables;

/**
 * This is an intermediate class for a multi-batch-get table operation.
 */
abstract class MultiGetBatchTableOperation extends MultiGetTableOperation {

    private final List<byte[]> keys;
    private final int batchSize;
    private final byte[] resumeKey;

    public MultiGetBatchTableOperation(OpCode opCode,
                                       List<byte[]> parentKeys,
                                       byte[] resumeKey,
                                       TargetTables targetTables,
                                       KeyRange subRange,
                                       int batchSize) {

        super(opCode, parentKeys.get(0), targetTables, subRange);
        this.keys = parentKeys;
        this.resumeKey= resumeKey;
        this.batchSize = batchSize;
    }

    /**
     * FastExternalizable constructor.  Must call superclass constructor first
     * to read common elements.
     */
    protected MultiGetBatchTableOperation(OpCode opCode,
                                          DataInput in,
                                          short serialVersion)
        throws IOException {

        super(opCode, in, serialVersion);
        int nkeys = in.readShort();
        if (nkeys > 0) {
            keys = new ArrayList<byte[]>(nkeys);
            for (int i = 0; i < nkeys; i++) {
                int len = in.readShort();
                byte[] key = new byte[len];
                in.readFully(key);
                keys.add(key);
            }
        } else {
            keys = null;
        }
        int len = in.readShort();
        if (len > 0) {
            resumeKey = new byte[len];
            in.readFully(resumeKey);
        } else {
            resumeKey = null;
        }
        batchSize = in.readInt();
    }

    List<byte[]> getParentKeys() {
        return keys;
    }

    int getBatchSize() {
        return batchSize;
    }

    byte[] getResumeKey() {
        return resumeKey;
    }

    /**
     * FastExternalizable writer.  Must call superclass method first to write
     * common elements.
     */
    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
        throws IOException {

        super.writeFastExternal(out, serialVersion);
        if (keys != null) {
            out.writeShort(keys.size());
            for (byte[] key: keys) {
                out.writeShort(key.length);
                out.write(key);
            }
        } else {
            out.writeShort(-1);
        }
        if (resumeKey != null) {
            out.writeShort(resumeKey.length);
            out.write(resumeKey);
        } else {
            out.writeShort(-1);
        }
        out.writeInt(batchSize);
    }
}
