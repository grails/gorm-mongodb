package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import org.bson.types.ObjectId

@Entity
class User implements RxMongoEntity<User> {
    ObjectId id

    Date        dateCreated
    Date        lastUpdated

    transient springSecurityService

    String username
    String password
    boolean enabled
    boolean accountExpired
    boolean accountLocked
    boolean passwordExpired

    static constraints = {
        username blank: false, unique: true
        password blank: false
        lastUpdated nullable: true
        dateCreated nullable: true
    }

    @Override
    String toString() {
        username
    }
}