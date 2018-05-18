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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import oracle.kv.Direction;
import oracle.kv.Key;
import oracle.kv.ValueVersion;
import oracle.kv.impl.api.ops.IndexIterate;
import oracle.kv.impl.api.ops.IndexKeysIterate;
import oracle.kv.impl.api.ops.InternalOperation;
import oracle.kv.impl.api.ops.Result;
import oracle.kv.impl.api.ops.ResultIndexKeys;
import oracle.kv.impl.api.ops.ResultIndexRows;
import oracle.kv.impl.api.parallelscan.ShardScanIterator;
import oracle.kv.impl.topo.RepGroupId;
import oracle.kv.table.KeyPair;
import oracle.kv.table.MultiRowOptions;
import oracle.kv.table.Row;
import oracle.kv.table.TableIterator;
import oracle.kv.table.TableIteratorOptions;

/**
 * Implementation of a scatter-gather iterator for secondary indexes. The
 * iterator will access the store by shards.
 * {@code ShardIndexStream} will use to read a single shard.
 * <p>
 * Discussion of inclusive/exclusive iterations
 * <p>
 * Each request sent to the server side needs a start or resume key and an
 * optional end key. By default these are inclusive.  A {@code FieldRange}
 * object may be included to exercise fine control over start/end values for
 * range queries.  {@code FieldRange} indicates whether the values are inclusive
 * or exclusive.  {@code FieldValue} objects are typed so the
 * inclusive/exclusive state is handled here (on the client side) where they
 * can be controlled per-type rather than on the server where they are simple
 * {@code byte[]}. This means that the start/end/resume keys are always
 * inclusive on the server side.
 */
public class IndexScan {

    static final Comparator<byte[]> KEY_BYTES_COMPARATOR =
        new Key.BytesComparator();

    /* Prevent construction */
    private IndexScan() {}

    /**
     * Creates a table iterator returning ordered rows.
     *
     * @param store
     * @param getOptions
     * @param iterateOptions
     *
     * @return a table iterator
     */
    static TableIterator<Row> createTableIterator(
        final TableAPIImpl tableAPI,
        final IndexKeyImpl indexKey,
        final MultiRowOptions mro,
        final TableIteratorOptions tio) {

       return createTableIterator(tableAPI, indexKey, mro, tio, null);
    }

    static TableIterator<Row> createTableIterator(
        final TableAPIImpl tableAPI,
        final IndexKeyImpl indexKey,
        final MultiRowOptions mro,
        final TableIteratorOptions tio,
        final Set<RepGroupId> shardSet) {

        final TargetTables targetTables =
            TableAPIImpl.makeTargetTables(indexKey.getTable(), mro);

        final IndexImpl index = (IndexImpl) indexKey.getIndex();
        final TableImpl table = (TableImpl) index.getTable();
        final IndexRange range = new IndexRange(indexKey, mro, tio);

        final boolean needDupElim = needDupElimination(indexKey);

        return new ShardScanIterator<Row>(tableAPI.getStore(), tio, shardSet) {

            @Override
            protected ShardStream createStream(RepGroupId groupId) {
                return new IndexRowScanStream(groupId);
            }

            @Override
            protected InternalOperation createOp(
                byte[] resumeSecondaryKey,
                byte[] resumePrimaryKey) {

                return new IndexIterate(index.getName(),
                                        targetTables,
                                        range,
                                        resumeSecondaryKey,
                                        resumePrimaryKey,
                                        batchSize);
            }

            @Override
            protected void convertResult(Result result, List<Row> rows) {

                final List<ResultIndexRows> indexRowList =
                    result.getIndexRowList();

                for (ResultIndexRows indexRow : indexRowList) {
                    Row converted = convert(indexRow);
                    rows.add(converted);
                }
            }

            /**
             * Converts a single key value into a row.
             */
            private Row convert(ResultIndexRows rowResult) {
                /*
                 * If ancestor table returns may be involved, start at the
                 * top level table of this hierarchy.
                 */
                final TableImpl startingTable =
                    targetTables.hasAncestorTables() ?
                    table.getTopLevelTable() : table;

                final RowImpl fullKey = startingTable.createRowFromKeyBytes(
                    rowResult.getKeyBytes());

                if (fullKey == null) {
                    throw new IllegalStateException
                        ("Unable to deserialize a row from an index result");
                }

                final ValueVersion vv =
                    new ValueVersion(rowResult.getValue(),
                                     rowResult.getVersion());

                RowImpl row =
                    tableAPI.getRowFromValueVersion(
                        vv,
                        fullKey,
                        rowResult.getExpirationTime(),
                        false);
                return row;
            }

            @Override
            protected byte[] extractResumeSecondaryKey(Result result) {

                /*
                 * The resume key is the last index key in the ResultIndexRows
                 * list of index keys.  Because the index key was only added in
                 * release 3.2 the index keys can be null if talking to an older
                 * server.  In that case, back out to extracting the key from
                 * the last Row in the rowList.  NOTE: this will FAIL if the
                 * index includes a multi-key component such as map or array.
                 * That is why new code was introduced in 3.2.
                 */
                byte[] bytes = result.getSecondaryResumeKey();

                /* this will only be null if talking to a pre-3.2 server */
                if (bytes != null || !result.hasMoreElements()) {
                    return bytes;
                }

                /* compatibility code for pre-3.2 servers */
                List<Row> rowList = new ArrayList<Row>();
                convertResult(result, rowList);
                Row lastRow = rowList.get(rowList.size() - 1);
                return index.serializeIndexKey(index.createIndexKey(lastRow));
            }

            @Override
            protected int compare(Row one, Row two) {
                throw new IllegalStateException("Unexpected call");
            }

            /**
             * IndexRowScanStream subclasses ShardIndexStream in order to
             * implement correct ordering of the streams used by an
             * IndexRowScanIterator. Specifically, the problem is that
             * IndexRowScanIterator returns Row objs, and as a result
             * IndexRowScanIterator.compare(), which compares Rows, does not
             * do correct ordering. Instead we must compare index keys. If
             * two index keys (from different shards) are equal, then the
             * associated primary keys are also compared, to make sure that 2
             * streams will never have the same order magnitude (the only way
             * that 2 streams may both return the same index-key, primary-key
             * pair is when both streams retrieve the same row from multiple
             * shards in the event of partition migration.
             */
            class IndexRowScanStream extends ShardStream {

                HashSet<BinaryValueImpl> thePrimKeysSet;

                IndexRowScanStream(RepGroupId groupId) {
                    super(groupId, null, null);
                    if (needDupElim) {
                        thePrimKeysSet = new HashSet<BinaryValueImpl>(1000);
                    }
                }

                @Override
                protected void setResumeKey(Result result) {

                    super.setResumeKey(result);

                    if (!needDupElim) {
                        return;
                    }

                    ListIterator<ResultIndexRows> listIter =
                        result.getIndexRowList().listIterator();

                    while (listIter.hasNext()) {
                        ResultIndexRows indexRow = listIter.next();

                        BinaryValueImpl binPrimKey =
                            FieldDefImpl.binaryDef.
                            createBinary(indexRow.getKeyBytes());

                        boolean added = thePrimKeysSet.add(binPrimKey);

                        if (!added) {
                            listIter.remove();
                        }
                    }
                }

                @Override
                protected int compareInternal(Stream o) {

                    IndexRowScanStream other = (IndexRowScanStream)o;

                    ResultIndexRows res1 =
                        currentResultSet.getIndexRowList().
                        get(currentResultPos);

                    ResultIndexRows res2 =
                        other.currentResultSet.getIndexRowList().
                        get(other.currentResultPos);

                    byte[] key1 = res1.getIndexKeyBytes();
                    byte[] key2 = res2.getIndexKeyBytes();

                    int cmp = IndexImpl.compareUnsignedBytes(key1, key2);

                    if (cmp == 0) {
                        cmp = KEY_BYTES_COMPARATOR.compare(res1.getKeyBytes(),
                                                           res2.getKeyBytes());
                    }

                    return itrDirection == Direction.FORWARD ? cmp : (cmp * -1);
                }
            }
        };
    }

    /**
     * Check whether elimination of duplicate table rows is needed. This is
     * true only if the index is multikey. For example, let "array" be a table
     * column that is an array of ints, and we are searching for rows whose
     * "array" contains a value > 10. Since the index contains an entry for
     * each value of "array", and a given row may contain many values  > 10
     * in its "array"Even then, no elimination is needed
     * in the following case:
     *
     * Let R be the set of index entries that satisfy the search conditions.
     * If all entries in R have the same index key (not including the prim
     * key columns), then there cannot be 2 entries in R that contain the same
     * prim key (i.e. point to the same table row). This is because at the JE
     * level, the index key includes both the declared index fileds and the prim
     * key columns, and these "physical" keys must be uniqye.
     *
     * The above case can arise in 2 situations:
     * - All the multi-key fields have equality conditions on them.
     * - The index is a MapBoth index and there is an equality condition on the
     *   map-key field.
     */
    private static boolean needDupElimination(IndexKeyImpl key) {

        IndexImpl index = (IndexImpl)key.getIndex();

        if (!index.isMultiKey() || key.isComplete()) {
            return false;
        }

        if (key.size() == 0) {
            return true;
        }

        List<IndexImpl.IndexField> ipaths = index.getIndexFields();

        /*
         * If the index is a MapBoth one, and the map-key field is set in the
         * index key, no dup elim is needed.
         */
        if (index.isMapBothIndex()) {

            for (int i = 0; i < key.size(); ++i) {
                if (ipaths.get(i).isMapKey()) {
                    return false;
                }
            }
        }

        /*
         * If any of the index fields that are not set in the index key are
         * multi-key fields, dup elim is needed.
         */
        for (int i = key.size(); i < index.numFields(); ++i) {
            if (ipaths.get(i).isMultiKey()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Creates a table iterator returning ordered key pairs.
     *
     * @return a table iterator
     */
    static TableIterator<KeyPair>
        createTableKeysIterator(final TableAPIImpl apiImpl,
                                final IndexKeyImpl indexKey,
                                final MultiRowOptions mro,
                                final TableIteratorOptions iterateOptions) {

        final TargetTables targetTables =
            TableAPIImpl.makeTargetTables(indexKey.getTable(), mro);
        final IndexImpl index = (IndexImpl) indexKey.getIndex();
        final TableImpl table = (TableImpl) index.getTable();
        final IndexRange range = new IndexRange(indexKey, mro, iterateOptions);

        return new ShardScanIterator<KeyPair>(apiImpl.getStore(),
                                              iterateOptions,
                                              null) {
            @Override
            protected InternalOperation createOp(byte[] resumeSecondaryKey,
                                                 byte[] resumePrimaryKey) {
                return new IndexKeysIterate(index.getName(),
                                            targetTables,
                                            range,
                                            resumeSecondaryKey,
                                            resumePrimaryKey,
                                            batchSize);
            }

            /**
             * Convert the results to KeyPair instances.  Note that in the
             * case where ancestor and/or child table returns are requested
             * the IndexKey returned is based on the the index and the table
             * containing the index, but the PrimaryKey returned may be from
             * a different, ancestor or child table.
             */
            @Override
            protected void convertResult(Result result,
                                         List<KeyPair> elementList) {

                final List<ResultIndexKeys> results =
                    result.getIndexKeyList();

                for (ResultIndexKeys res : results) {

                    final IndexKeyImpl indexKeyImpl =
                        convertIndexKey(res.getIndexKeyBytes());

                    final PrimaryKeyImpl pkey = convertPrimaryKey(res);

                    if (indexKeyImpl != null && pkey != null) {
                        elementList.add(new KeyPair(pkey, indexKeyImpl));
                    } else {
                        elementList.add(null);
                    }
                }
            }

            @Override
            protected int compare(KeyPair one, KeyPair two) {
                return one.compareTo(two);
            }

            private IndexKeyImpl convertIndexKey(byte[] bytes) {
                /* don't allow partial keys */
                return index.deserializeIndexKey(bytes, false);
            }

            private PrimaryKeyImpl convertPrimaryKey(ResultIndexKeys res) {
                /*
                 * If ancestor table returns may be involved, start at the
                 * top level table of this hierarchy.
                 */
                final TableImpl startingTable =
                    targetTables.hasAncestorTables() ?
                    table.getTopLevelTable() : table;
                final PrimaryKeyImpl pkey = startingTable.
                    createPrimaryKeyFromKeyBytes(res.getPrimaryKeyBytes());
                pkey.setExpirationTime(res.getExpirationTime());
                return pkey;
            }
        };
    }
}
