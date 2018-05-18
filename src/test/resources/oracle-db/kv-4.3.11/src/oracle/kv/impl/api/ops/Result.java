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

import static oracle.kv.impl.util.SerialVersion.TTL_SERIAL_VERSION;
import static oracle.kv.impl.util.SerialVersion.QUERY_VERSION_2;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import oracle.kv.Operation;
import oracle.kv.OperationExecutionException;
import oracle.kv.OperationResult;
import oracle.kv.Value;
import oracle.kv.Version;
import oracle.kv.impl.api.table.FieldDefImpl;
import oracle.kv.impl.api.ops.InternalOperation.OpCode;
import oracle.kv.impl.util.FastExternalizable;
import oracle.kv.impl.api.table.FieldDefSerialization;
import oracle.kv.impl.api.table.FieldValueImpl;
import oracle.kv.impl.api.table.FieldValueSerialization;

/**
 * The result of running a request.  Result may contain a return value of the
 * request.  It may also contain an error, an update to some topology
 * information, or information about how the request was satisfied (such as the
 * forwarding path it took).
 */
public abstract class Result
    implements OperationResult, FastExternalizable {

    /**
     * The OpCode determines the result type for deserialization, and may be
     * useful for matching to the request OpCode.
     */
    private final OpCode opCode;

    /**
     * Constructs a request result that contains a value resulting from an
     * operation.
     */
    private Result(OpCode op) {
        opCode = op;
        assert op.checkResultType(this) :
        "Incorrect type " + getClass().getName() + " for " + op;
    }

    /**
     * FastExternalizable constructor.  Subclasses must call this constructor
     * before reading additional elements.
     *
     * The OpCode was read by readFastExternal.
     */
    Result(OpCode op,
           @SuppressWarnings("unused") DataInput in,
           @SuppressWarnings("unused") short serialVersion) {

        this(op);
    }

    /**
     * FastExternalizable factory for all Result subclasses.
     */
    public static Result readFastExternal(DataInput in, short serialVersion)
        throws IOException {

        final OpCode op = OpCode.readFastExternal(in, serialVersion);
        return op.readResult(in, serialVersion);
    }

    /**
     * FastExternalizable writer.  Subclasses must call this method before
     * writing additional elements.
     */
    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
        throws IOException {

        opCode.writeFastExternal(out, serialVersion);
    }

    /**
     * Gets the boolean result for all operations.
     *
     * @throws IllegalStateException if the result is the wrong type
     */
    @Override
    public abstract boolean getSuccess();

    /**
     * Get the primary-index key to be used as the starting point for
     * the primary index scan that will produce the next result set.
     */
    public byte[] getPrimaryResumeKey() {
       throw new IllegalStateException(
           "result of type: " + getClass() +
           " does not contain a primary resume key");
    }

    /**
     * Get the secondary-index key to be used as the starting point for
     * the secondary index scan that will produce the next result set.
     */
    public byte[] getSecondaryResumeKey() {
       throw new IllegalStateException(
           "result of type: " + getClass() +
           " does not contain a secondary resume key");
    }

    /**
     * Gets the current Value result of a Get, Put or Delete operation.
     *
     * @throws IllegalStateException if the result is the wrong type
     */
    @Override
    public Value getPreviousValue() {
        throw new IllegalStateException
            ("result of type: " + getClass() + " does not contain a Value");
    }

    /**
     * Gets the current Version result of a Get, Put or Delete operation.
     *
     * @throws IllegalStateException if the result is the wrong type
     */
    @Override
    public Version getPreviousVersion() {
        throw new IllegalStateException
            ("result of type: " + getClass() +
             " does not contain a previous Version");
    }

    /**
     * Gets the new Version result of a Put operation.
     *
     * @throws IllegalStateException if the result is the wrong type
     */
    @Override
    public Version getNewVersion() {
        throw new IllegalStateException("result of type: " + getClass() +
                                        " does not contain a new Version");
    }

    /**
     * Gets the new expiration time a Put operation.
     *
     * @throws IllegalStateException if the result is the wrong type
     */
    @Override
    public long getNewExpirationTime() {
        throw new IllegalStateException("result of type: " + getClass() +
                                        " does not contain an " +
                                        "expiration time");
    }

    @Override
    public long getPreviousExpirationTime() {
        throw new IllegalStateException("result of type: " + getClass() +
                                        " does not contain a " +
                                        "previous expiration time");
    }

    /**
     * Gets the int result of a MultiDelete operation.
     *
     * @throws IllegalStateException if the result is the wrong type
     */
    public int getNDeletions() {
        throw new IllegalStateException
            ("result of type: " + getClass() + " does not contain a boolean");
    }

    /**
     * Gets the OperationExecutionException result of an Execute operation, or
     * null if no exception should be thrown.
     *
     * @throws IllegalStateException if the result is the wrong type
     */
    public OperationExecutionException
        getExecuteException(@SuppressWarnings("unused") List<Operation> ops) {

        throw new IllegalStateException
            ("result of type: " + getClass() +
             " does not contain an OperationExecutionException");
    }

    /**
     * Gets the OperationResult list result of an Execute operation, or null if
     * an OperationExecutionException should be thrown.
     *
     * @throws IllegalStateException if the result is the wrong type
     */
    public List<OperationResult> getExecuteResult() {
        throw new IllegalStateException
            ("result of type: " + getClass() +
             " does not contain a ExecuteResult");
    }

    /**
     * Gets the ResultKeyValueVersion list result of an iterate operation.
     *
     * @throws IllegalStateException if the result is the wrong type
     */
    public List<ResultKeyValueVersion> getKeyValueVersionList() {
        throw new IllegalStateException
            ("result of type: " + getClass() +
             " does not contain a ResultKeyValueVersion list");
    }

    /**
     * Gets the key list result of an iterate-keys operation.
     *
     * @throws IllegalStateException if the result is the wrong type
     */
    public List<ResultKey> getKeyList() {
        throw new IllegalStateException
            ("result of type: " + getClass() +
             " does not contain a key list");
    }

    /**
     * Gets the ResultIndexKeys list result of a table index keys
     * iterate operation.
     *
     * @throws IllegalStateException if the result is the wrong type
     */
    public List<ResultIndexKeys> getIndexKeyList() {
        throw new IllegalStateException
            ("result of type: " + getClass() +
             " does not contain a ResultIndexKeys list");
    }

    /**
     * Gets the ResultIndexRows list result of a table index row
     * iterate operation.
     *
     * @throws IllegalStateException if the result is the wrong type
     */
    public List<ResultIndexRows> getIndexRowList() {
        throw new IllegalStateException
            ("result of type: " + getClass() +
             " does not contain a ResultIndexRows list");
    }

    /**
     * Gets the ResultRecord list of a query operation.
     *
     * @throws IllegalStateException if the result is the wrong type
     *
     * @since ???
     */
    public List<FieldValueImpl> getQueryResults() {
        throw new IllegalStateException
            ("result of type: " + getClass() +
             " is not a query result");
    }

    /**
     * Gets the has-more-elements result of an iterate or iterate-keys
     * operation. True returned if the iteration is complete.
     *
     * @throws IllegalStateException if the result is the wrong type
     */
    public boolean hasMoreElements() {
        throw new IllegalStateException
            ("result of type: " + getClass() +
             " does not contain an iteration result");
    }

    /**
     * Gets the parent key index to start from in multi-get-batch or
     * multi-get-batch-keys operation.
     *
     * @throws IllegalStateException if the result is the wrong type
     */
    public int getResumeParentKeyIndex() {
        throw new IllegalStateException
            ("result of type: " + getClass() +
             " does not contain a resume parent key index");
    }

    /**
     * The number of records returned or processed as part of this operation.
     * Single operations only apply to one record, but the multi, iterate, or
     * execute operations will work on multiple records, and should override
     * this to provide the correct number of operations.
     */
    public int getNumRecords() {
        return 1;
    }


    /*
     * The result of a Get operation.
     */
    static class GetResult extends ValueVersionResult {

        GetResult(OpCode opCode, ResultValueVersion valueVersion) {
            super(opCode, valueVersion);
        }

         /**
         * FastExternalizable constructor.  Must call superclass constructor
         * first to read common elements.
         */
        GetResult(OpCode opCode, DataInput in, short serialVersion)
            throws IOException {

            super(opCode, in, serialVersion);
       }

        @Override
        public boolean getSuccess() {
            return getPreviousValue() != null;
        }
    }


    /*
     * The result of a Put operation.
     */
    static class PutResult extends ValueVersionResult {

        private final Version newVersion;  /* of the new record */
        private final long newExpirationTime; /* of the new record */

        /**
         * Constructs a result with required arguments.
         *
         * @param opCode code for operation that produced this result
         * @param prevVal prior value, can be null
         * @param newVersionAndExpiration the new version and expiration time
         * of the record being put. May be null if the operation failed.
         */
        PutResult(OpCode opCode,
                  ResultValueVersion prevVal,
                  InternalOperationHandler.VersionAndExpiration ve) {
            super(opCode, prevVal);
            if (ve != null) {
                newVersion = ve.getVersion();
                newExpirationTime = ve.getExpirationTime();
            } else {
                newVersion = null;
                newExpirationTime = 0L;
            }
        }

        /**
         * FastExternalizable constructor.  Must call superclass constructor
         * first to read common elements.
         */
        PutResult(OpCode opCode, DataInput in, short serialVersion)
            throws IOException {

            super(opCode, in, serialVersion);
            if (in.readByte() != 0) {
                newVersion =  Version.createVersion(in, serialVersion);
             } else {
                 newVersion = null;
             }
            newExpirationTime = readExpirationTime(in, serialVersion);
        }

        /**
         * FastExternalizable writer.  Must call superclass method first to
         * write common elements.
         */
        @Override
        public void writeFastExternal(DataOutput out, short serialVersion)
            throws IOException {
            super.writeFastExternal(out, serialVersion);
            out.write(newVersion == null ? 0 : 1);
            if (newVersion != null) {
                newVersion.writeFastExternal(out, serialVersion);
            }
            writeExpirationTime(out,
                                newExpirationTime,
                                serialVersion);
        }

        @Override
        public boolean getSuccess() {
            return newVersion != null;
        }

        @Override
        public Version getNewVersion() {
            return newVersion;
        }

        @Override
        public long getNewExpirationTime() {
            return newExpirationTime;
        }
    }


    /*
     * The result of a Delete operation.
     */
    static class DeleteResult extends ValueVersionResult {

        private final boolean success;

        DeleteResult(OpCode opCode,
                     ResultValueVersion prevVal,
                     boolean success) {
            super(opCode, prevVal);
            this.success = success;
        }

        /**
         * FastExternalizable constructor.  Must call superclass constructor
         * first to read common elements.
         */
        DeleteResult(OpCode opCode, DataInput in, short serialVersion)
            throws IOException {

            super(opCode, in, serialVersion);
            success = in.readBoolean();
        }

        /**
         * FastExternalizable writer.  Must call superclass method first to
         * write common elements.
         */
        @Override
        public void writeFastExternal(DataOutput out, short serialVersion)
            throws IOException {

            super.writeFastExternal(out, serialVersion);
            out.writeBoolean(success);
        }

        @Override
        public boolean getSuccess() {
            return success;
        }
    }


    /*
     * The result of a Delete operation.
     */
    static class MultiDeleteResult extends Result {

        private final int nDeletions;

        MultiDeleteResult(OpCode opCode, int nDeletions) {
            super(opCode);
            this.nDeletions = nDeletions;
        }

        /**
         * FastExternalizable constructor.  Must call superclass constructor
         * first to read common elements.
         */
        MultiDeleteResult(OpCode opCode, DataInput in, short serialVersion)
            throws IOException {

            super(opCode, in, serialVersion);
            nDeletions = in.readInt();
        }

        /**
         * FastExternalizable writer.  Must call superclass method first to
         * write common elements.
         */
        @Override
        public void writeFastExternal(DataOutput out, short serialVersion)
            throws IOException {

            super.writeFastExternal(out, serialVersion);
            out.writeInt(nDeletions);
        }

        @Override
        public int getNDeletions() {
            return nDeletions;
        }

        @Override
        public boolean getSuccess() {
            return nDeletions > 0;
        }

        @Override
        public int getNumRecords() {
            return nDeletions;
        }
    }


    /*
     * Base class for results with a Value and Version.
     */
    static abstract class ValueVersionResult extends Result {

        private final ResultValue resultValue;
        protected Version version;
        private final long expirationTime;

        ValueVersionResult(OpCode op, ResultValueVersion valueVersion) {
            super(op);
            if (valueVersion != null) {
                resultValue = (valueVersion.getValueBytes() != null) ?
                    (new ResultValue(valueVersion.getValueBytes())) :
                    null;
                version = valueVersion.getVersion();
                expirationTime = valueVersion.getExpirationTime();
            } else {
                resultValue = null;
                version = null;
                expirationTime = 0;
            }
        }

        /**
         * FastExternalizable constructor.  Must call superclass constructor
         * first to read common elements.
         */
        ValueVersionResult(OpCode op, DataInput in, short serialVersion)
            throws IOException {

            super(op, in, serialVersion);
            if (in.readByte() != 0) {
                resultValue = new ResultValue(in, serialVersion);
            } else {
                resultValue = null;
            }
            if (in.readByte() != 0) {
                version =  Version.createVersion(in, serialVersion);
            } else {
                version = null;
            }
            expirationTime = readExpirationTime(in, serialVersion);
        }

        /**
         * FastExternalizable writer.  Must call superclass method first to
         * write common elements.
         */
        @Override
        public void writeFastExternal(DataOutput out, short serialVersion)
            throws IOException {

            super.writeFastExternal(out, serialVersion);
            if (resultValue != null) {
                out.write(1);
                resultValue.writeFastExternal(out, serialVersion);
            } else {
                out.write(0);
            }
            out.write(version == null ? 0 : 1);
            if (version != null) {
                version.writeFastExternal(out, serialVersion);
            }
            writeExpirationTime(out,
                                expirationTime,
                                serialVersion);
        }

        @Override
        public Value getPreviousValue() {
            return (resultValue == null) ? null : resultValue.getValue();
        }

        @Override
        public Version getPreviousVersion() {
            return version;
        }

        @Override
        public long getPreviousExpirationTime() {
            return expirationTime;
        }
    }

    /*
     *
     */
    static class NOPResult extends Result {

        NOPResult(DataInput in, short serialVersion) {
            super(OpCode.NOP, in, serialVersion);
        }

        NOPResult() {
            super(OpCode.NOP);
        }

        @Override
        public boolean getSuccess() {
            return true;
        }

        /* NOPs don't actually handle any records. */
        @Override
        public int getNumRecords() {
            return 0;
        }
    }


    /*
     * The result of an Execute operation.
     */
    static class ExecuteResult extends Result {

        private final boolean success;
        private final List<Result> successResults;
        private final int failureIndex;
        private final Result failureResult;

        ExecuteResult(OpCode opCode, List<Result> successResults) {
            super(opCode);
            this.successResults = successResults;
            failureIndex = -1;
            failureResult = null;
            success = true;
        }

        ExecuteResult(OpCode opCode,
                      int failureIndex,
                      Result failureResult) {
            super(opCode);
            this.failureIndex = failureIndex;
            this.failureResult = failureResult;
            successResults = null;
            success = false;
        }

        /**
         * FastExternalizable constructor.  Must call superclass constructor
         * first to read common elements.
         */
        ExecuteResult(OpCode opCode, DataInput in, short serialVersion)
            throws IOException {

            super(opCode, in, serialVersion);
            success = in.readBoolean();
            if (success) {
                final int listSize = in.readInt();
                successResults = new ArrayList<Result>(listSize);
                for (int i = 0; i < listSize; i += 1) {
                    final Result result =
                        Result.readFastExternal(in, serialVersion);
                    successResults.add(result);
                }
                failureIndex = -1;
                failureResult = null;
            } else {
                failureIndex = in.readInt();
                failureResult = Result.readFastExternal(in, serialVersion);
                successResults = new ArrayList<Result>();
            }
        }

        /**
         * FastExternalizable writer.  Must call superclass method first to
         * write common elements.
         */
        @Override
        public void writeFastExternal(DataOutput out, short serialVersion)
            throws IOException {

            super.writeFastExternal(out, serialVersion);
            out.writeBoolean(success);
            if (success) {
                out.writeInt(successResults.size());
                for (final Result result : successResults) {
                    result.writeFastExternal(out, serialVersion);
                }
            } else {
                out.writeInt(failureIndex);
                failureResult.writeFastExternal(out, serialVersion);
            }
        }


        @Override
        public boolean getSuccess() {
            return success;
        }

        @Override
        public OperationExecutionException
            getExecuteException(List<Operation> ops) {

            if (success) {
                return null;
            }
            return new OperationExecutionException
                (ops.get(failureIndex), failureIndex, failureResult);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public List<OperationResult> getExecuteResult() {
            if (!success) {
                return null;
            }
            /* Cast: a Result is an OperationResult. */
            return (List) Collections.unmodifiableList(successResults);
        }

        @Override
        public int getNumRecords() {
            if (!success) {
                return 0;
            }
            return successResults.size();
        }
    }

    public static class PutBatchResult extends Result {

        private int numKVPairs;
        private List<Integer> keysPresent;

        PutBatchResult(int numKVPairs, List<Integer> keysPresent) {
            super(OpCode.PUT_BATCH);
            this.numKVPairs = numKVPairs;
            this.keysPresent = keysPresent;
        }


        PutBatchResult(OpCode op, DataInput in, short serialVersion)
            throws IOException {

            super(op, in, serialVersion);

            numKVPairs = in.readInt();

            final int count = in.readInt();
            if (count == 0) {
                keysPresent = Collections.emptyList();
                return;
            }

            keysPresent = new ArrayList<Integer>(count);
            for (int i = 0; i < count; i++) {
               keysPresent.add(in.readInt());
            }
        }

        @Override
        public void writeFastExternal(DataOutput out, short serialVersion)
            throws IOException {

            super.writeFastExternal(out, serialVersion);

            out.writeInt(numKVPairs);

            final int count = keysPresent.size();
            out.writeInt(count);

            for (int position : keysPresent) {
                out.writeInt(position);
            }
        }

        @Override
        public boolean getSuccess() {
            return true;
        }

        public List<Integer> getKeysPresent() {
            return keysPresent;
        }

        @Override
        public int getNumRecords() {
            return numKVPairs - keysPresent.size();
        }
    }



    /*
     * The result of a MultiGetIterate or StoreIterate operation.
     */
    static class IterateResult extends Result {

        private final List<ResultKeyValueVersion> elements;
        private final boolean moreElements;

        IterateResult(OpCode opCode,
                      List<ResultKeyValueVersion> elements,
                      boolean moreElements) {
            super(opCode);
            this.elements = elements;
            this.moreElements = moreElements;
        }

        /**
         * FastExternalizable constructor.  Must call superclass constructor
         * first to read common elements.
         */
        IterateResult(OpCode opCode, DataInput in, short serialVersion)
            throws IOException {

            super(opCode, in, serialVersion);

            final int listSize = in.readInt();
            elements = new ArrayList<ResultKeyValueVersion>(listSize);
            for (int i = 0; i < listSize; i += 1) {
                elements.add(new ResultKeyValueVersion(in, serialVersion));
            }

            moreElements = in.readBoolean();
        }

        /**
         * FastExternalizable writer.  Must call superclass method first to
         * write common elements.
         */
        @Override
        public void writeFastExternal(DataOutput out, short serialVersion)
            throws IOException {

            super.writeFastExternal(out, serialVersion);

            out.writeInt(elements.size());
            for (final ResultKeyValueVersion elem : elements) {
                elem.writeFastExternal(out, serialVersion);
            }

            out.writeBoolean(moreElements);
        }

        @Override
        public boolean getSuccess() {
            return elements.size() > 0;
        }

        @Override
        public List<ResultKeyValueVersion> getKeyValueVersionList() {
            return elements;
        }

        @Override
        public boolean hasMoreElements() {
            return moreElements;
        }

        @Override
        public int getNumRecords() {
            return elements.size();
        }

        @Override
        public byte[] getPrimaryResumeKey() {

            if (!moreElements || elements == null || elements.isEmpty()) {
                return null;
            }

            return elements.get(elements.size() - 1).getKeyBytes();
        }
    }


    /*
     * The result of a MultiGetKeysIterate or StoreKeysIterate operation.
     */
    static class KeysIterateResult extends Result {

        private final List<ResultKey> elements;
        private final boolean moreElements;

        KeysIterateResult(OpCode opCode,
                          List<ResultKey> elements,
                          boolean moreElements) {
            super(opCode);
            this.elements = elements;
            this.moreElements = moreElements;
        }

        /**
         * FastExternalizable constructor.  Must call superclass constructor
         * first to read common elements.
         */
        KeysIterateResult(OpCode opCode, DataInput in, short serialVersion)
            throws IOException {

            super(opCode, in, serialVersion);

            final int listSize = in.readInt();
            elements = new ArrayList<ResultKey>(listSize);
            for (int i = 0; i < listSize; i += 1) {
                elements.add(new ResultKey(in, serialVersion));
            }

            moreElements = in.readBoolean();
        }

        /**
         * FastExternalizable writer.  Must call superclass method first to
         * write common elements.
         */
        @Override
        public void writeFastExternal(DataOutput out, short serialVersion)
            throws IOException {

            super.writeFastExternal(out, serialVersion);

            out.writeInt(elements.size());
            for (final ResultKey rkey : elements) {
                rkey.writeFastExternal(out, serialVersion);
            }

            out.writeBoolean(moreElements);
        }

        @Override
        public boolean getSuccess() {
            return elements.size() > 0;
        }

        @Override
        public List<ResultKey> getKeyList() {
            return elements;
        }

        @Override
        public boolean hasMoreElements() {
            return moreElements;
        }

        @Override
        public int getNumRecords() {
            return elements.size();
        }

        @Override
        public byte[] getPrimaryResumeKey() {

            if (!moreElements || elements == null || elements.isEmpty()) {
                return null;
            }

            return elements.get(elements.size() - 1).getKeyBytes();
        }
    }


    /*
     * The result of a table index key iterate operation.
     */
    static class IndexKeysIterateResult extends Result {

        private final List<ResultIndexKeys> elements;
        private final boolean moreElements;

        IndexKeysIterateResult(OpCode opCode,
                               List<ResultIndexKeys> elements,
                               boolean moreElements) {
            super(opCode);
            this.elements = elements;
            this.moreElements = moreElements;
        }

        /**
         * FastExternalizable constructor.  Must call superclass constructor
         * first to read common elements.
         */
        IndexKeysIterateResult(OpCode opCode, DataInput in, short serialVersion)
            throws IOException {

            super(opCode, in, serialVersion);

            final int listSize = in.readInt();
            elements = new ArrayList<ResultIndexKeys>(listSize);
            for (int i = 0; i < listSize; i += 1) {
                elements.add(new ResultIndexKeys(in, serialVersion));
            }

            moreElements = in.readBoolean();
        }

        /**
         * FastExternalizable writer.  Must call superclass method first to
         * write common elements.
         */
        @Override
        public void writeFastExternal(DataOutput out, short serialVersion)
            throws IOException {

            super.writeFastExternal(out, serialVersion);

            out.writeInt(elements.size());
            for (final ResultIndexKeys elem : elements) {
                elem.writeFastExternal(out, serialVersion);
            }

            out.writeBoolean(moreElements);
        }

        @Override
        public boolean getSuccess() {
            return elements.size() > 0;
        }

        @Override
        public List<ResultIndexKeys> getIndexKeyList() {
            return elements;
        }

        @Override
        public boolean hasMoreElements() {
            return moreElements;
        }

        @Override
        public int getNumRecords() {
            return elements.size();
        }

        @Override
        public byte[] getPrimaryResumeKey() {

            if (!moreElements || elements == null || elements.isEmpty()) {
                return null;
            }

            return elements.get(elements.size() - 1).getPrimaryKeyBytes();
        }

        @Override
        public byte[] getSecondaryResumeKey() {

            if (!moreElements || elements == null || elements.isEmpty()) {
                return null;
            }

            return elements.get(elements.size() - 1).getIndexKeyBytes();
        }
    }


    /*
     * The result of a table index row iterate operation.
     */
    static class IndexRowsIterateResult extends Result {

        private final List<ResultIndexRows> elements;
        private final boolean moreElements;

        IndexRowsIterateResult(OpCode opCode,
                               List<ResultIndexRows> elements,
                               boolean moreElements) {
            super(opCode);
            this.elements = elements;
            this.moreElements = moreElements;
        }

        /**
         * FastExternalizable constructor.  Must call superclass constructor
         * first to read common elements.
         */
        IndexRowsIterateResult(OpCode opCode, DataInput in, short serialVersion)
            throws IOException {

            super(opCode, in, serialVersion);

            final int listSize = in.readInt();
            elements = new ArrayList<ResultIndexRows>(listSize);
            for (int i = 0; i < listSize; i += 1) {
                elements.add(new ResultIndexRows(in, serialVersion));
            }

            moreElements = in.readBoolean();
        }

        /**
         * FastExternalizable writer.  Must call superclass method first to
         * write common elements.
         */
        @Override
        public void writeFastExternal(DataOutput out, short serialVersion)
            throws IOException {

            super.writeFastExternal(out, serialVersion);

            out.writeInt(elements.size());
            for (final ResultIndexRows elem : elements) {
                elem.writeFastExternal(out, serialVersion);
            }

            out.writeBoolean(moreElements);
        }

        @Override
        public boolean getSuccess() {
            return elements.size() > 0;
        }

        @Override
        public List<ResultIndexRows> getIndexRowList() {
            return elements;
        }

        @Override
        public boolean hasMoreElements() {
            return moreElements;
        }

        @Override
        public int getNumRecords() {
            return elements.size();
        }

        @Override
        public byte[] getPrimaryResumeKey() {

            if (!moreElements || elements == null || elements.isEmpty()) {
                return null;
            }

            return elements.get(elements.size() - 1).getKeyBytes();
        }

        @Override
        public byte[] getSecondaryResumeKey() {

            if (!moreElements || elements == null || elements.isEmpty()) {
                return null;
            }

            return elements.get(elements.size() - 1).getIndexKeyBytes();
        }
    }


    /*
     * The result of a multi-get-batch iteration operation.
     */
    static class BulkGetIterateResult extends IterateResult {

        private final int resumeParentKeyIndex;

        BulkGetIterateResult(OpCode opCode,
                             List<ResultKeyValueVersion> elements,
                             boolean moreElements,
                             int resumeParentKeyIndex) {
            super(opCode, elements, moreElements);
            this.resumeParentKeyIndex = resumeParentKeyIndex;
        }

        /**
         * FastExternalizable constructor.  Must call superclass constructor
         * first to read common elements.
         */
        BulkGetIterateResult(OpCode opCode, DataInput in, short serialVersion)
            throws IOException {

            super(opCode, in, serialVersion);
            resumeParentKeyIndex = in.readInt();
        }

        /**
         * FastExternalizable writer.  Must call superclass method first to
         * write common elements.
         */
        @Override
        public void writeFastExternal(DataOutput out, short serialVersion)
            throws IOException {

            super.writeFastExternal(out, serialVersion);
            out.writeInt(resumeParentKeyIndex);
        }

        /**
         * Returns the parent key index to start from if has more elements,
         * returns -1 if no more element.
         */
        @Override
        public int getResumeParentKeyIndex() {
            return resumeParentKeyIndex;
        }
    }


    /*
     * The result of a multi-get-batch-keys iteration operation.
     */
    static class BulkGetKeysIterateResult extends KeysIterateResult {

        private final int resumeParentKeyIndex;

        BulkGetKeysIterateResult(OpCode opCode,
                                 List<ResultKey> elements,
                                 boolean moreElements,
                                 int lastParentKeyIndex) {
            super(opCode, elements, moreElements);
            this.resumeParentKeyIndex = lastParentKeyIndex;
        }

        /**
         * FastExternalizable constructor.  Must call superclass constructor
         * first to read common elements.
         */
        BulkGetKeysIterateResult(OpCode opCode,
                                 DataInput in,
                                 short serialVersion)
            throws IOException {

            super(opCode, in, serialVersion);
            resumeParentKeyIndex = in.readInt();
        }

        /**
         * FastExternalizable writer.  Must call superclass method first to
         * write common elements.
         */
        @Override
        public void writeFastExternal(DataOutput out, short serialVersion)
            throws IOException {

            super.writeFastExternal(out, serialVersion);
            out.writeInt(resumeParentKeyIndex);
        }

        /**
         * Returns the parent key index to start from if has more elements,
         * returns -1 if no more element.
         */
        @Override
        public int getResumeParentKeyIndex() {
            return resumeParentKeyIndex;
        }
    }

    /*
     * The result of a Query operation.
     * This class is public to allow access to the resume key.
     *
     * @since 4.0
     */
    public static class QueryResult extends Result {

        private final List<FieldValueImpl> results;

        private final FieldDefImpl resultDef;

        /*
         * added in QUERY_VERSION_2
         */
        private final boolean mayReturnNULL;

        private final boolean moreElements;

        private final byte[] primaryResumeKey;

        private final byte[] secondaryResumeKey;
        /* TBD: handle inserts, deletes, etc */

        QueryResult(OpCode opCode,
                    List<FieldValueImpl> results,
                    FieldDefImpl resultDef,
                    boolean mayReturnNULL,
                    boolean moreElements,
                    byte[] primaryResumeKey,
                    byte[] secondaryResumeKey) {
            super(opCode);
            this.results = results;
            this.resultDef = resultDef;
            this.mayReturnNULL = mayReturnNULL;
            this.moreElements = moreElements;
            this.primaryResumeKey = primaryResumeKey;
            this.secondaryResumeKey = secondaryResumeKey;
        }

        /**
         * FastExternalizable constructor.  Must call superclass constructor
         * first to read common elements.
         */
        QueryResult(OpCode opCode, DataInput in, short serialVersion)
            throws IOException {

            super(opCode, in, serialVersion);

            try {
                resultDef = FieldDefSerialization.readFieldDef(in, serialVersion);

                if (serialVersion >= QUERY_VERSION_2) {
                    mayReturnNULL = in.readBoolean();
                } else {
                    mayReturnNULL = false;
                }

                FieldDefImpl valDef = (resultDef.isWildcard() ?
                                       null :
                                       resultDef);

                final int listSize = in.readInt();
                results = new ArrayList<FieldValueImpl>(listSize);

                if (mayReturnNULL) {
                    for (int i = 0; i < listSize; i += 1) {
                        FieldValueImpl val = (FieldValueImpl)
                            FieldValueSerialization.
                            readFieldValue(valDef,
                                           in,
                                           serialVersion);
                        results.add(val);
                    }
                } else {
                    for (int i = 0; i < listSize; i += 1) {
                        FieldValueImpl val = (FieldValueImpl)
                            FieldValueSerialization.
                            readFieldValue(valDef,
                                           null, // valKind
                                           in,
                                           serialVersion);
                        results.add(val);
                    }
                }

                moreElements = in.readBoolean();

                /*
                 * If there are no more elements, resume key(s) are not written.
                 */
                if (moreElements) {
                    int keyLen = in.readShort();
                    if (keyLen > 0) {
                        primaryResumeKey = new byte[keyLen];
                        in.readFully(primaryResumeKey);
                    } else {
                        primaryResumeKey = null;
                    }

                    keyLen = in.readShort();
                    if (keyLen > 0) {
                        secondaryResumeKey = new byte[keyLen];
                        in.readFully(secondaryResumeKey);
                    } else {
                        secondaryResumeKey = null;
                    }

                } else {
                    primaryResumeKey = null;
                    secondaryResumeKey = null;
                }
            } catch (IOException e) {
                System.out.println("Failed to deserialize result");
                e.printStackTrace();
                throw e;
            } catch (ClassCastException e) {
                System.out.println("Failed to deserialize result");
                e.printStackTrace();
                throw e;
            }
        }

        /**
         * FastExternalizable writer.  Must call superclass method first to
         * write common elements.
         */
        @Override
        public void writeFastExternal(DataOutput out, short serialVersion)
            throws IOException {

            try {
                super.writeFastExternal(out, serialVersion);

                FieldDefSerialization.writeFieldDef(resultDef, out, serialVersion);

                if (serialVersion >= QUERY_VERSION_2) {
                    out.writeBoolean(mayReturnNULL);
                }

                out.writeInt(results.size());

                boolean isWildcard = resultDef.isWildcard();

                if (mayReturnNULL) {
                    for (final FieldValueImpl res : results) {
                        FieldValueSerialization.
                            writeFieldValue(res,
                                            isWildcard, //writeValDef
                                            out, serialVersion);
                    }
                } else {
                    for (final FieldValueImpl res : results) {
                        FieldValueSerialization.
                            writeFieldValue(res,
                                            isWildcard, //writeValDef
                                            true, // writeValKind
                                            out, serialVersion);
                    }
                }

                out.writeBoolean(moreElements);

                /*
                 * Only write resume key(s) if moreElements.
                 */
                if (moreElements) {
                    assert(primaryResumeKey != null || secondaryResumeKey != null);
                    if (primaryResumeKey != null) {
                        out.writeShort(primaryResumeKey.length);
                        out.write(primaryResumeKey);
                    } else {
                        out.writeShort(0);
                    }
                    if (secondaryResumeKey != null) {
                        out.writeShort(secondaryResumeKey.length);
                        out.write(secondaryResumeKey);
                    } else {
                        out.writeShort(0);
                    }
                }
            } catch (IOException e) {
                System.out.println("Failed to serialize result");
                e.printStackTrace();
                throw e;
            } catch (ClassCastException e) {
                System.out.println("Failed to deserialize result");
                e.printStackTrace();
                throw e;
            }
        }

        @Override
        public boolean getSuccess() {
            return results.size() > 0;
        }

        @Override
        public List<FieldValueImpl> getQueryResults() {
            return results;
        }

        @Override
        public boolean hasMoreElements() {
            return moreElements;
        }

        @Override
        public int getNumRecords() {
            return results.size();
        }

        @Override
        public byte[] getPrimaryResumeKey() {
            return primaryResumeKey;
        }

        @Override
        public byte[] getSecondaryResumeKey() {
            return secondaryResumeKey;
        }
    }

    /**
     * Utility method to write an expiration time conditionally into an output
     * stream based on serial version.
     */
    static void writeExpirationTime(DataOutput out,
                                    long expirationTime,
                                    short serialVersion)
    throws IOException {

        if (serialVersion >= TTL_SERIAL_VERSION) {
            if (expirationTime == 0) {
                out.write(0);
            } else {
                out.write(1);
                out.writeLong(expirationTime);
            }
        }
    }

    /**
     * Utility method to read an expiration time conditionally from an input
     * stream based on serial version. Returns 0 if it is not available in the
     * stream.
     */
    static long readExpirationTime(DataInput in,
                                   short serialVersion)
        throws IOException {

        if (serialVersion >= TTL_SERIAL_VERSION) {
            if (in.readByte() != 0) {
                return in.readLong();
            }
        }
        return 0;
    }
}
