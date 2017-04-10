package act.db.ebean;

import act.app.App;
import act.db.DbPlugin;
import act.db.DbService;
import act.inject.param.ParamValueLoaderService;

import java.util.Map;

public class EbeanPlugin extends DbPlugin {
    @Override
    public DbService initDbService(String id, App app, Map<String, String> conf) {
        ParamValueLoaderService.waiveFields("_ebean_intercept", "_ebean_identity");

        return new EbeanService(id, app, conf);
    }
}
