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

import java.util.concurrent.TimeUnit;
import java.util.List;

import oracle.kv.impl.admin.IllegalCommandException;
import oracle.kv.impl.admin.plan.MetadataPlan;
import oracle.kv.impl.admin.plan.TablePlanGenerator;
import oracle.kv.impl.api.table.FieldMap;
import oracle.kv.impl.api.table.TableImpl;
import oracle.kv.impl.api.table.TableMetadata;
import oracle.kv.impl.security.AccessCheckUtils;
import oracle.kv.impl.security.ExecutionContext;
import oracle.kv.impl.security.util.SecurityUtils;
import oracle.kv.table.TimeToLive;

import com.sleepycat.persist.model.Persistent;

/**
 * Adds a table
 *
 * version 0: original
 * version 1: added primaryKeySizes, ttl and ttlUnit fields
 */
@Persistent(version=1)
public class AddTable extends UpdateMetadata<TableMetadata> {
    private static final long serialVersionUID = 1L;

    private /*final*/ String tableName;
    private /*final*/ String parentName;
    private /*final*/ List<String> primaryKey;

    /*
     * The major key is the shard key. Since this is a DPL object, the
     * field cannot be renamed without upgrade issues, so we will
     * maintain the name.
     */
    private /*final*/ List<String> majorKey;
    private /*final*/ FieldMap fieldMap;
    private /*final*/ boolean r2compat;
    private /*final*/ int schemaId;
    private /*final*/ String description;
    /*
     * Note that we persist the base types instead of a TimeToLive instance
     * in order to avoid having to add DPL annotations to TimeToLive and its
     * superclass. If ttlUnit is null, no ttl was specified.
     */
    private /*final*/ int ttl;
    private /*final*/ TimeUnit ttlUnit;
    private /*final*/ List<Integer> primaryKeySizes;

    /**
     */
    public AddTable(MetadataPlan<TableMetadata> plan,
                    TableImpl table,
                    String parentName) {
        super(plan);

        /*
         * Caller verifies parameters
         */

        this.tableName = table.getName();
        this.parentName = parentName;
        this.primaryKey = table.getPrimaryKey();
        this.primaryKeySizes = table.getPrimaryKeySizes();

        /* Note that the major key is the shard key */
        this.majorKey = table.getShardKey();
        if (table.getDefaultTTL() != null) {
            this.ttl = (int) table.getDefaultTTL().getValue();
            this.ttlUnit = table.getDefaultTTL().getUnit();
        } else {
            this.ttl = 0;
            this.ttlUnit = null;
        }
        this.fieldMap = table.getFieldMap();
        this.r2compat = table.isR2compatible();
        this.schemaId = table.getSchemaId();
        this.description = table.getDescription();

        final TableMetadata md = plan.getMetadata();
        if ((md != null) && md.tableExists(tableName, parentName)) {
            throw new IllegalCommandException
            ("Table already exists: " +
             TableMetadata.makeQualifiedName(tableName, parentName));
        }

        ensureCurrentUserOwnsParent();
    }

    /*
     * No-arg ctor for use by DPL.
     */
    @SuppressWarnings("unused")
    private AddTable() {
    }

    /*
     * Checks if current user is the owner of a table.
     */
    private void ensureCurrentUserOwnsParent() {
        if (ExecutionContext.getCurrent() == null) {
            return;
        }
        final TableMetadata md = plan.getMetadata();
        if (md == null) {
            return;
        }
        if (parentName != null) {
            TableImpl parentTable = md.getTable(parentName);
            assert (parentTable != null);

            /* The parent table is a legacy table without owner, just return */
            if (parentTable.getOwner() == null) {
                return;
            }
            if (!AccessCheckUtils.currentUserOwnsResource(parentTable)) {
                throw new IllegalCommandException(
                    "Only the owner of table " + parentName + " is able to " +
                    "create child tables under it");
            }
        }
    }

    @Override
    protected TableMetadata updateMetadata() {
        TableMetadata md = plan.getMetadata();
        if (md == null) {
            md = new TableMetadata(true);
        }

        /*
         * If exist, then we are done. Return the metadata so that it is
         * broadcast, just in case this is a re-execute.
         */
        if (!md.tableExists(tableName, parentName)) {
            // TODO the add table method does not check for dup
            md.addTable(tableName,
                        parentName,
                        primaryKey,
                        primaryKeySizes,
                        majorKey,
                        fieldMap,
                        (ttlUnit == null) ? null :
                        TimeToLive.createTimeToLive(ttl, ttlUnit),
                        r2compat,
                        schemaId,
                        description,
                        SecurityUtils.currentUserAsOwner());
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
        String name = TableMetadata.makeQualifiedName(tableName, parentName);
        return TablePlanGenerator.makeName("AddTable", name, null);
    }

    /**
     * Returns true if this AddTable will end up creating the same table.
     * Checks that tableName, parentName, primaryKey, majorKey (shard key) and
     * fieldMap are the same. Intentionally excludes r2compat, schemId, and
     * description, since those don't directly affect the table metadata.
     */
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

        AddTable other = (AddTable) t;
        if (!tableName.equalsIgnoreCase(other.tableName)) {
            return false;
        }

        if (parentName == null) {
            if (other.parentName != null) {
                return false;
            }
        } else if (!parentName.equalsIgnoreCase(other.parentName)) {
            return false;
        }

        return (primaryKey.equals(other.primaryKey) &&
                majorKey.equals(other.majorKey) &&
                fieldMap.equals(other.fieldMap));
    }
}
