import oracle.kv.table.TableAPI;

import java.io.IOException;
import java.util.UUID;

public interface OracleNoSqlManager {
    TableAPI getTableApi();
    void stop() throws InterruptedException;
    UUID getUUID();
}
