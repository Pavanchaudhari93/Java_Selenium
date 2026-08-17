package com.framework.db.strategy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.framework.db.DBConfig;

/**
 * Shared skeleton for all JDBC-based strategies: loads the driver once and opens the
 * connection via {@link DriverManager}. Subclasses only need to supply the driver class
 * name and the URL-building logic for their specific database.
 */
public abstract class AbstractJdbcConnectionStrategy implements IDBConnectionStrategy {

    @Override
    public Connection openConnection(DBConfig config) throws SQLException {
        try {
            Class.forName(getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC driver not found on classpath: " + getDriverClassName(), e);
        }
        String url = buildJdbcUrl(config);
        return DriverManager.getConnection(url, config.getUsername(), config.getPassword());
    }

    /**
     * Utility for subclasses: renders extraParams as a URL/DSN parameter suffix.
     *
     * @param firstSeparator   separator prepended before the first param (e.g. "?")
     * @param repeatSeparator  separator used between subsequent params (e.g. "&" or ";")
     */
    protected String buildExtraParamsSuffix(DBConfig config, String firstSeparator, String repeatSeparator) {
        StringBuilder sb = new StringBuilder();
        String separator = firstSeparator;
        for (var entry : config.getExtraParams().entrySet()) {
            sb.append(separator).append(entry.getKey()).append('=').append(entry.getValue());
            separator = repeatSeparator;
        }
        return sb.toString();
    }
}
