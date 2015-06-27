package act.db.ebean;

import act.db.Dao;
import com.avaje.ebean.*;
import com.avaje.ebean.text.PathProperties;
import org.osgl.util.E;
import org.osgl.util.S;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EbeanQuery<MODEL_TYPE> implements Query<MODEL_TYPE>, Dao.Query<MODEL_TYPE, EbeanQuery<MODEL_TYPE>> {

    Query<MODEL_TYPE> q;

    public EbeanQuery(EbeanServer ebean, Class<MODEL_TYPE> modelType) {
        E.NPE(ebean);
        q = ebean.createQuery(modelType);
    }

    @Override
    public EbeanQuery<MODEL_TYPE> offset(int pos) {
        q.setFirstRow(pos);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> limit(int limit) {
        q.setMaxRows(limit);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> orderBy(String... fieldList) {
        E.illegalArgumentIf(fieldList.length == 0);
        q.order(S.join(" ", fieldList));
        return this;
    }

    @Override
    public MODEL_TYPE first() {
        return q.findUnique();
    }

    @Override
    public Iterable<MODEL_TYPE> fetch() {
        return new Iterable<MODEL_TYPE>() {
            @Override
            public Iterator<MODEL_TYPE> iterator() {
                return q.findIterate();
            }
        };
    }

    @Override
    public long count() {
        return q.findRowCount();
    }

    // --- Ebean Query methods: delegate to q

    @Override
    public RawSql getRawSql() {
        return q.getRawSql();
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setRawSql(RawSql rawSql) {
        q.setRawSql(rawSql);
        return this;
    }

    @Override
    public void cancel() {
        q.cancel();
    }

    @Override
    public EbeanQuery<MODEL_TYPE> copy() {
        q.copy();
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setPersistenceContextScope(PersistenceContextScope scope) {
        q.setPersistenceContextScope(scope);
        return this;
    }

    @Override
    public ExpressionFactory getExpressionFactory() {
        return q.getExpressionFactory();
    }

    @Override
    public boolean isAutofetchTuned() {
        return q.isAutofetchTuned();
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setAutofetch(boolean autofetch) {
        q.setAutofetch(autofetch);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setLazyLoadBatchSize(int lazyLoadBatchSize) {
        q.setLazyLoadBatchSize(lazyLoadBatchSize);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> select(String fetchProperties) {
        q.select(fetchProperties);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> fetch(String path, String fetchProperties) {
        q.fetch(path, fetchProperties);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> fetch(String assocProperty, String fetchProperties, FetchConfig fetchConfig) {
        q.fetch(assocProperty, fetchProperties, fetchConfig);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> fetch(String path) {
        q.fetch(path);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> fetch(String path, FetchConfig joinConfig) {
        q.fetch(path, joinConfig);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> apply(PathProperties pathProperties) {
        q.apply(pathProperties);
        return this;
    }

    @Override
    public List<Object> findIds() {
        return q.findIds();
    }

    @Override
    public QueryIterator<MODEL_TYPE> findIterate() {
        return q.findIterate();
    }

    @Override
    @Deprecated
    public void findVisit(QueryResultVisitor<MODEL_TYPE> visitor) {
        q.findVisit(visitor);
    }

    @Override
    public void findEach(QueryEachConsumer<MODEL_TYPE> consumer) {
        q.findEach(consumer);
    }

    @Override
    public void findEachWhile(QueryEachWhileConsumer<MODEL_TYPE> consumer) {
        q.findEachWhile(consumer);
    }

    @Override
    public List<MODEL_TYPE> findList() {
        return q.findList();
    }

    @Override
    public Set<MODEL_TYPE> findSet() {
        return q.findSet();
    }

    @Override
    public Map<?, MODEL_TYPE> findMap() {
        return q.findMap();
    }

    @Override
    public <K> Map<K, MODEL_TYPE> findMap(String keyProperty, Class<K> keyType) {
        return q.findMap(keyProperty, keyType);
    }

    @Override
    public MODEL_TYPE findUnique() {
        return q.findUnique();
    }

    @Override
    public int findRowCount() {
        return q.findRowCount();
    }

    @Override
    public FutureRowCount<MODEL_TYPE> findFutureRowCount() {
        return q.findFutureRowCount();
    }

    @Override
    public FutureIds<MODEL_TYPE> findFutureIds() {
        return q.findFutureIds();
    }

    @Override
    public FutureList<MODEL_TYPE> findFutureList() {
        return q.findFutureList();
    }

    @Override
    public PagedList<MODEL_TYPE> findPagedList(int pageIndex, int pageSize) {
        return q.findPagedList(pageIndex, pageSize);
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setParameter(String name, Object value) {
        q.setParameter(name, value);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setParameter(int position, Object value) {
        q.setParameter(position, value);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setId(Object id) {
        q.setId(id);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> where(String addToWhereClause) {
        q.where(addToWhereClause);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> where(Expression expression) {
        q.where(expression);
        return this;
    }

    @Override
    public ExpressionList<MODEL_TYPE> where() {
        return q.where();
    }

    @Override
    public ExpressionList<MODEL_TYPE> filterMany(String propertyName) {
        return q.filterMany(propertyName);
    }

    @Override
    public ExpressionList<MODEL_TYPE> having() {
        return q.having();
    }

    @Override
    public EbeanQuery<MODEL_TYPE> having(String addToHavingClause) {
        q.having(addToHavingClause);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> having(Expression addExpressionToHaving) {
        q.having(addExpressionToHaving);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> orderBy(String orderByClause) {
        q.orderBy(orderByClause);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> order(String orderByClause) {
        q.order(orderByClause);
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
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setOrderBy(OrderBy<MODEL_TYPE> orderBy) {
        q.setOrderBy(orderBy);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setDistinct(boolean isDistinct) {
        q.setDistinct(isDistinct);
        return this;
    }

    @Override
    public int getFirstRow() {
        return q.getFirstRow();
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setFirstRow(int firstRow) {
        q.setFirstRow(firstRow);
        return this;
    }

    @Override
    public int getMaxRows() {
        return q.getMaxRows();
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setMaxRows(int maxRows) {
        q.setMaxRows(maxRows);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setMapKey(String mapKey) {
        q.setMapKey(mapKey);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setUseCache(boolean useBeanCache) {
        q.setUseCache(useBeanCache);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setUseQueryCache(boolean useQueryCache) {
        q.setUseQueryCache(useQueryCache);
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
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setTimeout(int secs) {
        q.setTimeout(secs);
        return this;
    }

    @Override
    public EbeanQuery<MODEL_TYPE> setBufferFetchSizeHint(int fetchSize) {
        q.setBufferFetchSizeHint(fetchSize);
        return this;
    }

    @Override
    public String getGeneratedSql() {
        return q.getGeneratedSql();
    }

    @Override
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
        return this;
    }
}
