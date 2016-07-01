package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import org.bson.types.ObjectId

/**
 * Created by graemerocher on 30/06/16.
 */
@Entity
class CompanyA implements RxMongoEntity<CompanyA>{
    ObjectId id
    String name

    static mapping = {
        connections "test1", "test2"
    }

}
