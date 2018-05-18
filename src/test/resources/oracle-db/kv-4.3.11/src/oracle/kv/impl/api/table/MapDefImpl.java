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

import static oracle.kv.impl.api.table.TableImpl.KEY_TAG;
import static oracle.kv.impl.api.table.TableJsonUtils.COLLECTION;
import static oracle.kv.table.MapValue.ANONYMOUS;

import java.util.ListIterator;
import oracle.kv.impl.util.JsonUtils;
import oracle.kv.table.FieldDef;
import oracle.kv.table.MapDef;
import com.sleepycat.persist.model.Persistent;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

/**
 * MapDefImpl implements the MapDef interface.
 */
@Persistent(version=1)
public class MapDefImpl extends FieldDefImpl implements MapDef {

    private static final long serialVersionUID = 1L;

    private final FieldDefImpl element;

    private static final StringDefImpl keyType = new StringDefImpl();

    MapDefImpl(FieldDefImpl element, String description) {

        super(FieldDef.Type.MAP, description);
        if (element == null) {
            throw new IllegalArgumentException
                ("Map has no field and cannot be built");
        }
        this.element = element;
    }

   MapDefImpl(FieldDefImpl element) {
       this(element, null);
   }

    private MapDefImpl(MapDefImpl impl) {
        super(impl);
        element = impl.element.clone();
    }

    /**
     * For DPL
     */
    @SuppressWarnings("unused")
    private MapDefImpl() {
        element = null;
    }

    /*
     * Public api methods from Object and FieldDef
     */

    @Override
    public MapDefImpl clone() {

        if (this == FieldDefImpl.mapAnyDef ||
            this == FieldDefImpl.mapJsonDef) {
            return this;
        }

        return new MapDefImpl(this);
    }

    @Override
    public int hashCode() {
        return element.hashCode();
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }

        if (other instanceof MapDefImpl) {
            return element.equals(((MapDefImpl)other).getElement());
        }
        return false;
    }

    @Override
    public MapDef asMap() {
        return this;
    }

    @Override
    public MapValueImpl createMap() {
        return new MapValueImpl(this);
    }

    /*
     * Public api methods from MapDef
     */

    @Override
    public FieldDefImpl getElement() {
        return element;
    }

    @Override
    public FieldDef getKeyDefinition() {
        return keyType;
    }

    /*
     * FieldDefImpl internal api methods
     */

    @Override
    public boolean isPrecise() {
        return element.isPrecise();
    }

    @Override
    public boolean isSubtype(FieldDefImpl superType) {

        if (this == superType) {
            return true;
        }

        if (superType.isMap()) {
            MapDefImpl supMap = (MapDefImpl)superType;
            return element.isSubtype(supMap.element);
        }

        if (superType.isJson()) {
            return element.isSubtype(jsonDef);
        }

        if (superType.isAny()) {
             return true;
        }

        return false;
    }

    /**
     * Finds a FieldDefImpl associated with a path to a complex type.
     * The iterator iterates over components of the path to the field,
     * which is usually a field in an index.
     *
     * The caller will have consumed all portions of the iteration leading
     * up to the MapDefImpl. The remaining components reference one of several
     * things.  1-3 are used by the R3.2 multi-key map indexes.  4-5 are
     * used by the original single-key map indexes in 3.1.
     *  1.  <path-to-here>._key.  References the map's key, so it's a string.
     *  2.  <path-to-here>.[].  References the map's element (value), so
     *    the element is returned.
     *  3.  <path-to-here>.[].foo.  References "foo" within the map's
     *    element (value).  This needs to be resolved by the element, so this
     *    case calls the findField() on the element.
     *  4.  <path-to_here>.someString.  References the element as well.  This is
     *    used by R3.1-style map indexes, which index the value of a specific
     *    key, and the "someString" is the key that's indexed.  It does not
     *    match any actual metadata.
     *  5.  <path-to-here>.indexedKey.moreStuff.  Similar to (3) but because in
     *    this path there is an index on the "indexedKey" map entry, the
     *    current field is just consumed (as is [], above) and the rest of
     *    the path is resolved by the element.
     */
    @Override
    FieldDefImpl findField(ListIterator<String> fieldPath) {
        assert fieldPath.hasNext();

        String currentField = fieldPath.next();

        /*
         * If the field is <path-to-map>._key, it's a string
         */
        if (KEY_TAG.equalsIgnoreCase(currentField)) {
            if (fieldPath.hasNext()) {
                throw new IllegalArgumentException
                    (KEY_TAG +
                     " must be the final component of the field");
            }
            return keyType;
        }

        /*
         * If there are not further components the currentField is either
         * "[]" or the value of an indexed key (single-key map index).
         * In both cases the target of the operation is the map's element
         * so return it.
         */
        if (!fieldPath.hasNext()) {
            return element;
        }

        /*
         * If compatibility with R3.0 map indexes were not necessary, this
         * code would exist to ensure that the [] keyword is used to
         * reference a record in a map.  As it stands R3 map indexes can
         * be used to index a specific key value, which appears here and
         * must be silently consumed.
         *
         *  if (!ANONYMOUS.equalsIgnoreCase(currentField)) {
         *      throw new IllegalArgumentException
         *         ("Field not valid in field expression: " + currentField);
         *  }
         */

        /*
         * There are more components, call the element to resolve them.
         */
        return element.findField(fieldPath);
    }

    /**
     * If called for a map the fieldName applies to the key that is being
     * indexed in the map, so the target is the map's element.
     */
    @Override
    FieldDefImpl findField(String fieldName) {
        return element;
    }

    @Override
    void toJson(ObjectNode node) {
        super.toJson(node);
        ObjectNode collNode = node.putObject(COLLECTION);
        if (element != null) {
            element.toJson(collNode);
        }
    }

    /**
     * {
     *  "type": {
     *    "type" : "map",
     *    "values" : "simpleType"  or for a complex type
     *    "values" : {
     *        "type" : ...
     *        ...
     *     }
     *  }
     * }
     */
    @Override
    public JsonNode mapTypeToAvro(ObjectNode node) {
        if (node == null) {
            node = JsonUtils.createObjectNode();
        }

        node.put("type", "map");
        node.put("values", element.mapTypeToAvroJsonNode());
        return node;
    }

    @Override
    FieldValueImpl createValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return NullValueImpl.getInstance();
        }
        if (!node.isObject()) {
            throw new IllegalArgumentException
                ("Default value for type MAP is not a map");
        }
        if (node.size() != 0) {
            throw new IllegalArgumentException
                ("Default value for map must be null or an empty map");
        }
        return createMap();
    }

    @Override
    public short getRequiredSerialVersion() {
        return element.getRequiredSerialVersion();
    }

    /*
     * local methods
     */

    static boolean isMapKeyTag(String target) {
        return KEY_TAG.equalsIgnoreCase(target);
    }

    static boolean isMapValueTag(String target) {
        return ANONYMOUS.equalsIgnoreCase(target);
    }
}
