package act.db.ebean;

import act.app.App;
import act.db.DbPlugin;
import act.db.DbService;

import java.util.Map;

public class EbeanPlugin extends DbPlugin {
    @Override
    public DbService initDbService(String id, App app, Map<String, Object> conf) {
        return new EbeanService(id, app, conf);
    }
}
