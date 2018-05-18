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

import java.io.IOException;
import java.io.DataInput;

import oracle.kv.KeyRange;
import oracle.kv.impl.api.table.TargetTables;

/**
 * A multi-delete table operation over table(s) in the same partition.
 * This code is shared between normal client multiDelete operations and
 * table data removal code which is internal to RepNodes.  In the latter
 * case there are options that don't apply to client operations:
 * 1.  batch size is allowed
 * 2.  resume key used for batching (returned by a single iteration instance)
 * 3.  major key may be incomplete which is not possible in the client operation
 * This state is added to the object but is only set and used by a separate
 * direct object constructor.
 */
public class MultiDeleteTable extends MultiTableOperation {

    private final byte[] resumeKey;
    private final boolean majorPathComplete;
    private final int batchSize;

    /*
     * This is only used on the server side by table data removal to track the
     * last key deleted in order to use it as the resumeKey for batch deletes.
     */
    private transient byte[] lastDeleted;

    /**
     * Construct a multi-get operation, used by client.
     */
    public MultiDeleteTable(byte[] parentKey,
                            TargetTables targetTables,
                            KeyRange subRange) {
        super(OpCode.MULTI_DELETE_TABLE, parentKey, targetTables, subRange);
        this.majorPathComplete = true;
        this.batchSize = 0;
        this.resumeKey = null;
    }

    /**
     * FastExternalizable constructor.
     */
    protected MultiDeleteTable(DataInput in, short serialVersion)
        throws IOException {

        super(OpCode.MULTI_DELETE_TABLE, in, serialVersion);
        this.majorPathComplete = true;
        this.batchSize = 0;
        this.resumeKey = null;
    }

    /**
     * Construct a MultiDeleteTable operation for internal use by table data
     * removal.  This constructor requires only a single key and target
     * table but also requires batchSize, resumeKey, and majorPathComplete
     * state.  KeyRange does not apply.  Note: KeyRange might
     * be used by a more general-purpose delete mechanism if ever exposed.
     */
    public MultiDeleteTable(byte[] parentKey,
                            long targetTableId,
                            boolean majorPathComplete,
                            int batchSize,
                            byte[] resumeKey) {
        super(OpCode.MULTI_DELETE_TABLE, parentKey,
              new TargetTables(targetTableId), null);
        this.majorPathComplete = majorPathComplete;
        this.batchSize = batchSize;
        this.resumeKey = resumeKey;
    }

    byte[] getResumeKey() {
        return resumeKey;
    }

    boolean getMajorPathComplete() {
        return majorPathComplete;
    }

    int getBatchSize() {
        return batchSize;
    }

    public byte[] getLastDeleted() {
        return lastDeleted;
    }

    void setLastDeleted(byte[] lastDeleted) {
        this.lastDeleted = lastDeleted;
    }
}
