package playground;

import act.db.ebean.EbeanDao;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * Created by luog on 22/06/15.
 */
public class ContactService {
    //private EbeanDao<String, Contact, Eea> da;

    public void foo() {
    }

    public static void main(String[] args) throws Exception{
        Field f = ContactService.class.getDeclaredField("da");
        System.out.println(f.getType());
        Type type = (f.getGenericType());
        System.out.println(type);
    }
}
