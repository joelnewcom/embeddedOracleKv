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
 * Removes an existing component from the Topology
 */
@Persistent
public class Remove extends TopologyChange {

    private static final long serialVersionUID = 1L;
    private ResourceId resourceId;

    /**
     * @param sequenceNumber
     */
    public Remove(int sequenceNumber,
                  ResourceId resourceId) {
        super(sequenceNumber);

        assert resourceId != null;

        this.resourceId = resourceId;
    }

    @SuppressWarnings("unused")
    private Remove() {
        super();
    }

    /* (non-Javadoc)
     * @see oracle.kv.impl.topo.change.TopologyChange#getType()
     */
    @Override
    public Type getType() {
        return Type.REMOVE;
    }

    @Override
    public ResourceId getResourceId() {
        return resourceId;
    }

    /* (non-Javadoc)
     * @see oracle.kv.impl.topo.change.TopologyChange#getComponent()
     */
    @Override
    public Component<?> getComponent() {
        return null;
    }

    @Override
    public Remove clone() {
        return new Remove(sequenceNumber, resourceId);
    }
}
