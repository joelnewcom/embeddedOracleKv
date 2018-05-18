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

package oracle.kv.impl.admin.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import oracle.kv.KVVersion;
import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.IllegalCommandException;
import oracle.kv.impl.admin.param.Parameters;
import oracle.kv.impl.admin.plan.DeployTableMetadataPlan.AddIndexPlan;
import oracle.kv.impl.admin.plan.DeployTableMetadataPlan.AddTablePlan;
import oracle.kv.impl.admin.plan.DeployTableMetadataPlan.EvolveTablePlan;
import oracle.kv.impl.admin.plan.DeployTableMetadataPlan.RemoveIndexPlan;
import oracle.kv.impl.admin.plan.DeployTableMetadataPlan.RemoveTablePlan;
import oracle.kv.impl.admin.plan.task.AddTable;
import oracle.kv.impl.admin.plan.task.CompleteAddIndex;
import oracle.kv.impl.admin.plan.task.EvolveTable;
import oracle.kv.impl.admin.plan.task.ParallelBundle;
import oracle.kv.impl.admin.plan.task.RemoveIndex;
import oracle.kv.impl.admin.plan.task.RemoveIndexV2;
import oracle.kv.impl.admin.plan.task.RemoveTable;
import oracle.kv.impl.admin.plan.task.RemoveTablePrivileges;
import oracle.kv.impl.admin.plan.task.RemoveTableV2;
import oracle.kv.impl.admin.plan.task.StartAddIndex;
import oracle.kv.impl.admin.plan.task.StartAddTextIndex;
import oracle.kv.impl.admin.plan.task.UpdateMetadata;
import oracle.kv.impl.admin.plan.task.Utils;
import oracle.kv.impl.admin.plan.task.WaitForAddIndex;
import oracle.kv.impl.admin.plan.task.WaitForRemoveTableData;
import oracle.kv.impl.api.table.FieldMap;
import oracle.kv.impl.api.table.IndexImpl.AnnotatedField;
import oracle.kv.impl.api.table.TableImpl;
import oracle.kv.impl.api.table.TableMetadata;
import oracle.kv.impl.metadata.Metadata.MetadataType;
import oracle.kv.impl.param.ParameterMap;
import oracle.kv.impl.param.ParameterState;
import oracle.kv.impl.security.KVStorePrivilege;
import oracle.kv.impl.security.ResourceOwner;
import oracle.kv.impl.security.RoleInstance;
import oracle.kv.impl.security.SystemPrivilege;
import oracle.kv.impl.security.TablePrivilege;
import oracle.kv.impl.security.metadata.KVStoreUser;
import oracle.kv.impl.security.metadata.SecurityMetadata;
import oracle.kv.impl.security.util.SecurityUtils;
import oracle.kv.impl.tif.ElasticsearchHandler;
import oracle.kv.impl.tif.TextIndexFeeder;
import oracle.kv.impl.topo.RepGroupId;
import oracle.kv.impl.topo.Topology;
import oracle.kv.impl.util.SerialVersion;
import oracle.kv.table.Index;
import oracle.kv.table.Table;
import oracle.kv.table.TimeToLive;

import org.elasticsearch.client.transport.TransportClient;

/**
 * Static utility class for generating plans for secondary indexes.
 *
 * Exception handling note.  This code runs in the context of the admin service
 * and the rule in the admin is that non-fatal runtime exceptions are thrown as
 * IllegalCommandException.  For that reason these methods catch and rethrow
 * exceptions from called methods.
 */
public class TablePlanGenerator {

    private final static KVVersion TABLE_AUTH_VERSION = KVVersion.R3_3;
    private final static KVVersion CASCADE_VERSION = KVVersion.R4_3;

    /* Prevent construction */
    private TablePlanGenerator() {}

    /**
     * Creates a plan to add a table.
     */
    static DeployTableMetadataPlan
        createAddTablePlan(AtomicInteger idGen,
                           String planName,
                           Planner planner,
                           TableImpl table,
                           String parentName) {

        checkStoreVersion(planner.getAdmin(),
            SerialVersion.getKVVersion(table.getRequiredSerialVersion()));

        String tableName = table.getName();

        final String fullPlanName = makeName(planName, tableName, null);
        final DeployTableMetadataPlan plan =
            new AddTablePlan(idGen, fullPlanName, planner);

        tableName = getRealTableName(tableName, plan.getMetadata());
        try {
            plan.addTask(new AddTable(plan,
                                      table,
                                      parentName));
        } catch (IllegalArgumentException iae) {
            throw new IllegalCommandException
                ("Failed to add table: " + iae.getMessage(), iae);
        }
        return plan;
    }

    private static void checkStoreVersion(Admin admin, KVVersion v) {
        KVVersion storeVersion = admin.getStoreVersion();
        if (storeVersion.compareTo(v) < 0 ) {
            throw new IllegalCommandException("Store version is not capable " +
                "of executing plan. Required version is " +
                v.getNumericVersionString() +
                ", store version is " +
                storeVersion.getNumericVersionString());
        }
    }

    /**
     * Creates a plan to evolve a table.
     *
     * The table version is the version of the table used as a basis for this
     * evolution.  It is used to verify that only the latest version of a table
     * is evolved.
     */
    static DeployTableMetadataPlan
        createEvolveTablePlan(AtomicInteger idGen,
                              String planName,
                              Planner planner,
                              String tableName,
                              int tableVersion,
                              FieldMap fieldMap,
                              TimeToLive ttl) {
        checkTable(tableName);
        if (fieldMap == null || fieldMap.isEmpty()) {
            throw new IllegalCommandException("Fields cannot be null or empty");
        }

        checkStoreVersion(planner.getAdmin(),
            SerialVersion.getKVVersion(fieldMap.getRequiredSerialVersion()));

        final String fullPlanName = makeName(planName, tableName, null);
        final DeployTableMetadataPlan plan;
        if (Utils.storeHasVersion(planner.getAdmin(), TABLE_AUTH_VERSION)) {
            plan = new EvolveTablePlan(idGen, fullPlanName, tableName, planner);
        } else {
            plan = new DeployTableMetadataPlan(idGen, fullPlanName, planner);
        }

        tableName = getRealTableName(tableName, plan.getMetadata());
        try {
            plan.addTask(new EvolveTable(plan,
                                         tableName,
                                         tableVersion,
                                         fieldMap,
                                         ttl));
        } catch (IllegalArgumentException iae) {
            throw new IllegalCommandException
                ("Failed to evolve table: " + iae.getMessage(), iae);
        }

        return plan;
    }

    /**
     * Creates a plan to remove a table.
     */
    static AbstractPlan createRemoveTablePlan(AtomicInteger idGen,
                                              String planName,
                                              Planner planner,
                                              Topology topology,
                                              String tableName,
                                              boolean removeData) {
        checkTable(tableName);

        final String fullPlanName = makeName(planName, tableName, null);
        final AbstractPlan plan;
        final Admin admin = planner.getAdmin();

        if (Utils.storeHasVersion(admin, CASCADE_VERSION)) {
            plan = new RemoveTablePlanV2(idGen, fullPlanName, tableName,
                                         removeData, planner);
            addRemoveTablePrivsTasks((RemoveTablePlanV2)plan);
        } else if (Utils.storeHasVersion(admin, TABLE_AUTH_VERSION)) {
            plan = new RemoveTablePlan(idGen, fullPlanName, tableName,
                                       removeData, planner);
        } else {
            plan = new DeployTableMetadataPlan(idGen, fullPlanName, planner);
        }
        tableName = getRealTableName(tableName,
            admin.getMetadata(TableMetadata.class, MetadataType.TABLE));

        /*
         * If we need to remove data, we first mark the table for deletion and
         * broadcast that change. This will trigger the RNs to remove the
         * table data from it's respective shard. The plan will wait for all
         * RNs to finish. Once the data is deleted, the table object can be
         * removed.
         */
        try {
            addRemoveIndexTasks(plan, tableName, admin);
            if (removeData) {
                addRemoveTableTasks(plan, tableName, true);

                final ParallelBundle bundle = new ParallelBundle();
                for (RepGroupId id : topology.getRepGroupIds()) {
                    bundle.addTask(new WaitForRemoveTableData(plan,
                                                              id,
                                                              tableName));
                }
                plan.addTask(bundle);
            }
            addRemoveTableTasks(plan, tableName, false);
        } catch (IllegalArgumentException iae) {
            throw new IllegalCommandException
                ("Failed to remove table: " + iae.getMessage(), iae);
        }

        return plan;
    }

    /**
     * Add a task to remove each index defined on the table.  Do this before
     * removing data as indexes are affected and performance would be quite bad
     * otherwise.
     */
    static private void addRemoveIndexTasks(final AbstractPlan plan,
                                            final String tableName,
                                            final Admin admin) {
        final TableMetadata md = admin.getMetadata(TableMetadata.class,
                                                   MetadataType.TABLE);
        if (md != null) {
            TableImpl table = md.checkForRemove(tableName, true);
            for (String indexName : table.getIndexes().keySet()) {
                checkTextIndexForRemoval(plan, admin, indexName,
                                         tableName, false);

                try {
                    if (plan instanceof RemoveTablePlanV2) {
                        plan.addTask(RemoveIndexV2.newInstance(
                            (RemoveTablePlanV2) plan, indexName, tableName));
                    } else {
                        plan.addTask(RemoveIndex.newInstance(
                            (DeployTableMetadataPlan )plan, indexName,
                            tableName));
                    }
                } catch (IllegalArgumentException iae) {
                    throw new IllegalCommandException
                        ("Failed to remove index: " + iae.getMessage(), iae);
                }
            }
        }
    }

    static private void addRemoveTablePrivsTasks(final RemoveTablePlanV2 plan) {
        final Set<String> involvedRoles =
            getInvolvedRoles(plan.getTableId(), plan.getSecurityMetadata());

        for (String role : involvedRoles) {
            plan.addTask(new RemoveTablePrivileges(
                plan, role, TablePrivilege.getAllTablePrivileges(
                    plan.getTableId(), plan.getTableFullName())));
        }
    }

    static private void addRemoveTableTasks(final AbstractPlan plan,
                                            final String tableName,
                                            final boolean removeData) {

        if (plan instanceof RemoveTablePlanV2) {
            plan.addTask(RemoveTableV2.newInstance(
                (RemoveTablePlanV2) plan, tableName, removeData));
        } else {
            plan.addTask(RemoveTable.newInstance(
                (DeployTableMetadataPlan) plan, tableName, removeData));
        }
    }

    /**
     * Creates a plan to add an index.
     * This operates in 3 parts
     * 1.  Update metadata to include the new index, which is in state
     *     "Populating". In that state it will be populated and used on
     *      RepNodes but will not appear to users in metadata.
     * 2.  Ask all RepNode masters to populate the index in a parallel bundle
     * 3.  Update the metadata again with the state "Ready" on the index to make
     *     it visible to users.
     */
    static DeployTableMetadataPlan createAddIndexPlan(AtomicInteger idGen,
                                                      String planName,
                                                      Planner planner,
                                                      Topology topology,
                                                      String indexName,
                                                      String tableName,
                                                      String[] indexedFields,
                                                      String description) {
        checkTable(tableName);
        checkIndex(indexName);
        if (indexedFields == null) {    // TODO - check for empty?
            throw new IllegalCommandException("Indexed fields cannot be null");
        }

        final String fullPlanName = makeName(planName, tableName, indexName);
        final DeployTableMetadataPlan plan;
        if (Utils.storeHasVersion(planner.getAdmin(), TABLE_AUTH_VERSION)) {
            plan = new AddIndexPlan(idGen, fullPlanName, tableName, planner);
        } else {
            plan = new DeployTableMetadataPlan(idGen, fullPlanName, planner);
        }
        tableName = getRealTableName(tableName, plan.getMetadata());

        /*
         * Create the index, not-yet-visible
         */
        try {
            plan.addTask(new StartAddIndex(plan,
                                           indexName,
                                           tableName,
                                           indexedFields,
                                           description));

            /*
             * Wait for the added index to be populated. This may take a while.
             */
            final ParallelBundle bundle = new ParallelBundle();
            for (RepGroupId id : topology.getRepGroupIds()) {
                bundle.addTask(new WaitForAddIndex(plan,
                                                   id,
                                                   indexName,
                                                   tableName));
            }
            plan.addTask(bundle);

            /*
             * Complete the job, make the index visible
             */
            plan.addTask(new CompleteAddIndex(plan,
                                              indexName,
                                              tableName));
        } catch (IllegalArgumentException iae) {
            throw new IllegalCommandException
                ("Failed to add index: " + iae.getMessage(), iae);
        }

        return plan;
    }

    /**
     * Creates a plan to remove an index.
     * @param override
     */
    @SuppressWarnings("unused")
    static DeployTableMetadataPlan
        createRemoveIndexPlan(AtomicInteger idGen,
                              String planName,
                              Planner planner,
                              Topology topology,
                              String indexName,
                              String tableName,
                              boolean override) {
        checkTable(tableName);
        checkIndex(indexName);

        final String fullPlanName = makeName(planName, tableName, indexName);
        final DeployTableMetadataPlan plan;
        if (Utils.storeHasVersion(planner.getAdmin(), TABLE_AUTH_VERSION)) {
            plan = new RemoveIndexPlan(idGen, fullPlanName, tableName, planner);
        } else {
            plan = new DeployTableMetadataPlan(idGen, fullPlanName, planner);
        }

        checkTextIndexForRemoval
            (plan, planner.getAdmin(), indexName, tableName, override);

        tableName = getRealTableName(tableName, plan.getMetadata());
        try {
            plan.addTask(RemoveIndex.newInstance(plan, indexName, tableName));
        } catch (IllegalArgumentException iae) {
            throw new IllegalCommandException
                ("Failed to remove index: " + iae.getMessage(), iae);
        }

        return plan;
    }

    /**
     * Creates a plan to add a text index.
     * @param override
     */
    static DeployTableMetadataPlan
        createAddTextIndexPlan(AtomicInteger idGen,
                               String planName,
                               Planner planner,
                               String indexName,
                               String tableName,
                               AnnotatedField[] ftsFields,
                               Map<String,String> properties,
                               String description,
                               boolean override) {

        checkTable(tableName);
        checkIndex(indexName);
        if (ftsFields == null || ftsFields.length == 0) {
            throw new IllegalCommandException
                ("The set of text-indexed fields cannot be null or empty");
        }

        final Admin admin = planner.getAdmin();
        final Parameters p = admin.getCurrentParameters();

        ParameterMap pm = Utils.verifyAndGetSearchParams(p);
        final String esClusterName = pm.getOrDefault
            (ParameterState.SN_SEARCH_CLUSTER_NAME).asString();

        if ("".equals(esClusterName)) {
            throw new IllegalCommandException
                ("An Elasticsearch cluster must be registered with the store "+
                 " before a text index can be created.");
        }


        /* now ready to deploy the plan to create text index */
        final DeployTableMetadataPlan plan =
            new DeployTableMetadataPlan(idGen,
                                        makeName(planName, tableName, indexName),
                                        planner);
        tableName = getRealTableName(tableName, plan.getMetadata());

        /*
         * Create the index, not-yet-visible
         */
        try {

            /*
             * StartAddTextIndex's constructor will verify that there is not
             * already an index with the given name.
             */
            plan.addTask(new StartAddTextIndex(plan,
                                               indexName,
                                               tableName,
                                               ftsFields,
                                               properties,
                                               description));

            /*
             * If a stale ES index with the target index's name exists, balk,
             * or remove it straightaway, depending on the value of override.
             * We already know that the Admin's table metadata does not know
             * about it.
             */
            checkTextIndexForCreation(admin, indexName, tableName, override);

            /* TODO: do we want to wait for the index to be ready? */
            plan.addTask(new CompleteAddIndex(plan,
                                              indexName,
                                              tableName));
        } catch (IllegalArgumentException iae) {
            throw new IllegalCommandException
                ("Failed to add index: " + iae.getMessage(), iae);
        }

        return plan;
    }

    public static DeployTableMetadataPlan createBroadcastTableMDPlan
        (AtomicInteger idGen,
         Planner planner) {
        final DeployTableMetadataPlan plan =
            new DeployTableMetadataPlan(idGen, "Broadcast Table MD", planner);

        plan.addTask(new UpdateMetadata<>(plan));
        return plan;
    }

    public static TableImpl getAndCheckTable(String name, TableMetadata md) {
        if (md != null) {
            final TableImpl table = md.getTable(name, false);
            if (table != null) {
                return table;
            }
        }
        throw new IllegalCommandException(
            "Table does not exist: " + name);
    }

    public static List<? extends KVStorePrivilege>
        getRemoveTableRequiredPrivs(ResourceOwner tableOwner,
                                    boolean toRemoveIndex,
                                    long tableId) {

        final ResourceOwner currentUser = SecurityUtils.currentUserAsOwner();
        if ((currentUser != null) && (currentUser.equals(tableOwner))) {
            /* The owner, checks only USRVIEW to ensure authentication */
            return SystemPrivilege.usrviewPrivList;
        }
        final List<KVStorePrivilege> privsToCheck =
            new ArrayList<KVStorePrivilege>();
        if (toRemoveIndex) {
            privsToCheck.add(new TablePrivilege.DropIndex(tableId));
        }
        privsToCheck.add(SystemPrivilege.DROP_ANY_TABLE);
        return privsToCheck;
    }

    static Set<String> getInvolvedRoles(long tableId, SecurityMetadata secMd) {
        if (secMd == null) {
            return Collections.emptySet();
        }
        Set<String> involvedRoles = null;
        for (RoleInstance role : secMd.getAllRoles()) {
            for (KVStorePrivilege priv : role.getPrivileges()) {
                if ((priv instanceof TablePrivilege) &&
                     ((TablePrivilege) priv).getTableId() == tableId) {

                    if (involvedRoles == null) {
                        involvedRoles = new HashSet<>();
                    }
                    involvedRoles.add(role.name());
                }
            }
        }

        if (involvedRoles == null) {
            return Collections.emptySet();
        }
        return involvedRoles;
    }

    static Set<TableImpl> getOwnedTables(TableMetadata tableMd,
                                         SecurityMetadata secMd,
                                         String userName) {
        if (tableMd == null || tableMd.getTables().isEmpty()) {
            return Collections.emptySet();
        }
        if (secMd == null || secMd.getUser(userName) == null) {
            return Collections.emptySet();
        }

        Set<TableImpl> tables = null;
        final KVStoreUser user = secMd.getUser(userName);
        final ResourceOwner owner =
            new ResourceOwner(user.getElementId(), user.getName());

        for (Table table : tableMd.getTables().values()) {
            final TableImpl tableImpl = (TableImpl)table;

            if (owner.equals(tableImpl.getOwner())) {
                if (tables == null) {
                    tables = new HashSet<>();
                }
                addTablesToSet(owner, tableImpl, tables);
            }
        }

        if (tables == null) {
            return Collections.emptySet();
        }
        return tables;
    }

    private static void addTablesToSet(ResourceOwner owner,
                                       TableImpl table,
                                       Set<TableImpl> tables) {
        if (tables == null) {
            throw new IllegalArgumentException(
                "Set to store tables should not be null");
        }
        tables.add(table);
        for (Table table1 : table.getChildTables().values()) {
            addTablesToSet(owner, (TableImpl) table1, tables);
        }
    }

    private static void checkTable(String tableName) {
        if (tableName == null) {
            throw new IllegalCommandException("Table path cannot be null");
        }
    }

    private static void checkIndex(String indexName) {
        if (indexName == null) {
            throw new IllegalCommandException("Index name cannot be null");
        }
    }

    /**
     * Returns the real table name for the table name, making it insensitive to
     * case.  If the table does not exist, return the argument and allow the
     * caller to continue.  This is called for plans where the table may or may
     * not exist.
     */
    private static String getRealTableName(String tableName, TableMetadata md) {
        if (md != null) {
            final TableImpl table = md.getTable(tableName, false);
            if (table != null) {
                return table.getFullName();
            }
        }
        return tableName;
    }

    /**
     * Create a plan or task name that puts more information in the log stream.
     */
    public static String makeName(String name, String tableName,
                                  String indexName) {
        return makeName(name, tableName, indexName, null);

    }

    /**
     * Create a task name that puts more information in the log stream.
     */
    public static String makeName(String name, String tableName,
                                  String indexName, String targetName) {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        sb.append(":");
        sb.append(tableName);
        if (indexName != null) {
            sb.append(":");
            sb.append(indexName);
        }
        if (targetName != null) {
            sb.append(" on ");
            sb.append(targetName);
        }
        return sb.toString();
    }

    /*
     * Helper method for createAddTextIndexPlan.  Enforces the rule about
     * removing an existing text index that has the same name as the one we are
     * about to create.  The override boolean controls whether the offending
     * index is actually removed or not.
     */
    private static void checkTextIndexForCreation(Admin admin,
                                                  String indexName,
                                                  String tableName,
                                                  boolean override) {

        final Parameters p = admin.getCurrentParameters();
        final ParameterMap pm = Utils.verifyAndGetSearchParams(p);
        final String esClusterName = pm.getOrDefault
            (ParameterState.SN_SEARCH_CLUSTER_NAME).asString();
        final String esMembers = pm.getOrDefault
            (ParameterState.SN_SEARCH_CLUSTER_MEMBERS).asString();
        final String esIndexName =
            TextIndexFeeder.deriveESIndexName(p.getGlobalParams()
                                              .getKVStoreName(),
                                              tableName,
                                              indexName);

        try (final TransportClient esClient =
             ElasticsearchHandler.getTransportClient(esClusterName,
                                                     esMembers,
                                                     admin.getLogger())) {

            if (ElasticsearchHandler.existESIndex(esIndexName,
                                                  esClient.admin())) {

                if (override) {
                    /* Ensure that the index does not exist. */
                    ElasticsearchHandler.deleteESIndex(esIndexName,
                                                       esClient.admin());
                } else {
                    throw new IllegalCommandException
                        ("The command cannot proceed, because an index " +
                         "by the name " + esIndexName + " already exists in " +
                         "the Elasticsearch cluster.  This index does not " +
                         "correspond to a NoSQL Database text index " +
                         "belonging to this store.  You may force its " +
                         "removal by re-issuing the command with OVERRIDE.");
                }
            }

        } catch (Exception e) {
            if (e instanceof IllegalCommandException) {
                throw e;
            }
            /* Don't advise to use OVERRIDE in this case. */
            throw new IllegalCommandException
                ("The command cannot proceed, because the " +
                 "Elasticsearch cluster is not available.", e);
        }
    }

    /*
     * Helper method for createRemoveIndexPlan.  Before dropping a text index,
     * ensure ES cluster is healthy before proceeding with the drop index plan.
     */
    private static void checkTextIndexForRemoval(AbstractPlan plan,
                                                 Admin admin,
                                                 String indexName,
                                                 String tableName,
                                                 boolean override) {

        /* If the override property is true, then don't bother checking. */
        if (override) {
            return;
        }

        /* Consult metadata to determine whether the index is a text index. */
        final TableMetadata md = plan.getAdmin().
            getMetadata(TableMetadata.class, MetadataType.TABLE);
        final TableImpl tbl =
            (md != null) ? md.getTable(tableName, true) : null;
        final Index idx = (tbl != null) ? tbl.getIndex(indexName) : null;

        if (idx == null || !idx.getType().equals(Index.IndexType.TEXT)) {
            return;
        }

        /* If it's a text index, then verify cluster health. */
        final Parameters p = admin.getCurrentParameters();
        final ParameterMap pm = Utils.verifyAndGetSearchParams(p);
        final String esClusterName = pm.getOrDefault
            (ParameterState.SN_SEARCH_CLUSTER_NAME).asString();
        final String esMembers = pm.getOrDefault
            (ParameterState.SN_SEARCH_CLUSTER_MEMBERS).asString();

        /*
         * ES cluster must be "healthy."  See isClusterHealthy for
         * a discussion of what "healthy" means.
         */
        final String errForm =
            "The DROP operation cannot proceed, because it involves " +
            "the text index %s,%nand %s.%n" +
            "Dropping text indexes in this situation can cause%n" +
            "inconsistencies between NoSQL Database and Elasticsearch.%n" +
            "If you wish to proceed despite this concern, issue %n" +
            "the command 'DROP INDEX %s on %s OVERRIDE' to eliminate%n" +
            "the offending text index.  Otherwise, please try%n" +
            "the command again when the Elasticsearch cluster is healthy.";

        try (final TransportClient esClient =
             ElasticsearchHandler.getTransportClient(esClusterName,
                                                     esMembers,
                                                     admin.getLogger())) {

            if (!ElasticsearchHandler.isClusterHealthy
                (esMembers, esClient.admin())) {

                throw new IllegalCommandException
                    (String.format
                     (errForm, indexName,
                      "the Elasticsearch cluster's health level is low",
                      indexName, tableName));
            }
        } catch (Exception e) {
            if (e instanceof IllegalCommandException) {
                throw e;
            }
            throw new IllegalCommandException
                (String.format
                 (errForm, indexName,
                  "the Elasticsearch cluster is not available",
                  indexName, tableName), e);
        }
    }
}
