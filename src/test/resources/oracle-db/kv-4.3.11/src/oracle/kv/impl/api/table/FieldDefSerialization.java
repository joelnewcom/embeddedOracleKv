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

import oracle.kv.impl.util.SerializationUtil;
import oracle.kv.table.FieldDef;
import oracle.kv.table.FieldValue;

/**
 * Methods to serialize and deserialize FieldDefImpl instances. It is assumed
 * that ranges and default values for types that support them need not be
 * included. This is because FieldDef instances are only (currently)
 * serialized when serializing a query plan, which will not contain these.
 *
 * If that assumption changes, then ranges and defaults and perhaps even
 * comments will be added.
 */
public class FieldDefSerialization {

    /*******************************************************************
     *
     * Serialization methods
     *
     *******************************************************************/

    /*
     * Serialize using ObjectOutput
     *
     * type -- byte ordinal
     * atomic types --
     */
    public static void writeFieldDef(FieldDef def,
                                     DataOutput out,
                                     short serialVersion) throws IOException {

            /*
             * The type of the value
             */
            def.getType().writeFastExternal(out, serialVersion);

            switch (def.getType()) {
            case INTEGER:
            case LONG:
            case DOUBLE:
            case FLOAT:
            case STRING:
            case BINARY:
            case BOOLEAN:
            case NUMBER:
            case ANY:
            case ANY_ATOMIC:
            case ANY_RECORD:
            case EMPTY:
            case JSON:
            case ANY_JSON_ATOMIC:
                break;
            case FIXED_BINARY:
                /*
                 * Write the (fixed) size of the binary. Fixed binary can only
                 * be null or full-sized, so the size of its byte array is the
                 * same as the defined size.
                 */
                int size =((FixedBinaryDefImpl)def).getSize();
                SerializationUtil.writePackedInt(out, size);
                break;
            case ENUM:
                writeEnum(def, out, serialVersion);
                break;
            case TIMESTAMP:
                writeTimestamp(def, out, serialVersion);
                break;
            case RECORD:
                writeRecord((RecordDefImpl)def, out, serialVersion);
                break;
            case MAP:
                writeMap((MapDefImpl)def, out, serialVersion);
                break;
            case ARRAY:
                writeArray((ArrayDefImpl)def, out, serialVersion);
                break;
            }
    }

    /*
     * int -- numFields
     * fields (name, field)
     */
    static void writeRecord(RecordDefImpl def,
                            DataOutput out,
                            short serialVersion) throws IOException {

        SerializationUtil.writeString(out, def.getName());
        SerializationUtil.writePackedInt(out, def.getNumFields());

        for (FieldMapEntry fme : def.getFieldProperties()) {
            String fname = fme.getFieldName();
            FieldDefImpl fdef = fme.getFieldDef();
            SerializationUtil.writeString(out, fname);
            writeFieldDef(fdef, out, serialVersion);
            boolean nullable = fme.isNullable();
            out.writeBoolean(nullable);
            if (!nullable) {
                FieldValue defVal = fme.getDefaultValue();
                assert(defVal != null);
                FieldValueSerialization.writeFieldValue(
                    defVal, fdef.isWildcard() /* writeValDef */,
                    out, serialVersion);
            }
        }
    }

    /*
     * A map just has its element.
     */
    static void writeMap(MapDefImpl def,
                         DataOutput out,
                         short serialVersion) throws IOException {

        writeFieldDef(def.getElement(), out, serialVersion);
    }

    /*
     * An array just has its element.
     */
    static void writeArray(ArrayDefImpl def,
                           DataOutput out,
                           short serialVersion) throws IOException {

        writeFieldDef(def.getElement(), out, serialVersion);
    }

    @SuppressWarnings("unused")
    static void writeEnum(FieldDef def, DataOutput out, short serialVersion)
            throws IOException {

        EnumDefImpl enumDef = (EnumDefImpl) def;

        String[] values = enumDef.getValues();
        SerializationUtil.writePackedInt(out, values.length);
        for (String value : values) {
            SerializationUtil.writeString(out, value);
        }
    }

    @SuppressWarnings("unused")
    static void writeTimestamp(FieldDef def,
                               DataOutput out,
                               short serialVersion)
        throws IOException {

        SerializationUtil.writePackedInt(out, def.asTimestamp().getPrecision());
    }

    /*******************************************************************
     *
     * Deserialization methods
     *
     *******************************************************************/

    public static FieldDefImpl readFieldDef(DataInput in,
                                            short serialVersion)
            throws IOException {

        FieldDef.Type type = FieldDef.Type.readFastExternal(in, serialVersion);
        switch (type) {
        case INTEGER:
            return FieldDefImpl.integerDef;
        case LONG:
            return FieldDefImpl.longDef;
        case DOUBLE:
            return FieldDefImpl.doubleDef;
        case FLOAT:
            return FieldDefImpl.floatDef;
        case STRING:
            return FieldDefImpl.stringDef;
        case BINARY:
            return FieldDefImpl.binaryDef;
        case BOOLEAN:
            return FieldDefImpl.booleanDef;
        case NUMBER:
            return FieldDefImpl.numberDef;
        case FIXED_BINARY:
            int size = SerializationUtil.readPackedInt(in);
            return new FixedBinaryDefImpl(size, null);
        case ENUM:
            return readEnum(in, serialVersion);
        case TIMESTAMP:
            return readTimestamp(in, serialVersion);
        case RECORD:
            return readRecord(in, serialVersion);
        case MAP:
            return readMap(in, serialVersion);
        case ARRAY:
            return readArray(in, serialVersion);
        case ANY:
            return FieldDefImpl.anyDef;
        case ANY_ATOMIC:
            return FieldDefImpl.anyAtomicDef;
        case ANY_JSON_ATOMIC:
            return FieldDefImpl.anyJsonAtomicDef;
        case ANY_RECORD:
            return FieldDefImpl.anyRecordDef;
        case EMPTY:
            return FieldDefImpl.emptyDef;
        case JSON:
            return FieldDefImpl.jsonDef;
        default:
            throw new IllegalStateException("Unknown type code: " + type);
        }
    }

    static RecordDefImpl readRecord(DataInput in, short serialVersion)
            throws IOException {

        String name = SerializationUtil.readString(in);
        int size = SerializationUtil.readPackedInt(in);

        FieldMap fieldMap = new FieldMap();

        for (int i = 0; i < size; i++) {
            String fname = SerializationUtil.readString(in);
            FieldDefImpl fdef = readFieldDef(in, serialVersion);
            boolean nullable = in.readBoolean();
            FieldValueImpl defVal = null;
            if (!nullable) {
                defVal = (FieldValueImpl)
                    FieldValueSerialization.readFieldValue(
                        (fdef.isWildcard() ? null : fdef),
                        in, serialVersion);
            }
            fieldMap.put(fname, fdef, nullable, defVal);
        }

        if (name == null) {
            return new RecordDefImpl(fieldMap, null/*description*/);
        }

        return new RecordDefImpl(name, fieldMap);
    }

    /*
     * A map just has its element.
     */
    static MapDefImpl readMap(DataInput in, short serialVersion)
            throws IOException {

        return FieldDefFactory.createMapDef(readFieldDef(in, serialVersion));
    }

    /*
     * An array just has its element.
     */
    static ArrayDefImpl readArray(DataInput in, short serialVersion)
            throws IOException {

        return FieldDefFactory.createArrayDef(readFieldDef(in, serialVersion));
    }

    @SuppressWarnings("unused")
    static EnumDefImpl readEnum(DataInput in, short serialVersion)
            throws IOException {

        int numValues = SerializationUtil.readPackedInt(in);
        String[] values = new String[numValues];
        for (int i = 0; i < numValues; i++) {
            values[i] = SerializationUtil.readString(in);
        }
        return new EnumDefImpl(values, null);
    }

    @SuppressWarnings("unused")
    static TimestampDefImpl readTimestamp(DataInput in, short serialVersion)
        throws IOException {

        return new TimestampDefImpl(SerializationUtil.readPackedInt(in));
    }
}
