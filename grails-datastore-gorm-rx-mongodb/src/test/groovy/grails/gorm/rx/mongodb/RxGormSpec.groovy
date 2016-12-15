package grails.gorm.rx.mongodb

import org.grails.datastore.mapping.mongo.config.MongoSettings
import org.grails.datastore.rx.mongodb.RxMongoDatastoreClient
import spock.lang.Shared
import spock.lang.Specification

abstract class RxGormSpec extends Specification {

    @Shared RxMongoDatastoreClient client

    void setupSpec() {
        def classes = getDomainClasses()
        client = createMongoDatastoreClient(classes)
    }

    protected RxMongoDatastoreClient createMongoDatastoreClient(List<Class> classes) {
        def config = [(MongoSettings.SETTING_DATABASE_NAME): "test"]
        // disable decimal type support on Travis, since MongoDB 3.4 support doesn't exist there yet
        if(System.getenv('TRAVIS')) {
            config.put(MongoSettings.SETTING_DECIMAL_TYPE, false)
        }
        new RxMongoDatastoreClient(config, classes as Class[])
    }

    void setup() {
        client.dropDatabase()
        client.rebuildIndex()
    }

    void cleanupSpec() {
        client?.close()
    }

    abstract List<Class> getDomainClasses()
}
