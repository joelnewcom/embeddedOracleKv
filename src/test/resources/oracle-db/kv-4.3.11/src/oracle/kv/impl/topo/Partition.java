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
import oracle.kv.impl.topo.Topology.Component;

import com.sleepycat.persist.model.Persistent;

/**
 * An Entry in the PartitionMap
 */
@Persistent
public class Partition extends Topology.Component<PartitionId> {

    private static final long serialVersionUID = 1L;
    private RepGroupId repGroupId;

    public Partition(RepGroup repGroup) {
        this(repGroup.getResourceId());
    }

    public Partition(RepGroupId repGroupId) {
        this.repGroupId = repGroupId;
    }

    @SuppressWarnings("unused")
    private Partition() {
    }

    private Partition(Partition partition) {
        super(partition);
        repGroupId = partition.repGroupId;
    }

    /* (non-Javadoc)
     * @see oracle.kv.impl.topo.Topology.Component#getResourceType()
     */
    @Override
    public ResourceType getResourceType() {
        return ResourceType.PARTITION;
    }

    /* (non-Javadoc)
     * @see oracle.kv.impl.topo.Topology.Component#clone()
     */
    @Override
    public Component<?> clone() {
        return new Partition(this);
    }

    /**
     * Returns the RepGroupId associated with the partition
     */
    public RepGroupId getRepGroupId() {
        return repGroupId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result +
            ((repGroupId == null) ? 0 : repGroupId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        if (!super.equals(obj)) {
            return false;
        }

        Partition other = (Partition) obj;
        return propertiesEqual(other);
    }

    /**
     * @return true if the logical portion of the Partitions are equal.
     */
    public boolean propertiesEqual(Partition other) {

        if (repGroupId == null) {
            if (other.repGroupId != null) {
                return false;
            }
        } else if (!repGroupId.equals(other.repGroupId)) {
            return false;
        } 

        return true;
    }

    @Override
    public String toString() {
        return "[" + getResourceId() + "] " +  " shard=" +  repGroupId;
    }
}
