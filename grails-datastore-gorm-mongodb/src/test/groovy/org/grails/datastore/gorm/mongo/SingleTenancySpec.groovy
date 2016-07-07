package org.grails.datastore.gorm.mongo

import grails.gorm.MultiTenant
import grails.gorm.annotation.Entity
import grails.mongodb.MongoEntity
import org.bson.types.ObjectId
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.grails.datastore.mapping.mongo.config.MongoSettings
import org.grails.datastore.mapping.multitenancy.resolvers.FixedTenantResolver
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by graemerocher on 07/07/2016.
 */
class SingleTenancySpec extends Specification {

    @Shared MongoDatastore datastore

    void setupSpec() {
        Map config = [
                "grails.gorm.multiTenancy.mode":"SINGLE",
                "grails.gorm.multiTenancy.tenantResolverClass":DummyResolver,
                (MongoSettings.SETTING_URL): "mongodb://localhost/defaultDb",
                (MongoSettings.SETTING_CONNECTIONS): [
                        test1: [
                                url: "mongodb://localhost/test1Db"
                        ],
                        test2: [
                                url: "mongodb://localhost/test2Db"
                        ]
                ]
        ]
        this.datastore = new MongoDatastore(config, getDomainClasses() as Class[])
    }

    void "Test multi tenancy state"() {
        expect:
        City.DB.name == "defaultDb"
        CompanyB.DB.name == 'test1Db'
    }

    void cleanupSpec() {
        datastore.close()
    }


    List getDomainClasses() {
        [City, CompanyB]
    }

    static class DummyResolver extends FixedTenantResolver {
        DummyResolver() {
            super("test1")
        }
    }
}
@Entity
class CompanyB implements MongoEntity<CompanyB>, MultiTenant {
    ObjectId id
    String name
}