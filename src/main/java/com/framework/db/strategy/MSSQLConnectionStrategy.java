package com.framework.db.strategy;

import com.framework.db.DBConfig;

public class MSSQLConnectionStrategy extends AbstractJdbcConnectionStrategy {

    @Override
    public String getDriverClassName() {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    @Override
    public String buildJdbcUrl(DBConfig config) {
        if (config.getConnectionString() != null && !config.getConnectionString().isBlank()) {
            return config.getConnectionString();
        }
        String url = String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false",
                config.getHost(), config.getPort(), config.getDbName());
        return url + buildExtraParamsSuffix(config, ";", ";");
    }
}
