package act.db.ebean;

import act.app.App;
import act.app.DbServiceManager;
import act.db.DB;
import act.db.DaoBase;
import act.db.DbService;
import act.db.Model;
import act.util.General;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.QueryIterator;
import com.avaje.ebean.SqlUpdate;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import org.osgl.$;
import org.osgl.logging.L;
import org.osgl.logging.Logger;
import org.osgl.util.C;
import org.osgl.util.E;

import javax.inject.Inject;
import javax.persistence.Id;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@General
public class EbeanDao<ID_TYPE, MODEL_TYPE> extends DaoBase<ID_TYPE, MODEL_TYPE, EbeanQuery<MODEL_TYPE>> {

    private static final Logger logger = L.get(EbeanDao.class);

    private volatile EbeanServer ebean;
    private String tableName;
    private Field idField = null;
    private List<QueryIterator> queryIterators = C.newList();

    private App app;

    EbeanDao(EbeanService service) {
        for (Field f: ((Class<?>)modelType).getDeclaredFields()) {
            Id idAnno = f.getAnnotation(Id.class);
            if (null != idAnno) {
                idField = f;
                f.setAccessible(true);
                break;
            }
        }
        this.ebean = service.ebean();
        this.tableName = ((SpiEbeanServer) ebean).getBeanDescriptor((Class<?>)modelType).getBaseTable();
        this.app = service.app();
    }

    @Deprecated
    EbeanDao(Class<ID_TYPE> idType, Class<MODEL_TYPE> modelType, EbeanService service) {
        super(idType, modelType);
        E.NPE(modelType, service.ebean());
        for (Field f: modelType.getDeclaredFields()) {
            Id idAnno = f.getAnnotation(Id.class);
            if (null != idAnno) {
                idField = f;
                f.setAccessible(true);
                break;
            }
        }
        this.ebean = service.ebean();
        this.tableName = ((SpiEbeanServer) ebean).getBeanDescriptor(modelType).getBaseTable();
        this.app = service.app();
    }

    @Inject
    protected EbeanDao(Class<ID_TYPE> id_type, Class<MODEL_TYPE> modelType) {
        super(id_type, modelType);
        this.app = App.instance();
    }

    public void ebean(EbeanServer ebean) {
        this.ebean = $.notNull(ebean);
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

    private EbeanService getService(String dbId, DbServiceManager mgr) {
        DbService svc = mgr.dbService(dbId);
        E.invalidConfigurationIf(null == svc, "Cannot find db service by id: %s", dbId);
        E.invalidConfigurationIf(!(svc instanceof EbeanService), "The db service[%s|%s] is not ebean service", dbId, svc.getClass());
        return $.cast(svc);
    }

    protected EbeanServer ebean() {
        if (null != ebean) {
            return ebean;
        }
        synchronized (this) {
            if (null == ebean) {
                DB db = ((Class<?>)modelType).getAnnotation(DB.class);
                String dbId = null == db ? DbServiceManager.DEFAULT : db.value();
                EbeanService dbService = getService(dbId, app.dbServiceManager());
                E.NPE(dbService);
                ebean = dbService.ebean();
            }
        }
        return ebean;
    }

    void registerQueryIterator(QueryIterator i) {
        queryIterators.add(i);
    }

    @Override
    public MODEL_TYPE findById(ID_TYPE id) {
        return (MODEL_TYPE) ebean().find((Class<?>)modelType, id);
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
        ebean().refresh(entity);
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
        return q().findRowCount();
    }

    @Override
    public long countBy(String fields, Object... values) throws IllegalArgumentException {
        EbeanQuery<MODEL_TYPE> q = q(fields, values);
        return q.count();
    }

    @Override
    public void save(MODEL_TYPE entity) {
        ebean().save(entity);
    }

    @Override
    public void save(Iterable<MODEL_TYPE> iterable) {
        ebean().saveAll(C.list(iterable));
    }

    @Override
    public void save(MODEL_TYPE entity, String fields, Object... values) throws IllegalArgumentException {
        ebean.update(entity);
    }

    @Override
    public void delete(MODEL_TYPE entity) {
        ebean().delete(entity);
    }

    @Override
    public void delete(EbeanQuery<MODEL_TYPE> query) {
        ebean().delete(query);
    }

    @Override
    public void deleteById(ID_TYPE id) {
        ebean().delete(modelType(), id);
    }

    @Override
    public void deleteBy(String fields, Object... values) throws IllegalArgumentException {
        EbeanQuery<MODEL_TYPE> q = q(fields, values);
        ebean().delete(q);
    }

    @Override
    public void drop() {
        String sql = "DELETE from " + tableName;
        SqlUpdate sqlUpdate = ebean().createSqlUpdate(sql);
        ebean().execute(sqlUpdate);
    }

    @Override
    public EbeanQuery<MODEL_TYPE> q() {
        return new EbeanQuery<MODEL_TYPE>(this, (Class)modelType);
    }

    public Class modelType() {
        return (Class)modelType;
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
                where.like(field, val.toString());
            }
        }, ilike() {
            @Override
            void applyTo(ExpressionList<?> where, String field, Object val) {
                where.ilike(field, val.toString());
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
                R1.valueOf(sa[1]).applyTo(where, sa[0], val);
                break;
            case 3:
                R2.valueOf(sa[2]).applyTo(where, sa[0], sa[1], val);
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

}
