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

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import oracle.kv.Consistency;
import oracle.kv.ParallelScanIterator;
import oracle.kv.impl.api.KVStoreImpl;
import oracle.kv.impl.api.table.FieldValueImpl;
import oracle.kv.impl.api.table.TableMetadata;
import oracle.kv.impl.topo.RepGroupId;
import oracle.kv.query.ExecuteOptions;
import oracle.kv.table.FieldValue;

/**
 *
 */
public class RuntimeControlBlock {

    final Logger theLogger;

    final byte theTraceLevel;

    final PlanIter theRootIter;

    /*
     * The state array contains as many elements as there are PlanIter
     * instances in the query plan to be executed.
     */
    final private PlanIterState[] theIteratorStates;

    /*
     * The register array contains as many elements as required by the
     * instances in the query plan to be executed.
     */
    final private FieldValueImpl[] theRegisters;

    /*
     * TableMetadata is required by some operations to resolve table and index
     * names.
     */
    final private TableMetadata theTableMetadata;

    /*
     * ExecuteOptions are options set by the application and used to control
     * some aspects of database access, such as Consistency, timeouts, batch
     * sizes, etc.
     */
    final private ExecuteOptions theExecuteOptions;

    /*
     * An array storing the values of the extenrnal variables set for the
     * operation. These come from the map in the BoundStatement.
     */
    final FieldValue[] theExternalVars;

    /*
     * TableIterFactory is used to construct a PlanIter instances that knows
     * how to iterate tables. The factory instances are different depending
     * on whether the query is being run on the client or the server side.
     */
    final private TableIterFactory theTableIterFactory;

    /*
     * KVStoreImpl is set on the client side and is used by the dispatch
     * code that sends queries to the server side.
     */
    final private KVStoreImpl theStore;

    /*
     * Resume keys are in/out arguments. They are used as input to resume
     * iteration and are used by server side iteration methods. They are set as
     * output by the same iterators to save the last visited key in the table
     * or index that is being iterated. When there are no further results they
     * will be null on output. There are two keys to handle index iteration
     * which may end a chunk in a sequence of duplicate secondary keys, so it
     * needs the current primary key to accurately resume iteration.
     */
    private byte[] thePrimaryResumeKey;

    private byte[] theSecondaryResumeKey;

    /*
     * theNumResultsComputed is also in/out resume-related info. It is needed
     * to implement offset/limit at the server side. See comments for
     * TableQuery.numResultsComputed
     */
    private long theNumResultsComputed;

    /*
     * The RCB holds the TableIterator for the current remote call from the
     * ReceiveIter if there is one. This is here so that the query results
     * objects can return partition and shard metrics for the distributed
     * query operation.
     */
    private ParallelScanIterator<FieldValueImpl> theTableIterator;

    /*
     * To achieve improved client side parallelization when satisfying a
     * query, some clients (for example, the Hive/BigDataSQL mechanism)
     * will distribute requests among separate processes and then scan
     * either a set of partitions or shards; depending on the distribution
     * kind (ALL_PARTITIONS, SINGLE_PARTITION, ALL_SHARDS) computed for
     * the given query.
     */
    private final Set<Integer> thePartitions;
    private final Set<RepGroupId> theShards;

    public RuntimeControlBlock(
        Logger logger,
        PlanIter rootIter,
        int numIters,
        int numRegs,
        TableMetadata tableMetadata,
        ExecuteOptions executeOptions,
        FieldValue[] externalVars,
        byte[] primaryResumeKey,
        byte[] secondaryResumeKey,
        long numResultsComputed,
        TableIterFactory tableIterFactory,
        KVStoreImpl store) {

        this(logger, rootIter, numIters, numRegs, tableMetadata, executeOptions,
             externalVars, primaryResumeKey, secondaryResumeKey,
             numResultsComputed, tableIterFactory, store, null, null);
    }

    public RuntimeControlBlock(
        Logger logger,
        PlanIter rootIter,
        int numIters,
        int numRegs,
        TableMetadata tableMetadata,
        ExecuteOptions executeOptions,
        FieldValue[] externalVars,
        byte[] primaryResumeKey,
        byte[] secondaryResumeKey,
        long numResultsComputed,
        TableIterFactory tableIterFactory,
        KVStoreImpl store,
        final Set<Integer> partitions,
        final Set<RepGroupId> shards) {

        theLogger = logger;
        theRootIter = rootIter;
        theTraceLevel =
            (executeOptions != null ? executeOptions.getTraceLevel() : 0);
        theIteratorStates = new PlanIterState[numIters];
        theRegisters = new FieldValueImpl[numRegs];
        theTableMetadata = tableMetadata;
        theExecuteOptions = executeOptions;
        theExternalVars = externalVars;
        thePrimaryResumeKey = primaryResumeKey;
        theSecondaryResumeKey = secondaryResumeKey;
        theNumResultsComputed = numResultsComputed;
        theTableIterFactory = tableIterFactory;
        theStore = store;
        thePartitions = partitions;
        theShards = shards;
    }

    public Logger getLogger() {
        return theLogger;
    }

    byte getTraceLevel() {
        return theTraceLevel;
    }

    public void trace(String msg, int level) {
        if (theTraceLevel >= level) {
            theLogger.info(msg);
        }
    }

    public TableMetadata getTableMetadata() {
        return theTableMetadata;
    }

    public ExecuteOptions getExecuteOptions() {
        return theExecuteOptions;
    }

    Consistency getConsistency() {
        return (theExecuteOptions != null ?
                theExecuteOptions.getConsistency() :
                null);
    }

    long getTimeout() {
        return (theExecuteOptions != null ?
                theExecuteOptions.getTimeout() :
                0);
    }

    TimeUnit getTimeUnit() {
        return (theExecuteOptions != null ?
                theExecuteOptions.getTimeoutUnit() :
                null);
    }

    public void setState(int pos, PlanIterState state) {
        theIteratorStates[pos] = state;
    }

    public PlanIterState getState(int pos) {
        return theIteratorStates[pos];
    }

    public FieldValueImpl[] getRegisters() {
        return theRegisters;
    }

    public FieldValueImpl getRegVal(int regId) {
        return theRegisters[regId];
    }

    public void setRegVal(int regId, FieldValueImpl value) {
        theRegisters[regId] = value;
    }

    FieldValue[] getExternalVars() {
        return theExternalVars;
    }

    FieldValueImpl getExternalVar(int id) {

        if (theExternalVars == null) {
            return null;
        }
        return (FieldValueImpl)theExternalVars[id];
    }

    protected void setTableIterator(ParallelScanIterator<FieldValueImpl> iter) {
        theTableIterator = iter;
    }

    public ParallelScanIterator<FieldValueImpl> getTableIterator() {
        return theTableIterator;
    }

    public byte[] getPrimaryResumeKey() {
        return thePrimaryResumeKey;
    }

    public void setPrimaryResumeKey(byte[] resumeKey) {
        thePrimaryResumeKey = resumeKey;
    }

    public byte[] getSecondaryResumeKey() {
        return theSecondaryResumeKey;
    }

    public void setSecondaryResumeKey(byte[] resumeKey) {
        theSecondaryResumeKey = resumeKey;
    }

    long getNumResultsComputed() {
        return theNumResultsComputed;
    }

    public KVStoreImpl getStore() {
        return theStore;
    }

    TableIterFactory getTableIterFactory() {
        return theTableIterFactory;
    }

    public Set<Integer> getPartitionSet() {
        return thePartitions;
    }

    public Set<RepGroupId> getShardSet() {
        return theShards;
    }
}
