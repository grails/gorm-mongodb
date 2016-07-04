package org.grails.datastore.mapping.mongo.connections

import com.mongodb.MongoClientOptions
import groovy.transform.AutoClone
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

/**
 * Settings for MongoDB driver
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@AutoClone
@Builder(builderStrategy = SimpleStrategy, prefix = '')
class MongoConnectionSourceSettings extends AbstractMongoConnectionSourceSettings  {

    /**
     * The MongoClientOptions object
     */
    MongoClientOptions.Builder options = MongoClientOptions.builder()
}
