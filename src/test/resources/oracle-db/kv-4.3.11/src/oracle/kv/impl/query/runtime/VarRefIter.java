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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import oracle.kv.impl.query.compiler.Expr;
import oracle.kv.impl.query.compiler.QueryFormatter;

/**
 * VarRefIter represents a reference to a non-external variable in the query.
 * It simply returns the value that the variable is currently bound to. This
 * value is computed by the variable's "domain iterator" (the iterator that
 * evaluates the domain expression of the variable).
 *
 * For now, the only kind of variables we
 * have are implicitly created variables ranging over the table references
 * in the FROM clause. Such variables are always bound to the tuples produced
 * by their associated tables.
 *
 * theName:
 * The name of the variable. Used only when displaying the execution plan.
 *
 * theTupleRegs:
 * The registers holding the values of the tuple that this variable is
 * currently bound to, if any. These are the same registers used by the
 * underlying table expression.
 */
public class VarRefIter extends PlanIter {

    private final String theName;

    private final int[] theTupleRegs;

    public VarRefIter(Expr e, int resultReg, int[] tupleRegs, String name) {
        super(e, resultReg);
        theName = name;
        theTupleRegs = tupleRegs;
    }

    /**
     * FastExternalizable constructor.
     */
    VarRefIter(DataInput in, short serialVersion) throws IOException {

        super(in, serialVersion);
        theName = in.readUTF();
        theTupleRegs = deserializeIntArray(in);
    }

    /**
     * FastExternalizable writer.  Must call superclass method first to
     * write common elements.
     */
    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
            throws IOException {

        super.writeFastExternal(out, serialVersion);
        out.writeUTF(theName);
        serializeIntArray(theTupleRegs, out);
    }

    @Override
    public PlanIterKind getKind() {
        return PlanIterKind.VAR_REF;
    }

    @Override
    public int[] getTupleRegs() {
        return theTupleRegs;
    }

    @Override
    public void open(RuntimeControlBlock rcb) {
        rcb.setState(theStatePos, new PlanIterState());
    }

    @Override
    public boolean next(RuntimeControlBlock rcb) {

        PlanIterState state = rcb.getState(theStatePos);

        if (state.isDone()) {
            return false;
        }

        state.done();
        return true;
    }

    @Override
    public void reset(RuntimeControlBlock rcb) {
        PlanIterState state = rcb.getState(theStatePos);
        state.reset(this);
    }

    @Override
    public void close(RuntimeControlBlock rcb) {

        PlanIterState state = rcb.getState(theStatePos);
        if (state == null) {
            return;
        }

        state.close();
    }

    @Override
    protected void display(StringBuilder sb, QueryFormatter formatter) {
        formatter.indent(sb);
        displayContent(sb, formatter);
        displayRegs(sb);
    }

    @Override
    protected void displayContent(StringBuilder sb, QueryFormatter formatter) {
        sb.append("VAR_REF(");
        sb.append(theName);
        sb.append(")");
    }
}
