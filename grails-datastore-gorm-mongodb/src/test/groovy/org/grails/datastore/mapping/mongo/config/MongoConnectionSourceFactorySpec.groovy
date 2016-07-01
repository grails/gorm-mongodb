package org.grails.datastore.mapping.mongo.config

import com.mongodb.MongoClient
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.core.connections.ConnectionSources
import org.grails.datastore.mapping.core.connections.ConnectionSourcesInitializer
import org.grails.datastore.mapping.mongo.connections.MongoConnectionSourceFactory
import org.grails.datastore.mapping.mongo.connections.MongoConnectionSourceSettings
import spock.lang.Specification

/**
 * Created by graemerocher on 30/06/16.
 */
class MongoConnectionSourceFactorySpec extends Specification {

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
}
