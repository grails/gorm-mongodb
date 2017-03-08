package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.Role
import grails.gorm.rx.mongodb.domains.User
import grails.gorm.rx.mongodb.domains.UserRole
import rx.Observable

/**
 * Created by graemerocher on 08/03/2017.
 */
class AssociationQuerySpec extends RxGormSpec {

    void 'test query association'() {
        given:
        Role.saveAll(
                new Role(authority: "ROLE_ADMIN"),
                new Role(authority: "ROLE_MANAGER")
        ).toBlocking().first()

        User user = new User(username: 'fred', password: 'password', enabled: true).save().toBlocking().first()

        when:
        Role adminRole = Role.findByAuthority("ROLE_ADMIN").toBlocking().first()

        then:
        adminRole != null
        user != null

        when:
        UserRole userRole = new UserRole(user:user, role: adminRole).save().toBlocking().first()

        then:
        userRole != null

    }

    @Override
    List<Class> getDomainClasses() {
        [Role, User, UserRole]
    }
}
