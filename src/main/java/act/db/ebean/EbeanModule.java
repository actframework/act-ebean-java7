package act.db.ebean;

import act.db.ebean.util.EbeanDaoLoader;
import org.osgl.inject.Module;

@SuppressWarnings("unused")
public class EbeanModule extends Module {

    @Override
    protected void configure() {
        registerGenericTypedBeanLoader(EbeanDao.class, new EbeanDaoLoader());
    }

}
