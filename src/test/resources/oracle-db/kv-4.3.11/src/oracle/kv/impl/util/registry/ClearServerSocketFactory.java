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

package oracle.kv.impl.util.registry;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

import oracle.kv.impl.util.PortRange;

/**
 * The server socket factory used by the SN and RNs to allocate any available
 * free port. It permits control of the server connect backlog.
 */
public class ClearServerSocketFactory extends ServerSocketFactory {

    /**
     * Create a server socket factory which yields socket connections with
     * the specified backlog.
     *
     * @param backlog the backlog associated with the server socket. A value
     * of zero means use the java default value.
     * @param startPort the start of the port range
     * @param endPort the end of the port range. Both end points are inclusive.
     * A zero start and end port is used to denote an unconstrained allocation
     * of ports as defined by the method
     * ServerSocketFactory.isUnconstrained.
     */
    public ClearServerSocketFactory(int backlog,
                                    int startPort,
                                    int endPort) {
        super(backlog, startPort, endPort);
    }

    @Override
    public String toString() {
        return "<ClearServerSocketFactory" +
               " backlog=" + backlog +
               " port range=" + startPort + "," + endPort + ">";
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        return (obj instanceof ClearServerSocketFactory);
    }

    /**
     * Creates a server socket. Port selection is accomplished as follows:
     *
     * 1) If a specific non-zero port is specified, it's created using just
     * that port.
     *
     * 2) If the port range is unconstrained, that is, isUnconstrained is true
     * then any available port is used.
     *
     * 3) Otherwise, a port from within the specified port range is allocated
     * and IOException is thrown if all ports in that range are busy.
     */
    @Override
    public ServerSocket createServerSocket(int port) throws IOException {

        return commonCreateServerSocket(port);
    }

    /**
     * A trival implementation of prepareServerSocket, since we don't require
     * its functionality.
     */
    @Override
    public ServerSocket prepareServerSocket() {
        return null;
    }

    /**
     * A trival implementation of prepareServerSocket, since we don't require
     * its functionality.
     */
    @Override
    public void discardServerSocket(ServerSocket ss) {
        throw new UnsupportedOperationException(
            "discardServerSocket is not supported by this implementation.");
    }

    /**
     * Factory method to configure SSF appropriately.
     *
     * @return an SSF or null if the factory has been disabled
     */
    public static ClearServerSocketFactory create(int backlog,
                                                  String portRange) {

        if (ServerSocketFactory.isDisabled()) {
            return null;
        }

        if (PortRange.isUnconstrained(portRange)) {
            return new ClearServerSocketFactory(backlog, 0, 0);
        }

        final List<Integer> range = PortRange.getRange(portRange);
        return new ClearServerSocketFactory(backlog, range.get(0),
                                            range.get(1));
    }

    @Override
    protected ServerSocket instantiateServerSocket(int port)
        throws IOException {

        return new ServerSocket(port);
    }

    @Override
    protected ServerSocket instantiateServerSocket(int port, int backlog1)
        throws IOException {

        return new ServerSocket(port, backlog1);
    }
}
