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
 * Completes index addition, making the index publicly visible.
 */
@Persistent
public class RemoveIndex extends UpdateMetadata<TableMetadata> {
    private static final long serialVersionUID = 1L;

    protected /*final*/ String indexName;
    protected /*final*/ String tableName;

    public static RemoveIndex newInstance(MetadataPlan<TableMetadata> plan,
                                          String indexName,
                                          String tableName) {
        final RemoveIndex removeIndex =
            new RemoveIndex(plan, indexName, tableName);
        removeIndex.checkIndexForRemove();
        return removeIndex;
    }

    protected RemoveIndex(MetadataPlan<TableMetadata> plan,
                        String indexName,
                        String tableName) {
        super(plan);

        /*
         * Caller verifies parameters
         */
        this.indexName = indexName;
        this.tableName = tableName;
    }

    /*
     * No-arg ctor for use by DPL.
     */
    @SuppressWarnings("unused")
    private RemoveIndex() {
    }

    /**
     * Check if index to be removed can be found.  This method must be called
     * once the table metadata is available.
     */
    protected void checkIndexForRemove() {
        final TableMetadata md = getMetadata();
        if (md == null) {
            throw new IllegalCommandException("Table metadata not found");
        }
        final TableImpl table = md.getTable(tableName);
        if ((table == null) || (table.getIndex(indexName) == null)) {
            throw new IllegalCommandException
                ("RemoveIndex: index: " + indexName +
                 " does not exists in table: " + tableName);
        }
    }

    @Override
    protected TableMetadata updateMetadata() {
        final TableMetadata md = getMetadata();
        if (md == null) {
            throw new IllegalStateException("Table metadata not found");
        }

        /*
         * If the table or index is already gone, we are done.
         * Return the metadata so that it is broadcast, just in case this is
         * a re-execute.
         */
        final TableImpl table = md.getTable(tableName);
        if ((table != null) && (table.getIndex(indexName) != null)) {
            md.dropIndex(indexName, tableName);
            getPlan().getAdmin().saveMetadata(md, getPlan());
        }
        return md;
    }

    @Override
    public String toString() {
        return TablePlanGenerator.makeName("RemoveIndex", tableName,
                                           indexName);
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

        RemoveIndex other = (RemoveIndex) t;
        if (!tableName.equalsIgnoreCase(other.tableName)) {
            return false;
        }

        if (!indexName.equalsIgnoreCase(other.indexName)) {
            return false;
        }
        return true;
    }
}
