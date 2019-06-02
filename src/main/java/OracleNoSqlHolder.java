import java.util.*;

public class OracleNoSqlHolder {

    private static Map<UUID, OracleNoSqlManager> oracleNoSqlManagers = new HashMap();

    public static void addOracleNoSqlManager(OracleNoSqlManager oracleNoSqlManager) throws InterruptedException {
        if (oracleNoSqlManagers.containsKey(oracleNoSqlManager.getUUID())) {
            oracleNoSqlManager.stop();
            throw new IllegalStateException("There is already a registered Oracle NoSqlServer with id: " + oracleNoSqlManager.getUUID());
        }
        oracleNoSqlManagers.putIfAbsent(oracleNoSqlManager.getUUID(), oracleNoSqlManager);
    }

    public static OracleNoSqlManager getOracleNoSqlManager(UUID uuid) {
        if (!oracleNoSqlManagers.containsKey(uuid)) {
            throw new IllegalStateException("Could not find a registered oracle NoSqlServer by id: " + uuid);
        }
        return oracleNoSqlManagers.get(uuid);
    }

}
