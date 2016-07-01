package grails.gorm.rx.mongodb.api

import grails.gorm.rx.api.RxGormInstanceOperations
import org.bson.BsonDocument

/**
 * Methods on instances for RxGORM for MongoDB
 *
 * @author Graeme Rocher
 * @since 6.0
 */
interface RxMongoInstanceOperations<D> extends RxGormInstanceOperations<D> {
    /**
     * Converts this entity into a {@link org.bson.BsonDocument}
     *
     * @return The {@link org.bson.BsonDocument} instance
     */
    BsonDocument toBsonDocument(D instance)
}