package act.db.ebean.util;

/*-
 * #%L
 * ACT Ebean
 * %%
 * Copyright (C) 2015 - 2018 ActFramework
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

import act.db.sql.DataSourceConfig;
import act.db.sql.DataSourceProvider;
import act.db.sql.SqlDbService;
import act.db.sql.SqlDbServiceConfig;
import act.db.sql.monitor.DataSourceStatus;
import com.avaje.ebean.config.ServerConfig;
import org.avaje.datasource.DataSourceAlertFactory;
import org.avaje.datasource.DataSourceFactory;

import java.util.Map;
import javax.sql.DataSource;

public class EbeanDataSourceProvider extends DataSourceProvider {

    private SqlDbServiceConfig actConfig;
    private SqlDbService svc;

    public EbeanDataSourceProvider(SqlDbServiceConfig actConfig, SqlDbService svc) {
        this.actConfig = actConfig;
        this.svc = svc;
    }

    @Override
    public DataSource createDataSource(DataSourceConfig conf) {
        ServerConfig ebeanConfig = new EbeanConfigAdaptor().adaptFrom(actConfig, conf, svc);
        DataSourceFactory factory = ebeanConfig.service(DataSourceFactory.class);
        if (factory == null) {
            throw new IllegalStateException("No DataSourceFactory service implementation found in class path."
                    + " Probably missing dependency to avaje-datasource?");
        }

        DataSourceAlertFactory alertFactory = ebeanConfig.service(DataSourceAlertFactory.class);
        org.avaje.datasource.DataSourceConfig dsConfig = ebeanConfig.getDataSourceConfig();
        if (alertFactory != null) {
            dsConfig.setAlert(alertFactory.createAlert());
        }

        if (conf.readOnly) {
            // setup to use AutoCommit such that we skip explicit commit
            dsConfig.setAutoCommit(true);
        }
        String poolName = ebeanConfig.getName() + (conf.readOnly ? "-ro" : "");
        return new EbeanDataSourceWrapper(ebeanConfig, factory.createPool(poolName, dsConfig));
    }

    @Override
    public Map<String, String> confKeyMapping() {
        return null;
    }

    @Override
    public DataSourceStatus getStatus(DataSource ds) {
        return null;
    }
}
