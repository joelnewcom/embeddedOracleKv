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

import oracle.kv.Value;
import oracle.kv.Version;
import oracle.kv.impl.util.FastExternalizable;

/**
 * Holds key and value as byte arrays to avoid conversion to Key and Value
 * objects on the service side.
 */
public class ResultKeyValueVersion implements FastExternalizable {

    private final byte[] keyBytes;
    private final ResultValue resultValue;
    private final Version version;
    private final long expirationTime;

    public ResultKeyValueVersion(byte[] keyBytes,
                                 byte[] valueBytes,
                                 Version version,
                                 long expirationTime) {
        this.keyBytes = keyBytes;
        this.resultValue = new ResultValue(valueBytes);
        this.version = version;
        this.expirationTime = expirationTime;
    }

    /**
     * FastExternalizable constructor.  Must call superclass constructor
     * first to read common elements.
     */
    public ResultKeyValueVersion(DataInput in, short serialVersion)
        throws IOException {

        final int keyLen = in.readShort();
        keyBytes = new byte[keyLen];
        in.readFully(keyBytes);
        resultValue = new ResultValue(in, serialVersion);
        version = Version.createVersion(in, serialVersion);
        expirationTime = Result.readExpirationTime(in, serialVersion);
    }

    /**
     * FastExternalizable writer.  Must call superclass method first to
     * write common elements.
     */
    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
        throws IOException {

        out.writeShort(keyBytes.length);
        out.write(keyBytes);
        resultValue.writeFastExternal(out, serialVersion);
        version.writeFastExternal(out, serialVersion);
        Result.writeExpirationTime(out, expirationTime, serialVersion);
    }

    public byte[] getKeyBytes() {
        return keyBytes;
    }

    public Value getValue() {
        return resultValue.getValue();
    }

    public byte[] getValueBytes() {
        return resultValue.getBytes();
    }

    public Version getVersion() {
        return version;
    }

    public long getExpirationTime() {
        return expirationTime;
    }
}
