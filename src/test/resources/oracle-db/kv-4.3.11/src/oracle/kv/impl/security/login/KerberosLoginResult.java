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

/**
 * KerberosLoginResult is the result of a Kerberos login. It extends LoginResult
 * adding the mutual authentication token in addition to the LoginToken itself.
 *
 * This class, which includes the mutual authentication token bytes, was
 * introduced in release 3.5.
 *
 * @since 3.5
 */
public class KerberosLoginResult extends LoginResult {

    private static final long serialVersionUID = 1L;

    /*
     * Contains information that the client can use to authenticate the
     * server, when performing mutual authentication, otherwise null.
     */
    private byte[] mutualAuthenToken;

    /**
     * Creates an instance the supplies the mutual authentication token but no
     * login token.
     */
    public KerberosLoginResult(byte[] token) {
        super(null);
        this.mutualAuthenToken = token;
    }

    public KerberosLoginResult(LoginToken loginToken, byte[] authenToken) {
        super(loginToken);
        mutualAuthenToken = authenToken;
    }

    /**
     * FastExternalizable constructor.  Must call superclass constructor
     * first to read common elements.
     */
    public KerberosLoginResult(DataInput in, short serialVersion)
        throws IOException {

        super(in, serialVersion);

        int tokenLen = in.readShort();
        mutualAuthenToken = new byte[tokenLen];
        in.readFully(mutualAuthenToken);
    }

    /**
     * FastExternalizable writer.  Must call superclass method first to
     * write common elements.
     */
    @Override
    public void writeFastExternal(DataOutput out, short serialVersion)
        throws IOException {

        super.writeFastExternal(out, serialVersion);

        out.writeShort(mutualAuthenToken.length);
        out.write(mutualAuthenToken);
    }

    public byte[] getMutualAuthToken() {
        return mutualAuthenToken;
    }
}
