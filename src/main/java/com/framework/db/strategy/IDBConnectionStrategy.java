package com.framework.db.strategy;

import java.sql.Connection;
import java.sql.SQLException;

import com.framework.db.DBConfig;

/**
 * Strategy interface: each JDBC-based database (MySQL, Oracle, PostgreSQL, MSSQL)
 * knows how to build its own JDBC URL and open its own {@link Connection}.
 * Everything else (query execution, result mapping) is shared by
 * {@link com.arcon.utils.db.JdbcDatabaseClient}.
 */
public interface IDBConnectionStrategy {

    /** Fully-qualified JDBC driver class name, used to fail fast if the driver jar is missing. */
    String getDriverClassName();

    /** Builds the JDBC URL from the given config (or returns config.getConnectionString() if pre-set). */
    String buildJdbcUrl(DBConfig config);

    /** Opens and returns a live JDBC connection. */
    Connection openConnection(DBConfig config) throws SQLException;
}
