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

import act.db.Dao;
import com.avaje.ebean.*;
import org.jetbrains.annotations.Nullable;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.E;
import org.osgl.util.Generics;
import org.osgl.util.S;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EbeanQuery<MODEL_TYPE> implements Query<MODEL_TYPE>, Dao.Query<MODEL_TYPE, EbeanQuery<MODEL_TYPE>> {

    protected Class<MODEL_TYPE> modelType;

    Query<MODEL_TYPE> q;
    Query<MODEL_TYPE> qReadOnly;
    EbeanDao dao;

    public EbeanQuery() {
        List<Type> typeParams = Generics.typeParamImplementations(getClass(), EbeanQuery.class);
        int sz = typeParams.size();
        if (sz > 1) {
            dao = $.cast(typeParams.get(1));
        }
        if (sz > 0) {
            modelType = $.cast(typeParams.get(0));
        }
    }

    public EbeanQuery(EbeanDao dao, Class<MODEL_TYPE> modelType) {
        this.modelType = modelType;

        q = dao.ebean(false).createQuery(modelType);
        qReadOnly = dao.ebean(true).createQuery(modelType);
        syncEbeanQueries();

        this.dao = dao;
    }

    private void syncEbeanQueries() {

        _sync("detail");

        q.orderBy();
        _sync("orderBy");

        q.text();
        _sync("textExpressions");

        q.where();
        _sync("whereExpressions");

        q.having();
        _sync("havingExpressions");

    }

    private void _sync(String property) {
        $.setProperty(qReadOnly, $.getProperty(q, property), property);
    }

    public Query<MODEL_TYPE> rawQuery() {
        return q;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> asOf(Timestamp timestamp) {
        q.asOf(timestamp);
        qReadOnly.asOf(timestamp);
        return this;
    }

    @Override
    public List<Version<MODEL_TYPE>> findVersions() {
        return qReadOnly.findVersions();
    }

    @Override
    public List<Version<MODEL_TYPE>> findVersionsBetween(Timestamp timestamp, Timestamp timestamp1) {
        return qReadOnly.findVersionsBetween(timestamp, timestamp1);
    }

    @Override
    public RawSql getRawSql() {
        return q.getRawSql();
    }

    @Override
    public void findEach(QueryEachConsumer<MODEL_TYPE> consumer) {
        qReadOnly.findEach(consumer);
    }

    @Override
    public void findEachWhile(QueryEachWhileConsumer<MODEL_TYPE> consumer) {
        qReadOnly.findEachWhile(consumer);
    }

    @Nullable
    @Override
    public MODEL_TYPE findUnique() {
        return qReadOnly.findUnique();
    }

    @Override
    public EbeanQuery<MODEL_TYPE> apply(FetchPath fetchPath) {
        q.apply(fetchPath);
        qReadOnly.apply(fetchPath);
        return this;
    }

    @Override
    public ExpressionList<MODEL_TYPE> text() {
        return q.text();
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setUseDocStore(boolean b) {
        q.setUseDocStore(b);
        qReadOnly.setUseDocStore(b);
        return this;
    }

    @Override
    public int update() {
        return q.update();
    }

    @Override
    public int delete() {
        return q.delete();
    }

    @Override
    public Object getId() {
        return qReadOnly.getId();
    }

    @Override
    public Class<MODEL_TYPE> getBeanType() {
        return qReadOnly.getBeanType();
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setDisableLazyLoading(boolean b) {
        q.setDisableLazyLoading(b);
        qReadOnly.setDisableLazyLoading(b);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> offset(int pos) {
        q.setFirstRow(pos);
        qReadOnly.setFirstRow(pos);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> limit(int limit) {
        q.setMaxRows(limit);
        qReadOnly.setMaxRows(limit);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> orderBy(String... fieldList) {
        E.illegalArgumentIf(fieldList.length == 0);
        q.order(S.join(" ", fieldList));
        qReadOnly.order(S.join(" ", fieldList));
        return this;
    }

    @Override
    public MODEL_TYPE first() {
        List<MODEL_TYPE> list = qReadOnly.setMaxRows(1).findList();
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public EbeanQuery<MODEL_TYPE> fetchQuery(String path, String fetchProperties) {
        q.fetchQuery(path, fetchProperties);
        qReadOnly.fetchQuery(path, fetchProperties);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> fetchLazy(String path, String fetchProperties) {
        q.fetchLazy(path, fetchProperties);
        qReadOnly.fetchLazy(path, fetchProperties);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> fetchQuery(String path) {
        q.fetchQuery(path);
        qReadOnly.fetchQuery(path);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> fetchLazy(String path) {
        q.fetchLazy(path);
        qReadOnly.fetchLazy(path);
        return this;
    }

    @Override
    public Iterable<MODEL_TYPE> fetch() {
        C.List<MODEL_TYPE> list = C.newList();
        QueryIterator<MODEL_TYPE> qi = findIterate();
        while (qi.hasNext()) {
            list.add(qi.next());
        }
        qi.close();
        return list;
// we need to close the query iterable right now otherwise it holds the data connection forever
//        return new Iterable<MODEL_TYPE>() {
//            @Override
//            public Iterator<MODEL_TYPE> iterator() {
//                return findIterate();
//            }
//        };
    }

    @Override
    public long count() {
        return qReadOnly.findCount();
    }

    // --- Ebean Query methods: delegate to q


    @Override
    public EbeanQuery<MODEL_TYPE> setIncludeSoftDeletes() {
        q.setIncludeSoftDeletes();
        qReadOnly.setIncludeSoftDeletes();
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> asDraft() {
        q.asDraft();
        qReadOnly.asDraft();
        return this;
    }

    @Override
    public boolean isAutoTuned() {
        return qReadOnly.isAutoTuned();
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setAutoTune(boolean b) {
        q.setAutoTune(b);
        qReadOnly.setAutoTune(b);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setDisableReadAuditing() {
        q.setDisableReadAuditing();
        qReadOnly.setDisableReadAuditing();
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setRawSql(RawSql rawSql) {
        q.setRawSql(rawSql);
        qReadOnly.setRawSql(rawSql);
        return this;
    }

    public void setDefaultRawSqlIfRequired() {
    }

    @Override
    public void cancel() {
        q.cancel();
        qReadOnly.cancel();
    }

    @Override
    public EbeanQuery<MODEL_TYPE> copy() {
        EbeanQuery<MODEL_TYPE> copy = new EbeanQuery<>();
        copy.q = q.copy();
        copy.qReadOnly = qReadOnly.copy();
        return copy;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setPersistenceContextScope(PersistenceContextScope scope) {
        q.setPersistenceContextScope(scope);
        qReadOnly.setPersistenceContextScope(scope);
        return this;
    }

    @Override
    public ExpressionFactory getExpressionFactory() {
        return qReadOnly.getExpressionFactory();
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setLazyLoadBatchSize(int lazyLoadBatchSize) {
        q.setLazyLoadBatchSize(lazyLoadBatchSize);
        qReadOnly.setLazyLoadBatchSize(lazyLoadBatchSize);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> select(String fetchProperties) {
        q.select(fetchProperties);
        qReadOnly.select(fetchProperties);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> fetch(String path, String fetchProperties) {
        q.fetch(path, fetchProperties);
        qReadOnly.fetch(path, fetchProperties);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> fetch(String assocProperty, String fetchProperties, FetchConfig fetchConfig) {
        q.fetch(assocProperty, fetchProperties, fetchConfig);
        qReadOnly.fetch(assocProperty, fetchProperties, fetchConfig);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> fetch(String path) {
        q.fetch(path);
        qReadOnly.fetch(path);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> fetch(String path, FetchConfig joinConfig) {
        q.fetch(path, joinConfig);
        qReadOnly.fetch(path, joinConfig);
        return this;
    }

    @Override
    public <A> List<A> findIds() {
        return qReadOnly.findIds();
    }

    @Override
    public QueryIterator<MODEL_TYPE> findIterate() {
        QueryIterator<MODEL_TYPE> i = qReadOnly.findIterate();
        dao.registerQueryIterator(i);
        return i;
    }

    public void consume($.Visitor<MODEL_TYPE> visitor) {
        QueryIterator<MODEL_TYPE> i = qReadOnly.findIterate();
        try {
            while (i.hasNext()) {
                MODEL_TYPE entity = i.next();
                visitor.visit(entity);
            }
        } finally {
            i.close();
        }
    }

    @Override
    public List<MODEL_TYPE> findList() {
        return qReadOnly.findList();
    }

    @Override
    public Set<MODEL_TYPE> findSet() {
        return qReadOnly.findSet();
    }

    @Override
    public <K> Map<K, MODEL_TYPE> findMap() {
        return qReadOnly.findMap();
    }

    @Override
    public <A> List<A> findSingleAttributeList() {
        return qReadOnly.findSingleAttributeList();
    }

    @Override
    public int findCount() {
        return qReadOnly.findCount();
    }

    public MODEL_TYPE findOne() {
        return qReadOnly.findUnique();
    }

    @Override
    public FutureRowCount<MODEL_TYPE> findFutureCount() {
        return qReadOnly.findFutureCount();
    }

    @Override
    public FutureIds<MODEL_TYPE> findFutureIds() {
        return qReadOnly.findFutureIds();
    }

    @Override
    public FutureList<MODEL_TYPE> findFutureList() {
        return qReadOnly.findFutureList();
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setParameter(String name, Object value) {
        q.setParameter(name, value);
        qReadOnly.setParameter(name, value);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setParameter(int position, Object value) {
        q.setParameter(position, value);
        qReadOnly.setParameter(position, value);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setId(Object id) {
        q.setId(id);
        qReadOnly.setId(id);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> where(Expression expression) {
        q.where(expression);
        qReadOnly.where(expression);
        return this;
    }

    @Override
    public ExpressionList<MODEL_TYPE> where() {
        return qReadOnly.where();
    }

    @Override
    public ExpressionList<MODEL_TYPE> filterMany(String propertyName) {
        return qReadOnly.filterMany(propertyName);
    }

    @Override
    public ExpressionList<MODEL_TYPE> having() {
        return q.having();
    }

    @Override
    public EbeanQuery<MODEL_TYPE> having(Expression addExpressionToHaving) {
        q.having(addExpressionToHaving);
        qReadOnly.having(addExpressionToHaving);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> orderBy(String orderByClause) {
        if (S.blank(orderByClause)) {
            return this;
        }
        q.orderBy(orderByClause);
        qReadOnly.orderBy(orderByClause);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> order(String orderByClause) {
        q.order(orderByClause);
        qReadOnly.order(orderByClause);
        return this;
    }

    @Override
    public OrderBy<MODEL_TYPE> order() {
        return q.order();
    }

    @Override
    public OrderBy<MODEL_TYPE> orderBy() {
        return q.orderBy();
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setOrder(OrderBy<MODEL_TYPE> orderBy) {
        q.setOrder(orderBy);
        qReadOnly.setOrder(orderBy);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setOrderBy(OrderBy<MODEL_TYPE> orderBy) {
        q.setOrderBy(orderBy);
        qReadOnly.setOrderBy(orderBy);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setDistinct(boolean isDistinct) {
        q.setDistinct(isDistinct);
        qReadOnly.setDistinct(isDistinct);
        return this;
    }

    @Override
    public int getFirstRow() {
        return q.getFirstRow();
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setFirstRow(int firstRow) {
        q.setFirstRow(firstRow);
        qReadOnly.setFirstRow(firstRow);
        return this;
    }

    @Override
    public int getMaxRows() {
        return q.getMaxRows();
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setMaxRows(int maxRows) {
        q.setMaxRows(maxRows);
        qReadOnly.setMaxRows(maxRows);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setMapKey(String mapKey) {
        q.setMapKey(mapKey);
        qReadOnly.setMapKey(mapKey);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setUseCache(boolean useBeanCache) {
        q.setUseCache(useBeanCache);
        qReadOnly.setUseCache(useBeanCache);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setUseQueryCache(boolean useQueryCache) {
        q.setUseQueryCache(useQueryCache);
        qReadOnly.setUseQueryCache(useQueryCache);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setReadOnly(boolean readOnly) {
        q.setReadOnly(readOnly);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setLoadBeanCache(boolean loadBeanCache) {
        q.setLoadBeanCache(loadBeanCache);
        qReadOnly.setLoadBeanCache(loadBeanCache);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setTimeout(int secs) {
        q.setTimeout(secs);
        qReadOnly.setTimeout(secs);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setBufferFetchSizeHint(int fetchSize) {
        q.setBufferFetchSizeHint(fetchSize);
        qReadOnly.setBufferFetchSizeHint(fetchSize);
        return this;
    }

    @Override
    public String getGeneratedSql() {
        return q.getGeneratedSql();
    }

    @Override
    @Deprecated
    public EbeanQuery<MODEL_TYPE> setForUpdate(boolean forUpdate) {
        q.setForUpdate(forUpdate);
        return this;
    }

    @Override
    public boolean isForUpdate() {
        return q.isForUpdate();
    }

    @Override
    public EbeanQuery<MODEL_TYPE> alias(String alias) {
        q.alias(alias);
        qReadOnly.alias(alias);
        return this;
    }

    @Override
    public PagedList<MODEL_TYPE> findPagedList() {
        return qReadOnly.findPagedList();
    }

    @Override
    public Set<String> validate() {
        return q.validate();
    }

}
