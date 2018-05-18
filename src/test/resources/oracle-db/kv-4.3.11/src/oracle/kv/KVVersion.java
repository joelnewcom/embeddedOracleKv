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

import java.io.InputStream;
import java.io.IOException;
import java.io.Serializable;

import java.util.Properties;

/**
 * Oracle NoSQL DB version information.  Versions consist of major, minor and
 * patch numbers.
 *
 * <p>There is one KVVersion object per running JVM and it may be accessed
 * using the static field {@link #CURRENT_VERSION}.
 */
public class KVVersion implements Comparable<KVVersion>, Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Create a hidden field for each major and minor release.  Update the
     * patch version of the value for each build of the initial major or minor
     * release, but don't include the patch number in the field name, so that
     * they can be used for prerequisite checks in the code.  Don't change the
     * field values for patch releases.
     */

    /** @hidden */
    public static final KVVersion R1_2_123 =
        new KVVersion(11, 2, 1, 2, 123, null);  /* R1.2.123 12/2011 */
    /** @hidden */
    public static final KVVersion R2_0_23 =
        new KVVersion(11, 2, 2, 0, 23, null);   /* R2.0 10/2012 */
    /** @hidden */
    public static final KVVersion R2_1_0 =
        new KVVersion(12, 1, 2, 1, 0, null);    /* Initial R2.1 version */
    /** @hidden */
    public static final KVVersion R2_1 =
        new KVVersion(12, 1, 2, 1, 8, null);    /* R2.1 7/2013 */
    /** @hidden */
    public static final KVVersion R3_0 =
        new KVVersion(12, 1, 3, 0, 5, null);    /* R3 4/2013 */
    /** @hidden */
    public static final KVVersion R3_1 =
        new KVVersion(12, 1, 3, 1, 0, null);    /* R3.1 9/2014 */
    /** @hidden */
    public static final KVVersion R3_2 =
        new KVVersion(12, 1, 3, 2, 0, null);    /* R3.2 11/2014 */
    /** @hidden */
    public static final KVVersion R3_3 =
        new KVVersion(12, 1, 3, 3, 4, null);    /* R3.3 3/2015 */
    /** @hidden */
    public static final KVVersion R3_4 =
        new KVVersion(12, 1, 3, 4, 7, null);    /* R3.4 7/2015 */
    /** @hidden */
    public static final KVVersion R3_5 =
        new KVVersion(12, 1, 3, 5, 2, null);    /* R3.5 11/2015 */
    /** @hidden */
    public static final KVVersion R4_0 =
        new KVVersion(12, 1, 4, 0, 9, null);    /* R4.0 3/2016 */
    /** @hidden */
    public static final KVVersion R4_1 =
        new KVVersion(12, 1, 4, 1, 7, null);    /* R4.1 8/2016 */
    /** @hidden */
    public static final KVVersion R4_2 =
        new KVVersion(12, 1, 4, 2, 1, null);    /* R4.2 ?/2016 */
    /** @hidden */
    public static final KVVersion R4_3 =
        new KVVersion(12, 1, 4, 3, 11, null);    /* R4.3 ?/2016 */

    /**
     * The current software version.
     */
    public static final KVVersion CURRENT_VERSION =
        /*
         * WHEN YOU BUMP THIS VERSION, BE SURE TO BUMP THE VERSIONS IN
         * misc/rpm/*.spec and release-compat.xml.
         */
        R4_3;

   /**
    * The current prerequisite version.  Nodes can only join the cluster if
    * they are running at least this version of the software.
    */
    public static final KVVersion PREREQUISITE_VERSION = R3_0;

    private final int oracleMajor;
    private final int oracleMinor;
    private final int majorNum;
    private final int minorNum;
    private final int patchNum;
    private String releaseId = null;
    private String releaseDate = null;
    private final String name;
    private Properties versionProps;

    public static void main(String argv[]) {
        System.out.println(CURRENT_VERSION);
    }

    public KVVersion(int oracleMajor,
                     int oracleMinor,
                     int majorNum,
                     int minorNum,
                     int patchNum,
                     String name) {
        this.oracleMajor = oracleMajor;
        this.oracleMinor = oracleMinor;
        this.majorNum = majorNum;
        this.minorNum = minorNum;
        this.patchNum = patchNum;
        this.name = name;
    }

    @Override
    public String toString() {
        return getVersionString();
    }

    /**
     * Oracle Major number of the release version.
     *
     * @return The Oracle major number of the release version.
     */
    public int getOracleMajor() {
        return oracleMajor;
    }

    /**
     * Oracle Minor number of the release version.
     *
     * @return The Oracle minor number of the release version.
     */
    public int getOracleMinor() {
        return oracleMinor;
    }

    /**
     * Major number of the release version.
     *
     * @return The major number of the release version.
     */
    public int getMajor() {
        return majorNum;
    }

    /**
     * Minor number of the release version.
     *
     * @return The minor number of the release version.
     */
    public int getMinor() {
        return minorNum;
    }

    /**
     * Patch number of the release version.
     *
     * @return The patch number of the release version.
     */
    public int getPatch() {
        return patchNum;
    }

    public String getReleaseId() {
        initVersionProps();
        return releaseId;
    }

    public String getReleaseDate() {
        initVersionProps();
        return releaseDate;
    }

    private synchronized void initVersionProps() {
        if (versionProps != null) {
            return;
        }

        final InputStream releaseProps =
            KVVersion.class.getResourceAsStream("/version/build.properties");
        if (releaseProps == null) {
            return;
        }

        versionProps = new Properties();
        try {
            versionProps.load(releaseProps);
        } catch (IOException IOE) {
            throw new IllegalStateException(IOE);
        }
        releaseId = versionProps.getProperty("release.id");
        releaseDate = versionProps.getProperty("release.date");
    }

    /**
     * The numeric version string, without the patch tag.
     *
     * @return The release version
     */
    public String getNumericVersionString() {
        StringBuilder version = new StringBuilder();
        version.append(oracleMajor).append(".");
        version.append(oracleMinor).append(".");
        version.append(majorNum).append(".");
        version.append(minorNum).append(".");
        version.append(patchNum);
        return version.toString();
    }

    /**
     * Release version, suitable for display.
     *
     * @return The release version, suitable for display.
     */
    public String getVersionString() {
        initVersionProps();
        StringBuilder version = new StringBuilder();
        version.append(oracleMajor);
        version.append((oracleMajor == 12 ? "cR" : "gR"));
        version.append(oracleMinor).append(".");
        version.append(majorNum).append(".");
        version.append(minorNum).append(".");
        version.append(patchNum);
        if (name != null) {
            version.append(" (");
            version.append(name);
            version.append(")");
        }
        if (releaseId != null) {
            version.append(" ").append(releaseDate).append(" ");
            version.append(" Build id: ").append(releaseId);
        }
        return version.toString();
    }

    /**
     * Returns a KVVersion object representing the specified version string
     * without the release ID, release date,and name parts filled in. This
     * method is basically the inverse of getNumericVersionString(). This
     * method will also parse a full version string (returned from toString())
     * but only the numeric version portion of the string.
     *
     * @param versionString version string to parse
     * @return a KVVersion object
     */
    public static KVVersion parseVersion(String versionString) {

        /*
         * The full verion string will have spaces after the numeric portion of
         * version.
         */
        final String[] tokens = versionString.split(" ");

        /*
         * The full version will have "cR" or "gR" in the numeric portion of the
         * string. So we convert it to a numeric version (replace
         * the "cR"/"gR" with ".").
         */
        final String numericString = tokens[0].replaceAll("[cg]R", ".");

        final String[] numericTokens = numericString.split("\\.");

        if (numericTokens.length != 5) {
            throw new IllegalArgumentException
                ("Invalid version string: " + versionString);
        }

        try {
            return new KVVersion(Integer.parseInt(numericTokens[0]),
                                 Integer.parseInt(numericTokens[1]),
                                 Integer.parseInt(numericTokens[2]),
                                 Integer.parseInt(numericTokens[3]),
                                 Integer.parseInt(numericTokens[4]),
                                 null);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException
                ("Invalid version string: " + versionString, nfe);
        }
    }

    /*
     * Return -1 if the current version is earlier than the comparedVersion.
     * Return 0 if the current version is the same as the comparedVersion.
     * Return 1 if the current version is later than the comparedVersion.
     */
    @Override
    public int compareTo(KVVersion comparedVersion) {
        int result = 0;

        if (oracleMajor == comparedVersion.getOracleMajor()) {
            if (oracleMinor == comparedVersion.getOracleMinor()) {
                if (majorNum == comparedVersion.getMajor()) {
                    if (minorNum == comparedVersion.getMinor()) {
                        if (patchNum > comparedVersion.getPatch()) {
                            result = 1;
                        } else if (patchNum < comparedVersion.getPatch()) {
                            result = -1;
                        }
                    } else if (minorNum > comparedVersion.getMinor()) {
                        result = 1;
                    } else {
                        result = -1;
                    }
                } else if (majorNum > comparedVersion.getMajor()) {
                    result = 1;
                } else {
                    result = -1;
                }
            } else if (oracleMinor > comparedVersion.getOracleMinor()) {
                result = 1;
            } else {
                result = -1;
            }
        } else if (oracleMajor > comparedVersion.getOracleMajor()) {
            result = 1;
        } else {
            result = -1;
        }

        return result;
    }

    /*
     * If its type is KVVersion, and the version numbers are the same,
     * then we consider these two versions equal.
     */
    @Override
    public boolean equals(Object o) {
        return (o instanceof KVVersion) && (compareTo((KVVersion) o) == 0);
    }

    /* Produce a unique hash code for KVVersion. */
    @Override
    public int hashCode() {
        return majorNum * 1000 * 1000 + minorNum * 1000 + patchNum;
    }
}
