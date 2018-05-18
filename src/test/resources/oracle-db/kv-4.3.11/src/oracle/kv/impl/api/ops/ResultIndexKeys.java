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

import oracle.kv.impl.util.FastExternalizable;

/**
 * Holds results of an index key iteration over a table.  This result includes
 * primary key and index key byte arrays.  This is all of the information that
 * is available in a single secondary scan without doing an additional database
 * read of the primary data.
 */
public class ResultIndexKeys implements FastExternalizable {

    private final byte[] primaryKeyBytes;
    private final byte[] indexKeyBytes;
    private final long expirationTime;

    public ResultIndexKeys(byte[] primaryKeyBytes,
                           byte[] indexKeyBytes,
                           long expirationTime) {
        this.primaryKeyBytes = primaryKeyBytes;
        this.indexKeyBytes = indexKeyBytes;
        this.expirationTime = expirationTime;
    }

    /**
     * FastExternalizable constructor.  Must call superclass constructor
     * first to read common elements.
     */
    public ResultIndexKeys(DataInput in, short serialVersion)
        throws IOException {

        int keyLen = in.readShort();
        primaryKeyBytes = new byte[keyLen];
        in.readFully(primaryKeyBytes);
        keyLen = in.readShort();
        indexKeyBytes = new byte[keyLen];
        in.readFully(indexKeyBytes);
        expirationTime = Result.readExpirationTime(in, serialVersion);
    }

    /**
     * FastExternalizable writer.  Must call superclass method first to
     * write common elements.
     */
    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
        throws IOException {

        out.writeShort(primaryKeyBytes.length);
        out.write(primaryKeyBytes);
        out.writeShort(indexKeyBytes.length);
        out.write(indexKeyBytes);
        Result.writeExpirationTime(out, expirationTime, serialVersion);
    }

    public byte[] getPrimaryKeyBytes() {
        return primaryKeyBytes;
    }

    public byte[] getIndexKeyBytes() {
        return indexKeyBytes;
    }

    public long getExpirationTime() {
        return expirationTime;
    }
}
