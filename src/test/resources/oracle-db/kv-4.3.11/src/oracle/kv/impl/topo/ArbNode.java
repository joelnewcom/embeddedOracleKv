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

import oracle.kv.impl.topo.ResourceId.ResourceType;

import com.sleepycat.persist.model.Persistent;

/**
 * The AN topology component.
 */
@Persistent
public class ArbNode extends Topology.Component<ArbNodeId>
    implements Comparable<ArbNode> {

    private static final long serialVersionUID = 1L;

    private final StorageNodeId storageNodeId;

    public ArbNode(StorageNodeId storageNodeId) {

        this.storageNodeId = storageNodeId;
    }

    private ArbNode(ArbNode arbNode) {
        super(arbNode);

        storageNodeId = arbNode.storageNodeId;
    }

    /**
     * Empty constructor to satisfy DPL. Though this class is never persisted,
     * because it is referenced from existing persistent classes it must be
     * annotated and define an empty constructor.
     */
    @SuppressWarnings("unused")
    private ArbNode() {
        throw new IllegalStateException("Should not be invoked");
    }

    /* (non-Javadoc)
     * @see oracle.kv.impl.topo.Topology.Component#getResourceType()
     */
    @Override
    public ResourceType getResourceType() {
        return ResourceType.ARB_NODE;
    }

    @Override
    public int hashCode() {
        final int prime = 37;
        int result = super.hashCode();
        result = prime * result +
            ((storageNodeId == null) ? 0 : storageNodeId.hashCode());
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        ArbNode other = (ArbNode) obj;
        return propertiesEquals(other);
    }

    public boolean propertiesEquals(ArbNode other) {

        if (storageNodeId == null) {
            if (other.storageNodeId != null) {
                return false;
            }
        } else if (!storageNodeId.equals(other.storageNodeId)) {
            return false;
        }
        return true;
    }


    /**
     * Returns the replication group id associated with the AN.
     */
    public RepGroupId getRepGroupId() {
        return new RepGroupId(getResourceId().getGroupId());
    }

    /**
     * Returns the StorageNodeId of the SN hosting this AN
     */
    @Override
    public StorageNodeId getStorageNodeId() {
        return storageNodeId;
    }

    /* (non-Javadoc)
     * @see oracle.kv.impl.topo.Topology.Component#clone()
     */
    @Override
    public ArbNode clone() {
        return new ArbNode(this);
    }

    @Override
    public boolean isMonitorEnabled() {
        return true;
    }

    @Override
    public String toString() {
        return "[" + getResourceId() + "]" + " sn=" + storageNodeId;
    }

    @Override
    public int compareTo(ArbNode other) {
        return getResourceId().compareTo
            (other.getResourceId());
    }
}
