package com.framework.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.framework.db.strategy.IDBConnectionStrategy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single {@link IDatabaseClient} implementation shared by every JDBC-based database
 * (MySQL, Oracle, PostgreSQL, MSSQL). The database-specific part (driver name, URL format)
 * is delegated to an {@link IDBConnectionStrategy} (Strategy pattern); this class only
 * knows generic JDBC query/update execution and result-set-to-map mapping.
 */
public class JdbcDatabaseClient implements IDatabaseClient {

    private static final Logger log = LoggerFactory.getLogger(JdbcDatabaseClient.class);

    private final IDBConnectionStrategy strategy;
    private Connection connection;

    public JdbcDatabaseClient(IDBConnectionStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public void connect(DBConfig config) {
        try {
            this.connection = strategy.openConnection(config);
            log.info("Connected to {} database at {}:{}/{}", config.getDbType(), config.getHost(), config.getPort(), config.getDbName());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to " + config.getDbType() + " database", e);
        }
    }

    @Override
    public List<Map<String, Object>> executeQuery(String query) {
        ensureConnected();
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute query: " + query, e);
        }
        return rows;
    }

    @Override
    public int executeUpdate(String query) {
        ensureConnected();
        try (Statement stmt = connection.createStatement()) {
            return stmt.executeUpdate(query);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute update: " + query, e);
        }
    }

    /** Parameterized variant to safely bind values instead of building the query with string concatenation. */
    public int executeUpdate(String query, Object... params) {
        ensureConnected();
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute parameterized update: " + query, e);
        }
    }

    @Override
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.warn("Error closing JDBC connection", e);
            } finally {
                connection = null;
            }
        }
    }

    private void ensureConnected() {
        if (!isConnected()) {
            throw new IllegalStateException("Database client is not connected. Call connect(DBConfig) first.");
        }
    }
}
