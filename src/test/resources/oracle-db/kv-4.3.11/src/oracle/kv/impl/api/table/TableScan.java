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

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import oracle.kv.Depth;
import oracle.kv.KeyValueVersion;
import oracle.kv.StoreIteratorConfig;
import oracle.kv.ValueVersion;
import oracle.kv.Version;
import oracle.kv.impl.api.KVStoreImpl;
import oracle.kv.impl.api.KeySerializer;
import oracle.kv.impl.api.Request;
import oracle.kv.impl.api.StoreIteratorParams;
import oracle.kv.impl.api.ops.Result;
import oracle.kv.impl.api.ops.ResultKey;
import oracle.kv.impl.api.ops.ResultKeyValueVersion;
import oracle.kv.impl.api.ops.TableIterate;
import oracle.kv.impl.api.ops.TableKeysIterate;
import oracle.kv.impl.api.parallelscan.PartitionScanIterator;
import oracle.kv.impl.topo.PartitionId;
import oracle.kv.stats.DetailedMetrics;
import oracle.kv.table.MultiRowOptions;
import oracle.kv.table.PrimaryKey;
import oracle.kv.table.Row;
import oracle.kv.table.TableIterator;
import oracle.kv.table.TableIteratorOptions;

/**
 * Implementation of the table iterators. These iterators are partition- vs
 * shard-based. They extend the parallel scan code.
 */
public class TableScan {

    /* Prevent construction */
    private TableScan() {}

    /**
     * Creates a table iterator returning rows.
     *
     * @param store
     * @param key
     * @param getOptions
     * @param iterateOptions
     *
     * @return a table iterator
     */
    static TableIterator<Row>
        createTableIterator(final TableAPIImpl apiImpl,
                            final TableKey key,
                            final MultiRowOptions getOptions,
                            final TableIteratorOptions iterateOptions,
                            final Set<Integer> partitions) {

        final TargetTables targetTables =
            TableAPIImpl.makeTargetTables(key.getTable(), getOptions);

        final StoreIteratorConfig config = new StoreIteratorConfig();
        if (iterateOptions != null) {
            config.setMaxConcurrentRequests(
                            iterateOptions.getMaxConcurrentRequests());
        }

        final StoreIteratorParams params =
            new StoreIteratorParams(TableAPIImpl.getDirection(iterateOptions,
                                                              key),
                                    TableAPIImpl.getBatchSize(iterateOptions),
                                    key.getKeyBytes(),
                                    TableAPIImpl.makeKeyRange(key, getOptions),
                                    Depth.PARENT_AND_DESCENDANTS,
                                    TableAPIImpl.getConsistency(iterateOptions),
                                    TableAPIImpl.getTimeout(iterateOptions),
                                    TableAPIImpl.getTimeoutUnit(iterateOptions),
                                    partitions);

        /*
         * If the major key is complete do single-partition iteration.
         */
        if (key.getMajorKeyComplete()) {
            return createPartitionRowIterator(apiImpl,
                                              params,
                                              key,
                                              targetTables);
        }

        return new PartitionScanIterator<Row>(apiImpl.getStore(),
                                              config, params) {
            @Override
            protected TableIterate generateGetterOp(byte[] resumeKey) {
                return new TableIterate(params,
                                        targetTables,
                                        key.getMajorKeyComplete(),
                                        resumeKey);
            }

            @Override
            protected void convertResult(Result result, List<Row> elementList) {

                final List<ResultKeyValueVersion> byteKeyResults =
                    result.getKeyValueVersionList();

                final Row[] rows = convertTableRowResults(apiImpl,
                                                          byteKeyResults,
                                                          key.getTable(),
                                                          targetTables);
                if (rows == null) {
                    return;
                }
                Collections.addAll(elementList, rows);
            }

            @Override
            protected int compare(Row one, Row two) {
                return one.compareTo(two);
            }
        };
    }

    /**
     * Creates a table iterator returning primary keys.
     *
     * @param store
     * @param key
     * @param getOptions
     * @param iterateOptions
     *
     * @return a table iterator
     */
    static TableIterator<PrimaryKey>
        createTableKeysIterator(final TableAPIImpl apiImpl,
                                final TableKey key,
                                final MultiRowOptions getOptions,
                                final TableIteratorOptions iterateOptions) {

        final TargetTables targetTables =
            TableAPIImpl.makeTargetTables(key.getTable(), getOptions);

        final StoreIteratorConfig config = new StoreIteratorConfig();
        if (iterateOptions != null) {
            config.setMaxConcurrentRequests(
                            iterateOptions.getMaxConcurrentRequests());
        }

        final StoreIteratorParams params = new StoreIteratorParams(
            TableAPIImpl.getDirection(iterateOptions,
                                      key),
            TableAPIImpl.getBatchSize(iterateOptions),
            key.getKeyBytes(),
            TableAPIImpl.makeKeyRange(key, getOptions),
            Depth.PARENT_AND_DESCENDANTS,
            TableAPIImpl.getConsistency(iterateOptions),
            TableAPIImpl.getTimeout(iterateOptions),
            TableAPIImpl.getTimeoutUnit(iterateOptions));

        /*
         * If the major key is complete do single-partition iteration.
         */
        if (key.getMajorKeyComplete()) {
            return createPartitionKeyIterator(apiImpl,
                                              params,
                                              key,
                                              targetTables);
        }

        return new PartitionScanIterator<PrimaryKey>(apiImpl.getStore(),
                                                     config, params) {
            @Override
            protected TableKeysIterate generateGetterOp(byte[] resumeKey) {
                return new TableKeysIterate(params,
                                            targetTables,
                                            key.getMajorKeyComplete(),
                                            resumeKey);
            }

            @Override
            protected void convertResult(Result result,
                                         List<PrimaryKey> elementList) {
                final List<ResultKey> byteKeyResults = result.getKeyList();
                final PrimaryKey[] keys =
                    convertTableKeyResults(byteKeyResults,
                                           key.getTable(),
                                           targetTables);

                if (keys == null) {
                    return;
                }
                Collections.addAll(elementList, keys);
            }

            @Override
            protected int compare(PrimaryKey one, PrimaryKey two) {
                return one.compareTo(two);
            }
        };
    }

    /**
    * Creates a table iterator returning table record key/values.
    *
    * @param store
    * @param key
    * @param getOptions
    * @param iterateOptions
    *
    * @return a table iterator
    */
   static TableIterator<KeyValueVersion>
       createTableKVIterator(final TableAPIImpl apiImpl,
                             final TableKey key,
                             final MultiRowOptions getOptions,
                             final TableIteratorOptions iterateOptions,
                             final Set<Integer> partitions) {

       final TargetTables targetTables =
           TableAPIImpl.makeTargetTables(key.getTable(), getOptions);

       final StoreIteratorConfig config = new StoreIteratorConfig();
       if (iterateOptions != null) {
           config.setMaxConcurrentRequests(
                           iterateOptions.getMaxConcurrentRequests());
       }

       final StoreIteratorParams params =
           new StoreIteratorParams(TableAPIImpl.getDirection(iterateOptions,
                                                             key),
                                   TableAPIImpl.getBatchSize(iterateOptions),
                                   key.getKeyBytes(),
                                   TableAPIImpl.makeKeyRange(key, getOptions),
                                   Depth.PARENT_AND_DESCENDANTS,
                                   TableAPIImpl.getConsistency(iterateOptions),
                                   TableAPIImpl.getTimeout(iterateOptions),
                                   TableAPIImpl.getTimeoutUnit(iterateOptions),
                                   partitions);

       /*
        * If the major key is complete do single-partition iteration.
        */
       if (key.getMajorKeyComplete()) {
           throw new IllegalArgumentException("The major path cannot be " +
               "complete for the key.");
       }

       return new PartitionScanIterator<KeyValueVersion>(apiImpl.getStore(),
                                                         config, params) {
           @Override
           protected TableIterate generateGetterOp(byte[] resumeKey) {
               return new TableIterate(params,
                                       targetTables,
                                       key.getMajorKeyComplete(),
                                       resumeKey);
           }

           @Override
           protected void convertResult(Result result,
                                        List<KeyValueVersion> elementList) {

               final List<ResultKeyValueVersion> byteKeyResults =
                       result.getKeyValueVersionList();

                   int cnt = byteKeyResults.size();
                   if (cnt == 0) {
                       assert (!result.hasMoreElements());
                       return;
                   }
                   for (int i = 0; i < cnt; i += 1) {
                       final ResultKeyValueVersion entry =
                           byteKeyResults.get(i);
                       KeySerializer keySerializer =
                           storeImpl.getKeySerializer();
                       elementList.add(KVStoreImpl.createKeyValueVersion(
                           keySerializer.fromByteArray(entry.getKeyBytes()),
                           entry.getValue(),
                           entry.getVersion(),
                           entry.getExpirationTime()));
                   }
           }

           @Override
           protected int compare(KeyValueVersion one, KeyValueVersion two) {
               return one.getKey().compareTo(two.getKey());
           }
       };
   }

    /**
     * Common routine to convert a list of ResultKeyValueVersion objects into
     * an array of Row, suitable for iteration.
     */
    private static Row[]
        convertTableRowResults(final TableAPIImpl apiImpl,
                               final List<ResultKeyValueVersion> byteKeyResults,
                               TableImpl table,
                               TargetTables targetTables) {

        final int cnt = byteKeyResults.size();
        if (cnt == 0) {
            return null;
        }

        /*
         * Convert byte[] keys and values to Row objects.
         */
        final Row[] rowResults = new Row[cnt];

        int actualCount = 0;
        for (ResultKeyValueVersion entry : byteKeyResults) {

            /*
             * If there are ancestor tables, start looking at the top
             * of the hierarchy to catch them.
             */
            if (targetTables.hasAncestorTables()) {
                table = table.getTopLevelTable();
            }

            final RowImpl fullKey =
                table.createRowFromKeyBytes(entry.getKeyBytes());

            if (fullKey != null) {
                final Version version = entry.getVersion();
                assert version != null;
                final ValueVersion vv =
                    new ValueVersion(entry.getValue(), version);
                final RowImpl row =
                    apiImpl.getRowFromValueVersion(vv,
                                                   fullKey,
                                                   entry.getExpirationTime(),
                                                   false);
                rowResults[actualCount++] = row;
            } else {
                rowResults[actualCount++] = null;
            }
        }

        return rowResults;
    }

    /**
     * Common routine to convert a list of ResultKey representing table keys
     * into an array of PrimaryKey, suitable for iteration.
     */
    private static PrimaryKey[]
        convertTableKeyResults(final List<ResultKey> byteKeyResults,
                               TableImpl table,
                               TargetTables targetTables) {

        final int cnt = byteKeyResults.size();
        if (cnt == 0) {
            return null;
        }

        /*
         * Convert byte[] keys to PrimaryKey objects.
         */
        final PrimaryKey[] keyResults = new PrimaryKey[cnt];

        int actualCount = 0;
        for (ResultKey entry : byteKeyResults) {

            /*
             * If there are ancestor tables, start looking at the top
             * of the hierarchy to catch them.
             */
            if (targetTables.hasAncestorTables()) {
                table = table.getTopLevelTable();
            }
            final PrimaryKeyImpl pKey =
                table.createPrimaryKeyFromResultKey(entry);

            keyResults[actualCount++] = pKey;
        }

        return keyResults;
    }

    /**
     * Creates a single-partition table row iterator.
     */
    private static TableIterator<Row>
        createPartitionRowIterator(final TableAPIImpl apiImpl,
                                   final StoreIteratorParams params,
                                   final TableKey key,
                                   final TargetTables targetTables) {

        final KVStoreImpl store = apiImpl.getStore();
        final byte[] parentKeyBytes =
            store.getKeySerializer().toByteArray(key.getKey());
        final PartitionId partitionId =
            store.getDispatcher().getPartitionId(parentKeyBytes);

        /*
         * If there was a list of partitions specified, then we should check to
         * make sure the target partition is in the list. If not, then return
         * an iterator which has no elements.
         */
        final Set<Integer> partitions = params.getPartitions();
        if ((partitions != null) &&
            !partitions.contains(partitionId.getPartitionId())) {
            return new EmptyTableIterator<Row>();
        }

        final TableImpl table = key.getTable();

        return new MultiGetIteratorWrapper<Row>() {
            private boolean moreElements = true;
            private byte[] resumeKey = null;

            @Override
            Row[] getMoreElements() {

                if (!moreElements) {
                    return null;
                }

                final TableIterate op =
                    new TableIterate(params,
                                     targetTables,
                                     true,
                                     resumeKey);

                final Request req = store.makeReadRequest
                    (op, partitionId, params.getConsistency(),
                     params.getTimeout(),
                     params.getTimeoutUnit());
                final Result result = store.executeRequest(req);
                moreElements = result.hasMoreElements();
                final List<ResultKeyValueVersion> byteKeyResults =
                    result.getKeyValueVersionList();
                if (byteKeyResults.isEmpty()) {
                    assert !moreElements;
                    return null;
                }
                resumeKey =
                    byteKeyResults.get(byteKeyResults.size() - 1).getKeyBytes();
                return  convertTableRowResults(apiImpl,
                                               byteKeyResults,
                                               table,
                                               targetTables);
            }
        };
    }

    /**
     * Creates a single-partition table key iterator.
     */
    private static TableIterator<PrimaryKey>
        createPartitionKeyIterator(final TableAPIImpl apiImpl,
                                   final StoreIteratorParams params,
                                   final TableKey key,
                                   final TargetTables targetTables) {

        final KVStoreImpl store = apiImpl.getStore();
        final byte[] parentKeyBytes =
            store.getKeySerializer().toByteArray(key.getKey());
        final PartitionId partitionId =
            store.getDispatcher().getPartitionId(parentKeyBytes);

        /*
         * If there was a list of partitions specified, then we should check to
         * make sure the target partition is in the list. If not, then return
         * an iterator which has no elements.
         */
        final Set<Integer> partitions = params.getPartitions();
        if ((partitions != null) &&
            !partitions.contains(partitionId.getPartitionId())) {
            return new EmptyTableIterator<PrimaryKey>();
        }

        final TableImpl table = key.getTable();

        return new MultiGetIteratorWrapper<PrimaryKey>() {
            private boolean moreElements = true;
            private byte[] resumeKey = null;

            @Override
            PrimaryKey[] getMoreElements() {

                if (!moreElements) {
                    return null;
                }

                final TableKeysIterate op =
                    new TableKeysIterate(params,
                                         targetTables,
                                         true,
                                         resumeKey);
                final Request req = store.makeReadRequest
                    (op, partitionId, params.getConsistency(),
                     params.getTimeout(),
                     params.getTimeoutUnit());
                final Result result = store.executeRequest(req);
                moreElements = result.hasMoreElements();
                final List<ResultKey> byteKeyResults = result.getKeyList();
                if (byteKeyResults.isEmpty()) {
                    assert !moreElements;
                    return null;
                }
                resumeKey = byteKeyResults.
                    get(byteKeyResults.size() - 1).getKeyBytes();
                return  convertTableKeyResults(byteKeyResults,
                                               table,
                                               targetTables);
            }
        };
    }

    /**
     * Wrapper class for ParallelScanIterator when it is a single-partition
     * iteration.  This method uses ResultsQueueEntry<E> instead of E alone so
     * that code can be shared more easily with the parallel scan
     * implementation.
     *
     * This needs to implement TableIterator<E> but in this case the methods
     * specific to TableIterator<E> (actually ParallelScanIterator<E>) are
     * no-ops.  There are no relevant statistics in this path.
     */
    private static abstract class MultiGetIteratorWrapper<E>
        implements TableIterator<E> {

        private E[] elements = null;
        private int nextElement = 0;

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNext() {
            if (elements != null && nextElement < elements.length) {
                return true;
            }

            elements = getMoreElements();

            if (elements == null) {
                return false;
            }

            assert (elements.length > 0);
            nextElement = 0;
            return true;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return elements[nextElement++];
        }

        /**
         * Returns more elements or null if there are none.  May not return a
         * zero length array.
         */
        abstract E[] getMoreElements();

        /*
         * From ParallelScanIterator.
         */
        @Override
        public void close() {
        }

        @Override
        public List<DetailedMetrics> getPartitionMetrics() {
            return Collections.emptyList();
        }

        @Override
        public List<DetailedMetrics> getShardMetrics() {
            return Collections.emptyList();
        }
    }

    /*
     * A table iterator which has no elements.
     */
    private static class EmptyTableIterator<E>
        extends MultiGetIteratorWrapper<E> {
        @Override
        E[] getMoreElements() {
            return null;
        }
    }
}
