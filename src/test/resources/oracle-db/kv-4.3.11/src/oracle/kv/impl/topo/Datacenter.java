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

package oracle.kv.impl.topo;

import static oracle.kv.impl.util.ObjectUtil.checkNull;

import oracle.kv.impl.topo.ResourceId.ResourceType;

import com.sleepycat.je.rep.NodeType;
import com.sleepycat.persist.model.Persistent;

/**
 * The Datacenter topology component.
 *
 * version 0: original
 * version 1: added repFactor field
 * version 2: added allowArbiters field
 */
@Persistent(version=2)
public class Datacenter extends Topology.Component<DatacenterId> {

    private static final long serialVersionUID = 1L;

    /** Data centers with version=1 are of type PRIMARY by default. */
    private static final DatacenterType DEFAULT_DATACENTER_TYPE =
        DatacenterType.PRIMARY;

    private String name;
    private int repFactor;
    private boolean allowArbiters;

    /** Creates a new Datacenter. */
    public static Datacenter newInstance(final String name,
                                         final int repFactor,
                                         final DatacenterType datacenterType,
                                         final boolean allowArbiters) {

        checkNull("datacenterType", datacenterType);

        switch (datacenterType) {
        case PRIMARY:

            /*
             * Create an instance of the original Datacenter type, to maintain
             * compatibility as needed during an upgrade.
             */
            return new Datacenter(name, repFactor, allowArbiters);

        case SECONDARY:
            return new DatacenterV2(name, repFactor,
                                    datacenterType, allowArbiters);
        default:
            throw new AssertionError();
        }
    }

    private Datacenter(String name, int repFactor, boolean allowArbiters) {
        this.name = name;
        this.repFactor = repFactor;
        this.allowArbiters = allowArbiters;
        int minRepFactor = 1;
        if (allowArbiters) {
            minRepFactor = 0;
        }
        if (repFactor < minRepFactor) {
            throw new IllegalArgumentException(
                "Replication factor must be greater than or equal to " +
                minRepFactor);
        }
    }

    private Datacenter(Datacenter datacenter) {
        super(datacenter);
        name = datacenter.name;
        repFactor = datacenter.repFactor;
    }

    private Datacenter() {
    }

    /* (non-Javadoc)
     * @see oracle.kv.impl.topo.Topology.Component#getResourceType()
     */
    @Override
    public ResourceType getResourceType() {
        return ResourceType.DATACENTER;
    }

    /* Returns the name associated with the Datacenter. */
    public String getName() {
        return name;
    }

    public int getRepFactor() {
        return repFactor;
    }

    /* repfactor is excluded from the hash code because it's mutable. */
    public void setRepFactor(int factor) {
        repFactor = factor;
    }

    /**
     * Returns the type of the data center.
     */
    public DatacenterType getDatacenterType() {
        return DEFAULT_DATACENTER_TYPE;
    }

    public boolean getAllowArbiters() {
        return allowArbiters;
    }


    /* (non-Javadoc)
     * @see oracle.kv.impl.topo.Topology.Component#clone()
     */
    @Override
    public Datacenter clone() {
        return new Datacenter(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + getDatacenterType().hashCode();
        result = prime * result + getRepFactor();
        result = prime * result + (getAllowArbiters() ? 0 : 1);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof Datacenter)) {
            return false;
        }
        final Datacenter other = (Datacenter) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        if (repFactor == other.repFactor &&
            getDatacenterType().equals(other.getDatacenterType()) &&
            allowArbiters == other.allowArbiters) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("id=" + getResourceId() + " name=" + name +
                  " repFactor=" + repFactor + " type=" + getDatacenterType() +
                  " allowArbiters=" + getAllowArbiters());
        return sb.toString();
    }

    /**
     * A nested class of Datacenter that contains methods that should only be
     * called on the server side.  This class is not included in the
     * client-side JAR file, which also means that the JE NodeType class is not
     * needed there.
     */
    public static class ServerUtil {

        /**
         * Returns the default rep node type for a datacenter.
         *
         * <p>Under normal circumstances, all of the RNs in a datacenter will
         * use this value as their node type.  Administrators can override the
         * node type to temporarily change the node type for a set of RNs as
         * part of disaster recovery procedures.  [#23447]
         *
         * @param dc the datacenter
         * @return the default rep node type
         */
        public static NodeType getDefaultRepNodeType(final Datacenter dc) {
            return getDefaultRepNodeType(dc.getDatacenterType());
        }

        /**
         * Returns the default rep node type for the specified datacenter type.
         *
         * @param type the datacenter type
         * @return the default rep node type
         */
        public static NodeType getDefaultRepNodeType(
            final DatacenterType type) {

            switch (type) {
            case PRIMARY:
                return NodeType.ELECTABLE;
            case SECONDARY:
                return NodeType.SECONDARY;
            default:
                throw new AssertionError();
            }
        }
    }

    /**
     * Define a subclass of Datacenter for instances with a non-default value
     * for the DatacenterType.
     */
    @Persistent
    private static class DatacenterV2 extends Datacenter {
        private static final long serialVersionUID = 1L;
        private DatacenterType datacenterType;

        DatacenterV2(final String name,
                     final int repFactor,
                     final DatacenterType datacenterType,
                     final boolean allowArbiters) {
            super(name, repFactor, allowArbiters);
            checkNull("datacenterType", datacenterType);
            this.datacenterType = datacenterType;
        }

        private DatacenterV2(final DatacenterV2 datacenter) {
            super(datacenter);
            datacenterType = datacenter.datacenterType;
        }

        /** For DPL */
        @SuppressWarnings("unused")
        private DatacenterV2() { }

        @Override
        public DatacenterType getDatacenterType() {
            return datacenterType;
        }

        /* (non-Javadoc)
         * @see oracle.kv.impl.topo.Topology.Component#clone()
         */
        @Override
        public DatacenterV2 clone() {
            return new DatacenterV2(this);
        }
    }
}
