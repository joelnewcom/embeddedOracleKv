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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;

import oracle.kv.impl.util.SerializationUtil;
import oracle.kv.table.ArrayValue;
import oracle.kv.table.FieldDef;
import oracle.kv.table.FieldValue;
import oracle.kv.table.MapDef;
import oracle.kv.table.MapValue;
import oracle.kv.table.RecordValue;

import static oracle.kv.impl.util.SerialVersion.QUERY_VERSION_2;

/**
 *
 */
public class FieldValueSerialization {

    private static final int NULL_VALUE = -1;
    private static final int NULL_REFERENCE = -2;
    private static final int NULL_JSON_VALUE = -3;

    /*******************************************************************
     *
     * Serialization methods
     *
     *******************************************************************/

    /**
     * If writeValDef is true, the deserializer does not have the FieldDef for
     * this value, or the FieldDef it knows about is a wildcard (in both of
     * these cases, the readFieldValue() method will be called with the def
     * param being null). In these cases, the serializer must serialize the
     * type as well as the value and the deserializer will read this type first
     * in order to parse the value bytes correctly.
     *
     * This variant of writeFieldValue should be called when it is possible
     * that the given FieldValue is null or the NullValue. In this case, the
     * method writes an extra byte at the start of the serialized value, to
     * indicate if the value is indeed null or NullValue. If the value turns
     * out to a "normal" one, the extra byte will store the kind of the value
     * (the enum returned by val.getType()).
     *
     * If neither null nor NullValue are possible, it's better to call the
     * second variant below, passing true for the "writeValKind" param, to
     * indicate that the value kind has not been written already. In this
     * case, the value kind will be written only if needed, ie., only if
     * the writeValDef param is also true.
     */
    public static void writeFieldValue(
        FieldValue val,
        boolean writeValDef,
        DataOutput out,
        short serialVersion) throws IOException {

        if (val == null) {
            out.writeByte(NULL_REFERENCE);
        } else if (val.isNull()) {
            out.writeByte(NULL_VALUE);
        } else if (val.isJsonNull()) {
            out.writeByte(NULL_JSON_VALUE);
        } else {
            val.getType().writeFastExternal(out, serialVersion);
            writeFieldValue(val, writeValDef, false, out, serialVersion);
        }
    }

    public static void writeFieldValue(
        FieldValue val,
        boolean writeValDef,
        boolean writeValKind,
        DataOutput out,
        short serialVersion) throws IOException {

        assert(val != null);
        assert(!val.isNull());

        FieldValueImpl value = (FieldValueImpl) val;
        FieldDefImpl valDef = (FieldDefImpl)val.getDefinition();

        /*
         * The following checks are valid under the following assumption:
         * RecordValues which are constructed by a record-constructor expr (not
         * yet implemented) will not have ANY_RECORD as their associated type.
         * Notice that a record-constructor expr will probably look like this:
         * "{" name_expr ":" value_expr ("," name_expr ":" value_expr)* "}"
         * If so, this assumption means that a RECORD type must be built on the
         * fly for each RecordValue constructed.
         */
        if (valDef.isWildcard() && !val.isJsonNull()) {
            throw new IllegalStateException(
                "An item cannot have a wildcard type\n" + val);
        }

        if (valDef.getType() != value.getType()) {
            throw new IllegalStateException(
                "Mismatch between value kind and associated type\n" +
                "Value kind : " + val.getType() + "\n" +
                "Type : " + valDef);
        }

        /*
         * Notice that we do NOT write the value kind if the receiver has type
         * info (i.e., if writeValDef == false). This has implications for the
         * query processor, and specifically for value-constructing exprs. For
         * example, if the static type of an array-constructor expr is
         * ARRAY(LONG), the constructed array must contain longs only, i.e.
         * it cannot contain integers. This means that if the static element
         * type of the array constructor is not a wildcard type, we must cast
         * every item produced by the input exprs of the array constructor to
         * that static element type. Furthermore, if the static type of the
         * top expr on the server side is, say, LONG, then we must cast each
         * item produced by that expr to LONG, before we serialized it and
         * ship it to the client. The check below enforces this restriction.
         */
        if (writeValDef && writeValKind) {
            if (val.isJsonNull()) {
                out.writeByte(NULL_JSON_VALUE);
                return;
            }
            val.getType().writeFastExternal(out, serialVersion);
        }
        switch (val.getType()) {
        case INTEGER:
            SerializationUtil.writePackedInt(out, value.getInt());
            break;
        case LONG:
            SerializationUtil.writePackedLong(out, value.getLong());
            break;
        case DOUBLE:
            out.writeDouble(value.getDouble());
            break;
        case FLOAT:
            out.writeFloat(value.getFloat());
            break;
        case STRING:
            out.writeUTF(value.getString());
            break;
        case BOOLEAN:
            out.writeBoolean(value.getBoolean());
            break;
        case NUMBER: {
            byte[] bytes = value.getBytes();
            SerializationUtil.writePackedInt(out, bytes.length);
            out.write(bytes);
            break;
        }
        case BINARY:
            byte[] bytes = value.getBytes();
            SerializationUtil.writePackedInt(out, bytes.length);
            if (bytes.length > 0) {
                out.write(bytes);
            }
            break;
        case FIXED_BINARY:
            /*
             * Write the (fixed) size of the binary. Fixed binary can only
             * be null or full-sized, so the size of its byte array is the
             * same as the defined size.
             */
            int size =((FixedBinaryDefImpl)valDef).getSize();
            SerializationUtil.writePackedInt(out, size);
            bytes = value.getBytes();
            assert bytes.length == size;
            out.write(bytes);
            break;
        case ENUM:
            if (writeValDef) {
                FieldDef def = value.getDefinition();
                FieldDefSerialization.writeEnum(def, out, serialVersion);
            }
            out.writeShort(value.asEnum().getIndex());
            break;
        case TIMESTAMP:
            if (writeValDef) {
                FieldDef def = value.getDefinition();
                FieldDefSerialization.writeTimestamp(def, out, serialVersion);
            }
            bytes = ((TimestampValueImpl)value).getBytes();
            assert(bytes.length > 0);
            out.writeByte(bytes.length);
            out.write(bytes);
            break;
        case RECORD:
            writeRecord(value, writeValDef, out, serialVersion);
            break;
        case MAP:
            writeMap(value, writeValDef, out, serialVersion);
            break;
        case ARRAY:
            writeArray(value, writeValDef, out, serialVersion);
            break;
        case ANY:
        case ANY_ATOMIC:
        case ANY_JSON_ATOMIC:
        case ANY_RECORD:
            throw new IllegalStateException
                ("ANY* types cannot be materialized as values");
        case JSON:
            throw new IllegalStateException
                ("JSON cannot be materialized as a value");
        case EMPTY:
            throw new IllegalStateException(
                "EMPTY type does not contain any values");
        }
    }

    /**
     * Record format:
     *
     * definition of the record (only if needed)
     * record fields, in definition order
     *  there is an optimization to avoid writing the type byte for
     *  fields that are not nullable, which means that there is no
     *  need to differentiate between a null value and non-null value.
     *
     * NOTE: it is unclear whether this optimization will be helpful or more
     * confusing to non-Java drivers when they must handle this format. If
     * the intent is to have these drivers treat data as *mostly* schemaless,
     * as they do with the JSON-based proxy, requiring them to understand
     * nullable vs not nullable fields may be excessive. Watch this space.
     */
    private static void writeRecord(
        FieldValueImpl val,
        boolean writeValDef,
        DataOutput out,
        short serialVersion) throws IOException {

        RecordDefImpl recordDef = (RecordDefImpl)val.getDefinition();
        RecordValueImpl record = (RecordValueImpl)val;

        if (writeValDef) {
            FieldDefSerialization.writeRecord(recordDef, out, serialVersion);
        }

        for (int pos = 0; pos < recordDef.getNumFields(); ++pos) {

            FieldDefImpl fdef = recordDef.getFieldDef(pos);
            FieldValueImpl fval = record.get(pos);

            /*
             * If the field is not nullable, call the 3rd version of
             * writeFieldValue, passing true for "writevalKind."
             * This will avoid writing the type byte if possible.
             */
            if (!recordDef.isNullable(pos)) {
                writeFieldValue(fval,
                                fdef.isWildcard(), // writeValDef
                                true,              // writeValKind
                                out, serialVersion);
            } else {
                writeFieldValue(fval,
                                fdef.isWildcard(), // writeValDef
                                out, serialVersion);
            }
        }
    }

    /**
     * Map format:
     *  element type (only if needed)
     *  int -- size
     *  entries:
     *    string -- key
     *    FieldValue -- value
     */
    private static void writeMap(
        FieldValueImpl value,
        boolean writeValDef,
        DataOutput out,
        short serialVersion) throws IOException {

        MapValueImpl map = (MapValueImpl)value.asMap();
        MapDef mapDef = map.getDefinition();
        FieldDefImpl elemDef = (FieldDefImpl)mapDef.getElement();
        boolean wildcard = elemDef.isWildcard();

        if (writeValDef) {
            FieldDefSerialization.writeFieldDef(elemDef, out, serialVersion);
        }

        int size = map.size();
        SerializationUtil.writePackedInt(out, size);

        if (size == 0) {
            return;
        }

        for (Map.Entry<String, FieldValue> entry :
                 map.getFieldsInternal().entrySet()) {

            out.writeUTF(entry.getKey());

            writeFieldValue(entry.getValue(),
                            wildcard, // writeValDef
                            true, // writeValKind
                            out, serialVersion);
        }
    }

    /**
     * Array format:
     *
     * Pre-QUERY_VERSION_2
     *  element type (only if needed)
     *  int -- size
     *  entries:
     *    FieldValue -- value
     *
     * QUERY_VERSION_2 (handles homogenous wildcard arrays of atomics)
     *  element def (if needed)
     *  if (element type is wildcard)
     *    boolean - isHomogeneous
     *    if (isHomogeneous)
     *      element def of the homogeneous type
     *  int - size
     *  entries - if the element type is homogeneous or is otherwise known,
     *    the definitions of the entries are not written, saving space
     */
    private static void writeArray(
        FieldValueImpl value,
        boolean writeValDef,
        DataOutput out,
        short serialVersion) throws IOException {

        ArrayValueImpl array = (ArrayValueImpl)value.asArray();

        FieldDefImpl elemDef = array.getElementDef();
        FieldDefImpl homogeneousType = array.getHomogeneousType();
        boolean wildcard = elemDef.isWildcard();
        boolean homogeneous = (homogeneousType != null);

        if (writeValDef) {
            FieldDefSerialization.writeFieldDef(elemDef, out, serialVersion);
        }

        if (serialVersion >= QUERY_VERSION_2 && wildcard) {

            out.writeBoolean(homogeneous);

            if (homogeneous) {
                FieldDefSerialization.writeFieldDef(homogeneousType,
                                                    out, serialVersion);
                wildcard = false;
            }
        }

        int size = array.size();
        SerializationUtil.writePackedInt(out, size);

        if (size == 0) {
            return;
        }

        for (FieldValue fieldVal : array.getArrayInternal()) {
            writeFieldValue(fieldVal,
                            wildcard, // writeValDef
                            true, // writeValKind
                            out,
                            serialVersion);
        }
    }

    /*******************************************************************
     *
     * Deserialization methods
     *
     *******************************************************************/

    public static FieldValue readFieldValue(
        FieldDef def,
        DataInput in,
        short serialVersion) throws IOException {

        int ordinal = in.readByte();

        if (ordinal == NULL_REFERENCE) {
            return null;
        }

        if (ordinal == NULL_VALUE) {
            return NullValueImpl.getInstance();
        }

        if (ordinal == NULL_JSON_VALUE) {
            return NullJsonValueImpl.getInstance();
        }

        FieldDef.Type valKind = FieldDef.Type.values()[ordinal];

        return readFieldValue(def, valKind, in, serialVersion);
    }

    public static FieldValue readFieldValue(
        FieldDef def,
        FieldDef.Type valKind,
        DataInput in,
        short serialVersion) throws IOException {

        if (def == null) {
            if (valKind == null) {
                int ordinal = in.readByte();

                if (ordinal == NULL_JSON_VALUE) {
                    return NullJsonValueImpl.getInstance();
                }

                valKind = FieldDef.Type.values()[ordinal];
            }
        } else if (valKind == null) {
            valKind = def.getType();
        }

        switch (valKind) {

        case INTEGER: {
            int val = SerializationUtil.readPackedInt(in);
            return FieldDefImpl.integerDef.createInteger(val);
        }
        case LONG: {
            long val = SerializationUtil.readPackedLong(in);
            return FieldDefImpl.longDef.createLong(val);
        }
        case DOUBLE: {
            double val = in.readDouble();
            return FieldDefImpl.doubleDef.createDouble(val);
        }
        case FLOAT: {
            float val = in.readFloat();
            return FieldDefImpl.floatDef.createFloat(val);
        }
        case STRING: {
            String val = in.readUTF();
            return FieldDefImpl.stringDef.createString(val);
        }
        case BOOLEAN: {
            return FieldDefImpl.booleanDef.createBoolean(in.readBoolean());
        }
        case NUMBER: {
            int len = SerializationUtil.readPackedInt(in);
            assert(len > 0);
            byte[] bytes = new byte[len];
            in.readFully(bytes);
            return FieldDefImpl.numberDef.createNumber(bytes);
        }
        case BINARY: {
            int len = SerializationUtil.readPackedInt(in);
            byte[] bytes = new byte[len];
            if (len > 0) {
                in.readFully(bytes);
            }
            return FieldDefImpl.binaryDef.createBinary(bytes);
        }
        case FIXED_BINARY: {
            int len = SerializationUtil.readPackedInt(in);
            byte[] bytes = new byte[len];
            if (len > 0) {
                in.readFully(bytes);
            }
            return new FixedBinaryDefImpl(len, null).createFixedBinary(bytes);
        }
        case ENUM: {
            EnumDefImpl enumDef =
                (def == null ?
                 FieldDefSerialization.readEnum(in, serialVersion) :
                 (EnumDefImpl) def);

            assert(enumDef != null);
            short index = in.readShort();
            return enumDef.createEnum(index);
        }
        case TIMESTAMP: {
            TimestampDefImpl timestampDef =
                (def == null ?
                 FieldDefSerialization.readTimestamp(in, serialVersion) :
                 (TimestampDefImpl) def);

            assert(timestampDef != null);
            int len = in.readByte();
            assert(len > 0);
            byte[] bytes = new byte[len];
            in.readFully(bytes);
            return timestampDef.createTimestamp(bytes);
        }
        case RECORD:
            return readRecord(def, in, serialVersion);
        case MAP:
            return readMap(def, in, serialVersion);
        case ARRAY:
            return readArray(def, in, serialVersion);
        default:
            throw new IllegalStateException("Type not supported: " + valKind);
        }
    }

    static RecordValue readRecord(
        FieldDef def,
        DataInput in,
        short serialVersion) throws IOException {

        RecordDefImpl recordDef =
            (def == null ?
             FieldDefSerialization.readRecord(in, serialVersion) :
             (RecordDefImpl)def);

        RecordValueImpl record = recordDef.createRecord();

        for (int pos = 0; pos < recordDef.getNumFields(); ++pos) {

            FieldDefImpl fdef = recordDef.getFieldDef(pos);
            if (fdef.isWildcard()) {
                fdef = null;
            }

            FieldValue fval = null;
            /*
             * If the field is not a wildcard, and it's not nullable its type will
             * not have been written. Use a different variant of readFieldValue().
             */
            if (fdef != null && !recordDef.isNullable(pos)) {
                fval = readFieldValue(fdef, fdef.getType(), in, serialVersion);
            } else {
                fval = readFieldValue(fdef, in, serialVersion);
            }

            if (fval != null) {
                record.putInternal(pos, fval);
            }
        }

        return record;
    }

    /**
     * See writeMap for expected format
     */
    static MapValue readMap(
        FieldDef def,
        DataInput in,
        short serialVersion) throws IOException {

        FieldDefImpl elemDef = null;
        MapDef mapDef = null;

        if (def != null) {
            mapDef =  def.asMap();
            elemDef = (FieldDefImpl)mapDef.getElement();
        } else {
            elemDef = FieldDefSerialization.readFieldDef(in, serialVersion);
            mapDef = FieldDefFactory.createMapDef(elemDef);
        }

        MapValueImpl map = (MapValueImpl) mapDef.createMap();

        boolean wildcard = elemDef.isWildcard();

        if (wildcard) {
            elemDef = null;
        }

        int size = SerializationUtil.readPackedInt(in);

        for (int i = 0; i < size; i++) {

            String fname = in.readUTF();
            FieldValue fval = readFieldValue(elemDef, null, in, serialVersion);

            assert(fval.isNull() ||
                   elemDef == null ||
                   fval.getType() == elemDef.getType());

            assert !fval.isNull();
            map.put(fname, fval);
        }

        return map;
    }

    /**
     * See writeArray for expected format
     */
    static ArrayValue readArray(
        FieldDef def,
        DataInput in,
        short serialVersion) throws IOException {

        ArrayDefImpl arrayDef = null;
        FieldDefImpl elemDef = null;
        boolean wildcard;

        if (def != null) {
            arrayDef = (ArrayDefImpl)def;
            elemDef = arrayDef.getElement();
            wildcard = elemDef.isWildcard();

        } else {
            elemDef = FieldDefSerialization.readFieldDef(in, serialVersion);
            arrayDef = FieldDefFactory.createArrayDef(elemDef);
            wildcard = elemDef.isWildcard();
        }
        ArrayValueImpl array = arrayDef.createArray();

        /*
         * If this is a wildcard array, the sender includes info about whether
         * the array is actually a homogeneous one, and if so, what is the
         * homogeneous type.
         */
        if (serialVersion >= QUERY_VERSION_2 && wildcard) {
            boolean homogeneous = in.readBoolean();

            if (homogeneous) {
                elemDef = FieldDefSerialization.readFieldDef(in, serialVersion);
                array.setHomogeneousType(elemDef);
                wildcard = false;
            }
        }

        if (wildcard) {
            /*
             * elemDef is passed as input to the readFieldValue() call below.
             * If it is a wildcard type, we set it to null, which means that we
             * don't have any type info for the elements, and we expect to find
             * such info in front of each element inside the serialized format.
             */
            elemDef = null;
        }

        int size = SerializationUtil.readPackedInt(in);

        for (int i = 0; i < size; i++) {

            FieldValue fval = readFieldValue(elemDef, null, in, serialVersion);

            assert(elemDef == null || fval.getType() == elemDef.getType()) ;

            array.add(fval);
        }

        return array;
    }
}
