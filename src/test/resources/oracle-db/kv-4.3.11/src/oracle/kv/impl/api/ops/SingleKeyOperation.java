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

import oracle.kv.impl.util.UserDataControl;

/**
 * An operation that applies to a single key, from which the partition is
 * derived.
 */
public abstract class SingleKeyOperation extends InternalOperation {

    /**
     * The key.
     */
    private final byte[] keyBytes;

    /**
     * Construct an operation with a single key.
     */
    public SingleKeyOperation(OpCode opCode, byte[] keyBytes) {
        super(opCode);
        this.keyBytes = keyBytes;
    }

    /**
     * FastExternalizable constructor.  Must call superclass constructor first
     * to read common elements.
     */
    SingleKeyOperation(OpCode opCode, DataInput in, short serialVersion)
        throws IOException {

        super(opCode, in, serialVersion);
        final int keyLen = in.readShort();
        keyBytes = new byte[keyLen];
        in.readFully(keyBytes);
    }

    /**
     * FastExternalizable writer.  Must call superclass method first to write
     * common elements.
     */
    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
        throws IOException {

        super.writeFastExternal(out, serialVersion);
        out.writeShort(keyBytes.length);
        out.write(keyBytes);
    }

    /**
     * Returns the byte array of the Key associated with the operation.
     */
    public byte[] getKeyBytes() {
        return keyBytes;
    }

    /**
     * Returns the tableId, which is 0 if this is not a table operation.
     */
    abstract long getTableId();

    @Override
    public String toString() {
        return super.toString() + " Key: " +
               UserDataControl.displayKey(keyBytes);
    }
}
