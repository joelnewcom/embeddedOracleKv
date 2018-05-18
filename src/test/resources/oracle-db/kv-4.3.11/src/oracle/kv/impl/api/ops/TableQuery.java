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

import static oracle.kv.impl.util.SerialVersion.QUERY_VERSION;
import static oracle.kv.impl.util.SerialVersion.QUERY_VERSION_2;
import static oracle.kv.impl.util.SerialVersion.QUERY_VERSION_3;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import oracle.kv.impl.api.query.PreparedStatementImpl.DistributionKind;
import oracle.kv.impl.api.table.FieldDefImpl;
import oracle.kv.impl.api.table.FieldDefSerialization;
import oracle.kv.impl.api.table.FieldValueSerialization;
import oracle.kv.impl.query.runtime.PlanIter;
import oracle.kv.impl.util.SerialVersion;
import oracle.kv.table.FieldValue;

/**
 * TableQuery represents and drives the execution of a query subplan at a
 * server site, over one partition or one shard.
 *
 * Instances of TableQuery are created via the parallel-scan infrastructure,
 * when invoked from the open() method of ReceiveIters in the query plan.
 *
 * This class contains the result schema (resultDef) so it can be passed
 * to the result class (Result.QueryResult) and serialized only once for each
 * batch of results. If this were not done, each RecordValue result would have
 * its associated RecordDef serialized (redundantly) along with it.
 */
public class TableQuery extends InternalOperation {

    private final PlanIter queryPlan;

    private final FieldDefImpl resultDef;

    /*
     * added in QUERY_VERSION_2
     */
    private final boolean mayReturnNULL;

    /*
     * Optional Bind Variables. If none exist or are not set this is null.
     * If it would be easier for callers this could be made an empty Map.
     */
    private final FieldValue[] externalVars;

    private final int numIterators;

    private final int numRegisters;

    private final int batchSize;

    private final byte traceLevel;

    private byte[] primaryResumeKey;

    private byte[] secondaryResumeKey;

    /*
     * The number of results received from the server so far. This is needed
     * when a LIMIT clause is pushed to the server. When a server is asked
     * to produce the next result batch, it needs to know how many results
     * it has produced already (in previous batches), so it does not exceed
     * the specified limit. This is necessary when the query is executed
     * at a single partition, because in that case the whole OFFSET/LIMIT
     * clauses are executed at the server. When the query is distributed to
     * multiple partitions/shards, the OFFSET/LIMIT clauses are executed at
     * the client, but a server never needs to send more than OFFSET + LIMIT
     * results. So, a LIMIT equal to the user-specified OFFSET+LIMIT is pushed
     * to the server as an optimization, and numResultsComputed is used as a
     * further optimization. For example, if the batch size is 100 and the
     * server-side limit is 110, this optimization saves the computation and
     * transimission of at least 90 results (110 results with the optimization
     * vs 200 results without). (The savings may be more than 90 because after
     * the client receives one batch, it may immediately ask for the next
     * batch, if its results queue is not full. But with a queue size of 3
     * batches, the maximum savings is 3 batch sizes).
     */
    private final long numResultsComputed;

    public TableQuery(PlanIter queryPlan,
                      FieldDefImpl resultDef,
                      boolean mayReturnNULL,
                      DistributionKind distKind,
                      FieldValue[] externalVars,
                      int numIterators,
                      int numRegisters,
                      int batchSize,
                      byte traceLevel,
                      byte[] primaryResumeKey,
                      byte[] secondaryResumeKey,
                      long numResultsComputed) {

        /*
         * The distinct OpCodes are primarily for a finer granularity of
         * statistics, allowing the different types of queries to be tallied
         * independently.
         */
        super(distKind == DistributionKind.ALL_PARTITIONS ?
              OpCode.QUERY_MULTI_PARTITION :
              (distKind == DistributionKind.ALL_SHARDS ?
               OpCode.QUERY_MULTI_SHARD :
               OpCode.QUERY_SINGLE_PARTITION));
        this.queryPlan = queryPlan;
        this.resultDef = resultDef;
        this.mayReturnNULL = mayReturnNULL;
        this.externalVars = externalVars;
        this.numIterators = numIterators;
        this.numRegisters = numRegisters;
        this.primaryResumeKey = primaryResumeKey;
        this.secondaryResumeKey = secondaryResumeKey;
        this.batchSize = batchSize;
        this.traceLevel = traceLevel;
        this.numResultsComputed = numResultsComputed;
    }

    PlanIter getQueryPlan() {
        return queryPlan;
    }

    FieldDefImpl getResultDef() {
        return resultDef;
    }

    boolean mayReturnNULL() {
        return mayReturnNULL;
    }

    FieldValue[] getExternalVars() {
        return externalVars;
    }

    int getNumIterators() {
        return numIterators;
    }

    int getNumRegisters() {
        return numRegisters;
    }

    int getBatchSize() {
        return batchSize;
    }

    byte getTraceLevel() {
        return traceLevel;
    }

    byte[] getPrimaryResumeKey() {
        return primaryResumeKey;
    }

    byte[] getSecondaryResumeKey() {
        return secondaryResumeKey;
    }

    long getNumResultsComputed() {
        return numResultsComputed;
    }

    /**
     * FastExternalizable writer.  Must call superclass method first to write
     * common elements.
     */
    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
        throws IOException {

        super.writeFastExternal(out, serialVersion);

        if (serialVersion < QUERY_VERSION_2) {
            /* write query-plan version */
            out.writeShort(1);
        }

        PlanIter.serializeIter(queryPlan, out, serialVersion);

        FieldDefSerialization.writeFieldDef(resultDef, out, serialVersion);

        if (serialVersion >= QUERY_VERSION_2) {
            out.writeBoolean(mayReturnNULL);
        }

        writeExternalVars(externalVars, out, serialVersion);

        out.writeInt(numIterators);
        out.writeInt(numRegisters);
        out.writeInt(batchSize);

        if (serialVersion >= QUERY_VERSION_3) {
            out.writeByte(traceLevel);
        }

        if (primaryResumeKey == null) {
            out.writeShort(-1);
        } else {
            out.writeShort(primaryResumeKey.length);
            out.write(primaryResumeKey);
        }
        if (secondaryResumeKey == null) {
            out.writeShort(-1);
        } else {
            out.writeShort(secondaryResumeKey.length);
            out.write(secondaryResumeKey);
        }

        out.writeLong(numResultsComputed);
    }

    /**
     * FastExternalizable constructor.  Must call superclass constructor first
     * to read common elements.
     */
    protected TableQuery(OpCode opCode, DataInput in, short serialVersion)
        throws IOException {

        super(opCode, in, serialVersion);

        if (serialVersion < QUERY_VERSION) {
            String required =
                SerialVersion.getKVVersion(QUERY_VERSION).
                getNumericVersionString();
            String found =
                SerialVersion.getKVVersion(serialVersion).
                getNumericVersionString();
            throw new UnsupportedOperationException(
                "Query operations require a client version equal to or greater"
                + " than " + required + ". The client version detected is " +
                found);
        }

        try {

            if (serialVersion < QUERY_VERSION_2) {
                /* read and skip the query-plan version */
                in.readShort();
            }

            queryPlan = PlanIter.deserializeIter(in, serialVersion);
            resultDef = FieldDefSerialization.readFieldDef(in, serialVersion);

            if (serialVersion < QUERY_VERSION_2) {
                mayReturnNULL = false;
            } else {
                mayReturnNULL = in.readBoolean();
            }
            externalVars = readExternalVars(in, serialVersion);

            numIterators = in.readInt();
            numRegisters = in.readInt();
            batchSize = in.readInt();

            if (serialVersion < QUERY_VERSION_3) {
                traceLevel = 0;
            } else {
                traceLevel = in.readByte();
            }

            int keyLen = in.readShort();
            if (keyLen < 0) {
                primaryResumeKey = null;
            } else {
                primaryResumeKey = new byte[keyLen];
                in.readFully(primaryResumeKey);
            }
            keyLen = in.readShort();
            if (keyLen < 0) {
                secondaryResumeKey = null;
            } else {
                secondaryResumeKey = new byte[keyLen];
                in.readFully(secondaryResumeKey);
            }

            numResultsComputed = in.readLong();

        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    static void writeExternalVars(
        FieldValue[] vars,
        DataOutput out,
        short serialVersion)
        throws IOException {

        if (vars != null && vars.length > 0) {
            int numVars = vars.length;
            out.writeInt(numVars);

            for (int i = 0; i < numVars; ++i) {
                FieldValueSerialization.writeFieldValue(vars[i],
                                                        true, // writeValDef
                                                        out, serialVersion);
            }
        } else {
            out.writeInt(0);
        }
    }

    static FieldValue[] readExternalVars(DataInput in, short serialVersion)
        throws IOException {

        int numVars = in.readInt();
        if (numVars == 0) {
            return null;
        }

        FieldValue[] vars = new FieldValue[numVars];

        for (int i = 0; i < numVars; i++) {
            FieldValue val =
                FieldValueSerialization.readFieldValue(null, // def
                                                       in, serialVersion);

            vars[i] = val;
        }
        return vars;
    }
}
