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

package oracle.kv.impl.topo.change;

import java.io.Serializable;

import oracle.kv.impl.topo.ResourceId;
import oracle.kv.impl.topo.Topology;
import oracle.kv.impl.topo.Topology.Component;

import com.sleepycat.persist.model.Persistent;

/**
 * The Base class for all topology changes. A sequence of changes can be
 * applied to a Topology instance, via {@link Topology#apply} to make it more
 * current.
 * <p>
 * Each TopologyChange represents a logical change entry in a logical log with
 * changes being applied in sequence via {@link Topology#apply} to modify the
 * topology and bring it up to date.
 */
@Persistent
public abstract class TopologyChange implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    public enum Type {ADD, UPDATE, REMOVE}

    int sequenceNumber;

    TopologyChange(int sequenceNumber) {
        super();
        this.sequenceNumber = sequenceNumber;
    }

    protected TopologyChange() {
    }

    public abstract Type getType();

    /**
     * Identifies the resource being changed
     */
    public abstract ResourceId getResourceId();

    /**
     * Returns the impacted component, or null if one is not available.
     */
    public abstract Component<?> getComponent();

    @Override
    public abstract TopologyChange clone();

    /**
     * Returns The sequence number associated with this change.
     */
    public int getSequenceNumber() {
        return sequenceNumber;
    }
    
    @Override
    public String toString() {
        return "seq=" + sequenceNumber + "/" + getType() + " " + 
            getResourceId() + "/" + getComponent();
    }
}
