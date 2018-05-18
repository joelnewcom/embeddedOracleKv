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

import static oracle.kv.impl.util.SerialVersion.RESULT_INDEX_ITERATE_VERSION;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import oracle.kv.Version;

/**
 * This class holds results of an index row iteration over a table.  It extends
 * ResultKeyValueVersion to do this, adding the index key in addition to the
 * row itself.  The index key is needed by the client in order to accurately do
 * chunked iteration using a resume key.  The resume key cannot be constructed
 * from the Row alone in all cases.
 *
 * This class, which includes the index key bytes, was introduced in release
 * 3.2, so it only includes/expected index key bytes if the serialVersion of
 * the "other" side of the connection is at least the version associated with
 * 3.2 (V6).
 *
 * @since 3.2
 */
public class ResultIndexRows extends ResultKeyValueVersion {

    /* this class adds the index key to ResultKeyValueVersion */
    private final byte[] indexKeyBytes;

    public ResultIndexRows(byte[] indexKeyBytes,
                           byte[] primaryKeyBytes,
                           byte[] valueBytes,
                           Version version,
                           long expirationTime) {
        super(primaryKeyBytes, valueBytes, version, expirationTime);
        this.indexKeyBytes = indexKeyBytes;
    }

    /**
     * FastExternalizable constructor.  Must call superclass constructor
     * first to read common elements.
     */
    public ResultIndexRows(DataInput in, short serialVersion)
        throws IOException {
        super(in, serialVersion);
        if (serialVersion >= RESULT_INDEX_ITERATE_VERSION) {
            int keyLen = in.readShort();
            indexKeyBytes = new byte[keyLen];
            in.readFully(indexKeyBytes);
        } else {
            indexKeyBytes = null;
        }
    }

    /**
     * FastExternalizable writer.  Must call superclass method first to
     * write common elements.
     */
    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
        throws IOException {
        super.writeFastExternal(out, serialVersion);
        if (serialVersion >= RESULT_INDEX_ITERATE_VERSION) {
            out.writeShort(indexKeyBytes.length);
            out.write(indexKeyBytes);
        }
    }

    public byte[] getIndexKeyBytes() {
        return indexKeyBytes;
    }
}
