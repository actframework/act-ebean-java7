package act.db.ebean.datasource;

import act.db.ebean.DataSourceProvider;
import com.alibaba.druid.pool.DruidDataSource;
import org.avaje.datasource.DataSourceConfig;
import org.osgl.util.C;
import org.osgl.util.E;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;

/**
 * Provide Druid datasource
 */
public class DruidDataSourceProvider extends DataSourceProvider {

    @Override
    public DataSource createDataSource(DataSourceConfig conf) {
        DruidDataSource source = new DruidDataSource();
        source.setUrl(conf.getUrl());
        source.setUsername(conf.getUsername());
        source.setPassword(conf.getPassword());
        source.setDriverClassName(conf.getDriver());
        source.setMaxActive(conf.getMaxConnections());
        source.setMinIdle(conf.getMinConnections());
        source.setMaxWait(conf.getWaitTimeoutMillis());
        source.setValidationQuery(conf.getHeartbeatSql());
        source.setMaxPoolPreparedStatementPerConnectionSize(conf.getPstmtCacheSize());

        Map<String, String> miscConf = conf.getCustomProperties();
        String s = miscConf.get("initialSize");
        if (null != s) {
            source.setInitialSize(Integer.parseInt(s));
        } else {
            source.setInitialSize(source.getMinIdle());
        }

        s = miscConf.get("timeBetweenEvictionRunsMillis");
        if (null != s) {
            source.setTimeBetweenEvictionRunsMillis(Long.parseLong(s));
        }

        s = miscConf.get("minEvictableIdleTimeMillis");
        if (null != s) {
            source.setMinEvictableIdleTimeMillis(Long.parseLong(s));
        }

        s = miscConf.get("testWhileIdle");
        if (null != s) {
            source.setTestWhileIdle(Boolean.parseBoolean(s));
        }

        s = miscConf.get("testOnBorrow");
        if (null != s) {
            source.setTestOnBorrow(Boolean.parseBoolean(s));
        }

        s = miscConf.get("testOnReturn");
        if (null != s) {
            source.setTestOnReturn(Boolean.parseBoolean(s));
        }

        s = miscConf.get("filters");
        if (null != s) {
            try {
                source.setFilters(s);
            } catch (SQLException e) {
                throw E.unexpected(e);
            }
        }

        s = miscConf.get("poolPreparedStatements");
        if (null != s) {
            source.setPoolPreparedStatements(Boolean.parseBoolean(s));
        }

        return source;
    }

    @Override
    public Map<String, String> confKeyMapping() {
        return C.map("minIdle", "minConnections",
                "maxActive", "maxConnections",
                "maxWait", "connectionTimeout",
                "validationQuery", "heartbeatSql",
                "pstmtCacheSize", "maxPoolPreparedStatementPerConnectionSize"
        );
    }

}
