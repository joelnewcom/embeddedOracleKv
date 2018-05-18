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

package oracle.kv.impl.admin.plan.task;

import java.util.logging.Level;

import oracle.kv.impl.admin.Admin;
import oracle.kv.impl.admin.IllegalCommandException;
import oracle.kv.impl.admin.plan.AbstractPlan;
import oracle.kv.impl.admin.plan.MetadataPlan;
import oracle.kv.impl.metadata.Metadata;

import com.sleepycat.persist.model.Persistent;

/**
 * Broadcasts metadata to all RNs. This task may be used standalone
 * or may be subclassed to provide additional functionality.
 */
@Persistent
public class UpdateMetadata<T extends Metadata<?>> extends SingleJobTask {
    private static final long serialVersionUID = 1L;

    protected /*final*/ MetadataPlan<T> plan;

    public UpdateMetadata(MetadataPlan<T> plan) {
        this.plan = plan;
    }

    /*
     * No-arg ctor for use by DPL.
     */
    protected UpdateMetadata() {
    }

    /**
     * Gets the updated metadata to broadcast. The default implementation
     * calls plan.getMetadata(). If null is returned no broadcast is made and
     * the task ends with SUCCEEDED.
     *
     * @return the metadata to broadcast or null
     */
    protected T updateMetadata() {
        return plan.getMetadata();
    }

    protected AbstractPlan getPlan() {
        return plan;
    }

    protected T getMetadata() {
        if (plan == null) {
            throw new IllegalCommandException(
                "Task was not initialized with a MetadataPlan properly, " +
                "or this task did not overload this method correctly");
        }
        return plan.getMetadata();
    }

    @Override
    public State doWork() throws Exception {
        final T md = updateMetadata();

        if (md == null) {
            return State.SUCCEEDED;
        }
        final Admin admin = getPlan().getAdmin();
        admin.getLogger().log(Level.INFO, "About to broadcast {0}", md);

        if (!Utils.broadcastMetadataChangesToRNs(admin.getLogger(),
                                                 md,
                                                 admin.getCurrentTopology(),
                                                 toString(),
                                                 admin.getParams()
                                                 .getAdminParams(),
                                                 getPlan())) {
            return State.INTERRUPTED;
        }
        return State.SUCCEEDED;
    }

    @Override
    public boolean continuePastError() {
        return true;
    }

    @Override
    public boolean logicalCompare(Task t) {
        return true;
    }
}
