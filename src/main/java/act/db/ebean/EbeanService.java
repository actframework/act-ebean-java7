package act.db.ebean;

/*-
 * #%L
 * ACT Ebean
 * %%
 * Copyright (C) 2015 - 2017 ActFramework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static act.app.event.SysEventId.PRE_LOAD_CLASSES;

import act.Act;
import act.app.App;
import act.conf.AppConfigKey;
import act.db.Dao;
import act.db.ebean.util.EbeanConfigAdaptor;
import act.db.ebean.util.EbeanDataSourceProvider;
import act.db.ebean.util.EbeanDataSourceWrapper;
import act.db.sql.DataSourceConfig;
import act.db.sql.DataSourceProvider;
import act.db.sql.SqlDbService;
import act.db.sql.tx.TxContext;
import act.event.SysEventListenerBase;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.TxScope;
import com.avaje.ebean.config.ServerConfig;
import org.osgl.$;
import org.osgl.util.E;
import org.osgl.util.S;
import osgl.version.Version;
import osgl.version.Versioned;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EventObject;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.sql.DataSource;

@Versioned
public final class EbeanService extends SqlDbService {

    public static final Version VERSION = EbeanPlugin.VERSION;

    // the ebean service instance
    private EbeanServer ebean;

    // the ebean service instance for readonly operations
    private EbeanServer ebeanReadOnly;

    public EbeanService(final String dbId, final App app, final Map<String, String> config) {
        super(dbId, app, config);
        String s = config.get("agentPackage");
        String scanPackage = app().config().get(AppConfigKey.SCAN_PACKAGE, null);
        final String agentPackage = null == s ? scanPackage : S.string(s).trim();
        E.invalidConfigurationIf(S.blank(agentPackage), "\"agentPackage\" not configured");
        if (isTraceEnabled()) {
            trace("\"agentPackage\" configured: %s", agentPackage);
        }
        app.eventBus().bind(PRE_LOAD_CLASSES, new SysEventListenerBase(S.concat(dbId, "-ebean-pre-cl")) {
            @Override
            public void on(EventObject event) {
                String s = S.buffer("debug=").append(Act.isDev() ? "1" : "0")
                        .append(";packages=")
                        .append(agentPackage)
                        .toString();
                if (!EbeanAgentLoader.loadAgentFromClasspath("ebean-agent", s)) {
                    warn("ebean-agent not found in classpath - not dynamically loaded");
                }
            }
        });
    }

    @Override
    protected boolean supportDdl() {
        return true;
    }

    @Override
    protected void dataSourceProvided(DataSource dataSource, DataSourceConfig dsConfig, boolean readonly) {
        ServerConfig ebeanConfig;
        if (dataSource instanceof EbeanDataSourceWrapper) {
            EbeanDataSourceWrapper wrapper = (EbeanDataSourceWrapper) dataSource;
            ebeanConfig = wrapper.ebeanConfig;
        } else {
            ebeanConfig = new EbeanConfigAdaptor().adaptFrom(this.config, dsConfig, this);
            ebeanConfig.setDataSource(dataSource);
        }
        IdGeneratorRegister rg = Act.getInstance(IdGeneratorRegister.class);
        rg.registerTo(ebeanConfig);
        app().eventBus().trigger(new EbeanConfigLoaded(ebeanConfig));
        if (readonly) {
            ebeanConfig.setDdlGenerate(false);
            ebeanConfig.setDdlRun(false);
            ebeanReadOnly = EbeanServerFactory.create(ebeanConfig);
        } else {
            ebean = EbeanServerFactory.create(ebeanConfig);
            if (null == ebeanReadOnly) {
                ebeanReadOnly = ebean;
            }
        }
    }

    @Override
    protected DataSourceProvider builtInDataSourceProvider() {
        return new EbeanDataSourceProvider(config, this);
    }

    @Override
    protected void releaseResources() {
        if (null != ebean) {
            ebean.shutdown(true, false);
            if (logger.isDebugEnabled()) {
                logger.debug("ebean shutdown: %s", id());
            }
            ebean = null;
        }
        if (null != ebeanReadOnly) {
            ebeanReadOnly.shutdown(true, false);
            if (logger.isDebugEnabled()) {
                logger.debug("ebean readonly shutdown: %s", id());
            }
            ebeanReadOnly = null;
        }
        super.releaseResources();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <DAO extends Dao> DAO defaultDao(Class<?> modelType) {
        if (EbeanModelBase.class.isAssignableFrom(modelType)) {
            Type type = modelType.getGenericSuperclass();
            if (type instanceof ParameterizedType) {
                return $.cast(new EbeanDao((Class) ((ParameterizedType) type).getActualTypeArguments()[0], modelType, this));
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
        dao.dbService(this);
        return (DAO) dao;
    }

    @Override
    public Class<? extends Annotation> entityAnnotationType() {
        return Entity.class;
    }

    @Override
    protected void doStartTx(Object delegate, boolean readOnly) {
        if (readOnly) {
            TxScope scope = TxScope.required().setReadOnly(true);
            ebeanReadOnly.beginTransaction(scope);
        } else {
            ebean.beginTransaction();
        }
    }

    @Override
    protected void doRollbackTx(Object delegate, Throwable cause) {
        Transaction tx = ebean.currentTransaction();
        if (null == tx) {
            return;
        }
        if (TxContext.readOnly()) {
            ebeanReadOnly.endTransaction();
        } else {
            logger.warn(cause, "Roll back transaction");
            ebean.rollbackTransaction();
        }
    }

    @Override
    protected void doEndTxIfActive(Object delegate) {
        Transaction tx = ebean.currentTransaction();
        if (null == tx) {
            return;
        }
        if (TxContext.readOnly()) {
            ebeanReadOnly.endTransaction();
        } else {
            ebean.commitTransaction();
        }
    }

    public EbeanServer ebean(boolean readOnly) {
        return readOnly ? ebeanReadOnly : ebean;
    }

    public EbeanServer ebean() {
        return ebean;
    }

    public EbeanServer ebeanReadOnly() {
        return ebeanReadOnly;
    }

}
