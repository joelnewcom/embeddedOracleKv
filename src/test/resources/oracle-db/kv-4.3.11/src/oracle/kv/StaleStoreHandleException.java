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
 * Thrown when a KVStore instance is no longer valid. The application should
 * call {@link KVStore#close} and obtain a new handle from {@link
 * KVStoreFactory#getStore KVStoreFactory.getStore}.
 */
public class StaleStoreHandleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String rationale;

    /**
     * For internal use only.
     * @hidden
     */
    public StaleStoreHandleException(String rationale) {
        super(rationale);
        this.rationale = rationale;
    }

    @Override
    public String getMessage() {
        return "This KVStore instance is no longer valid and should be " +
            "closed: " + rationale;
    }
}
