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

import oracle.kv.ReturnValueVersion;
import oracle.kv.Value;
import oracle.kv.Version;
import oracle.kv.table.TimeToLive;

/**
 * Inserts a key/data pair.
 */
public class PutIfVersion extends Put {

    private final Version matchVersion;

    /**
     * Constructs a put-if-version operation.
     */
    public PutIfVersion(byte[] keyBytes,
                        Value value,
                        ReturnValueVersion.Choice prevValChoice,
                        Version matchVersion) {
        this(keyBytes, value, prevValChoice, matchVersion, 0);
    }

    /**
     * Constructs a put-if-version operation with a table id.
     */
    public PutIfVersion(byte[] keyBytes,
                        Value value,
                        ReturnValueVersion.Choice prevValChoice,
                        Version matchVersion,
                        long tableId) {
        this(keyBytes, value, prevValChoice, matchVersion, tableId,
                null, false);
    }

    public PutIfVersion(byte[] keyBytes,
            Value value,
            ReturnValueVersion.Choice prevValChoice,
            Version matchVersion,
            long tableId,
            TimeToLive ttl,
            boolean updateTTL) {
        super(OpCode.PUT_IF_VERSION, keyBytes, value, prevValChoice, tableId,
                ttl, updateTTL);
        this.matchVersion = matchVersion;
    }
    /**
     * FastExternalizable constructor.  Must call superclass constructor first
     * to read common elements.
     */
    PutIfVersion(DataInput in, short serialVersion)
        throws IOException {

        super(OpCode.PUT_IF_VERSION, in, serialVersion);
        matchVersion = Version.createVersion(in, serialVersion);
    }

    Version getMatchVersion() {
        return matchVersion;
    }

    /**
     * FastExternalizable writer.  Must call superclass method first to write
     * common elements.
     */
    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
        throws IOException {

        super.writeFastExternal(out, serialVersion);
        matchVersion.writeFastExternal(out, serialVersion);
    }

    @Override
    public String toString() {
        return super.toString() + " MatchVersion: " + matchVersion;
    }
}
