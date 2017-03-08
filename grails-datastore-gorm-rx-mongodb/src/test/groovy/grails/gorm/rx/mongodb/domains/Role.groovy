package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import org.bson.types.ObjectId

@Entity
class Role implements RxMongoEntity<Role> {
    ObjectId id

    Date dateCreated
    Date lastUpdated

    String authority

    static constraints = {
        authority blank: false, unique: true
        lastUpdated nullable: true
        dateCreated nullable: true
    }

    String toString() {
        authority
    }
}