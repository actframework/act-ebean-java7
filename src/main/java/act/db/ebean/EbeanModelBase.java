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
import act.db.Dao;
import act.db.Model;
import org.osgl.$;

import javax.persistence.MappedSuperclass;
@MappedSuperclass
public abstract class EbeanModelBase <ID_TYPE, MODEL_TYPE extends EbeanModelBase>
        implements Model<ID_TYPE, MODEL_TYPE> {

    // for JSON deserialization
    public ID_TYPE getId() {
        return _id();
    }

    // for JSON deserialization
    public void setId(ID_TYPE id) {
        _id(id);
    }

    /**
     * Returns a {@link Dao} object that can operate on this entity of
     * the entities with the same type.
     *
     * <p>Note this method needs to be enhanced by framework to be called</p>
     *
     * @return the {@code Dao} object
     */
    public static <T extends Dao>
    T dao() {
        return (T) App.instance().dbServiceManager().dao(EbeanModelBase.class);
    }

    @Override
    public int hashCode() {
        return $.hc(getClass(), _id());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if ($.eq(obj.getClass(), getClass())) {
            Model that = (Model) obj;
            return $.eq(that._id(), this._id());
        }
        return false;
    }

    protected final MODEL_TYPE _me() {
        return (MODEL_TYPE)this;
    }

}
