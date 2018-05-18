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

package oracle.kv.impl.util.client;

import java.util.logging.Handler;
import java.util.logging.Logger;

import oracle.kv.impl.util.LogFormatter;

/**
 * Client-only utilities for creating and formatting java.util loggers and
 * handlers.
 */
public class ClientLoggerUtils {

    /**
     * Obtain a logger which sends output to the console.
     */
    public static Logger getLogger(Class<?> cl, String resourceId) {
        Logger logger = Logger.getLogger(cl.getName() + "." + resourceId);
        logger.setUseParentHandlers(false);
        Handler handler = new oracle.kv.util.ConsoleHandler();
        handler.setFormatter(new LogFormatter(null));
        logger.addHandler(handler);

        return logger;
    }
}
