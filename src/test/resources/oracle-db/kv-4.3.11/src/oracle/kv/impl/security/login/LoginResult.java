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
package oracle.kv.impl.security.login;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import oracle.kv.impl.util.FastExternalizable;

/**
 * LoginResult is the result of a login operation.  It currently contains only
 * a single field, but it is expected that later versions will expand on this.
 */
public class LoginResult implements Serializable, FastExternalizable {

    private static final long serialVersionUID = 1;

    private LoginToken loginToken;

    /**
     * Constructor.
     */
    public LoginResult() {
        this.loginToken = null;
    }

    public LoginResult(LoginToken loginToken) {
        this.loginToken = loginToken;
    }

    public LoginResult setLoginToken(LoginToken token) {
        this.loginToken = token;
        return this;
    }

    /* for FastExternalizable */
    public LoginResult(DataInput in, short serialVersion)
        throws IOException {

        final boolean hasToken = in.readBoolean();
        if (hasToken) {
            loginToken = new LoginToken(in, serialVersion);
        } else {
            loginToken = null;
        }
    }

    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
        throws IOException {

        if (loginToken == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            loginToken.writeFastExternal(out, serialVersion);
        }
    }

    public LoginToken getLoginToken() {
        return loginToken;
    }
}
