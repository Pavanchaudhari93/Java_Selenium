package com.framework.db.strategy;

import com.framework.db.DBConfig;

public class MySQLConnectionStrategy extends AbstractJdbcConnectionStrategy {

    @Override
    public String getDriverClassName() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    public String buildJdbcUrl(DBConfig config) {
        if (config.getConnectionString() != null && !config.getConnectionString().isBlank()) {
            return config.getConnectionString();
        }
        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true",
                config.getHost(), config.getPort(), config.getDbName());
        return url + buildExtraParamsSuffix(config, "&", "&");
    }
}
