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

/**
 * A wrapper exception used to wrap known serializable java runtime exceptions
 * that the client is guaranteed to have on its classpath back to the client.
 * The exception is wrapped as its cause. The RequestDispatcher at the client
 * unwraps the <i>cause</i> and throws the wrapped exception. Note that this
 * type of wrapping is different from the wrapping done via a FaultException
 * which is passed "as is" back to the client.
 *
 * The prototypical example motivating this type of wrapping is an
 * IllegalArgumentException in instances where the check can only be performed
 * remotely on the RN and not locally at the client.
 */
public class WrappedClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public WrappedClientException(RuntimeException wrappedException) {
        super(wrappedException);
    }
}
