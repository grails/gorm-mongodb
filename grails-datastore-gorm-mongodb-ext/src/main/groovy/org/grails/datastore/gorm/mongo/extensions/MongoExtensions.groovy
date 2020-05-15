/*
 * Copyright 2015 original authors
 *
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
 */
package org.grails.datastore.gorm.mongo.extensions

import com.mongodb.BasicDBObject
import com.mongodb.CursorType
import com.mongodb.DBObject
import com.mongodb.MongoNamespace
import com.mongodb.ReadConcern
import com.mongodb.ReadPreference
import com.mongodb.WriteConcern
import com.mongodb.bulk.BulkWriteResult
import com.mongodb.client.*
import com.mongodb.client.model.*
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.InsertManyResult
import com.mongodb.client.result.InsertOneResult
import com.mongodb.client.result.UpdateResult
import com.mongodb.lang.Nullable
import groovy.transform.CompileStatic
import org.bson.Document
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.mapping.mongo.AbstractMongoSession
import org.grails.datastore.mapping.mongo.MongoConstants
import org.grails.datastore.mapping.mongo.engine.AbstractMongoObectEntityPersister
import org.grails.datastore.mapping.mongo.engine.MongoEntityPersister
import org.grails.datastore.mapping.mongo.query.MongoQuery

import java.util.concurrent.TimeUnit

import static java.util.Arrays.asList
import static java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * Extra methods for MongoDB API
 *
 * This extension makes it possible to use Groovy's map syntax instead of having to construct {@link org.bson.Document} instances
 *
 * @author Graeme Rocher
 * @since 4.0.5
 */
@CompileStatic
class MongoExtensions {


    static <T> T asType(Document document, Class<T> cls) {
        if(Document.isAssignableFrom(cls)) {
            return (T)document
        }
        else {
            def datastore = GormEnhancer.findDatastore(cls)
            AbstractMongoSession session = (AbstractMongoSession)datastore.currentSession
            if (session != null) {
                return session.decode(cls, document)
            }
            else if(cls.name == 'grails.converters.JSON') {
                return cls.newInstance( document )
            }
            else {
                throw new IllegalArgumentException("Cannot convert DBOject [$document] to writer type $cls. Type is not a persistent entity")
            }
        }
    }

    static <T> T asType(FindIterable iterable, Class<T> cls) {
        if(FindIterable.isAssignableFrom(cls)) {
            return (T)iterable
        }
        else {
            def datastore = GormEnhancer.findDatastore(cls)
            AbstractMongoSession session = (AbstractMongoSession)datastore.currentSession

            if (session != null) {
                return session.decode(cls, iterable)
            }
            else {
                throw new IllegalArgumentException("Cannot convert DBOject [$iterable] to writer type $cls. Type is not a persistent entity")
            }
        }
    }

    static <T> List<T> toList(FindIterable iterable, Class<T> cls) {
        def datastore = GormEnhancer.findDatastore(cls)
        AbstractMongoSession session = (AbstractMongoSession)datastore.currentSession

        MongoEntityPersister p = (MongoEntityPersister)session.getPersister(cls)
        if (p)
            return new MongoQuery.MongoResultList(((FindIterable<Document>)iterable).iterator(),0,p)
        else {
            throw new IllegalArgumentException("Cannot convert DBCursor [$iterable] to writer type $cls. Type is not a persistent entity")
        }
    }

    @CompileStatic
    static DBObject toDBObject(Document document) {
        def object = new BasicDBObject()
        for(key in document.keySet()) {
            def value = document.get(key)
            if(value instanceof Document) {
                value = toDBObject((Document)value)
            }
            else if(value instanceof Collection) {
                Collection col = (Collection)value
                Collection newCol = []
                for(i in col) {
                    if(i instanceof Document) {
                        newCol << toDBObject((Document)i)
                    }
                    else {
                        newCol << i
                    }
                }
                value = newCol
            }
            object.put(key, value)
        }
        return object
    }

    /**
     * Adds a method to return a collection using the dot syntax
     *
     * @param db The database object
     * @param name The collection name
     * @return A {@link MongoCollection}
     */
    static Object propertyMissing(MongoDatabase db, String name) {
        db.getCollection(name)
    }

    /**
     * Adds a method to return a collection using the dot syntax
     *
     * @param db The database object
     * @param name The collection name
     * @return A {@link MongoCollection}
     */
    static Object getAt(MongoDatabase db, String name) {
        db.getCollection(name)
    }

    private static Bson toBson(Map<String, Object> map) {
        if (map == null) {
            return null
        }
        (Bson) new Document(map)
    }


    /************** FindIterable Extensions *************/

    static FindIterable<Document> filter(FindIterable<Document> iterable, @Nullable Map<String, Object> filter) {
        iterable.filter(toBson(filter))
    }
    static FindIterable<Document> projection(FindIterable<Document> iterable, @Nullable Map<String, Object> projection) {
        iterable.projection(toBson(projection))
    }
    static FindIterable<Document> sort(FindIterable<Document> iterable, @Nullable Map<String, Object> sort) {
        iterable.sort(toBson(sort))
    }
    static FindIterable<Document> hint(FindIterable<Document> iterable, @Nullable Map<String, Object> hint) {
        iterable.hint(toBson(hint))
    }
    static FindIterable<Document> max(FindIterable<Document> iterable, @Nullable Map<String, Object> max) {
        iterable.max(toBson(max))
    }
    static FindIterable<Document> min(FindIterable<Document> iterable, @Nullable Map<String, Object> min) {
        iterable.min(toBson(min))
    }

    /************** DistinctIterable Extensions *************/

    static DistinctIterable<Document> filter(DistinctIterable<Document> iterable, Map<String, Object> filter) {
        iterable.filter(toBson(filter))
    }

    /************** MongoDatabase Extensions *************/

    static MongoIterable<String> getCollectionNames(MongoDatabase db) {
        db.listCollectionNames()
    }

    static MongoCollection<Document> createAndGetCollection(MongoDatabase db, final String collectionName, final Map<String, Object> options) {
        CreateCollectionOptions createCollectionOptions = MongoConstants.mapToObject(CreateCollectionOptions, options)
        db.createCollection(collectionName, createCollectionOptions)
        db.getCollection(collectionName)
    }

    /************** MongoCollection Extensions *************/

/*
    long countDocuments(Bson filter);

    long countDocuments(Bson filter, CountOptions options);

    long countDocuments(ClientSession clientSession, Bson filter);

    long countDocuments(ClientSession clientSession, Bson filter, CountOptions options);

    <TResult> DistinctIterable<TResult> distinct(String fieldName, Bson filter, Class<TResult> resultClass);

    <TResult> DistinctIterable<TResult> distinct(ClientSession clientSession, String fieldName, Bson filter, Class<TResult> resultClass);

    FindIterable<TDocument> find(Bson filter);

    <TResult> FindIterable<TResult> find(Bson filter, Class<TResult> resultClass);

    FindIterable<TDocument> find(ClientSession clientSession, Bson filter);

    <TResult> FindIterable<TResult> find(ClientSession clientSession, Bson filter, Class<TResult> resultClass);

    AggregateIterable<TDocument> aggregate(List<? extends Bson> pipeline);

    <TResult> AggregateIterable<TResult> aggregate(List<? extends Bson> pipeline, Class<TResult> resultClass);

    AggregateIterable<TDocument> aggregate(ClientSession clientSession, List<? extends Bson> pipeline);

    <TResult> AggregateIterable<TResult> aggregate(ClientSession clientSession, List<? extends Bson> pipeline, Class<TResult> resultClass);

    ChangeStreamIterable<TDocument> watch(List<? extends Bson> pipeline);

    <TResult> ChangeStreamIterable<TResult> watch(List<? extends Bson> pipeline, Class<TResult> resultClass);

    ChangeStreamIterable<TDocument> watch(ClientSession clientSession, List<? extends Bson> pipeline);

    <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, List<? extends Bson> pipeline, Class<TResult> resultClass);



    DeleteResult deleteOne(Bson filter, DeleteOptions options);

    DeleteResult deleteOne(ClientSession clientSession, Bson filter);


    DeleteResult deleteOne(ClientSession clientSession, Bson filter, DeleteOptions options);

    DeleteResult deleteMany(Bson filter);

    DeleteResult deleteMany(Bson filter, DeleteOptions options);

    DeleteResult deleteMany(ClientSession clientSession, Bson filter);

    DeleteResult deleteMany(ClientSession clientSession, Bson filter, DeleteOptions options);

    UpdateResult updateOne(Bson filter, Bson update);

    UpdateResult updateOne(Bson filter, Bson update, UpdateOptions updateOptions);

    UpdateResult updateOne(ClientSession clientSession, Bson filter, Bson update);

    UpdateResult updateOne(ClientSession clientSession, Bson filter, Bson update, UpdateOptions updateOptions);

    UpdateResult updateOne(Bson filter, List<? extends Bson> update);

    UpdateResult updateOne(Bson filter, List<? extends Bson> update, UpdateOptions updateOptions);

    UpdateResult updateOne(ClientSession clientSession, Bson filter, List<? extends Bson> update);

    UpdateResult updateOne(ClientSession clientSession, Bson filter, List<? extends Bson> update, UpdateOptions updateOptions);

    UpdateResult updateMany(Bson filter, Bson update);

    UpdateResult updateMany(Bson filter, Bson update, UpdateOptions updateOptions);

    UpdateResult updateMany(ClientSession clientSession, Bson filter, Bson update);

    UpdateResult updateMany(ClientSession clientSession, Bson filter, Bson update, UpdateOptions updateOptions);

    UpdateResult updateMany(Bson filter, List<? extends Bson> update);

    UpdateResult updateMany(Bson filter, List<? extends Bson> update, UpdateOptions updateOptions);

    UpdateResult updateMany(ClientSession clientSession, Bson filter, List<? extends Bson> update);

    UpdateResult updateMany(ClientSession clientSession, Bson filter, List<? extends Bson> update, UpdateOptions updateOptions);

    String createIndex(Bson keys);

    String createIndex(Bson keys, IndexOptions indexOptions);

    String createIndex(ClientSession clientSession, Bson keys);

    String createIndex(ClientSession clientSession, Bson keys, IndexOptions indexOptions);

    void dropIndex(Bson keys);

    void dropIndex(Bson keys, DropIndexOptions dropIndexOptions);

    void dropIndex(ClientSession clientSession, Bson keys);

    void dropIndex(ClientSession clientSession, Bson keys, DropIndexOptions dropIndexOptions);



    static Document findOne(MongoCollection<Document> collection, final Document query) {
        collection
                .find((Bson) query)
                .limit(1)
                .first()
    }

    static Document findOne(MongoCollection<Document> collection, final Map query) {
        findOne(collection, new Document(query))
    }

    static String getName(MongoCollection<Document> collection) {
        collection.namespace.collectionName
    }

    static Document findOne(MongoCollection<Document> collection, final Map<String, Object> query) {
        findOne(collection, new Document(query))
    }

    static Document findOne(MongoCollection<Document> collection, ObjectId id) {
        Document query = new Document(AbstractMongoObectEntityPersister.MONGO_ID_FIELD, id)
        findOne(collection, query)
    }

    static Document findOne(MongoCollection<Document> collection, CharSequence id) {
        Document query = new Document(AbstractMongoObectEntityPersister.MONGO_ID_FIELD, id)
        collection.findOne(query)
    }

    static <T> T findOne(MongoCollection<Document> collection, Document query, Class<T> type) {
        collection
                .find((Bson) query, type)
                .limit(1)
                .first()
    }

    static <T> T findOne(MongoCollection<Document> collection, Serializable id, Class<T> type) {
        Document query = new Document(AbstractMongoObectEntityPersister.MONGO_ID_FIELD, id)
        findOne(collection, query, type)
    }


    static Document findOne(MongoCollection<Document> collection, final Map query, final Map projection) {
        collection
                .find(new BasicDBObject(query) )
                .projection(new BasicDBObject(projection))
                .limit(1)
                .first()
    }


    static Document findOne(MongoCollection<Document> collection, final Map<String,Object> query, final Map projection) {
        collection
                .find((Bson) new Document(query) )
                .projection((Bson) new Document(projection) )
                .limit(1)
                .first()
    }


    static Document findOne(MongoCollection<Document> collection, final Map query, final Map projection, final Map sort) {
        collection
                .find(new BasicDBObject(query))
                .projection(new BasicDBObject(projection) )
                .sort(new BasicDBObject(sort) )
                .limit(1)
                .first()
    }


    static Document findOne(MongoCollection<Document> collection, final Map query, final Map projection, final Map sort) {
        collection
                .find((Bson) new Document(query) )
                .projection((Bson) new Document(projection) )
                .sort((Bson) new Document(sort) )
                .limit(1)
                .first()
    }

    static Document findOne(MongoCollection<Document> collection) {
        collection
                .find()
                .first()
    }

    static Document findOne(MongoCollection<Document> collection, final Map query, final Map projection, final ReadPreference readPreference) {
        collection
            .withReadPreference(readPreference)
            .find(new BasicDBObject(query))
            .projection(new BasicDBObject(projection))
            .limit(1)
            .first()
    }


    static Document findOne(MongoCollection<Document> collection, final Map query, final Map projection, final ReadPreference readPreference) {
        collection
                .withReadPreference(readPreference)
                .find( (Bson)new Document(query) )
                .projection((Bson) new Document(projection) )
                .limit(1)
                .first()
    }


    static Document findOne(MongoCollection<Document> collection, final Map query, final Map projection, final Map sort,
                            final ReadPreference readPreference) {
        collection
                .withReadPreference(readPreference)
                .find(new BasicDBObject(query))
                .projection(new BasicDBObject(projection))
                .sort(new BasicDBObject(sort))
                .limit(1)
                .first()
    }

    static Document findOne(MongoCollection<Document> collection, final Map query, final Map projection, final Map sort,
                            final ReadPreference readPreference) {
        collection
                .withReadPreference(readPreference)
                .find( (Bson)new Document(query) )
                .projection( new Document(projection) )
                .sort( new Document(sort) )
                .limit(1)
                .first()
    }


    static DBCursor find(DBCollection collection, final Map query) {
        collection.find((Document)new BasicDBObject(query))
    }

    static FindIterable<Document> find(MongoCollection<Document> collection, final Map<String, Object> query) {
        collection.find((Bson)new Document(query))
    }

    static <T>  FindIterable<T> find(MongoCollection<T> collection, final Map<String, Object> query, Class<T> type) {
        collection.find((Bson)new Document(query), type)
    }


    static DBCursor find(DBCollection collection, final Map<String, Object> query, final Map<String, Object> projection) {
        ollection.find((Document)new BasicDBObject(query), (Document)new BasicDBObject(projection))
    }

    static FindIterable<Document> find(MongoCollection<Document> collection, final Map<String, Object> query, final Map<String, Object> projection) {
        collection.find((Bson) new Document(query))
                  .projection((Bson) new Document(projection) )
    }


    static long count(final MongoCollection<Document> collection, final Map query) {
        countDocuments(collection, query, null)
    }

    static long count(MongoCollection<Document> collection, final Map<String, Object> query) {
        countDocuments(collection, query)
    }

    static long count(MongoCollection<Document> collection, final Map<String, Object> query, final ReadPreference readPreference) {
        countDocuments(collection, query, readPreference);
    }

    static long count(MongoCollection<Document> collection, final Map query, final Map<String, Object> options) {
        countDocuments(collection, query, options)
    }

    static long count(final MongoCollection<Document> collection, final Map query, final ReadPreference readPreference) {
        countDocuments(collection, query, readPreference)
    }

    static long getCount(final MongoCollection<Document> collection, final Map query) {
        countDocuments(collection, query, null)
    }

    static long countDocuments(final MongoCollection<Document> collection,
                               final Map<String, Object> query) {
        collection.countDocuments((Bson) new Document(query))
    }

    static long countDocuments(final MongoCollection<Document> collection,
                               final Map<String, Object> query,
                               final ReadPreference readPreference) {
        collection
                .withReadPreference(readPreference)
                .countDocuments((Bson) new Document(query))
    }

    static long countDocuments(final MongoCollection<Document> collection,
                               final Map<String, Object> query,
                               final  Map<String, Object> options) {
        collection.countDocuments((Bson) new Document(query), MongoConstants.mapToObject(CountOptions, options))
    }

    static long countDocuments(final MongoCollection<Document> collection, final Map query) {
        collection.countDocuments(new BasicDBObject(query));
    }

    static long countDocuments(final MongoCollection<Document> collection, final Map query, final ReadPreference readPreference) {
        CountOptions options = new CountOptions()
        options.limit(0)
        options.skip(0)

        collection
                .withReadPreference(readPreference)
                .countDocuments(new BasicDBObject(query), options)
    }


    static long countDocuments(final MongoCollection<Document> collection, final Map query, final int limit, final int skip) {
        CountOptions options = new CountOptions()
        options.limit(limit)
        options.skip(skip)
        collection.countDocuments(new BasicDBObject(query), options)
    }


    static long countDocuments(final MongoCollection<Document> collection,
                               final Map query,
                               final Map projection, final long limit, final long skip,
                         final ReadPreference readPreference) {
        collection.getCount((Document)new BasicDBObject(query), (Document)new BasicDBObject(projection), limit, skip, readPreference)
    }

    static void createIndex(final MongoCollection<Document> collection, final Map keys, final String name) {
        createIndex(collection, keys, name, false)
    }

    static void createIndex(final MongoCollection<Document> collection, final Map keys, final String name, final boolean unique) {
        IndexOptions options = new IndexOptions()
        options.name(name)
        options.unique(unique)
        collection.createIndex(new BasicDBObject(keys), options)
    }

    static void createIndex(final MongoCollection<Document> collection, final Map keys) {
        collection.createIndex(new BasicDBObject(keys))
    }

    static void createIndex(final MongoCollection<Document> collection, final Map keys, final Map options) {
        IndexOptions indexOptions = MongoConstants.mapToObject(IndexOptions, options)
        collection.createIndex(new BasicDBObject(keys), indexOptions)
    }

    static void createIndex(MongoCollection<Document> collection, final Map keys, final String name, final boolean unique) {
        collection.createIndex((Bson) new Document(keys), new IndexOptions().name(name).unique(unique))
    }

    static void createIndex(final MongoCollection<Document> collection, final Map<String, Object> keys) {
        collection.createIndex((Bson)new Document(keys))
    }

    static void createIndex(final MongoCollection<Document> collection, final Map<String, Object> keys, final IndexOptions options) {
        collection.createIndex((Bson)new Document(keys), options)
    }

    static void createIndex(final MongoCollection<Document> collection, final Map<String, Object> keys, final Map<String, Object> options) {
        collection.createIndex((Bson)new Document(keys), MongoConstants.mapToObject(IndexOptions, options))
    }
    static void dropIndex(final MongoCollection<Document> collection, final Map index) {
        collection.dropIndex(new BasicDBObject(index))
    }

    static void dropIndex(final MongoCollection<Document> collection, final Map<String, Object> index) {
        collection.dropIndex((Bson) new Document(index))
    }

    static InsertOneResult insert(final MongoCollection<BasicDBObject> collection, final Map document) {
        collection.insertOne(new BasicDBObject(document))
    }

    static void insertOne(final MongoCollection<Document> collection, final Map<String, Object> document) {
        collection.insertOne(new Document(document))
    }

    static InsertOneResult leftShift(final MongoCollection<Document> collection, final Map document) {
        insertOne(collection, document)
    }

    static InsertOneResult insertOne(final MongoCollection<Document> collection, final Map document, final WriteConcern writeConcern) {
        collection
                .withWriteConcern(writeConcern)
                .insertOne(new Document(document))
    }

    static InsertOneResult insertOne(final MongoCollection<Document> collection,
                                     final Map<String, Object> document,
                                     final WriteConcern writeConcern) {
        collection
                .withWriteConcern(writeConcern)
                .insertOne(new Document(document))
    }

    static InsertManyResult insertMany(final MongoCollection<Document> collection, final Map... documents) {
        collection.insertMany(documents.collect() { Map m -> new BasicDBObject(m) } as List<Document>)
    }

    static void insertMany(final MongoCollection<Document> collection, final Map<String, Object>... documents) {
        collection.insertMany documents.collect() { Map m -> new Document(m) } as List<Document>
    }

    static InsertManyResult leftShift(final MongoCollection<Document> collection, final Map... documents) {
        insertMany(collection, documents)
    }

    static MongoCollection<Document> leftShift(final MongoCollection<Document> collection, final Map<String, Object>... documents) {
        insert(collection, documents)
        return collection
    }

    static InsertManyResult insertMany(final MongoCollection<Document> collection, final WriteConcern writeConcern, final Map... documents) {
        insertMany(collection, documents, writeConcern);
    }

    static MongoCollection<Document> insert(final MongoCollection<Document> collection, final WriteConcern writeConcern, final Map<String, Object>... documents) {
        insert(collection, documents, writeConcern);
    }

    static InsertManyResult insertMany(final MongoCollection<Document> collection, final Map[] documents, final WriteConcern writeConcern) {
        insertMany(collection, asList(documents), writeConcern);
    }

    static InsertManyResult insertMany(final MongoCollection<Document> collection, final Map<String,Object>[] documents, final WriteConcern writeConcern) {
        insertMany(collection, asList(documents), writeConcern)
    }

    static InsertManyResult insert(final MongoCollection<Document> collection, final List<? extends Map> documents) {
        collection.insertMany(documents.collect() { Map m -> new BasicDBObject(m) })
    }

    static InsertManyResult insertMany(final MongoCollection<Document> collection, final List<? extends Map<String, Object>> documents) {
        collection.insertMany documents.collect() { Map m -> new BasicDBObject(m) }
    }

    static InsertManyResult insertMany(final MongoCollection<Document> collection, final List<? extends Map<String, Object>> documents) {
        collection.insertMany documents.collect() { Map m -> new Document(m) } as List<Document>
    }

    static InsertManyResult leftShift(final MongoCollection<Document> collection, final List<? extends Map> documents) {
        insertMany(collection, documents)
    }

    static InsertManyResult insertMany(final MongoCollection<Document> collection,
                                   final Map[] documents,
                                   final WriteConcern aWriteConcern) {
        return insertMany(collection, asList(documents), aWriteConcern);
    }

    static InsertManyResult insertMany(final MongoCollection<Document> collection,
                                       final List<? extends Map> documents,
                                       final WriteConcern aWriteConcern) {
        collection
                .withWriteConcern(aWriteConcern)
                .insertMany( documents.collect() { Map m -> new BasicDBObject(m) })
    }

    static InsertManyResult insertMany(final MongoCollection<Document> collection,
                                       final List<? extends Map> documents,
                                       final WriteConcern aWriteConcern) {
        collection
                .withWriteConcern(aWriteConcern)
                .insertMany( documents.collect() { Map m -> new Document(m) })
    }

    static InsertManyResult insertMany(final MongoCollection<Document> collection,
                                       final List<? extends Map> documents,
                                       final InsertManyOptions insertManyOptions) {
        collection.insertMany(documents.collect() { Map m -> new BasicDBObject(m) }, insertManyOptions)
    }

    static InsertManyResult insertMany(final MongoCollection<Document> collection,
                                                final List<? extends Map<String, Object>> documents,
                                                final WriteConcern writeConcern,
                                                final InsertManyOptions insertManyOptions) {
        collection
                .withWriteConcern(writeConcern)
                .insertMany(documents.collect() { Map m -> new BasicDBObject(m) }, insertManyOptions)
    }

    static  WriteResult save(final DBCollection collection, final Map document) {
        collection.save( (Document)new BasicDBObject(document) )
    }

    static WriteResult save(final DBCollection collection, final Map document, final WriteConcern writeConcern) {
        collection.save( (Document)new BasicDBObject(document), writeConcern )
    }

    static MongoCollection save(final MongoCollection<Document> collection, final Map<String, Object> document) {
        updateMany(collection, document)
    }

    static  MongoCollection save(final MongoCollection<Document> collection, final Map<String, Object> document, final WriteConcern writeConcern) {
        insert collection, document, writeConcern
    }

    static UpdateResult updateOne(final MongoCollection<Document> collection, Map<String,Object> filter, Map<String, Object> update) {
        collection.updateOne((Bson)new Document(filter), new Document(update))
    }

    static UpdateResult update(final MongoCollection<Document> collection, Map<String,Object> filter, Map<String, Object> update) {
        collection.updateOne((Bson)new Document(filter), new Document(update))
    }

    static UpdateResult updateOne(final MongoCollection<Document> collection, Map<String,Object> filter, Map<String, Object> update, Map<String, Object> options) {
        collection.updateOne((Bson)new Document(filter), new Document(update), MongoConstants.mapToObject(UpdateOptions, options))
    }
    
    static UpdateResult updateOne(final MongoCollection<Document> collection,
                                  final Map query,
                                  final Map update,
                                  final boolean upsert) {
        UpdateOptions options = new UpdateOptions()
        options.upsert(upsert)
        collection.updateOne(new Document(query), new Document(update), options)
    }

    static UpdateResult updateMulti(final MongoCollection<Document> collection,
                                    final Map query,
                                    final Map update,
                                    final boolean upsert,
                                    final WriteConcern aWriteConcern) {
        UpdateOptions options = new UpdateOptions()
        options.upsert(upsert)

        collection
                .writeConcern(aWriteConcern)
                .update(new BasicDBObject(query),new BasicDBObject(update), options)
    }

    static UpdateResult updateMulti(final MongoCollection<Document> collection,
                               final Map query,
                               final Map update,
                               final boolean upsert) {
        collection.update((Document)new Document(query), (Document)new Document(update), upsert, multi)
    }

    static WriteResult update(final DBCollection collection, final Map query, final Map update) {
        collection.update((Document)new Document(query), (Document)new Document(update))
    }

    static UpdateResult updateMany(final MongoCollection<Document> collection, Map<String,Object> filter, Map<String, Object> update) {
        collection.updateMany((Bson)new Document(filter), new Document(update))
    }

    static UpdateResult updateMany(final MongoCollection<Document> collection, Map<String,Object> filter, Map<String, Object> update, Map<String, Object> options) {
        collection.updateMany((Bson)new Document(filter), new Document(update), MongoConstants.mapToObject(UpdateOptions, options))
    }

    static UpdateResult updateMulti(final MongoCollection<Document> collection, final Map query, final Map update) {
        collection.updateMany(new Document(query), new Document(update))
    }

    static DeleteResult delete(final MongoCollection<Document> collection, final Map query) {
        collection.delete(new Document(query))
    }

    static DeleteResult rightShift(final MongoCollection<Document> collection, final Map query) {
        delete collection, query
    }

    static DeleteResult delete(final MongoCollection<Document> collection, final Map query, final WriteConcern writeConcern) {
        collection.withWriteConcern(writeConcern)
                .delete(new Document(query))
    }

    static DeleteResult deleteMany(final MongoCollection<Document> collection, final Map<String,Object> query) {
        collection.deleteMany( (Bson) new Document(query) )
    }

    static DeleteResult remove(final MongoCollection<Document> collection, final Map<String,Object> query) {
        deleteMany collection, query
    }

    static MongoCollection<Document> rightShift(final MongoCollection<Document> collection, final Map<String, Object> query) {
        deleteMany collection, query
        return collection
    }

    static DeleteResult deleteMany(final MongoCollection<Document> collection, final Map<String,Object> query, final WriteConcern writeConcern) {
        collection
                .withWriteConcern(writeConcern)
                .deleteMany( (Bson) new Document(query) )
    }

    static DeleteResult deleteOne(final MongoCollection<Document> collection, final Map<String,Object> query) {
        collection.deleteOne( (Bson)new Document(query) )
    }

    static DeleteResult deleteOne(final MongoCollection<Document> collection, final Map<String,Object> query, final WriteConcern writeConcern) {
        collection
                .withWriteConcern(writeConcern)
                .deleteOne( (Bson) new Document(query) )
    }

    static void setHintFields(final DBCollection collection, final List<? extends Map> indexes) {
//        collection.hintFields = indexes.collect() {  Map m -> new Document(m) } as List<Document>
    }

    static Document findOneAndUpdate(final MongoCollection<Document> collection,
                                     final Map query,
                                     final Map sort,
                                     final Map update) {
        findOneAndUpdate(collection, query, null, sort, update, false, false);
    }

    static Document findOneAndUpdate(final MongoCollection<Document> collection,
                                  final Map query,
                                  final Map update) {
        findOneAndUpdate(collection, query, null, null, update, false, false)
    }

    static Document findOneAndDelete(final MongoCollection<Document> collection, final Map query) {
        FindOneAndDeleteOptions options = new FindOneAndDeleteOptions()
        collection.findOneAndDelete(new Document(query), options)
    }

    static Document findOneAndUpdate(final MongoCollection<Document> collection,
                                     final Map query,
                                     final Map fields,
                                     final Map sort,
                                     final Map update,
                                     final boolean returnNew,
                                     final boolean upsert) {
        findOneAndUpdate(collection, query, fields, sort, update, returnNew, upsert, 0L, MILLISECONDS)
    }

    static Document findOneAndUpdate(final MongoCollection<Document> collection,
                                     final Map query,
                                     final Map fields,
                                     final Map sort,
                                     final Map update,
                                     final boolean returnNew,
                                     final boolean upsert,
                                     final long maxTime,
                                     final TimeUnit maxTimeUnit) {

        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
        if (upsert) {
            options.upsert(upsert)
        }
        if (fields) {
            options.projection(new Document(fields))
        }
        if (sort) {
            options.sort(new Document(sort))
        }
        if (returnNew) {
            options.returnDocument(ReturnDocument.AFTER)
        }
        options.maxTime(maxTime, maxTimeUnit)
        collection.findOneAndUpdate(new Document(query), new Document(update))
    }

    static Document findOneAndReplace(final MongoCollection<Document> collection,
                                      final Map query,
                                      final Map fields,
                                      final Map sort,
                                      final boolean remove,
                                      final Map update,
                                      final boolean returnNew,
                                      final boolean upsert,
                                      final long maxTime,
                                      final TimeUnit maxTimeUnit) {
        FindOneAndReplaceOptions options = new FindOneAndReplaceOptions()
        options.upsert(upsert)
        options.maxTime(maxTime, maxTimeUnit)
        if (sort) {
            options.sort(new Document(sort))
        }
        if (fields) {
            options.projection(new Document(fields))
        }
        if (returnNew) {
            options.returnDocument(ReturnDocument.AFTER)
        }
        collection.findOneAndReplace(new Document(query), new Document(update))

    }

    static List distinct(final  MongoCollection<Document> collection, final String fieldName, final Map query) {
        collection.distinct(fieldName, new Document(query))
    }

    static List distinct(final  MongoCollection<Document> collection, final String fieldName, final Map query, final ReadPreference readPreference) {
        collection.distinct(fieldName, new Document(query), readPreference)
    }

    static DistinctIterable<Document> distinct(final MongoCollection<Document> collection, final String fieldName) {
        collection.distinct(fieldName, Document)
    }

    static DistinctIterable<Document> distinct(final MongoCollection<Document> collection, final String fieldName, final ReadPreference readPreference) {
        collection
                .withReadPreference(readPreference)
                .distinct(fieldName, Document)
    }

    static DistinctIterable<Document> distinct(final MongoCollection<Document> collection, final String fieldName, Map<String, Object> query) {
        collection.distinct(fieldName, Document)
                  .filter((Bson) new Document(query) )
    }

    static DistinctIterable<Document> distinct(final MongoCollection<Document> collection, final String fieldName, Map<String, Object> query, final ReadPreference readPreference) {
        collection
                .withReadPreference(readPreference)
                .distinct(fieldName, Document)
                .filter((Bson) new Document(query) )
    }

    static AggregateIterable<Document> aggregate(final MongoCollection<Document> collection, final List<? extends Map> pipeline) {
        collection.aggregate(pipeline.collect {  Map m -> new Document(m) })
    }

    static AggregateIterable<Document> aggregate(final  MongoCollection<Document> collection, final List<? extends Map> pipeline, final ReadPreference readPreference) {
        collection.withReadPreference(readPreference).aggregate(pipeline.collect() {  Map m -> new Document(m) })
    }

    static MapReduceIterable<Document> mapReduce(final MongoCollection<Document> collection,
                                                 final String mapFunction,
                                                 final String reduceFunction) {
        collection.mapReduce(mapFunction, reduceFunction)
    }

    static MapReduceIterable<Document> mapReduce(final  MongoCollection<Document> collection,
                                     final String mapFunction,
                                     final String reduceFunction,
                                     final ReadPreference readPreference) {
        collection
                .withReadPreference(readPreference)
                .mapReduce(mapFunction, reduceFunction)
    }


    static UpdateResult replaceOne(MongoCollection<Document> collection, Map<String, Object> filter, Document replacement) {
        collection.replaceOne((Bson) new Document(filter), replacement)
    }

    static UpdateResult replaceOne(MongoCollection<Document> collection, Map<String, Object> filter, Document replacement, Map<String,Object> options) {
        collection.replaceOne((Bson) new Document(filter),
                replacement,
                MongoConstants.mapToObject(ReplaceOptions, options))
    }

    static Document findOneAndDelete(MongoCollection<Document> collection, Map<String, Object> filter) {
        collection.findOneAndDelete((Bson) new Document(filter) )
    }

    static Document findOneAndDelete(MongoCollection<Document> collection, Map<String, Object> filter, Map<String, Object> options) {
        collection.findOneAndDelete((Bson) new Document(filter), MongoConstants.mapToObject(FindOneAndDeleteOptions, options) )
    }

    static Document findOneAndReplace(MongoCollection<Document> collection, Map<String, Object> filter, Map<String, Object> replacement) {
        collection.findOneAndReplace( (Bson)new Document(filter), new Document(replacement) )
    }

    static Document findOneAndReplace(MongoCollection<Document> collection, Map<String, Object> filter, Map<String, Object> replacement, Map<String, Object> options) {
        collection.findOneAndReplace( (Bson)new Document(filter), new Document(replacement), MongoConstants.mapToObject(FindOneAndReplaceOptions, options) )
    }

    static Document findOneAndUpdate(MongoCollection<Document> collection, Map<String, Object> filter, Map<String, Object> update) {
        collection.findOneAndUpdate( (Bson)new Document(filter), new Document(update) )
    }

    static Document findOneAndUpdate(MongoCollection<Document> collection, Map<String, Object> filter, Map<String, Object> update, Map<String, Object> options) {
        collection.findOneAndUpdate( (Bson)new Document(filter), new Document(update), MongoConstants.mapToObject(FindOneAndUpdateOptions, options) )
    }*/

}

