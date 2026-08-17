package com.framework.db.strategy;

import com.framework.db.DBConfig;

public class PostgreSQLConnectionStrategy extends AbstractJdbcConnectionStrategy {

    @Override
    public String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    public String buildJdbcUrl(DBConfig config) {
        if (config.getConnectionString() != null && !config.getConnectionString().isBlank()) {
            return config.getConnectionString();
        }
        String url = String.format("jdbc:postgresql://%s:%d/%s", config.getHost(), config.getPort(), config.getDbName());
        return url + buildExtraParamsSuffix(config, "?", "&");
    }
}
