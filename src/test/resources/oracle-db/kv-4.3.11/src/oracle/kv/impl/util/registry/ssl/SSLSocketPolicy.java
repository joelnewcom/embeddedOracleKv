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

package oracle.kv.impl.util.registry.ssl;

import static oracle.kv.impl.util.registry.ssl.SSLClientSocketFactory.Use;

import oracle.kv.impl.security.ssl.SSLControl;
import oracle.kv.impl.util.registry.ClientSocketFactory;
import oracle.kv.impl.util.registry.RMISocketPolicy;
import oracle.kv.impl.util.registry.ServerSocketFactory;

/**
 * An RMISocketPolicy implementation is responsible for producing
 * socket factories for RMI.
 */
public class SSLSocketPolicy implements RMISocketPolicy {

    private SSLControl serverSSLControl;
    private SSLControl clientSSLControl;

    /**
     * Create an SSLSocketPolicy.
     */
    public SSLSocketPolicy(SSLControl serverSSLControl,
                           SSLControl clientSSLControl) {
        this.serverSSLControl = serverSSLControl;
        this.clientSSLControl = clientSSLControl;
    }

    /**
     * Prepare for use as standard client policy.
     */
    @Override
    public void prepareClient(String storeContext) {
        /*
         * The client socket factory picks up configuration from its environment
         * because it may be serialized and sent to another process.  We use the
         * client factories locally as well, so set the default context to
         * match our requirements.
         */
        if (isTrusted()) {
            SSLClientSocketFactory.setTrustedControl(clientSSLControl);
        } else {
            SSLClientSocketFactory.setUserControl(clientSSLControl,
                                                  storeContext);
        }
    }

    /**
     * Create a SocketFactoryPair appropriate for creation of an RMI registry.
     */
    @Override
    public SocketFactoryPair getRegistryPair(SocketFactoryArgs args) {
        final ServerSocketFactory ssf =
            SSLServerSocketFactory.create(serverSSLControl,
                                          args.getSsfBacklog(),
                                          args.getSsfPortRange());
        final ClientSocketFactory csf = null;

        return new SocketFactoryPair(ssf, csf);
    }

    /**
     * Return a Client socket factory for appropriate for registry
     * access by the client.
     */
    @Override
    public ClientSocketFactory getRegistryCSF(SocketFactoryArgs args) {
        return new SSLClientSocketFactory(args.getCsfName(),
                                          args.getCsfConnectTimeout(),
                                          args.getCsfReadTimeout(),
                                          args.getKvStoreName());
    }

    /**
     * Create a SocketFactoryPair appropriate for exporting an object over RMI.
     */
    @Override
    public SocketFactoryPair getBindPair(SocketFactoryArgs args) {
        final ServerSocketFactory ssf =
            SSLServerSocketFactory.create(serverSSLControl,
                                          args.getSsfBacklog(),
                                          args.getSsfPortRange());

        ClientSocketFactory csf = null;
        if (args.getCsfName() != null) {
            if (isTrusted()) {
                csf = new SSLClientSocketFactory(args.getCsfName(),
                                                 args.getCsfConnectTimeout(),
                                                 args.getCsfReadTimeout(),
                                                 Use.TRUSTED);
            } else {
                csf = new SSLClientSocketFactory(args.getCsfName(),
                                                 args.getCsfConnectTimeout(),
                                                 args.getCsfReadTimeout(),
                                                 args.getKvStoreName());
            }
        }

        return new SocketFactoryPair(ssf, csf);
    }

    /**
     * Report whether the SSF/CSF pairing can be optionally dropped without
     * impacting correct behavior.  I.e., is the policy simply providing tuning
     * parameters?
     */
    @Override
    public boolean isPolicyOptional() {
        return false;
    }

    /**
     * Reports whether the policy allows a server to be able to "trust" an
     * incoming client connection.
     */
    @Override
    public boolean isTrustCapable() {
        return isTrusted();
    }

    /**
     * An SSL socket policy is a trusted policy if it includes a client
     * authenticator in the server side of the configuration.
     */
    private boolean isTrusted() {
        return serverSSLControl != null &&
            serverSSLControl.peerAuthenticator() != null;
    }

}

