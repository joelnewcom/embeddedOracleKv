import oracle.kv.*;
import oracle.kv.table.TableAPI;

import java.io.*;
import java.net.ServerSocket;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class OracleNoSqlManagerImpl implements OracleNoSqlManager {
    private static final int TRESHHOLD_FAULT_ATTEMPTS = 15;
    public static final String SCHEMA_INSERT_TABLES_DDL = "schema/insert-tables.ddl";
    private ClassLoader classLoader = getClass().getClassLoader();
    private Process noSqlDb;
    private TableAPI tableAPI;

    private UUID uuid;

    private OracleNoSqlManagerImpl() {
    }

    static OracleNoSqlManagerImpl build(UUID uuid) {
        OracleNoSqlManagerImpl oracleNoSqlManagerImpl = new OracleNoSqlManagerImpl();
        try {
            oracleNoSqlManagerImpl.start();
            oracleNoSqlManagerImpl.setUUID(uuid);

        } catch (InterruptedException e) {

        } catch (IOException e) {
            e.printStackTrace();
        }

        return oracleNoSqlManagerImpl;
    }

    private void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    private void start() throws InterruptedException, IOException {
        final String fullPath = classLoader.getResource("oracle-db/kv-4.3.11/lib/kvstore.jar").getPath().substring(1);
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", fullPath, "kvlite", "-secure-config", "disable");
        noSqlDb = processBuilder.start();
        KVStore kvstore = getKvStoreSynchron();
        runDDLScript();
        tableAPI = kvstore.getTableAPI();
    }

    @Override
    public TableAPI getTableApi() {
        return tableAPI;
    }

    @Override
    public void stop() throws InterruptedException {
        noSqlDb.destroy();
        noSqlDb.waitFor();
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    private void runDDLScript() throws InterruptedException, IOException {
        final String fullPathOfSql = classLoader.getResource("oracle-db/kv-4.3.11/lib/sql.jar").getPath().substring(1);
        final String fullPathOfSchema = classLoader.getResource(SCHEMA_INSERT_TABLES_DDL).getPath().substring(1);
        ProcessBuilder processBuilderOfSql = new ProcessBuilder("java", "-jar", fullPathOfSql, "-helper-hosts", "localhost:5000", "-store", "kvstore", "load", "-file", fullPathOfSchema);

        Process sqlShell = processBuilderOfSql.start();
        OutputStream sqlOutPutStream = sqlShell.getOutputStream();
        while (sqlShell.isAlive()) {
            System.out.print(outputStream(sqlShell.getInputStream()));
        }
        sqlShell.waitFor();
    }


    private void displayResult(StatementResult result, String statement) {
        System.out.println("===========================");
        if (result.isSuccessful()) {
            System.out.println("Statement was successful:\n\t" +
                    statement);
            System.out.println("Results:\n\t" + result.getInfo());
        } else if (result.isCancelled()) {
            System.out.println("Statement was cancelled:\n\t" +
                    statement);
        } else {
            /*
             * statement was not successful: may be in error, or may still
             * be in progress.
             */
            if (result.isDone()) {
                System.out.println("Statement failed:\n\t" + statement);
                System.out.println("Problem:\n\t" +
                        result.getErrorMessage());
            } else {
                System.out.println("Statement in progress:\n\t" +
                        statement);
                System.out.println("Status:\n\t" + result.getInfo());
            }
        }
    }

    /**
     * Tries to connect to a running oracle noSql server.
     *
     * @return the KVStore Handle, if connected successfully
     */
    private KVStore getKvStoreSynchron() throws InterruptedException {
        boolean isKvStoreOffline = true;
        int faultyAttemptCounter = 0;
        KVStore kvstore = null;
        while (isKvStoreOffline && (faultyAttemptCounter < OracleNoSqlManagerImpl.TRESHHOLD_FAULT_ATTEMPTS)) {
            try {
                String[] hhosts = {"localhost:5000"};
                kvstore = KVStoreFactory.getStore(new KVStoreConfig("kvstore", hhosts));
                isKvStoreOffline = false;
            } catch (FaultException fe) {
                System.out.println("Failed connection attempt: " + faultyAttemptCounter++);
            }
            TimeUnit.SECONDS.sleep(1);
        }
        if (isKvStoreOffline) {
            throw new IllegalStateException(format("Kv store no accessable after %s attempts", faultyAttemptCounter));
        }

        System.out.println(format("Connected to kvStore after: %s attempts", faultyAttemptCounter));
        return kvstore;
    }

    // convert InputStream to String
    private static String outputStream(InputStream is) {

        StringBuilder sb = new StringBuilder();

        String line;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is));) {

            while ((line = br.readLine()) != null) {
                System.out.println(line);
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return sb.toString();
    }

    private Integer findRandomOpenPortOnAllLocalInterfaces() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();

        }
    }

}
