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

package oracle.kv.impl.api.table;

import oracle.kv.table.TimeToLive;


/**
 * A TableChange that evolves an existing table.
 */
class EvolveTable extends TableChange {
    private static final long serialVersionUID = 1L;

    private final String tableName;
    private final FieldMap fields;
    private final TimeToLive ttl;

    EvolveTable(TableImpl table, int seqNum) {
        super(seqNum);
        tableName = table.getFullName();
        fields = table.getFieldMap();
        ttl = table.getDefaultTTL();
    }

    @Override
    public boolean apply(TableMetadata md) {
        md.evolveTable(tableName, fields, ttl);
        return true;
    }
}
