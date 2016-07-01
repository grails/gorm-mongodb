package org.grails.datastore.rx.mongodb

import com.mongodb.rx.client.MongoClient
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.rx.bson.CodecsRxDatastoreClient
import org.grails.datastore.rx.internal.RxDatastoreClientImplementor

/**
 * Interface for datastore clients implemented for MongoDB
 *
 * @author Graeme Rocher
 * @since 6.0
 */
interface RxMongoDatastoreClientImplementor extends CodecsRxDatastoreClient<MongoClient>, RxDatastoreClientImplementor<MongoClient> {
    /**
     * Rebuilds the MongoDB index, useful if the database is dropped
     */
    void rebuildIndex()

    /**
     * Drops the configured database using a blocking operation
     */
    void dropDatabase()

    /**
     * Obtain a collection for the entity
     *
     * @param entity The entity
     * @param type The type
     * @return A {@link com.mongodb.rx.client.MongoCollection}
     */
    public <T1> com.mongodb.rx.client.MongoCollection<T1> getCollection(PersistentEntity entity, Class<T1> type)

    /**
     * Obtain the collection name of the given entity
     *
     * @param entity The entity
     * @return The collection name
     */
    public String getCollectionName(PersistentEntity entity)

    /**
     * Obtain the database name for the given entity
     *
     * @param entity The entity
     * @return The database name
     */
    public String getDatabaseName(PersistentEntity entity)

    /**
     * @return The native interface
     */
    MongoClient getNativeInterface()
}