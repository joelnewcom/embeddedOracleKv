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

import oracle.kv.impl.admin.plan.SecurityMetadataPlan;
import oracle.kv.impl.security.metadata.KVStoreUser;
import oracle.kv.impl.security.metadata.KVStoreUser.UserType;
import oracle.kv.impl.security.metadata.SecurityMetadata;

/**
 * Add external user.
 */
public class AddExternalUser extends UpdateMetadata<SecurityMetadata> {

    private static final long serialVersionUID = 1L;

    private final String userName;
    private final boolean isEnabled;
    private final boolean isAdmin;

    public AddExternalUser(SecurityMetadataPlan plan,
                           String userName,
                           boolean isEnabled,
                           boolean isAdmin) {
        super(plan);

        final SecurityMetadata secMd = plan.getMetadata();

        Utils.ensureFirstAdminUser(secMd, isEnabled, isAdmin);

        this.userName = userName;
        this.isAdmin = isAdmin;
        this.isEnabled = isEnabled;

        Utils.checkPreExistingUser(secMd, userName, isEnabled, isAdmin,
                                   null /* no password */);
    }

    @Override
    protected SecurityMetadata updateMetadata() {
        SecurityMetadata md = plan.getMetadata();
        if (md == null) {
            final String storeName =
                    plan.getAdmin().getParams().getGlobalParams().
                    getKVStoreName();
            md = new SecurityMetadata(storeName);
        }

        if (md.getUser(userName) == null) {
            final KVStoreUser newUser =
                KVStoreUser.newInstance(userName, true /* enableRoles */);
            newUser.setEnabled(isEnabled).setAdmin(isAdmin).
                setUserType(UserType.EXTERNAL);

            md.addUser(newUser);
            plan.getAdmin().saveMetadata(md, plan);
        }

        return md;
    }

    /**
     * Returns true if this AddUser will end up creating the same user.
     * Checks that userName, isEnabled, isAdmin are the same.
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

        AddExternalUser other = (AddExternalUser) t;
        if (!userName.equals(other.userName)) {
            return false; 
        }

        if (isEnabled != other.isEnabled || isAdmin != other.isAdmin) {
            return false;
        }

        return true;
    }
}
