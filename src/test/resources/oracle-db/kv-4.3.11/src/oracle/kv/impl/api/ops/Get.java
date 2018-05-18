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

/**
 * A get operation gets a value from the KV Store.
 */
public class Get extends SingleKeyOperation {

    /**
     * Table operations include the table id.  0 means no table.
     */
    private final long tableId;

    /**
     * Construct a get operation.
     */
    public Get(byte[] keyBytes) {
        this(keyBytes, 0);
    }

    /**
     * Construct a get operation with a table id.
     */
    public Get(byte[] keyBytes,
               long tableId) {
        super(OpCode.GET, keyBytes);
        this.tableId = tableId;
    }

    /**
     * Returns the tableId, which is 0 if this is not a table operation.
     */
    @Override
    long getTableId() {
        return tableId;
    }

    /**
     * FastExternalizable constructor.  Must call superclass constructor first
     * to read common elements.
     */
    Get(DataInput in, short serialVersion)
        throws IOException {

        super(OpCode.GET, in, serialVersion);
        if (serialVersion >= TABLE_API_VERSION) {

            /*
             * Read table id.
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        if (tableId != 0) {
            sb.append(" Table Id ");
            sb.append(tableId);
        }
        return sb.toString();
    }
}
