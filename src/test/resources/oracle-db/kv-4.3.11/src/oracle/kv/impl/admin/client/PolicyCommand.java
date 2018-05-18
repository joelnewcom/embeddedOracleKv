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

import java.rmi.RemoteException;

import oracle.kv.impl.admin.CommandServiceAPI;
import oracle.kv.impl.param.ParameterMap;
import oracle.kv.impl.param.ParameterState;
import oracle.kv.util.shell.Shell;
import oracle.kv.util.shell.ShellCommand;
import oracle.kv.util.shell.ShellException;

class PolicyCommand extends ShellCommand {

    PolicyCommand() {
        super("change-policy", 4);
    }

    @Override
    public String execute(String[] args, Shell shell)
        throws ShellException {

        if (args.length < 3) {
            shell.badArgCount(this);
        }
        CommandShell cmd = (CommandShell) shell;
        CommandServiceAPI cs = cmd.getAdmin();

        boolean showHidden = cmd.showHidden();
        boolean dryRun = false;
        int i;
        boolean foundParams = false;
        for (i = 1; i < args.length; i++) {
            String arg = args[i];
            if ("-hidden".equals(arg)) {
                showHidden = true;
            } else if ("-dry-run".equals(arg)) {
                dryRun = true;
            } else if ("-params".equals(arg)) {
                ++i;
                foundParams = true;
                break;
            } else {
                shell.unknownArgument(arg, this);
            }
        }

        if (!foundParams) {
            shell.requiredArg(null, this);
        }
        if (args.length <= i) {
            return "No parameters were specified";
        }
        try {
            ParameterMap map = cs.getPolicyParameters();
            CommandUtils.parseParams(map, args, i, ParameterState.Info.POLICY,
                                     null, showHidden, this);
            if (dryRun) {
                return CommandUtils.formatParams(map, showHidden, null);
            }
            if (shell.getVerbose()) {
                shell.verboseOutput
                    ("New policy parameters:" + eol +
                     CommandUtils.formatParams(map, showHidden, null));
            }
            cs.setPolicies(map);
        } catch (RemoteException re) {
            cmd.noAdmin(re);
        }
        return "";
    }

    @Override
    protected String getCommandSyntax() {
        return name + " [-dry-run] -params [name=value]*";
    }

    @Override
    protected String getCommandDescription() {
        return
            "Modifies store-wide policy parameters that apply to not yet " +
            "deployed" + eolt + "services. The parameters to change " +
            "follow the -params flag and are" + eolt + "separated by " +
            "spaces. Parameter values with embedded spaces must be" +
            eolt + "quoted.  For example name=\"value with spaces\". " +
            "If -dry-run is" + eolt + "specified the new parameters " +
            "are returned without changing them.";
    }
}
