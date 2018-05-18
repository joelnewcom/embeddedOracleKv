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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import oracle.kv.AuthenticationFailureException;
import oracle.kv.AuthenticationRequiredException;
import oracle.kv.KVSecurityException;
import oracle.kv.KVVersion;
import oracle.kv.LoginCredentials;
import oracle.kv.PasswordCredentials;
import oracle.kv.impl.admin.AdminFaultException;
import oracle.kv.impl.admin.CommandResult;
import oracle.kv.impl.admin.CommandServiceAPI;
import oracle.kv.impl.admin.NonfatalAssertionException;
import oracle.kv.impl.admin.param.AdminParams;
import oracle.kv.impl.admin.param.Parameters;
import oracle.kv.impl.admin.param.StorageNodeParams;
import oracle.kv.impl.security.PasswordExpiredException;
import oracle.kv.impl.security.SessionAccessException;
import oracle.kv.impl.security.login.AdminLoginManager;
import oracle.kv.impl.security.login.LoginManager;
import oracle.kv.impl.security.util.KVStoreLogin;
import oracle.kv.impl.security.util.PasswordReader;
import oracle.kv.impl.security.util.ShellPasswordReader;
import oracle.kv.impl.util.CommandParser;
import oracle.kv.impl.util.registry.RegistryUtils;
import oracle.kv.shell.AggregateCommand;
import oracle.kv.shell.DeleteCommand;
import oracle.kv.shell.ExecuteCommand;
import oracle.kv.shell.GetCommand;
import oracle.kv.shell.PutCommand;
import oracle.kv.util.ErrorMessage;
import oracle.kv.util.shell.CommandWithSubs;
import oracle.kv.util.shell.CommonShell;
import oracle.kv.util.shell.Shell;
import oracle.kv.util.shell.ShellCommand;
import oracle.kv.util.shell.ShellException;
import oracle.kv.util.shell.ShellRCFile;

import com.sleepycat.je.rep.ReplicatedEnvironment.State;

/**
 * To implement a new command:
 * 1.  Implement a class that extends ShellCommand.
 * 2.  Add it to the static list, commands, in this class.
 *
 * Commands that have subcommands should extend SubCommand.  See one of the
 * existing classes for example code (e.g. PlanCommand).
 */
public class CommandShell extends CommonShell {

    public static final String COMMAND_NAME = "runcli";
    public static final String COMMAND_NAME_ALIAS = "runadmin";
    public static final String COMMAND_DESC =
        "runs the command line interface";
    public static final String COMMAND_ARGS =
        CommandParser.getHostUsage() + " " +
        CommandParser.getPortUsage() + " " +
        CommandParser.optional(CommandParser.getStoreUsage()) + eolt +
        CommandParser.optional(CommandParser.getAdminHostUsage() + " " +
        CommandParser.getAdminPortUsage()) + eolt +
        CommandParser.optional(CommandParser.getUserUsage()) + " " +
        CommandParser.optional(CommandParser.getSecurityUsage()) + eolt +
        CommandParser.optional(CommandParser.getAdminUserUsage()) + " " +
        CommandParser.optional(CommandParser.getAdminSecurityUsage()) + eolt +
        CommandParser.optional(CommandParser.getTimeoutUsage()) + " " +
        CommandParser.optional(CommandParser.getConsistencyUsage()) + eolt +
        CommandParser.optional(CommandParser.getDurabilityUsage()) + eolt +
        CommandParser.optional(CommandParser.getDnsCacheTTLUsage()) + eolt +
        "[single command and arguments]";

    /*
     * Internal use only, used to indicate that the CLI is run
     * from KVStoreMain.
     */
    public static final String RUN_BY_KVSTORE_MAIN= "-kvstore-main";

    /* The section name in rc file for this shell */
    private static String RC_FILE_SECTION = "kvcli";

    private CommandServiceAPI cs;
    private final CommandLoginHelper loginHelper;
    private boolean retry = false;
    private String commandName = null;

    /* Host and port of the currently connected admin */
    private String adminHostname = null;
    private int adminRegistryPort = 0;

    /*
     * Addresses of the known Admins. The list is updated whenever a new
     * connection to an Admin is established. This list may be null during
     * initialization or if there is an error updating the list.
     */
    private List<URI> knownAdmins = null;

    /*
     * State of the connected Admin. This is set at time of connection and may
     * not reflect the current state of the Admin. The value may be null if not
     * connected or the Admin is not configured.
     */
    private State adminState = null;
    private String adminUser;
    private String adminSecurityFile;

    static final String prompt = "kv-> ";
    static final String usageHeader =
        "Oracle NoSQL Database Administrative Commands:" + eol;
    static final String versionString = " (" +
        KVVersion.CURRENT_VERSION.getNumericVersionString() + ")";

    /*
     * The list of commands available
     */
    private static
        List<? extends ShellCommand> commands =
                       Arrays.asList(new AggregateCommand(),
                                     new AwaitCommand(),
                                     new ConfigureCommand(),
                                     new ConnectCommand(),
                                     new DdlCommand(),
                                     new DebugCommand(),
                                     new DeleteCommand(),
                                     new ExecuteCommand(),
                                     new Shell.ExitCommand(),
                                     new GetCommand(),
                                     new Shell.HelpCommand(),
                                     new HiddenCommand(),
                                     new HistoryCommand(),
                                     new Shell.LoadCommand(),
                                     new LogtailCommand(),
                                     new PageCommand(),
                                     new PingCommand(),
                                     new PlanCommand(),
                                     new PolicyCommand(),
                                     new PoolCommand(),
                                     new PutCommand(),
                                     new RepairAdminQuorumCommand(),
                                     new ShowCommand(),
                                     new SnapshotCommand(),
                                     new TableCommand(),
                                     new TableSizeCommand(),
                                     new TimeCommand(),
                                     new TopologyCommand(),
                                     new VerboseCommand(),
                                     new VerifyCommand()
                                     );

    /*
     * The keywords whose following value will be masked with mask
     * character(*) in the command line history.
     */
    private static final String[] maskFlags = new String[] {
        "-password", "identified by"
    };

    public CommandShell(InputStream input, PrintStream output) {
        super(input, output, maskFlags);
        Collections.sort(commands, new Shell.CommandComparator());
        loginHelper = new CommandLoginHelper();
        setLoginHelper(loginHelper);
    }

    @Override
    public void init() {
        if (!isNoConnect()) {
            try {
                connect();
            } catch (ShellException se) {
                /*
                 * The exception may be thrown while connecting a store or
                 * admin service
                 */
                displayResultReport("connect store or admin service",
                                    se.getCommandResult(),
                                    se.getMessage());
                if (getDebug()) {
                    se.printStackTrace(output);
                }
                /*
                 * If in json mode, we give up following execution in order to
                 * return a single result report to client.
                 */
                if (getJson()) {
                    if (dontExit()) {
                        /* For testing, save the exist code and do not exit*/
                        this.exitCode = EXIT_UNKNOWN;
                        return;
                    }
                    System.exit(EXIT_UNKNOWN);
                }
            }
        } else {
            output.println("Not connected to a store or admin service.  Use " +
                           "the connect command to connect.");
        }
    }

    @Override
    public void shutdown() {
        closeStore();
    }

    @Override
    public List<? extends ShellCommand> getCommands() {
        return commands;
    }

    @Override
    public String getPrompt() {
        return isNoPrompt() ? null : prompt;
    }

    @Override
    public String getUsageHeader() {
        return usageHeader;
    }

    /*
     * If retry is true, return that, but be sure to reset the value
     */
    @Override
    public boolean doRetry() {
        boolean oldVal = retry;
        retry = false;
        return oldVal;
    }

    @Override
    public void handleUnknownException(String line, Exception e) {
        if (e instanceof AdminFaultException) {
            AdminFaultException afe = (AdminFaultException) e;
            String faultClassName = afe.getFaultClassName();
            String msg = afe.getMessage();
            /* Don't treat IllegalCommandException as a "fault" */
            if (faultClassName.contains("IllegalCommandException")) {
                /* strip version info from message -- it's just usage */
                int endIndex = msg.indexOf(versionString);
                if (endIndex > 0) {
                    msg = msg.substring(0, endIndex);
                }
                e = null;
            }
            history.add(line, e);
            exitCode = EXIT_UNKNOWN;
            displayResultReport(line, afe.getCommandResult(), msg);
            if ((e != null) && getDebug()) {
                e.printStackTrace(output);
            }
        } else if (e instanceof KVSecurityException) {
            super.handleKVSecurityException(line, (KVSecurityException) e);
        } else {
            super.handleUnknownException(line, e);
        }
    }

    /**
     * Handles uncaught {@link KVSecurityException}s during execution of admin
     * commands. Currently we will retry login and connect to admin for
     * {@link AuthenticationRequiredException}s. The
     * {@link KVSecurityException}s during execution of store commands have
     * been wrapped as {@link ShellException}s and handled elsewhere.
     *
     * @param line command line
     * @param kvse instance of KVSecurityException
     * @return true if re-connect to admin successfully, or a ShellException
     * calls for a retry, otherwise returns false.
     */
    @Override
    public boolean handleKVSecurityException(String line,
                                             KVSecurityException kvse) {
        if (kvse instanceof AuthenticationRequiredException) {
            try {
                /* Login and connect to the admin again. */
                connectAdmin(true /* force login */);

                /* Retry the command */
                return true;
            } catch (ShellException se) {
                return handleShellException(line, se);
            } catch (Exception e) {
                handleUnknownException(line, e);
                return false;
            }
        }
        return super.handleKVSecurityException(line, kvse);
    }

    /**
     * Gets the command service. Makes a connection if necessary and caches
     * the handle. Subsequent calls may return the same handle.
     */
    public CommandServiceAPI getAdmin()
        throws ShellException {
        return getAdmin(false);

    }

    /**
     * Gets the command service. Makes a connection if necessary and caches
     * the handle. Subsequent calls may return the same handle. If force is
     * true a new connection is always made.
     *
     * @param force if true always make a new connection
     */
    public CommandServiceAPI getAdmin(boolean force)
        throws ShellException {

        ensureConnection(force);
        return cs;
    }

    String getAdminHostname() {
        return adminHostname;
    }

    /* public for unit test */
    public int getAdminPort() {
        return adminRegistryPort;
    }

    public void connect()
        throws ShellException {

        /*
         * Also update AdminLogin with security file, so that Admin login
         * will use the security file specified to login Admin after store
         * login failed.
         */
        loginHelper.updateAdminLogin(adminUser, adminSecurityFile);
        connectStore();
        connectAdmin(false /* force login */);
    }

    public LoginManager getLoginManager() {
        return loginHelper.getAdminLoginMgr();
    }

    /*
     * Centralized call for connection issues.  If the exception indicates that
     * the admin service cannot be contacted null out the handle and force a
     * reconnect on a retry.
     */
    public void noAdmin(RemoteException e)
        throws ShellException{

        if (e != null) {
            if (cs != null) {
                Throwable t = e.getCause();
                if (t != null) {
                    if (t instanceof EOFException ||
                        t instanceof java.net.ConnectException ||
                        t instanceof java.rmi.ConnectException) {
                        cs = null;
                        retry = true;
                    }
                }
            }
        }

        throw new ShellException("Cannot contact admin", e,
                                 ErrorMessage.NOSQL_5300,
                                 CommandResult.NO_CLEANUP_JOBS);
    }

    /**
     * Ensure that we have a connection to the admin.  If the current
     * connection is read-only, then make one attempt to connect again in hopes
     * of connecting to the master.  Make 10 connection attempts if there is no
     * current connection, including additional attempts if a read-only
     * connection is found first. If force is true a new connection is always
     * made.
     *
     * @param force if true always make a new connection
     */
    private void ensureConnection(boolean force) throws ShellException {
        if ((cs != null) && !force) {
            /*
             * We have a command service, but it is read-only.  Try to
             * reconnect to the master once.
             */
            if (isReadOnly()) {
                connectAdmin(false /* force login */);
            }
            return;
        }

        ShellException ce = null;
        echo("Lost connection to Admin service." + eol);
        echo("Reconnecting...");
        for (int i = 0; i < 10; i++) {
            try {
                echo(".");
                connectAdmin(false /* force login */);
                echo(eol);
                return;
            } catch (ShellException se) {
                final Throwable t = se.getCause();
                if (t != null &&
                    t instanceof AuthenticationFailureException) {
                    throw se;
                }
                ce = se;
            }
            try {
                Thread.sleep(6000);
            } catch (InterruptedException ignore) { }
        }
        if (ce != null) {
            throw ce;
        }
    }

    /**
     * Connects to the Admin at the specified host and port as the specified
     * user. If the Admin at host:port is not the master an attempt is made to
     * find and connect to the master.
     *
     * @param host hostname of Admin
     * @param port registry port of Admin
     * @param user user name
     * @param securityFileName login file path
     *
     * @throws ShellException if no Admin found
     */
    public void connectAdmin(String host,
                             int port,
                             String user,
                             String securityFileName)
        throws ShellException {

        loginHelper.updateAdminLogin(user, securityFileName);
        connectAdmin(host, port, false /* force login */);
        updateKnownAdmins();
    }

    /**
     * Reconnects to the master Admin. The initial attempt is made
     * the last Admin at adminHostname:adminRegistryPort. If that fails the
     * list of known Admins is tried. Upon successful return cs is set to the
     * connected Admin, adminHostname:adminRegistryPort are set to it's address,
     * and readOnly is true if the Admin is not the master.
     *
     * @param forceLogin force login before connection
     *
     * @throws ShellException if no Admin found
     */
    private void connectAdmin(boolean forceLogin) throws ShellException {
        ShellException e = null;

        /* Attempt to reconnect to the current admin */
        try {
            connectAdmin(adminHostname, adminRegistryPort, forceLogin);

            /*
             * If a connection was made to the master we are done. Note that if
             * the Admin is not configured isReadOnly() will return false and
             * we will remain connected to that bootstrap Admin.
             */
            if (!isReadOnly()) {
                updateKnownAdmins();
                return;
            }
        } catch (ShellException se) {
            e = se;
        }

        /*
         * We either have no Admin, or it is not a master. If we know of other
         * Admins, try to connect to one of them.
         */
        if (knownAdmins != null) {
            for (URI addr : knownAdmins) {
                try {
                    connectAdmin(addr.getHost(), addr.getPort(), forceLogin);

                    /* If the master was found, we are done */
                    if (!isReadOnly()) {
                        break;
                    }
                } catch (ShellException se) {
                    e = se;
                }
            }
        }

        /* If no Admin at this point, fail */
        if (cs == null) {
            if (e != null) {
                throw e;
            }
            throw new ShellException("Cannot connect to Admin at " +
                                     adminHostname + ":" + adminRegistryPort,
                                     e, ErrorMessage.NOSQL_5300,
                                     CommandResult.NO_CLEANUP_JOBS);
        }

        if (isReadOnly()) {
            echo("Connected to Admin in read-only mode" + eol);
        }
        updateKnownAdmins();
    }

    /**
     * Updates the list of known Admins. The list should be updated each time
     * we connect as the Admin configuration may have changed.
     */
    private void updateKnownAdmins() {
        final List<URI> admins = new ArrayList<>();

        try {
            final Parameters p = cs.getParameters();

            for (AdminParams ap : p.getAdminParams()) {
                final StorageNodeParams snp = p.get(ap.getStorageNodeId());
                try {
                    admins.add(new URI("rmi", null,
                                       snp.getHostname(), snp.getRegistryPort(),
                                       null, null, null));
                } catch (URISyntaxException use) {
                    throw new NonfatalAssertionException("Unexpected bad URL",
                                                         use);
                }
            }
            knownAdmins = admins;
        } catch (Exception ignore) {
            /*
             * There are host of problems that can happen, but since the known
             * Admin list is not critical to operation we silently ignore them.
             */
        }

        /* Only update the list if we actually found Admins */
        if (!admins.isEmpty()) {
            knownAdmins = admins;
        }
    }

    /**
     * Connects to the Admin at the specified host and port. If the
     * Admin at host:port is not the master an attempt is made to find and
     * connect to the master. Upon successful return cs is set to the connected
     * Admin, adminHostname:adminRegistryPort are set to it's address, and
     * readOnly is true if the Admin is not the master.
     *
     * @param host hostname of Admin
     * @param port registry port of Admin
     * @param forceLogin force login before connection
     *
     * @throws ShellException if no Admin found
     */
    private void connectAdmin(String host, int port, boolean forceLogin)
        throws ShellException {

        Exception e;
        try {
            final CommandServiceAPI admin =
                loginHelper.getAuthenticatedAdmin(host, port, forceLogin);
            final State state = admin.getAdminStatus().getReplicationState();
            if (state != null) {
                switch (state) {
                case MASTER:
                    setAdmin(admin, state, host, port);
                    return;
                case REPLICA:
                    setAdmin(admin, state, host, port);

                    /* A replica is ok, but try to connect to the master */
                    final URI rmiaddr = cs.getMasterRmiAddress();
                    try {
                        echo("Redirecting to master at " + rmiaddr + eol);
                        connectAdmin(rmiaddr.getHost(), rmiaddr.getPort(),
                                     false);
                        return;
                    } catch (ShellException e2) {
                        echo("Redirect failed to master at " + rmiaddr + eol);
                    }
                    return;
                case DETACHED:
                case UNKNOWN:
                    break;
                }
            }
            /*
             * Here because state == DETACHED, UNKNOWN, or no state (not yet
             * configured).  Switch to this admin since at least we were able
             * to talk to it.
             */
            setAdmin(admin, state, host, port);
            return;
        } catch (AuthenticationRequiredException are) {
            /*
             * Will fail if try connecting to a secured admin without login.
             * Retry connecting and force login.
             */
            echo(String.format("Admin %s:%d requires authentication." + eol,
                               host, port));
            connectAdmin(host, port, true);
            return;
        } catch (AdminFaultException afe) {
            /*
             * Logging in another admin may be problematic, because it needs
             * to verify the login token from the issued admin, which may fail
             * due to network issues. In this case, a SessionAccessException
             * will be thrown, and we try to force to directly log in the new
             * admin.
             */
            if (afe.getFaultClassName().equals(
                SessionAccessException.class.getName())) {
                echo("Problem in verifying the redirected login. " +
                     "Need to log in admin at " + host + ":" + port +
                     " again." + eol);
                connectAdmin(host, port, true);
                return;
            }
            e = afe;
        } catch (RemoteException re) {
            e = re;
        } catch (NotBoundException nbe) {
            e = nbe;
        }
        throw new ShellException("Cannot connect to Admin at " +
                                 host + ":" + port, e, ErrorMessage.NOSQL_5300,
                                 CommandResult.NO_CLEANUP_JOBS);
    }

    private void setAdmin(CommandServiceAPI admin, State state,
                          String host, int port) {
        cs = admin;
        adminState = state;
        adminHostname = host;
        adminRegistryPort = port;
    }

    /**
     * Returns true if the command shell is connected to the Admin in read only
     * mode. This value set when the connection is made and should be used for
     * informational purposes only as mastership may change. Returns false
     * if the Admin is the master, has not been configured, or no connection
     * has been made.
     */
    boolean isReadOnly() {
        return (adminState != null) && !adminState.isMaster();
    }

    static class ConnectCommand extends CommandWithSubs {
        private static final String COMMAND = "connect";
        private static final String HOST_FLAG = "-host";
        private static final String HOST_FLAG_DESC = HOST_FLAG + " <hostname>";
        private static final String PORT_FLAG = "-port";

        private static final List<? extends SubCommand> subs =
            Arrays.asList(new ConnectAdminSubCommand(),
                          new ConnectStoreSubCommand());

        ConnectCommand() {
            super(subs, COMMAND, 4, 2);
        }

        @Override
        protected String getCommandOverview() {
            return "Encapsulates commands that connect to the specified " +
                "host and registry port" + eol + "to perform administrative " +
                "functions or connect to the specified store to" + eol +
                "perform data access functions.";
        }

        /* ConnectAdmin command */
        static class ConnectAdminSubCommand extends SubCommand {
            private static final String SUB_COMMAND = "admin";
            private static final String PORT_FLAG_DESC =
                PORT_FLAG + " <registry port>";

            static final String CONNECT_ADMIN_COMMAND_DESC =
                "Connects to the specified host and registry port to " +
                "perform" + eolt + "administrative functions.  An Admin " +
                "service must be active on the" + eolt + "target host.  " +
                "If the instance is secured, you may need to provide" + eolt +
                "login credentials.";

            static final String CONNECT_ADMIN_COMMAND_SYNTAX =
                COMMAND + " " + SUB_COMMAND + " " + HOST_FLAG_DESC + " " +
                PORT_FLAG_DESC + eolt +
                CommandParser.optional(CommandParser.getUserUsage()) + " " +
                CommandParser.optional(CommandParser.getSecurityUsage());

            ConnectAdminSubCommand() {
                super(SUB_COMMAND, 3);
            }

            @Override
            public String execute(String[] args, Shell shell)
                throws ShellException {

                Shell.checkHelp(args, this);
                String host = null;
                int port = 0;
                String user = null;
                String security = null;

                for (int i = 1; i < args.length; i++) {
                    String arg = args[i];
                    if (HOST_FLAG.equals(arg)) {
                        host = Shell.nextArg(args, i++, this);
                    } else if (PORT_FLAG.equals(arg)) {
                        port = parseUnsignedInt(Shell.nextArg(args, i++, this));
                    } else if (CommandParser.USER_FLAG.equals(arg)) {
                        user = Shell.nextArg(args, i++, this);
                    } else if (CommandParser.SECURITY_FLAG.equals(arg)) {
                        security = Shell.nextArg(args, i++, this);
                    } else {
                        invalidArgument(arg);
                    }
                }
                if (host == null || port == 0) {
                    shell.badArgCount(this);
                }
                final CommandShell cmd = (CommandShell) shell;
                cmd.connectAdmin(host, port, user, security);

                /*
                 * If security enabled, the command history will be cleared
                 * after each re-connection.
                 */
                if (cmd.loginHelper.isSecuredAdmin()) {
                    cmd.getHistory().clear();
                }
                return "Connected.";
            }

            @Override
            protected String getCommandSyntax() {
                return CONNECT_ADMIN_COMMAND_SYNTAX;
            }

            @Override
            protected String getCommandDescription() {
                return CONNECT_ADMIN_COMMAND_DESC;
            }
        }

        /* ConnectStore command */
        static class ConnectStoreSubCommand extends SubCommand {
            final static String SUB_COMMAND = "store";
            final static String CONNECT_STORE_COMMAND_SYNTAX =
                COMMAND + " " + SUB_COMMAND + " " +
                ConnectStoreCommand.CONNECT_STORE_COMMAND_ARGUMENTS;

            private final ConnectStoreCommand connCommand;

            ConnectStoreSubCommand() {
                super(SUB_COMMAND, 4);
                connCommand = new ConnectStoreCommand();
            }

            @Override
            public String execute(String[] args, Shell shell)
                throws ShellException {

                Shell.checkHelp(args, this);
                return connCommand.execute(args, shell);
            }

            @Override
            protected String getCommandSyntax() {
                return CONNECT_STORE_COMMAND_SYNTAX;
            }

            @Override
            protected String getCommandDescription() {
                return ConnectStoreCommand.CONNECT_STORE_COMMAND_DESC;
            }
        }
    }

    private class CommandShellParser extends ShellParser {

        CommandShellParser(String[] args,
                           String[] rcArgs,
                           String[] requiredFlags) {
            super(args, rcArgs, requiredFlags);
        }

        @Override
        public String getShellUsage() {
            final String usage;
            if (commandName == null) {
                usage = KVCLI_USAGE_PREFIX + eolt + COMMAND_ARGS;
            } else {
                usage = KVSTORE_USAGE_PREFIX + commandName + " " + eolt +
                    COMMAND_ARGS;
            }
            return usage;
        }

        @Override
        protected void verifyArgs() {
            super.verifyArgs();
            if (!isNoConnect()) {
                if (adminHostname == null && adminRegistryPort == 0) {
                    adminHostname = getHostname();
                    adminRegistryPort = getRegistryPort();
                }
                if (adminHostname == null || adminRegistryPort == 0) {
                    usage("Missing required argument");
                }

                /*
                 * If no separate user or login file is set for admin, admin
                 * will share the same user and login file with store.
                 */
                if (adminUser == null && adminSecurityFile == null) {
                    adminUser = getUserName();
                    adminSecurityFile = getSecurityFile();
                }
            }
        }

        // TODO: change to json
        @Override
        public boolean checkExtraArg(String arg) {
            if (ADMIN_HOST_FLAG.equals(arg)) {
                adminHostname = nextArg(arg);
                return true;
            } else if (ADMIN_PORT_FLAG.equals(arg)) {
                adminRegistryPort = Integer.parseInt(nextArg(arg));
                return true;
            } else if (ADMIN_USER_FLAG.equals(arg)) {
                /* Admin and store may need different users and login files */
                adminUser = nextArg(arg);
                return true;
            } else if (ADMIN_SECURITY_FLAG.equals(arg)) {
                adminSecurityFile = nextArg(arg);
                return true;
            }
            return false;
        }

        @Override
        public boolean checkExtraHiddenFlag(String arg) {
            if (RUN_BY_KVSTORE_MAIN.equals(arg)) {
                commandName = nextArg(arg);
                return true;
            }
            return false;
        }
    }

    public void parseArgs(String args[]) {
        final String[] requiredFlags =
            new String[]{CommandParser.HOST_FLAG,
                         CommandParser.PORT_FLAG};
        final String[] rcArgs = ShellRCFile.readSection(RC_FILE_SECTION);
        final ShellParser parser = new CommandShellParser(args, rcArgs,
                                                          requiredFlags);
        parser.parseArgs();
    }

    public static void main(String[] args) {
        CommandShell shell = new CommandShell(System.in, System.out);
        try {
            shell.parseArgs(args);
        } catch (Exception e) {
            String error;
            if(args != null && checkArg(args, CommandParser.JSON_FLAG)) {
                String operation = "";
                for(String arg: args) {
                    operation += arg + " ";
                }
                CommandResult result =
                    new CommandResult.CommandFails(
                        "Argument error", ErrorMessage.NOSQL_5100,
                        CommandResult.NO_CLEANUP_JOBS);
                error = Shell.toJsonReport(operation, result);
            } else {
                error = "Argument error: " + e.toString();
            }
            System.err.println(error);
            if (shell.dontExit()) {
                return;
            }
            System.exit(1);
        }
        shell.start();
        if ((shell.getExitCode() != EXIT_OK) && !shell.dontExit()) {
            System.exit(shell.getExitCode());
        }
    }

    /**
     * A helper class for client login of Admin.
     */
    private class CommandLoginHelper extends LoginHelper {
        private final KVStoreLogin adminLogin = new KVStoreLogin();
        private static final String loginConnErrMsg =
            "Cannot connect to Admin login service %s:%d";
        private LoginManager adminLoginMgr;
        private boolean isSecuredAdmin;

        /* Uses to record the last AFE during loginAdmin() method */
        private AuthenticationFailureException lastAdminAfe;

        public CommandLoginHelper() {
            super();
        }

        /**
         * Updates the login information of admin.
         *
         * @param newUser new user name
         * @param newLoginFile new login file path
         * @throws ShellException if there was a problem loading security
         * configuration information
         */
        private void updateAdminLogin(final String newUser,
                                      final String newLoginFile)
            throws ShellException {

            try {
                adminLogin.updateLoginInfo(newUser, newLoginFile);

                /* Login is needed if SSL transport is used */
                isSecuredAdmin = adminLogin.foundSSLTransport();
                adminLogin.prepareRegistryCSF();
                adminLoginMgr = null;
            } catch (IllegalStateException ise) {
                throw new ShellException(ise.getMessage());
            } catch (IllegalArgumentException iae) {
                throw new ShellException(iae.getMessage());
            }
        }

        /**
         * Returns the CommandServiceAPI. If admin is secured, authentication
         * will be conducted using login file or via user interaction.
         *
         * @param host hostname of admin
         * @param port registry port of admin
         * @param forceLogin force login before connection
         * @return commandServiceAPI of admin
         */
        private CommandServiceAPI
            getAuthenticatedAdmin(final String host,
                                  final int port,
                                  final boolean forceLogin)
            throws ShellException,
                   RemoteException,
                   NotBoundException {

            if (forceLogin || (isSecuredAdmin && (adminLoginMgr == null))) {
                loginAdmin(host, port);
            }
            return RegistryUtils.getAdmin(host, port, adminLoginMgr);
        }

        private LoginManager getAdminLoginMgr() {
            return adminLoginMgr;
        }

        /*
         * Tries to login the admin.
         */
        private void loginAdmin(String host, int port)
            throws ShellException {

            final LoginCredentials storeCreds = getCredentials();
            final String adminLoginUser = adminLogin.getUserName();
            lastAdminAfe = null;

            /*
             * When store creds is available, if no admin user is
             * specified, or the admin user is identical with store user,
             * try to login admin using the store creds.
             */
            if (storeCreds != null) {
                if ((adminLoginUser == null) ||
                    adminLoginUser.equals(storeCreds.getUsername())) {
                    if (loginAdmin(host, port, storeCreds)) {
                        return;
                    }
                }
            }

            /* Try anonymous login if user name is not specified. */
            if (adminLoginUser == null) {
                if (loginAdmin(host, port, null /* anonymous loginCreds */)) {
                    return;
                }
                echo("Could not login as anonymous: " +
                     lastAdminAfe.getMessage() + eol);
            }

            /* Login using explicit username and login file. */
            try {
                if (!loginAdmin(host, port,
                    adminLogin.makeShellLoginCredentials())) {
                    throw new ShellException(
                        "Login failed: " + lastAdminAfe.getMessage(),
                        lastAdminAfe);
                }
            } catch (IOException ioe) {
                throw new ShellException("Failed to get login credentials: " +
                                         ioe.getMessage());
            } catch (IllegalArgumentException iae) {
                throw new ShellException("Login properties error: " +
                                         iae.getMessage());
            }
        }

        /**
         * Tries to login to admin using specified credentials.
         *
         * @param hostname the host name of the admin service.
         * @param registryPort the registry port of the admin service.
         * @param loginCreds login credentials. If null, anonymous login will
         * be tried.
         * @return true if and only if login successfully
         * @throws ShellException if fail to connect to admin login service.
         */
        private boolean loginAdmin(String hostname,
                                   int registryPort,
                                   final LoginCredentials loginCreds)
            throws ShellException {

            final String userName =
                (loginCreds == null) ? null : loginCreds.getUsername();
            final AdminLoginManager alm =
                new AdminLoginManager(userName, true);

            try {
                if (alm.bootstrap(hostname, registryPort,
                                  loginCreds)) {
                    adminLoginMgr = alm;
                    echo("Logged in admin as " +
                         Objects.toString(userName, "anonymous") + eol);
                    return true;
                }
                throw new ShellException(
                    String.format(loginConnErrMsg, hostname, registryPort));
            } catch (PasswordExpiredException pee) {
                if (userName != null) {
                    echo(pee.getMessage() + eol);

                    if (loginCreds instanceof PasswordCredentials &&
                        renewPassword(alm, hostname,
                                      registryPort,
                                      (PasswordCredentials)loginCreds)) {
                        adminLoginMgr = alm;
                        echo("Logged in admin as " + userName + eol);
                        return true;
                    }
                }
                lastAdminAfe = pee;
                adminLoginMgr = null;
                return false;
            } catch (AuthenticationFailureException afe) {
                lastAdminAfe = afe;
                adminLoginMgr = null;
                return false;
            }
        }

        boolean isSecuredAdmin() {
            return isSecuredAdmin;
        }

        private boolean renewPassword(AdminLoginManager alm,
                                      String hostname,
                                      int registryPort,
                                      final PasswordCredentials oldCreds)
            throws ShellException {
            try {
                /* Prompt for password renewal */
                final PasswordReader READER = new ShellPasswordReader();
                final char[] newPlainPassword =
                    READER.readPassword("Enter the new password: ");
                final String errorMsg =
                    CommandUtils.verifyPassword(READER, newPlainPassword);

                if (errorMsg != null) {
                    throw new ShellException(
                        "Renew password failed: " + errorMsg);
                }

                return alm.renewPassword(hostname, registryPort,
                                         oldCreds, newPlainPassword);
            } catch (IOException ioe) {
                throw new ShellException(
                    "Could not read password from console: " + ioe);
            }
        }

        private boolean isAnonymousLogin() {
            return isSecuredAdmin &&
                adminLogin.getUserName() == null &&
                adminLoginMgr != null &&
                storeLogin.getUserName() == null;
        }
    }

    public boolean isAnonymousLogin() {
        return loginHelper.isAnonymousLogin();
    }

    public boolean isSecuredAdmin() {
        return loginHelper.isSecuredAdmin();
    }
}
