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

import static oracle.kv.impl.util.SerialVersion.TABLE_API_VERSION;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import oracle.kv.ReturnValueVersion.Choice;
import oracle.kv.impl.api.lob.KVLargeObjectImpl;

/**
 * The delete operation deletes the key/value pair associated with the key.
 */
public class Delete extends SingleKeyOperation {

    /**
     * Whether to return previous value/version.
     */
    private final Choice prevValChoice;

    /**
     * Table operations include the table id.  0 means no table.
     */
    private final long tableId;

    /**
     * Constructs a delete operation.
     */
    public Delete(byte[] keyBytes, Choice prevValChoice) {
        this(keyBytes, prevValChoice, 0);
    }

    /**
     * Constructs a delete operation with a table id.
     */
    public Delete(byte[] keyBytes, Choice prevValChoice, long tableId) {
        this(OpCode.DELETE, keyBytes, prevValChoice, tableId);
    }

    /**
     * For subclasses, allows passing OpCode.
     */
    Delete(OpCode opCode,
           byte[] keyBytes,
           Choice prevValChoice,
           long tableId) {
        super(opCode, keyBytes);
        this.prevValChoice = prevValChoice;
        this.tableId = tableId;
    }

    /**
     * FastExternalizable constructor.  Must call superclass constructor first
     * to read common elements.
     */
    Delete(DataInput in, short serialVersion)
        throws IOException {

        this(OpCode.DELETE, in, serialVersion);
    }

    /**
     * For subclasses, allows passing OpCode.
     */
    Delete(OpCode opCode, DataInput in, short serialVersion)
        throws IOException {

        super(opCode, in, serialVersion);
        prevValChoice = Choice.readFastExternal(in, serialVersion);
        if (serialVersion >= TABLE_API_VERSION) {

            /*
             * Read table id.  If there is no table the value is 0.
             */
            tableId = in.readLong();
        } else {
            tableId = 0;
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
        prevValChoice.writeFastExternal(out, serialVersion);
        if (serialVersion >= TABLE_API_VERSION) {

            /*
             * Write the table id.  If this is not a table operation the
             * id will be 0.
             */
            out.writeLong(tableId);
        } else if (tableId != 0) {
            throwVersionRequired(serialVersion, TABLE_API_VERSION);
        }
    }

    public Choice getReturnValueVersionChoice() {
        return prevValChoice;
    }

    /**
     * Returns the tableId, which is 0 if this is not a table operation.
     */
    @Override
    long getTableId() {
        return tableId;
    }

    @Override
    public byte[] checkLOBSuffix(byte[] lobSuffixBytes) {
        return KVLargeObjectImpl.hasLOBSuffix(getKeyBytes(), lobSuffixBytes) ?
               getKeyBytes() :
               null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Delete ");
        if (tableId != 0) {
            sb.append("Table Id ");
            sb.append(tableId);
            sb.append(" ");
        }
        sb.append(super.toString());
        return sb.toString();
    }
}
