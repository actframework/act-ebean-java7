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

import com.avaje.ebean.config.ServerConfig;
import org.avaje.datasource.DataSourcePool;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

public class EbeanDataSourceWrapper implements DataSource {

    public ServerConfig ebeanConfig;
    public DataSourcePool ebeanDs;

    public EbeanDataSourceWrapper(ServerConfig ebeanConfig, DataSourcePool ebeanDs) {
        ebeanConfig.setDataSource(ebeanDs);
        this.ebeanConfig = ebeanConfig;
        this.ebeanDs = ebeanDs;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return ebeanDs.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return ebeanDs.getConnection(username, password);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return ebeanDs.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return ebeanDs.isWrapperFor(iface);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return ebeanDs.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        ebeanDs.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        ebeanDs.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return ebeanDs.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return ebeanDs.getParentLogger();
    }
}
