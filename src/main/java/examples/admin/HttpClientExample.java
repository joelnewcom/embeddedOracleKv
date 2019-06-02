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

package admin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.util.Base64;
import java.util.Scanner;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

/**
 * Example of client application sending HTTP/HTTPS request to admin web
 * service. Before running this example, a KVLite instance need to start up
 * with hosted admin web service.
 *
 * Command to start up a secured KVLite:
 * <pre><code>
 *     java -jar kvstore.jar kvlite -admin-web-port 5001 \
 *         -host localhost -root /tmp/kvroot
 * </code></pre>
 * Output information of KVLite:
 * <pre><code>
 * Generated password for user admin:PASSWORD
 * User login file: ./kvroot/security/user.security
 * Created new kvlite store with args:
 * -root /tmp/kvroot -store kvstore -host localhost -port 5000 \
 *     -admin-web-port 5001 -secure-config enable
 * </code></pre>
 * In the KVLite output, record the randomly generated PASSWORD text. Go to
 * the example directory of release package, then compile and run this client
 * example as below:
 * <pre><code>
 * javac -classpath .:../lib/kvclient.jar admin/HttpClientExample.java
 * java -classpath .:../lib/kvclient.jar admin.HttpClientExample \
 *     -host localhost -port 5001 -root /tmp/kvroot -user admin -password \
 *     PASSWORD (replace this with the password just recorded)
 * </code></pre>
 * The console output of this example will display the topology information in
 * JSON format.
 */
public class HttpClientExample {

    /**
     * The admin web service host name. Default value is localhost.
     */
    String hostName = "localhost";

    /**
     * The admin web service port number. Default value is 5001.
     */
    String hostPort = "5001";

    /**
     * The absolute root directory where KVLite create the service. Default
     * value is "/tmp/kvroot".
     */
    String kvroot = "/tmp/kvroot";

    /**
     * User created when started secured KVLite.
     */
    String user = "admin";

    /**
     * Password used to access the secured KVLite. If this field is not set,
     * the program will assume accessing a non-secured KVLite for the
     * operation.
     */
    String password = null;

    public static void main(String args[]) throws Exception {
        try {
            final HttpClientExample example = new HttpClientExample(args);
            example.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public HttpClientExample (String args[]) {

        final int nArgs = args.length;
        int argc = 0;

        while (argc < nArgs) {
            final String thisArg = args[argc++];

            if (thisArg.equals("-host")) {
                if (argc < nArgs) {
                    hostName = args[argc++];
                }
            } else if (thisArg.equals("-port")) {
                if (argc < nArgs) {
                    hostPort = args[argc++];
                }
            } else if (thisArg.equals("-root")) {
                if (argc < nArgs) {
                    kvroot = args[argc++];
                }
            } else if (thisArg.equals("-user")) {
                if (argc < nArgs) {
                    user = args[argc++];
                }
            } else if (thisArg.equals("-password")) {
                if (argc < nArgs) {
                    password = args[argc++];
                }
            } else {
                usage("Unknown argument: " + thisArg);
            }
        }
    }

    private void usage(String message) {
        System.out.println("\n" + message + "\n");
        System.out.println("usage: " + getClass().getName());
        System.out.println("\t-host <host name> (default: localhost) " +
                           "-port <port number> (default: 5000)" +
                           "-root <root path> (default: /tmp/kvroot)" +
                           "-user <user name> (default: admin)" +
                           "-password <password> (If not specify, " +
                           "use http without login)");
        System.exit(1);
    }

    public void run() throws Exception {

        /* For running show topology command */
        final String commandPath = "/V0/nosql/admin/topology";
        /*
         * User can also use external json library to parse a json object
         * to a json string
         */
        final String jsonPayload =
            "{\"command\" : \"show\"}";

        /* HTTPS access secured KVLite */
        if (password != null && !password.isEmpty()) {
            runShowTopology(createHttpsConn(commandPath), jsonPayload);
            return;
        }

        /* HTTP access non-secured KVLite */
        runShowTopology(createHttpConn(commandPath), jsonPayload);
    }

    private void runShowTopology(HttpURLConnection urlConn, String json)
        throws Exception {

        /* Show topology require POST HTTP method */
        urlConn.setRequestMethod("POST");

        /* Send the JSON in HTTP payload */
        urlConn.setDoOutput(true);
        final DataOutputStream output =
                new DataOutputStream(urlConn.getOutputStream());
        output.writeBytes(json);
        output.close();

        /* Read the show topology result */
        final DataInputStream input =
            new DataInputStream(urlConn.getInputStream());
        final StringBuffer showTopoResult = new StringBuffer();
        int readChar = input.read();
        while (readChar != -1) {
            showTopoResult.append((char)readChar);
            readChar = input.read();
        }
        input.close();

        /* Display the show topology result */
        System.out.println(showTopoResult);
    }

    /*
     * Create HTTP connection for target command.
     */
    private HttpURLConnection createHttpConn(String commandPath)
        throws Exception {
        final URL url =
            new URL("http://" + hostName + ":" + hostPort + commandPath);
        final HttpURLConnection urlConn =
            (HttpURLConnection) url.openConnection();
        return urlConn;
    }

    /*
     * Create the HTTPS connection, login to obtain token, then inject the
     * token in request header for target command.
     */
    private HttpURLConnection createHttpsConn(String commandPath)
        throws Exception {

        /* Generate the login URL */
        final String loginAPI = "/V0/nosql/admin/login";
        URL url = new URL("https://" + hostName + ":" + hostPort + loginAPI);

        /* Load the client trust file for HTTPS */
        final String trustFileLocation = kvroot + "/security/client.trust";
        final InputStream is = new FileInputStream(trustFileLocation);
        final String trustStoreType = "JKS";
        final KeyStore ts = KeyStore.getInstance(trustStoreType);
        ts.load(is, null);
        final String algorithm = "SunX509";
        final TrustManagerFactory tmf =
            TrustManagerFactory.getInstance(algorithm);
        tmf.init(ts);

        /* Create SSL context for HTTPS */
        final String protocol = "TLS";
        SSLContext sslContext = SSLContext.getInstance(protocol);
        sslContext.init(null,tmf.getTrustManagers(),null);
        HttpsURLConnection.setDefaultSSLSocketFactory(
            sslContext.getSocketFactory());

        /* Create an all host valid verifier */
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            @Override
            public boolean verify(String host, SSLSession sess) {
                return true;
            }
        };

        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        HttpsURLConnection urlConn = (HttpsURLConnection) url.openConnection();

        /* Set basic authentication information */
        final String encoded =
            Base64.getEncoder().encodeToString(
                (user + ":" + password).getBytes());
        urlConn.setRequestProperty("Authorization", "Basic " + encoded);

        /* Read the request result */
        final Scanner scanner = new Scanner(urlConn.getInputStream());

        /*
         * Extract the login token.
         * User can also use an external json library to parse a string to
         * json object ro read the json field
         */
        String token = null;
        while (scanner.hasNext()) {
            final String lineText = scanner.nextLine();
            if (lineText.contains("token")) {
                final String tokenValue = lineText.split(":")[1];
                token =
                    tokenValue.substring(tokenValue.indexOf("\"") + 1,
                                         tokenValue.lastIndexOf("\""));
            }
        }
        scanner.close();

        if (token == null) {
            throw new IllegalStateException("Cannot find login token");
        }

        /* Open a new connection for target command */
        url = new URL("https://" + hostName + ":" + hostPort + commandPath);
        urlConn = (HttpsURLConnection) url.openConnection();
        /* Inject login token in request header */
        urlConn.setRequestProperty("Authorization", "Bearer " + token);
        return urlConn;
    }
}
