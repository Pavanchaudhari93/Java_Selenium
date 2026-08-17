package com.framework.db;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds connection parameters for any supported database type.
 * Built via the fluent builder methods so the same class serves
 * JDBC-based databases (host/port/dbName/credentials) and MongoDB
 * (connectionString or host/port/dbName).
 */
public class DBConfig {

    private DBType dbType;
    private String host;
    private int port;
    private String dbName;
    private String username;
    private String password;
    /** Optional full connection string / URI. When set, takes precedence over host/port/dbName. */
    private String connectionString;
    /** Extra JDBC/Mongo URI parameters, e.g. useSSL=false, sslMode=require */
    private final Map<String, String> extraParams = new HashMap<>();

    public DBConfig() {
    }

    public DBConfig(DBType dbType, String host, int port, String dbName, String username, String password) {
        this.dbType = dbType;
        this.host = host;
        this.port = port;
        this.dbName = dbName;
        this.username = username;
        this.password = password;
    }

    public DBType getDbType() {
        return dbType;
    }

    public DBConfig setDbType(DBType dbType) {
        this.dbType = dbType;
        return this;
    }

    public String getHost() {
        return host;
    }

    public DBConfig setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public DBConfig setPort(int port) {
        this.port = port;
        return this;
    }

    public String getDbName() {
        return dbName;
    }

    public DBConfig setDbName(String dbName) {
        this.dbName = dbName;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public DBConfig setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public DBConfig setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public DBConfig setConnectionString(String connectionString) {
        this.connectionString = connectionString;
        return this;
    }

    public Map<String, String> getExtraParams() {
        return extraParams;
    }

    public DBConfig addExtraParam(String key, String value) {
        this.extraParams.put(key, value);
        return this;
    }
}
