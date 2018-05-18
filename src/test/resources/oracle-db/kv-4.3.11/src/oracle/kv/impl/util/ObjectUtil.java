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

package oracle.kv.impl.util;

/** Miscellaneous utilities for {@link Object} classes. */
public class ObjectUtil {

    /** Prevent instantiation. */
    private ObjectUtil() { }

    /**
     * Checks that a variable is not {@code null}, throwing {@link
     * IllegalArgumentException} if it is.  The exception message includes the
     * name of the variable.
     *
     * @param variableName the name of the variable being checked
     * @param value the value of the variable to check
     */
    public static void checkNull(final String variableName,
                                 final Object value) {
        if (value == null) {
            throw new IllegalArgumentException(
                "The value of " + variableName + " must not be null");
        }
    }
}
