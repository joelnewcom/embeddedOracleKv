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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.Collections;

/**
 * This class encapsulates methods used to help parse and navigate paths
 * to nested fields in metadata, although it works with both simple and
 * complex paths.
 *
 * Simple fields (e.g. "name") have a single component. Fields that navigate
 * into nested fields (e.g. "address.city") have multiple components. The
 * state maintained by TableFieldPath includes:
 *
 * This class is also used by the query compiler. In the compiler, a path
 * expression is represented as multiple step expressions. This class is used
 * to collect information from each step expr and thus provide an alternative
 * representation of a path expr in a single java object. This is done when
 * we want to push secondary-index predicates from a WHERE clause down to a
 * scan over the index. The definition of a secondary index column is
 * represented as a TablePath, and we want to create another TablePath instance
 * from the compiler path expr in order to "match" the two.
 *
 * fieldMap:
 * The FieldMap of the containing object that provides context for navigations.
 * In most cases it will be the FieldMap associated with a TableImpl. In some
 * cases it is the FieldMap of a RecordValueImpl.
 *
 * steps:
 * The list of steps/components of the path. Simple fields will have a
 * single entry. Complex fields, more than one. The steps include identifiers,
 * as well as the "special" steps "_key" and "[]"
 *
 * pathName:
 * The full string name of the path. It is computed on-the-fly when needed (see
 * getPathName() method). The string has the "external" format, i.e., it looks
 * like a DML path expr. Specifically: 
 * - it uses ".keys()" for the keys of map fields,
 * - it uses ".values()" for the values of map fields,
 * - it uses "[]" for the values of array fields.
 *
 * isComplex:
 * True if this is a complex path.
 */
public class TablePath {

    final private FieldMap fieldMap;

    private List<String> steps;

    private String pathName;

    private boolean isComplex;

    public TablePath(TableImpl table, String path) {
        this(table.getFieldMap(), path);
    }

    protected TablePath(FieldMap fieldMap, String path) {

        this.fieldMap = fieldMap;

        if (path != null) {
            steps = parsePathName(path);
        } else {
            steps = new ArrayList<String>();
        }

        isComplex = (steps.size() > 1);
    }

    public TablePath(FieldMap fieldMap, List<String> steps) {
        this.fieldMap = fieldMap;
        this.steps = steps;
        isComplex = (steps.size() > 1);
    }

    @Override
    public String toString() {
        return getPathName();
    }

    @Override
    public int hashCode() {
        return getPathName().hashCode();
    }

    /*
     * Comparing the steps in order is sufficient to distinguish TablePath
     * instances. Comparisons are never made across tables, and all paths
     * within the same table are unique.
     */
    @Override
    public boolean equals(Object o) {

        if (!(o instanceof TablePath)) {
            return false;
        }

        TablePath other = (TablePath)o;

        if (steps.size() != other.steps.size()) {
            return false;
        }

        for (int i = 0; i < steps.size(); ++i) {
            if (!steps.get(i).equalsIgnoreCase(other.steps.get(i))) {
                return false;
            }
        }

        return true;
    }

    public void clear() {
        pathName = null;
        steps.clear();
        isComplex = false;
    }

    public boolean isEmpty() {
        return steps.isEmpty();
    }

    final FieldMap getFieldMap() {
        return fieldMap;
    }

    public final boolean isComplex() {
        return isComplex;
    }

    public int numSteps() {
        return steps.size();
    }

    public final List<String> getSteps() {
        return steps;
    }

    public final String getStep(int i) {
        return steps.get(i);
    }

    public final void add(String step) {
        steps.add(step);
        if (steps.size() > 1) {
            isComplex = true;
        }
        pathName = null;
    }

    public final void add(int pos, String step) {
        steps.add(pos, step);
        if (steps.size() > 1) {
            isComplex = true;
        }
        pathName = null;
    }

    public void reverseSteps() {
        Collections.reverse(steps);
    }

    ListIterator<String> iterator() {
        return steps.listIterator();
    }

    public final String getLastStep() {
        return steps.get(steps.size() - 1);
    }

    /**
     * Returns the FieldDef associated with the first (and maybe only)
     * component of the field.
     */
    public FieldDefImpl getFirstDef() {
        return fieldMap.getFieldDef(steps.get(0));
    }

    public final String getPathName() {

        if (pathName == null && steps != null) {

            String step = steps.get(0);
            assert(!step.equals(TableImpl.BRACKETS));
            assert(!step.equals(TableImpl.KEY_TAG));

            StringBuilder sb = new StringBuilder(step);

            for (int i = 1; i < steps.size(); ++i) {

                step = steps.get(i);

                if (step.equals(TableImpl.KEY_TAG)) {
                    sb.append(TableImpl.DOT_KEYS); // ".keys()"
                    assert(i == steps.size() - 1);

                } else if (steps.get(i).equals(TableImpl.BRACKETS)) {

                    StringBuilder sb2 = new StringBuilder();

                    for (int j = 0; j < i; ++j) {
                        sb2.append(steps.get(j));
                        if (j < i - 1) {
                            sb2.append(".");
                        }
                    }

                    FieldDefImpl pathDef =
                        TableImpl.findTableField(fieldMap, sb2.toString());

                    if (pathDef.isArray()) {
                        sb.append(step); // "[]"
                    } else {
                        assert(pathDef.isMap());
                        sb.append(TableImpl.DOT_VALUES); // ".values()"
                    }

                } else {
                    sb.append('.').append(step);
                }
            }

            pathName = sb.toString();
        }

        return pathName;
    }

    public final String concatPathSteps() {

        Iterator<String> iter = steps.iterator();
        return createPathName(iter);
    }

    /**
     * Returns a list of field names components in a complex field name,
     * or a single name if the field name is not complex (this is for
     * simplicity in use).
     */
    public static List<String> parsePathName(String pathname) {

        List<String> list = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();

        for (char ch : pathname.toCharArray()) {
            if (ch == '.') {
                if (sb.length() == 0) {
                    throw new IllegalArgumentException(
                        "Malformed field name: " + pathname);
                }
                list.add(sb.toString());
                sb.delete(0, sb.length());
            } else {
                sb.append(ch);
            }
        }

        if (sb.length() > 0) {
            list.add(sb.toString());
        }
        return list;
    }

    /**
     * Constructs a single dot-separated string field path from one or
     * more components.
     */
    public static String createPathName(Iterator<String> iter) {

        StringBuilder sb = new StringBuilder();
        while (iter.hasNext()) {
            String current = iter.next();
            sb.append(current);
            if (iter.hasNext()) {
                sb.append(TableImpl.SEPARATOR);
            }
        }
        return sb.toString();
    }
}
