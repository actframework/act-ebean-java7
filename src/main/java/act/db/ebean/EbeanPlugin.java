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

import act.app.App;
import act.asm.Opcodes;
import act.db.DbPlugin;
import act.db.DbService;
import act.db.sql.tx.TxError;
import act.db.sql.tx.TxInfo;
import act.db.sql.tx.TxStart;
import act.db.sql.tx.TxStop;
import act.event.ActEventListenerBase;
import act.inject.param.ParamValueLoaderService;
import com.avaje.ebean.TxScope;
import com.avaje.ebeaninternal.api.HelpScopeTrans;
import com.avaje.ebeaninternal.api.ScopeTrans;
import org.osgl.OsglConfig;
import osgl.version.Version;

import java.util.EventObject;
import java.util.Map;

public class EbeanPlugin extends DbPlugin {

    public static final Version VERSION = Version.of(EbeanPlugin.class);

    private static final ThreadLocal<ScopeTrans> txHolder = new ThreadLocal<>();

    @Override
    public void register() {
        super.register();
        registerGlobalMappingFilter();
    }

    @Override
    protected void applyTo(App app) {
        super.applyTo(app);
        app.eventBus().bind(TxStart.class, new ActEventListenerBase<TxStart>() {
            @Override
            public void on(TxStart eventObject) {
                TxInfo info = eventObject.source();
                TxScope scope = new TxScope();
                scope.setReadOnly(info.readOnly());
                ScopeTrans tx = HelpScopeTrans.createScopeTrans(scope);
                txHolder.set(tx);
            }
        }).bind(TxStop.class, new ActEventListenerBase() {
            @Override
            public void on(EventObject eventObject) throws Exception {
                ScopeTrans tx = txHolder.get();
                if (null != tx) {
                    tx.onExit(null, 1);
                }
            }
        }).bind(TxError.class, new ActEventListenerBase<TxError>() {
            @Override
            public void on(TxError eventObject) throws Exception {
                ScopeTrans tx = txHolder.get();
                if (null != tx) {
                    Throwable cause = eventObject.source();
                    tx.onExit(cause, Opcodes.ATHROW);
                }
            }
        });

    }

    @Override
    public DbService initDbService(String id, App app, Map<String, String> conf) {
        ParamValueLoaderService.waiveFields("_ebean_intercept", "_ebean_identity");

        return new EbeanService(id, app, conf);
    }

    private void registerGlobalMappingFilter() {
        OsglConfig.addGlobalMappingFilter("starts:_ebean_");
    }

}
