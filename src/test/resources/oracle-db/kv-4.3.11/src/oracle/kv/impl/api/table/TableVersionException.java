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

package oracle.kv.impl.api.table;

/**
 * Exception to indicate that the table version of a serialized row is higher
 * than that available on the client or server.  It must be caught by internal
 * code to trigger a fetch of new metadata from the client.  In the case of the
 * server is needs to trigger an exception that will cause the client to retry
 * the operation, giving the server a chance to update its metadata.
 */
public class TableVersionException extends RuntimeException {

    private final int requiredVersion;

    private static final long serialVersionUID = 1L;

    TableVersionException(int requiredVersion) {
        this.requiredVersion = requiredVersion;
    }

    public int getRequiredVersion() {
        return requiredVersion;
    }
}
