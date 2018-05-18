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

import oracle.kv.Depth;
import oracle.kv.Direction;
import oracle.kv.KeyRange;

/**
 * A multi-key iterate operation.
 */
abstract public class MultiKeyIterate extends MultiKeyOperation {

    private final Direction direction;
    private final int batchSize;
    private final byte[] resumeKey;

    /**
     * Construct a multi-key iterate operation.
     */
    MultiKeyIterate(OpCode opCode,
                    byte[] parentKey,
                    KeyRange subRange,
                    Depth depth,
                    Direction direction,
                    int batchSize,
                    byte[] resumeKey) {
        super(opCode, parentKey, subRange, depth);
        this.direction = direction;
        this.batchSize = batchSize;
        this.resumeKey = resumeKey;
    }

    /**
     * FastExternalizable constructor.  Must call superclass constructor first
     * to read common elements.
     */
    MultiKeyIterate(OpCode opCode, DataInput in, short serialVersion)
        throws IOException {

        super(opCode, in, serialVersion);

        direction = Direction.readFastExternal(in, serialVersion);
        batchSize = in.readInt();

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
}
