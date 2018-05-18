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

import oracle.kv.impl.api.table.RecordDefImpl;
import oracle.kv.impl.api.table.RecordValueImpl;

import static oracle.kv.impl.query.QueryException.Location;
import oracle.kv.table.FieldRange;
import oracle.kv.Direction;

/**
 * An interface to abstract client and server side implementations of
 * BaseTableIter. The client side implementation exists primarily for
 * debuggability.
 *
 * The compiler creates an instance T of BaseTableIter. During the open() method
 * on T, TableIterFactory.createTableIter() is called to create an instance TC
 * of either ClientTableIter or ServerTableIter. A ref to TC is stored in T, and
 * the next(), reset(), and close() methods on T are propagated to TC.
 *
 * TBD: should this just pass BaseTableIter and have the constructors
 * get state directly via accessor methods?
 */
public interface TableIterFactory {
    public PlanIter createTableIter(
        RuntimeControlBlock rcb,
        Location loc,
        int statePos,
        int resultReg,
        int[] tupleRegs,
        String tableName,
        String indexName,
        RecordDefImpl typeDefinition,
        Direction dir,
        RecordValueImpl primKeyRecord,
        RecordValueImpl secKeyRecord,
        FieldRange range,
        PlanIter filterIter,
        boolean usesCoveringIndex,
        boolean eliminateIndexDups,
        PlanIter[] pushedExternals);
}
