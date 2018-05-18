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

package oracle.kv;

/**
 * The exception is thrown when an expected piece of metadata is not found.
 * This probably indicates an inconsistency between the client's metadata and
 * that on the server side.  This can happen if, for example, a table is
 * dropped then re-created using the same name.  In this case a client's
 * {@link oracle.kv.table.Table} instance may still represent the old table and it must be
 * refreshed using {@link oracle.kv.table.TableAPI#getTable}.
 *
 * @since 3.4
 */
public class MetadataNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * For internal use only.
     * @hidden
     */
    public MetadataNotFoundException(String msg) {
        super(msg);
    }
}
