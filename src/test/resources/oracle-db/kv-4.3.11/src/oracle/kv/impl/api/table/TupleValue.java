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

import java.util.List;

import oracle.kv.table.FieldDef;
import oracle.kv.table.FieldValue;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;

/**
 * TupleValue instances are created internally only during query execution
 * and are never returned to the application.
 *
 * See the "Records vs tuples" section in the javadoc of PlanIter (in
 * impl/query/runtime/PlanIter.java) for more details.
 *
 * This class could have been placed in the query.runtime package. However
 * doing so would require making many of the package-scoped methds of
 * FieldValueImpl public or protected methods. This is the reason we chose
 * to put TupleValue in the api.table package.
 */
public class TupleValue extends FieldValueImpl {

    private static final long serialVersionUID = 1L;

    final FieldValueImpl[] theRegisters;

    final int[] theTupleRegs;

    final RecordDefImpl theDef;

    public TupleValue(RecordDefImpl def, FieldValueImpl[] regs, int[] regIds) {
        super();
        theRegisters = regs;
        theTupleRegs = regIds;
        theDef = def;
    }

    /*
     * Public api methods from Object and FieldValue
     */

    @Override
    public FieldValueImpl clone() {
        throw new IllegalStateException(
            "TupleValue does not implement clone");
    }

    @Override
    public String toString() {
        return toJsonString(false);
    }

    @Override
    public int hashCode() {
        int code = size();
        for (int i = 0; i < size(); ++i) {
            String fname = theDef.getFieldName(i);
            code += fname.hashCode();
            code += get(i).hashCode();
        }
        return code;
    }

    @Override
    public boolean equals(Object otherObj) {

        if (this == otherObj) {
            return true;
        }

        if (otherObj instanceof RecordValueImpl) {
            RecordValueImpl other = (RecordValueImpl) otherObj;

            /* field-by-field comparison */
            if (size() == other.size() &&
                getDefinition().equals(other.getDefinition())) {

                for (int i = 0; i < size(); ++i) {

                    FieldValue val1 = get(i);
                    FieldValue val2 = other.get(i);
                    if (!val1.equals(val2)) {
                        return false;
                    }
                }

                return true;
            }

            return false;
        }

        if (otherObj instanceof TupleValue) {
            TupleValue other = (TupleValue) otherObj;

            if (size() == other.size() &&
                getDefinition().equals(other.getDefinition())) {

                for (int i = 0; i < size(); ++i) {
                    FieldValue val1 = get(i);
                    FieldValue val2 = other.get(i);
                    if (!val1.equals(val2)) {
                        return false;
                    }
                }

                return true;
            }

            return false;
        }

        return false;
    }

    @Override
    public int compareTo(FieldValue o) {
        throw new IllegalStateException(
            "TupleValue is not comparable to any other value");
    }

    @Override
    public FieldDef.Type getType() {
        return FieldDef.Type.RECORD;
    }

    @Override
    public boolean isComplex() {
        return true;
    }

    @Override
    public RecordDefImpl getDefinition() {
        return theDef;
    }

    /*
     * Public api methods from RecordValue
     */

    @Override
    public int size() {
        return theTupleRegs.length;
    }

    public List<String> getFieldNames() {
        return getDefinition().getFieldNames();
    }

    public List<String> getFieldNamesInternal() {
        return getDefinition().getFieldNamesInternal();
    }

    public String getFieldName(int pos) {
        return getDefinition().getFieldName(pos);
    }

    public int getFieldPos(String fieldName) {
        return getDefinition().getFieldPos(fieldName);
    }

    public FieldValueImpl get(String fieldName) {
        int pos = getDefinition().getFieldPos(fieldName);
        return theRegisters[theTupleRegs[pos]];
    }

    public FieldValueImpl get(int pos) {
        return theRegisters[theTupleRegs[pos]];
    }

    /*
     * FieldValueImpl internal api methods
     */

    @Override
    public boolean isTuple() {
        return true;
    }

    @Override
    public JsonNode toJsonNode() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();

        for (int i = 0; i < theTupleRegs.length; ++i) {
            FieldValueImpl val = get(i);
            assert(val != null);
            node.put(getFieldName(i), val.toJsonNode());
        }
        return node;
    }

    @Override
    public void toStringBuilder(StringBuilder sb) {
        sb.append('{');
        for (int i = 0; i < theTupleRegs.length; ++i) {
            FieldValueImpl val = get(i);
            assert(val != null);
            if (i > 0) {
                sb.append(',');
            }
            sb.append('\"');
            sb.append(getDefinition().getFieldName(i));
            sb.append('\"');
            sb.append(':');
            val.toStringBuilder(sb);
        }
        sb.append('}');
    }

    /*
     * Local methods
     */

    public int getNumFields() {
        return theTupleRegs.length;
    }

    FieldDefImpl getFieldDef(String fieldName) {
        return getDefinition().getField(fieldName);
    }

    FieldDefImpl getFieldDef(int pos) {
        return getDefinition().getFieldDef(pos);
    }

    void putFieldValue(int fieldPos, FieldValueImpl value) {
        theRegisters[theTupleRegs[fieldPos]] = value;
    }

    public RecordValueImpl toRecord() {

        RecordValueImpl rec = theDef.createRecord();

        for (int i = 0; i < size(); ++i) {
            rec.put(i, get(i));
        }

        return rec;
    }

    public void toTuple(RecordValueImpl rec) {

        assert(theDef.equals(rec.getDefinition()));

        for (int i = 0; i < size(); ++i) {
            theRegisters[theTupleRegs[i]] = rec.get(i);
        }
    }
}
