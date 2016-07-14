package grails.gorm.rx.mongodb.multitenancy

import grails.gorm.annotation.Entity
import grails.gorm.rx.MultiTenant
import grails.gorm.rx.mongodb.RxMongoEntity
import grails.gorm.rx.mongodb.domains.City
import grails.mongodb.MongoEntity
import org.bson.types.ObjectId
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.mongo.config.MongoSettings
import org.grails.datastore.mapping.multitenancy.AllTenantsResolver
import org.grails.datastore.mapping.multitenancy.resolvers.SystemPropertyTenantResolver
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import static com.mongodb.client.model.Filters.eq

/**
 * Created by graemerocher on 13/07/2016.
 */
class MultiTenancySpec extends Specification{
    @Shared @AutoCleanup RxMongoDatastoreClient datastoreClient

    void setupSpec() {
        Map config = [
                "grails.gorm.multiTenancy.mode"               :"DISCRIMINATOR",
                "grails.gorm.multiTenancy.tenantResolverClass":MyResolver,
                (MongoSettings.SETTING_URL)                   : "mongodb://localhost/defaultDb",
        ]
        this.datastoreClient = new RxMongoDatastoreClient(DatastoreUtils.createPropertyResolver(config), getDomainClasses() as Class[])
    }

    void setup() {
        System.setProperty(SystemPropertyTenantResolver.PROPERTY_NAME, "")
    }

    void "Test persist and retrieve entities with multi tenancy"() {
        setup:
        CompanyC.DB.drop().toBlocking().first()

        when:"A tenant id is present"
        System.setProperty(SystemPropertyTenantResolver.PROPERTY_NAME, "test1")

        then:"the correct tenant is used"
        CompanyC.count().toBlocking().first() == 0
        CompanyC.DB.name == 'defaultDb'

        when:"An object is saved"
        new CompanyC(name: "Foo").save(flush:true).toBlocking().first()

        then:"The results are correct"
        CompanyC.count().toBlocking().first() == 1

        when:"The tenant id is switched"
        System.setProperty(SystemPropertyTenantResolver.PROPERTY_NAME, "test2")

        then:"the correct tenant is used"
        CompanyC.DB.name == 'defaultDb'
        CompanyC.count().toBlocking().first() == 0
        !CompanyC.find(eq("name", "Foo")).toObservable().toList().toBlocking().first()
        CompanyC.withTenant("test1") { Serializable tenantId ->
            assert tenantId
            count().toBlocking().first() == 1
        }
        CompanyC.withTenant("test1").count().toBlocking().first() == 1

        when:"each tenant is iterated over"
        Map tenantIds = [:]
        CompanyC.eachTenant { String tenantId ->
            tenantIds.put(tenantId, count().toBlocking().first())
        }

        then:"The result is correct"
        tenantIds == [test1:1, test2:0]
    }

    List getDomainClasses() {
        [City, CompanyC]
    }

    static class MyResolver extends SystemPropertyTenantResolver implements AllTenantsResolver {
        @Override
        Iterable<Serializable> resolveTenantIds() {
            ['test1','test2']
        }
    }
}
@Entity
class CompanyC implements RxMongoEntity<CompanyC>, MultiTenant<CompanyC> {
    ObjectId id
    String name
    String parent
    static mapping = {
        tenantId name:'parent'
    }
}