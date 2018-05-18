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

package oracle.kv.impl.api.ops;

import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.DbInternal.Search;
import com.sleepycat.je.Get;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationResult;
import com.sleepycat.je.SecondaryCursor;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.Transaction;

import oracle.kv.impl.api.table.IndexImpl;
import oracle.kv.impl.api.table.IndexKeyImpl;
import oracle.kv.impl.api.table.IndexRange;
import oracle.kv.impl.util.TxnUtil;

/**
 * A class to encapsulate index iteration over secondary databases.
 *
 * The usage pattern is:
 *   IndexScanner scanner = new IndexScanner(...);
 *   try {
 *     while (scanner.next()) {
 *        DatabaseEntry indexKeyEntry = scanner.getIndexKey();
 *        DatabaseEntry primaryKeyEntry = scanner.getPrimaryKey();
 *        DatabaseEntry dataEntry = scanner.getData();
 *        // do things with indexKeyEntry, primaryKeyEntry, dataEntry
 *     }
 *   } finally {
 *     scanner.close();  // closes cursor
 *   }
 *
 * The next() call returns false when there are no more entries in range.
 *
 * Any security-relevant operations are performed by callers. Batching is
 * also handled by callers.
 */
public class IndexScanner {

    private final IndexImpl index;
    private final IndexRange range;
    private final byte[] resumeSecondaryKey;
    private final byte[] resumePrimaryKey;
    private final SecondaryCursor cursor;
    private final DatabaseEntry dataEntry;
    private final LockMode lockMode;

    /*
     * These are "final" once initialized
     */
    private DatabaseEntry indexKeyEntry;
    private DatabaseEntry primaryKeyEntry;
    private boolean initialized = false;
    private OperationResult result;

    IndexScanner(Transaction txn,
                 SecondaryDatabase db,
                 IndexImpl index,
                 IndexRange range,
                 byte[] resumeSecondaryKey,
                 byte[] resumePrimaryKey,
                 boolean keyOnly) {
        this(txn, db, index, range, resumeSecondaryKey,
             resumePrimaryKey, OperationHandler.CURSOR_READ_COMMITTED,
             LockMode.DEFAULT, keyOnly);
    }

    IndexScanner(Transaction txn,
                 SecondaryDatabase db,
                 IndexImpl index,
                 IndexRange range,
                 byte[] resumeSecondaryKey,
                 byte[] resumePrimaryKey,
                 CursorConfig cursorConfig,
                 LockMode lockMode,
                 boolean keyOnly) {

        this.index = index;
        this.range = range;
        this.resumeSecondaryKey = resumeSecondaryKey;
        this.resumePrimaryKey = resumePrimaryKey;
        this.lockMode = lockMode;

        dataEntry = (keyOnly ? null : new DatabaseEntry());

        cursor = db.openCursor(txn, cursorConfig);
    }

    /**
     * Returns the index key
     */
    public final DatabaseEntry getIndexKey() {
        return indexKeyEntry;
    }

    /**
     * Returns the primary key
     */
    public final DatabaseEntry getPrimaryKey() {
        return primaryKeyEntry;
    }

    /**
     * Returns the data
     */
    public final DatabaseEntry getData() {
        return dataEntry;
    }

    /**
     * Returns the cursor
     */
    final SecondaryCursor getCursor() {
        return cursor;
    }

    /**
     * This is called to lock the current data entry for key-only scans.
     */
    public boolean getCurrent() {
        assert(dataEntry == null);
        result = cursor.get(indexKeyEntry,
                            primaryKeyEntry,
                            dataEntry,
                            Get.CURRENT,
                            LockMode.DEFAULT.toReadOptions());
        return result != null;
    }

    /**
     * This is called if this.dataEntry is partial and the data needs to be
     * locked and fetched.
     */
    public boolean getLockedData(DatabaseEntry newEntry) {
        assert !newEntry.getPartial();
        result = cursor.get(indexKeyEntry,
                            primaryKeyEntry,
                            newEntry,
                            Get.CURRENT,
                            LockMode.DEFAULT.toReadOptions());
        return result != null;
    }

    /**
     * Returns the expiration time of the current valid result if non-null,
     * otherwise 0.
     *
     * This means that the caller must have received a true result from
     * one of the navigational interfaces indicating there's a current
     * record.
     */
    public long getExpirationTime() {
        return (result != null ? result.getExpirationTime() : 0);
    }

    /**
     * Returns the current OperationResult. If the most recent operation failed
     * to find a record this will be null.
     */
    public OperationResult getResult() {
        return result;
    }

    /**
     * Closes the scanner.
     */
    public void close() {
        TxnUtil.close(cursor);
    }

    /*
     * Gets the next, or previous entry and checks range, returning true if
     * there is a "next" key and it is in range.
     */
    public boolean next() {
        boolean haveKey;
        if (range.isReverse()) {
            haveKey = getPrev();
        } else {
            haveKey = getNext();
        }
        if (haveKey) {
            return inRange(indexKeyEntry.getData());
        }
        return false;
    }

    /*
     * Forward scans
     */
    private boolean getNext() {

        if (!initialized) {
            initialized = true;
            return initForward();
        }

        if (range.getExactMatch()) {
            result = cursor.get(indexKeyEntry,
                                primaryKeyEntry,
                                dataEntry,
                                Get.NEXT_DUP,
                                lockMode.toReadOptions());
        } else {
            result = cursor.get(indexKeyEntry,
                                primaryKeyEntry,
                                dataEntry,
                                Get.NEXT,
                                lockMode.toReadOptions());
        }
        return result != null;
    }

    private boolean initForward() {

        byte[] startKey = range.getStartKey();

        /*
         * If resuming an iteration, start there.  It overrides the
         * startKey.  A resumeKey doesn't mean that it isn't an exact
         * match query.  It's possible that batch size is < number of
         * duplicates.
         */
        if (resumeSecondaryKey != null) {
            indexKeyEntry = new DatabaseEntry(resumeSecondaryKey);
            primaryKeyEntry = new DatabaseEntry(resumePrimaryKey);
            result = resume();
        } else if (startKey != null) {
            indexKeyEntry = new DatabaseEntry(startKey);
            primaryKeyEntry = new DatabaseEntry();
            if (range.getExactMatch()) {
                result = cursor.get(indexKeyEntry,
                                    primaryKeyEntry,
                                    dataEntry,
                                    Get.SEARCH,
                                    lockMode.toReadOptions());
            } else {
                result = cursor.get(indexKeyEntry,
                                    primaryKeyEntry,
                                    dataEntry,
                                    Get.SEARCH_GTE,
                                    lockMode.toReadOptions());
            }
        } else {

            /*
             * This is a full index scan.
             *
             * If somehow an invalid IndexRange was sent and exact match is
             * true in this path, return no entries.
             */
            if (range.getExactMatch()) {
                return false;
            }

            indexKeyEntry = new DatabaseEntry();
            primaryKeyEntry = new DatabaseEntry();
            result = cursor.get(indexKeyEntry,
                                primaryKeyEntry,
                                dataEntry,
                                Get.FIRST,
                                lockMode.toReadOptions());
        }
        return result != null;
    }

    /**
     * Do a reverse index scan.  Positioning of the cursor is tricky
     * for this case.  "start" is the end of the scan and "end" is the start.
     * A resumeKey is equivalent to the end (which is inclusive).
     * 1.  No range constraints.  Start at the end and move backwards.  This
     * happens when there is no end key and no prefix.
     * 2.  Start only.  Start at the end and move backwards to start.  This
     * happens when start is set and there is no prefix
     * 3.  End only.  Find end, move backwards.  This happens when there is
     * an end key and no prefix.
     * 4.  Prefix serves as both start and/or end.  When specified with a
     * start key only it is used to find the end of the range.  When specified
     * with an end key it is used to terminate the iteration.  When specified
     * alone it is both the start and the end.
     */
    private boolean getPrev() {

        if (!initialized) {
            initialized = true;
            return initReverse();
        }
        if (range.getExactMatch()) {
            result = cursor.get(indexKeyEntry,
                                primaryKeyEntry,
                                dataEntry,
                                Get.PREV_DUP,
                                lockMode.toReadOptions());
        } else {
            result = cursor.get(indexKeyEntry,
                                primaryKeyEntry,
                                dataEntry,
                                Get.PREV,
                                lockMode.toReadOptions());
        }
        return result != null;
    }

    private boolean initReverse() {

        byte[] endKey = range.getEndKey();

        if (resumeSecondaryKey != null) {
            indexKeyEntry = new DatabaseEntry(resumeSecondaryKey);
            primaryKeyEntry = new DatabaseEntry(resumePrimaryKey);
            result = resume();
        } else if (endKey != null) {
            /*
             * End keys are exclusive.  Move to the prev sec key, and to the
             * last dup for that key.
             */
            indexKeyEntry = new DatabaseEntry(endKey);
            primaryKeyEntry = new DatabaseEntry();

            result = DbInternal.search(cursor, indexKeyEntry, primaryKeyEntry,
                                       dataEntry, Search.LT,
                                       lockMode.toReadOptions());
        } else if (range.getPrefixKey() != null) {
            indexKeyEntry = new DatabaseEntry();
            primaryKeyEntry = new DatabaseEntry();
            result = getEndFromPrefix();
        } else {
            /*
             * This is either a complete index iteration or an iteration
             * without a bounded end.  In both cases start at the last
             * record in the index.
             */
            indexKeyEntry = new DatabaseEntry();
            primaryKeyEntry = new DatabaseEntry();
            result = cursor.get(indexKeyEntry,
                                primaryKeyEntry,
                                dataEntry,
                                Get.LAST,
                                lockMode.toReadOptions());
        }
        return result != null;
    }

    /**
     * Moves to the record after (or before, for a reverse scan) the given
     * key/pkey.
     */
    private OperationResult resume() {

        final Search search = range.isReverse() ? Search.LT : Search.GT;

        /* First search within dups for the given sec key. */
        result = DbInternal.searchBoth(
            cursor, indexKeyEntry, primaryKeyEntry, dataEntry, search,
            lockMode.toReadOptions());

        /*
         * If NOTFOUND and exact-match then we're done because the search is
         * limited to the dups within the given sec key.
         */
        if (result != null || range.getExactMatch()) {
            return result;
        }

        /*
         * There are no more records with the given sec key.  Move to the next
         * (or prev) sec key, and to the first (or last) dup for that key.
         */
        return DbInternal.search(cursor, indexKeyEntry, primaryKeyEntry,
                                 dataEntry, search, lockMode.toReadOptions());
    }

    /**
     * Get the last matching record from a prefix.  This is a reverse scan
     * that has a prefix but no end key so the end is implicitly at the
     * end of the prefix.  Index field serialization does not include
     * explicit separators (like primary key serialization) but it does have
     * schema available and prefixes are complete, valid fields.  The algorithm
     * is:
     * 1.  deserialize the prefix to IndexKeyImpl
     * 2.  add "one" to the index key using IndexKeyImpl.incrementLastField().
     * 3.  reserialize and use this as an exclusive key.
     */
    private OperationResult getEndFromPrefix() {

        assert range.getPrefixKey() != null;

        /* Deserialize */
        IndexKeyImpl indexKey = index.deserializeIndexKey(range.getPrefixKey(),
                                                          true); // allowPartial

        /*
         * Increment the last field with a value in the index key.  If this
         * returns false then there are no further keys so go to the end of
         * the index.
         */
        if (!indexKey.incrementIndexKey()) {
            /* Nothing there, go to the end of the index */
            return cursor.get(indexKeyEntry, primaryKeyEntry, dataEntry,
                              Get.LAST, lockMode.toReadOptions());
        }

        /* Reserialize */
        byte[] bytes = index.serializeIndexKey(indexKey);

        /*
         * Look for end.  This is an exclusive value.  Prefixes in this
         * case are, by definition inclusive.  End keys can only be
         * exclusive if explicitly declared and that path does not call
         * this function.
         */
        indexKeyEntry.setData(bytes);

        /* Match record < exclusive key */
        return DbInternal.search(cursor, indexKeyEntry, primaryKeyEntry,
                                 dataEntry, Search.LT,lockMode.toReadOptions());
    }

    private boolean inRange(byte[] checkKey) {
        return range.inRange(checkKey);
    }
}
