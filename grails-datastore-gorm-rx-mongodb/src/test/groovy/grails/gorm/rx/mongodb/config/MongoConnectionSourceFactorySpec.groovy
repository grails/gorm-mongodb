package grails.gorm.rx.mongodb.config

import com.mongodb.ReadPreference
import com.mongodb.rx.client.MongoClient
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.core.connections.ConnectionSources
import org.grails.datastore.mapping.core.connections.ConnectionSourcesInitializer
import org.grails.datastore.mapping.mongo.config.MongoSettings
import org.grails.datastore.rx.mongodb.connections.MongoConnectionSourceFactory
import org.grails.datastore.rx.mongodb.connections.MongoConnectionSourceSettings
import spock.lang.Specification

/**
 * Created by graemerocher on 30/06/16.
 */
class MongoConnectionSourceFactorySpec extends Specification {

    void "Test that the configured database name is used if unspecified"() {
        when:"A factory instance"
        MongoConnectionSourceFactory factory = new MongoConnectionSourceFactory()
        factory.setDatabaseName("foo")

        ConnectionSources<MongoClient, MongoConnectionSourceSettings> sources = ConnectionSourcesInitializer.create(factory, DatastoreUtils.createPropertyResolver(
                (MongoSettings.SETTING_URL): "mongodb://localhost"
        ))

        then:"The correct database name is used"
        sources.defaultConnectionSource.settings.database == 'foo'

        when:"A database name is specified"
        sources = ConnectionSourcesInitializer.create(factory, DatastoreUtils.createPropertyResolver(
                (MongoSettings.SETTING_URL): "mongodb://localhost/bar"
        ))

        then:"The correct database name is used"
        sources.defaultConnectionSource.settings.database == 'bar'
    }

    void "Test MongoDB connection sources factory creates the correct configuration"() {
        when:"A factory instance"
        MongoConnectionSourceFactory factory = new MongoConnectionSourceFactory()
        ConnectionSources<MongoClient, MongoConnectionSourceSettings> sources = ConnectionSourcesInitializer.create(factory, DatastoreUtils.createPropertyResolver(
                (MongoSettings.SETTING_URL): "mongodb://localhost/myDb",
                (MongoSettings.SETTING_CONNECTIONS): [
                        another:[
                                url:"mongodb://localhost/anotherDb"
                        ]
                ]
        ))

        then:"The connection sources are correct"
        sources.defaultConnectionSource.name == ConnectionSource.DEFAULT
        sources.defaultConnectionSource.settings.url.database == 'myDb'
        sources.allConnectionSources.size() == 2
        sources.getConnectionSource('another').settings.url.database == 'anotherDb'

        cleanup:
        sources?.close()
    }

    void "test mongo client settings builder with fallback"() {
        when:"using a property resolver"
        Map myMap = ['grails.mongodb.options.readPreference': 'secondary',
                     (MongoSettings.SETTING_URL): "mongodb://localhost/myDb",
                     'grails.mongodb.options.clusterSettings.maxWaitQueueSize': '10',
                     (MongoSettings.SETTING_CONNECTIONS): [
                             another:[
                                     url:"mongodb://localhost/anotherDb"
                             ]
                     ]]

        MongoConnectionSourceFactory factory = new MongoConnectionSourceFactory()
        ConnectionSources<MongoClient, MongoConnectionSourceSettings> sources = ConnectionSourcesInitializer.create(factory, DatastoreUtils.createPropertyResolver(myMap))


        then:"The connection sources are correct"
        sources.defaultConnectionSource.name == ConnectionSource.DEFAULT
        sources.defaultConnectionSource.settings.url.database == 'myDb'
        sources.defaultConnectionSource.settings.options.build().readPreference == ReadPreference.secondary()
        sources.allConnectionSources.size() == 2
        sources.getConnectionSource('another').settings.url.database == 'anotherDb'
        sources.getConnectionSource('another').settings.options.build().readPreference == ReadPreference.secondary()

        cleanup:
        sources?.close()
    }

}
