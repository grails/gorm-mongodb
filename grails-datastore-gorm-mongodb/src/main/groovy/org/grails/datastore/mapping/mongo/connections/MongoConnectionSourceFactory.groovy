package org.grails.datastore.mapping.mongo.connections

import com.mongodb.MongoClient
import com.mongodb.MongoClientOptions
import com.mongodb.MongoClientURI
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.core.connections.ConnectionSourceFactory
import org.grails.datastore.mapping.core.connections.DefaultConnectionSource
import org.grails.datastore.mapping.mongo.config.MongoSettings
import org.springframework.core.env.PropertyResolver

/**
 * A factory for building {@link MongoClient} instances
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class MongoConnectionSourceFactory implements ConnectionSourceFactory<MongoClient, MongoConnectionSourceSettings> {

    MongoClientOptions.Builder clientOptionsBuilder

    @Override
    ConnectionSource<MongoClient, MongoConnectionSourceSettings> create(String name, PropertyResolver configuration, MongoConnectionSourceSettings fallback = null) {
        String prefix = ConnectionSource.DEFAULT == name ? MongoSettings.PREFIX : MongoSettings.SETTING_CONNECTIONS + ".$name"
        MongoConnectionSourceSettingsBuilder settingsBuilder = new MongoConnectionSourceSettingsBuilder(configuration, prefix, fallback)
        MongoConnectionSourceSettings settings = settingsBuilder.build()

        MongoClientOptions.Builder builder = clientOptionsBuilder ?: settingsBuilder.clientOptionsBuilder

        MongoClient client = builder != null ? new MongoClient(new MongoClientURI(settings.url.toString(), builder)) : new MongoClient(new MongoClientURI(settings.url.toString()))
        return new DefaultConnectionSource<MongoClient, MongoConnectionSourceSettings>(name, client, settings);
    }

    @Override
    Serializable getConnectionSourcesConfigurationKey() {
        return MongoSettings.SETTING_CONNECTIONS
    }
}
