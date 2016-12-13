package act.db.ebean;

import act.ActComponent;
import act.app.App;
import act.db.DbPlugin;
import act.db.DbService;
import act.inject.param.ParamValueLoaderService;

import java.util.Map;

@ActComponent
public class EbeanPlugin extends DbPlugin {
    @Override
    public DbService initDbService(String id, App app, Map<String, Object> conf) {
        ParamValueLoaderService.waiveFields("_ebean_intercept", "_ebean_identity");

        return new EbeanService(id, app, conf);
    }
}
