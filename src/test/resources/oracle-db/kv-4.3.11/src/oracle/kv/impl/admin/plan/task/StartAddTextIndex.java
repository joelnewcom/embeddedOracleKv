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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oracle.kv.impl.admin.IllegalCommandException;
import oracle.kv.impl.admin.plan.MetadataPlan;
import oracle.kv.impl.admin.plan.TablePlanGenerator;
import oracle.kv.impl.api.table.TableImpl;
import oracle.kv.impl.api.table.TableMetadata;
import oracle.kv.impl.api.table.IndexImpl;
import oracle.kv.impl.api.table.IndexImpl.AnnotatedField;

/**
 * Creates a new index.  The index is not visible until it is populated.
 */
public class StartAddTextIndex extends UpdateMetadata<TableMetadata> {
    private static final long serialVersionUID = 1L;

    private final String indexName;
    private final String tableName;
    private final AnnotatedField[] ftsFields;
    private final Map<String,String> properties;
    private final String description;

    @SuppressWarnings("unused")
    public StartAddTextIndex(MetadataPlan<TableMetadata> plan,
                             String indexName,
                             String tableName,
                             AnnotatedField[] ftsFields,
                             Map<String,String> properties,
                             String description) {
        super(plan);
        this.indexName = indexName;
        this.tableName = tableName;
        this.ftsFields = ftsFields;
        this.properties = properties;
        this.description = description;

        /*
         * Make sure that the table metadata exists and validate that (1) the
         * table exists, 2) the index does not exist and (2) the state passed
         * in can be used for the index.  The simplest way to do the latter is
         * to create a transient copy of the index.
         */
        final TableMetadata md = plan.getMetadata();
        if (md == null) {
            throw new IllegalStateException("Table metadata not found");
        }
        final TableImpl table = md.getTable(tableName);

        if (table == null) {
            throw new IllegalCommandException
                ("AddIndex: table does not exist: " + tableName);
        }
        if (table.getTextIndex(indexName) != null) {
            throw new IllegalCommandException
                    ("AddIndex: index: " + indexName +
                     " already exists in table: " + tableName);
        }
        
        List<String> fieldNames = new ArrayList<String>(ftsFields.length);
        Map<String,String> annotations = new HashMap<String,String>(ftsFields.length);
        IndexImpl.populateMapFromAnnotatedFields(Arrays.asList(ftsFields),
                                                 fieldNames,
                                                 annotations);
        new IndexImpl(indexName, table, fieldNames, annotations,
                      properties, description);
    }

    @Override
    protected TableMetadata updateMetadata() {
        final TableMetadata md = plan.getMetadata();
        if (md == null) {
            throw new IllegalStateException("Table metadata not found");
        }
        final TableImpl table = md.getTable(tableName);

        if (table == null) {
            throw new IllegalStateException("Table " + tableName +
                                             " is missing from " + md);
        }
        if (table.getIndex(indexName) == null) {
            md.addTextIndex(indexName,
                            tableName,
                            new ArrayList<AnnotatedField>
                                (Arrays.asList(ftsFields)),
                            properties,
                            description);
            plan.getAdmin().saveMetadata(md, plan);
        }
        return md;
    }

    @Override
    public boolean continuePastError() {
        return false;
    }

    @Override
    public String toString() {
        return TablePlanGenerator.makeName("StartAddTextIndex", tableName,
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

        StartAddTextIndex other = (StartAddTextIndex) t;
        return (tableName.equalsIgnoreCase(other.tableName) &&
                indexName.equalsIgnoreCase(other.indexName) &&
                Arrays.equals(ftsFields, other.ftsFields));
    }
}
