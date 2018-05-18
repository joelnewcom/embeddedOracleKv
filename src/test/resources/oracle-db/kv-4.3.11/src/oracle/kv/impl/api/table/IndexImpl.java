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

import static oracle.kv.impl.api.table.TableImpl.SEPARATOR;
import static oracle.kv.impl.api.table.TableJsonUtils.DESC;
import static oracle.kv.impl.api.table.TableJsonUtils.FIELDS;
import static oracle.kv.impl.api.table.TableJsonUtils.NAME;
import static oracle.kv.impl.api.table.TableJsonUtils.TYPE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import oracle.kv.impl.admin.IllegalCommandException;
import oracle.kv.impl.util.JsonUtils;
import oracle.kv.table.FieldDef;
import oracle.kv.table.FieldRange;
import oracle.kv.table.FieldValue;
import oracle.kv.table.Index;
import oracle.kv.table.IndexKey;
import oracle.kv.table.RecordValue;
import oracle.kv.table.Table;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

/**
 * Implementation of the Index interface.  Instances of this class are created
 * and associated with a table when an index is defined.  It contains the index
 * metdata as well as many utility functions used in serializing and
 * deserializing index keys.
 *
 * An index can be viewed as a sorted table of N + 1 columns. Each of the first
 * N columns has an indexable atomic type (one of the numeric types or string
 * or enum). The last column stores serialized primary keys "pointing" to rows
 * in the undelying base table.
 *
 * The rows of the index are computed as follows:
 *
 * - Each index column C (other than the last one) is associated with a path
 *   expr Pc, which when evaluated on a base-table row R produces one or more
 *   indexable atomic values. Let Pc(R) be the *set* of values produced by Pc
 *   on a row R (Pc(R) may produce duplicate values, but the duplicates do not
 *   participate in index creation). If Pc is a path expr that may produce
 *   multiple values from a row, we say that C is a "multi-key" column, and
 *   the whole index is a "multi-key" index.
 *
 * - Each Pc may have at most one step, call it MK, that may produce multiple
 *   values. MK is a [] or _key step whose input is an array or map value from
 *   a row. We denote with MK-Pc the path expr that contains the steps from the
 *   start of Pc up to (and including) MK, and with R-Pc the remaining steps in
 *   Pc.
 *
 * - An index may contain more than one multi-key column, but the path exprs
 *   for all of these columns must all have the same MK-Pc.
 *
 * - Then, conceptually, the index rows are computed by a query like this:
 *
 *   select a.Pc1 as C1, c.R-Pc2 as C2, c.R-Pc3 as C3, primary_key(a) as PK
 *   from A as a, a.MK-Pc as c
 *   order by a.Pc1, c.R-Pc2, c.R-Pc3
 *
 *   In the above query, we assumed the index has 4 columns (N = 3), two of
 *   which (C2 and C3) are multi-key columns sharing the MK-Pc path. If there
 *   are no multi-key columns, the query is simpler:
 *
 *   select a.Pc1 as C1, a.Pc2 as C2, a.Pc3 as C3, primary_key(a) as PK
 *   from A as a,
 *   order by a.Pc1, a.Pc2, a.Pc3
 */
public class IndexImpl implements Index, Serializable {

    private static final long serialVersionUID = 1L;

    /* The indicator for null value used in key serialization */
    private static final byte NULL_INDICATOR = 0x01;

	/* The indicator for not null value used in key serialization */
    private static final byte NOT_NULL_INDICATOR = 0x00;

    /* the index name */
    private final String name;

    /* the (optional) index description, user-provided */
    private final String description;

    /* the associated table */
    private final TableImpl table;

    /*
     * The stringified path exprs that define the index columns. In the case of
     * map indexes a path expr may contain the special strings TableImpl.KEY_TAG
     * ("_key") and TableImpl.BRACKETS ("[]") to distinguish between the 3
     * possible ways of indexing a map: (a) all the keys (using a _key step),
     * (b) all the values (using a [] step), or (c) the value of a specific map
     * entry (using the specific key of the entry we want indexed). In case of
     * array indexes, the "[]" could be used optionally, but we have decided not
     * to allow it, so that there is a single representation of array indexes
     * (there is only one way of indexing arrays).
     */
    private final List<String> fields;

    /* status is used when an index is being populated to indicate readiness */
    private IndexStatus status;

    /*
     * transient version of the index column definitions, materialized as
     * IndexField for efficiency. It is technically final but is not because it
     * needs to be initialized in readObject after deserialization.
     */
    private transient List<IndexField> indexFields;

    /*
     * transient indication of whether this is a multiKeyMapIndex.  This is
     * used for serialization/deserialization of map indexes.  It is
     * technically final but is not because it needs to be initialized in
     * readObject after deserialization.
     */
    private transient boolean isMultiKeyMapIndex;

    /*
     * transient RecordDefImpl representing the definition of IndexKeyImpl
     * instances for this index.
     */
    private transient RecordDefImpl indexKeyDef;

    private final Map<String, String> annotations;

    /*
     * properties of the index; used by text indexes only; can be null.
     */
    private final Map<String, String> properties;

    /*
     * Indicates whether this index supports nulls in the serialized index key.
     * For older, existing indexes this will be false. For new indexes it is
     * true, unless test-specific state is set to turn this feature off.
     */
    private final boolean isNullSupported;

    public enum IndexStatus {
        /** Index is transient */
        TRANSIENT() {
            @Override
            public boolean isTransient() {
                return true;
            }
        },

        /** Index is being populated */
        POPULATING() {
            @Override
            public boolean isPopulating() {
                return true;
            }
        },

        /** Index is populated and ready for use */
        READY() {
            @Override
            public boolean isReady() {
                return true;
            }
        };

        /**
         * Returns true if this is the {@link #TRANSIENT} type.
         * @return true if this is the {@link #TRANSIENT} type
         */
        public boolean isTransient() {
            return false;
        }

        /**
         * Returns true if this is the {@link #POPULATING} type.
         * @return true if this is the {@link #POPULATING} type
         */
        public boolean isPopulating() {
            return false;
        }

        /**
         * Returns true if this is the {@link #READY} type.
         * @return true if this is the {@link #READY} type
         */
        public boolean isReady() {
            return false;
        }
    }

    public IndexImpl(String name, TableImpl table, List<String> fields,
                     String description) {
    	this(name, table, fields, null, null, description);
    }

    /* Constructor for Full Text Indexes. */
    public IndexImpl(String name, TableImpl table,
                     List<String> fields,
                     Map<String, String> annotations,
                     Map<String, String> properties,
                     String description) {
    	this.name = name;
    	this.table = table;
    	this.fields = translateFields(fields);
    	this.annotations = annotations;
    	this.properties = properties;
    	this.description = description;
    	status = IndexStatus.TRANSIENT;
    	isNullSupported = isEnableNullSupported();

    	/* validate initializes indexFields as well as isMultiKeyMapIndex */
    	validate();
    	assert indexFields != null;
    }

    public static void populateMapFromAnnotatedFields
        (List<AnnotatedField> fields,
         List<String> fieldNames,
         Map<String, String> annotations) {

    	for (AnnotatedField f : fields) {
            String fieldName = f.getFieldName();
            String translatedFieldName =
                TableImpl.translateFromExternalField(fieldName);
            fieldName = (translatedFieldName == null ?
                         fieldName :
                         translatedFieldName);
            fieldNames.add(fieldName);
            annotations.put(fieldName, f.getAnnotation());
    	}
    }

    @Override
    public Table getTable() {
        return table;
    }

    @Override
    public String getName()  {
        return name;
    }

    /**
     * Returns true if this index indexes all the entries (both keys and
     * associated data) of a map, with the key field appearing before any
     * of the data fields. This is info needed by the query optimizer.
     */
    public boolean isMapBothIndex() {

        List<IndexField> ipaths = getIndexFields();
        boolean haveMapKey = false;
        boolean haveMapValue = false;

        if (!isMultiKeyMapIndex) {
            return false;
        }

        for (IndexField ipath : ipaths) {

            if (ipath.isMapKey()) {
                haveMapKey = true;
                if (haveMapValue) {
                    return false;
                }
            } else if (ipath.isMapValue()) {
                haveMapValue = true;
                if (haveMapKey) {
                    break;
                }
            }
        }

        return (haveMapKey && haveMapValue);
    }

    /*
     * Returns a potentially modified list of the string paths for the
     * indexed fields. They are translated into strings that are used
     * by the corresponding IndexKey. For the original strings, use
     * getFieldsInternal().
     */
    @Override
    public List<String> getFields() {
        return getIndexKeyDef().getFields();
    }

    public IndexField getIndexPath(int i) {
        return indexFields.get(i);
    }

    /**
     * Returns an list of the fields that define a text index.
     * These are in order of declaration which is significant.
     *
     * @return the field names
     */
    public List<AnnotatedField> getFieldsWithAnnotations() {
    	if (! isTextIndex()) {
            throw new IllegalStateException
                ("getFieldsWithAnnotations called on non-text index");
    	}

    	final List<AnnotatedField> fieldsWithAnnotations =
    			new ArrayList<AnnotatedField>(fields.size());

    	for(String field : fields) {
            fieldsWithAnnotations.add
                (new AnnotatedField(field, annotations.get(field)));
    	}
        return fieldsWithAnnotations;
    }

    Map<String, String> getAnnotations() {
        if (isTextIndex()) {
            return Collections.unmodifiableMap(annotations);
        }
        return Collections.emptyMap();
    }

    Map<String, String> getAnnotationsInternal() {
    	return annotations;
    }

    public Map<String, String> getProperties() {
        if (properties != null) {
            return properties;
        }
        return Collections.emptyMap();
    }

    @Override
    public String getDescription()  {
        return description;
    }

    @Override
    public IndexKeyImpl createIndexKey() {
        return new IndexKeyImpl(this, getIndexKeyDef());
    }

    /*
     * Creates an IndexKeyImpl from a RecordValue that is known to be
     * a flattened IndexKey. This is used by the query engine where IndexKeys
     * are serialized and deserialized as plain RecordValue instances.
     *
     * Unlike the method below, this assumes a flattened structure in value
     * ("value" must have the same type def as the IndexKey).
     */
    public IndexKeyImpl createIndexKeyFromFlattenedRecord(RecordValue value) {
        IndexKeyImpl ikey = createIndexKey();
        ikey.copyFrom(value);
        return ikey;
    }

    @Override
    public IndexKeyImpl createIndexKey(RecordValue value) {
        if (value instanceof IndexKey) {
            throw new IllegalArgumentException(
                "Cannot call createIndexKey with IndexKey argument");
        }
        IndexKeyImpl ikey = createIndexKey();
        populateIndexRecord(ikey, (RecordValueImpl) value);
        return ikey;
    }

    @Override
    public IndexKey createIndexKeyFromJson(String jsonInput, boolean exact) {
        return createIndexKeyFromJson
            (new ByteArrayInputStream(jsonInput.getBytes()), exact);
    }

    @Override
    public IndexKey createIndexKeyFromJson(InputStream jsonInput,
                                           boolean exact) {
        IndexKeyImpl key = createIndexKey();

        /*
         * Using addMissingFields false to not add missing fields, if Json
         * contains a subset of index fields, then build partial index key.
         */
        ComplexValueImpl.createFromJson(key, jsonInput, exact,
                                        false /*addMissingFields*/);
        return key;
    }

    @Override
    public FieldRange createFieldRange(String path) {

        FieldDef ifieldDef = getIndexKeyDef().getField(path);

        if (ifieldDef == null) {
            throw new IllegalArgumentException(
                "Field does not exist in index: " + path);
        }
        return new FieldRange(path, ifieldDef, 0);
    }

    /**
     * Populates the IndexKey from the record, handling complex values.
     */
    private void populateIndexRecord(IndexKeyImpl indexKey,
                                     RecordValueImpl value) {
        assert !(value instanceof IndexKey);
        int i = 0;
        for (IndexField field : getIndexFields()) {
            FieldValueImpl v = value.getComplex(field);
            if (v != null) {
                indexKey.put(i, v);
            }
            i++;
        }
        indexKey.validate();
    }

    public int numFields() {
        return fields.size();
    }

    /**
     * Returns true if the index comprises only fields from the table's primary
     * key.  Nested types can't be key components so there is no need to handle
     * a complex path.
     */
    public boolean isKeyOnly() {
        for (String field : fields) {
            if (!table.isKeyComponent(field)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return true if this index has multiple keys per record.  This can happen
     * if there is an array or map in the index.  An index can only contain one
     * array or map.
     */
    public boolean isMultiKey() {
    	if (! isTextIndex()) {
            for (IndexField field : getIndexFields()) {
                if (field.isMultiKey()) {
                    return true;
                }
            }
    	}
        return false;
    }

    public IndexStatus getStatus() {
        return status;
    }

    public void setStatus(IndexStatus status) {
        this.status = status;
    }

    public TableImpl getTableImpl() {
        return table;
    }

    /*
     * Returns an unmodified list of the string paths for the indexed
     * fields. This is potentially different from that returned by
     * Index.getFields().
     */
    public List<String> getFieldsInternal() {
        return fields;
    }

    /**
     * Returns the list of IndexField objects defining the index.  It is
     * transient, and if not yet initialized, initialize it.
     */
    public List<IndexField> getIndexFields() {
        if (indexFields == null) {
            initTransientState();
        }
        return indexFields;
    }

    RecordDefImpl getIndexKeyDef() {
        if (indexKeyDef == null) {
            initTransientState();
        }
        return indexKeyDef;
    }

    public String getFieldName(int i) {
        return getIndexKeyDef().getFieldName(i);
    }

    public FieldDefImpl getFieldDef(int i) {
        return getIndexKeyDef().getFieldDef(i);
    }

    /**
     * Initializes the transient list of index fields.  This is used when
     * the IndexImpl was constructed via deserialization and the constructor
     * and validate() were not called.
     *
     * TODO: figure out how to do transient initialization in the
     * deserialization case.  It is not as simple as implementing readObject()
     * because an intact Table is required.  Calling validate() from TableImpl's
     * readObject() does not work either (generates an NPE).
     */
    private void initTransientState() {

        assert(indexFields == null && indexKeyDef == null);

        List<IndexField> list = new ArrayList<IndexField>(fields.size());
        int position = 0;
        for (String field : fields) {
            IndexField indexField = new IndexField(table, field, position++);

            /* this sets the multiKey state of the IndexField */
            validateIndexField(indexField, false);
            list.add(indexField);
        }
        indexFields = list;
        indexKeyDef = createRecordDef();
    }

    /**
     * If there's a multi-key field in the index return a new IndexField
     * based on the the path to the complex instance.
     */
    private IndexField findMultiKeyField() {
        for (IndexField field : getIndexFields()) {
            if (field.isMultiKey()) {
                return field.getMultiKeyField();
            }
        }

        throw new IllegalStateException
            ("Could not find any multiKeyField in index " + name);
    }

    public boolean isMultiKeyMapIndex() {
        return isMultiKeyMapIndex;
    }

    /**
     * Extracts an index key from the key and data for this
     * index.  The key has already matched this index.
     *
     * @param key the key bytes
     *
     * @param data the row's data bytes
     *
     * @param keyOnly true if the index only uses key fields.  This
     * optimizes deserialization.
     *
     * @return the byte[] serialization of an index key or null if there
     * is no entry associated with the row, or the row does not match a
     * table record.
     *
     * While not likely it is possible that the record is not actually  a
     * table record and the key pattern happens to match.  Such records
     * will fail to be deserialized and throw an exception.  Rather than
     * treating this as an error, silently ignore it.
     *
     * TODO: maybe make this faster.  Right now it turns the key and data
     * into a Row and extracts from that object which is a relatively
     * expensive operation, including full Avro deserialization.
     */
    public byte[] extractIndexKey(byte[] key,
                                  byte[] data,
                                  boolean keyOnly) {
        RowImpl row = table.createRowFromBytes(key, data, keyOnly);
        if (row != null) {
            return serializeIndexKey(row, 0);
        }
        return null;
    }

    /**
     * Extracts multiple index keys from a single record.  This is used if
     * one of the indexed fields is an array.  Only one array is allowed
     * in an index.
     *
     * @param key the key bytes
     *
     * @param data the row's data bytes
     *
     * @param keyOnly true if the index only uses key fields.  This
     * optimizes deserialization.
     *
     * @return a List of byte[] serializations of index keys or null if there
     * is no entry associated with the row, or the row does not match a
     * table record.  This list may contain duplicate values.  The caller is
     * responsible for handling duplicates (and it does).
     *
     * While not likely it is possible that the record is not actually  a
     * table record and the key pattern happens to match.  Such records
     * will fail to be deserialized and throw an exception.  Rather than
     * treating this as an error, silently ignore it.
     *
     * TODO: can this be done without reserializing to Row?  It'd be
     * faster but more complex.
     *
     * 1.  Deserialize to RowImpl
     * 2.  Find the map or array value and get its size
     * 3.  for each map or array entry, serialize a key using that entry
     */
    public List<byte[]> extractIndexKeys(byte[] key,
                                         byte[] data,
                                         boolean keyOnly) {

        RowImpl row = table.createRowFromBytes(key, data, keyOnly);
        return extractIndexKeys(row);
    }

    public List<byte[]> extractIndexKeys(RowImpl row) {

        if (row == null) {
            return null;
        }

        IndexField indexField = findMultiKeyField();

        FieldValueImpl val = row.getComplex(indexField);

        /*
         * Here is an example about how val may be null:
         *
         * create table Foo (id INTEGER, map MAP(MAP(INTEGER))).
         * create index idx on Foo (keys(map.someKey))
         * row.map has no entry for "someKey".
         */
        if (val == null) {
            if (!isNullSupported()) {
                return null;
            }
            val = NullValueImpl.getInstance();
        }

        boolean nullMultiKeyValue =
            (val.isNull() ||
             (val.isArray() && val.asArray().size() == 0) ||
             (val.isMap() && val.asMap().size() == 0));

        if (nullMultiKeyValue && !isNullSupported()) {
            return null;
        }

        if (!isMultiKeyMapIndex || nullMultiKeyValue) {
            final int size = (nullMultiKeyValue ? 1 : val.asArray().size());
            ArrayList<byte[]> returnList = new ArrayList<byte[]>(size);

            for (int i = 0; i < size; i++) {
                byte[] serKey = serializeIndexKey(row, i, nullMultiKeyValue);

                /*
                 * It should not be possible for this to be null because
                 * it is not possible to add null values to arrays, but
                 * a bit of paranoia cannot hurt.
                 */
                if (serKey != null) {
                    returnList.add(serKey);
                }
            }
            return returnList;
        }

        assert(val.isMap());
        MapValueImpl mapVal = (MapValueImpl) val;
        ArrayList<byte[]> returnList =
            new ArrayList<byte[]>(mapVal.size());
        Map<String, FieldValue> map = mapVal.getFieldsInternal();
        for (String mapKey : map.keySet()) {
            byte[] serKey = serializeIndexKey(row, mapKey);
            if (serKey != null) {
                returnList.add(serKey);
            }
        }
        return returnList;
    }

    public void toJsonNode(ObjectNode node) {
        node.put(NAME, name);
        node.put(TYPE, getType().toString().toLowerCase());
        if (description != null) {
            node.put(DESC, description);
        }
        if (isMultiKey()) {
            node.put("multi_key", "true");
        }
        ArrayNode fieldArray = node.putArray(FIELDS);
        for (IndexField field : getIndexFields()) {
            fieldArray.add(field.getPathName());
        }
        if (annotations != null) {
            putMapAsJson(node, "annotations", annotations);
        }
        if (properties != null) {
            putMapAsJson(node, "properties", properties);
        }
    }

    private static void putMapAsJson(ObjectNode node,
                                     String mapName,
                                     Map<String, String> map) {
        ObjectNode mapNode = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            mapNode.put(entry.getKey(), entry.getValue());
        }
        node.put(mapName, mapNode);
    }

    /**
     * Validate that the name, fields, and types of the index match
     * the table.  This also initializes the (transient) list of index fields in
     * indexFields, so that member must not be used in validate() itself.
     *
     * This method must only be called from the constructor.  It is not
     * synchronized and changes internal state.
     */
    private void validate() {

        TableImpl.validateIdentifier(name, false);

        IndexField multiKeyField = null;

        if (fields.isEmpty()) {
            throw new IllegalCommandException
                ("Index requires at least one field");
        }

        assert indexFields == null;

        indexFields = new ArrayList<IndexField>(fields.size());

        int position = 0;
        for (String field : fields) {

            if (field == null || field.length() == 0) {
                throw new IllegalCommandException
                    ("Invalid (null or empty) index field name");
            }

            IndexField ifield = new IndexField(table, field, position++);

            /*
             * The check for multiKey needs to consider all fields as well as
             * fields that reference into complex types.  A multiKey field may
             * occur at any point in the navigation path (first, interior, leaf).
             *
             * The call to isMultiKey() will set the multiKey state in
             * the IndexField.
             *
             * Allow more than one multiKey field in a single index IFF they are
             * in the same object (map or array).
             */
            validateIndexField(ifield, true);

            /* Don't restrict number of multi-key fields for text indexes. */
            if (ifield.isMultiKey() && !isTextIndex()) {
                IndexField mkey = ifield.getMultiKeyField();
                if (multiKeyField != null && !mkey.equals(multiKeyField)) {
                    throw new IllegalCommandException
                        ("Indexes may contain only one multiKey field");
                }
                multiKeyField = mkey;
            }

            if (indexFields.contains(ifield)) {
                throw new IllegalCommandException
                    ("Index already contains the field: " + field);
            }

            indexFields.add(ifield);
        }

        assert fields.size() == indexFields.size();

        /*
         * initialize transient RecordDef representing the IndexKeyImpl
         * definition.
         */
        indexKeyDef = createRecordDef();

        table.checkForDuplicateIndex(this);
    }

    /**
     * Validates the given index path expression (ipath) and returns its data
     * type (which must be one of the indexable atomic types).
     *
     * This call has a side effect of setting the multiKey state in the
     * IndexField so that the lookup need not be done twice.
     */
    private FieldDef validateIndexField(IndexField ipath, boolean isNewIndex) {

        StringBuilder sb = new StringBuilder();

        List<String> steps = ipath.getSteps();
        int numSteps = steps.size();

        int stepIdx = 0;
        String step = steps.get(stepIdx);
        sb.append(step);
        FieldDef stepDef = ipath.getFirstDef();

        if (stepDef == null) {
            throw new IllegalCommandException(
                "Invalid index field definition : " + ipath + "\n" +
                "There is no field named " + step);
        }

        while (stepIdx < numSteps) {

            /*
             * TODO: Prevent any path through these types from
             * participating in a text index, until the text index
             * implementation supports them correctly.
             */
            if (isTextIndex() &&
                (stepDef.isBinary() ||
                 stepDef.isFixedBinary() || stepDef.isEnum())) {
                    throw new IllegalCommandException
                        ("Invalid index field definition : " + ipath + "\n" +
                         "Fields of type " + stepDef.getType() +
                         " cannot participate in a FULLTEXT index.");
            }

            if (stepDef.isRecord()) {

                ++stepIdx;
                if (stepIdx >= numSteps) {
                    break;
                }

                step = steps.get(stepIdx);
                stepDef = stepDef.asRecord().getFieldDef(step);

                if (stepDef == null) {
                    throw new IllegalCommandException(
                        "Invalid index field definition : " + ipath + "\n" +
                        "There is no field named \"" + step + "\" after " +
                        "path " + sb.toString());
                }

                sb.append(SEPARATOR);
                sb.append(step);

            } else if (stepDef.isArray()) {

                if (ipath.isMultiKey()) {
                    throw new IllegalCommandException(
                        "Invalid index field definition : " + ipath + "\n" +
                        "The definition contains more than one multi-key " +
                        "fields. The second multi-key field is " + step);
                }

                ipath.setMultiKeyPath(sb.toString(), ipath.getPosition());

                /*
                 * If there is a next step and it is [], consume it.
                 *
                 * Else, if we are creating a new index, throw an exception,
                 * because as of version 4.2 the use of [] is mandatory for
                 * array indexes.
                 *
                 * Otherwise, we must be reading from a store created prior
                 * tp v4.2. In this case, the [] may be missing. We must add
                 * it to the index path, because it is expected to be there
                 * in other parts of the code (for example, the index matching
                 * done by query processor).
                 */
                if (stepIdx + 1 < numSteps &&
                    steps.get(stepIdx + 1).equals(TableImpl.BRACKETS)) {
                    ++stepIdx;

                } else if (isNewIndex) {
                    throw new IllegalCommandException(
                        "Invalid index field definition : " + ipath + "\n" +
                        "Can not index an array as a whole; use " + step +
                        "[]  to index the elements of the array");

                } else {
                    ++stepIdx;
                    ++numSteps;
                    ipath.add(stepIdx, TableImpl.BRACKETS);
                }

                step = TableImpl.BRACKETS;
                stepDef = stepDef.asArray().getElement();
                sb.append(SEPARATOR);
                sb.append(step);

            } else if (stepDef.isMap()) {

                ++stepIdx;
                if (stepIdx >= numSteps) {
                    throw new IllegalCommandException(
                        "Invalid index field definition : " + ipath + "\n" +
                        "Can not index a map as a whole; use " +
                        "[] to index the elements of the map or keys(" +
                        ") to index the keys of the map");
                }

                step = steps.get(stepIdx);

                if (step.equals(TableImpl.BRACKETS)) {

                    if (ipath.isMultiKey()) {
                        throw new IllegalCommandException(
                            "Invalid index field definition : " + ipath + "\n" +
                            "The definition contains more than one multi-key " +
                            "fields. The second multi-key field is " + step);
                    }

                    ipath.setMultiKeyPath(sb.toString(), ipath.getPosition());
                    ipath.setIsMapValue();
                    isMultiKeyMapIndex = true;

                    /* Consume the [] step */
                    stepDef = stepDef.asMap().getElement();
                    sb.append(SEPARATOR);
                    sb.append(step);

                } else if (step.equals(TableImpl.KEY_TAG)) {

                    if (ipath.isMultiKey()) {
                        throw new IllegalCommandException(
                            "Invalid index field definition : " + ipath + "\n" +
                            "The definition contains more than one multi-key " +
                            "fields. The second multi-key field is " + step);
                    }

                    ipath.setMultiKeyPath(sb.toString(), ipath.getPosition());
                    ipath.setIsMapKey();
                    isMultiKeyMapIndex = true;

                    /* Consume the _key step */
                    stepDef = FieldDefImpl.stringDef;
                    sb.append(SEPARATOR);
                    sb.append(step);

                } else {
                    stepDef = stepDef.asMap().getElement();
                    sb.append(SEPARATOR);
                    sb.append(step);
                }

            } else {

                ++stepIdx;
                if (stepIdx >= numSteps) {
                    break;
                }

                step = steps.get(stepIdx);
                throw new IllegalCommandException(
                    "Invalid index field definition : " + ipath + "\n" +
                    "There is no field named \"" + step + "\" after " +
                    "path " + sb.toString());
            }
        }

        if (!stepDef.isValidIndexField()) {
            throw new IllegalCommandException(
                "Invalid index field definition : " + ipath + "\n" +
                "Cannot index values of type " + stepDef);
        }

        ipath.typeDef = (FieldDefImpl) stepDef;

        /*
         * If NULLs are allowed in index key, the nullablity of 2 kinds of
         * field below is true:
         *  1. The complex field or nested field of complex type.
         *  2. The simple field is nullable and not a primary key field.
         */
        boolean nullable = (ipath.isComplex() ||
                            (!table.isKeyComponent(ipath.getPathName()) &&
                             ipath.getFieldMap()
                                 .getFieldMapEntry(ipath.getPathName())
                                     .isNullable())) &&
                           isNullSupported();
        ipath.setNullable(nullable);

        return stepDef;
    }


    @Override
    public String toString() {
        return "Index[" + name + ", " + table.getId() + ", " + status + "]";
    }

    /**
     * Create a binary index key from a row. This method is used for simple
     * (non-multi-key) indexes and for indexes on arrays. It is also used
     * for multi-key map indexes if the map is null or empty inside the given
     * row.
     *
     * @param record the record to extract a binary index key from. Actually,
     * this is RowImpl. The caller can vouch for the validity of the object.
     *
     * @param arrayIndex will be 0 if not doing an array lookup, or if the
     * desired array index is actually 0.  For known array lookups it may be
     * >0.
     *
     * @return the serialized index key or null if the record cannot
     * be serialized.
     *
     * These are conditions that will cause serialization to fail:
     * 1.  The record has a null values in one of the index keys
     * 2.  An index key field contains a map and the record does not
     * have a value for the indexed map key value
     *
     * TODO: consider sharing more code with the other serializeIndexKey()
     * method.
     *
     * This is public so it can be used by TableSizeCommand. Otherwise it'd be
     * private.
     */
    public byte[] serializeIndexKey(RecordValueImpl record,
                                    int arrayIndex) {
        return serializeIndexKey(record, arrayIndex, false);
    }

    private byte[] serializeIndexKey(RecordValueImpl record,
                                     int arrayIndex,
                                     boolean nullMultiKeyValue) {

        if (isMultiKeyMapIndex()) {
            if (!isNullSupported() || !nullMultiKeyValue) {
                throw new IllegalStateException("Wrong serializer for " +
                    "map index");
            }
        }

        TupleOutput out = null;

        try {
            out = new TupleOutput();

            int ind = 0;
            for (IndexField field : getIndexFields()) {

                FieldValue val =
                    (field.isMultiKey() && nullMultiKeyValue ?
                     NullValueImpl.getInstance() :
                     record.findFieldValue(field.iterator(), arrayIndex));

                /*
                 * Here is an example about how val may be null:
                 *
                 * create table Foo (id INTEGER, arr ARRAY(MAP(INTEGER))).
                 * create index idx on Foo (arr[].someKey))
                 * row.arr[i] has no entry for "someKey".
                 */
                if (val == null) {
                    if (!isNullSupported()) {
                        return null;
                    }
                    val = NullValueImpl.getInstance();
                } else {
                    /*
                     * If the value is NULL, and the index does not support
                     * indexing of NULLs, it is not possible to create a binary
                     * index key. In this case, this row has no entry for this
                     * index.
                     */
                    if (val.isNull() && !isNullSupported()) {
                        return null;
                    }
                }

                serializeValue(out, val, allowNull(ind++));
            }

            return (out.size() != 0 ? out.toByteArray() : null);

        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    /**
     * Create a binary index key from a row. This method is used for multi-key
     * map indexes.
     *
     * @param record the record to extract a binary index key from. Actually,
     * this is RowImpl. The caller can vouch for the validity of the object.
     *
     * @param mapKey will be null if not doing a map lookup.
     *
     * @return the serialized index key or null if the record cannot
     * be serialized.
     *
     * These are conditions that will cause serialization to fail:
     * 1. The record has a null value in one of the index keys
     * 2. An index key field contains a map and the record does not
     *    have a value for the indexed map key value
     *
     * TODO: consider sharing more code with the other serializeIndexKey()
     * method.
     *
     * This method is package protected vs private because it's used by test
     * code.
     */
    byte[] serializeIndexKey(RecordValueImpl record, String mapKey) {

        assert isMultiKeyMapIndex();
        TupleOutput out = null;
        try {
            out = new TupleOutput();

            int ind = 0;
            for (IndexField field : getIndexFields()) {

                /*
                 * findFieldValue handles the special map fields of "_key" and
                 * "[]" and returns the correct information in both cases. See
                 * MapValueImpl.findFieldValue().
                 */
                FieldValue val = record.findFieldValue(
                    field.iterator(), mapKey);

                if (val == null) {
                    if (!isNullSupported()) {
                        /* Failed to find a value, this is a partial key. */
                        return null;
                    }
                    val = NullValueImpl.getInstance();
                } else {
                    /*
                     * If the value is NULL, and the index does not support
                     * indexing of NULLs, it is not possible to  create a binary
                     * index key. In this case, this row has no entry for this
                     * index.
                     */
                    if (val.isNull() && !isNullSupported()) {
                        return null;
                    }
                }

                serializeValue(out, val, allowNull(ind++));
            }

            return (out.size() != 0 ? out.toByteArray() : null);

        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    /**
     *  Create a binary index key from an IndexKey. In this case
     * the IndexKey may be partially filled.
     *
     * This is the version used by most client-based callers.
     *
     * @return the serialized index key or null if the IndexKey cannot
     * be serialized (e.g. it has null values).
     */
    public byte[] serializeIndexKey(IndexKeyImpl indexKey) {
        return serializeIndexKey(indexKey, true);
    }

    /*
     * if includeNulls is true, include null separators in serialization. This
     * is the normal case. The case where includeNulls is false is reserved for
     * handling pre-4.2 clients that don't expect the null separators.
     */
    private byte[] serializeIndexKey(IndexKeyImpl indexKey,
                                     boolean includeNulls) {

        TupleOutput out = null;

        try {
            out = new TupleOutput();

            int numFields = indexKeyDef.getNumFields();

            for (int i = 0; i < numFields; ++i) {

                FieldValue val = indexKey.get(i);

                if (val == null) {
                    /* A partial key, done with fields */
                    break;
                }

                /*
                 * If any values are NULL it is not possible to serialize the
                 * index key, even partially. NULL values cannot be indexed
                 * so this row has no entry for this index.
                 */
                if (val.isNull() && (!isNullSupported() || !includeNulls)) {
                    return null;
                }

                serializeValue(out, val, (allowNull(i) && includeNulls));
            }

            return (out.size() != 0 ? out.toByteArray() : null);

        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    /*
     * This method is used during the executoin of an IndexKeysIterateHandler,
     * which sends binary index keys (extracted from the index scan) back to
     * the client. The method is used when the binary index keys contain null
     * indicators and the client is pre-4.2. In this case the null indicators
     * must be removed from the binary keys, because the client wouldn't be 
     * able to deserialize them correctly otherwise.
     */
    public byte[] reserializeToOldKey(byte[] indexKey) {
        IndexKeyImpl ikey = deserializeIndexKey(indexKey,
                                                false, /* keys are complete */
                                                true); /* may include nulls */
        return serializeIndexKey(ikey, false);
    }

    static TupleInput serializeValue(FieldValue value,
                                     boolean withNullIndicator) {
        TupleOutput output = new TupleOutput();
        serializeValue(output, value, withNullIndicator);
        return new TupleInput(output);
    }

    private static void serializeValue(TupleOutput out,
                                       FieldValue val,
                                       boolean withNullIndicator) {

        if (withNullIndicator) {
            out.writeByte(getSerializeNullIndicator(val));
            if (val == null || val.isNull()) {
                return;
            }
        }

        switch (val.getType()) {
        case INTEGER:
            out.writeSortedPackedInt(val.asInteger().get());
            break;
        case STRING:
            out.writeString(val.asString().get());
            break;
        case LONG:
            out.writeSortedPackedLong(val.asLong().get());
            break;
        case DOUBLE:
            out.writeSortedDouble(val.asDouble().get());
            break;
        case FLOAT:
            out.writeSortedFloat(val.asFloat().get());
            break;
        case NUMBER:
            out.write(((NumberValueImpl)val).getBytes());
            break;
        case ENUM:
            /* enumerations are sorted by declaration order */
            out.writeSortedPackedInt(val.asEnum().getIndex());
            break;
        case BOOLEAN:
            out.writeBoolean(val.asBoolean().get());
            break;
        case TIMESTAMP:
            out.write(((TimestampValueImpl)val).getBytes(true));
            break;
        default:
            throw new IllegalStateException
            ("Type not supported in indexes: " +
             val.getType());
        }
    }

    public boolean isNullSupported() {
        return isNullSupported;
    }

    private static byte getSerializeNullIndicator(FieldValue val) {
        return (val == null || val.isNull()) ?
                NULL_INDICATOR : NOT_NULL_INDICATOR;
    }

    private static boolean isNullIndicator(byte indicator) {
        return (indicator == NULL_INDICATOR);
    }

    /**
     * Returns true if the index field on the specified position allows NULL
     * value, otherwise return false.
     */
    boolean allowNull(int idxField) {
        return indexFields.get(idxField).isNullable();
    }

    /**
     * Deserialize the serialized format of an index key directly into a Row.
     *
     * Arrays -- if there is an array index the index key returned will
     * be the serialized value of a single array entry and not the array
     * itself. This value needs to be deserialized back into a single-value
     * array.
     *
     * Maps -- if there is a map index the index key returned will
     * be the serialized value of a single map entry.  It may be key-only or
     * it may be key + value. In both cases the map and the appropriate key
     * need to be created.
     *
     * In this path, partial population is allowed.
     *
     * @param data the bytes
     * @param row the Row to use.
     */
    public void rowFromIndexKey(byte[] data, RowImpl row) {

        TupleInput input = null;

        try {
            input = new TupleInput(data);

            int ind = 0;
            for (IndexField ifield : getIndexFields()) {

                if (input.available() <= 0) {
                    break;
                }

                if (allowNull(ind++)) {
                    byte in = input.readByte();
                    if (isNullIndicator(in)) {
                        row.putComplex(ifield.iterator(),
                                       NullValueImpl.getInstance());
                        continue;
                    }
                }

                FieldDefImpl def = ifield.getTypeDef();

                switch (def.getType()) {
                case INTEGER:
                case STRING:
                case LONG:
                case DOUBLE:
                case BOOLEAN:
                case FLOAT:
                case NUMBER:
                case ENUM:
                case TIMESTAMP:
                    FieldValue val =
                        def.createValue(FieldValueImpl.readTuple(def, input));
                    row.putComplex(ifield.iterator(), val);
                    break;

                 default:
                    throw new IllegalStateException
                        ("Type not supported in indexes: " + def.getType());
                }
            }
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ioe) {
                /* ignore IOE on close */
            }
        }
    }

    /**
     * Deserializes the byte[] format of IndexKeyImpl into IndexKeyImpl.
     * This is used for return values that require IndexKeyImpl vs a Row.
     *
     * @param data the bytes
     * @param partialOK true if not all fields must be in the data stream.
     * @return an instance of IndexKeyImpl
     */
    public IndexKeyImpl deserializeIndexKey(byte[] data,
                                            boolean partialOK) {
        return deserializeIndexKey(data, partialOK, true);
    }

    /**
     * Same as above, adding:
     * @param mayIncludeNulls true if nulls are to be considered.
     */
    IndexKeyImpl deserializeIndexKey(byte[] data,
                                     boolean partialOK,
                                     boolean mayIncludeNulls) {
        TupleInput input = null;
        IndexKeyImpl indexKey = new IndexKeyImpl(this, indexKeyDef);

        try {
            input = new TupleInput(data);

            int numFields = indexKeyDef.getNumFields();

            for (int i = 0; i < numFields; ++i) {

                if (input.available() <= 0) {
                    break;
                }

                if (mayIncludeNulls && allowNull(i)) {
                    byte in = input.readByte();
                    if (isNullIndicator(in)) {
                        indexKey.putNull(i);
                        continue;
                    }
                }

                FieldDefImpl def = indexKeyDef.getFieldDef(i);

                switch (def.getType()) {
                case INTEGER:
                case STRING:
                case LONG:
                case DOUBLE:
                case FLOAT:
                case NUMBER:
                case ENUM:
                case BOOLEAN:
                case TIMESTAMP:
                    FieldValue val =
                        def.createValue(FieldValueImpl.readTuple(def, input));
                    indexKey.put(i, val);
                    break;

                 default:
                     throw new IllegalStateException(
                        "Type not supported in indexes: " + def.getType());
                }
            }

            if (!partialOK && !indexKey.isComplete()) {
                throw new IllegalStateException(
                    "Missing fields from index data for index " +
                    getName() + ", expected " + numFields +
                    ", received " +   indexKey.size());
            }
            return indexKey;
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ioe) {
                /* ignore IOE on close */
            }
        }
    }

    /**
     * Returns true if the given path is the same as one of the paths that
     * define the index columns.
     */
    public boolean isIndexField(TablePath path) {

        for (IndexField iField : getIndexFields()) {
            if (iField.equals(path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks to see if the index contains the *single* named field.
     * For simple types this is a simple contains operation.
     *
     * For complex types this needs to validate for a put of a complex
     * type that *may* contain an indexed field.
     * Validation of such fields must be done later.
     *
     * In the case of a nested field name with dot-separated names,
     * this code simply checks that fieldName is one of the components of
     * the complex field (using String.contains()).
     */
    boolean containsField(String fieldName) {
        String fname = fieldName.toLowerCase();

        for (IndexField indexField : getIndexFields()) {
            if (indexField.isComplex()) {
                if (indexField.getPathName().contains(fname)) {
                    return true;
                }

            } else {
                if (indexField.getPathName().equals(fname)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Translate external representation to internal.  E.g.:
     * keys(mapfield) => mapfield._key
     * mapfield[] => mapfield.[]
     *
     * This could be optimized for the case where there's nothing to do, but
     * this isn't a high performance path, so unconditionally copy the list.
     *
     * If there is a failure to parse the original list is returned, allowing
     * errors to be handled in validation of fields.
     */
    public static List<String> translateFields(List<String> fieldList) {

        ArrayList<String> newList = new ArrayList<String>(fieldList.size());

        for (String field : fieldList) {
            /*
             * it is possible for the field to be null, at least in test
             * cases.  If so, return the original list.
             */
            if (field == null) {
                return fieldList;
            }

            String newField = TableImpl.translateFromExternalField(field);
            /*
             * A null return means that the format of the field string is
             * not legal.  Return the original list and let the inevitable
             * failure happen (no such field) on the untranslated list.
             */
            if (newField == null) {
                return fieldList;
            }
            newList.add(newField);
        }
        return newList;
    }

    /**
     * Creates an IndexKeyImpl for the Index. This means generating a RecordDef
     * for the flattened key fields. The rules for the new RecordDef are:
     * 1. top-level atomic fields are left as-is (the field name, e.g. "a")
     * 2. paths into nested Records that do not involve arrays or maps are
     * left intact (e.g. "a.b.c")
     * 3. array elements turn into <path-to-array>[]. Internally these need to
     * be translated to <path-to-array>.[]
     * 4. map elements turn into <path-to-map>[].  Internally these need to
     * be translated to <path-to-map>.[]
     * 5. map keys turn into keys(path-to-map), which will be internally
     * translated to path-to-map._key
     *
     * Index fields cannot contain more than one array or map which simplifies
     * the translations. It is not possible
     */
    private RecordDefImpl createRecordDef() {

        FieldMap fieldMap = new FieldMap();

        /*
         * Use the list of IndexField because it's already got complex
         * paths translated to a normalized form with use of [] in the
         * appropriate places.
         */
        for (IndexImpl.IndexField indexField: getIndexFields()) {

            FieldDefImpl def = indexField.getTypeDef();
            String translatedName = indexField.getPathName();
            final FieldMapEntry fme = new FieldMapEntry(translatedName, def);

            fieldMap.put(fme);
        }

        return new RecordDefImpl(fieldMap, null);
    }

    /**
     * Encapsulates a single field in an index, which may be simple or
     * complex.  Simple fields (e.g. "name") have a single component. Fields
     * that navigate into nested fields (e.g. "address.city") have multiple
     * components.  The state of whether a field is simple or complex is kept
     * by TablePath.
     *
     * IndexField adds this state:
     *   multiKeyField -- if this field results in a multi-key index this holds
     *     the portion of the field's path that leads to the FieldValue that
     *     makes it multi-key -- an array or map.  This is used as a cache to
     *     make navigation to that field easier.
     *   multiKeyType -- if multiKeyPath is set, this indicates if the field
     *     is a map key or map value field.
     * Arrays don't need additional state.
     *
     * Field names are case-insensitive, so strings are stored lower-case to
     * simplify case-insensitive comparisons.
     */
    public static class IndexField extends TablePath {

        /* the path to a multi-key field (map or array) */
        private IndexField multiKeyField;

        private MultiKeyType multiKeyType;

        /* the position in the key */
        private final int position;

        private FieldDefImpl typeDef;

        /* the nullability of the field */
        private boolean nullable;

        /* ARRAY is not included because no callers need that information */
        private enum MultiKeyType { NONE, MAPKEY, MAPVALUE }

        /* public access for use by the query compiler */
        public IndexField(TableImpl table, String field, int position) {
            super(table, field);
            multiKeyType = MultiKeyType.NONE;
            this.position = position;
        }

        private IndexField(FieldMap fieldMap, String field, int position) {
            super(fieldMap, field);
            multiKeyType = MultiKeyType.NONE;
            this.position = position;
        }

        IndexField getMultiKeyField() {
            return multiKeyField;
        }

        public boolean isMultiKey() {
            return multiKeyField != null;
        }

        public int getPosition() {
            return position;
        }

        public FieldDefImpl getTypeDef() {
            return typeDef;
        }

        public void setTypeDef(FieldDefImpl def) {
            typeDef = def;
        }

        boolean isNullable() {
            return nullable;
        }

        private void setMultiKeyPath(String path, int position) {
            multiKeyField = new IndexField(getFieldMap(), path, position);
        }

        public boolean isMapKey() {
            return multiKeyType == MultiKeyType.MAPKEY;
        }

        private void setIsMapKey() {
            multiKeyType = MultiKeyType.MAPKEY;
        }

        public boolean isMapValue() {
            return multiKeyType == MultiKeyType.MAPVALUE;
        }

        private void setIsMapValue() {
            multiKeyType = MultiKeyType.MAPVALUE;
        }

        private void setNullable(boolean nullable) {
            this.nullable = nullable;
        }
    }

    @Override
    public Index.IndexType getType() {
        if (annotations == null) {
            return Index.IndexType.SECONDARY;
        }
        return Index.IndexType.TEXT;
    }

    private boolean isTextIndex() {
        return getType() == Index.IndexType.TEXT;
    }

    /**
     * This lightweight class stores an index field, along with
     * an annotation.  Not all index types require annotations;
     * It is used for the mapping specifier in full-text indexes.
     */
    public static class AnnotatedField implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String fieldName;

        private final String annotation;

        public AnnotatedField(String fieldName, String annotation) {
            assert(fieldName != null);
            this.fieldName = fieldName;
            this.annotation = annotation;
        }

        /**
         * The name of the indexed field.
         */
        public String getFieldName() {
            return fieldName;
        }

        /**
         *  The field's annotation.  In Text indexes, this is the ES mapping
         *  specification, which is a JSON string and may be null.
         */
        public String getAnnotation() {
            return annotation;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }

            AnnotatedField other = (AnnotatedField) obj;

            if (! fieldName.equals(other.fieldName)) {
                return false;
            }

            return (annotation == null ?
                    other.annotation == null :
                    JsonUtils.jsonStringsEqual(annotation, other.annotation));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + fieldName.hashCode();
            if (annotation != null) {
                result = prime * result + annotation.hashCode();
            }
            return result;
        }
    }

    @Override
    public String getAnnotationForField(String fieldName) {
        if (isTextIndex() == false) {
            return null;
        }
        return annotations.get(fieldName);
    }

    public RowImpl deserializeRow(byte[] keyBytes, byte[] valueBytes) {
        return table.createRowFromBytes(keyBytes, valueBytes, false);
    }

    /* For testing purpose */
    final static String INDEX_NULL_DISABLE = "test.index.null.disable";
    private boolean isEnableNullSupported() {
        if (Boolean.getBoolean(INDEX_NULL_DISABLE)) {
            return false;
        }
        return true;
    }

    /**
     * This is directly from JE's com.sleepycat.je.tree.Key class and is the
     * default byte comparator for JE's btree.
     *
     * Compare using a default unsigned byte comparison.
     */
    static int compareUnsignedBytes(byte[] key1,
                                    int off1,
                                    int len1,
                                    byte[] key2,
                                    int off2,
                                    int len2) {
        int limit = Math.min(len1, len2);

        for (int i = 0; i < limit; i++) {
            byte b1 = key1[i + off1];
            byte b2 = key2[i + off2];
            if (b1 == b2) {
                continue;
            }
            /*
             * Remember, bytes are signed, so convert to shorts so that we
             * effectively do an unsigned byte comparison.
             */
            return (b1 & 0xff) - (b2 & 0xff);
        }

        return (len1 - len2);
    }

    static int compareUnsignedBytes(byte[] key1, byte[] key2) {
        return compareUnsignedBytes(key1, 0, key1.length, key2, 0, key2.length);
    }
}
