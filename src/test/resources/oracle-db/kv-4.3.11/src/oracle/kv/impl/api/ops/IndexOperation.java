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

import oracle.kv.impl.api.table.IndexImpl;
import oracle.kv.impl.api.table.IndexRange;
import oracle.kv.impl.api.table.TargetTables;
import oracle.kv.impl.util.SerialVersion;

/**
 * An index operation identifies the index by name and table and includes
 * start, end, and resume keys.  Rules for keys:
 * <ul>
 * <li>the resume key overrides the start key</li>
 * <li>a null start key is used to operate over the entire index</li>
 * <li>the boolean dontIterate is used to do exact match operations.  If true,
 * then only matching entries are returned.  There may be more than one.</li>
 * </ul>
 *
 * Resume key is tricky because secondary databases may have duplicates so in
 * order to resume properly it's necessary to have both the resume secondary
 * key and the resume primary key.
 *
 * This class is public to make it available to tests.
 */
public class IndexOperation extends InternalOperation {

    /*
     * These members represent the serialized state of the class in the
     * protocol.  These are in order.
     */
    private final String indexName;
    protected final TargetTables targetTables;
    protected final IndexRange range;
    private final byte[] resumeSecondaryKey;
    private final byte[] resumePrimaryKey;
    private final int batchSize;
    private final short clientSerialVersion;

    /**
     * Constructs an index operation.
     *
     * For subclasses, allows passing OpCode.
     */
    public IndexOperation(OpCode opCode,
                          String indexName,
                          TargetTables targetTables,
                          IndexRange range,
                          byte[] resumeSecondaryKey,
                          byte[] resumePrimaryKey,
                          int batchSize) {
        super(opCode);
        this.indexName = indexName;
        this.targetTables = targetTables;
        this.range = range;
        this.resumeSecondaryKey = resumeSecondaryKey;
        this.resumePrimaryKey = resumePrimaryKey;
        this.batchSize = batchSize;
        this.clientSerialVersion = SerialVersion.CURRENT;
    }

    /**
     * FastExternalizable constructor.  Must call superclass constructor first
     * to read common elements.
     *
     * For subclasses, allows passing OpCode.
     */
    IndexOperation(OpCode opCode, DataInput in, short serialVersion)
        throws IOException {

        super(opCode, in, serialVersion);

        this.clientSerialVersion = serialVersion;

        /* index name */
        indexName = in.readUTF();

        targetTables = new TargetTables(in, serialVersion);

        /* index range */
        range = new IndexRange(in, serialVersion);

        /* resume key */
        int keyLen = in.readShort();
        if (keyLen < 0) {
            resumeSecondaryKey = null;
            resumePrimaryKey = null;
        } else {
            /*
             * Resume keys, if present always have both secondary and
             * primary, in that order.
             */
            resumeSecondaryKey = new byte[keyLen];
            in.readFully(resumeSecondaryKey);
            keyLen = in.readShort();
            resumePrimaryKey = new byte[keyLen];
            in.readFully(resumePrimaryKey);
        }

        /* batch size */
        batchSize = in.readInt();
    }

    /**
     * FastExternalizable writer.  Must call superclass method first to write
     * common elements.
     */
    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
        throws IOException {

        super.writeFastExternal(out, serialVersion);

        out.writeUTF(indexName);

        targetTables.writeFastExternal(out, serialVersion);

        range.writeFastExternal(out, serialVersion);

        if (resumeSecondaryKey == null) {
            out.writeShort(-1);
        } else {
            out.writeShort(resumeSecondaryKey.length);
            out.write(resumeSecondaryKey);
            out.writeShort(resumePrimaryKey.length);
            out.write(resumePrimaryKey);
        }
        out.writeInt(batchSize);
    }

    IndexRange getIndexRange() {
        return range;
    }

    byte[] getResumeSecondaryKey() {
        return resumeSecondaryKey;
    }

    byte[] getResumePrimaryKey() {
        return resumePrimaryKey;
    }

    int getBatchSize() {
        return batchSize;
    }

    String getIndexName() {
        return indexName;
    }

    TargetTables getTargetTables() {
        return targetTables;
    }

    @Override
    public String toString() {
        return super.toString(); //TODO
    }

    /*
     * This method is used when an IndexOperation is sent from a pre-4.2 client
     * to a 4.2+ server and the index was created by the 4.2+ server. In this
     * case, the binary start/stop index keys that are created by the client do
     * not contain the null indicator bytes that are normally expected by the
     * server. To handle this, the start/stop keys are deserialized by a version
     * of IndexImpl.deserializeIndexKey() that does not expect nulls, and then
     * reseialized to add the null indicator bytes.
     */
    void checkReserializeKeys(IndexImpl index) {
        if (!index.isNullSupported() ||
            clientSerialVersion > SerialVersion.V10) {
            return;
        }
        range.reserializeOldKeys(index);
    }

    /*
     * This method is used by IndexKeysIterateHandler, which sends binary
     * index keys (extracted from the index scan) back to the client. If the
     * binary index keys contain null indicators but the client is pre-4.2,
     * the null indicators must be removed from the binary keys, because
     * the client wouldn't be able to deserialize them correctly otherwise.
     */
    byte[] checkReserializeKeys(IndexImpl index, byte[] indexKey) {
        if (!index.isNullSupported() ||
            clientSerialVersion > SerialVersion.V10) {
            return indexKey;
        }

        /*
         * Index key is in "new" format, with nulls. Reserialize to old
         * format
         */
        return index.reserializeToOldKey(indexKey);
    }
}
