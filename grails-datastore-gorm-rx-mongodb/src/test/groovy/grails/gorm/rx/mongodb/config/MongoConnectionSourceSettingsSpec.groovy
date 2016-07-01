package grails.gorm.rx.mongodb.config

import com.mongodb.ReadPreference
import com.mongodb.ServerAddress
import com.mongodb.async.client.MongoClientSettings
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.rx.mongodb.connections.MongoConnectionSourceSettings
import org.grails.datastore.rx.mongodb.connections.MongoConnectionSourceSettingsBuilder
import spock.lang.Specification
/**
 * Created by graemerocher on 29/06/16.
 */
class MongoConnectionSourceSettingsSpec extends Specification{

    void "test mongo client settings builder"() {
        when:"using a property resolver"
        Map myMap = ['grails.mongodb.options.readPreference': 'secondary',
                     'grails.mongodb.host': 'mycompany',
                     'grails.mongodb.port': '1234',
                     'grails.mongodb.username': 'foo',
                     'grails.mongodb.password': 'bar',
                     'grails.mongodb.options.clusterSettings.maxWaitQueueSize': '10']

        def builder = new MongoConnectionSourceSettingsBuilder(DatastoreUtils.createPropertyResolver(myMap))
        MongoConnectionSourceSettings settings = builder.build()
        MongoClientSettings clientSettings = settings.options

        then:"The settings are correct"
        clientSettings.clusterSettings.hosts.contains(new ServerAddress("mycompany", 1234))
        clientSettings.readPreference == ReadPreference.secondary()
        clientSettings.clusterSettings.maxWaitQueueSize == 10
        clientSettings.credentialList.size() == 1
    }
}
