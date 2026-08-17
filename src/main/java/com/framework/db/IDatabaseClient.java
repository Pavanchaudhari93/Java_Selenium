package com.framework.db;

import java.util.List;
import java.util.Map;

/**
 * Common contract implemented by every database client (relational and NoSQL).
 * Allows {@link com.arcon.utils.db.DBUtils} to work with any {@link DBType} through
 * one uniform interface, regardless of the underlying driver.
 */
public interface IDatabaseClient extends AutoCloseable {

    /** Opens the underlying connection/session using the supplied config. */
    void connect(DBConfig config);

    /**
     * Executes a read query.
     * For JDBC databases: a SQL SELECT statement.
     * For MongoDB: "collectionName|{filterJson}" (filterJson optional, empty = find all).
     *
     * @return each row/document represented as a column-name/field-name to value map.
     */
    List<Map<String, Object>> executeQuery(String query);

    /**
     * Executes a write operation (INSERT/UPDATE/DELETE for JDBC, insert/update/delete for Mongo
     * expressed the same "collectionName|{documentJson}" way as {@link #executeQuery(String)}).
     *
     * @return number of rows/documents affected.
     */
    int executeUpdate(String query);

    boolean isConnected();

    void disconnect();

    @Override
    default void close() {
        disconnect();
    }
}
