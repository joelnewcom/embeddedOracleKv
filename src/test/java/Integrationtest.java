import oracle.kv.*;
import oracle.kv.table.TableAPI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class Integrationtest {

    private ClassLoader classLoader = getClass().getClassLoader();
    private static UUID serverId = UUID.randomUUID();

    @BeforeClass
    public static void initDB() throws InterruptedException {
        OracleNoSqlManager oracleNoSqlManagerImpl = OracleNoSqlManagerImpl.build(serverId);
        OracleNoSqlHolder.addOracleNoSqlManager(oracleNoSqlManagerImpl);
    }

    @AfterClass
    public static void stopOracleDB() throws InterruptedException {
        OracleNoSqlHolder.getOracleNoSqlManager(serverId).stop();
    }

    @Test
    public void testDB() {

        OracleNoSqlManager oracleNoSqlManager = OracleNoSqlHolder.getOracleNoSqlManager(serverId);


        TableAPI tableAPI = oracleNoSqlManager.getTableApi();
//        List<String> operations = getDatabaseDDL(SCHEMA_INSERT_TABLES_DDL);

        //StatementResult result = kvstore.;
        //displayResult(result, schema);

    }

    private List<String> getDatabaseDDL(String filePath) {
        List<String> keyWords = Arrays.asList("DROP", "CREATE", "ALTER");
        String schema = getContentOfFile(filePath);
        return new ArrayList<>();

//        String[] splitted = schema.

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

    private KVStore getKvStoreSynchron(int maxFaultAttemptsTillGiveup) throws InterruptedException {
        boolean isKvStoreOffline = true;
        int faultyAttemptCounter = 0;
        KVStore kvstore = null;
        while (isKvStoreOffline && (faultyAttemptCounter < maxFaultAttemptsTillGiveup)) {
            try {
                String[] hhosts = {"localhost:5000"};
                kvstore = KVStoreFactory.getStore(new KVStoreConfig("kvstore", hhosts));
                isKvStoreOffline = false;
            } catch (FaultException fe) {
                System.out.println("FaultyAttempts: " + faultyAttemptCounter++);
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

    private String getContentOfFile(String fileName) {

        StringBuilder result = new StringBuilder("");

        //Get file from resources folder

        File file = new File(classLoader.getResource(fileName).getFile());

        try (Scanner scanner = new Scanner(file)) {

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                result.append(line).append("\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result.toString();

    }
}
