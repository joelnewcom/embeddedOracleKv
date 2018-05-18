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

package oracle.kv.impl.api.parallelscan;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.kv.Direction;
import oracle.kv.ParallelScanIterator;
import oracle.kv.RequestTimeoutException;
import oracle.kv.StoreIteratorException;
import oracle.kv.impl.api.KVStoreImpl;
import oracle.kv.impl.api.KVStoreImpl.TaskExecutor;
import oracle.kv.impl.api.Request;
import oracle.kv.impl.api.ops.Result;
import oracle.kv.impl.api.parallelscan.ParallelScanHook.HookType;

/**
 * Implementation of a scatter-gather iterator. The iterator will access the
 * store, possibly in parallel, and sort the values before returning them
 * through the next() method.
 * <p>
 * Theory of Operation
 * <p>
 * When the iterator is created, one {@code Stream} instance is created
 * for each single range, such as a shard or partition. {@code Stream} maintains
 * state required for reading from the store, and the returned data. The
 * instances are kept in a {@code TreeSet} and remain until there are no more
 * records available from the range. How the streams are sorted is described
 * below.
 * <p>
 * When the {@code Stream} is first created it is submitted to the thread pool
 * executor. When run, it will attempt to use makeReadRequest() method to read a
 * block of records from the store. The returned data is placed on the
 * {@code blocks} queue. If there are more records in the store, it will use
 * setResumeKey method to save resume key and if there is room on the queue, the
 * stream re-submits itself to eventually read another block. There is locking
 * which will prevent the stream from being submitted more than once. There is
 * more on threads and reading below.
 * <p>
 * Sorting
 * <p>
 * {@code Stream} implements {@code Comparable}. (Note that {@code Stream} has a
 * natural ordering that is inconsistent with {@code equals}.) When a stream is
 * inserted into the {@code TreeSet} it will sort
 * (using {@code Comparable.compareTo}) on the next element in the stream. If
 * the stream does not have a next element (because the stream has been drained
 * and the read from the store has not yet returned)
 * {@code Comparable.compareTo()} will return -1 causing the stream to sort
 * to the beginning of the set. This means that the first stream in the
 * {@code TreeSet} has the overall next element or is empty waiting on data.
 * See the section below on Direction for an exception to behavior.
 * <p>
 * To get the next overall element the basic steps are as follows:
 * <p>
 * 1. Remove the first stream from the {@code TreeSet} <br>
 * 2. Remove the next element from the stream  <br>
 * 3. Re-insert the stream into the {@code TreeSet}  <br>
 * 4. If the element is not null, return  <br>
 * 5. If the element is null, wait and go back to 1  <br>
 * <p>
 * Removing the element at #2 will cause the stream to update the next
 * element with the element next in line (if any). This will cause the
 * stream to sort based on the new element when re-inserted at #3.
 * <p>
 * There is an optimization at step #5 which will skip the wait if
 * removing an element (#2) resulting in the stream having a non-null
 * next element.
 * <p>
 * Reading
 * <p>
 * Initially, each {@code Stream} is submitted to the executor. As mentioned
 * above the {@code Stream} will read a block and if there is more data
 * available and space in the queue, will re-submit itself to read more data.
 * This will result in reading {@code QUEUE_SIZE} blocks for each range. The
 * {@code ShardStream} is not re-submitted once the queue is filled. If reading
 * from the store results in an end-of-data, a flag is set preventing the stream
 * from being submitted.
 * <p>
 * Once elements are removed from the iterator and a block is removed from the
 * queue, an attempt is made to submit a stream for reading. This happens
 * at step #2 above in {@code Stream.removeNext()}. The method
 * {@code removeNext()} will first attempt to remove an element from the
 * current block. If that block is empty it removes the next block on the
 * queue, making that the current block. At this point the stream is
 * submitted to the executor.
 */
public abstract class BaseParallelScanIteratorImpl<K>
    implements ParallelScanIterator<K> {

    /*
     * Use to convert, instead of TimeUnit.toMills(), when you don't want
     * a negative result.
     */
    private static final long NANOS_TO_MILLIS = 1000000L;

    /* Time to wait for data to be returned form the store */
    private static final long WAIT_TIME_MS = 100L;

    /*
     * The default size of the queue of blocks in each stream. Note that the
     * maximum number of blocks maintained by the stream is QUEUE_SIZE + 1.
     * (The queue plus the current block).
     */
    private static final int QUEUE_SIZE = 3;

    protected KVStoreImpl storeImpl;

    protected Logger logger;

    protected long requestTimeoutMs;

    protected Direction itrDirection;

    protected TaskExecutor taskExecutor;

    /*
     * The sorted set of streams. Only streams that have elements or are
     * waiting on reads are in the set. Streams that have exhausted the
     * store are discarded.
     */
    protected TreeSet<Stream> streams;

    /* True if the iterator has been closed */
    protected volatile boolean closed = false;

    /* The exception passed to close(Exception) */
    protected Exception closeException = null;

    /*
     * The next element to be returned from this iterator. This may be null
     * if waiting for reads to complete.
     */
    protected K next = null;

    /*
     * The size of queue of blocks in each stream.
     */
    private int maxResultsBatches = 0;

    /* -- From Iterator -- */

    @Override
    public boolean hasNext() {
        if (isClosed()) {
            return false;
        }

        if (next == null) {
            next = getNext();
        }
        return (next != null);
    }

    @Override
    public K next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        final K lastReturned = next;
        next = null;
        return lastReturned;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /* -- From ParallelScanIterator -- */

    @Override
    public void close() {
        close(null);
    }

    /* Sets the max number of result batches */
    protected void setMaxResultsBatches(int maxResultsBatches) {
        this.maxResultsBatches = maxResultsBatches;
    }

    /* Returns the max number of result batches. */
    protected int getMaxResultsBatches() {
        return (maxResultsBatches > 0) ? maxResultsBatches : QUEUE_SIZE ;
    }

    /**
     * Returns true if the iterator is closed, otherwise false. If there
     * was an exception which caused the iterator to be closed a
     * StoreIteratorException is thrown containing the original exception.
     *
     * @return true if the iterator is closed
     */
    private boolean isClosed() {
        if (closed) {
            /*
             * The iterator is closed. If there was an exception during
             * some internal operation, throw it now.
             */
            if (closeException != null) {
                throw new StoreIteratorException(closeException, null);
            }
            return true;
        }
        return false;
    }

    /**
     * Gets the next value in sorted order. If no more values remain or the
     * iterator is canceled, null is returned. This method will block
     * waiting for data from the store.
     *
     * @return the next value in sorted order or null
     */
    private K getNext() {

        final long limitNs = System.nanoTime() +
                             MILLISECONDS.toNanos(requestTimeoutMs);

        while (!isClosed()) {

            /*
             * The first stream in the set will contain the next
             * element in order.
             */
            final Stream stream = streams.pollFirst();

            /* If there are no more streams we are done. */
            if (stream == null) {
                close();
                return null;
            }

            final K entry = stream.removeNext();

            /*
             * Return the stream back to the set where it will sort on the
             * next element.
             */
            if (!stream.isDone()) {
                streams.add(stream);
            }

            /*
             * removeNext() may have waited or thrown an exception so check
             * for closed.
             */
            if (isClosed()) {
                return null;
            }

            if (entry != null) {
                return entry;
            }

            /* The stream is empty, if we have time, wait */
            long waitMs =
                   Math.min((limitNs - System.nanoTime()) / NANOS_TO_MILLIS,
                            WAIT_TIME_MS);

            if (waitMs <= 0) {
                throw new RequestTimeoutException
                    ((int)requestTimeoutMs,
                     ("Operation timed out on shard: " + stream),
                     null, false);
            }

            stream.waitForNext(waitMs);
        }
        return null;    /* Closed */
    }

    /**
     * Close the iterator, recording the specified remote exception. If
     * the reason is not null, the exception is thrown from the hasNext()
     * or next() methods.
     *
     * @param reason the exception causing the close or null
     */
    protected abstract void close(Exception reason);

    /**
     * Compares the two elements. Returns a negative integer, zero, or a
     * positive integer if object one is less than, equal to, or greater
     * than object two.
     *
     * @return a negative integer, zero, or a positive integer
     */
    protected abstract int compare(K one, K two);

    /**
     * Convert the given Result obj (storing results received from the server)
     * into list of elements (the client version of the same results).
     *
     * If a RuntimeException is thrown, the iterator will be closed and the
     * exception return to the application.
     *
     * NOTE: If a server result (in Result) fails to convert to a client result,
     * this method MUST put a null in the elementList. This is important because
     * Stream.currentBlock and Stream.currentResultSet MUST have the same number
     * of results. A server result may fail to convert to a client result
     * because the server may return records that do not actually belong to the
     * table being scanned.
     *
     * @param result result object
     * @param resultList list to place the converted elements
     */
    protected abstract void convertResult(
        Result result,
        List<K> elementList);


    /**
     * Object that encapsulates the activity around reading records of a single
     * range, such as a shard or partition.
     *
     * Note: this class has a natural ordering that is inconsistent with
     * equals.
     *
     * blocks :
     * The queue of unconverted server results (queue of Result objs) waiting to
     * be consumed). NOTE: blocks is always accessed in synchronized code
     * (except in assertions).
     *
     * currentResultSet :
     * The latest block of server results that have been converted to client
     * results (which are stored in this.currentBlock). currentResultSet is
     * not in this.blocks. NOTE: currentResultSet is accessed only by the
     * consumer/iterator thread.
     *
     * currentBlock :
     * The block of elements (converted server results) currently being consumed.
     * NOTE: currentBlock is accessed only by the consumer/iterator thread.
     *
     * currentResultPos :
     * Serves as an index into currentResultSet, pointing to the server version
     * of the client result pointed by nextElem.
     *
     * nextElem :
     * The next element to be given by this stream to the consuming iterator.
     * NOTE: nextElem is accessed only by the consumer/iterator thread.
     *
     * NOTE: nextElem or currentResultPos are used to sort the streams in the
     * "streams" TreeSet of the containing parallel iterator obj. As a result,
     * nextElem and currentResultPos must (and are indeed) updated only when
     * "this" is not a memeber of the "streams" TreeSet.
     *
     * doneReading :
     * True if there are no more results to be fetched from the server by this
     * stream. The stream may still have locally cached results, so this.done
     * may be false while this.doneReading is true.
     * NOTE: doneReading is always accessed in synchronized code (except in
     * assertions)
     *
     * done :
     * True if this stream has no more results to return.
     * NOTE: done is always set in synchronized code. However, it is read in
     * non-synchronized code (via the isDone() method. This should be ok
     * because once set to true, done will never go back to false, and because
     * the BaseParallelScanIteratorImpl.getNext() is tolerant to isDone()
     * returning false when the stream has actually move to the "done" state.
     *
     * active :
     * True if this stream has been submitted to fetch more results from the
     * server.
     */
    public abstract class Stream implements Comparable<Stream>, Runnable {

        /* The block of values being drained */
        private final BlockingQueue<Result> blocks =
            new LinkedBlockingQueue<Result>(getMaxResultsBatches());

        /* The block of server results being drained */
        protected Result currentResultSet;

        /* The block of values being drained */
        private List<K> currentBlock;

        /* Position of the "current" server result inside currentResultSet */
        protected int currentResultPos = -1;

        /* The last element removed, used for sorting */
        private K nextElem = null;

        /* False if nothing left to read */
        private boolean doneReading = false;

        /* True if there are no more values */
        private boolean done = false;

        /* True if this stream is  */
        private boolean active = false;

        /**
         * Remove the next element from this stream and return it. If no
         * elements are available null is returned.
         *
         * Method run by the consumer thread only.
         *
         * @return the next element from this stream or null
         */
        private K removeNext() {

            assert !done;

            final K ret = nextElem;

            if (currentBlock != null && !currentBlock.isEmpty()) {
                do {
                    nextElem = currentBlock.remove(0);
                    ++currentResultPos;
                    /*
                     * nextElem may be null here because a server result failed
                     * to convert to a client result (see comment on
                     * BaseParallelScanIteratorImpl.convertResult method above).
                     */
                } while (nextElem == null && !currentBlock.isEmpty());
            } else {
                nextElem = null;
            }

            /*
             * If there are no more results in the current block, attempt
             * to get a new block.
             */
            while (nextElem == null) {

                synchronized (this) {

                    /*
                     * Retrieve and remove the Result at the head of the queue,
                     * returning null if the queue is empty.
                     */
                    currentResultSet = blocks.poll();
                    currentBlock = null;
                    currentResultPos = -1;

                    /*
                     * We may have pulled a block off the queue, submit this
                     * stream to get more.
                     */
                    submit();

                    /*
                     * If there are no more cached blocks break out of this loop
                     * and return null. The iterator will then put this stream
                     * back to the tree set and wait for more server results to
                     * arrive in this stream. However, if there are no more
                     * results to be fetched from the server (ie, doneReading is
                     * false), set done to false to signal that this stream is
                     * finished.
                     */
                    if (currentResultSet == null) {
                        done = doneReading;
                        break;
                    }
                }

                int nRecords = currentResultSet.getNumRecords();

                /*
                 * Convert the Result into a list of elements ready to be
                 * returned from the iterator. If a server fails to convert,
                 * null is placed in the element list.
                 */
                if (nRecords > 0) {
                    currentBlock = new ArrayList<K>(nRecords);
                    convertResult(currentResultSet, currentBlock);
                    assert(nRecords == currentBlock.size());

                    do {
                        nextElem = currentBlock.remove(0);
                        ++currentResultPos;
                    } while (nextElem == null && !currentBlock.isEmpty());
                }
            }

            return ret;
        }

        /**
         * Waits up to waitMs for the next element to be available.
         *
         * Method run by the consumer thread only, when it needs to get a
         * result from this stream in order to proceed, and this stream has
         * run out of cached results.
         *
         * @param waitMs the max time in ms to wait
         */
        private void waitForNext(long waitMs) {

            /*
             * If the stream was previously empty, but it now has a value,
             * skip the sleep and immediately try again. This can happen
             * when the stream is initially created (priming the pump) and
             * when the reads are not keeping up with removal through the
             * iterator.
             */
            if (nextElem != null) {
                return;
            }

            try {
                synchronized (this) {
                    /* Wait if there is no data left and still reading */
                    if (blocks.isEmpty() && !doneReading) {
                        wait(waitMs);
                    }
                }
            } catch (InterruptedException ex) {
                if (!closed) {
                    logger.log(Level.WARNING, "Unexpected interrupt ", ex);
                }
            }
        }

        /**
         * Returns true if all of the elements have been removed from this
         * stream.
         *
         * Method run by the consumer thread only
         */
        boolean isDone() {
            return done;
        }

        /**
         * Submit this stream to request data if (a) it isn't already submitted
         * (active), and (b) there is more server data to read, and (c) there
         * is room for it.
         *
         * This method is called from both the consumer/iterator thread and
         * the producer thread:
         *
         *  - The producer thread resubmits the stream after fetching a block
         *    of results
         *  - The iterator thread does the initial submit on each stream, and
         *    then it submits a stream after its currentBlock becomes empty.
         */
        public synchronized void submit() {

            if (active ||
                doneReading ||
                (blocks.remainingCapacity() == 0)) {
                return;
            }

            active = true;

            try{
                taskExecutor.submit(this);
            } catch (RejectedExecutionException ree) {
                active = false;
                close(ree);
            }
        }

        /**
         *  Method run by the producer thread only
         */
        @Override
        public void run() {

            try {
                assert active;
                assert !doneReading;
                assert blocks.remainingCapacity() > 0;

                final long start = System.nanoTime();

                final int count = readBlock();

                final long end = System.nanoTime();
                final long thisTimeMs = (end - start) / NANOS_TO_MILLIS;
                updateDetailedMetrics(thisTimeMs, count);
            } catch (RuntimeException re) {
                active = false;
                close(re);
            }
        }

        /**
         * Read a block of records from the store. Returns the number of
         * records read.
         *
         * Method run by the producer thread only
         */
        private int readBlock() {

            final Request req = makeReadRequest();

            if (req == null) {
                synchronized (this) {
                    /* About to exit, active may be reset in submit() */
                    active = false;
                    doneReading = true;
                    /* Wake up the iterator if it is waiting */
                    notify();
                    return 0;
                }
            }

            assert storeImpl.getParallelScanHook() == null ?
                true :
                storeImpl.getParallelScanHook().
                callback(Thread.currentThread(),
                         HookType.BEFORE_EXECUTE_REQUEST, null);

            final Result result = storeImpl.executeRequest(req);

            final boolean hasMore = hasMoreElements(result);
            int nRecords = result.getNumRecords();

            if (nRecords > 0) {
                setResumeKey(result);
            }

            synchronized (this) {
                /* About to exit, active may be reset in submit() */
                active = false;
                doneReading = !hasMore;

                if (nRecords == 0) {
                    assert storeImpl.getParallelScanHook() == null ?
                           true :
                           storeImpl.getParallelScanHook().
                           callback(Thread.currentThread(),
                                    HookType.AFTER_PROCESSING_STREAM, null);
                    notify();
                    return 0;
                }

                blocks.add(result);

                /* Wake up the iterator if it is waiting */
                notify();
            }

            submit();
            return nRecords;
        }

        /**
         * Update resume key for next read request.
         * @param result result object
         * @param elementList the list of elements converted by convert
         */
        protected abstract void setResumeKey(Result result);

        /**
         * Create a request using resume key to get next block of records.
         * The read request may return null for bulk get operations when the
         * keys supplied by the application run out and no further requests to
         * the store are required.
         *
         * see KVStore.storeIterator(Iterator, int, KeyRange, Depth,
         *                           Consistency, long, TimeUnit,
         *                           StoreIteratorConfig)
         */
        protected abstract Request makeReadRequest();

        protected boolean hasMoreElements(Result result) {
            return result.hasMoreElements();
        }

        /**
         * Compares this object with the specified object for order for
         * determining the placement of the stream in the TreeSet. See
         * the IndexScan class doc a detailed description of its operation.
         */
        @Override
        public int compareTo(Stream other) {
            /*
             * The same stream is always equal. This is the only time that
             * 0 can be returned.
             */
            if (this == other) {
                return 0;
            }

            /*
             * If unordered, we skip comparing the streams and always sort
             * to the top if we have a next element. If no elements, sort
             * to the bottom. This will keep full streams at the top and
             * empty streams at the bottom.
             */
            if (itrDirection == Direction.UNORDERED) {
                return (nextElem == null) ? 1 : -1;
            }

            /* Forward or reverse */

            /* If we don't have a next, then sort to the top of the set. */
            if (nextElem == null) {
                return -1;
            }

            /* If the other next is null then keep them on top. */
            final K otherNext = other.nextElem;
            if (otherNext == null) {
                return 1;
            }

            /*
             * Finally, compare the elements, inverting the result
             * if necessary.
             */
            final int comp = compareInternal(other);

            /*
             * If the different stream elements are equal then there is
             * a duplicate entry, possibly due to partition migration. We
             * currently don't handle is case, so punt.
             */
            if (comp == 0) {
                close(new IllegalStateException("Detected an unexpected " +
                                                "duplicate record"));
            }

            return comp;
        }

        /**
         * Subclasses may override this
         */
        protected int compareInternal(Stream other) {
            int cmp = compare(nextElem, other.nextElem);
            return itrDirection == Direction.FORWARD ? cmp : (cmp * -1);
        }

        /**
         * Get stream status string.
         */
        public String getStatus() {
            return done + ", " + active + ", " + doneReading + ", "
                   + blocks.size();
        }

        /**
         * Update the metrics for this iterator.
         *
         * @param timeInMs the time spent reading
         * @param recordCount the number of records read
         */
        protected abstract void updateDetailedMetrics(
            final long timeInMs,
            final long recordCount);
    }
}
