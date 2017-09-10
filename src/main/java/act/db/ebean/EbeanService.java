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

import static act.app.event.AppEventId.PRE_LOAD_CLASSES;

import act.Act;
import act.app.App;
import act.conf.AppConfigKey;
import act.db.Dao;
import act.db.ebean.util.EbeanConfigAdaptor;
import act.db.sql.DataSourceConfig;
import act.db.sql.SqlDbService;
import act.event.AppEventListenerBase;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.ServerConfig;
import org.osgl.$;
import org.osgl.bootstrap.Version;
import org.osgl.util.E;
import org.osgl.util.S;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EventObject;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.sql.DataSource;

public final class EbeanService extends SqlDbService {

    public static final Version VERSION = EbeanPlugin.VERSION;

    // the ebean service instance
    private EbeanServer ebean;

    private ServerConfig ebeanConfig;

    public EbeanService(final String dbId, final App app, final Map<String, String> config) {
        super(dbId, app, config);
        String s = config.get("agentPackage");
        final String agentPackage = null == s ? S.string(app().config().get(AppConfigKey.SCAN_PACKAGE)) : S.string(s).trim();
        E.invalidConfigurationIf(S.blank(agentPackage), "\"agentPackage\" not configured");
        if (isTraceEnabled()) {
            trace("\"agentPackage\" configured: %s", agentPackage);
        }
        app.eventBus().bind(PRE_LOAD_CLASSES, new AppEventListenerBase(S.concat(dbId, "-ebean-pre-cl")) {
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
    protected void dataSourceProvided(DataSource dataSource, DataSourceConfig dsConfig) {
        ebeanConfig = new EbeanConfigAdaptor().adaptFrom(this.config, dsConfig, this);
        ebeanConfig.setDataSource(dataSource);
        app().eventBus().trigger(new EbeanConfigLoaded(ebeanConfig));
        ebean = EbeanServerFactory.create(ebeanConfig);
    }

    @Override
    protected DataSource createDataSource() {
        ebeanConfig = new EbeanConfigAdaptor().adaptFrom(this.config, this.config.dataSourceConfig, this);
        app().eventBus().trigger(new EbeanConfigLoaded(ebeanConfig));
        ebean = EbeanServerFactory.create(ebeanConfig);
        return ebeanConfig.getDataSource();
    }

    @Override
    protected void releaseResources() {
        if (null != ebean) {
            ebean.shutdown(true, false);
            if (isDebugEnabled()) {
                debug("ebean shutdown: %s", id());
            }
            ebean = null;
            ebeanConfig = null;
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

}
