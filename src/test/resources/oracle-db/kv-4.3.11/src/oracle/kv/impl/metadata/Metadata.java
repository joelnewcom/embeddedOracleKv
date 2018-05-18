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

package oracle.kv.impl.metadata;

/**
 * Interface implemented by all metadata objects.
 *
 * @param <I> the type of metadata information object return by this metadata
 */
public interface Metadata<I extends MetadataInfo> {

    /** Metadata types */
    public enum MetadataType {
        /* New types must be added to the end of this list */
        TOPOLOGY() {
            @Override
            public String getKey() { return "Topology";}
        },
        TABLE() {
            @Override
            public String getKey() { return "Table";}
        },
        SECURITY() {
            @Override
            public String getKey() { return "Security";}
        };

        /**
         * Gets a unique string for this type. This string may be used as a
         * key into a metadata store.
         *
         * @return a unique string
         */
        abstract public String getKey();
    }

    /**
     * The sequence number of an newly created metadata, empty object.
     */
    public static final int EMPTY_SEQUENCE_NUMBER = 0;

    /**
     * A value indicating that a sequence number is not known. Note that
     * UNKNOWN_SEQ_NUM < EMPTY_SEQUENCE_NUMBER < the first sequence number
     */
    public static final int UNKNOWN_SEQ_NUM = -1;
    
    /**
     * Gets the type of this metadata object.
     *
     * @return the type of this metadata object
     */
    public MetadataType getType();

    /**
     * Gets the highest sequence number of this metadata object.
     * Returns Metadata.EMPTY_SEQUENCE_NUMBER if the metadata has not been
     * initialized.
     *
     * @return the highest sequence number of this metadata object
     */
    public int getSequenceNumber();

    /**
     * Gets an information object for this metadata. The returned object will
     * include the changes between this object and the metadata at the
     * specified sequence number. If the metadata object can not supply
     * information based on the sequence number an empty metadata information
     * object is returned.
     *
     * @param startSeqNum the inclusive start of the sequence of
     * changes to be included
     * @return a metadata info object
     */
    public I getChangeInfo(int startSeqNum);
}
