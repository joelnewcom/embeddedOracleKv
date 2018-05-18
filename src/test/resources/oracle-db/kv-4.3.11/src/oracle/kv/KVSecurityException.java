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
 * The common abstract base class of KVStore security-related exceptions. These
 * exceptions are produced only when accessing a secure KVStore instance.
 *
 * @since 3.0
 */
public abstract class KVSecurityException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * For internal use only.
     * @hidden
     */
    protected KVSecurityException(String msg) {
        super(msg);
    }

    /**
     * For internal use only.
     * @hidden
     */
    protected KVSecurityException(Throwable cause) {
        super(cause);
    }
}
