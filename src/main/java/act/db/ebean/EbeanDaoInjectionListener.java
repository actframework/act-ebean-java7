package act.db.ebean;

import act.app.App;
import act.db.Dao;
import act.db.DbService;
import act.di.guice.DaoInjectionListenerBase;
import org.osgl.$;

public class EbeanDaoInjectionListener extends DaoInjectionListenerBase {

    @Override
    public Class<? extends Dao> targetDaoType() {
        return EbeanDao.class;
    }

    @Override
    public void afterInjection(Dao dao) {
        DbService dbService = App.instance().dbServiceManager().dbService(svcId());
        if (dbService instanceof EbeanService) {
            EbeanDao ebeanDao = $.cast(dao);
            EbeanService ebeanService = $.cast(dbService);
            ebeanDao.ebean(ebeanService.ebean());
            ebeanDao.modelType(modelType());
        }
    }
}
