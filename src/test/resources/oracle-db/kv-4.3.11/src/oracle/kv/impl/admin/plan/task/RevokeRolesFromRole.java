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

import java.util.Set;

import oracle.kv.impl.admin.IllegalCommandException;
import oracle.kv.impl.admin.plan.SecurityMetadataPlan;
import oracle.kv.impl.security.RoleInstance;
import oracle.kv.impl.security.metadata.SecurityMetadata;

public class RevokeRolesFromRole extends UpdateMetadata<SecurityMetadata> {

    private static final long serialVersionUID = 1L;
    private final String revokee;
    private final Set<String> roles;

    public RevokeRolesFromRole(SecurityMetadataPlan plan,
                               String revokee,
                               Set<String> roles) {
        super(plan);

        final SecurityMetadata secMd = plan.getMetadata();
        this.revokee = revokee;
        this.roles = roles;

        /* Fail if target role does not exist */
        if (secMd == null || secMd.getRole(revokee) == null) {
            throw new IllegalCommandException(
                "Role with name " + revokee + " does not exist in store");
        }
    }

    @Override
    protected SecurityMetadata updateMetadata() {
        final SecurityMetadata secMd = plan.getMetadata();

        /* Return null if role does not exist */
        if (secMd == null || secMd.getRole(revokee) == null) {
            return null;
        }

        final RoleInstance newCopy = secMd.getRole(revokee).clone();
        secMd.updateRole(
            newCopy.getElementId(), newCopy.revokeRoles(roles));
        plan.getAdmin().saveMetadata(secMd, plan);

        return secMd;
    }

    /**
     * Returns true if this RevokeRolesFromRole will end up granting the same
     * roles to the same role. Checks that revokee and role set
     * are the same.
     */
    @Override
    public boolean logicalCompare(Task t) {
        if (this == t) {
            return true;
        }

        if (t == null) {
            return false;
        }

        if (getClass() != t.getClass()) {
            return false;
        }

        RevokeRolesFromRole other = (RevokeRolesFromRole) t;
        if (!revokee.equalsIgnoreCase(other.revokee)) {
            return false; 
        }

        return roles.equals(other.roles);
    }
}
