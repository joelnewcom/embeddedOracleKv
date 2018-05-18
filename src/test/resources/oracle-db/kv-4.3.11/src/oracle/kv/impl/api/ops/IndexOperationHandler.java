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

import java.util.Collections;
import java.util.List;

import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.Transaction;

import oracle.kv.FaultException;
import oracle.kv.MetadataNotFoundException;
import oracle.kv.UnauthorizedException;
import oracle.kv.impl.api.ops.InternalOperation.OpCode;
import oracle.kv.impl.api.ops.InternalOperationHandler.PrivilegedTableAccessor;
import oracle.kv.impl.api.ops.MultiTableOperationHandler.TargetTableAccessChecker;
import oracle.kv.impl.api.table.IndexImpl;
import oracle.kv.impl.security.ExecutionContext;
import oracle.kv.impl.security.KVStorePrivilege;
import oracle.kv.impl.security.SystemPrivilege;
import oracle.kv.impl.security.TablePrivilege;
import oracle.kv.table.Index;
import oracle.kv.table.Table;

/**
 * Base server handler for subclasses of {@link IndexOperation}.
 */
public abstract class IndexOperationHandler<T extends IndexOperation>
        extends InternalOperationHandler<T>
    implements PrivilegedTableAccessor {

    IndexOperationHandler(OperationHandler handler,
                          OpCode opCode,
                          Class<T> operationType) {
        super(handler, opCode, operationType);
    }

    public IndexScanner getIndexScanner(T op,
                                        Transaction txn,
                                        CursorConfig cursorConfig,
                                        LockMode lockMode,
                                        boolean keyOnly) {
        final String tableName = getTableName(op);
        return new IndexScanner(txn,
                                getSecondaryDatabase(op, tableName),
                                getIndex(op, tableName),
                                op.getIndexRange(),
                                op.getResumeSecondaryKey(),
                                op.getResumePrimaryKey(),
                                cursorConfig,
                                lockMode,
                                keyOnly);
    }

    String getTableName(T op) {
        long id = op.getTargetTables().getTargetTableId();
        Table table = getRepNode().getTable(id);
        if (table == null) {
            throw new MetadataNotFoundException
                ("Cannot access table.  It may not exist, id: " + id);
        }
        return table.getFullName();
    }

    IndexImpl getIndex(T op, String tableName) {
        final Index index =
            getRepNode().getIndex(op.getIndexName(), tableName);
        if (index == null) {
            throw new MetadataNotFoundException
                ("Cannot find index " + op.getIndexName() + " in table "
                 + tableName);
        }

        /*
         * Pre-4.2 clients don't know how to serialize for 4.2+ indexes that
         * support null values. If (1) the client is pre-4.2 and (2) the
         * index supports nulls, reserialize the index keys. The IndexOperation
         * can make this decision and modify its IndexRange as needed.
         */
        op.checkReserializeKeys((IndexImpl) index);
        return (IndexImpl) index;
    }

    SecondaryDatabase getSecondaryDatabase(T op, String tableName) {
        final SecondaryDatabase db =
            getRepNode().getIndexDB(op.getIndexName(), tableName);
        if (db == null) {
            throw new MetadataNotFoundException("Cannot find index database: " +
                                                op.getIndexName() + ", " +
                                                tableName);
        }
        return db;
    }

    public void verifyTableAccess(T op)
        throws UnauthorizedException, FaultException {

        if (ExecutionContext.getCurrent() == null) {
            return;
        }

        new TargetTableAccessChecker(operationHandler, this,
                                     op.getTargetTables())
            .checkAccess();
    }

    @Override
    List<? extends KVStorePrivilege> getRequiredPrivileges(T op) {
        /*
         * Checks the basic privilege for authentication here, and leave the
         * the table access checking in {@code verifyTableAccess()}.
         */
        return SystemPrivilege.usrviewPrivList;
    }

    @Override
    public List<? extends KVStorePrivilege>
        tableAccessPrivileges(long tableId) {
        return Collections.singletonList(
            new TablePrivilege.ReadTable(tableId));
    }
}
