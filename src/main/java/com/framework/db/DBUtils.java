package com.framework.db;

import org.testng.Assert;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Thread-safe facade over the Database Utility Framework (see com.arcon.utils.db package).
 * Test classes call {@link #connect} once per thread, then use {@link #getRecords} /
 * {@link #update} / {@link #close} — the underlying DB-specific client is created via
 * {@link DatabaseClientFactory} (Factory pattern) and never exposed to callers.
 *
 * Usage:
 *   DBUtils.connect(new DBConfig(DBType.MYSQL, "host", 3306, "epm", "user", "pass"));
 *   List<Map<String,Object>> rows = DBUtils.getRecords("SELECT * FROM users");
 *   DBUtils.update("UPDATE users SET status='active' WHERE id=1");
 *   DBUtils.close();
 */
public class DBUtils {

    private static final ThreadLocal<IDatabaseClient> clientThreadLocal = new ThreadLocal<>();

    private DBUtils() {
    }

    public static void connect(DBConfig config) {
        IDatabaseClient client = DatabaseClientFactory.createClient(config.getDbType());
        client.connect(config);
        clientThreadLocal.set(client);
    }

    public static void connect(DBType dbType, String host, int port, String dbName, String username, String password) {
        connect(new DBConfig(dbType, host, port, dbName, username, password));
    }

    public static List<Map<String, Object>> getRecords(String query) {
        return getClient().executeQuery(query);
    }

    public static int update(String query) {
        return getClient().executeUpdate(query);
    }

    public static boolean isConnected() {
        IDatabaseClient client = clientThreadLocal.get();
        return client != null && client.isConnected();
    }

    public static void close() {
        IDatabaseClient client = clientThreadLocal.get();
        if (client != null) {
            client.disconnect();
            clientThreadLocal.remove();
        }
    }

    /**
     * Reads a single column's value out of one record returned by {@link #getRecords}.
     * Column name lookup is case-insensitive since JDBC drivers don't guarantee label casing.
     */
    public static Object getColumnValue(Map<String, Object> record, String columnName) {
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(columnName)) {
                return entry.getValue();
            }
        }
        throw new IllegalArgumentException("Column '" + columnName + "' not found. Available columns: " + record.keySet());
    }

    /** Convenience overload: reads a column value straight out of the row at rowIndex in a result set. */
    public static Object getColumnValue(List<Map<String, Object>> records, int rowIndex, String columnName) {
        return getColumnValue(getRecordAt(records, rowIndex), columnName);
    }

    /**
     * Validates that a single record's column matches the expected value (soft check — returns
     * a boolean instead of throwing). Useful when the caller wants to branch on the outcome.
     */
    public static boolean validateColumnValue(Map<String, Object> record, String columnName, Object expectedValue) {
        Object actualValue = getColumnValue(record, columnName);
        return Objects.equals(String.valueOf(actualValue), String.valueOf(expectedValue));
    }

    /** Same as {@link #validateColumnValue(Map, String, Object)} but picks the record out of a result set by index. */
    public static boolean validateColumnValue(List<Map<String, Object>> records, int rowIndex, String columnName, Object expectedValue) {
        return validateColumnValue(getRecordAt(records, rowIndex), columnName, expectedValue);
    }

    /** Hard-assert variant: fails the test immediately (via TestNG Assert) if the column doesn't match. */
    public static void assertColumnValue(Map<String, Object> record, String columnName, Object expectedValue) {
        Object actualValue = getColumnValue(record, columnName);
        Assert.assertEquals(String.valueOf(actualValue), String.valueOf(expectedValue),
                "Mismatch for column '" + columnName + "'");
    }

    /** Hard-assert variant that picks the record out of a result set by index. */
    public static void assertColumnValue(List<Map<String, Object>> records, int rowIndex, String columnName, Object expectedValue) {
        assertColumnValue(getRecordAt(records, rowIndex), columnName, expectedValue);
    }

    private static Map<String, Object> getRecordAt(List<Map<String, Object>> records, int rowIndex) {
        if (records == null || rowIndex < 0 || rowIndex >= records.size()) {
            throw new IndexOutOfBoundsException("No record at index " + rowIndex + " (result set size: " + (records == null ? 0 : records.size()) + ")");
        }
        return records.get(rowIndex);
    }

    /** Escape hatch for DB-specific operations (e.g. Mongo updateMany) not covered by the common interface. */
    public static IDatabaseClient getClient() {
        IDatabaseClient client = clientThreadLocal.get();
        if (client == null) {
            throw new IllegalStateException("DBUtils is not connected on this thread. Call DBUtils.connect(...) first.");
        }
        return client;
    }
}
