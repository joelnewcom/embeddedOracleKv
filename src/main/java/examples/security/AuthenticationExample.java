/*-
 * Copyright (C) 2011, 2019 Oracle and/or its affiliates. All rights reserved.
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

package security;

import java.util.Properties;

import oracle.kv.AuthenticationFailureException;
import oracle.kv.KVSecurityConstants;
import oracle.kv.KVStore;
import oracle.kv.KVStoreConfig;
import oracle.kv.KVStoreFactory;
import oracle.kv.Key;
import oracle.kv.PasswordCredentials;
import oracle.kv.ReauthenticateHandler;
import oracle.kv.Value;
import oracle.kv.ValueVersion;

/**
 * An extremely simple KVStore client application that authenticates access to a
 * security-enabled KVStore instance, and then writes and reads a single record.
 * It can be used to validate a secure KVStore installation.
 * 
 * <p>
 * When accessing a secure store, there are security properties that must be
 * specified to guide NoSQL client code in accessing the store. There are 2
 * basic approaches for doing this in an application.
 * 
 * <p>
 * The first approach is to set the <code>oracle.kv.security</code> system
 * property to reference a security properties file on stored disk. In that
 * approach, the properties are automatically loaded from the file and applied
 * to KVStoreConfig objects. That approach is simple, but requires the security
 * properties to be stored in a file on disk.
 * 
 * <p>
 * The second basic approach, which is used by this example, is to directly set
 * security properties using {@link KVStoreConfig#setSecurityProperties}. This
 * example has hard-coded support for specific properties, but an application
 * could also load properties from a security properties file using one of the
 * load() methods defined on {@link Properties}.
 * 
 * <p>
 * Before running this example program, start a security-enabled KVStore
 * instance. Use the KVStore instance name, host, port, client.trust, and
 * user/password for running this program, as follows:
 * 
 * <pre>
 * java security.AuthenticationExample -store &lt;instance name&gt; &#92;
 *                                     -host  &lt;host name&gt;     &#92;
 *                                     -port  &lt;port number&gt;   &#92;
 *                                     -trust &lt;trust store&gt;   &#92;
 *                                     -user  &lt;login user&gt;    &#92;
 *                                     -pwd   &lt;password&gt;
 * </pre>
 * 
 * The default instance name is kvstore, the default host name is localhost and
 * the default port number is 5000. You may consult your database administrator
 * for the truststore file which is the client.trust file that was generated by
 * makebootconfig or securityconfig when configuring security for the KVStore
 * instance, as well as the user/password for accessing the database.
 * 
 * <p>
 * Notice that including passwords on a command line is not good security
 * practice, since it will be visible to other users on the system using
 * commands such as "ps", and that production programs should consider steps to
 * hide the password from other users.
 */
public class AuthenticationExample {

    private KVStore store;

    /**
     * Runs the HelloBigDataWorld command line program.
     */
    public static void main(String args[]) {
        try {
            AuthenticationExample example = new AuthenticationExample(args);
            example.runExample();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses command line args and opens the KVStore.
     */
    AuthenticationExample(String[] argv) {

        String storeName = "kvstore";
        String hostName = "localhost";
        String hostPort = "5000";
        String trustStore = null;
        String user = null;
        String password = null;

        final int nArgs = argv.length;
        int argc = 0;

        while (argc < nArgs) {
            final String thisArg = argv[argc++];

            if (thisArg.equals("-store")) {
                if (argc < nArgs) {
                    storeName = argv[argc++];
                } else {
                    usage("-store requires an argument");
                }
            } else if (thisArg.equals("-host")) {
                if (argc < nArgs) {
                    hostName = argv[argc++];
                } else {
                    usage("-host requires an argument");
                }
            } else if (thisArg.equals("-port")) {
                if (argc < nArgs) {
                    hostPort = argv[argc++];
                } else {
                    usage("-port requires an argument");
                }
            } else if (thisArg.equals("-trust")) {
                if (argc < nArgs) {
                    trustStore = argv[argc++];
                } else {
                    usage("-trust requires an argument");
                }
            } else if (thisArg.equals("-user")) {
                if (argc < nArgs) {
                    user = argv[argc++];
                } else {
                    usage("-user requires an argument");
                }
            } else if (thisArg.equals("-pwd")) {
                if (argc < nArgs) {
                    password = argv[argc++];
                } else {
                    usage("-pwd requires an argument");
                }
            } else {
                usage("Unknown argument: " + thisArg);
            }
        }
        
        /* Make sure the required arguments are set */
        if (trustStore == null) {
            usage("-trust is required");
        } else if (user == null) {
            usage("-user is required");
        } else if (password == null) {
            usage("-pwd is required");
        }

        /*
         * The property settings shown here should generally match the property
         * settings in the client.security file generated by makebootconfig and
         * securityconfig. An application may also choose to use
         * Properties.load() to read them from an external file, or may set the
         * oracle.kv.security system property to cause them to automatically be
         * read from a file.
         */
        final Properties securityProps = new Properties();
        securityProps.setProperty(KVSecurityConstants.TRANSPORT_PROPERTY,
                KVSecurityConstants.SSL_TRANSPORT_NAME);
        securityProps.setProperty
            (KVSecurityConstants.SSL_TRUSTSTORE_FILE_PROPERTY, trustStore);

        final KVStoreConfig kvConfig =
            new KVStoreConfig(storeName, hostName + ":" + hostPort);
        kvConfig.setSecurityProperties(securityProps);

        /*
         * For simplicity, password is supplied from user input and stored in
         * memory. This may have security risks. A more secure way would be
         * using the pwdfile or oracle wallet, which is beyond the scope of this
         * example.
         */
        @SuppressWarnings("null")
        final PasswordCredentials loginCreds =
            new PasswordCredentials(user, password.toCharArray());

        try {

            /*
             * An application can encounter an expired authentication session at
             * any point in its lifetime, so robust code that must remain
             * running should always be written to respond to authentication
             * session expirations.
             */
            store = KVStoreFactory.getStore
                (kvConfig, loginCreds,
                 new ReauthenticateHandler() {
                     /*
                      * Automatically performs reauthentication so there's no
                      * subsequent need to retry operations by catching
                      * AuthenticationRequiredException.
                      */
                     @Override
                    public void reauthenticate(KVStore kstore) {
                         kstore.login(loginCreds);
                     }
                 });
        } catch (AuthenticationFailureException afe) {
            /*
             * Could potentially retry the login, possibly with different
             * credentials, but in this simple example, we just fail the
             * attempt.
             */
            usage("Authentication failed!");
        }
    }

    private void usage(String message) {
        System.out.println("\n" + message + "\n");
        System.out.println("usage: " + getClass().getName());
        System.out.println("\t-store <instance name> (default: kvstore) " +
                           "-host <host name> (default: localhost) " +
                           "-port <port number> (default: 5000) " +
                           "-trust <trust store> (required) " +
                           "-user <login user> (require) " +
                           "-pwd <password> (required)");
        System.exit(1);
    }

    /**
     * Performs example operations and closes the KVStore.
     */
    void runExample() {

        final String keyString = "Hello";
        final String valueString = "Big Data World!";

        store.put(Key.createKey(keyString),
                  Value.createValue(valueString.getBytes()));

        final ValueVersion valueVersion = store.get(Key.createKey(keyString));

        System.out.println(keyString + " " +
                           new String(valueVersion.getValue().getValue()));

        store.close();
    }
}
