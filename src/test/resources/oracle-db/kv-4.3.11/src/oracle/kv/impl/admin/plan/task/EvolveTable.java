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

package oracle.kv.impl.admin.plan.task;

import oracle.kv.impl.admin.IllegalCommandException;
import oracle.kv.impl.admin.plan.MetadataPlan;
import oracle.kv.impl.api.table.FieldMap;
import oracle.kv.impl.api.table.TableImpl;
import oracle.kv.impl.api.table.TableMetadata;
import oracle.kv.table.TimeToLive;

import com.sleepycat.persist.model.Persistent;

/**
 * Evolve a table
 */
@Persistent(version=1)
public class EvolveTable extends UpdateMetadata<TableMetadata> {
    private static final long serialVersionUID = 1L;

    private /*final*/ String tableName;
    private /*final*/ int tableVersion;
    private /*final*/ FieldMap fieldMap;
    private /*final*/ TimeToLive ttl;

    /**
     */
    public EvolveTable(MetadataPlan<TableMetadata> plan,
                       String tableName,
                       int tableVersion,
                       FieldMap fieldMap,
                       TimeToLive ttl) {
        super(plan);

        /*
         * Caller verifies parameters
         */
        this.tableName = tableName;
        this.fieldMap = fieldMap;
        this.tableVersion = tableVersion;
        this.ttl = ttl;

        final TableMetadata md = plan.getMetadata();
        if (md == null || md.getTable(tableName) == null) {
            throw new IllegalCommandException
                ("Table does not exist: " + tableName);
        }
    }

    /*
     * No-arg ctor for use by DPL.
     */
    @SuppressWarnings("unused")
    private EvolveTable() {
    }

    @Override
    protected TableMetadata updateMetadata() {
        final TableMetadata md = plan.getMetadata();
        if (md == null) {
            throw new IllegalStateException("Table metadata not found");
        }
        final TableImpl table = md.getTable(tableName);
        if (table == null) {
            throw new IllegalStateException
                ("Cannot find table to evolve: " + tableName);
        }

        if (md.evolveTable(table, tableVersion, fieldMap, ttl)) {
            plan.getAdmin().saveMetadata(md, plan);
        }
        return md;
    }

    @Override
    public boolean logicalCompare(Task t) {
        if (this == t) {
            return true;
        }

        if (t == null) {
            return false;
        }

        if (getClass() != t.getClass()) {
            return false;
        }

        EvolveTable other = (EvolveTable) t;
        return (tableName.equalsIgnoreCase(other.tableName) &&
                (tableVersion == other.tableVersion)  &&
                (fieldMap.equals(other.fieldMap)));
    }
}
