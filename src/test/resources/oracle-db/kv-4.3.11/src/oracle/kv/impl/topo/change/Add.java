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
import oracle.kv.impl.topo.Topology.Component;

import com.sleepycat.persist.model.Persistent;

/**
 * Represents the creation of a new component in the topology.
 */
@Persistent
public class Add extends TopologyChange {

    private static final long serialVersionUID = 1L;

    Component<?> component;

    Add(int sequenceNumber, Component<?> component) {
        super(sequenceNumber);
        assert component.getResourceId() != null;
        this.component = component;
    }

    @SuppressWarnings("unused")
    private Add() { super();}

    /* (non-Javadoc)
     * @see oracle.kv.impl.topo.change.TopologyChange#getType()
     */
    @Override
    public Type getType() {
        return Type.ADD;
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
    public Add clone() {
        Component<?> comp = component.clone();
        comp.setTopology(null);
        return new Add(sequenceNumber, comp);
    }

    @Override 
    public String toString() {
        return "Add " + component.getResourceId() + " seq=" + 
            sequenceNumber;
    }
}
