package playground;

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
