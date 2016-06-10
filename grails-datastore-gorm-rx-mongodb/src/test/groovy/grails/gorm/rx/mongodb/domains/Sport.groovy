package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import org.bson.types.ObjectId

/**
 * Created by graemerocher on 09/05/16.
 */
@Entity
class Sport implements RxMongoEntity<Sport> {
    ObjectId id

    String name
    Set<Club> clubs
    static hasMany = [clubs: Club]
}
