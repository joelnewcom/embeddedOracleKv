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

import oracle.kv.impl.topo.ResourceId;
import oracle.kv.impl.topo.ResourceId.ResourceType;
import oracle.kv.impl.topo.Topology.Component;

import com.sleepycat.persist.model.Persistent;

/**
 * Represents the update of an existing component in the topology. The update
 * replaces the previous component associated with the ResourceId.
 */
@Persistent
public class Update extends TopologyChange {

    private static final long serialVersionUID = 1L;

    Component<?> component;

    public Update(int sequenceNumber, Component<?> component) {
        super(sequenceNumber);
        assert component.getResourceId() != null;
        this.component = component;
    }

    @SuppressWarnings("unused")
    private Update() {
    }

    /* (non-Javadoc)
     * @see oracle.kv.impl.topo.change.TopologyChange#getType()
     */
    @Override
    public Type getType() {
        return Type.UPDATE;
    }

    @Override
    public Component<?> getComponent() {
        return component;
    }

    /* (non-Javadoc)
     * @see oracle.kv.impl.topo.change.TopologyChange#getResourceId()
     */
    @Override
    public ResourceId getResourceId() {
        return component.getResourceId();
    }

    @Override
    public Update clone() {
        Component<?> comp = component.clone();
        /* Rep groups have no attributes and cannot change. */
        assert (comp.getResourceId().getType() != ResourceType.REP_GROUP);
        comp.setTopology(null);
        return new Update(sequenceNumber, comp);
    }

    @Override 
    public String toString() {
        return "Update " + component.getResourceId() + " seq=" + 
            sequenceNumber;
    }
}
