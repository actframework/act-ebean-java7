package act.db.ebean.util;

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

import act.db.sql.SqlDbService;
import act.db.sql.SqlDbServiceConfig;
import com.avaje.ebean.config.MatchingNamingConvention;
import com.avaje.ebean.config.NamingConvention;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.UnderscoreNamingConvention;
import org.avaje.datasource.DataSourceConfig;
import org.osgl.logging.LogManager;
import org.osgl.logging.Logger;
import org.osgl.util.S;

import javax.inject.Singleton;
import java.util.Properties;
import java.util.Set;

import static act.db.sql.util.NamingConvention.Default.MATCHING;

/**
 * Adapt {@link act.db.sql.SqlDbServiceConfig} to {@link ServerConfig}
 */
@Singleton
public class EbeanConfigAdaptor {

    private static final Logger LOGGER = LogManager.get(EbeanConfigAdaptor.class);

    public ServerConfig adaptFrom(SqlDbServiceConfig actConfig, act.db.sql.DataSourceConfig dsConfig,  SqlDbService svc) {
        ServerConfig config = new ServerConfig();

        config.setName(svc.id());
        config.setDataSourceConfig(adaptFrom(dsConfig, svc));

        config.setDdlGenerate(actConfig.ddlGeneratorConfig.create);
        config.setDdlRun(actConfig.ddlGeneratorConfig.create);
        config.setDdlCreateOnly(!actConfig.ddlGeneratorConfig.drop);

        config.setNamingConvention(namingConvention(actConfig));

        Set<Class> modelClasses = svc.modelClasses();
        if (null != modelClasses && !modelClasses.isEmpty()) {
            for (Class modelClass : modelClasses) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(S.concat("add model class into Ebean config: ", modelClass.getName()));
                }
                config.addClass(modelClass);
            }
        }

        return config;
    }


    public DataSourceConfig adaptFrom(act.db.sql.DataSourceConfig actConfig, SqlDbService svc) {
        Properties properties = new Properties();
        properties.putAll(actConfig.customProperties);
        DataSourceConfig config = new DataSourceConfig();
        config.loadSettings(properties, svc.id());

        config.setUrl(actConfig.url);
        config.setDriver(actConfig.driver);
        config.setUsername(actConfig.username);
        config.setPassword(actConfig.password);
        config.setAutoCommit(actConfig.autoCommit);

        config.setMinConnections(actConfig.minConnections);
        config.setMaxConnections(actConfig.maxConnections);
        config.setHeartbeatSql(actConfig.heartbeatSql);
        config.setIsolationLevel(actConfig.isolationLevel);
        config.setMaxAgeMinutes(actConfig.maxAgeMinutes);
        config.setMaxInactiveTimeSecs(actConfig.maxInactiveTimeSecs);
        config.setHeartbeatFreqSecs(actConfig.heartbeatFreqSecs);
        config.setCstmtCacheSize(actConfig.cstmtCacheSize);
        config.setPstmtCacheSize(actConfig.pstmtCacheSize);
        config.setTrimPoolFreqSecs(actConfig.trimPoolFreqSecs);
        config.setWaitTimeoutMillis(actConfig.waitTimeoutMillis);
        config.setLeakTimeMinutes(actConfig.leakTimeMinutes);
        config.setPoolListener(actConfig.poolListener);
        config.setOffline(actConfig.offline);
        config.setCaptureStackTrace(actConfig.captureStackTrace);

        return config;
    }

    private NamingConvention namingConvention(SqlDbServiceConfig svcConfig) {
        if (!svcConfig.rawConf.containsKey("naming.convention")) {
            // https://github.com/actframework/act-ebean/issues/13
            return new UnderscoreNamingConvention();
        }
        //TODO provide more actuate naming convention matching logic
        if (MATCHING == svcConfig.tableNamingConvention) {
            return new MatchingNamingConvention();
        }
        return new UnderscoreNamingConvention();
    }

}
