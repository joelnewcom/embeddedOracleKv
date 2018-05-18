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

package oracle.kv.impl.admin.client;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.rmi.RemoteException;

import oracle.kv.impl.admin.CommandServiceAPI;
import oracle.kv.impl.admin.param.Parameters;
import oracle.kv.impl.topo.Topology;
import oracle.kv.util.Ping;
import oracle.kv.util.shell.Shell;
import oracle.kv.util.shell.ShellCommand;
import oracle.kv.util.shell.ShellException;

/*
 * Add flags, trim down output
 */
class PingCommand extends ShellCommand {

    PingCommand() {
        super("ping", 3);
    }

    @Override
    public String execute(String[] args, Shell shell)
        throws ShellException {
        if (args.length > 1) {
            shell.unknownArgument(args[1], this);
        }
        CommandShell cmd = (CommandShell) shell;
        CommandServiceAPI cs = cmd.getAdmin();
        try {
            Topology topo = cs.getTopology();
            Parameters params = cs.getParameters();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(os);
            Ping.pingTopology(topo, params, false /* showHidden */,
                              shell.getJson(), ps, cmd.getLoginManager());
            return os.toString();
        } catch (RemoteException re) {
            cmd.noAdmin(re);
        }
        return "";
    }

    @Override
    protected String getCommandSyntax() {
        return "ping [-json]";
    }

    @Override
    public String getCommandDescription() {
        return
            "\"Ping\"s the runtime components of a store." +
            eolt + "Components available from the Topology are " +
            "contacted," + eolt + "as well as Admin services.";
    }
}
