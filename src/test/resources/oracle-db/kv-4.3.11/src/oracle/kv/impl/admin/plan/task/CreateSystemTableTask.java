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

import java.util.List;

import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.IllegalCommandException;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.api.table.FieldMap;
import oracle.kv.impl.api.table.TableBuilder;
import oracle.kv.impl.api.table.TableMetadata;
import oracle.kv.impl.metadata.Metadata.MetadataType;
import oracle.kv.impl.security.util.SecurityUtils;

/**
 * A task for adding a system table into KVStore. The system table should be
 * added at the end of deploying the KVStore.
 */
public class CreateSystemTableTask extends SingleJobTask {

    private static final long serialVersionUID = 1L;

    private AbstractPlan plan;

    private final String tableName;
    private final String parentName;
    private final List<String> primaryKey;
    private final List<Integer> primaryKeySizes;

    /*
     * The major key is the shard key. Since this is a DPL object, the
     * field cannot be renamed without upgrade issues, so we will
     * maintain the name.
     */
    private final List<String> majorKey;
    private final FieldMap fieldMap;
    private final boolean r2compat;
    private final int schemaId;
    private final String description;

    public CreateSystemTableTask(AbstractPlan plan, TableBuilder tableBuilder) {
        this.plan = plan;
        this.tableName = tableBuilder.getName();
        this.parentName =
                tableBuilder.getParent() == null? null:
                    tableBuilder.getParent().getFullName();
        this.primaryKey = tableBuilder.getPrimaryKey();
        this.primaryKeySizes = tableBuilder.getPrimaryKeySizes();

        /* Note that the major key is the shard key */
        this.majorKey = tableBuilder.getShardKey();

        this.fieldMap = tableBuilder.getFieldMap();
        this.r2compat = tableBuilder.isR2compatible();
        this.schemaId = tableBuilder.getSchemaId();
        this.description = tableBuilder.getDescription();

        final TableMetadata md =
                plan.getAdmin().getMetadata(TableMetadata.class,
                                            MetadataType.TABLE);
        if ((md != null) && md.tableExists(tableName, parentName)) {
            throw new IllegalCommandException
            ("Table already exists: " +
             TableMetadata.makeQualifiedName(tableName, parentName));
        }
    }

    @Override
    public State doWork()
        throws Exception {
        TableMetadata md =
                plan.getAdmin().getMetadata(TableMetadata.class,
                                            MetadataType.TABLE);
        if (md == null) {
            md = new TableMetadata(true);
        }

        /*
         * Don't add the table if it exists. Still do the broadcast, just in
         * case this is a re-execute.
         */
        if (!md.tableExists(tableName, parentName)) {
            // TODO the add table method does not check for dup
            md.addSysTable(tableName,
                           parentName,
                           primaryKey,
                           primaryKeySizes,
                           majorKey,
                           fieldMap,
                           null, /* TTL */
                           r2compat,
                           schemaId,
                           description,
                           SecurityUtils.currentUserAsOwner());
            plan.getAdmin().saveMetadata(md, plan);
        }

        final Admin admin = plan.getAdmin();

        if (!Utils.broadcastMetadataChangesToRNs(plan.getLogger(),
                                                 md,
                                                 admin.getCurrentTopology(),
                                                 toString(),
                                                 admin.getParams()
                                                 .getAdminParams(),
                                                 plan)) {
            return State.INTERRUPTED;
        }

        return State.SUCCEEDED;
    }

    @Override
    public boolean continuePastError() {
        return false;
    }

    @Override
    public String toString() {
       return super.toString() + " create table " + tableName;
    }
}
