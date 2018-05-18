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

import com.sleepycat.je.tree.Key;

import oracle.kv.Direction;
import oracle.kv.impl.util.FastExternalizable;
import oracle.kv.table.FieldRange;
import oracle.kv.table.MultiRowOptions;
import oracle.kv.table.TableIteratorOptions;

/**
 * IndexRange is an internal class used to define the parameters for an index
 * scan.  The range is defined as a range of byte[] arrays to be used.  All
 * fields are optional.  Without an IndexRange an index scan operates over the
 * entire index.
 *
 * prefixKey -- if an IndexKey is specified then its fields are used as an
 * exact match prefix for the operation.  It is sent independently so that
 * it can be used on the server side to stop iteration
 *
 * startKey -- the starting key for a forward scan or end for a reverse scan.
 * this is implicitly inclusive.
 *
 * endKey -- the ending key for a forward scan or the start key for a reverse
 * scan.  This is implicitly exclusive.
 *
 * direction -- the Direction of the scan.  This is encapsulated here because
 * it fits with the rest of the state.
 *
 * exactMatch -- set to true if the operation is an exact match vs a scan, as
 * indicated by a completely specified IndexKey.
 */
public class IndexRange implements FastExternalizable {

    private static final int FLAG_FORWARD = 0x1;
    private static final int FLAG_REVERSE = 0x2;
    private static final int FLAG_UNORDERED = 0x4;
    private static final int FLAG_EXACT = 0x8;

    private /* final */ byte[] prefixKey;
    private /* final */ byte[] startKey;
    private /* final */ byte[] endKey;
    private final boolean exactMatch;
    private final Direction direction;

    IndexRange(IndexKeyImpl indexKey,
               MultiRowOptions getOptions,
               TableIteratorOptions iterateOptions) {
        this(indexKey,
             getOptions != null ? getOptions.getFieldRange() : null,
             iterateOptions == null ? Direction.FORWARD :
             iterateOptions.getDirection());
    }

    public IndexRange(IndexKeyImpl indexKey,
                      FieldRange range,
                      Direction direction) {

        IndexKeyImpl startTarget = indexKey;
        IndexKeyImpl endTarget = null;
        boolean startInclusive = true;
        this.direction = direction;

        if (indexKey.isComplete()) {
            if (range != null) {
                throw new IllegalArgumentException
                    ("FieldRange may not be used with a fully " +
                     "specified IndexKey");
            }
            exactMatch = true;

        } else if (range != null) {
            /*
             * Start key
             */
            if (range.getStart() != null) {
                startTarget = indexKey.clone();
                /*
                 * Add the start range value to the IndexKey
                 */
                startTarget.put(range.getFieldName(), range.getStart());
                startInclusive = range.getStartInclusive();
            }
            /*
             * End key
             */
            if (range.getEnd() != null) {
                endTarget = indexKey.clone();
                /*
                 * Add the end range value to the IndexKey
                 */
                endTarget.put(range.getFieldName(), range.getEnd());
                endTarget.validate();
            }

            exactMatch = false;

        } else {
            exactMatch = false;
        }

        if (startTarget.size() != 0) {

            startTarget.validate();
            /*
             * If exclusive, increment the key.  If this fails that means
             * that there are not going to be ANY keys in the valid range.
             * For example the user may have passed the maximum enum value
             * as the exclusive start value for an index of (enum, int, ...).
             */
            if (!startInclusive) {
                if (!startTarget.incrementIndexKey()) {
                    throw new IllegalArgumentException
                        ("Exclusive start value on an indexed field cannot " +
                         "be its maximum value");
                }
            }

            byte[] start =
                startTarget.getIndexImpl().serializeIndexKey(startTarget);

            /*
             * startKey and indexKey are identical, then this is a simple prefix
             * match.
             */
            if (startTarget == indexKey) {
                /* no range, just match the index key */
                prefixKey = start;
                startKey = start;
            } else {
                startKey = start;
                prefixKey = indexKey.size() > 0 ?
                    indexKey.getIndexImpl().serializeIndexKey(indexKey) :
                    null;
            }
        } else {
            prefixKey = null;
            startKey = null;
        }

        if (endTarget != null) {
            /*
             * If the end key is inclusive increment the last field value
             * so that the serialized array can be treated as exclusive.
             * Range cannot be null here, but this keeps Eclipse happy.
             */
            if (range != null && range.getEndInclusive()) {
                if (!endTarget.incrementIndexKey()) {
                    /*
                     * This means that the end will actually be the end
                     * of the index, so zero out the end key.
                     */
                    endKey = null;
                    return;
                }
            }
            endKey = endTarget.getIndexImpl().serializeIndexKey(endTarget);
        } else {
            if (range != null &&
                indexKey.getIndexImpl().allowNull(indexKey.size())) {
                /*
                 * Follow the rule of NULL last, for start-no-end range, add
                 * end target in which use NULL for the range field, it is to
                 * filter out the rows that contains NULL for the range field.
                 */
                endTarget = indexKey.clone();
                endTarget.putNull(range.getFieldName());
                endKey = endTarget.getIndexImpl().serializeIndexKey(endTarget);
            } else {
                endKey = null;
            }
        }
    }

    /**
     * FastExternalizable constructor.
     * Order of fields:
     *   flags (byte) (includes both direction and exactMatch state)
     *   prefixKey (optional)
     *   startKey (optional)
     *   endKey (optional)
     */
    public IndexRange(DataInput in,
                      @SuppressWarnings("unused") short serialVersion)
        throws IOException {

        final int flags = in.readUnsignedByte();

        exactMatch = ((flags & FLAG_EXACT) != 0);

        if ((flags & FLAG_FORWARD) != 0) {
            direction = Direction.FORWARD;
        } else if ((flags & FLAG_REVERSE) != 0) {
            direction = Direction.REVERSE;
        } else if ((flags & FLAG_UNORDERED) != 0) {
            direction = Direction.UNORDERED;
        } else {
            throw new AssertionError("Flags must have a direction");
        }

        /* prefix key */
        int keyLen = in.readShort();
        if (keyLen < 0) {
            prefixKey = null;
        } else {
            prefixKey = new byte[keyLen];
            in.readFully(prefixKey);
        }

        /* start key */
        keyLen = in.readShort();
        if (keyLen < 0) {
            startKey = null;
        } else {
            startKey = new byte[keyLen];
            in.readFully(startKey);
        }

        /* end key */
        keyLen = in.readShort();
        if (keyLen < 0) {
            endKey = null;
        } else {
            endKey = new byte[keyLen];
            in.readFully(endKey);
        }
    }

    /**
     * FastExternalizable writer.
     */
    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
        throws IOException {

        int flags = 0;
        if (exactMatch) {
            flags |= FLAG_EXACT;
        }
        if (direction == Direction.FORWARD) {
            flags |= FLAG_FORWARD;
        } else if (direction == Direction.REVERSE) {
            flags |= FLAG_REVERSE;
        } else if (direction == Direction.UNORDERED) {
            flags |= FLAG_UNORDERED;
        } else {
            throw new AssertionError("Direction must be set");
        }

        out.writeByte(flags);

        if (prefixKey == null) {
            out.writeShort(-1);
        } else {
            out.writeShort(prefixKey.length);
            out.write(prefixKey);
        }

        if (startKey == null) {
            out.writeShort(-1);
        } else {
            out.writeShort(startKey.length);
            out.write(startKey);
        }

        if (endKey == null) {
            out.writeShort(-1);
        } else {
            out.writeShort(endKey.length);
            out.write(endKey);
        }
    }

    public byte[] getPrefixKey() {
        return prefixKey;
    }

    public byte[] getStartKey() {
        return startKey;
    }

    public byte[] getEndKey() {
        return endKey;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isForward() {
        return direction == Direction.FORWARD;
    }

    public boolean isReverse() {
        return direction == Direction.REVERSE;
    }

    public boolean isUnordered() {
        return direction == Direction.UNORDERED;
    }

    public boolean getExactMatch() {
        return exactMatch;
    }

    public boolean inRange(byte[] checkKey) {
        return inRange(checkKey, direction);
    }

    /**
     * Checks if the key is in the scan range.  This is a server side
     * function.
     *
     * startKey is inclusive, endKey is exclusive.
     *
     * Comparing to start key is a simple comparison of bytes.  End key is
     * trickier because if the range is on a composite index and the range is
     * internal to the fields (not a leaf) then the comparison must "truncate"
     * the trailing bytes of the value in order to avoid false negatives on
     * the LT comparison.  To illustrate,  Let's say the index is (int, string)
     * and there is a range operation for entries LT an int value.  The endKey
     * might be [129] (value 2) but the checkKey passed in will have additional
     * bytes for the string value, e.g. [129 114 115 ...].  That array will
     * compare as GT the endKey but it should be EQ.
     */
    private boolean inRange(byte[] checkKey, Direction dir) {

        /*
         * Forward iterations handle exact match simply because they
         * start with the exactly correct key, so any match is a good
         * match.  Reverse iterations start *past* the exact match key
         * so they need to do more checking on the matched keys.
         */
        if (exactMatch && dir != Direction.REVERSE) {
            return true;
        }
        boolean in = true;
        if (prefixKey != null) {
            /*
             * if prefixKey is set it must match exactly.  Even though the
             * comparison should fail before getting an out of bounds
             * exception because of the null termination of the string format,
             * check the case where the checkKey length is shorter than the
             * prefix.  The case where the checkKey is a superset of the
             * prefixKey will fail because the terminating null is included
             * in the prefixKey length.
             */
            if (checkKey.length < prefixKey.length) {
                in = false;
            } else {
                in = (IndexImpl.compareUnsignedBytes
                      (checkKey, 0, prefixKey.length,
                       prefixKey, 0, prefixKey.length) == 0);
            }
        }
        /*
         * Check end constraint, it is exclusive.  UNORDERED scans are done
         * as FORWARD on the server side.  UNORDERED mostly has meaning on
         * the client.
         */
        if (in && endKey != null &&
            (dir == Direction.FORWARD || dir == Direction.UNORDERED)) {
            in = (IndexImpl.compareUnsignedBytes
                  (checkKey, 0, Math.min(endKey.length, checkKey.length),
                   endKey, 0, endKey.length) < 0);
        }
        /*
         * Check start constraint, it is inclusive.
         */
        if (in && startKey != null && dir == Direction.REVERSE) {
            in = IndexImpl.compareUnsignedBytes(checkKey, startKey) >= 0;
        }
        return in;
    }

    /**
     * Test use.  This tests both start and end.  This is why Direction is not
     * declared final even though it is, technically.
     */
    boolean inRange(IndexKeyImpl key) {
        byte[] bytes = key.getIndexImpl().serializeIndexKey(key);
        return inRange(bytes, Direction.FORWARD) &&
            inRange(bytes, Direction.REVERSE);
    }

    private String format(byte[] bytes) {
        return Key.getNoFormatString(bytes);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("prefix key:");
        sb.append(prefixKey != null ? format(prefixKey) : "null");
        sb.append(", start key:");
        sb.append(startKey != null ? format(startKey) : "null");
        sb.append(", end key:");
        sb.append(endKey != null ? format(endKey) : "null");
        sb.append(", direction:" + direction);
        sb.append(", exactMatch:" + exactMatch);
        return sb.toString();
    }

    /*
     * Pre-4.2 clients don't know how to serialize for 4.2+ indexes that
     * support null values. This method is called if the client is pre-4.2
     * and index supports nulls, reserialize the index keys.
     */
    public void reserializeOldKeys(IndexImpl index) {
        if (prefixKey != null) {
            prefixKey = reserialize(prefixKey, index);
        }
        if (startKey != null) {
            startKey = reserialize(startKey, index);
        }
        if (endKey != null) {
            endKey = reserialize(endKey, index);
        }
    }

    private byte[] reserialize(byte[] key, IndexImpl index) {
        IndexKeyImpl ikey = index.deserializeIndexKey(key, true, false);
        return index.serializeIndexKey(ikey);
    }

}
