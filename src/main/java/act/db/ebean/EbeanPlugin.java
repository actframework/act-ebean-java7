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
import act.db.DbPlugin;
import act.db.DbService;
import act.inject.param.ParamValueLoaderService;
import osgl.version.Version;

import java.util.Map;

public class EbeanPlugin extends DbPlugin {

    public static final Version VERSION = Version.of(EbeanPlugin.class);

    @Override
    public DbService initDbService(String id, App app, Map<String, String> conf) {
        ParamValueLoaderService.waiveFields("_ebean_intercept", "_ebean_identity");

        return new EbeanService(id, app, conf);
    }
}
