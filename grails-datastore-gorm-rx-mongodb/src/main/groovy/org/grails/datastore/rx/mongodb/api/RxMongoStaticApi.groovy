package org.grails.datastore.rx.mongodb.api

import com.mongodb.ReadPreference
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Projections
import com.mongodb.client.model.TextSearchOptions
import com.mongodb.rx.client.*
import grails.gorm.rx.mongodb.MongoCriteriaBuilder
import grails.gorm.rx.mongodb.RxMongoEntity
import grails.gorm.rx.mongodb.api.RxMongoAllOperations
import grails.gorm.rx.mongodb.api.RxMongoStaticOperations
import grails.gorm.rx.multitenancy.Tenants
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.bson.BsonDocument
import org.bson.Document
import org.bson.conversions.Bson
import org.grails.datastore.mapping.engine.internal.MappingUtils
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.multitenancy.MultiTenancySettings
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClientImplementor
import org.grails.datastore.rx.mongodb.client.DelegatingRxMongoDatastoreClient
import org.grails.gorm.rx.api.RxGormStaticApi
import rx.Observable
/**
 * Subclasses {@link RxMongoStaticApi} and provides additional functionality specific to MongoDB
 *
 * @param <D> The type of the domain class
 * @author Graeme Rocher
 */
@CompileStatic
class RxMongoStaticApi<D> extends RxGormStaticApi<D> implements RxMongoAllOperations<D> {
    final RxMongoDatastoreClientImplementor mongoDatastoreClient

    RxMongoStaticApi(PersistentEntity entity, RxMongoDatastoreClientImplementor datastoreClient) {
        super(entity, datastoreClient)
        this.mongoDatastoreClient = datastoreClient
    }
    /**
     * Finds all of the entities in the collection.
     *
     * @param filter the query filter
     * @return the find observable interface
     */
    FindObservable<D> find(Bson filter) {
        def findObservable = getCollection().find(filter)
        return addMultiTenantFilterIfNecessary(findObservable)
    }

    @Override
    MongoDatabase getDB() {
        String databaseName = mongoDatastoreClient.getDatabaseName(entity)
        return mongoDatastoreClient.nativeInterface.getDatabase(databaseName)
    }

    @Override
    MongoCollection<D> getCollection() {
        return mongoDatastoreClient.getCollection(entity, entity.javaClass)
    }

    @Override
    RxMongoStaticOperations<D> withCollection(String name) {
        def delegatingClient = new DelegatingRxMongoDatastoreClient(mongoDatastoreClient)
        delegatingClient.targetCollectionName = name
        def delegateApi = new RxMongoStaticApi<D>(entity, delegatingClient)
        return delegateApi
    }

    @Override
    def <T> T withClient(MongoClient client, @DelegatesTo(RxMongoStaticOperations) Closure<T> callable) {
        def delegatingClient = new DelegatingRxMongoDatastoreClient(mongoDatastoreClient)
        delegatingClient.targetMongoClient = client
        def delegateApi = new RxMongoStaticApi<D>(entity, delegatingClient)
        callable.setResolveStrategy(Closure.DELEGATE_FIRST)
        callable.setDelegate(delegateApi)
        return callable.call()
    }

    @Override
    RxMongoStaticOperations<D> withClient(MongoClient client) {
        def delegatingClient = new DelegatingRxMongoDatastoreClient(mongoDatastoreClient)
        delegatingClient.targetMongoClient = client
        def delegateApi = new RxMongoStaticApi<D>(entity, delegatingClient)
        return delegateApi
    }

    @Override
    def <T> T withCollection(String name, @DelegatesTo(RxMongoStaticOperations) Closure<T> callable) {
        def delegatingClient = new DelegatingRxMongoDatastoreClient(mongoDatastoreClient)
        delegatingClient.targetCollectionName = name
        def delegateApi = new RxMongoStaticApi<D>(entity, delegatingClient)
        callable.setResolveStrategy(Closure.DELEGATE_FIRST)
        callable.setDelegate(delegateApi)
        return callable.call()
    }

    @Override
    def <T> T withDatabase(String name, @DelegatesTo(RxMongoStaticOperations) Closure<T> callable) {
        def delegatingClient = new DelegatingRxMongoDatastoreClient(mongoDatastoreClient)
        delegatingClient.targetDatabaseName = name
        def delegateApi = new RxMongoStaticApi<D>(entity, delegatingClient)
        callable.setResolveStrategy(Closure.DELEGATE_FIRST)
        callable.setDelegate(delegateApi)
        return callable.call()
    }

    @Override
    RxMongoStaticOperations<D> withDatabase(String name) {
        def delegatingClient = new DelegatingRxMongoDatastoreClient(mongoDatastoreClient)
        delegatingClient.targetDatabaseName = name
        def delegateApi = new RxMongoStaticApi<D>(entity, delegatingClient)
        return delegateApi
    }

    @Override
    MongoCriteriaBuilder<D> createCriteria() {
        return new MongoCriteriaBuilder<D>(entity.javaClass, mongoDatastoreClient, mongoDatastoreClient.mappingContext)
    }

    @Override
    Observable<Integer> countHits(String query, Map options = Collections.emptyMap()) {
        search(query, options).reduce(0, { Integer i, D d ->
            return ++i
        })
    }

    @Override
    Observable<D> search(String query, Map options = Collections.emptyMap()) {
        def coll = mongoDatastoreClient.getCollection(entity, entity.javaClass)
        Bson search
        if(options.language) {
            search = Filters.text(query, new TextSearchOptions().language(options.language.toString()))
        }
        else {
            search = Filters.text(query)
        }
        def findObservable = coll.find(search)
        int offset = options.offset instanceof Number ? ((Number)options.offset).intValue() : 0
        int max = options.max instanceof Number ? ((Number)options.max).intValue() : -1
        if(offset > 0) findObservable.skip(offset)
        if(max > -1) findObservable.limit(max)
        findObservable = addMultiTenantFilterIfNecessary(findObservable)
        findObservable.toObservable()
    }


    @Override
    Observable<D> searchTop(String query, int limit = 5, Map options = Collections.emptyMap()) {
        def coll = mongoDatastoreClient.getCollection(entity, entity.javaClass)

        Bson search
        if(options.language) {
            search = Filters.text(query, new TextSearchOptions().language(options.language.toString()))
        }
        else {
            search = Filters.text(query)
        }

        def score = Projections.metaTextScore("score")
        def findObservable = coll.find(search)
                .projection(score)
                .sort(score)
                .limit(limit)

        findObservable = addMultiTenantFilterIfNecessary(findObservable)
        findObservable.toObservable()
    }

    @Override
    Observable<D> aggregate(List pipeline, Map<String, Object> options = Collections.emptyMap()) {
        def mongoCollection = mongoDatastoreClient.getCollection(entity, entity.javaClass)

        if(options.readPreference != null) {
            mongoCollection = mongoCollection.withReadPreference(ReadPreference.valueOf(options.readPreference.toString()))
        }
        List<Bson> newPipeline = preparePipeline(pipeline)
        AggregateObservable aggregateObservable = mongoCollection.aggregate(newPipeline)
        for(opt in options.keySet()) {
            if(aggregateObservable.respondsTo(opt)) {
                setOption((Object)aggregateObservable, opt, options)
            }
        }

        return (Observable<D>)aggregateObservable.toObservable()
    }

    @CompileDynamic
    private static void setOption(Object target, String opt, Map options) {
        target."$opt"(options.get(opt))
    }

    private List<Bson> preparePipeline(List pipeline) {
        List<Bson> newPipeline = new ArrayList<Bson>()
        if (multiTenancyMode == MultiTenancySettings.MultiTenancyMode.MULTI) {
            newPipeline.add(
                    Filters.eq(MappingUtils.getTargetKey(entity.tenantId), Tenants.currentId((Class<RxDatastoreClient>) datastoreClient.getClass()))
            )
        }
        for (o in pipeline) {
            if (o instanceof Bson) {
                newPipeline << (Bson)o
            } else if (o instanceof Map) {
                newPipeline << new Document((Map) o)
            }
        }
        newPipeline
    }
    protected <FT> FindObservable<FT> addMultiTenantFilterIfNecessary(FindObservable<FT> findIterable) {
        if (multiTenancyMode == MultiTenancySettings.MultiTenancyMode.MULTI) {
            return findIterable.filter(
                    Filters.eq(MappingUtils.getTargetKey(entity.tenantId), Tenants.currentId((Class<RxDatastoreClient>) datastoreClient.getClass()))
            )
        }
        return findIterable
    }

    @Override
    BsonDocument toBsonDocument(D instance) {
        return ((RxMongoEntity)instance).toBsonDocument()
    }
}
