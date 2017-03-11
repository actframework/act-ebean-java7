package act.db.ebean;

import act.app.App;
import act.db.DbService;
import act.db.di.DaoInjectionListenerBase;
import org.osgl.$;
import org.osgl.inject.BeanSpec;
import org.osgl.util.Generics;

import java.lang.reflect.Type;
import java.util.List;

public class EbeanDaoInjectionListener extends DaoInjectionListenerBase {

    @Override
    public Class[] listenTo() {
        return new Class[]{EbeanDao.class};
    }

    @Override
    public void onInjection(Object injectee, BeanSpec spec) {
        List<Type> typeParameters = spec.typeParams();
        if (typeParameters.isEmpty()) {
            typeParameters = Generics.typeParamImplementations(spec.rawType(), EbeanDao.class);
        }
        if (null == typeParameters) {
            logger.warn("No type parameter information provided");
            return;
        }
        $.T2<Class, String> resolved = resolve(typeParameters);
        DbService dbService = App.instance().dbServiceManager().dbService(resolved._2);
        if (dbService instanceof EbeanService) {
            EbeanService service = $.cast(dbService);
            EbeanDao dao = $.cast(injectee);
            dao.ebean(service.ebean());
            dao.modelType(resolved._1);
        }
    }
}
