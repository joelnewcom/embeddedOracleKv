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

package oracle.kv.table;

/**
 * A wrapper class for return values from
 * {@link TableAPI#tableKeysIterator(IndexKey, MultiRowOptions,
 *  oracle.kv.table.TableIteratorOptions)}
 * This classes allows the iterator to return all field value information that
 * can be obtained directly from the index without an additional fetch.
 *
 * Note: this class has a natural ordering that is inconsistent with
 * equals. Ordering is based on the indexKey only.
 *
 * @since 3.0
 */
public class KeyPair implements Comparable<KeyPair> {
    private final PrimaryKey primaryKey;
    private final IndexKey indexKey;

    /**
     * @hidden
     * For internal use only.
     */
    public KeyPair(PrimaryKey primaryKey, IndexKey indexKey) {
        this.primaryKey = primaryKey;
        this.indexKey = indexKey;
    }

    /**
     * Returns the PrimaryKey from the pair.
     *
     * @return the PrimaryKey
     */
    public PrimaryKey getPrimaryKey() {
        return primaryKey;
    }

    /**
     * Returns the IndexKey from the pair.
     *
     * @return the IndexKey
     */
    public IndexKey getIndexKey() {
        return indexKey;
    }

    /**
     * Compares the IndexKey of this object with the IndexKey of the specified
     * object for order.  If the IndexKey values are the same a secondary
     * comparison is done on the PrimaryKey values.
     */
    @Override
    public int compareTo(KeyPair other) {
        int value = indexKey.compareTo(other.getIndexKey());
        if (value == 0) {
            value = primaryKey.compareTo(other.getPrimaryKey());
        }
        return value;
    }
}

