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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import oracle.kv.Direction;
import oracle.kv.KeyRange;
import oracle.kv.impl.api.StoreIteratorParams;
import oracle.kv.impl.api.table.TargetTables;

/**
 * This is an intermediate class for a table iteration where the records
 * may or may not reside on the same partition.
 */
abstract class TableIterateOperation extends MultiTableOperation {

    private final boolean majorComplete;
    private final Direction direction;
    private final int batchSize;
    private final byte[] resumeKey;

    protected TableIterateOperation(OpCode opCode,
                                    StoreIteratorParams sip,
                                    TargetTables targetTables,
                                    boolean majorComplete,
                                    byte[] resumeKey) {
        super(opCode, sip.getParentKeyBytes(), targetTables, sip.getSubRange());
        this.majorComplete = majorComplete;
        this.direction = sip.getPartitionDirection();
        this.batchSize = sip.getBatchSize();
        this.resumeKey = resumeKey;
    }

    /*
     * Internal use constructor that avoids StoreIteratorParams
     * construction.
     */
    protected TableIterateOperation(OpCode opCode,
                                    byte[] parentKeyBytes,
                                    TargetTables targetTables,
                                    Direction direction,
                                    KeyRange range,
                                    boolean majorComplete,
                                    int batchSize,
                                    byte[] resumeKey) {
        super(opCode, parentKeyBytes, targetTables, range);
        this.majorComplete = majorComplete;
        this.direction = direction;
        this.batchSize = batchSize;
        this.resumeKey = resumeKey;
    }

    /**
     * FastExternalizable constructor.  Must call superclass constructor first
     * to read common elements.
     */
    TableIterateOperation(OpCode opCode, DataInput in, short serialVersion)
        throws IOException {

        super(opCode, in, serialVersion);
        majorComplete = in.readBoolean();
        direction = Direction.readFastExternal(in, serialVersion);

        /*
         * When doing a scan that includes the parent key the parent is handled
         * separately from the descendants. The parent key does not make a
         * valid resume key, so if the batch size is 1, increase it to ensure
         * that the parent key is not the resume key. This is mostly not a
         * problem for table scans, but it does not hurt.
         */
        int tmpBatchSize = in.readInt();
        if (getResumeKey() == null && tmpBatchSize == 1) {
            batchSize = 2;
        } else {
            batchSize = tmpBatchSize;
        }

        final int keyLen = in.readShort();
        if (keyLen < 0) {
            resumeKey = null;
        } else {
            resumeKey = new byte[keyLen];
            in.readFully(resumeKey);
        }
    }
    /**
     * FastExternalizable writer.  Must call superclass method first to write
     * common elements.
     */
    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
        throws IOException {

        super.writeFastExternal(out, serialVersion);

        out.writeBoolean(majorComplete);
        direction.writeFastExternal(out, serialVersion);
        out.writeInt(batchSize);

        if (resumeKey == null) {
            out.writeShort(-1);
        } else {
            out.writeShort(resumeKey.length);
            out.write(resumeKey);
        }
    }

    Direction getDirection() {
        return direction;
    }

    int getBatchSize() {
        return batchSize;
    }

    byte[] getResumeKey() {
        return resumeKey;
    }

    boolean getMajorComplete() {
        return majorComplete;
    }
}
