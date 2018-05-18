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
 * This exception is thrown from methods where an authenticated user is 
 * attempting to perform an operation for which they are not authorized.  An
 * application that receives this exception typically should not retry the
 * operation.
 * <p>
 * Note that in this release, this exception should not occur through legitimate
 * use of the KVStore API.  Future release may introduce additional
 * authorization capabilities that may cause this exception to be encountered.
 *
 * @since 3.0
 */
public class UnauthorizedException extends KVSecurityException {

    private static final long serialVersionUID = 1L;

    /**
     * For internal use only.
     * @hidden
     */
    public UnauthorizedException(String msg) {
        super(msg);
    }

    /**
     * For internal use only.
     * @hidden
     */
    public UnauthorizedException(Throwable cause) {
        super(cause);
    }
}
