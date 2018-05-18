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

package oracle.kv.query;

import java.util.concurrent.TimeUnit;

import oracle.kv.Consistency;
import oracle.kv.Direction;
import oracle.kv.Durability;
import oracle.kv.KVStore;
import oracle.kv.KVStoreConfig;
import oracle.kv.impl.api.KVStoreImpl;
import oracle.kv.table.TableIteratorOptions;

/**
 * Class contains several options for the KVStore.execute() methods.
 * It contains the following execution options: consistency, durability and
 * timeout.
 *
 * @see KVStore#execute(String, ExecuteOptions)
 * @see KVStore#executeSync(String, ExecuteOptions)
 * @see KVStore#executeSync(Statement, ExecuteOptions)
 *
 * @since 4.0
 */
public class ExecuteOptions {

    private Consistency consistency;
    private Durability durability;
    private long timeout;
    private TimeUnit timeoutUnit;
    private int maxConcurrentRequests;
    private int resultsBatchSize;
    private byte traceLevel;

    public ExecuteOptions() {}

    /**
     * Sets the execution consistency.
     */
    public ExecuteOptions setConsistency(Consistency consistency) {
        this.consistency = consistency;
        return this;
    }

    /**
     * Gets the last set execution consistency.
     */
    public Consistency getConsistency() {
        return consistency;
    }

    /**
     * Sets the execution durability.
     */
    public ExecuteOptions setDurability(Durability durability) {
        this.durability = durability;
        return this;
    }

    /**
     * Gets the last set execution durability.
     */
    public Durability getDurability() {
        return durability;
    }

    /**
     * The {@code timeout} parameter is an upper bound on the time interval for
     * processing the operation.  A best effort is made not to exceed the
     * specified limit. If zero, the {@link KVStoreConfig#getRequestTimeout
     * default request timeout} is used.
     * <p>
     * If {@code timeout} is not 0, the {@code timeoutUnit} parameter must not
     * be {@code null}.
     *
     * @param timeout the timeout value to use
     * @param timeoutUnit the {@link TimeUnit} used by the
     * <code>timeout</code> parameter or null
     */
    public ExecuteOptions setTimeout(long timeout,
                                     TimeUnit timeoutUnit) {

        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must be >= 0");
        }
        if ((timeout != 0) && (timeoutUnit == null)) {
            throw new IllegalArgumentException("A non-zero timeout requires " +
                "a non-null timeout unit");
        }

        this.timeout = timeout;
        this.timeoutUnit = timeoutUnit;
        return this;
    }

    /**
     * Gets the timeout, which is an upper bound on the time interval for
     * processing the read or write operations.  A best effort is made not to
     * exceed the specified limit. If zero, the
     * {@link KVStoreConfig#getRequestTimeout default request timeout} is used.
     *
     * @return the timeout
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * Gets the unit of the timeout parameter, and may
     * be {@code null} only if {@link #getTimeout} returns zero.
     *
     * @return the timeout unit or null
     */
    public TimeUnit getTimeoutUnit() {
        return timeoutUnit;
    }

    /**
     * Returns the maximum number of concurrent requests.
     */
    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    /**
     * Sets the maximum number of concurrent requests.
     */
    public ExecuteOptions setMaxConcurrentRequests(int maxConcurrentRequests) {
        this.maxConcurrentRequests = maxConcurrentRequests;
        return this;
    }

    /**
     * Returns the number of results per request.
     */
    public int getResultsBatchSize() {
        return resultsBatchSize > 0 ? resultsBatchSize :
            KVStoreImpl.DEFAULT_ITERATOR_BATCH_SIZE;
    }

    /**
     * Sets the number of results per request.
     */
    public ExecuteOptions setResultsBatchSize(int resultsBatchSize) {
        this.resultsBatchSize = resultsBatchSize;
        return this;
    }

    /**
     * Returns the trace level for a query
     * @hidden
     */
    public byte getTraceLevel() {
        return traceLevel;
    }

    /**
     * Sets the trace level for a query
     * @hidden
     */
    public ExecuteOptions setTraceLevel(byte level) {
        this.traceLevel = level;
        return this;
    }

    /**
     * For internal use only.
     * @hidden
     */
    public TableIteratorOptions createTableIteratorOptions(
        Direction direction) {

        return new TableIteratorOptions(direction,
                                        consistency,
                                        timeout,
                                        timeoutUnit,
                                        maxConcurrentRequests,
                                        resultsBatchSize);
    }
}
