package act.db.jpa;

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

import act.db.EntityClassRepository;
import act.util.AnnotatedClassFinder;
import org.osgl.$;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.Entity;
import javax.persistence.Table;

@Singleton
public final class EntityFinder {

    private final EntityClassRepository repo;

    @Inject
    public EntityFinder(EntityClassRepository repo) {
        this.repo = $.notNull(repo);
    }

    @AnnotatedClassFinder(Entity.class)
    public void foundEntity(Class<?> entityClass) {
        repo.registerModelClass(entityClass);
    }

    @AnnotatedClassFinder(Table.class)
    public void foundTable(Class<?> tableClass) {
        repo.registerModelClass(tableClass);
    }

}
