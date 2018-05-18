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

package oracle.kv.impl.admin.plan;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import oracle.kv.impl.admin.IllegalCommandException;
import oracle.kv.impl.admin.PlanLocksHeldException;
import oracle.kv.impl.api.table.TableMetadata;
import oracle.kv.impl.metadata.Metadata;
import oracle.kv.impl.metadata.Metadata.MetadataType;
import oracle.kv.impl.metadata.MetadataInfo;
import oracle.kv.impl.security.metadata.SecurityMetadata;

/**
 * Base class for plans which operate on multiple metadata types.
 *
 * When a MultiMetadataPlan is constructed, the metadata sequence numbers of
 * the metadata objects are saved so that a check can be made when the plan
 * runs, making sure the metadata has not changed out from under the plan.
 * Note that, if the plan itself changes any metadata, it should track the
 * changes using Plan.updatingMetadata().
 */
public abstract class MultiMetadataPlan extends AbstractPlan {

    private static final long serialVersionUID = 1L;

    protected static final Set<MetadataType> TABLE_SECURITY_TYPES =
        Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList(MetadataType.TABLE, MetadataType.SECURITY)));

    /*
     * The sequence numbers of the metadata objects for the plan when it was
     * created. A plan should check if MD sequence numbers have changed when it
     * executes. Also if a plan makes multiple changes to MD over the course of
     * execution it should update the basis so that the plan can be restarted.
     */
    private final Map<MetadataType, Integer> bases = new HashMap<>();

    protected MultiMetadataPlan(AtomicInteger idGen,
                                String planName,
                                Planner planner) {
        super(idGen, planName, planner);

        for (MetadataType mdType : getMetadataTypes()) {
            final Metadata<? extends MetadataInfo> md = getMetadata(mdType);
            bases.put(mdType, (md == null) ? Metadata.EMPTY_SEQUENCE_NUMBER :
                               md.getSequenceNumber());
        }
    }

    /**
     * Returns the metadata types used by this plan.
     *
     * @return the metadata types
     */
    protected abstract Set<MetadataType> getMetadataTypes();

    /* Simpler methods to get table and security metadata */
    public TableMetadata getTableMetadata() {
        return (TableMetadata) getMetadata(MetadataType.TABLE);
    }

    public SecurityMetadata getSecurityMetadata() {
        return (SecurityMetadata) getMetadata(MetadataType.SECURITY);
    }

    /**
     * Get metadata with given metadata type. Only table and security metadata
     * can be loaded using this method.
     *
     * @param type
     * @return metadata of given type
     */
    private Metadata<? extends MetadataInfo> getMetadata(MetadataType type) {
        switch (type) {
        case TABLE:
            return getAdmin().getMetadata(TableMetadata.class, type);
        case SECURITY:
            return getAdmin().getMetadata(SecurityMetadata.class, type);
        case TOPOLOGY:
            return getAdmin().getCurrentTopology();
        default:
            throw new IllegalArgumentException(
                "Unknown metadata type: " + type);
        }
    }

    /**
     * Check the current sequence number for each metadata object with the
     * basis values recorded for this plan.  If they do not match an
     * IllegalArgumentException is thrown.
     */
    @Override
    public void preExecuteCheck(boolean force, Logger executeLogger) {

        for (MetadataType mdType : bases.keySet()) {
            final Metadata<? extends MetadataInfo> md = getMetadata(mdType);
            final int seqNum = (md == null) ? Metadata.EMPTY_SEQUENCE_NUMBER :
                md.getSequenceNumber();
            checkSequenceNumBasis(seqNum, mdType);
        }
    }

    @Override
    public void preExecutionSave() {
        /* Nothing to save before execution. */
    }

    @Override
    public String getDefaultName() {
        return "Multiple Metadata Plan";
    }

    @Override
    public boolean isExclusive() {
      return false;
    }

    @Override
    public void getCatalogLocks() throws PlanLocksHeldException {
        planner.lockElasticity(getId(), getName());
        getPerTaskLocks();
    }

    /**
     * Updates the basis for this plan if the specified metadata is one of
     * metadata types this plan uses and the metadata's sequence number is
     * greater than the basis. If the basis is updated, true is returned.
     *
     * @param metadata the updated metadata
     * @return true if the plan's basis of this type of metadata was updated
     */
    @Override
    public boolean updatingMetadata(Metadata<?> metadata) {
        final MetadataType mdType = metadata.getType();
        if (!bases.keySet().contains(mdType)) {
            return false;
        }
        return updateBasis(mdType, metadata.getSequenceNumber());
    }

    /**
     * Updates the basis of given metadata type for this plan. If newBasis is
     * greater than the current basis, the current basis will be set to
     * newBasis and true is returned, otherwise false is returned.
     *
     * @param mdType the metadata type
     * @param newBasis the new basis
     * @return true if the basis was updated
     *
     * @throws IllegalStateException if the newBasis is less than the current
     * basis of given metadata type
     */
    private boolean updateBasis(MetadataType mdType, int newBasis) {
        if (bases.get(mdType) == newBasis) {
            return false;
        }

        if (bases.get(mdType) > newBasis) {
            throw new IllegalStateException(
                this + " attempting to persist older version of " +
                mdType + "  metadata");
        }
        bases.put(mdType, newBasis);
        return true;
    }

    private void checkSequenceNumBasis(int seqNum, MetadataType mdType) {
        final int basis = bases.get(mdType);
        if (seqNum != basis) {
            throw new IllegalCommandException(
             "Plan " + this + " was based on the " +
             mdType + " metadata at " +
             "sequence " + basis +
             " but the current metadata is at sequence " +
             (seqNum != Metadata.EMPTY_SEQUENCE_NUMBER ? seqNum : "unknown") +
             ". Please cancel this plan and create a new plan.");
        }
    }
}
