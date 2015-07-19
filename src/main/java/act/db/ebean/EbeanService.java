package act.db.ebean;

import act.Act;
import act.app.App;
import act.app.DbServiceManager;
import act.app.event.AppPreLoadClasses;
import act.app.event.AppPreStart;
import act.db.Dao;
import act.db.DbService;
import act.event.AppEventListenerBase;
import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import org.osgl._;
import org.osgl.util.C;
import org.osgl.util.S;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static act.app.event.AppEventId.PRE_LOAD_CLASSES;
import static act.app.event.AppEventId.PRE_START;

public class EbeanService extends DbService {

    // the ebean service instance
    private EbeanServer ebean;

    private ConcurrentMap<Class<?>, Dao> daoMap;

    private Map<String, Object> conf;

    private static Set<Class<?>> modelTypes = C.newSet();

    public EbeanService(final String dbId, final App app, Map<String, Object> config) {
        super(dbId, app);
        daoMap = new ConcurrentHashMap<Class<?>, Dao>();
        this.conf = config;
        final String agentPackage = conf.get("ebean.agentPackage").toString();
        app.eventBus().bind(PRE_START, new AppEventListenerBase<AppPreStart>(S.builder(dbId).append("-ebean-prestart")) {
            @Override
            public void on(AppPreStart event) {
                ebean = EbeanServerFactory.create(serverConfig(dbId, conf));
                Ebean.register(ebean, S.eq(DbServiceManager.DEFAULT, dbId));
            }
        }).bind(PRE_LOAD_CLASSES, new AppEventListenerBase<AppPreLoadClasses>(S.builder(dbId).append("-ebean-pre-cl")) {
            @Override
            public void on(AppPreLoadClasses event) {
                String s = S.builder("debug=").append(Act.isDev() ? "1" : "0").append(";packages=").append(agentPackage).toString();
                if (!EbeanAgentLoader.loadAgentFromClasspath("avaje-ebeanorm-agent", s)) {
                    logger.warn("avaje-ebeanorm-agent not found in classpath - not dynamically loaded");
                }
            }
        });
    }

    @Override
    protected void releaseResources() {
        ebean.shutdown(false, false);
    }

    @Override
    protected <DAO extends Dao> DAO defaultDao(Class<?> modelType) {
        return _.cast(new EbeanDao(modelType, ebean));
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

        for (Class<?> c : modelTypes) {
            sc.addClass(c);
        }

        return sc;
    }

    private DataSourceConfig datasourceConfig(Map<String, Object> conf) {
        DataSourceConfig dsc = new DataSourceConfig();

        String username = (String) conf.get("username");
        if (null == username) {
            logger.warn("No data source user configuration specified. Will use the default 'sa' user");
            username = "sa";
        }
        dsc.setUsername(username);

        String password = (String) conf.get("password");
        if (null == password) {
            password = "";
        }
        dsc.setPassword(password);

        String driver = (String) conf.get("databaseDriver");
        if (null == driver) {
            logger.warn("No database driver configuration specified. Will use the default h2 driver!");
            driver = "org.h2.Driver";
        }
        dsc.setDriver(driver);

        String url = (String) conf.get("databaseUrl");
        if (null == url) {
            logger.warn("No database URL configuration specified. Will use the default h2 inmemory test database");
            url = "jdbc:h2:mem:tests";
        }
        dsc.setUrl(url);

        String heartbeatsql = (String) conf.get("heartbeatsql");
        if (null != heartbeatsql) {
            dsc.setHeartbeatSql(heartbeatsql);
        }

        String isolationlevel = (String) conf.get("isolationlevel");
        if (null != isolationlevel) {
            dsc.setIsolationLevel(dsc.getTransactionIsolationLevel(isolationlevel));
        }

        if (conf.containsKey("minConnections")) {
            int minConn = Integer.parseInt((String) conf.get("minConnections"));
            dsc.setMinConnections(minConn);
        }

        if (conf.containsKey("maxConnections")) {
            int maxConn = Integer.parseInt((String) conf.get("maxConnections"));
            dsc.setMaxConnections(maxConn);
        }

        if (conf.containsKey("capturestacktrace")) {
            boolean b = Boolean.parseBoolean((String) conf.get("capturestacktrace"));
            dsc.setCaptureStackTrace(b);
        }

        return dsc;

    }

    public static void registerModelType(Class<?> modelType) {
        modelTypes.add(modelType);
    }
}
