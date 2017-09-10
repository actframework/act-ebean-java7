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

import act.Act;
import act.app.App;
import act.app.event.AppEventId;
import act.db.DbService;
import act.db.di.DaoInjectionListenerBase;
import act.event.AppEventListenerBase;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.util.Generics;

import java.lang.reflect.Type;
import java.util.EventObject;
import java.util.List;

public class EbeanDaoInjectionListener extends DaoInjectionListenerBase {

    @Override
    public Class[] listenTo() {
        return new Class[]{EbeanDao.class};
    }

    @Override
    public void onInjection(Object injectee, BeanSpec spec) {
        final EbeanDao dao = $.cast(injectee);
        if (dao.ebeanServerProvided()) {
            return;
        }
        List<Type> typeParameters = spec.typeParams();
        if (typeParameters.isEmpty()) {
            typeParameters = Generics.typeParamImplementations(spec.rawType(), EbeanDao.class);
        }
        if (null == typeParameters) {
            logger.warn("No type parameter information provided");
            return;
        }
        final $.T2<Class, String> resolved = resolve(typeParameters);
        DbService dbService = App.instance().dbServiceManager().dbService(resolved._2);
        if (dbService instanceof EbeanService) {
            final EbeanService service = $.cast(dbService);
            Act.eventBus().bind(AppEventId.DB_SVC_LOADED, new AppEventListenerBase() {
                @Override
                public void on(EventObject eventObject) throws Exception {
                    dao.ebean(service.ebean());
                    dao.modelType(resolved._1);
                }
            });
        }
    }
}
