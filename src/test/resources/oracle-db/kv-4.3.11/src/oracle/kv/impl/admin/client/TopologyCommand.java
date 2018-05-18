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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import oracle.kv.impl.admin.CommandResult;
import oracle.kv.impl.admin.CommandResult.CommandSucceeds;
import oracle.kv.impl.admin.CommandResult.CommandWarns;
import oracle.kv.impl.admin.CommandServiceAPI;
import oracle.kv.impl.admin.param.Parameters;
import oracle.kv.impl.admin.topo.TopologyCandidate;
import oracle.kv.impl.topo.DatacenterId;
import oracle.kv.impl.topo.DatacenterType;
import oracle.kv.impl.topo.RepNodeId;
import oracle.kv.impl.topo.StorageNodeId;
import oracle.kv.impl.util.TopologyPrinter;
import oracle.kv.util.shell.CommandWithSubs;
import oracle.kv.util.shell.Shell;
import oracle.kv.util.shell.ShellException;

/*
 * Subcommands of topology
 */
class TopologyCommand extends CommandWithSubs {

    private static final List<? extends SubCommand> subs = Arrays.asList(
        new TopologyChangeRFSub(),              /* change-repfactor */
        new TopologyChangeZoneTypeSub(),        /* change-zone-type */
        new TopologyCloneSub(),                 /* clone */
        new TopologyContractSub(),              /* contract */
        new TopologyCreateSub(),                /* create */
        new TopologyDeleteSub(),                /* delete */
        new TopologyListSub(),                  /* list */
        new TopologyMoveRNSub(),                /* move-repnode */
        new TopologyPreviewSub(),               /* preview */
        new TopologyRebalanceSub(),             /* rebalance */
        new TopologyRedistributeSub(),          /* redistribute */
        new TopologyValidateSub(),              /* validate */
        new TopologyViewSub(),                  /* view */
        new TopologyChangeZoneArbitersSub()     /* change-zone-arbiters */
        );

    private static final String TOPOLOGY_COMMAND_NAME = "topology";

    /**
     * A warning message to be displayed when commands attempt to create
     * topology candidates whose names contain the substring reserved for
     * system use.
     */
    public static final String RESERVED_CANDIDATE_NAME_WARNING =
        "Warning: The topology candidate name contains '" +
        TopologyCandidate.RESERVED_SUBSTRING +
        "', which is reserved for" + eol + " system use." + eol + eol;

    TopologyCommand() {
        super(subs,
              TOPOLOGY_COMMAND_NAME,
              4,  /* prefix length */
              0); /* min args -- let subs control it */
    }

    @Override
    protected String getCommandOverview() {
        return "Encapsulates commands that manipulate store topologies." + eol +
            "Examples are " +
            "redistribution/rebalancing of nodes or changing replication" +
            eol + "factor.  Topologies are created and modified using this " +
            "command.  They" + eol + "are then deployed by using the " +
            "\"plan deploy-topology\" command.";
    }

    static class TopologyChangeRFSub extends SubCommand {

        final static String dcFlagsDeprecation =
            "The -dc and -dcname flags, and the dc<ID> ID format, are" +
            " deprecated" + eol +
            "and have been replaced by -zn, -znname, and zn<ID>." +
            eol + eol;

        TopologyChangeRFSub() {
            super("change-repfactor", 8);
        }

        @Override
        public String execute(String[] args, Shell shell)
            throws ShellException {

            Shell.checkHelp(args, this);
            CommandShell cmd = (CommandShell)shell;
            CommandServiceAPI cs = cmd.getAdmin();
            String topoName = null;
            String poolName = null;
            DatacenterId dcid = null;
            String dcName = null;
            int rf = 0;
            boolean deprecatedDcFlag = false;
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                if ("-name".equals(arg)) {
                    topoName = Shell.nextArg(args, i++, this);
                } else if ("-pool".equals(arg)) {
                    poolName = Shell.nextArg(args, i++, this);
                } else if (CommandUtils.isDatacenterIdFlag(arg)) {
                    dcid = parseDatacenterId(Shell.nextArg(args, i++, this));
                    if (CommandUtils.isDeprecatedDatacenterId(arg, args[i])) {
                        deprecatedDcFlag = true;
                    }
                } else if (CommandUtils.isDatacenterNameFlag(arg)) {
                    dcName = Shell.nextArg(args, i++, this);
                    if (CommandUtils.isDeprecatedDatacenterName(arg)) {
                        deprecatedDcFlag = true;
                    }
                } else if ("-rf".equals(arg)) {
                    String rfString = Shell.nextArg(args, i++, this);
                    rf = parseUnsignedInt(rfString);

                    /* this is more for typos than actual validation */
                    if (rf > 30) {
                        return "Replication factor out of valid range: " + rf;
                    }
                } else {
                    shell.unknownArgument(arg, this);
                }
            }
            if (topoName == null || poolName == null || rf == 0 ||
                (dcid == null && dcName == null)) {
                shell.requiredArg(null, this);
            }
            final String deprecatedDcFlagPrefix =
                !deprecatedDcFlag ? "" : dcFlagsDeprecation;
            try {
                if (dcid == null) {
                    dcid = CommandUtils.getDatacenterId(dcName, cs, this);
                } else {
                    CommandUtils.ensureDatacenterExists(dcid, cs, this);
                }
                CommandUtils.validatePool(poolName, cs, this);
                CommandUtils.ensureTopoExists(topoName, cs, this);
                CommandUtils.validateRepFactor(dcid, rf, cs, this);
                return deprecatedDcFlagPrefix +
                    cs.changeRepFactor(topoName, poolName, dcid, rf);
            } catch (RemoteException re) {
                cmd.noAdmin(re);
            }
            return "";
        }

        @Override
        protected String getCommandSyntax() {
            return "topology change-repfactor -name <name> -pool " +
                "<pool name>" + eolt + "-zn <id> | -znname <name> -rf " +
                "<replication factor>";
        }

        @Override
        protected String getCommandDescription() {
            return
                "Modifies the topology to change the replication factor of " +
                "the specified" + eolt + "zone to a new value.  The " +
                "replication factor may not be" + eolt + "decreased at " +
                "this time.";
        }
    }

    static class TopologyChangeZoneTypeSub extends SubCommand {

        TopologyChangeZoneTypeSub() {
            super("change-zone-type", 13);
        }

        @Override
        public String execute(String[] args, Shell shell)
            throws ShellException {

            Shell.checkHelp(args, this);
            final CommandShell cmd = (CommandShell)shell;
            final CommandServiceAPI cs = cmd.getAdmin();
            String topoName = null;
            DatacenterId dcid = null;
            DatacenterType type = null;
            String dcName = null;
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                if ("-name".equals(arg)) {
                    topoName = Shell.nextArg(args, i++, this);
                } else if (CommandUtils.isDatacenterIdFlag(arg)) {
                    dcid = parseDatacenterId(Shell.nextArg(args, i++, this));
                } else if (CommandUtils.isDatacenterNameFlag(arg)) {
                    dcName = Shell.nextArg(args, i++, this);
                } else if ("-type".equals(arg)) {
                    final String typeValue = Shell.nextArg(args, i++, this);
                    type = parseDatacenterType(typeValue);
                } else {
                    shell.unknownArgument(arg, this);
                }
            }
            if (topoName == null || type == null ||
                (dcid == null && dcName == null)) {
                shell.requiredArg(null, this);
            }

            try {
                if (dcid == null) {
                    dcid = CommandUtils.getDatacenterId(dcName, cs, this);
                } else {
                    CommandUtils.ensureDatacenterExists(dcid, cs, this);
                }
                CommandUtils.ensureTopoExists(topoName, cs, this);
                return cs.changeZoneType(topoName, dcid, type);
            } catch (RemoteException re) {
                cmd.noAdmin(re);
            }
            return "";
        }

        @Override
        protected String getCommandSyntax() {
            return "topology change-zone-type -name <name> " + eolt +
                "{-zn <id> | -znname <name>} -type {primary | secondary}";
        }

        @Override
        protected String getCommandDescription() {
            return
                "Modifies the topology to change the type of " +
                "the specified" + eolt + "zone to a new type.";
        }
    }

    @SuppressWarnings("null")
    static class TopologyChangeZoneArbitersSub extends SubCommand {

        TopologyChangeZoneArbitersSub() {
            super("change-zone-arbiters", 13);
        }

        @Override
        public String execute(String[] args, Shell shell)
            throws ShellException {

            Shell.checkHelp(args, this);
            final CommandShell cmd = (CommandShell)shell;
            final CommandServiceAPI cs = cmd.getAdmin();
            String topoName = null;
            DatacenterId dcid = null;
            Boolean allowArbiters = null;
            String dcName = null;
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                if ("-name".equals(arg)) {
                    topoName = Shell.nextArg(args, i++, this);
                } else if (CommandUtils.isDatacenterIdFlag(arg)) {
                    dcid = parseDatacenterId(Shell.nextArg(args, i++, this));
                } else if (CommandUtils.isDatacenterNameFlag(arg)) {
                    dcName = Shell.nextArg(args, i++, this);
                } else if ("-arbiters".equals(arg)) {
                    allowArbiters = true;
                } else if ("-no-arbiters".equals(arg)) {
                   allowArbiters = false;
                } else {
                    shell.unknownArgument(arg, this);
                }
            }
            if (topoName == null || allowArbiters == null ||
                (dcid == null && dcName == null)) {
                shell.requiredArg(null, this);
            }

            try {
                if (dcid == null) {
                    dcid = CommandUtils.getDatacenterId(dcName, cs, this);
                } else {
                    CommandUtils.ensureDatacenterExists(dcid, cs, this);
                }
                CommandUtils.ensureTopoExists(topoName, cs, this);
                return cs.changeZoneArbiters(topoName, dcid, allowArbiters);
            } catch (RemoteException re) {
                cmd.noAdmin(re);
            }
            return "";
        }

        @Override
        protected String getCommandSyntax() {
            return "topology change-zone-arbiters -name <name> " + eolt +
                "{-zn <id> | -znname <name>}  {-arbiter | -no-arbiter}";
        }

        @Override
        protected String getCommandDescription() {
            return
                "Modifies the topology to change the arbiter attribute of " +
                "the specified" + eolt + "zone.";
        }
    }

    static class TopologyCloneSub extends SubCommand {

        TopologyCloneSub() {
            super("clone", 3);
        }

        @Override
        public String execute(String[] args, Shell shell)
            throws ShellException {

            Shell.checkHelp(args, this);
            CommandShell cmd = (CommandShell)shell;
            CommandServiceAPI cs = cmd.getAdmin();
            String topoName = null;
            String fromName = null;
            boolean isCurrent = false;
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                if ("-name".equals(arg)) {
                    topoName = Shell.nextArg(args, i++, this);
                } else if ("-from".equals(arg)) {
                    fromName = Shell.nextArg(args, i++, this);
                } else if ("-current".equals(arg)) {
                    isCurrent = true;
                } else {
                    shell.unknownArgument(arg, this);
                }
            }
            if (topoName == null || (fromName == null && !isCurrent)) {
                shell.requiredArg(null, this);
                throw new AssertionError("Not reached");
            }

            final String reservedWarning =
                topoName.contains(TopologyCandidate.RESERVED_SUBSTRING) ?
                RESERVED_CANDIDATE_NAME_WARNING : "";
            try {
                if (isCurrent) {
                    return reservedWarning + cs.copyCurrentTopology(topoName);
                }
                CommandUtils.ensureTopoExists(fromName, cs, this);
                return reservedWarning + cs.copyTopology(fromName, topoName);
            } catch (RemoteException re) {
                cmd.noAdmin(re);
                throw new AssertionError("Not reached");
            }
        }

        @Override
        protected String getCommandSyntax() {
            return "topology clone -from <from topology> -name " +
                "<to topology> or "+
                eolt + "topology clone -current -name <toTopology>";
        }

        @Override
        protected String getCommandDescription() {
            return
                "Clones an existing topology so as to create a new " +
                "candidate topology " + eolt +
                "to be used for topology change operations.";
        }
    }

    static class TopologyCreateSub extends SubCommand {

        TopologyCreateSub() {
            super("create", 3);
        }

        @Override
        public String execute(String[] args, Shell shell)
            throws ShellException {

            Shell.checkHelp(args, this);
            CommandShell cmd = (CommandShell)shell;
            CommandServiceAPI cs = cmd.getAdmin();
            String topoName = null;
            String poolName = null;
            int numPartitions = 0;
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                if ("-name".equals(arg)) {
                    topoName = Shell.nextArg(args, i++, this);
                } else if ("-pool".equals(arg)) {
                    poolName = Shell.nextArg(args, i++, this);
                } else if ("-partitions".equals(arg)) {
                    String partString = Shell.nextArg(args, i++, this);
                    numPartitions = parseUnsignedInt(partString);
                } else {
                    shell.unknownArgument(arg, this);
                }
            }
            if (topoName == null || poolName == null || numPartitions == 0) {
                shell.requiredArg(null, this);
                throw new AssertionError("Not reached");
            }

            String returnValue = "";
            try {
                CommandUtils.validatePool(poolName, cs, this);
                returnValue = cs.createTopology(topoName, poolName,
                                                numPartitions, shell.getJson());
            } catch (RemoteException re) {
                cmd.noAdmin(re);
                throw new AssertionError("Not reached");
            }
            final boolean isReservedName =
                topoName.contains(TopologyCandidate.RESERVED_SUBSTRING);
            if (shell.getJson()) {
                String operation = TOPOLOGY_COMMAND_NAME + " " +
                    getCommandName();
                CommandResult result;
                if (isReservedName) {
                    result = new CommandWarns(RESERVED_CANDIDATE_NAME_WARNING,
                                              returnValue);
                } else {
                    result = new CommandSucceeds(returnValue);
                }
                return Shell.toJsonReport(operation, result);
            }
            if (isReservedName) {
                return RESERVED_CANDIDATE_NAME_WARNING + returnValue;
            }
            return returnValue;
        }

        @Override
        protected String getCommandSyntax() {
            return "topology create -name <candidate name> -pool " +
                "<pool name>" + eolt + "-partitions <num> [-json]";
        }

        @Override
        protected String getCommandDescription() {
            return
                "Creates a new topology with the specified number of " +
                "partitions" + eolt + "using the specified storage pool.";
        }
    }

    static class TopologyDeleteSub extends SubCommand {

        TopologyDeleteSub() {
            super("delete", 3);
        }

        @Override
        public String execute(String[] args, Shell shell)
            throws ShellException {

            Shell.checkHelp(args, this);
            CommandShell cmd = (CommandShell)shell;
            CommandServiceAPI cs = cmd.getAdmin();
            String topoName = null;
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                if ("-name".equals(arg)) {
                    topoName = Shell.nextArg(args, i++, this);
                } else {
                    shell.unknownArgument(arg, this);
                }
            }
            if (topoName == null) {
                shell.requiredArg("-name", this);
            }

            try {
                CommandUtils.ensureTopoExists(topoName, cs, this);
                return cs.deleteTopology(topoName);
            } catch (RemoteException re) {
                cmd.noAdmin(re);
            }
            return "";
        }

        @Override
        protected String getCommandSyntax() {
            return "topology delete -name <name>";
        }

        @Override
        protected String getCommandDescription() {
            return "Deletes a topology.";
        }
    }

    static class TopologyListSub extends SubCommand {

        TopologyListSub() {
            super("list", 3);
        }

        @Override
        public String execute(String[] args, Shell shell)
            throws ShellException {

            Shell.checkHelp(args, this);
            CommandShell cmd = (CommandShell)shell;
            CommandServiceAPI cs = cmd.getAdmin();
            boolean showHidden = false;
            for (int i = 1; i < args.length; i++) {
                final String arg = args[i];
                if ("-hidden".equals(arg)) {
                    showHidden = true;
                } else {
                    shell.unknownArgument(arg, this);
                }
            }
            try {
                List<String> topos = cs.listTopologies();
                Collections.sort(topos);
                StringBuilder sb = new StringBuilder();
                for (String oneTopo : topos) {
                    if (!showHidden &&
                        oneTopo.startsWith(
                            TopologyCandidate.INTERNAL_NAME_PREFIX)) {
                        continue;
                    }
                    sb.append(oneTopo).append(eol);
                }
                return sb.toString();
            } catch (RemoteException re) {
                cmd.noAdmin(re);
            }
            return "";
        }

        @Override
        protected String getCommandSyntax() {
            return "topology list";
        }

        @Override
        protected String getCommandDescription() {
            return "Lists existing topologies.";
        }
    }

    static class TopologyMoveRNSub extends SubCommand {

        TopologyMoveRNSub() {
            super("move-repnode", 4);
        }

        @Override
        protected boolean isHidden() {
            return true;
        }

        @Override
        public String execute(String[] args, Shell shell)
            throws ShellException {

            Shell.checkHelp(args, this);
            CommandShell cmd = (CommandShell)shell;
            CommandServiceAPI cs = cmd.getAdmin();
            String topoName = null;
            RepNodeId rnid = null;
            StorageNodeId snid = null;
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                if ("-name".equals(arg)) {
                    topoName = Shell.nextArg(args, i++, this);
                } else if ("-rn".equals(arg)) {
                    String rnString = Shell.nextArg(args, i++, this);
                    try {
                        rnid = RepNodeId.parse(rnString);
                    } catch (IllegalArgumentException iae) {
                        return "Invalid RepNode id: " + rnString;
                    }
                } else if ("-sn".equals(arg)) {
                    String snString = Shell.nextArg(args, i++, this);
                    try {
                        snid = StorageNodeId.parse(snString);
                    } catch (IllegalArgumentException iae) {
                        return "Invalid StorageNode id: " + snString;
                    }
                } else {
                    shell.unknownArgument(arg, this);
                }
            }
            if (topoName == null || rnid == null) {
                shell.requiredArg(null, this);
            }

            try {
                CommandUtils.ensureTopoExists(topoName, cs, this);
                CommandUtils.ensureRepNodeExists(rnid, cs, this);
                if (snid != null) {
                    CommandUtils.ensureStorageNodeExists(snid, cs, this);
                }
                return cs.moveRN(topoName, rnid, snid);
            } catch (RemoteException re) {
                cmd.noAdmin(re);
            }
            return "";
        }

        @Override
        protected String getCommandSyntax() {
            return "topology move-repnode -name <name> -rn <id>";
        }

        @Override
        protected String getCommandDescription() {
            return
                "Modifies the topology to move the specified RepNode to " +
                "an available" + eolt + "storage node chosen by the system.";
        }
    }

    static class TopologyPreviewSub extends SubCommand {

        TopologyPreviewSub() {
            super("preview", 3);
        }

        @Override
        public String execute(String[] args, Shell shell)
            throws ShellException {

            Shell.checkHelp(args, this);
            CommandShell cmd = (CommandShell)shell;
            CommandServiceAPI cs = cmd.getAdmin();
            String topoName = null;
            String startName = null;
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                if ("-name".equals(arg)) {
                    topoName = Shell.nextArg(args, i++, this);
                } else if ("-start".equals(arg)) {
                    startName = Shell.nextArg(args, i++, this);
                } else {
                    shell.unknownArgument(arg, this);
                }
            }
            if (topoName == null) {
                shell.requiredArg("-name", this);
            }

            try {
                CommandUtils.ensureTopoExists(topoName, cs, this);
                if (startName != null) {
                    CommandUtils.ensureTopoExists(startName, cs, this);
                }
                return cs.preview(topoName, startName, shell.getVerbose());
            } catch (RemoteException re) {
                cmd.noAdmin(re);
            }
            return "";
        }

        @Override
        protected String getCommandSyntax() {
            return "topology preview -name <name> [-start <from topology>]";
        }

        @Override
        protected String getCommandDescription() {
            return
                "Describes the actions that would be taken to transition " +
                "from the " + eolt + "starting topology to the named, target " +
                "topology. If -start is not " + eolt + "specified "  +
                "the current topology is used. This command should be used " +
                eolt +  "before deploying a new topology.";
        }
    }

    static class TopologyRebalanceSub extends SubCommand {

        static final String dcFlagsDeprecation =
            "The -dc and -dcname flags, and the dc<ID> ID format, are" +
            " deprecated" + eol +
            "and have been replaced by -zn, -znname, and zn<ID>." +
            eol + eol;

        TopologyRebalanceSub() {
            super("rebalance", 3);
        }

        @Override
        public String execute(String[] args, Shell shell)
            throws ShellException {

            Shell.checkHelp(args, this);
            CommandShell cmd = (CommandShell)shell;
            CommandServiceAPI cs = cmd.getAdmin();
            String topoName = null;
            String poolName = null;
            DatacenterId dcid = null;
            String dcName = null;
            boolean deprecatedDcFlag = false;
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                if ("-name".equals(arg)) {
                    topoName = Shell.nextArg(args, i++, this);
                } else if ("-pool".equals(arg)) {
                    poolName = Shell.nextArg(args, i++, this);
                } else if (CommandUtils.isDatacenterIdFlag(arg)) {
                    dcid = parseDatacenterId(Shell.nextArg(args, i++, this));
                    if (CommandUtils.isDeprecatedDatacenterId(arg, args[i])) {
                        deprecatedDcFlag = true;
                    }
                } else if (CommandUtils.isDatacenterNameFlag(arg)) {
                    dcName =  Shell.nextArg(args, i++, this);
                    if (CommandUtils.isDeprecatedDatacenterName(arg)) {
                        deprecatedDcFlag = true;
                    }
                } else {
                    shell.unknownArgument(arg, this);
                }
            }
            if (topoName == null || poolName == null) {
                shell.requiredArg(null, this);
            }
            final String deprecatedDcFlagPrefix =
                !deprecatedDcFlag ? "" : dcFlagsDeprecation;
            try {
                CommandUtils.validatePool(poolName, cs, this);
                CommandUtils.ensureTopoExists(topoName, cs, this);
                if (dcName != null) {
                    dcid = CommandUtils.getDatacenterId(dcName, cs, this);
                }
                if (dcid != null) {
                    CommandUtils.ensureDatacenterExists(dcid, cs, this);
                }
                return deprecatedDcFlagPrefix +
                    cs.rebalanceTopology(topoName, poolName, dcid);
            } catch (RemoteException re) {
                cmd.noAdmin(re);
            }
            return "";
        }

        @Override
        protected String getCommandSyntax() {
            return "topology rebalance -name <name> -pool " +
                "<pool name> [-zn <id> | -znname <name>]";
        }

        @Override
        protected String getCommandDescription() {
            return
                "Modifies the named topology to create a \"balanced\" " +
                "topology. If the" + eolt + "optional -zn flag is used " +
                "only storage nodes from the specified" + eolt +
                "zone will be used for the operation.";
        }
    }

    static class TopologyContractSub extends SubCommand {

        TopologyContractSub() {
            super("contract", 3);
        }

        @Override
        public String execute(String[] args, Shell shell)
            throws ShellException {

            Shell.checkHelp(args, this);
            CommandShell cmd = (CommandShell)shell;
            CommandServiceAPI cs = cmd.getAdmin();
            String topoName = null;
            String poolName = null;
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                if ("-name".equals(arg)) {
                    topoName = Shell.nextArg(args, i++, this);
                } else if ("-pool".equals(arg)) {
                    poolName = Shell.nextArg(args, i++, this);
                } else {
                    shell.unknownArgument(arg, this);
                }
            }
            if (topoName == null || poolName == null) {
                shell.requiredArg(null, this);
            }

            String contractInfo = "";
            try {
                CommandUtils.validatePool(poolName, cs, this);
                CommandUtils.ensureTopoExists(topoName, cs, this);
                contractInfo = cs.contractTopology(topoName, poolName);
            } catch (RemoteException re) {
                cmd.noAdmin(re);
            }

            /* Check whether there are snapshots for the topology. */
            try {
                String [] list = cs.listSnapshots(null);

                if (list.length == 0) {
                    return contractInfo;
                }

                String snapshotsList = "";
                for (String ss : list) {
                    snapshotsList += ss + eol;
                }
                String warnMessage = null;
                if (list.length == 1) {
                    warnMessage = "Warning: the following snapshot will " +
                                  "be removed, please backup it:";
                } else {
                    warnMessage = "Warning: the following snapshots will " +
                            "be removed, please backup them:";
                }
                return contractInfo + eol + eol + warnMessage + eol +
                       snapshotsList;

            } catch (RemoteException re) {
                cmd.noAdmin(re);
            } catch (IllegalArgumentException iae) {
                throw new ShellException(iae.getMessage());
            }
            return "";
        }

        @Override
        protected String getCommandSyntax() {
            return "topology contract -name <name> -pool " +
                "<pool name>";
        }

        @Override
        protected String getCommandDescription() {
            return "Modifies the named topology to contract storage nodes.";
        }
    }

    static class TopologyRedistributeSub extends SubCommand {

        TopologyRedistributeSub() {
            super("redistribute", 3);
        }

        @Override
        public String execute(String[] args, Shell shell)
            throws ShellException {

            Shell.checkHelp(args, this);
            CommandShell cmd = (CommandShell)shell;
            CommandServiceAPI cs = cmd.getAdmin();
            String topoName = null;
            String poolName = null;
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                if ("-name".equals(arg)) {
                    topoName = Shell.nextArg(args, i++, this);
                } else if ("-pool".equals(arg)) {
                    poolName = Shell.nextArg(args, i++, this);
                } else {
                    shell.unknownArgument(arg, this);
                }
            }
            if (topoName == null || poolName == null) {
                shell.requiredArg(null, this);
            }

            try {
                CommandUtils.validatePool(poolName, cs, this);
                CommandUtils.ensureTopoExists(topoName, cs, this);
                return cs.redistributeTopology(topoName, poolName);
            } catch (RemoteException re) {
                cmd.noAdmin(re);
            }
            return "";
        }

        @Override
        protected String getCommandSyntax() {
            return "topology redistribute -name <name> -pool " +
                "<pool name>";
        }

        @Override
        protected String getCommandDescription() {
            return
                "Modifies the named topology to redistribute resources " +
                "to more efficiently" + eolt + "use those available.";
        }
    }

    static class TopologyValidateSub extends SubCommand {

        TopologyValidateSub() {
            super("validate", 3);
        }

        @Override
        public String execute(String[] args, Shell shell)
            throws ShellException {

            Shell.checkHelp(args, this);
            CommandShell cmd = (CommandShell)shell;
            CommandServiceAPI cs = cmd.getAdmin();
            String topoName = null;
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                if ("-name".equals(arg)) {
                    topoName = Shell.nextArg(args, i++, this);
                } else {
                    shell.unknownArgument(arg, this);
                }
            }

            try {
                if (topoName != null) {
                    CommandUtils.ensureTopoExists(topoName, cs, this);
                }
                return cs.validateTopology(topoName);
            } catch (RemoteException re) {
                cmd.noAdmin(re);
            }
            return "";
        }

        @Override
        protected String getCommandSyntax() {
            return "topology validate [-name <name>]";
        }

        @Override
        protected String getCommandDescription() {
            return
                "Validates the specified topology. If no name is given, " +
                "the current " + eolt +
                "topology is validated. Validation will generate " +
                "\"violations\" and " + eolt + "\"notes\". Violations are " +
                "issues that can cause problems and should be " + eolt +
                "investigated. Notes are informational and highlight " +
                "configuration " + eolt +
                "oddities that could be potential issues or could be expected.";

        }
    }

    static class TopologyViewSub extends SubCommand {

        TopologyViewSub() {
            super("view", 3);
        }

        @Override
        public String execute(String[] args, Shell shell)
            throws ShellException {

            Shell.checkHelp(args, this);
            CommandShell cmd = (CommandShell)shell;
            CommandServiceAPI cs = cmd.getAdmin();
            String topoName = null;
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                if ("-name".equals(arg)) {
                    topoName = Shell.nextArg(args, i++, this);
                } else {
                    shell.unknownArgument(arg, this);
                }
            }
            if (topoName == null) {
                shell.requiredArg("-name", this);
            }

            try {
                CommandUtils.ensureTopoExists(topoName, cs, this);
                TopologyCandidate tc = cs.getTopologyCandidate (topoName);
                Parameters params = cs.getParameters();
                return TopologyPrinter.printTopology(tc, params,
                                                     shell.getVerbose());
            } catch (RemoteException re) {
                cmd.noAdmin(re);
            }
            return "";
        }

        @Override
        protected String getCommandSyntax() {
            return "topology view -name <name>";
        }

        @Override
        protected String getCommandDescription() {
            return
                "Displays details of the specified topology.";
        }
    }
}
