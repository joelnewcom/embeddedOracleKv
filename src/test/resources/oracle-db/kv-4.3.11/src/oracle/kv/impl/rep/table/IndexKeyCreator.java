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

package oracle.kv.impl.rep.table;

import java.util.List;
import java.util.Set;

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;
import com.sleepycat.je.SecondaryMultiKeyCreator;
import oracle.kv.impl.api.table.IndexImpl;

/**
 *
 */
public class IndexKeyCreator implements SecondaryKeyCreator,
                                        SecondaryMultiKeyCreator {

    private volatile IndexImpl index;
    /*
     * Keep this state to make access faster
     */
    private final boolean keyOnly;
    private final boolean isMultiKey;

    IndexKeyCreator(IndexImpl index) {
        this.index = index;
        this.keyOnly = index.isKeyOnly();
        this.isMultiKey = index.isMultiKey();
    }

    boolean primaryKeyOnly() {
        return keyOnly;
    }

    boolean isMultiKey() {
        return isMultiKey;
    }

    void setIndex(IndexImpl newIndex) {
        index = newIndex;
    }

    /* -- From SecondaryKeyCreator -- */

    @Override
    public boolean createSecondaryKey(SecondaryDatabase secondaryDb,
                                      DatabaseEntry key,
                                      DatabaseEntry data,
                                      DatabaseEntry result) {
        byte[] res =
            index.extractIndexKey(key.getData(),
                                  (data != null ? data.getData() : null),
                                  keyOnly);
        if (res != null) {
            result.setData(res);
            return true;
        }
        return false;
    }

    /* -- From SecondaryMultiKeyCreator -- */

    @Override
    public void createSecondaryKeys(SecondaryDatabase secondaryDb,
                                    DatabaseEntry key,
                                    DatabaseEntry data,
                                    Set<DatabaseEntry> results) {

        /*
         * Ideally we'd pass the results Set to index.extractIndexKeys but
         * IndexImpl is client side as well and DatabaseEntry is not currently
         * in the client classes pulled from JE.  DatabaseEntry is simple, but
         * also references other JE classes that are not client side.  It is a
         * slippery slope.
         *
         * If the extra object allocations show up in profiling then something
         * can be done.
         */
        List<byte[]> res = index.extractIndexKeys(key.getData(),
                                                  data.getData(),
                                                  keyOnly);
        if (res != null) {
            for (byte[] bytes : res) {
                results.add(new DatabaseEntry(bytes));
            }
        }
    }

    @Override
    public String toString() {
        return "IndexKeyCreator[" + index.getName() + "]";
    }
}
