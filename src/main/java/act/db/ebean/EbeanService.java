package act.db.ebean;

import act.Act;
import act.ActComponent;
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
import org.avaje.datasource.DataSourceConfig;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.S;

import javax.persistence.Entity;
import java.lang.annotation.Annotation;
import java.util.EventObject;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static act.app.event.AppEventId.CLASS_LOADED;
import static act.app.event.AppEventId.PRE_LOAD_CLASSES;

@ActComponent
public class EbeanService extends DbService {

    // the ebean service instance
    private EbeanServer ebean;

    private ConcurrentMap<Class<?>, Dao> daoMap;

    private Map<String, Object> conf;

    private ServerConfig serverConfig;

    private static Set<Class<?>> modelTypes = C.newSet();

    public EbeanService(final String dbId, final App app, final Map<String, Object> config) {
        super(dbId, app);
        daoMap = new ConcurrentHashMap<Class<?>, Dao>();
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
                ebean = EbeanServerFactory.create(serverConfig);
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
                if (!EbeanAgentLoader.loadAgentFromClasspath("avaje-ebeanorm-agent", s)) {
                    logger.warn("avaje-ebeanorm-agent not found in classpath - not dynamically loaded");
                }
            }
        });
    }

    @Override
    protected void releaseResources() {
        if (null != ebean) {
            ebean.shutdown(true, false);
        }
    }

    @Override
    public <DAO extends Dao> DAO defaultDao(Class<?> modelType) {
        return $.cast(new EbeanDao(modelType, this));
    }

    @Override
    public <DAO extends Dao> DAO newDaoInstance(Class<DAO> daoType) {
        E.illegalArgumentIf(!EbeanDao.class.isAssignableFrom(daoType), "expected EbeanDao, found: %s", daoType);
        EbeanDao dao = $.cast(app().newInstance(daoType));
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


    private ServerConfig serverConfig(String id, Map<String, Object> conf) {
        ServerConfig sc = new ServerConfig();
        sc.setName(id);

        sc.setDataSourceConfig(datasourceConfig(conf));

        String ddlGenerate = (String) conf.get("ddl.generate");
        if (null != ddlGenerate) {
            sc.setDdlGenerate(Boolean.parseBoolean(ddlGenerate));
        } else if (Act.isDev()) {
            sc.setDdlGenerate(true);
        }

        String ddlRun = (String) conf.get("ddl.run");
        if (null != ddlRun) {
            sc.setDdlRun(Boolean.parseBoolean(ddlRun));
        } else if (Act.isDev()) {
            sc.setDdlRun(true);
        }

        String ddlCreateOnly = (String) conf.get("ddl.createOnly");
        if (null != ddlCreateOnly) {
            sc.setDdlCreateOnly(Boolean.parseBoolean(ddlCreateOnly));
        } else {
            sc.setDdlCreateOnly(false);
        }

        for (Class<?> c : modelTypes) {
            sc.addClass(c);
        }

        return sc;
    }

    private DataSourceConfig datasourceConfig(Map<String, Object> conf) {
        Properties properties = new Properties();
        properties.putAll(conf);
        DataSourceConfig dsc = new DataSourceConfig();
        dsc.loadSettings(properties, "");
        ensureDefaultDatasourceConfig(dsc);
        return dsc;
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

        String driver = dsc.getDriver();
        if (null == driver) {
            logger.warn("No database driver configuration specified. Will use the default h2 driver!");
            driver = "org.h2.Driver";
        }
        dsc.setDriver(driver);

        String url = dsc.getUrl();
        if (null == url) {
            logger.warn("No database URL configuration specified. Will use the default h2 inmemory test database");
            url = "jdbc:h2:mem:tests";
        }
        dsc.setUrl(url);
    }

    public static void registerModelType(Class<?> modelType) {
        modelTypes.add(modelType);
        Class<?> superType = modelType.getSuperclass();
        if (!Object.class.equals(superType)) {
            registerModelType(superType);
        }
    }
}
