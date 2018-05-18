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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.LogManager;

/**
 * Logger utilities common to both client and server side.
 */
public class CommonLoggerUtils {

    /**
     * Get the value of a specified Logger property.
     */
    public static String getLoggerProperty(String property) {
        LogManager mgr = LogManager.getLogManager();
        return mgr.getProperty(property);
    }

    /**
     * Utility method to return a String version of a stack trace
     */
    public static String getStackTrace(Throwable t) {
        if (t == null) {
            return "";
        }

        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();
        stackTrace = stackTrace.replaceAll("&lt", "<");
        stackTrace = stackTrace.replaceAll("&gt", ">");

        return stackTrace;
    }
}
