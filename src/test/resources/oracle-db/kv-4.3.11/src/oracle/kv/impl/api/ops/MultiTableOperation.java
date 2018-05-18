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
import oracle.kv.impl.api.table.TargetTables;

/**
 * This is an intermediate class that encapsulates a get or iteration
 * operation on multiple tables. It can be directly constructed by
 * server-side iteration code, but is usually used in the context of
 * a table iteration client request.
 */
abstract class MultiTableOperation extends MultiKeyOperation {

    /*
     * Encapsulates the target table, child tables, ancestor tables.
     */
    private final TargetTables targetTables;

    /**
     * Construct a multi-get operation.
     */
    public MultiTableOperation(OpCode opCode,
                               byte[] parentKey,
                               TargetTables targetTables,
                               KeyRange subRange) {
        super(opCode, parentKey, subRange, Depth.PARENT_AND_DESCENDANTS);
        this.targetTables = targetTables;
    }

    /**
     * FastExternalizable constructor.  Must call superclass constructor first
     * to read common elements.
     */
    MultiTableOperation(OpCode opCode, DataInput in, short serialVersion)
        throws IOException {

        super(opCode, in, serialVersion);

        targetTables = new TargetTables(in, serialVersion);
    }

    TargetTables getTargetTables() {
        return targetTables;
    }

    /**
     * FastExternalizable writer.  Must call superclass method first to write
     * common elements.
     */
    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
        throws IOException {

        super.writeFastExternal(out, serialVersion);

        targetTables.writeFastExternal(out, serialVersion);
    }
}
