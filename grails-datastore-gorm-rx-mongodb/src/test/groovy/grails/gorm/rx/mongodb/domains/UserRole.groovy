package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity

import org.bson.types.ObjectId
import org.codehaus.groovy.util.HashCodeHelper

@Entity
class UserRole implements RxMongoEntity<UserRole> {
    ObjectId id

    Date        dateCreated
    Date        lastUpdated

    User user
    Role role

    boolean equals(other) {
        if (!(other instanceof UserRole)) {
            return false
        }

        other.user?.id == user?.id &&
                other.role?.id == role?.id
    }




    int hashCode() {
        int hash = HashCodeHelper.initHash()
        if (user) hash = HashCodeHelper.updateHash(hash, user.id)
        if (role) hash = HashCodeHelper.updateHash(hash, role.id)
        hash
    }

    static constraints = {
        lastUpdated nullable: true
        dateCreated nullable: true
    }

    @Override
    public String toString() {
        return "UserRole{" +
                "id=" + id +
                ", dateCreated=" + dateCreated +
                ", lastUpdated=" + lastUpdated +
                ", user=" + user +
                ", role=" + role +
                '}';
    }
}