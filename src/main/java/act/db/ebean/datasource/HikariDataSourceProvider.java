package act.db.ebean.datasource;

import act.db.ebean.DataSourceProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.avaje.datasource.DataSourceConfig;
import org.osgl.util.C;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Provide HikariCP data source
 */
public class HikariDataSourceProvider extends DataSourceProvider {

    @Override
    public DataSource createDataSource(DataSourceConfig conf) {
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(conf.getUrl());
        hc.setUsername(conf.getUsername());
        hc.setPassword(conf.getPassword());
        hc.setDriverClassName(conf.getDriver());
        hc.setMaximumPoolSize(conf.getMaxConnections());
        int minConn = conf.getMinConnections();
        if (minConn != new DataSourceConfig().getMinConnections()) {
            // Only set min connection when it is not the default value
            // because HikariCP recommend not to set this value by default
            hc.setMinimumIdle(conf.getMinConnections());
        }
        hc.setConnectionTimeout(conf.getWaitTimeoutMillis());
        hc.setAutoCommit(conf.isAutoCommit());
        hc.setConnectionTestQuery(conf.getHeartbeatSql());

        Map<String, String> miscConf = conf.getCustomProperties();
        String s = miscConf.get("idleTimeout");
        if (null != s) {
            int n = Integer.parseInt(s);
            hc.setIdleTimeout(n);
        } else {
            hc.setIdleTimeout(conf.getMaxInactiveTimeSecs() * 1000);
        }

        s = miscConf.get("maxLifetime");
        if (null != s) {
            long n = Long.parseLong(s);
            hc.setMaxLifetime(n);
        } else {
            hc.setMaxLifetime(conf.getMaxAgeMinutes() * 60 * 1000L);
        }

        s = miscConf.get("poolName");
        if (null != s) {
            hc.setPoolName(s);
        }

        return new HikariDataSource(hc);
    }

    @Override
    public Map<String, String> confKeyMapping() {
        return C.map("jdbcUrl", "url",
                "maximumPoolSize", "maxConnections",
                "minimumIdle", "minConnections",
                "connectionTimeout", "waitTimeout"
        );
    }
}
