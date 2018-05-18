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
import oracle.kv.impl.admin.plan.TablePlanGenerator;
import oracle.kv.impl.api.table.TableImpl;
import oracle.kv.impl.api.table.TableMetadata;

import com.sleepycat.persist.model.Persistent;

/**
 * Remove/drop a table
 */
@Persistent
public class RemoveTable extends UpdateMetadata<TableMetadata> {
    private static final long serialVersionUID = 1L;

    protected /*final*/ String tableName;
    protected /*final*/ boolean markForDelete;

    public static RemoveTable newInstance(MetadataPlan<TableMetadata> plan,
                                          String tableName,
                                          boolean markForDelete) {
        final RemoveTable removeTable =
            new RemoveTable(plan, tableName, markForDelete);
        removeTable.checkTableForRemove();
        return removeTable;
    }

    protected RemoveTable(MetadataPlan<TableMetadata> plan,
                       String tableName,
                       boolean markForDelete) {
        super(plan);

        /*
         * Caller verifies parameters
         */
        this.tableName = tableName;
        this.markForDelete = markForDelete;
    }

    /**
     * Check if table to be removed can be found. This method must be called
     * once the table metadata is available.
     */
    protected void checkTableForRemove() {
        final TableMetadata md = getMetadata();

        if (md != null) {
            md.checkForRemove(tableName, true);
            return;
        }
        throw new IllegalCommandException
            ("Table does not exist: " + tableName);
    }

    /*
     * No-arg ctor for use by DPL.
     */
    @SuppressWarnings("unused")
    private RemoveTable() {
    }

    @Override
    protected TableMetadata updateMetadata() {
        final TableMetadata md = getMetadata();
        if (md == null) {
            throw new IllegalStateException("Table metadata not found");
        }

        /*
         * See if the table is still present.  This will not throw if the
         * table is absent. Return the metadata so that it is broadcast, just
         * in case this is a re-execute.
         */
        final TableImpl table = md.getTable(tableName);
        if (table != null) {
            md.dropTable(tableName, markForDelete);
            getPlan().getAdmin().saveMetadata(md, getPlan());
        }
        return md;
    }

    @Override
    public String toString() {
        return TablePlanGenerator.makeName("RemoveTable", tableName, null);
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

        RemoveTable other = (RemoveTable) t;
        return (tableName.equalsIgnoreCase(other.tableName) &&
                (markForDelete == other.markForDelete));
    }
}
