package grails.gorm.rx.mongodb.multitenancy

import grails.gorm.annotation.Entity
import grails.gorm.rx.MultiTenant
import grails.gorm.rx.mongodb.RxMongoEntity
import org.bson.types.ObjectId
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.mongo.config.MongoSettings
import org.grails.datastore.mapping.multitenancy.exceptions.TenantNotFoundException
import org.grails.datastore.mapping.multitenancy.resolvers.SystemPropertyTenantResolver
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
/**
 * Created by graemerocher on 08/07/2016.
 */
class SingleTenancySpec extends Specification {
    @Shared @AutoCleanup RxMongoDatastoreClient datastoreClient

    void setupSpec() {
        Map config = [
                "grails.gorm.multiTenancy.mode"               :"SINGLE",
                "grails.gorm.multiTenancy.tenantResolverClass":SystemPropertyTenantResolver,
                (MongoSettings.SETTING_URL)                   : "mongodb://localhost/defaultDb",
                (MongoSettings.SETTING_CONNECTIONS)           : [
                        test1: [
                                url: "mongodb://localhost/test1Db"
                        ],
                        test2: [
                                url: "mongodb://localhost/test2Db"
                        ]
                ]
        ]
        this.datastoreClient = new RxMongoDatastoreClient(DatastoreUtils.createPropertyResolver(config), getDomainClasses() as Class[])
    }

    void setup() {
        System.setProperty(SystemPropertyTenantResolver.PROPERTY_NAME, "")
    }

    void "Test no tenant id"() {
        when:
        CompanyB.DB

        then:
        thrown(TenantNotFoundException)
    }

    void "Test multi tenancy state"() {
        given:
        System.setProperty(SystemPropertyTenantResolver.PROPERTY_NAME, "test1")
        expect:
        CompanyB.DB.name == 'test1Db'
    }

    void "Test persist and retrieve entities with multi tenancy"() {
        setup:
        CompanyB.eachTenant {
            DB.drop().toBlocking().first()
        }

        when:"A tenant id is present"
        System.setProperty(SystemPropertyTenantResolver.PROPERTY_NAME, "test1")

        then:"the correct tenant is used"
        CompanyB.count().toBlocking().first() == 0
        CompanyB.DB.name == 'test1Db'

        when:"An object is saved"
        new CompanyB(name: "Foo").save(flush:true).toBlocking().first()

        then:"The results are correct"
        CompanyB.count().toBlocking().first() == 1

        when:"The tenant id is switched"
        System.setProperty(SystemPropertyTenantResolver.PROPERTY_NAME, "test2")

        then:"the correct tenant is used"
        CompanyB.DB.name == 'test2Db'
        CompanyB.count().toBlocking().first() == 0
        CompanyB.withTenant("test1") {
            count().toBlocking().first() == 1
        }

        when:"each tenant is iterated over"
        Map tenantIds = [:]
        CompanyB.eachTenant { String tenantId ->
            tenantIds.put(tenantId, count().toBlocking().first())
        }

        then:"The result is correct"
        tenantIds == [test1:1, test2:0]
    }
    List getDomainClasses() {
        [CompanyB]
    }

}
@Entity
class CompanyB implements RxMongoEntity<CompanyB>, MultiTenant<CompanyB> {
    ObjectId id
    String name
}
