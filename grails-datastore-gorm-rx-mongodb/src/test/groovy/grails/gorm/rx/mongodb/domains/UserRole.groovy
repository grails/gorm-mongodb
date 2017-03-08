package grails.gorm.rx.mongodb.domains

import grails.gorm.annotation.Entity
import grails.gorm.rx.mongodb.RxMongoEntity
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.bson.types.ObjectId

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
        def builder = new HashCodeBuilder()
        if (user) builder.append(user.id)
        if (role) builder.append(role.id)
        builder.toHashCode()
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