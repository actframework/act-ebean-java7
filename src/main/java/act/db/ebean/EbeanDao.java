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

import static act.Act.app;

import act.app.DbServiceManager;
import act.db.*;
import act.db.Model;
import act.db.sql.tx.TxContext;
import act.util.General;
import act.util.Stateless;
import com.avaje.ebean.*;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import org.osgl.$;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.*;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import javax.persistence.Id;
import javax.sql.DataSource;

@General
public class EbeanDao<ID_TYPE, MODEL_TYPE> extends DaoBase<ID_TYPE, MODEL_TYPE, EbeanQuery<MODEL_TYPE>> {

    private static final Logger logger = L.get(EbeanDao.class);

    @Stateless private volatile EbeanServer ebean;
    @Stateless private volatile EbeanServer ebeanReadOnly;
    @Stateless private volatile EbeanService dbSvc;
    @Stateless private volatile DataSource ds;
    @Stateless private volatile DataSource dsReadOnly;
    @Stateless private String tableName;
    @Stateless private Field idField = null;
    @Stateless private List<QueryIterator> queryIterators = C.newList();

    EbeanDao(EbeanService service) {
        init(modelType());
        this.dbService(service);
    }

    EbeanDao(Class<ID_TYPE> idType, Class<MODEL_TYPE> modelType, EbeanService service) {
        super(idType, modelType);
        init(modelType);
        this.dbService(service);
        this.ds = service.dataSource();
        this.dsReadOnly = service.dataSourceReadOnly();
    }

    public EbeanDao(Class<ID_TYPE> id_type, Class<MODEL_TYPE> modelType) {
        super(id_type, modelType);
        init(modelType);
    }

    public EbeanDao() {
        init(modelType());
    }

    public void ebean(EbeanServer ebean, boolean readonly) {
        setEbean($.requireNotNull(ebean), readonly);
    }

    public void dbService(EbeanService service) {
        this.dbSvc = service;
        this.ebean(service.ebean(true), true);
        this.ebean(service.ebean(false), false);
    }

    public void modelType(Class<?> type) {
        this.modelType = $.cast(type);
    }

    @Override
    protected void releaseResources() {
        if (null != queryIterators) {
            for (QueryIterator i : queryIterators) {
                try {
                    i.close();
                } catch (Exception e) {
                    logger.warn(e, "error closing query iterators");
                }
            }
            queryIterators.clear();
            queryIterators = null;
        }
    }

    private void init(Class<MODEL_TYPE> modelType) {
        for (Field f: modelType.getDeclaredFields()) {
            Id idAnno = f.getAnnotation(Id.class);
            if (null != idAnno) {
                idField = f;
                f.setAccessible(true);
                break;
            }
        }
    }

    private void setEbean(EbeanServer ebean, boolean readonly) {
        if (readonly) {
            this.ebeanReadOnly = ebean;
            return;
        }
        this.ebean = ebean;
        this.tableName = ((SpiEbeanServer) ebean).getBeanDescriptor(modelType()).getBaseTable();
    }

    private EbeanService getService(String dbId, DbServiceManager mgr) {
        DbService svc = mgr.dbService(dbId);
        E.invalidConfigurationIf(null == svc, "Cannot find db service by id: %s", dbId);
        E.invalidConfigurationIf(!(svc instanceof EbeanService), "The db service[%s|%s] is not ebean service", dbId, svc.getClass());
        return $.cast(svc);
    }

    private EbeanServer ebean_(boolean defaultReadOnly) {
        boolean ctxReadOnly = TxContext.readOnly(defaultReadOnly);
        E.illegalStateIf(!defaultReadOnly && ctxReadOnly, "Cannot do write operation within readonly transaction");
        return ebean(ctxReadOnly);
    }

    public EbeanServer ebean(boolean readonly) {
        dbSvc.beginTxIfRequired(null);
        if (!readonly && null != ebean) {
            return ebean;
        }
        if (readonly && null != ebeanReadOnly) {
            return ebeanReadOnly;
        }
        synchronized (this) {
            DB db = modelType().getAnnotation(DB.class);
            String dbId = null == db ? DbServiceManager.DEFAULT : db.value();
            EbeanService dbService = getService(dbId, app().dbServiceManager());
            dbService(dbService);
            return readonly ? ebeanReadOnly : ebean;
        }
    }

    public DataSource ds() {
        if (null != ds) {
            return ds;
        }
        synchronized (this) {
            if (null == ds) {
                DB db = modelType().getAnnotation(DB.class);
                String dbId = null == db ? DbServiceManager.DEFAULT : db.value();
                EbeanService dbService = getService(dbId, app().dbServiceManager());
                E.NPE(dbService);
                ds = dbService.dataSource();
            }
        }
        return ds;
    }

    void registerQueryIterator(QueryIterator i) {
        queryIterators.add(i);
    }

    @Override
    public MODEL_TYPE findById(ID_TYPE id) {
        return ebean_(true).find(modelType(), id);
    }

    @Override
    public MODEL_TYPE findLatest() {
        throw E.unsupport();
    }

    @Override
    public MODEL_TYPE findLastModified() {
        throw E.unsupport();
    }

    @Override
    public Iterable<MODEL_TYPE> findBy(String fields, Object... values) throws IllegalArgumentException {
        EbeanQuery<MODEL_TYPE> q = q(fields, values);
        return q.fetch();
    }

    @Override
    public Iterable<MODEL_TYPE> findByIdList(Collection<ID_TYPE> idList) {
        EbeanQuery<MODEL_TYPE> q = q();
        q.where().idIn(C.list(idList));
        return q.fetch();
    }

    @Override
    public MODEL_TYPE findOneBy(String fields, Object... values) throws IllegalArgumentException {
        EbeanQuery<MODEL_TYPE> q = q(fields, values);
        return q.first();
    }

    @Override
    public Iterable<MODEL_TYPE> findAll() {
        return q().fetch();
    }

    @Override
    public List<MODEL_TYPE> findAllAsList() {
        return q().findList();
    }

    @Override
    public MODEL_TYPE reload(MODEL_TYPE entity) {
        ebean_(true).refresh(entity);
        return entity;
    }

    @Override
    public ID_TYPE getId(MODEL_TYPE entity) {
        if (entity instanceof Model) {
            return (ID_TYPE) ((Model) entity)._id();
        } else if (null != idField) {
            try {
                return (ID_TYPE) idField.get(entity);
            } catch (IllegalAccessException e) {
                throw E.unexpected(e);
            }
        } else {
            return null;
        }
    }

    @Override
    public long count() {
        return q().findCount();
    }

    @Override
    public long countBy(String fields, Object... values) throws IllegalArgumentException {
        EbeanQuery<MODEL_TYPE> q = q(fields, values);
        return q.count();
    }

    @Override
    public MODEL_TYPE save(MODEL_TYPE entity) {
        ebean_(false).save(entity);
        return entity;
    }

    public MODEL_TYPE save(Transaction tx, MODEL_TYPE entity) {
        ebean_(false).save(entity, tx);
        return entity;
    }

    @Override
    public List<MODEL_TYPE> save(Iterable<MODEL_TYPE> iterable) {
        List<MODEL_TYPE> list = C.list(iterable);
        if (list.isEmpty()) {
            return list;
        }
        ebean_(false).saveAll(list);
        return list;
    }

    public List<MODEL_TYPE> save(Transaction tx, Iterable<MODEL_TYPE> iterable) {
        List<MODEL_TYPE> list = C.list(iterable);
        ebean_(false).saveAll(list, tx);
        return list;
    }

    @Override
    public void save(MODEL_TYPE entity, String fields, Object... values) throws IllegalArgumentException {
        ebean_(false).update(entity);
    }

    public void save(Transaction tx, MODEL_TYPE entity, String fields, Object... values) throws IllegalArgumentException {
        ebean_(false).update(entity, tx);
    }

    @Override
    public void delete(MODEL_TYPE entity) {
        ebean_(false).delete(entity);
    }

    public void delete(Transaction tx, MODEL_TYPE entity) {
        ebean_(false).delete(entity, tx);
    }

    @Override
    public void delete(EbeanQuery<MODEL_TYPE> query) {
        ebean_(false).delete(query.rawQuery(), null);
    }

    public void delete(Transaction tx, EbeanQuery<MODEL_TYPE> query) {
        ebean_(false).delete(query.rawQuery(), tx);
    }

    @Override
    public void deleteById(ID_TYPE id) {
        ebean_(false).delete(modelType(), id);
    }

    public void deleteById(Transaction tx, ID_TYPE id) {
        ebean_(false).delete(modelType(), id, tx);
    }

    @Override
    public void deleteBy(String fields, Object... values) throws IllegalArgumentException {
        delete(q(fields, values));
    }

    public void deleteBy(Transaction tx, String fields, Object... values) throws IllegalArgumentException {
        delete(tx, q(fields, values));
    }

    @Override
    public void deleteAll() {
        delete(q());
    }

    public void deleteAll(Transaction tx) {
        delete(tx, q());
    }

    @Override
    public void drop() {
        String sql = "DELETE from " + tableName;
        SqlUpdate sqlUpdate = ebean_(false).createSqlUpdate(sql);
        ebean_(false).execute(sqlUpdate);
    }

    @Override
    public EbeanQuery<MODEL_TYPE> q() {
        return new EbeanQuery<>(this, modelType());
    }

    @Override
    public EbeanQuery<MODEL_TYPE> createQuery() {
        return q();
    }

    boolean ebeanServerProvided() {
        return null != ebean;
    }

    private enum R2 {
        betweenProperties() {
            @Override
            void applyTo(ExpressionList<?> where, String field1, String field2, Object val) {
                where.betweenProperties(field1, field2, val);
            }
        },
        bp() {
            @Override
            void applyTo(ExpressionList<?> where, String field1, String field2, Object val) {
                where.betweenProperties(field1, field2, val);
            }
        };
        abstract void applyTo(ExpressionList<?> where, String field1, String field2, Object val);
    }

    public static final String ID = "_id";

    private enum R1 {
        eq() {
            @Override
            void applyTo(ExpressionList<?> where, String field, Object val) {
                if (ID.equals(field)) {
                    where.idEq(val);
                    return;
                }
                if (null == val) {
                    where.isNull(field);
                } else {
                    where.eq(field, val);
                }
            }
        }, ne() {
            @Override
            void applyTo(ExpressionList<?> where, String field, Object val) {
                where.ne(field, val);
            }
        }, ieq() {
            @Override
            void applyTo(ExpressionList<?> where, String field, Object val) {
                where.ieq(field, val.toString());
            }
        }, between() {
            @Override
            void applyTo(ExpressionList<?> where, String field, Object val) {
                if (val instanceof $.T2) {
                    $.T2 t2 = $.cast(val);
                    where.between(field, t2._1, t2._2);
                } else if (val.getClass().isArray()) {
                    int len = Array.getLength(val);
                    if (len != 2) {
                        throw E.unexpected("<between> value array length is not correct, expected: 2; found: %s", len);
                    }
                    where.between(field, Array.get(val, 0), Array.get(val, 1));
                } else if (val instanceof Collection) {
                    int len = ((Collection) val).size();
                    if (len != 2) {
                        throw E.unexpected("<between> value collection size is not correct, expected: 2; found: %s", len);
                    }
                    Iterator<?> itr = ((Collection) val).iterator();
                    where.between(field, itr.next(), itr.next());
                } else {
                    throw E.unexpected("<between> value type not recognized: %s", val.getClass());
                }
            }
        }, gt() {
            @Override
            void applyTo(ExpressionList<?> where, String field, Object val) {
                where.gt(field, val);
            }
        }, ge() {
            @Override
            void applyTo(ExpressionList<?> where, String field, Object val) {
                where.ge(field, val);
            }
        }, lt() {
            @Override
            void applyTo(ExpressionList<?> where, String field, Object val) {
                where.lt(field, val);
            }
        }, le() {
            @Override
            void applyTo(ExpressionList<?> where, String field, Object val) {
                where.le(field, val);
            }
        }, isNull() {
            @Override
            void applyTo(ExpressionList<?> where, String field, Object val) {
                // val is not relevant here
                where.isNull(field);
            }
        }, isNotNull() {
            @Override
            void applyTo(ExpressionList<?> where, String field, Object val) {
                // val is not relevant here
                where.isNotNull(field);
            }
        }, like() {
            @Override
            void applyTo(ExpressionList<?> where, String field, Object val) {
                String s = S.string(val);
                if (!s.contains("%")) {
                    s = S.builder("%").append(s).append("%").toString();
                }
                where.like(field, s);
            }
        }, ilike() {
            @Override
            void applyTo(ExpressionList<?> where, String field, Object val) {
                String s = S.string(val);
                if (!s.contains("%")) {
                    s = S.builder("%").append(s).append("%").toString();
                }
                where.ilike(field, s);
            }
        }, startsWith() {
            @Override
            void applyTo(ExpressionList<?> where, String field, Object val) {
                where.startsWith(field, val.toString());
            }
        }, istartsWith() {
            @Override
            void applyTo(ExpressionList<?> where, String field, Object val) {
                where.istartsWith(field, val.toString());
            }
        }, endsWith() {
            @Override
            void applyTo(ExpressionList<?> where, String field, Object val) {
                where.endsWith(field, val.toString());
            }
        }, contains() {
            @Override
            void applyTo(ExpressionList<?> where, String field, Object val) {
                where.contains(field, val.toString());
            }
        }, icontains() {
            @Override
            void applyTo(ExpressionList<?> where, String field, Object val) {
                where.icontains(field, val.toString());
            }
        }, in() {
            @Override
            void applyTo(ExpressionList<?> where, String field, Object val) {
                E.NPE(field, val);
                if (val instanceof Query) {
                    where.in(field, (Query)val);
                } else if (val instanceof Collection) {
                    if (ID.equals(field)) {
                        where.idIn(C.list((Collection)val));
                    } else {
                        where.in(field, (Collection) val);
                    }
                } else if (val.getClass().isArray()) {
                    int len = Array.getLength(val);
                    if (len == 0) {
                        if (ID.equals(field)) {
                            where.idIn(C.list());
                        } else {
                            where.in(field, new Object[0]);
                        }
                    } else {
                        Object[] array = new Object[len];
                        for (int i = 0; i < len; ++i) {
                            array[i] = Array.get(val, i);
                        }
                        if (ID.equals(field)) {
                            where.idIn(C.listOf(array));
                        } else {
                            where.in(field, array);
                        }
                    }
                } else {
                    throw E.unexpected("Unknown <in> value type: %s", val.getClass());
                }
            }
        }
        ;
        abstract void applyTo(ExpressionList<?> where, String field, Object val);
    }

    private void buildWhere(ExpressionList<MODEL_TYPE> where, String key, Object val) {
        String[] sa = key.split("\\s+");
        switch (sa.length) {
            case 1:
                where.eq(sa[0], val);
                break;
            case 2:
                String op = sa[1];
                if ("!=".equalsIgnoreCase(op)) {
                    op = "ne";
                } else if ("==".equalsIgnoreCase(op)) {
                    op = "eq";
                } else if (">".equalsIgnoreCase(op)) {
                    op = "gt";
                } else if (">=".equals(op)) {
                    op = "ge";
                } else if ("<".equals(op)) {
                    op = "lt";
                } else if ("<=".equals(op)) {
                    op = "le";
                }
                R1.valueOf(op).applyTo(where, sa[0], val);
                break;
            case 3:
                R2.valueOf(sa[2]).applyTo(where, sa[0], sa[1], val);
                break;
            default:
                throw E.unexpected("Unknown where expression: %s", key);
        }
    }

    @Override
    public EbeanQuery<MODEL_TYPE> q(String keys, Object... values) {
        int len = values.length;
        E.illegalArgumentIf(len == 0, "no values supplied");
        String[] sa = keys.split("[,;:]+");
        E.illegalArgumentIf(sa.length != len, "The number of values does not match the number of fields");
        EbeanQuery<MODEL_TYPE> q = q();
        ExpressionList<MODEL_TYPE> el = q.where();
        for (int i = 0; i < len; ++i) {
            buildWhere(el, sa[i], values[i]);
        }
        return q;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> createQuery(String s, Object... objects) {
        return q(s, objects);
    }
}
