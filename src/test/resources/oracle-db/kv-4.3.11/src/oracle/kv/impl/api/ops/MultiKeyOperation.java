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
import oracle.kv.KeyRange;
import oracle.kv.impl.util.UserDataControl;

/**
 * A multi-key operation has a parent key, optional KeyRange and depth.
 */
abstract class MultiKeyOperation extends InternalOperation {

    /**
     * The parent key, or null.
     */
    private final byte[] parentKey;

    /**
     * Sub-key range of traversal, or null.
     */
    private final KeyRange subRange;

    /**
     * Depth of traversal, always non-null.
     */
    private final Depth depth;

    /**
     * Constructs a multi-key operation.
     *
     * For subclasses, allows passing OpCode.
     */
    MultiKeyOperation(OpCode opCode,
                      byte[] parentKey,
                      KeyRange subRange,
                      Depth depth) {
        super(opCode);
        this.parentKey = parentKey;
        this.subRange = subRange;
        this.depth = depth;
    }

    /**
     * FastExternalizable constructor.  Must call superclass constructor first
     * to read common elements.
     *
     * For subclasses, allows passing OpCode.
     */
    MultiKeyOperation(OpCode opCode, DataInput in, short serialVersion)
        throws IOException {

        super(opCode, in, serialVersion);

        final int keyLen = in.readShort();
        if (keyLen < 0) {
            parentKey = null;
        } else {
            parentKey = new byte[keyLen];
            in.readFully(parentKey);
        }

        if (in.readByte() == 0) {
            subRange = null;
        } else {
            subRange = new KeyRange(in, serialVersion);
        }

        final int depthOrdinal = in.readByte();
        if (depthOrdinal == -1) {
            depth = null;
        } else {
            depth = Depth.valueOf(depthOrdinal);
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

        if (parentKey == null) {
            out.writeShort(-1);
        } else {
            out.writeShort(parentKey.length);
            out.write(parentKey);
        }

        if (subRange == null) {
            out.write(0);
        } else {
            out.write(1);
            subRange.writeFastExternal(out, serialVersion);
        }

        if (depth == null) {
            out.writeByte(-1);
        } else {
            out.writeByte(depth.ordinal());
        }
    }

    byte[] getParentKey() {
        return parentKey;
    }

    KeyRange getSubRange() {
        return subRange;
    }

    Depth getDepth() {
        return depth;
    }

    @Override
    public String toString() {
        return super.toString() +
            " parentKey: " + UserDataControl.displayKey(parentKey) +
            " subRange: " + UserDataControl.displayKeyRange(subRange) +
            " depth: " + depth;
    }
}
