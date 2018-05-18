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

import oracle.kv.impl.admin.plan.MetadataPlan;
import oracle.kv.impl.admin.plan.TablePlanGenerator;
import oracle.kv.impl.api.table.IndexImpl;
import oracle.kv.impl.api.table.TableMetadata;

import com.sleepycat.persist.model.Persistent;

/**
 * Completes index addition, making the index publicly visible.
 */
@Persistent
public class CompleteAddIndex extends UpdateMetadata<TableMetadata> {
    private static final long serialVersionUID = 1L;

    private /*final*/ String indexName;
    private /*final*/ String tableName;

    /**
     */
    public CompleteAddIndex(MetadataPlan<TableMetadata> plan,
                            String indexName,
                            String tableName) {
        super(plan);
        this.indexName = indexName;
        this.tableName = tableName;
    }

    /*
     * No-arg ctor for use by DPL.
     */
    @SuppressWarnings("unused")
    private CompleteAddIndex() {
    }

    @Override
    protected TableMetadata updateMetadata() {
        final TableMetadata md = plan.getMetadata();

        if (md == null) {
            throw new IllegalStateException("Table metadata not found");
        }
        if (!md.updateIndexStatus(indexName,
                                  tableName,
                                  IndexImpl.IndexStatus.READY)) {
            throw new IllegalStateException
                ("CompleteAddIndex: index does not exists: " +
                 indexName + " in table: " + tableName);
        }
        plan.getAdmin().saveMetadata(md, plan);
        return md;
    }

    @Override
    public boolean continuePastError() {
        return false;
    }

    @Override
    public String toString() {
        return TablePlanGenerator.makeName("CompleteAddIndex", tableName,
                                           indexName);
    }
    
    @Override
    public boolean logicalCompare(Task t) {
        return true;
    }
}
