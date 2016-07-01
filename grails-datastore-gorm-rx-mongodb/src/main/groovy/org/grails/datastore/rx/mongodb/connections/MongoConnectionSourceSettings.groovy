package org.grails.datastore.rx.mongodb.connections

import com.mongodb.async.client.MongoClientSettings
import com.mongodb.rx.client.ObservableAdapter
import groovy.transform.AutoClone
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy
import org.grails.datastore.mapping.mongo.connections.AbstractMongoConnectionSourceSettings

/**
 * Settings for MongoDB Rx driver
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@AutoClone
@Builder(builderStrategy = SimpleStrategy, prefix = '')
class MongoConnectionSourceSettings extends AbstractMongoConnectionSourceSettings  {

    /**
     * The observable adapter to use
     */
    ObservableAdapter observableAdapter
    /**
     * The {@link MongoClientSettings} object
     */
    MongoClientSettings options
}
