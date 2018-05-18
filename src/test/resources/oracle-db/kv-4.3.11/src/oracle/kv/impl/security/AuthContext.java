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
package oracle.kv.impl.security;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import oracle.kv.impl.security.login.LoginToken;
import oracle.kv.impl.util.FastExternalizable;

/**
 * AuthContext captures security information for an RMI method call.
 */
public class AuthContext implements Serializable, FastExternalizable {

    private static final long serialVersionUID = 1;

    private static final int HAS_TOKEN           = 0x1;
    private static final int HAS_FORWARDER_TOKEN = 0x2;
    private static final int HAS_CLIENT_HOST     = 0x4;

    /* The primary login token */
    private LoginToken loginToken;

    /* The login token for the forwarding entity */
    private LoginToken forwarderToken;

    /* The client host for the loginToken, as reported by the forwarder */
    private String clientHost;

    /**
     * Create a AuthContext for an operation being initiated from the
     * original client.
     */
    public AuthContext(LoginToken loginToken) {
        this.loginToken = loginToken;
        this.forwarderToken = null;
        this.clientHost = null;
    }

    /**
     * Create a AuthContext for an operation being forwarded by an SN
     * component to another component on behalf of the original client.
     */
    public AuthContext(LoginToken loginToken,
                       LoginToken forwarderToken,
                       String clientHost) {
        this.loginToken = loginToken;
        this.forwarderToken = forwarderToken;
        this.clientHost = clientHost;
    }

    /* for FastExternalizable */
    public AuthContext(DataInput in, short serialVersion)
        throws IOException {

        final int flags = in.readByte();
        if ((flags & HAS_TOKEN) != 0) {
            loginToken = new LoginToken(in, serialVersion);
        } else {
            loginToken = null;
        }

        if ((flags & HAS_FORWARDER_TOKEN) != 0) {
            forwarderToken = new LoginToken(in, serialVersion);
        } else {
            forwarderToken = null;
        }

        if ((flags & HAS_CLIENT_HOST) != 0) {
            clientHost = in.readUTF();
        } else {
            clientHost = null;
        }
    }

    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
        throws IOException {

        int flags = 0;
        if (loginToken != null) {
            flags |= HAS_TOKEN;
        }
        if (forwarderToken != null) {
            flags |= HAS_FORWARDER_TOKEN;
        }
        if (clientHost != null) {
            flags |= HAS_CLIENT_HOST;
        }

        out.writeByte((byte) flags);

        if (loginToken != null) {
            loginToken.writeFastExternal(out, serialVersion);
        }
        if (forwarderToken != null) {
            forwarderToken.writeFastExternal(out, serialVersion);
        }
        if (clientHost != null) {
            out.writeUTF(clientHost);
        }
    }

    /**
     * Get the login token for the originating requester.
     */
    public LoginToken getLoginToken() {
        return loginToken;
    }

    /*
     * The ForwarderLoginToken is populated for forwarded requests.
     * It is needed to allow the clientHost to be specified.
     */
    public LoginToken getForwarderLoginToken() {
        return forwarderToken;
    }

    /**
     * The host that originated a forwarded request, for audit logging.
     */
    public String getClientHost() {
        return clientHost;
    }
}
