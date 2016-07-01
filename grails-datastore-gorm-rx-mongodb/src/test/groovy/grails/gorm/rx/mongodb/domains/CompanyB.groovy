package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import org.bson.types.ObjectId
import org.grails.datastore.mapping.core.connections.ConnectionSource

/**
 * Created by graemerocher on 30/06/16.
 */
@Entity
class CompanyB implements RxMongoEntity<CompanyB> {
    ObjectId id
    String name


    static mapping = {
        connections "test2", ConnectionSource.DEFAULT
    }
}
