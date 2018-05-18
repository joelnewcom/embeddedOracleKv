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

import java.util.Set;

/**
 * Thrown when write operations cannot be initiated because a quorum of
 * Replicas as determined by the {@link Durability.ReplicaAckPolicy} was not
 * available.
 *
 * <p>The likelihood of this exception being thrown depends on the number of
 * nodes per replication group, the rate of node failures and how quickly a
 * failed node is restored to operation, and the specified {@code
 * ReplicaAckPolicy}.  The {@code ReplicaAckPolicy} for the default durability
 * policy (specified by {@link KVStoreConfig#getDurability}) is {@link
 * Durability.ReplicaAckPolicy#SIMPLE_MAJORITY}.  With {@code SIMPLE_MAJORITY},
 * this exception is thrown only when the majority of nodes in a replication
 * group are unavailable, and in a well-maintained KVStore system with at least
 * three nodes per replication group this exception should rarely be
 * thrown.</p>
 *
 * <p>If the client overrides the default and specifies {@link
 * Durability.ReplicaAckPolicy#ALL}, then this exception will be thrown when
 * any node in a replication group is unavailable; in other words, it is much
 * more likely to be thrown.  If the client specifies {@link
 * Durability.ReplicaAckPolicy#NONE}, then this exception will never be
 * thrown.</p>
 * 
 * <p>When this exception is thrown the KVStore service will perform
 * administrative notifications so that actions can be taken to correct the
 * problem.  Depending on the nature of the application, the client may wish to
 * <ul>
 * <li>retry the write operation immediately,</li>
 * <li>fall back to a read-only mode and resume write operations at a later
 * time, or</li>
 * <li>give up and report an error at a higher level.</li>
 * </ul>
 * </p>
 */
public class DurabilityException extends FaultException {

    private static final long serialVersionUID = 1L;

    private final Durability.ReplicaAckPolicy ackPolicy;
    private final int requiredNodeCount;
    private final Set<String> availableReplicas;

    /**
     * For internal use only.
     * @hidden
     */
    public DurabilityException(Throwable cause,
                               Durability.ReplicaAckPolicy ackPolicy,
                               int requiredNodeCount,
                               Set<String> availableReplicas) {
        super(cause, true /*isRemote*/);
        this.ackPolicy = ackPolicy;
        this.requiredNodeCount = requiredNodeCount;
        this.availableReplicas = availableReplicas;
    }

    /**
     * Returns the Replica ack policy that was in effect for the operation.
     */
    public Durability.ReplicaAckPolicy getCommitPolicy() {
        return ackPolicy;
    }

    /**
     * Returns the number of nodes that were required to be active in order to
     * satisfy the Replica ack policy associated with the operation.
     */
    public int getRequiredNodeCount() {
        return requiredNodeCount;
    }

    /**
     * Returns the set of Replicas that were available at the time of the
     * operation.
     */
    public Set<String> getAvailableReplicas() {
        return availableReplicas;
    }
}
