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

package oracle.kv.impl.api.query;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import oracle.kv.impl.api.KVStoreImpl;
import oracle.kv.impl.api.table.FieldDefImpl;
import oracle.kv.impl.api.table.FieldDefFactory;
import oracle.kv.impl.api.table.FieldMap;
import oracle.kv.impl.api.table.PrimaryKeyImpl;
import oracle.kv.impl.query.compiler.QueryControlBlock;
import oracle.kv.impl.query.compiler.StaticContext.VarInfo;
import oracle.kv.impl.query.runtime.PlanIter;
import oracle.kv.impl.topo.PartitionId;
import oracle.kv.impl.topo.RepGroupId;

import oracle.kv.StatementResult;
import oracle.kv.query.BoundStatement;
import oracle.kv.query.PreparedStatement;
import oracle.kv.query.ExecuteOptions;
import oracle.kv.table.PrimaryKey;
import oracle.kv.table.RecordDef;
import oracle.kv.table.FieldDef;
import oracle.kv.table.FieldValue;

/**
 * Implementation of a PreparedStatement. This class contains the query plan,
 * along with enough information to construct a runtime context in which the
 * query can be executed (RuntimeControlBlock).
 *
 * An instance of PreparedStatementImpl is created by CompilerAPI.prepare(),
 * after the query has been compiled.
 */
public class PreparedStatementImpl
    implements PreparedStatement,
               InternalStatement {

    /**
     * The type of distribution for the query.
     */
    public enum DistributionKind {
        SINGLE_PARTITION, /** the query goes to a single partition */
        ALL_PARTITIONS,   /** the query goes to all partitions */
        ALL_SHARDS,       /** the query goes to all shards (it uses an index) */
        UNKNOWN           /** the distribution is UNKNOWN */
    }

    /*
     * The query plan
     */
    private final PlanIter queryPlan;

    /*
     * The type of the result
     */
    private final RecordDef resultDef;

    /*
     * The number of registers required to run the plan
     */
    private final int numRegisters;

    /*
     * The number of iterators in the plan
     */
    private final int numIterators;

    /*
     * externalVars maps the name of each external var declared in the query
     * to the numeric id and data type for that var.
     */
    private final Map<String, VarInfo> externalVars;

    /*
     * The DistributionKind
     */
    private final DistributionKind distributionKind;

    /*
     * The PartitionId for SINGLE_PARTITION distribution, null or
     * PartitionID.NULL_ID otherwise.
     */
    private final PartitionId partitionId;

    /*
     * The PrimaryKey for SINGLE_PARTITION distribution. It is only valid for
     * SINGLE_PARTITION distribution. It is null or empty otherwise.
     */
    private final PrimaryKeyImpl shardKey;

    /*
     * Needed for unit testing only
     */
    private QueryControlBlock qcb;

    public PreparedStatementImpl(
        PlanIter queryPlan,
        FieldDefImpl resultDef,
        int numRegisters,
        int numIterators,
        Map<String, VarInfo> externalVars,
        QueryControlBlock qcb) {

        this.queryPlan = queryPlan;
        this.numRegisters = numRegisters;
        this.numIterators = numIterators;
        this.externalVars = externalVars;
        this.qcb = qcb;
        this.distributionKind = qcb.getPushedDistributionKind();
        PrimaryKeyImpl pkey = qcb.getPushedPrimaryKey();
        if (pkey != null && pkey.isEmpty()) {
            this.shardKey = null;
        } else {
            this.shardKey = pkey;
        }
        if (shardKey != null &&
            distributionKind == DistributionKind.SINGLE_PARTITION) {
            partitionId = shardKey.getPartitionId(qcb.getStore());
        } else {
            partitionId = PartitionId.NULL_ID;
        }

        if (!resultDef.isRecord()) {

            String fname = qcb.getResultColumnName();
            if (fname == null) {
                fname = "Column_1";
            }

            FieldMap fieldMap = new FieldMap();
            fieldMap.put(fname,
                         resultDef,
                         true/*nullable*/,
                         null/*defaultValue*/);

            this.resultDef = FieldDefFactory.createRecordDef(fieldMap, null);
        } else {
            this.resultDef = resultDef.asRecord();
        }
    }

    @Override
    public RecordDef getResultDef() {
        return resultDef;
    }

    @Override
    public Map<String, FieldDef> getVariableTypes() {
        return getExternalVarsTypes();
    }

    @Override
    public FieldDef getVariableType(String variableName) {
        return getExternalVarType(variableName);
    }

    @Override
    public BoundStatement createBoundStatement() {
        return new BoundStatementImpl(this);
    }

    /*
     * Needed for unit testing only
     */
    public QueryControlBlock getQCB() {
        return qcb;
    }

    public PlanIter getQueryPlan() {
    	return queryPlan;
    }

    public int getNumRegisters() {
        return numRegisters;
    }

    public int getNumIterators() {
        return numIterators;
    }

    public Map<String, FieldDef> getExternalVarsTypes() {

        Map<String, FieldDef> varsMap = new HashMap<String, FieldDef>();

        if (externalVars == null) {
            return varsMap;
        }

        for (Map.Entry<String, VarInfo> entry : externalVars.entrySet()) {
            String varName = entry.getKey();
            VarInfo vi = entry.getValue();
            varsMap.put(varName, vi.getType().getDef());
        }

        return varsMap;
    }

    boolean hasExternalVars() {
        return (externalVars != null && !externalVars.isEmpty());
    }

    public FieldDef getExternalVarType(String name) {

        if (externalVars == null) {
            return null;
        }

        VarInfo vi = externalVars.get(name);
        if (vi != null) {
            return vi.getType().getDef();
        }

        return null;
    }

    /**
     * Convert the map of external vars (maping names to values) to an array
     * with the values only. The array is indexed by an internalid assigned
     * to each external variable. This method also checks that all the external
     * vars declared in the query have been bound.
     */
    public FieldValue[] getExternalVarsArray(Map<String, FieldValue> vars) {

        if (externalVars == null) {
            assert(vars.isEmpty());
            return null;
        }

        int count = 0;
        for (Map.Entry<String, VarInfo> entry : externalVars.entrySet()) {
            String name = entry.getKey();
            ++count;

            if (vars.get(name) == null) {
                throw new IllegalArgumentException(
                    "Variable " + name + " has not been bound");
            }
        }

        FieldValue[] array = new FieldValue[count];

        count = 0;
        for (Map.Entry<String, FieldValue> entry : vars.entrySet()) {
            String name = entry.getKey();
            FieldValue value = entry.getValue();

            VarInfo vi = externalVars.get(name);
            if (vi == null) {
                throw new IllegalStateException(
                    "Variable " + name + " does not appear in query");
            }

            array[vi.getId()] = value;
            ++count;
        }

        assert(count == array.length);
        return array;
    }

    @Override
    public String toString() {
        return queryPlan.display();
    }

    @Override
    public StatementResult executeSync(
        KVStoreImpl store,
        ExecuteOptions options) {

        return new QueryStatementResultImpl(
            store.getTableAPIImpl(), options, this);
    }

    /*
     * These methods may eventually be part of the public PreparedStatement
     * interface. If done, DistributionKind would move there as well.
     */

    /**
     * Returns the DistributionKind for the prepared query
     */
    public DistributionKind getDistributionKind() {
        return distributionKind;
    }

    /**
     * Returns the shard key for the prepared query. It is only valid if
     * DistributionKind is SINGLE_PARTITION. Otherwise it returns null.
     */
    public PrimaryKey getShardKey() {
        return shardKey;
    }

    /**
     * Returns the PartitionId for queries with DistributionKind of
     * SINGLE_PARTITION. NOTE: for queries with variables that are part of the
     * shard key this method will return an incorrect PartitionId. This is
     * because the query compiler puts a placeholder value in the PrimaryKey
     * that is a valid value for the type. TBD: detect this and throw an
     * exception if this method is called on such a query.
     */
    public PartitionId getPartitionId() {
        return partitionId;
    }

    /**
     * Not part of the public PreparedStatement interface available to
     * external clients. This method is employed when the Oracle NoSQL DB
     * Hive/BigDataSQL integration mechanism is used to process a query,
     * and disjoint partition sets are specified for each split.
     */
    public StatementResult executeSyncPartitions(
        KVStoreImpl store,
        ExecuteOptions options,
        final Set<Integer> partitions) {

        return new QueryStatementResultImpl(
            store.getTableAPIImpl(), options, this, partitions, null);
    }

    /**
     * Not part of the public PreparedStatement interface available to
     * external clients. This method is employed when the Oracle NoSQL DB
     * Hive/BigDataSQL integration mechanism is used to process a query,
     * and disjoint shard sets are specified for each split.
     */
    public StatementResult executeSyncShards(
        final KVStoreImpl store,
        final ExecuteOptions options,
        final Set<RepGroupId> shards) {

        return new QueryStatementResultImpl(
            store.getTableAPIImpl(), options, this, null, shards);
    }
}
