package org.grails.datastore.rx.mongodb.client

import com.mongodb.rx.client.MongoClient
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.core.connections.ConnectionSources
import org.grails.datastore.mapping.core.connections.InMemoryConnectionSources
import org.grails.datastore.mapping.core.connections.SingletonConnectionSources
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.mongo.config.MongoMappingContext
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.rx.internal.RxDatastoreClientImplementor
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClientImplementor
import org.grails.datastore.rx.mongodb.connections.MongoConnectionSourceSettings
import org.grails.datastore.rx.mongodb.query.RxMongoQuery
import org.grails.datastore.rx.query.QueryState

/**
 * overrides the default client and provides the ability to customize the writer database name, collection name or client connection
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class DelegatingRxMongoDatastoreClient extends RxMongoDatastoreClient {

    final @Delegate RxMongoDatastoreClientImplementor datastoreClient

    String targetCollectionName
    String targetDatabaseName
    MongoClient targetMongoClient

    @CompileDynamic
    DelegatingRxMongoDatastoreClient(ConnectionSource<MongoClient, MongoConnectionSourceSettings> connectionSource, RxMongoDatastoreClientImplementor datastoreClient) {
        super(new SingletonConnectionSources<MongoClient, MongoConnectionSourceSettings>(connectionSource, DatastoreUtils.createPropertyResolver([:])), (MongoMappingContext)datastoreClient.mappingContext)
        this.datastoreClient = datastoreClient
    }


    DelegatingRxMongoDatastoreClient(RxMongoDatastoreClientImplementor datastoreClient) {
        super(new SingletonConnectionSources<MongoClient, MongoConnectionSourceSettings>((ConnectionSource<MongoClient, MongoConnectionSourceSettings>)datastoreClient.connectionSources.defaultConnectionSource, datastoreClient.connectionSources.baseConfiguration), (MongoMappingContext)datastoreClient.mappingContext)
        this.datastoreClient = datastoreClient
    }

    @Override
    protected void initialize(MongoMappingContext mappingContext) {
        // no-op
    }

    @Override
    CodecRegistry getCodecRegistry() {
        return datastoreClient.getCodecRegistry()
    }

    @Override
    MongoClient getNativeInterface() {
        if(targetMongoClient != null) {
            return targetMongoClient
        }
        else {
            return datastoreClient.getNativeInterface()
        }
    }

    String getCollectionName(PersistentEntity entity) {
        if(targetCollectionName != null) {
            return targetCollectionName
        }
        else {
            return datastoreClient.getCollectionName(entity)
        }
    }

    String getDatabaseName(PersistentEntity entity) {
        if(targetDatabaseName != null) {
            return targetDatabaseName
        }
        else {
            return datastoreClient.getDatabaseName(entity)
        }
    }

    Query createEntityQuery(PersistentEntity entity, QueryState queryState) {
        return new RxMongoQuery(this, entity, queryState)
    }
}
