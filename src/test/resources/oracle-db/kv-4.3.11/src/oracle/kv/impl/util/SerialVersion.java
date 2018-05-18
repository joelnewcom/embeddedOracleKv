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

import java.util.HashMap;
import java.util.Map;

import oracle.kv.KVVersion;

/**
 * Defines the previous and current serialization version for services and
 * clients.
 *
 * <p>As features that affect serialized formats are introduced constants
 * representing those features should be added here, associated with the
 * versions in which they are introduced. This creates a centralized location
 * for finding associations of features, serial versions, and release
 * versions. Existing constants (as of release 4.0) are spread throughout the
 * source and can be moved here as time permits.
 *
 * @see oracle.kv.impl.util.registry.VersionedRemote
 */
public class SerialVersion {

    public static final short UNKNOWN = -1;

    private static final Map<Short, KVVersion> kvVersions = new HashMap<>();

    /* R1 version */
    public static final short V1 = 1;
    static { init(V1, KVVersion.R1_2_123); }

    /* Introduced at R2 (2.0.23) */
    public static final short V2 = 2;
    static { init(V2, KVVersion.R2_0_23); }

    /* Introduced at R2.1 (2.1.8) */
    public static final short V3 = 3;
    static { init(V3, KVVersion.R2_1); }

    /*
     * Introduced at R3.0 (3.0.5)
     *  - secondary datacenters
     *  - table API
     */
    public static final short V4 = 4;
    static { init(V4, KVVersion.R3_0); }

    public static final short TABLE_API_VERSION = V4;

    /* Introduced at R3.1 (3.1.0) for role-based authorization */
    public static final short V5 = 5;
    static { init(V5, KVVersion.R3_1); }

    /*
     * Introduced at R3.2 (3.2.0):
     * - real-time session update
     * - index key iteration
     */
    public static final short V6 = 6;
    static { init(V6, KVVersion.R3_2); }

    public static final short RESULT_INDEX_ITERATE_VERSION = V6;

    /*
     * Introduced at R3.3 (3.3.0) for secondary Admin type and JSON flag to
     * verifyConfiguration, and password expiration.
     */
    public static final short V7 = 7;
    static { init(V7, KVVersion.R3_2); }

    /*
     * Introduced at R3.4 (3.4.0) for the added replica threshold parameter on
     * plan methods, and the CommandService.getAdminStatus,
     * repairAdminQuorum, and createFailoverPlan methods.
     * Also added MetadataNotFoundException.
     *
     * Added bulk get APIs to Key/Value and Table interface.
     */
    public static final short V8 = 8;
    static { init(V8, KVVersion.R3_4); }

    public static final short BATCH_GET_VERSION = V8;

    /*
     * Introduced at R3.5 (3.5.0) for Admin automation V1 features, including
     * json format output, error code, and Kerberos authentication.
     *
     * Added bulk put APIs to Key/Value and Table interface.
     */
    public static final short V9 = 9;
    static { init(V9, KVVersion.R3_5); }

    public static final short BATCH_PUT_VERSION = V9;

    /*
     * Introduced at R4.0/V10:
     * - new query protocol operations. These were added in V10, but because
     *   they were "preview" there is no attempt to handle V10 queries in
     *   releases > V10. Because the serialization format of queries has changed
     *   such operations will fail with an appropriate message.
     * - time to live
     * - Arbiters
     * - Full text search
     */
    public static final short V10 = 10;
    static { init(V10, KVVersion.R4_0); }

    public static final short TTL_SERIAL_VERSION = V10;

    /*
     * Introduced at R4.1/V11
     * - SN/topology contraction
     * - query protocol change (not compatible with V10)
     * - new SNA API for mount point sizes
     */
    public static final short V11 = 11;
    static { init(V11, KVVersion.R4_1); }

    public static final short QUERY_VERSION = V11;

    /*
     * Introduced at R4.2/V12
     * - query protocol change (compatible with V11)
     * - getStorageNodeInfo added to SNA
     */
    public static final short V12 = 12;
    static { init(V12, KVVersion.R4_2); }

    public static final short QUERY_VERSION_2 = V12;

    /*
     * Introduced at R4.3/V13
     * - new SNI API for checking parameters
     */
    public static final short V13 = 13;
    static { init(V13, KVVersion.R4_3); }

     /* query protocol change (compatible with V11) */
    public static final short QUERY_VERSION_3 = V13;

    /*
     * When adding a new version and updating CURRENT, be sure to make
     * corresponding changes in KVVersion as well as the files referenced
     * from there to add a new release version.
     */
    public static final short CURRENT = V13;

    private static void init(short version, KVVersion kvVersion) {
        kvVersions.put(version, kvVersion);
    }

    public static KVVersion getKVVersion(short serialVersion) {
        return kvVersions.get(serialVersion);
    }
}
