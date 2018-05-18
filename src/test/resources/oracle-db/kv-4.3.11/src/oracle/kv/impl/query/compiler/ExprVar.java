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

package oracle.kv.impl.query.compiler;

import oracle.kv.impl.api.table.TableImpl;
import oracle.kv.impl.api.table.FieldDefImpl;
import oracle.kv.impl.query.QueryException;
import oracle.kv.impl.query.QueryStateException;
import oracle.kv.impl.query.types.ExprType;
import oracle.kv.impl.query.types.ExprType.Quantifier;
import oracle.kv.impl.query.types.TypeManager;

/**
 * Represents a variable definition as well as references to that variable.
 *
 * theVarKind:
 * The kind of the variable. For now, we have the following kinds:
 *
 * - FOR: A FOR variable is defined implicitly for each table expression in the
 *   FROM clause of a SFW expression. Such a variable is bound successively to
 *   the rows returned by the associated table expression.
 *
 * - CTX_ITEM : The variable bound to the context item of a path-step expr.
 *
 * - CTX_ELEM : The variable bound to the current array/map item of a slice or
 *   filter step expr.
 *
 * - CTX_ELEM_POS : The variable bound to the position of the current array/map
 *   item of a slice or filter step expr.
 *
 * - EXTERNAL : External variables must be bound by the app before the query
 *   is executed; they act as const global variables.
 *
 * theName:
 * The variable's name.
 *
 * theFromClause:
 * For FOR variables only: the FROM clause that defines the variable.
 *
 * theDomainExpr:
 * The expr the this var ranges over. Is null for external vars and $$element,
 * $$elementPos, and $$key.
 *
 * theCtxExpr:
 * For context variables only: the step expr that defines the variable.
 *
 * theDeclaredType
 * For external vars only: the type appearing in the var declaration.
 *
 * theId:
 * For external vars only: a unique id that serves as a pointer to the array
 * of the variable's values, stored in the RCB.
 */
class ExprVar extends Expr {

    static enum VarKind {
        FOR,
        CTX_ITEM,
        CTX_ELEM,
        CTX_ELEM_POS,
        CTX_KEY,
        EXTERNAL
    }

    static final String theCtxVarName = "$";
    static final String theElementVarName = "$element";
    static final String theElementPosVarName = "$pos";
    static final String theKeyVarName = "$key";
    static final String theValueVarName = "$value";

    private VarKind theVarKind;

    private final String theName;

    private ExprSFW.FromClause theFromClause;

    private Expr theDomainExpr;

    private Expr theCtxExpr;

    private ExprType theDeclaredType;

    private int theId;

    ExprVar(
        QueryControlBlock qcb,
        StaticContext sctx,
        QueryException.Location location,
        String name,
        ExprSFW.FromClause fromClause) {

        super(qcb,sctx, ExprKind.VAR, location);
        theVarKind = VarKind.FOR;
        theName = name;
        theFromClause = fromClause;
        theDomainExpr = fromClause.getDomainExpr();
    }

    ExprVar(
        QueryControlBlock qcb,
        StaticContext sctx,
        QueryException.Location location,
        String name,
        Expr ctxExpr) {

        super(qcb, sctx, ExprKind.VAR, location);
        theName = name;
        theCtxExpr = ctxExpr;

        if (theName.equals(theCtxVarName)) {
            theVarKind = VarKind.CTX_ITEM;
            theDomainExpr = ctxExpr;
        } else if (theName.equals(theElementVarName)) {
            theVarKind = VarKind.CTX_ELEM;
        } else if (theName.equals(theValueVarName)) {
            theVarKind = VarKind.CTX_ELEM;
        } else if (theName.equals(theElementPosVarName)) {
            theVarKind = VarKind.CTX_ELEM_POS;
        } else if (theName.equals(theKeyVarName)) {
            theVarKind = VarKind.CTX_KEY;
        } else {
            theVarKind = VarKind.FOR;
            theDomainExpr = ctxExpr;
        }
    }

    ExprVar(
        QueryControlBlock qcb,
        StaticContext sctx,
        QueryException.Location location,
        String name,
        FieldDefImpl type,
        int id) {

        super(qcb, sctx, ExprKind.VAR, location);
        theName = name;
        theVarKind = VarKind.EXTERNAL;
        theDeclaredType = TypeManager.createType(type, Quantifier.ONE);
        theId = id;
    }

    String getName() {
        return theName;
    }

    int getId() {
        return theId;
    }

    VarKind getVarKind() {
        return theVarKind;
    }

    boolean isFor() {
        return theVarKind == VarKind.FOR;
    }

    boolean isExternal() {
        return theVarKind == VarKind.EXTERNAL;
    }

    boolean isContext() {
        return (theVarKind == VarKind.CTX_ELEM ||
                theVarKind == VarKind.CTX_ELEM_POS ||
                theVarKind == VarKind.CTX_KEY ||
                theVarKind == VarKind.CTX_ITEM);
    }

    Expr getDomainExpr() {
        return theDomainExpr;
    }

    Expr getCtxExpr() {
        return theCtxExpr;
    }

    /*
     * If this is a variable ranging over a base table in the FROM clause
     * return the TableImpl for that table. Otherwise return null.
     */
    TableImpl getTable() {

        if (theVarKind != VarKind.FOR || theFromClause == null) {
            return null;
        }

        if (theDomainExpr.getKind() == ExprKind.BASE_TABLE) {
            ExprBaseTable tableExpr = (ExprBaseTable)theDomainExpr;
            return tableExpr.getTable();
        }

        return null;
    }

    @Override
    int getNumChildren() {
        return 0;
    }

    @Override
    ExprType computeType() {

        switch (theVarKind) {
        case EXTERNAL: {
            return theDeclaredType;
        }
        case CTX_ELEM_POS: {
            return TypeManager.LONG_ONE();
        }
        case CTX_ITEM: {
            ExprKind exprKind = theCtxExpr.getKind();
            Expr input = theCtxExpr.getInput();
            ExprType inType = input.getType();

            if (exprKind == ExprKind.FIELD_STEP ||
                exprKind == ExprKind.MAP_FILTER) {

                while (inType.isArray()) {
                    inType = inType.getArrayElementType(Quantifier.ONE);
                }

                return inType.getItemType();

            } else if (exprKind == ExprKind.ARRAY_SLICE ||
                       exprKind == ExprKind.ARRAY_FILTER) {

                if (!inType.isArray()) {
                    return TypeManager.createArrayType(inType, Quantifier.ONE);
                }
 
                return inType.getItemType();

            } else {
                throw new QueryStateException(
                "Unexpected input expression: " + exprKind);
            }
        }
        case CTX_ELEM: {
            /*
             * Note: we cannot call getType() on theCtxExpr because, during
             * translation, theCtxExpr may not be fully built yet.
             */
            ExprKind exprKind = theCtxExpr.getKind();
            Expr input = theCtxExpr.getInput();
            ExprType inType = input.getType();

            if (exprKind == ExprKind.ARRAY_FILTER) {
                return inType.getArrayElementType(Quantifier.ONE);

            } else if (exprKind == ExprKind.MAP_FILTER) {

                while (inType.isArray()) {
                    inType = inType.getArrayElementType(Quantifier.ONE);
                }

                return inType.getMapElementType(Quantifier.ONE);

            } else {
                throw new QueryStateException(
                    "Unexpected input expression: " + exprKind);
            }

        }
        case CTX_KEY: {
            assert(theCtxExpr.getKind() == ExprKind.MAP_FILTER);
            return TypeManager.STRING_ONE();
        }
        case FOR: {
            return theDomainExpr.getType().getItemType();
        }
        default: {
            throw new QueryStateException(
                "Unknown variable kind: " + theVarKind);
        }
        }
    }

    @Override
    public boolean mayReturnNULL() {
        switch (theVarKind) {
        case EXTERNAL:
            return theDeclaredType.isAnyJson();
        case CTX_ITEM:
        case CTX_ELEM_POS:
        case CTX_KEY:
            return false;
        case CTX_ELEM:
            Expr input = theCtxExpr.getInput();
            ExprType inType = input.getType();
            if (inType.isRecord() ||
                inType.isSubType(TypeManager.JSON_STAR())) {
                return true;
            }
            return false;
        case FOR: {
            return theDomainExpr.mayReturnNULL();
        }
        default: {
            throw new QueryStateException(
                "Unknown variable kind: " + theVarKind);
        }
        }
    }

    @Override
    void display(StringBuilder sb, QueryFormatter formatter) {
        formatter.indent(sb);
        displayContent(sb, formatter);
    }

    @Override
    void displayContent(StringBuilder sb, QueryFormatter formatter) {
        sb.append(theName);
    }
}
