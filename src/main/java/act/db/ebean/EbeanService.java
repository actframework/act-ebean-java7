package act.db.ebean;

import act.Act;
import act.app.App;
import act.app.DbServiceManager;
import act.conf.AppConfigKey;
import act.db.Dao;
import act.db.DbService;
import act.event.AppEventListenerBase;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.ServerConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.avaje.datasource.DataSourceConfig;
import org.osgl.$;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceException;
import javax.sql.DataSource;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EventObject;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static act.app.event.AppEventId.CLASS_LOADED;
import static act.app.event.AppEventId.PRE_LOAD_CLASSES;

public final class EbeanService extends DbService {

    private static Logger logger = LogManager.get(EbeanService.class);

    // the ebean service instance
    private EbeanServer ebean;

    private Map<String, Object> conf;

    private ServerConfig serverConfig;

    // the datasource for low level JDBC API usage
    private DataSource ds;

    private static Set<Class<?>> modelTypes = C.newSet();

    public EbeanService(final String dbId, final App app, final Map<String, Object> config) {
        super(dbId, app);
        this.conf = config;
        Object o = conf.get("agentPackage");
        final String agentPackage = null == o ? S.string(app().config().get(AppConfigKey.SCAN_PACKAGE)) : S.string(o).trim();
        E.invalidConfigurationIf(S.blank(agentPackage), "\"agentPackage\" not configured");
        logger.info("\"agentPackage\" configured: %s", agentPackage);
        final EbeanService svc = this;
        app.eventBus().bind(CLASS_LOADED, new AppEventListenerBase(S.builder(dbId).append("-ebean-prestart")) {
            @Override
            public void on(EventObject event) {
                svc.serverConfig = serverConfig(dbId, conf);
                app().eventBus().emit(new PreEbeanCreation(serverConfig));
                try {
                    ebean = EbeanServerFactory.create(serverConfig);
                } catch (PersistenceException e) {
                    // try disable ddlRun
                    // see http://stackoverflow.com/questions/35676651/ebean-run-ddl-only-if-the-database-does-not-exist/36253846
                    if (Act.isDev()) {
                        serverConfig.setDdlRun(false);
                        ebean = EbeanServerFactory.create(serverConfig);
                    } else {
                        throw e;
                    }
                }
                Ebean.register(ebean, S.eq(DbServiceManager.DEFAULT, dbId));
            }
        }).bind(PRE_LOAD_CLASSES, new AppEventListenerBase(S.builder(dbId).append("-ebean-pre-cl")) {
            @Override
            public void on(EventObject event) {
                String s = S.builder("debug=").append(Act.isDev() ? "1" : "0")
                        .append(";packages=")
                        //.append("act.db.ebean.*,")
                        .append(agentPackage)
                        .toString();
                if (!EbeanAgentLoader.loadAgentFromClasspath("ebean-agent", s)) {
                    logger.warn("ebean-agent not found in classpath - not dynamically loaded");
                }
            }
        });
    }

    @Override
    protected void releaseResources() {
        if (null != ebean) {
            ebean.shutdown(true, false);
        }
        modelTypes.clear();
        conf.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <DAO extends Dao> DAO defaultDao(Class<?> modelType) {
        if (EbeanModelBase.class.isAssignableFrom(modelType)) {
            Type type = modelType.getGenericSuperclass();
            if (type instanceof ParameterizedType) {
                return $.cast(new EbeanDao((Class)((ParameterizedType) type).getActualTypeArguments()[0], modelType, this));
            }
        }
        Class<?> idType = findModelIdTypeByAnnotation(modelType, Id.class);
        E.illegalArgumentIf(null == idType, "Cannot find out Dao for model type[%s]: unable to identify the ID type", modelType);
        return $.cast(new EbeanDao(idType, modelType, this));
    }

    @Override
    public <DAO extends Dao> DAO newDaoInstance(Class<DAO> daoType) {
        E.illegalArgumentIf(!EbeanDao.class.isAssignableFrom(daoType), "expected EbeanDao, found: %s", daoType);
        EbeanDao dao = $.cast(app().getInstance(daoType));
        dao.ebean(this.ebean());
        return (DAO) dao;
    }

    @Override
    public Class<? extends Annotation> entityAnnotationType() {
        return Entity.class;
    }

    public EbeanServer ebean() {
        return ebean;
    }

    public DataSource ds() {
        return ds;
    }

    private ServerConfig serverConfig(String id, Map<String, Object> conf) {
        ServerConfig sc = new ServerConfig();
        sc.setName(id);
        Properties properties = new Properties();
        properties.putAll(conf);
        sc.loadFromProperties(properties);

        HikariDataSource dataSource = dataSource(conf);
        sc.setDataSource(dataSource);
        ds = dataSource;

        boolean noddl = false;
        String ddlGenerate = (String) conf.get("ddl.generate");
        if (null != ddlGenerate) {
            sc.setDdlGenerate(Boolean.parseBoolean(ddlGenerate));
        } else if (Act.isDev()) {
            String url = dataSource.getJdbcUrl();
            if (url.startsWith("jdbc:h2:")) {
                String file = url.substring("jdbc:h2:".length()) + ".mv.db";
                File _file = new File(file);
                if (_file.exists()) {
                    noddl = true;
                }
            }
            sc.setDdlGenerate(!noddl);
        }

        String ddlRun = (String) conf.get("ddl.run");
        if (null != ddlRun) {
            sc.setDdlRun(Boolean.parseBoolean(ddlRun));
        } else if (Act.isDev()) {
            sc.setDdlRun(!noddl);
        }

        String ddlCreateOnly = (String) conf.get("ddl.createOnly");
        if (null != ddlCreateOnly) {
            sc.setDdlCreateOnly(Boolean.parseBoolean(ddlCreateOnly));
        } else if (Act.isDev()) {
            sc.setDdlCreateOnly(!noddl);
        }

        for (Class<?> c : modelTypes) {
            sc.addClass(c);
        }

        return sc;
    }

    private HikariDataSource dataSource(Map<String, Object> conf) {
        HikariConfig hc = new HikariConfig();
        for (Map.Entry<String, Object> entry : conf.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if ("username".equals(key)) {
                hc.setUsername(S.string(val));
            } else if ("password".equals(key)) {
                hc.setPassword(S.string(val));
            } else if ("url".equals(key) || "jdbcUrl".equals(key)) {
                hc.setJdbcUrl(S.string(val));
            } else if ("maximumPoolSize".equals(key)) {
                hc.setMaximumPoolSize(Integer.parseInt(S.string(val)));
            } else if ("autoCommit".equals(key)) {
                hc.setAutoCommit(Boolean.valueOf(S.string(val)));
            } else if ("idleTimeout".equals(key)) {
                hc.setIdleTimeout(Long.parseLong(S.string(val)));
            } else if ("maxLifetime".equals(key)) {
                hc.setMaxLifetime(Long.parseLong(S.string(val)));
            } else if ("connectionTimeout".equals(key)) {
                hc.setConnectionTimeout(Long.parseLong(S.string(val)));
            } else if ("minimumIdle".equals(key)) {
                hc.setMinimumIdle(Integer.parseInt(S.string(val)));
            } else if ("poolName".equals(key)) {
                hc.setPoolName(S.string(val));
            } else if ("driverClassName".equals(key)) {
                hc.setDriverClassName(S.string(val));
            } else {
                hc.addDataSourceProperty(entry.getKey(), entry.getValue());
            }
        }
        ensureDefaultDatasourceConfig(hc);
        return new HikariDataSource(hc);
    }

    private void ensureDefaultDatasourceConfig(HikariConfig dsc) {
        String username = dsc.getUsername();
        if (null == username) {
            logger.warn("No data source user configuration specified. Will use the default 'sa' user");
            username = "sa";
        }
        dsc.setUsername(username);

        String password = dsc.getPassword();
        if (null == password) {
            password = "";
        }
        dsc.setPassword(password);

        String url = dsc.getJdbcUrl();
        if (null == url) {
            logger.warn("No database URL configuration specified. Will use the default h2 inmemory test database");
            url = "jdbc:h2:mem:tests";
        }
        dsc.setJdbcUrl(url);


        String driver = dsc.getDriverClassName();
        if (null == driver) {
            if (url.contains("mysql")) {
                driver = "com.mysql.jdbc.Driver";
            } else if (url.contains("postgresql")) {
                driver = "org.postgresql.Driver";
            } else if (url.contains("jdbc:h2:")) {
                driver = "org.h2.Driver";
            } else if (url.contains("jdbc:oracle")) {
                driver = "oracle.jdbc.OracleDriver";
            } else if (url.contains("sqlserver")) {
                driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            } else if (url.contains("jdbc:db2")) {
                driver = "com.ibm.db2.jcc.DB2Driver";
            } else {
                throw E.invalidConfiguration("JDBC driver needs to be configured for datasource: %s", id());
            }
            logger.warn("JDBC driver not configured, system automatically set to: " + driver);
        }
        dsc.setDriverClassName(driver);
    }

    private void ensureDefaultDatasourceConfig(DataSourceConfig dsc) {
        String username = dsc.getUsername();
        if (null == username) {
            logger.warn("No data source user configuration specified. Will use the default 'sa' user");
            username = "sa";
        }
        dsc.setUsername(username);

        String password = dsc.getPassword();
        if (null == password) {
            password = "";
        }
        dsc.setPassword(password);

        String url = dsc.getUrl();
        if (null == url) {
            logger.warn("No database URL configuration specified. Will use the default h2 inmemory test database");
            url = "jdbc:h2:mem:tests";
        }
        dsc.setUrl(url);

        String driver = dsc.getDriver();
        if (null == driver) {
            if (url.contains("mysql")) {
                driver = "com.mysql.jdbc.Driver";
            } else if (url.contains("postgresql")) {
                driver = "org.postgresql.Driver";
            } else if (url.contains("jdbc:h2:")) {
                driver = "org.h2.Driver";
            } else if (url.contains("jdbc:oracle")) {
                driver = "oracle.jdbc.OracleDriver";
            } else if (url.contains("sqlserver")) {
                driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            } else if (url.contains("jdbc:db2")) {
                driver = "com.ibm.db2.jcc.DB2Driver";
            } else {
                throw E.invalidConfiguration("JDBC driver needs to be configured for datasource: %s", id());
            }
            logger.warn("JDBC driver not configured, system automatically set to: " + driver);
        }
        dsc.setDriver(driver);
    }

    public static void registerModelType(Class<?> modelType) {
        modelTypes.add(modelType);
        Class<?> superType = modelType.getSuperclass();
        if (!Object.class.equals(superType)) {
            registerModelType(superType);
        }
    }
}
