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

package oracle.kv.impl.query.runtime;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static oracle.kv.impl.util.SerialVersion.QUERY_VERSION_2;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import oracle.kv.Consistency;
import oracle.kv.Depth;
import oracle.kv.Direction;
import oracle.kv.KVSecurityException;
import oracle.kv.ParallelScanIterator;
import oracle.kv.StoreIteratorConfig;
import oracle.kv.StoreIteratorException;
import oracle.kv.impl.api.KVStoreImpl;
import oracle.kv.impl.api.Request;
import oracle.kv.impl.api.StoreIteratorParams;
import oracle.kv.impl.api.ops.Result;
import oracle.kv.impl.api.ops.TableQuery;
import oracle.kv.impl.api.parallelscan.PartitionScanIterator;
import oracle.kv.impl.api.parallelscan.ShardScanIterator;
import oracle.kv.impl.api.query.PreparedStatementImpl.DistributionKind;
import oracle.kv.impl.api.table.BinaryValueImpl;
import oracle.kv.impl.api.table.BooleanValueImpl;
import oracle.kv.impl.api.table.FieldDefImpl;
import oracle.kv.impl.api.table.FieldValueImpl;
import oracle.kv.impl.api.table.PrimaryKeyImpl;
import oracle.kv.impl.api.table.RecordDefImpl;
import oracle.kv.impl.api.table.RecordValueImpl;
import oracle.kv.impl.api.table.TableImpl;
import oracle.kv.impl.api.table.TupleValue;
import oracle.kv.impl.query.QueryException;
import oracle.kv.impl.query.QueryStateException;
import oracle.kv.impl.query.compiler.Expr;
import oracle.kv.impl.query.compiler.FunctionLib.FuncCode;
import oracle.kv.impl.query.compiler.QueryFormatter;
import oracle.kv.impl.query.compiler.SortSpec;
import oracle.kv.impl.topo.PartitionId;
import oracle.kv.impl.topo.RepGroupId;
import oracle.kv.impl.util.SerializationUtil;
import oracle.kv.query.ExecuteOptions;
import oracle.kv.stats.DetailedMetrics;
import oracle.kv.table.TableIteratorOptions;

/**
 * ReceiveIter are placed at the boundaries between parts of the query that
 * execute on different "machines". Currently, there can be only one ReceiveIter
 * in the whole query plan. It executes at a "client machine" and its child
 * subplan executes at a "server machine". The child subplan may actually be
 * replicated on several server machines (RNs), in which case the ReceiveIter
 * acts as a UNION ALL expr, collecting and propagating the results it receives
 * from its children. Furthermore, the ReceiveIter may perform a merge-sort over
 * its inputs (if the inputs return sorted results).
 *
 * If the ReceiveIter is the root iter, it just propagates to its output the
 * FieldValues (most likely RecordValues) it receives from the RNs. Otherwise,
 * if its input iter produces tuples, the ReceiveIter will recreate these tuples
 * at its output by unnesting into tuples the RecordValues arriving from the RNs.
 */
public class ReceiveIter extends PlanIter {

    private static class ReceiveIterState extends PlanIterState {

        final boolean theRunOnClient;

        final PartitionId thePartitionId;

        ParallelScanIterator<FieldValueImpl> theRemoteResultsIter;

        HashSet<BinaryValueImpl> thePrimKeysSet;

        ReceiveIterState(
            PartitionId pid,
            boolean eliminateIndexDups,
            boolean runOnClient) {

            theRunOnClient = runOnClient;
            thePartitionId = pid;

            if (eliminateIndexDups) {
                thePrimKeysSet = new HashSet<BinaryValueImpl>(1000);
            }
        }

        @Override
        public void done() {
            super.done();
            clear();
        }

        @Override
        protected void reset(PlanIter iter) {
            super.reset(iter);
            clear();
        }

        @Override
        protected void close() {
            super.close();
            clear();
        }

        void clear() {
            if (theRemoteResultsIter != null) {
                theRemoteResultsIter.close();
                theRemoteResultsIter = null;
            }
            if (thePrimKeysSet != null) {
                thePrimKeysSet.clear();
            }
        }
    }

    private final PlanIter theInputIter;

    private final FieldDefImpl theInputType;

    /*
     * added in QUERY_VERSION_2
     */
    private final boolean theMayReturnNULL;

    private final int[] theSortFieldPositions;

    private final SortSpec[] theSortSpecs;

    private final int[] thePrimKeyPositions;

    private final int[] theTupleRegs;

    private final DistributionKind theDistributionKind;

    private final RecordValueImpl thePrimaryKey;

    private final String theTableName;

    private final PlanIter[] thePushedExternals;

    private final int theNumRegs;

    private final int theNumIters;

    public ReceiveIter(
        Expr e,
        int resultReg,
        int[] tupleRegs,
        PlanIter input,
        FieldDefImpl inputType,
        boolean mayReturnNULL,
        int[] sortFieldPositions,
        SortSpec[] sortSpecs,
        int[] primKeyPositions,
        DistributionKind distrKind,
        PrimaryKeyImpl primKey,
        PlanIter[] pushedExternals,
        int numRegs,
        int numIters) {

        super(e, resultReg);

        theInputIter = input;
        theInputType = inputType;
        theMayReturnNULL = mayReturnNULL;
        theSortFieldPositions = sortFieldPositions;
        theSortSpecs = sortSpecs;
        thePrimKeyPositions = primKeyPositions;
        theTupleRegs = tupleRegs;

        theDistributionKind = distrKind;

        if (primKey != null) {
            thePrimaryKey = primKey.getDefinition().createRecord();
            thePrimaryKey.copyFrom(primKey);
            theTableName = primKey.getTable().getFullName();
        } else {
            thePrimaryKey = null;
            theTableName = null;
        }

        thePushedExternals = pushedExternals;

        theNumRegs = numRegs;
        theNumIters = numIters;
    }

    /**
     * FastExternalizable constructor.
     */
    public ReceiveIter(DataInput in, short serialVersion) throws IOException {
        super(in, serialVersion);

        theNumRegs = in.readInt();
        theNumIters = in.readInt();

        theInputIter = deserializeIter(in, serialVersion);
        theInputType = (FieldDefImpl) deserializeFieldDef(in, serialVersion);

        if (serialVersion < QUERY_VERSION_2) {
            theMayReturnNULL = true;
        } else {
            theMayReturnNULL = in.readBoolean();
        }

        theSortFieldPositions = deserializeIntArray(in);
        theSortSpecs = deserializeSortSpecs(in, serialVersion);
        thePrimKeyPositions = deserializeIntArray(in);
        theTupleRegs = deserializeIntArray(in);

        short ordinal = in.readShort();
        theDistributionKind = DistributionKind.values()[ordinal];
        theTableName = SerializationUtil.readString(in);
        if (theTableName != null) {
            thePrimaryKey = deserializeKey(in, serialVersion);
        } else {
            thePrimaryKey = null;
        }
        thePushedExternals = deserializeIters(in, serialVersion);
    }

    /**
     * FastExternalizable writer.  Must call superclass method first to
     * write common elements.
     */
    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
            throws IOException {

        super.writeFastExternal(out, serialVersion);

        out.writeInt(theNumRegs);
        out.writeInt(theNumIters);

        serializeIter(theInputIter, out, serialVersion);
        serializeFieldDef(theInputType, out, serialVersion);

        if (serialVersion >= QUERY_VERSION_2) {
            out.writeBoolean(theMayReturnNULL);
        }

        serializeIntArray(theSortFieldPositions, out);
        serializeSortSpecs(theSortSpecs, out, serialVersion);
        serializeIntArray(thePrimKeyPositions, out);
        serializeIntArray(theTupleRegs, out);

        out.writeShort(theDistributionKind.ordinal());
        SerializationUtil.writeString(out, theTableName);
        if (theTableName != null) {
            serializeKey(thePrimaryKey, out, serialVersion);
        }
        serializeIters(thePushedExternals, out, serialVersion);
    }

    @Override
    public PlanIterKind getKind() {
        return PlanIterKind.RECV;
    }

    @Override
    public int[] getTupleRegs() {
        return theInputIter.getTupleRegs();
    }

    /**
     * This method executes a query on the server side and stores in the
     * iterator state a ParalleScanIterator over the results.
     *
     * At some point a refactor of how parallel scan and index scan work may
     * be necessary to take into consideration these facts:
     *  o a query may be an update or read-only (this can probably be known
     *  ahead of time once the query is prepared). In any case the type of
     *  query and Durability specified will affect routing of the query.
     *  o some iterator params are not relevant (direction, keys, ranges, Depth)
     */
    private void ensureIterator(
        RuntimeControlBlock rcb,
        ReceiveIterState state) {

        if (state.theRemoteResultsIter != null) {
            return;
        }

        KVStoreImpl store = rcb.getStore();
        ExecuteOptions options = rcb.getExecuteOptions();

        final int batchSize = (options != null ?
                               options.getResultsBatchSize() :
                               KVStoreImpl.DEFAULT_ITERATOR_BATCH_SIZE);
        assert(batchSize != 0);

        switch (theDistributionKind) {
        case SINGLE_PARTITION:
            state.theRemoteResultsIter =
                runOnOnePartition(store, rcb, batchSize);
            break;
        case ALL_PARTITIONS:
            state.theRemoteResultsIter =
                runOnAllPartitions(store, rcb, batchSize);
            break;
        case ALL_SHARDS:
            state.theRemoteResultsIter =
                runOnAllShards(store, rcb, batchSize);
            break;
        default:
            throw new QueryStateException(
                "Unknown distribution kind: " + theDistributionKind);
        }

        rcb.setTableIterator(state.theRemoteResultsIter);
    }

    /**
     * Execute the child plan of this ReceiveIter on all partitions
     */
    private ParallelScanIterator<FieldValueImpl> runOnAllPartitions(
        final KVStoreImpl store,
        final RuntimeControlBlock rcb,
        final int batchSize) {

        ExecuteOptions options = rcb.getExecuteOptions();

        StoreIteratorConfig config = new StoreIteratorConfig();

        if (options != null) {
            config.setMaxConcurrentRequests(options.getMaxConcurrentRequests());
        }

        /*
         * Compute the direction to be stored in the BaseParallelScanIterator.
         * Because the actual comparisons among the query results are done by
         * the streams, the BaseParallelScanIterator just needs to know whether
         * sorting is needed or not in order to invoke the comparison method or
         * not. So, we just need to pass UNORDERED or FORWARD.
         */
        Direction dir = (theSortFieldPositions != null ?
                         Direction.FORWARD :
                         Direction.UNORDERED);

        StoreIteratorParams params =
            new StoreIteratorParams(
                dir,
                batchSize,
                null, // key bytes
                null, // key range
                Depth.PARENT_AND_DESCENDANTS,
                rcb.getConsistency(),
                rcb.getTimeout(),
                rcb.getTimeUnit(),
                rcb.getPartitionSet());

        return new PartitionScanIterator<FieldValueImpl>(
            store, config, params) {

            @Override
            protected QueryPartitionStream createStream(
                RepGroupId groupId,
                int partitionId) {
                return new QueryPartitionStream(groupId, partitionId);
            }

            @Override
            protected TableQuery generateGetterOp(byte[] resumeKey) {
                throw new QueryStateException("Unexpected call");
            }

            @Override
            protected void convertResult(
                Result result,
                List<FieldValueImpl> elementList) {

                List<FieldValueImpl> queryResults = result.getQueryResults();

                // TODO: try to avoid this useless loop
                for (FieldValueImpl res : queryResults) {
                    elementList.add(res);
                }
            }

            @Override
            protected int compare(FieldValueImpl one, FieldValueImpl two) {
                throw new QueryStateException("Unexpected call");
            }

            class QueryPartitionStream extends PartitionStream {

                /* See comment for TableQuery.numResultsComputed */
                private long theNumResultsComputed;

                QueryPartitionStream(RepGroupId groupId, int partitionId) {
                    super(groupId, partitionId, null);
                }

                @Override
                protected Request makeReadRequest() {

                    TableQuery op = new TableQuery(
                        theInputIter,
                        theInputType,
                        theMayReturnNULL,
                        DistributionKind.ALL_PARTITIONS,
                        rcb.getExternalVars(),
                        theNumIters, theNumRegs,
                        batchSize,
                        rcb.getTraceLevel(),
                        resumeKey,
                        null, // resumeSecondaryKey
                        theNumResultsComputed);

                    return storeImpl.makeReadRequest(
                        op,
                        new PartitionId(partitionId),
                        storeIteratorParams.getConsistency(),
                        storeIteratorParams.getTimeout(),
                        storeIteratorParams.getTimeoutUnit());
                }

                @Override
                protected void setResumeKey(Result result) {
                    super.setResumeKey(result);
                    theNumResultsComputed += result.getNumRecords();
                    
                    rcb.trace(
                        "Received " + result.getNumRecords() +
                        " results from group : " + groupId +
                        " partition " + partitionId, 1);
                    
                }

                @Override
                protected int compareInternal(Stream o) {

                    QueryPartitionStream other = (QueryPartitionStream)o;
                    int cmp;

                    FieldValueImpl v1 =
                        currentResultSet.getQueryResults().
                        get(currentResultPos);

                    FieldValueImpl v2 =
                        other.currentResultSet.getQueryResults().
                        get(other.currentResultPos);

                    if (theInputType.isRecord()) {
                        RecordValueImpl rec1 = (RecordValueImpl)v1;
                        RecordValueImpl rec2 = (RecordValueImpl)v2;
                        cmp = compareRecords(rec1, rec2);
                    } else {
                        cmp = compareAtomics(v1, v2, 0);
                    }

                    if (cmp == 0) {
                        return (partitionId < other.partitionId ? -1 : 1);
                    }

                    return cmp;
                }
            }
        };
    }

    /**
     * Execute the child plan of this ReceiveIter on a single partition
     */
    private ParallelScanIterator<FieldValueImpl> runOnOnePartition(
        final KVStoreImpl store,
        final RuntimeControlBlock rcb,
        final int batchSize) {

        final Consistency consistency = rcb.getConsistency();
        final long timeout = rcb.getTimeout();
        final TimeUnit timeUnit = rcb.getTimeUnit();

        ReceiveIterState state = (ReceiveIterState)rcb.getState(theStatePos);
        final PartitionId pid = state.thePartitionId;

        return new ParallelScanIterator<FieldValueImpl>() {

            private List<FieldValueImpl> theResults = null;

            private Iterator<FieldValueImpl> theResultsIter = null;

            private byte[] theResumeKey = null;

            private boolean theMoreRemoteResults = true;

            /* See comment for TableQuery.numResultsComputed */
            private long theNumResultsComputed;

            @Override
            public boolean hasNext() {

                if (theResultsIter != null && theResultsIter.hasNext()) {
                    return true;
                }

                theResultsIter = null;

                if (!theMoreRemoteResults) {
                    return false;
                }

                TableQuery op = new TableQuery(
                    theInputIter,
                    theInputType,
                    theMayReturnNULL,
                    DistributionKind.SINGLE_PARTITION,
                    rcb.getExternalVars(),
                    theNumIters,
                    theNumRegs,
                    batchSize,
                    rcb.getTraceLevel(),
                    theResumeKey,
                    null, // resumeSecondaryKey
                    theNumResultsComputed);

                Request req = store.makeReadRequest(op, pid,
                                                    consistency, timeout,
                                                    timeUnit);

                Result result = store.executeRequest(req);

                theResults = result.getQueryResults();

                theNumResultsComputed += theResults.size();
                theMoreRemoteResults = result.hasMoreElements();

                if (theResults.isEmpty()) {
                    assert(!theMoreRemoteResults);
                    return false;
                }

                theResumeKey = result.getPrimaryResumeKey();
                theResultsIter = theResults.iterator();
                return true;
            }

            @Override
            public FieldValueImpl next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return theResultsIter.next();
            }

            @Override
            public void close() {
                theResultsIter = null;
                theResults = null;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<DetailedMetrics> getPartitionMetrics() {
                return Collections.emptyList();
            }

            @Override
            public List<DetailedMetrics> getShardMetrics() {
                return Collections.emptyList();
            }
        };
    }

    /**
     * Execute the child plan of this ReceiveIter on all shards
     * TODO: remove duplicates in result
     */
    private ParallelScanIterator<FieldValueImpl> runOnAllShards(
        final KVStoreImpl store,
        final RuntimeControlBlock rcb,
        @SuppressWarnings("unused")
        final int batchSize) {

        ExecuteOptions options = rcb.getExecuteOptions();

        /*
         * Compute the direction to be stored in the BaseParallelScanIterator.
         * Because the actual comparisons among the query results are done by
         * the streams, the BaseParallelScanIterator just needs to know whether
         * sorting is needed or not in order to invoke the comparison method or
         * not. So, we just need to pass UNORDERED or FORWARD.
         */
        Direction dir = (theSortFieldPositions != null ?
                         Direction.FORWARD :
                         Direction.UNORDERED);

        TableIteratorOptions opts =
            (options != null ?
             options.createTableIteratorOptions(dir) :
             new TableIteratorOptions(dir, null, 0, null));

        return new ShardScanIterator<FieldValueImpl>(
                       store, opts, rcb.getShardSet()) {

            @Override
            protected QueryShardStream createStream(RepGroupId groupId) {
                return new QueryShardStream(groupId);
            }

            @Override
            protected TableQuery createOp(
                byte[] resumeSecondaryKey,
                byte[] resumePrimaryKey) {
                throw new QueryStateException("Unexpected call");
            }

            @Override
            protected void convertResult(
                Result result,
                List<FieldValueImpl> elementList) {

                List<FieldValueImpl> queryResults = result.getQueryResults();
                for (FieldValueImpl res : queryResults) {
                    elementList.add(res);
                }
            }

            @Override
            protected int compare(FieldValueImpl one, FieldValueImpl two) {
                throw new QueryStateException("Unexpected call");
            }

            class QueryShardStream extends ShardStream {

                /* See comment for TableQuery.numResultsComputed */
                private long theNumResultsComputed;

                QueryShardStream(RepGroupId groupId) {
                    super(groupId, null, null);
                }

                @Override
                protected Request makeReadRequest() {

                    TableQuery op = new TableQuery(
                        theInputIter,
                        theInputType,
                        theMayReturnNULL,
                        DistributionKind.ALL_SHARDS,
                        rcb.getExternalVars(),
                        theNumIters, theNumRegs,
                        batchSize,
                        rcb.getTraceLevel(),
                        resumePrimaryKey,
                        resumeSecondaryKey,
                        theNumResultsComputed);

                    return storeImpl.makeReadRequest(
                        op,
                        groupId,
                        consistency,
                        requestTimeoutMs,
                        MILLISECONDS);
                }

                @Override
                protected void setResumeKey(Result result) {
                    super.setResumeKey(result);
                    theNumResultsComputed += result.getNumRecords();
                }

                @Override
                protected int compareInternal(Stream o) {

                    QueryShardStream other = (QueryShardStream)o;
                    int cmp;

                    FieldValueImpl v1 =
                        currentResultSet.getQueryResults().
                        get(currentResultPos);

                    FieldValueImpl v2 =
                        other.currentResultSet.getQueryResults().
                        get(other.currentResultPos);

                    if (theInputType.isRecord()) {
                        RecordValueImpl rec1 = (RecordValueImpl)v1;
                        RecordValueImpl rec2 = (RecordValueImpl)v2;
                        cmp = compareRecords(rec1, rec2);
                    } else {
                        cmp = compareAtomics(v1, v2, 0);
                    }

                    if (cmp == 0) {
                        return getGroupId().compareTo(other.getGroupId());
                    }

                    return cmp;
                }
            }
        };
    }

    @Override
    public void open(RuntimeControlBlock rcb) {

        boolean runOnClient = false;
        String onClient = System.getProperty("test.queryonclient");
        boolean alwaysFalse = false;

        if (onClient != null && !onClient.isEmpty()) {
            runOnClient = true;
            theInputIter.open(rcb);
        }

        PartitionId pid = PartitionId.NULL_ID;

        if (theDistributionKind == DistributionKind.SINGLE_PARTITION) {

            TableImpl table = rcb.getTableMetadata().getTable(theTableName);
            if (table == null) {
                throw new QueryException(
                    "Table does not exist: " + theTableName, getLocation());
            }

            PrimaryKeyImpl primaryKey = table.createPrimaryKey(thePrimaryKey);

            if (thePushedExternals != null &&
                thePushedExternals.length > 0) {

                int size = thePushedExternals.length;

                for (int i = 0; i < size; ++i) {

                    PlanIter iter = thePushedExternals[i];

                    if (iter == null) {
                        continue;
                    }

                    iter.open(rcb);
                    iter.next(rcb);
                    FieldValueImpl val = rcb.getRegVal(iter.getResultReg());
                    iter.close(rcb);

                    FieldValueImpl newVal = BaseTableIter.castValueToIndexKey(
                        table, null, i, val, FuncCode.OP_EQ);

                    if (newVal != val) {
                        if (newVal == BooleanValueImpl.falseValue) {
                            alwaysFalse = true;
                            break;
                        }

                        val = newVal;
                    }

                    String colName = table.getPrimaryKeyColumnName(i);
                    primaryKey.put(colName, val);
                }

                pid = primaryKey.getPartitionId(rcb.getStore());

            } else {
                pid = primaryKey.getPartitionId(rcb.getStore());
            }
        }

        ReceiveIterState state =
            new ReceiveIterState(pid,
                                 (thePrimKeyPositions != null),
                                 runOnClient);

        rcb.setState(theStatePos, state);

        if (theTupleRegs != null) {
            TupleValue tuple = new TupleValue((RecordDefImpl)theInputType,
                                              rcb.getRegisters(),
                                              theTupleRegs);
            rcb.setRegVal(theResultReg, tuple);
        }

        if (alwaysFalse) {
            state.done();
        }
    }

    @Override
    public boolean next(RuntimeControlBlock rcb) {
        /*
         * Catch StoreIteratorException and if the cause is a QueryException,
         * throw that instead to provide more information to the caller.
         */
        try {
            ReceiveIterState state =
                (ReceiveIterState)rcb.getState(theStatePos);

            if (state.isDone()) {
                return false;
            }

            if (state.theRunOnClient) {
                return theInputIter.next(rcb);
            }

            ensureIterator(rcb, state);

            FieldValueImpl res;

            do {
                boolean more = state.theRemoteResultsIter.hasNext();

                if (!more) {
                    state.done();
                    return false;
                }

                res = state.theRemoteResultsIter.next();

                /* Eliminate index duplicates */
                if (thePrimKeyPositions != null) {
                    BinaryValueImpl binPrimKey = createBinaryPrimKey(res);
                    boolean added = state.thePrimKeysSet.add(binPrimKey);
                    if (!added) {
                        continue;
                    }
                }

                break;

            } while (true);

            if (theTupleRegs != null) {
                TupleValue tuple = (TupleValue)rcb.getRegVal(theResultReg);
                tuple.toTuple((RecordValueImpl)res);
            } else {
                rcb.setRegVal(theResultReg, res);
            }

            return true;

        } catch (StoreIteratorException sie) {

            /*
             * Map SIE cause to known exceptions and throw them instead.
             * NOTE: maybe the cause should always be thrown?
             */
            if (sie.getCause() instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) sie.getCause();
            }
            if (sie.getCause() instanceof QueryException) {
                throw (QueryException) sie.getCause();
            }
            if (sie.getCause() instanceof QueryStateException) {
                throw (QueryStateException) sie.getCause();
            }
            if (sie.getCause() instanceof KVSecurityException) {
                throw (KVSecurityException) sie.getCause();
            }
            throw sie;
        }
    }

    @Override
    public void reset(RuntimeControlBlock rcb) {
        ReceiveIterState state = (ReceiveIterState)rcb.getState(theStatePos);
        state.reset(this);
        if (state.theRunOnClient) {
            theInputIter.reset(rcb);
        }
    }

    @Override
    public void close(RuntimeControlBlock rcb) {

        ReceiveIterState state = (ReceiveIterState)rcb.getState(theStatePos);
        if (state == null) {
            return;
        }

        state.close();
        if (state.theRunOnClient) {
            theInputIter.close(rcb);
        }
    }

    private BinaryValueImpl createBinaryPrimKey(FieldValueImpl result) {

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final DataOutput out = new DataOutputStream(baos);

        try {
            if (!result.isRecord()) {
                assert(thePrimKeyPositions.length == 1);
                writeValue(out, result, 0);

            } else {
                for (int i = 0; i < thePrimKeyPositions.length; ++i) {

                    FieldValueImpl fval = 
                        ((RecordValueImpl)result).get(thePrimKeyPositions[i]);

                    writeValue(out, fval, i);
                }
            }
        } catch (IOException e) {
            throw new QueryStateException(
                "Failed to create binary prim key due to IOException:\n" +
                e.getMessage());
        }

        byte[] bytes = baos.toByteArray();
        return FieldDefImpl.binaryDef.createBinary(bytes);
    }

    private void writeValue(DataOutput out, FieldValueImpl val, int i)
        throws IOException {

        switch (val.getType()) {
        case INTEGER:
            SerializationUtil.writePackedInt(out, val.getInt());
            break;
        case LONG:
            SerializationUtil.writePackedLong(out, val.getLong());
            break;
        case DOUBLE:
            out.writeDouble(val.getDouble());
            break;
        case FLOAT:
            out.writeFloat(val.getFloat());
            break;
        case STRING:
            SerializationUtil.writeString(out, val.getString());
            break;
        case ENUM:
            out.writeShort(val.asEnum().getIndex());
            break;
        default:
            throw new QueryStateException(
                "Unexpected type for primary key column : " +
                val.getType() + ", at result column " + i);
        }
    }

    @Override
    protected void displayContent(StringBuilder sb, QueryFormatter formatter) {

        if (theSortFieldPositions != null) {
            formatter.indent(sb);
            sb.append("Sort Field Positions : ");
            for (int i = 0; i < theSortFieldPositions.length; ++i) {
                sb.append(theSortFieldPositions[i]);
                if (i < theSortFieldPositions.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append(",\n");
        }

        if (thePrimKeyPositions != null) {
            formatter.indent(sb);
            sb.append("Primary Key Positions : ");
            for (int i = 0; i < thePrimKeyPositions.length; ++i) {
                sb.append(thePrimKeyPositions[i]);
                if (i < thePrimKeyPositions.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append(",\n");
        }

        formatter.indent(sb);
        sb.append("DistributionKind : ").append(theDistributionKind);
        sb.append(",\n");

        if (thePrimaryKey != null && thePrimaryKey.size() > 0) {
            formatter.indent(sb);
            sb.append("Primary Key :").append(thePrimaryKey);
            sb.append(",\n");
        }

        if (thePushedExternals != null) {
            sb.append("\n");
            formatter.indent(sb);
            sb.append("EXTERNAL KEY EXPRS: " + thePushedExternals.length);

            for (PlanIter iter : thePushedExternals) {

                sb.append("\n");
                if (iter != null) {
                    iter.display(sb, formatter);
                } else {
                    formatter.indent(sb);
                    sb.append("null");
                }
            }
            sb.append(",\n\n");
        }

        formatter.indent(sb);
        sb.append("Number of Registers :").append(theNumRegs);
        sb.append(",\n");
        formatter.indent(sb);
        sb.append("Number of Iterators :").append(theNumIters);
        sb.append(",\n");
        theInputIter.display(sb, formatter);
    }

    int compareRecords(RecordValueImpl rec1, RecordValueImpl rec2) {

        for (int i = 0; i < theSortFieldPositions.length; ++i) {
            int pos = theSortFieldPositions[i];
            FieldValueImpl v1 = rec1.get(pos);
            FieldValueImpl v2 = rec2.get(pos);

            int comp = compareAtomics(v1, v2, i);

            if (comp != 0) {
                return comp;
            }
        }

        /* they must be equal */
        return 0;
    }

    int compareAtomics(FieldValueImpl v1, FieldValueImpl v2, int sortPos) {

        int comp;

        if (v1.isNull()) {
            if (v2.isNull()) {
                comp = 0;
            } else {
                comp = (theSortSpecs[sortPos].theNullsFirst ? -1 : 1);
            }
        } else if (v2.isNull()) {
            comp = (theSortSpecs[sortPos].theNullsFirst ? 1 : -1);
        } else {
            comp = v1.compareTo(v2);
        }

        return (theSortSpecs[sortPos].theIsDesc ? -comp : comp);
    }
}
