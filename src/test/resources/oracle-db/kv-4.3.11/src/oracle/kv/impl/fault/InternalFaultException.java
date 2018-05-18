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

package oracle.kv.impl.fault;

import java.io.PrintWriter;
import java.io.StringWriter;

import oracle.kv.KVVersion;

/**
 * An exception wrapper used to indicate a fault encountered in a server side
 * process while servicing an internal, non-data operation request.
 * Application-specific subclasses of this class are typically used by the
 * ProcessFaultHandler to throw an exception when processing such a request.
 *
 * <p>
 * Given the distributed nature of the KVS, the client may not have access to
 * the class associated with the "cause" object created by the server, or the
 * class definition may represent a different and potentially incompatible
 * version. This wrapper class ensures that the stack trace and textual
 * information is preserved and communicated to the client.
 */
public abstract class InternalFaultException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String faultClassName;
    private final String originalStackTrace;

    public InternalFaultException(Throwable cause) {
        super(cause.getMessage() + " (" +
              KVVersion.CURRENT_VERSION.getNumericVersionString() + ")");
        /* Preserve the stack trace and the exception class name. */
        final StringWriter sw = new StringWriter(500);
        cause.printStackTrace(new PrintWriter(sw));
        originalStackTrace = sw.toString();

        faultClassName = cause.getClass().getName();
    }

    /* The name of the original exception class, often used for testing. */
    public String getFaultClassName() {
        return faultClassName;
    }

    @Override
    public String toString() {
        return getMessage() + " " + originalStackTrace;
    }
}
