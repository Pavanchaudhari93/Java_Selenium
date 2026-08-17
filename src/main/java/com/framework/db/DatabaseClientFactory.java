package com.framework.db;

import java.util.EnumMap;
import java.util.Map;

import com.framework.db.strategy.IDBConnectionStrategy;
import com.framework.db.strategy.MSSQLConnectionStrategy;
import com.framework.db.strategy.MySQLConnectionStrategy;
import com.framework.db.strategy.OracleConnectionStrategy;
import com.framework.db.strategy.PostgreSQLConnectionStrategy;

/**
 * Factory Pattern: creates the correct {@link IDatabaseClient} for a given {@link DBType}.
 * JDBC-based types are all wired to the same {@link JdbcDatabaseClient} executor, only the
 * connection {@link IDBConnectionStrategy} differs (Strategy Pattern); MongoDB gets its own
 * client since it isn't JDBC-based, but still returns the common {@link IDatabaseClient} type.
 */
public final class DatabaseClientFactory {

    private static final Map<DBType, IDBConnectionStrategy> JDBC_STRATEGIES = new EnumMap<>(DBType.class);

    static {
        JDBC_STRATEGIES.put(DBType.MYSQL, new MySQLConnectionStrategy());
        JDBC_STRATEGIES.put(DBType.ORACLE, new OracleConnectionStrategy());
        JDBC_STRATEGIES.put(DBType.POSTGRESQL, new PostgreSQLConnectionStrategy());
        JDBC_STRATEGIES.put(DBType.MSSQL, new MSSQLConnectionStrategy());
    }

    private DatabaseClientFactory() {
    }

    public static IDatabaseClient createClient(DBType dbType) {
        if (dbType == DBType.MONGODB) {
            return new MongoDatabaseClient();
        }
        IDBConnectionStrategy strategy = JDBC_STRATEGIES.get(dbType);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
        return new JdbcDatabaseClient(strategy);
    }

    /** Convenience overload: creates the client and immediately connects it. */
    public static IDatabaseClient createAndConnect(DBConfig config) {
        IDatabaseClient client = createClient(config.getDbType());
        client.connect(config);
        return client;
    }
}
