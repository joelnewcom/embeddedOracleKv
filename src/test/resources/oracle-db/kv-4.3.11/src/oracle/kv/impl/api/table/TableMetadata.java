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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import oracle.kv.impl.admin.IllegalCommandException;
import oracle.kv.impl.api.table.IndexImpl.AnnotatedField;
import oracle.kv.impl.api.table.IndexImpl.IndexStatus;
import oracle.kv.impl.api.table.TableImpl.TableStatus;
import oracle.kv.impl.metadata.Metadata;
import oracle.kv.impl.metadata.MetadataInfo;
import oracle.kv.impl.metadata.MetadataKey;
import oracle.kv.impl.security.ResourceOwner;
import oracle.kv.table.Index;
import oracle.kv.table.Table;
import oracle.kv.table.TimeToLive;

/**
 * This is internal implementation that wraps Table and Index metadata
 * operations such as table/index creation, etc.
 *
 * TableMetadata stores tables in a tree.  The top level is a map from
 * name (String) to Table and contains top-level tables only.  Each top-level
 * table may or may not contain child tables.  When this class is serialized
 * the entire tree of Table objects, along with their contained Index objects,
 * is serialized.
 *
 * When a table lookup is performed it must be done top-down.  First the lookup
 * walks to the "root" of the metadata structure, which is the map contained in
 * this instance.  For top-level tables the lookup is a simple get.  For child
 * tables the code unwinds down the stack of parents to get the child.
 *
 * When a table is first inserted into TableMetadata it is assigned a
 * numeric id. Ids are allocated from the keyId member.
 *
 * Note that this implementation is not synchronized. If multiple threads
 * access a table metadata instance concurrently, and at least one of the
 * threads modifies the table metadata structurally, it must be synchronized
 * externally.
 */
public class TableMetadata implements Metadata<TableChangeList>, Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, Table> tables =
        new TreeMap<String, Table>(FieldComparator.instance);

    private int seqNum = Metadata.EMPTY_SEQUENCE_NUMBER;

    private long keyId = INITIAL_KEY_ID;

    private static final int INITIAL_KEY_ID = 1;

    /*
     * Record of changes to the metadata. If null no changes will be kept.
     */
    private final List<TableChange> changeHistory;

    /**
     * Construct a table metadata object. If keepChanges is true any changes
     * made to are recorded and can be accessed through the getMetadataInfo()
     * interface.
     *
     * @param keepChanges
     */
    public TableMetadata(boolean keepChanges) {
        changeHistory = keepChanges ? new LinkedList<TableChange>() : null;
    }

    public TableImpl addTable(String name,
            String parentName,
            List<String> primaryKey,
            List<String> shardKey,
            FieldMap fieldMap,
            boolean r2compat,
            int schemaId,
            String description,
            ResourceOwner owner) {

        return addTable(name, parentName, primaryKey,
                        null, /* primary key sizes */
                        shardKey, fieldMap,
                        null /* TTL*/,
                        r2compat, schemaId, description, owner);
    }

    public TableImpl addTable(String name,
                              String parentName,
                              List<String> primaryKey,
                              List<Integer> primaryKeySizes,
                              List<String> shardKey,
                              FieldMap fieldMap,
                              TimeToLive ttl,
                              boolean r2compat,
                              int schemaId,
                              String description,
                              ResourceOwner owner) {
        TableImpl table = insertTable(name, parentName,
                                      primaryKey, primaryKeySizes, shardKey,
                                      fieldMap,
                                      ttl,
                                      r2compat, schemaId,
                                      description,
                                      owner, false);
        addTableChange(table);
        return table;
    }

    public TableImpl addSysTable(String name,
                                 String parentName,
                                 List<String> primaryKey,
                                 List<Integer> primaryKeySizes,
                                 List<String> shardKey,
                                 FieldMap fieldMap,
                                 TimeToLive ttl,
                                 boolean r2compat,
                                 int schemaId,
                                 String description,
                                 ResourceOwner owner) {
        TableImpl table = insertTable(name, parentName,
                                      primaryKey, primaryKeySizes, shardKey,
                                      fieldMap,
                                      ttl,
                                      r2compat, schemaId,
                                      description,
                                      owner, true);
        addTableChange(table);
        return table;
    }

    private void addTableChange(TableImpl table) {
        bumpSeqNum();
        if (changeHistory != null) {
            changeHistory.add(new AddTable(table, seqNum));
        }
    }

    /**
     * Drops a table. If the table has indexes or child tables an
     * IllegalArgumentException is thrown. If markForDelete is true the table's
     * status is set to DELETING and is not removed.
     *
     * @param tableName the table name
     * @param markForDelete if true mark the table as DELETING
     *
     * @returns the removed table
     */
    public void dropTable(String tableName, boolean markForDelete) {
        removeTable(tableName, markForDelete);

        bumpSeqNum();
        if (changeHistory != null) {
            changeHistory.add(new DropTable(tableName,
                                            markForDelete, seqNum));
        }
    }

    /**
     * Evolves a table using new fields but only if it's not already been done
     * and if the supplied version indicates that the evolution started with
     * the latest table version.
     *
     * If this operation was retried the evolution may have already been
     * applied.  Check field equality and if equal, consider the evolution
     * done.
     *
     * @return true if the evolution happens, false otherwise
     *
     * @throws IllegalCommandException if an attempt is made to evolve a version
     * other than the latest table version
     */
    public boolean evolveTable(TableImpl table, int tableVersion,
                               FieldMap fieldMap, TimeToLive ttl) {

        if (table.isSystemTable()) {
            throw new IllegalCommandException
                ("Cannot evolve table " +
                 makeQualifiedName(null, table.getName()));
        }
        if (fieldMap.equals(table.getFieldMap())) {
            if (TableImpl.compareTTL(ttl, table.getDefaultTTL())) {
                return false;
            }
        }

        if (tableVersion != table.numTableVersions()) {
            throw new IllegalCommandException
                ("Table evolution must be performed on the latest version, " +
                 "version supplied is " + tableVersion + ", latest is " +
                 table.numTableVersions());
        }

        table.evolve(fieldMap, ttl);
        bumpSeqNum();
        if (changeHistory != null) {
            changeHistory.add(new EvolveTable(table, seqNum));
        }
        return true;
    }

    public void addIndex(String indexName,
                         String tableName,
                         List<String> fields,
                         String description) {
        final IndexImpl index = insertIndex(indexName, tableName,
                                            fields, description);
        bumpSeqNum();
        if (changeHistory != null) {
            changeHistory.add(new AddIndex(index, seqNum));
        }
    }

    public void addTextIndex(String indexName,
                             String tableName,
                             List<AnnotatedField> fields,
                             Map<String,String> properties,
                             String description) {
        List<String> fieldNames = new ArrayList<String>(fields.size());
        Map<String,String> annotations = new HashMap<String,String>(fields.size());
        IndexImpl.populateMapFromAnnotatedFields(fields,
                                                 fieldNames,
                                                 annotations);

        final IndexImpl index = insertTextIndex(indexName, tableName,
                                                fieldNames, annotations,
                                                properties, description);

        bumpSeqNum();
        if (changeHistory != null) {
            changeHistory.add(new AddIndex(index, seqNum));
        }
    }

    public void dropIndex(String indexName, String tableName) {
        if (removeIndex(indexName, tableName)) {
            bumpSeqNum();
            if (changeHistory != null) {
                changeHistory.add(new DropIndex(indexName,
                                                tableName,
                                                seqNum));
            }
        }
    }

    public boolean updateIndexStatus(String indexName,
                                     String tableName,
                                     IndexStatus status) {
        final IndexImpl index = changeIndexStatus(indexName, tableName,
                                                  status);
        if (index != null) {
            bumpSeqNum();
            if (changeHistory != null) {
                changeHistory.add(new UpdateIndexStatus(index, seqNum));
            }
            return true;
        }
        return false;
    }

    /*
     * Add the table described.  It must not exist or an exception is thrown.
     * If it has a parent the parent must exist.
     */
    TableImpl insertTable(String name,
                          String parentName,
                          List<String> primaryKey,
                          List<Integer> primaryKeySizes,
                          List<String> shardKey,
                          FieldMap fields,
                          TimeToLive ttl,
                          boolean r2compat,
                          int schemaId,
                          String description,
                          ResourceOwner owner,
                          boolean sysTable) {

        TableImpl table = null;

        if (r2compat) {
            verifyIdNotUsed(name);
        }

        if (parentName != null) {
            final TableImpl parent = getTable(parentName,
                                              true);
            if (parent.childTableExists(name)) {
                throw new IllegalArgumentException
                    ("Cannot create table.  Table exists: " +
                     makeQualifiedName(name, parentName));
            }

            if (parent.isSystemTable() != sysTable) {
                throw new IllegalArgumentException
                    ("Cannot create table " + name + ". It must" +
                     ((sysTable) ? " not" : "") + " be a system table, " +
                     "because its parent is " +
                     ((sysTable) ? "" : " not") + " a system table");
            }
            table = TableImpl.createTable(name, parent,
                                          primaryKey, primaryKeySizes, shardKey,
                                          fields, r2compat, schemaId,
                                          description, true, owner, ttl,
                                          sysTable);
            table.setId(allocateId());
            parent.getMutableChildTables().put(name, table);
        } else {
            if (tables.containsKey(name)) {
                throw new IllegalArgumentException
                    ("Cannot create table.  Table exists: " + name);
            }
            table = TableImpl.createTable(name, null,
                                          primaryKey, primaryKeySizes, shardKey,
                                          fields, r2compat, schemaId,
                                          description, true, owner, ttl,
                                          sysTable);
            table.setId(allocateId());
            tables.put(name, table);
        }
        return table;

    }

    /*
     * Evolve the table described.  It must not exist or an exception is thrown.
     */
    TableImpl evolveTable(String tableName, FieldMap fields, TimeToLive ttl) {
        final TableImpl table = getTable(tableName, true);
        table.evolve(fields, ttl);
        return table;
    }

    /**
     * Removes a table. If the table has indexes or child tables an
     * IllegalArgumentException is thrown. If markForDelete is true the table's
     * status is set to DELETING and is not removed.
     *
     * @param tableName the table name
     * @param markForDelete if true mark the table as DELETING
     *
     * @return the removed table
     */
    Table removeTable(String tableName, boolean markForDelete) {
        final TableImpl table = checkForRemove(tableName, true);
        if (markForDelete) {
            table.setStatus(TableStatus.DELETING);
            return table;
        }
        Table parent = table.getParent();
        if (parent != null) {
            ((TableImpl)parent).getMutableChildTables().remove(table.getName());
        } else {
            /* a top-level table */
            tables.remove(table.getName());
        }
        return table;
    }

    /**
     * Called to see if it is ok to remove this table.  If mustExist
     * is true then throw if the table does not exists.
     */
    public TableImpl checkForRemove(String tableName,
                                    boolean mustExist) {
        final TableImpl table = getTable(tableName, mustExist);
        String qname = makeQualifiedName(null, tableName);
        if (table != null) {
            if (table.isSystemTable()) {
                throw new IllegalCommandException
                    ("Cannot remove system table: " + qname);
            }

            if (!table.getChildTables().isEmpty()) {
                throw new IllegalCommandException
                    ("Cannot remove " + qname +
                     ", it is still referenced by " +
                     "child tables");
            }
        }
        return table;
    }

    IndexImpl insertIndex(String indexName,
                          String tableName,
                          List<String> fields,
                          String description) {
        final TableImpl table = getTable(tableName, true);
        if (table.isSystemTable()) {
            throw new IllegalCommandException
                ("Cannot add index " + indexName + " on system table: " +
                 makeQualifiedName(null, tableName));
        }

        if (table.getIndex(indexName) != null) {
            throw new IllegalArgumentException
                ("Index exists: " + indexName + " on table: " +
                 makeQualifiedName(null, tableName));
        }
        IndexImpl index = new IndexImpl(indexName, table, fields,
                                        description);
        index.setStatus(IndexStatus.POPULATING);
        table.addIndex(index);
        return index;
    }

    boolean removeIndex(String indexName, String tableName) {
        final TableImpl table = getTable(tableName, true);
        if (table.isSystemTable()) {
            throw new IllegalCommandException
                ("Cannot remove index " + indexName + " on system table: " +
                 makeQualifiedName(null, tableName));
        }

        Index index = table.getIndex(indexName);
        if (index == null) {
            throw new IllegalArgumentException
                ("Index does not exist: " + indexName + " on table: " +
                 makeQualifiedName(null, tableName));
        }
        table.removeIndex(indexName);

        return true;
    }

    /*
     * Update the index status to the desired status.  If a change was made
     * return the Index, if the status is unchanged return null, allowing
     * this operation to be an idempotent no-op.
     */
    IndexImpl changeIndexStatus(String indexName,
                                String tableName,
                                IndexStatus status) {
        final TableImpl table = getTable(tableName, true);

        IndexImpl index = (IndexImpl) table.getIndex(indexName);
        if (index == null) {
            throw new IllegalArgumentException
                ("Index does not exist: " + indexName + " on table: " +
                 makeQualifiedName(null, tableName));
        }
        if (index.getStatus() == status) {
            return null;
        }
        index.setStatus(status);
        return index;
    }

    IndexImpl insertTextIndex(String indexName,
                              String tableName,
                              List<String> fields,
                              Map<String, String> annotations,
                              Map<String, String> properties,
                              String description) {
        final TableImpl table = getTable(tableName, true);
        if (table.isSystemTable()) {
            throw new IllegalCommandException
                ("Cannot add text index " + indexName + " on table: " +
                 makeQualifiedName(null, tableName));
        }

        if (table.getTextIndex(indexName) != null) {
            throw new IllegalArgumentException
                ("Text Index exists: " + indexName + " on table: " +
                 makeQualifiedName(null, tableName));
        }
        IndexImpl index = new IndexImpl(indexName, table, fields,
                                        annotations, properties, description);
        index.setStatus(IndexStatus.POPULATING);
        table.addIndex(index);
        return index;
    }

    /**
     * Return the named table.
     *
     * @param tableName is a "." separated path to the table name, e.g.
     * parent.child.target.  For top-level tables it is a single
     * component
     */
    public TableImpl getTable(String tableName, boolean mustExist) {

        String path[] = TableImpl.parseFullName(tableName);
        return getTable(path, mustExist);
    }

    public TableImpl getTable(String tableName) {
        return getTable(tableName, false);
    }

    public TableImpl getTable(String[] tableName) {
        return getTable(tableName, false);
    }

    public TableImpl getTable(String[] path, boolean mustExist) {

        if (path == null || path.length == 0) {
            return null;
        }

        String firstKey = path[0];
        TableImpl targetTable = findTable(firstKey);

        if (path.length > 1) {
            for (int i = 1; i < path.length && targetTable != null; i++) {
                try {
                    targetTable = getChildTable(path[i], targetTable);
                } catch (IllegalArgumentException ignored) {
                    targetTable = null;
                    break;
                }
            }
        }

        if (targetTable == null && mustExist) {
            throw new IllegalArgumentException
               ("Table: " + makeQualifiedName(path) +
                " does not exist in " + this);
        }
        return targetTable;
    }

    public boolean tableExists(String name, String tableName) {
        StringBuilder sb = new StringBuilder();
        if (tableName != null) {
            sb.append(tableName);
            sb.append(TableImpl.SEPARATOR);
        }
        if (name != null) {
            sb.append(name);
        }
        return (getTable(sb.toString()) != null);
    }

    /**
     * Returns the specified Index or null if it, or its containing table
     * does not exist.
     */
    public Index getIndex(String tableName, String indexName) {
        TableImpl table = getTable(tableName);
        if (table != null) {
            return table.getIndex(indexName);
        }
        return null;
    }

    public Index getTextIndex(String tableName, String indexName) {
        TableImpl table = getTable(tableName);
        if (table != null) {
            return table.getTextIndex(indexName);
        }
        return null;
    }

    public static String makeQualifiedName(TableImpl table) {
        return makeQualifiedName(null, table.getFullName());
    }

    public static String makeQualifiedName(String[] pathName) {
        StringBuilder sb = new StringBuilder();
        for (String step : pathName) {
            sb.append(step);
        }
        return makeQualifiedName(null, sb.toString());
    }

    /**
     * Create a string that uniquely identifies a table for use in error
     * messages.  Format is [parentName][.]name
     */
    public static String makeQualifiedName(String name,
                                           String parentName) {
        StringBuilder sb = new StringBuilder();
        if (parentName != null) {
            sb.append(parentName);
            if (name != null) {
                sb.append(TableImpl.SEPARATOR);
            }
        }
        if (name != null) {
            sb.append(name);
        }
        return sb.toString();
    }

    /**
     * Return the named child table.
     */
    public TableImpl getChildTable(String tableName, Table parent) {
        return (TableImpl) parent.getChildTable(tableName);
    }

    /*
     * Get a table from TableMetadataKey.  This is used by RepNodes to return
     * tables requested by clients.  In this path it's necessary to filter out
     * created, but not-yet-populated indexes.
     */
    public TableImpl getTable(TableMetadataKey mdKey) {
        TableImpl table = getTable(mdKey.getTableName());
        if (table != null && table.getIndexes().size() > 0) {
            /* clone, filter */
            table = table.clone();

            for(Iterator<Map.Entry<String, Index>> it =
                    table.getMutableIndexes().entrySet().iterator();
                it.hasNext(); ) {
                Map.Entry<String, Index> entry = it.next();
                if (!((IndexImpl)entry.getValue()).getStatus().isReady()) {
                    it.remove();
                }
            }
        }
        return table;
    }

    /**
     * Return all top-level tables.
     */
    public Map<String, Table> getTables() {
        return tables;
    }

    /**
     * Adds all table names, parent and child to the list.  Child tables are
     * listed before parent tables because the iteration is depth-first.  This
     * simplifies code that does things like removing all tables or code that
     * depends on this order.  If other orders are desirable a parameter or other
     * method could be added to affect the iteration.
     */
    public List<String> listTables() {
        final List<String> tableList = new ArrayList<String>();
        iterateTables(new TableMetadataIteratorCallback() {
                @Override
                public boolean tableCallback(Table table) {
                    tableList.add(table.getFullName());
                    return true;
                }
            });
        return tableList;
    }

    /**
     * Returns the number of tables in the structure, including
     * child tables.
     */
    private int numTables() {
        final int[] num = new int[1];
        iterateTables(new TableMetadataIteratorCallback() {
                @Override
                public boolean tableCallback(Table table) {
                    ++num[0];
                    return true;
                }
            });
        return num[0];
    }

    /**
     * Returns true if there are no tables defined.
     *
     * @return true if there are no tables defined
     */
    public boolean isEmpty() {
        return tables.isEmpty();
    }

    /*
     * Find the named top-level table.
     */
    private TableImpl findTable(String key) {
        return (TableImpl) tables.get(key);
    }

    /**
     * Return all text indexes
     */
    public List<Index> getTextIndexes() {

        final List<Index> textIndexes = new ArrayList<Index>();

        iterateTables(new TableMetadataIteratorCallback() {
                @Override
                public boolean tableCallback(Table table) {
                    textIndexes.addAll
                        (table.getIndexes(Index.IndexType.TEXT).values());
                    return true;
                }
            });

        return textIndexes;
    }

    /**
     * Convenience method for getting all text index names.
     */
    public Set<String> getTextIndexNames() {
    	final Set<String> textIndexNames = new HashSet<String>();
    	for (Index ti : getTextIndexes()) {
            textIndexNames.add(ti.getName());
    	}
    	return textIndexNames;
    }

    private void bumpSeqNum() {
        seqNum++;
    }

    /*
     * Bump and return a new table id. Verify that the string version of
     * the id doesn't already exist as a table name.  If so, bump again.
     */
    private long allocateId() {
        while (true) {
            ++keyId;
            try {
                verifyIdNotUsed(TableImpl.createIdString(keyId));
                return keyId;
            } catch (IllegalArgumentException iae) {
                /* try the next id */
            }
        }
    }

    /* -- From Metadata -- */

    @Override
    public MetadataType getType() {
        return MetadataType.TABLE;
    }

    @Override
    public int getSequenceNumber() {
        return seqNum;
    }

    @Override
    public TableChangeList getChangeInfo(int startSeqNum) {
        return new TableChangeList(seqNum, getChanges(startSeqNum));
    }

    /* -- Change support methods -- */

    private List<TableChange> getChanges(int startSeqNum) {

        /* Skip if we are out of date, or don't have changes */
        if ((startSeqNum >= seqNum) ||
            (changeHistory == null) ||
            changeHistory.isEmpty()) {
            return null;
        }

        /* Also skip if they are way out of date (or not initialized) */
        if (startSeqNum < changeHistory.get(0).getSequenceNumber()) {
            return null;
        }

        List<TableChange> list = null;

        for (TableChange change : changeHistory) {
            if (change.getSequenceNumber() > startSeqNum) {
                if (list == null) {
                    list = new LinkedList<TableChange>();
                }
                list.add(change);
            }
        }
        return list;
    }

    /**
     * Updates the metadata data from an info object. Returns true
     * if the table metadata was modified.
     *
     * @param metadataInfo info object to update from
     * @return true if the table metadata was modified
     */
    public boolean update(MetadataInfo metadataInfo) {

        if (metadataInfo instanceof TableChangeList) {
            return apply((TableChangeList)metadataInfo);
        }
        throw new IllegalArgumentException("Unknow metadata info: " +
                                           metadataInfo);
    }

    private boolean apply(TableChangeList changeList) {
        if (changeList.isEmpty()) {
            return false;
        }

        final int origSeqNum = seqNum;

        for (TableChange change : changeList) {
            if (change.getSequenceNumber() <= seqNum) {
                break;
            }
            if (change.getSequenceNumber() > (seqNum + 1)) {
                break;
            }
            if (!change.apply(this)) {
                break;
            }
            seqNum = change.getSequenceNumber();
            if (changeHistory != null) {
                changeHistory.add(change);
            }
        }
        return origSeqNum != seqNum;
    }

    /**
     * Creates a copy of this TableMetadata object.
     *
     * @return the new TableMetadata instance
     */
    public TableMetadata getCopy() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
            ObjectOutputStream oos = new ObjectOutputStream(bos) ;
            oos.writeObject(this);
            oos.close();

            ByteArrayInputStream bis =
                new ByteArrayInputStream(bos.toByteArray()) ;
            ObjectInputStream ois = new ObjectInputStream(bis);

            return (TableMetadata)ois.readObject();
        } catch (IOException ioe) {
            throw new IllegalStateException("Unexpected exception", ioe);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unexpected exception", e);
        }
    }

    @Override
    public String toString() {
        return "TableMetadata[" + seqNum + ", " + tables.size() + ", " +
            ((changeHistory == null) ? "-" : changeHistory.size()) + "]";
    }

    /**
     * Compares two TableMetadata instances by comparing all tables, including
     * child tables.  This might logically be implemented as an override of
     * equals but that might mean adding hashCode() to avoid warnings and
     * that's not necessary.  If anyone ever needs a true equals() overload
     * then this can change.
     *
     * @return true if the objects have the same content, false otherwise.
     */
    public boolean compareMetadata(final TableMetadata omd) {
        int num = numTables();
        if (num == omd.numTables()) {
            final int[] numCompared = new int[1];
                iterateTables(new TableMetadataIteratorCallback() {
                        @Override
                        public boolean tableCallback(Table table) {
                            if (!existsAndEqual((TableImpl) table, omd)) {
                                return false;
                            }
                            ++numCompared[0];
                            return true;
                        }
                    });
                return numCompared[0] == num;
        }
        return false;
    }

    /**
     * Iterates all tables and ensures that the string version of the
     * id for the table doesn't match the idString. Throws if it
     * exists.  This is called when creating a new table in r2compat mode.
     */
    private void verifyIdNotUsed(final String idString) {
        iterateTables(new TableMetadataIteratorCallback() {
                @Override
                public boolean tableCallback(Table table) {
                    String tableId = ((TableImpl)table).getIdString();
                    if (tableId.equals(idString)) {
                        throw new IllegalArgumentException
                            ("Cannot create a table overlay with the name " +
                             idString + ", it exists as a table Id");
                    }
                    return true;
                }
            });
    }

    /**
     * Returns true if the table name exists in the TableMetadata and
     * the two tables are equal.
     */
    private static boolean existsAndEqual(TableImpl table,
                                          TableMetadata md) {
        TableImpl otherTable = md.getTable(table.getFullName());
        if (otherTable != null && table.equals(otherTable)) {

            /*
             * Check child tables individually.  Table equality does not
             * consider children.
             */
            for (Table child : table.getChildTables().values()) {
                if (!existsAndEqual((TableImpl) child, md)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static class TableMetadataKey implements MetadataKey, Serializable {
        private static final long serialVersionUID = 1L;
        private final String tableName;

        public TableMetadataKey(final String tableName) {
            this.tableName = tableName;
        }

        public String getTableName() {
            return tableName;
        }

        public MetadataKey getMetadataKey() {
            return this;
        }

        /*
         * For debugging
         */
        @Override
        public String toString() {
            return "TableMetadataKey[" +
                   (tableName != null ? tableName : "null") + "]";
        }
    }

    /**
     * Iterate over all tables, calling back to the callback for each.
     */
    public void iterateTables(TableMetadataIteratorCallback callback) {
        for (Table table : getTables().values()) {
            if (!iterateTables(table, callback)) {
                break;
            }
        }
    }

    /**
     * Implements iteration of all tables, depth-first (i.e. child tables are
     * visited before parents.
     */
    private static boolean
        iterateTables(Table table, TableMetadataIteratorCallback callback) {
        for (Table child : table.getChildTables().values()) {
            if (!iterateTables(child, callback)) {
                return false;
            }
        }
        if (!callback.tableCallback(table)) {
            return false;
        }
        return true;
    }

    /**
     * An interface used for operations that need to iterate the entire tree of
     * metadata.
     */
    public interface TableMetadataIteratorCallback {

        /**
         * Returns true if the iteration should continue, false if not.
         */
        boolean tableCallback(Table t);
    }
}
