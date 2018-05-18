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

import java.util.concurrent.TimeUnit;

import oracle.kv.Consistency;
import oracle.kv.Direction;
import oracle.kv.KVStoreConfig;

/**
 * TableIteratorOptions extends ReadOptions and is passed to read-only store
 * operations that return iterators.  It is used to specify non-default
 * behavior.  Default behavior is configured when a store is opened using
 * {@link KVStoreConfig}.
 *
 * @since 3.0
 */
public class TableIteratorOptions extends ReadOptions {

    private final Direction direction;

    private final int maxConcurrentRequests;
    private final int batchResultsSize;
    private int maxResultsBatches;

    /**
     * Creates a {@code TableIteratorOptions} with the specified parameters.
     * Equivalent to
     * {@code TableIteratorOptions(direction, consistency, timeout,
     * timeoutUnit, 0, 0)}
     *
     * @param direction a direction
     * @param consistency the read consistency to use or null
     * @param timeout the timeout value to use
     * @param timeoutUnit the {@link TimeUnit} used by the
     * <code>timeout</code> parameter or null
     *
     * @throws IllegalArgumentException if direction is null, the timeout
     * is negative or timeout is > 0 and timeoutUnit is null
     */
    public TableIteratorOptions(Direction direction,
                                Consistency consistency,
                                long timeout,
                                TimeUnit timeoutUnit) {
        this(direction, consistency, timeout, timeoutUnit, 0, 0);
    }

    /**
     * Creates a {@code TableIteratorOptions} with the specified parameters.
     * <p>
     * If {@code consistency} is {@code null}, the
     * {@link KVStoreConfig#getConsistency default consistency}
     * is used. If {@code timeout} is zero the
     * {@link KVStoreConfig#getRequestTimeout default request timeout} is used.
     * <p>
     * {@code maxConcurrentRequests} specifies the maximum degree of parallelism
     * (in effect the maximum number of client-side threads) to be used when
     * running an iteration. Setting {@code maxConcurrentRequests} to 1 causes
     * the iteration to be performed using only the current thread. Setting it
     * to 0 lets the KV Client determine the number of threads based on topology
     * information (up to a maximum of the number of available processors as
     * returned by java.lang.Runtime.availableProcessors()). Values less than 0
     * are reserved for some future use and cause an
     * {@code IllegalArgumentException} to be thrown.
     *
     * @param direction a direction
     * @param consistency the read consistency to use or null
     * @param timeout the timeout value to use
     * @param timeoutUnit the {@link TimeUnit} used by the
     * <code>timeout</code> parameter or null
     * @param maxConcurrentRequests the maximum number of client-side threads
     * @param batchResultsSize the number of results per request
     *
     * @throws IllegalArgumentException if direction is null, the timeout
     * is negative, timeout is > 0 and timeoutUnit is null, or if
     * maxConcurrentRequests or maxResultsSize is less
     * than 0.
     *
     * @since 3.4
     */
    public TableIteratorOptions(Direction direction,
                                Consistency consistency,
                                long timeout,
                                TimeUnit timeoutUnit,
                                int maxConcurrentRequests,
                                int batchResultsSize) {
        super(consistency, timeout, timeoutUnit);
        if (direction == null) {
            throw new IllegalArgumentException("direction must not be null");
        }
        if (maxConcurrentRequests < 0) {
            throw new IllegalArgumentException
                ("maxConcurrentRequests must be >= 0");
        }
        if (batchResultsSize < 0) {
            throw new IllegalArgumentException("batchResultsSize must be >= 0");
        }
        this.direction = direction;
        this.maxConcurrentRequests = maxConcurrentRequests;
        this.batchResultsSize = batchResultsSize;
    }

    /**
     * @deprecated since 3.4, no longer supported.
     *
     * replaced by {@link #TableIteratorOptions(Direction, Consistency, long,
     * TimeUnit, int, int)}
     */
    @Deprecated
    public TableIteratorOptions(Direction direction,
                                Consistency consistency,
                                long timeout,
                                TimeUnit timeoutUnit,
                                int maxConcurrentRequests,
                                int batchResultsSize,
                                int maxResultsBatches) {
        this(direction, consistency, timeout, timeoutUnit,
             maxConcurrentRequests, batchResultsSize);
        this.maxResultsBatches = maxResultsBatches;
    }

    /**
     * Returns the direction.
     *
     * @return the direction
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Returns the maximum number of concurrent requests.
     *
     * @return the maximum number of concurrent requests
     */
    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    /**
     * Returns the number of results per request.
     *
     * @return the number of results
     */
    public int getResultsBatchSize() {
        return batchResultsSize;
    }

    /**
     * @deprecated since 3.4, no longer supported.
     *
     * Returns the value set by deprecated TableIteratorOptions constructor.
     *
     * @return the value set by deprecated TableIteratorOptions constructor.
     */
    @Deprecated
    public int getMaxResultsBatches() {
        return maxResultsBatches;
    }
}
