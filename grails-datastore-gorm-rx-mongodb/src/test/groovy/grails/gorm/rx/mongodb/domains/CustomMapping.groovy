package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import grails.mongodb.geo.Point
import org.bson.types.ObjectId

import static grails.mongodb.mapping.MappingBuilder.document

@Entity
class CustomMapping implements RxMongoEntity<Country> {
    ObjectId id
    String name
    Point loc

    static mapping = document {
        collection "mycoll"
        database "mydb"
        name property {
            reference false
            attr "my_name"
        }
        loc property {
            geoIndex "2dsphere"
        }
    }
}