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

package oracle.kv.impl.api;

import java.io.Serializable;

import oracle.kv.impl.topo.RepNodeId;

import com.sleepycat.je.rep.ReplicatedEnvironment;

/**
 * The status updates that are returned as part of a response.
 *
 * The status update information is expected to be a small number of bytes
 * in the typical case and cheap to compute so that it does not add
 * significantly to the overhead of a request/response.
 */
public class StatusChanges implements Serializable {

    private static final long serialVersionUID = 1L;

    /* The state at the *responding node* as contained in the Response */
    private final ReplicatedEnvironment.State state;

    /* The masterId, it's non-null if the node is currently active, that is
     * it's a master or replica.
     */
    private final RepNodeId masterId;

    /*
     * Note that we are using time order events. Revisit if this turns out to
     * be an issue and we should be using heavier weight group-wise sequenced
     * election proposal numbers instead.
     */
    private final long statusTime;

    public StatusChanges(ReplicatedEnvironment.State state,
                         RepNodeId masterId,
                         long sequenceNum) {
        super();
        this.state = state;
        this.masterId = masterId;
        this.statusTime = sequenceNum;
    }

    public ReplicatedEnvironment.State getState() {
        return state;
    }

    /**
     * Returns the master RN as known to the responding RN
     *
     * @return the unique id associated with the master RN
     */
    public RepNodeId getCurrentMaster() {
        return masterId;
    }

    /**
     * Returns the time associated with the status transition at the responding
     * node.
     */
    public long getStatusTime() {
        return statusTime;
    }
}
