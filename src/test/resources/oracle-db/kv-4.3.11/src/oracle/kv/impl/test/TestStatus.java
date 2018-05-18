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

package oracle.kv.impl.test;

/**
 * Used to determine whether the code is operating in a test environment. It's
 * the responsibility of the individual test to set the state. In unit tests,
 * this is typically done in the setUp method.
 */
public class TestStatus {

    private static boolean isActive = false;
    private static boolean isWriteNoSyncAllowed = false;
    private static boolean manyRNs = false;

    /**
     * Indicates whether the code is running in a test environment. Must be
     * set explicitly by a test.
     */
    public static void setActive(boolean isActive) {
        TestStatus.isActive = isActive;
    }

    /**
     * Return true if the stats was set to be active via setActive() to
     * indicate that we are running in a test environment.
     */
    public static boolean isActive() {
        return isActive;
    }

    /**
     * Whether write-no-sync durability is allowed in unit tests.  This should
     * only be set by tests, never in production mode.
     */
    public static void setWriteNoSyncAllowed(boolean isWriteNoSyncAllowed) {
        TestStatus.isWriteNoSyncAllowed = isWriteNoSyncAllowed;
    }

    /**
     * If false, the production durability level should be used.  If true, we
     * have the option of reducing sync durability to write-no-sync in cases
     * where this seems appropriate, in order to speed up unit tests.
     */
    public static boolean isWriteNoSyncAllowed() {
        return isWriteNoSyncAllowed;
    }

    /**
     * Indicates whether the test will instantiate many RNs on a single machine,
     * thereby in danger of running out of swap and heap when the default
     * sizings are used.
     */
    public static void setManyRNs(boolean many) {
        TestStatus.manyRNs = many;
    }

    public static boolean manyRNs() {
        return manyRNs;
    }
}
