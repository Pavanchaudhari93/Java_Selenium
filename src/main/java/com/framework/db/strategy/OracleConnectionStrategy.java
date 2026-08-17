package com.framework.db.strategy;

import com.framework.db.DBConfig;

public class OracleConnectionStrategy extends AbstractJdbcConnectionStrategy {

    @Override
    public String getDriverClassName() {
        return "oracle.jdbc.OracleDriver";
    }

    @Override
    public String buildJdbcUrl(DBConfig config) {
        if (config.getConnectionString() != null && !config.getConnectionString().isBlank()) {
            return config.getConnectionString();
        }
        // dbName is treated as the Service Name (thin driver, service-name form)
        return String.format("jdbc:oracle:thin:@//%s:%d/%s", config.getHost(), config.getPort(), config.getDbName());
    }
}
