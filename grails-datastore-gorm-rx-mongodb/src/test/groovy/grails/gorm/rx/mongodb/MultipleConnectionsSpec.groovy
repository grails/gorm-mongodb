package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.CompanyA
import grails.gorm.rx.mongodb.domains.CompanyB
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.mongo.config.MongoSettings
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient

/**
 * Created by graemerocher on 30/06/16.
 */
class MultipleConnectionsSpec extends RxGormSpec {
    @Override
    List<Class> getDomainClasses() {
        [CompanyA, CompanyB]
    }

    void "Test multiple datasources state"() {

        expect:
        CompanyA.DB.name == 'test1Db'
        CompanyB.DB.name == 'test2Db'
        CompanyA.withConnection("test2") { DB.name == 'test2Db' }
    }

    void "Test query multiple data sources"() {
        setup:
        CompanyA.DB.drop().toBlocking().first()
        CompanyB.DB.drop().toBlocking().first()

        when:"An entity is saved"
        new CompanyA(name:"One").save().toBlocking().first()

        then:"The results are correct"
        CompanyA.count().toBlocking().first() == 1
        CompanyA.withConnection("test2") { count().toBlocking().first() } == 0

        when:"An entity is saved to another connection"
        new CompanyA(name:"Two").save().toBlocking().first()
        CompanyA.withConnection("test2") { saveAll(new CompanyA(name: "Three")).toBlocking().first() }
        CompanyB.withConnection(ConnectionSource.DEFAULT) {
            save(new CompanyB(name:"Four")).toBlocking().first()
        }

        then:"The results are correct"
        CompanyA.withConnection("test2") { count().toBlocking().first() } == 1
        CompanyA.count().toBlocking().first() == 2
        CompanyB.count().toBlocking().first() == 1
    }

    @Override
    protected RxMongoDatastoreClient createMongoDatastoreClient(List<Class> classes) {
        Map config = [
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
        return new RxMongoDatastoreClient(DatastoreUtils.createPropertyResolver(config), CompanyA, CompanyB)
    }
}
