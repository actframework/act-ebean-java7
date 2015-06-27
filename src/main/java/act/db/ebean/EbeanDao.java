package act.db.ebean;

import act.app.App;
import act.app.DbServiceManager;
import act.db.*;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.ExpressionList;
import org.osgl._;
import org.osgl.util.C;
import org.osgl.util.E;

import javax.inject.Inject;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class EbeanDao<ID_TYPE, MODEL_TYPE, DAO_TYPE extends EbeanDao<ID_TYPE, MODEL_TYPE, DAO_TYPE>> extends DaoBase<ID_TYPE, MODEL_TYPE, EbeanQuery<MODEL_TYPE>, DAO_TYPE> {

    private Class<MODEL_TYPE> modelType;
    private volatile EbeanServer ebean;

    @Inject
    private App app;

    EbeanDao(Class<MODEL_TYPE> modelType, EbeanServer ebean) {
        E.NPE(modelType, ebean);
        this.modelType = modelType;
        this.ebean = ebean;
    }

    protected EbeanDao(Class<MODEL_TYPE> modelType) {
        this.modelType = modelType;
    }

    private EbeanService getService(String dbId, DbServiceManager mgr) {
        DbService svc = mgr.dbService(dbId);
        E.invalidConfigurationIf(null == svc, "Cannot find db service by id: %s", dbId);
        E.invalidConfigurationIf(!(svc instanceof EbeanService), "The db service[%s|%s] is not ebean service", dbId, svc.getClass());
        return _.cast(svc);
    }

    protected EbeanServer ebean() {
        if (null != ebean) {
            return ebean;
        }
        synchronized (this) {
            if (null == ebean) {
                DB db = modelType.getAnnotation(DB.class);
                String dbId = null == db ? DbServiceManager.DEFAULT : db.value();
                EbeanService dbService = getService(dbId, app.dbServiceManager());
                E.NPE(dbService);
                ebean = dbService.ebean();
            }
        }
        return ebean;
    }

    @Override
    public MODEL_TYPE findById(ID_TYPE id) {
        return ebean().find(modelType, id);
    }

    @Override
    public Iterable<MODEL_TYPE> findBy(String fields, Object... values) throws IllegalArgumentException {
        EbeanQuery<MODEL_TYPE> q = q(fields, values);
        return q.fetch();
    }

    @Override
    public Iterable<MODEL_TYPE> findByIdList(List<ID_TYPE> idList) {
        EbeanQuery<MODEL_TYPE> q = q();
        q.where().idIn(idList);
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
    public MODEL_TYPE reload(MODEL_TYPE model) {
        ebean().refresh(model);
        return model;
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
    public void save(MODEL_TYPE entity, String fields, Object... values) throws IllegalArgumentException {
        ebean.update(entity);
    }

    @Override
    public void delete(MODEL_TYPE entity) {
        ebean().delete(entity);
    }

    @Override
    public EbeanQuery<MODEL_TYPE> q() {
        return new EbeanQuery<MODEL_TYPE>(ebean(), modelType);
    }

    @Override
    public DAO_TYPE on(String dbId) {
        return getService(dbId, app.dbServiceManager()).dao(modelType);
    }

    public Class modelType() {
        return modelType;
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
                if (val instanceof _.T2) {
                    _.T2 t2 = _.cast(val);
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

    private EbeanQuery<MODEL_TYPE> q(String keys, Object... values) {
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
