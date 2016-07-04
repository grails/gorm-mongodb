package org.grails.datastore.rx.mongodb.connections

import com.mongodb.rx.client.MongoClient
import com.mongodb.rx.client.MongoClients
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.core.connections.ConnectionSourceFactory
import org.grails.datastore.mapping.core.connections.DefaultConnectionSource
import org.grails.datastore.mapping.mongo.config.MongoSettings
import org.springframework.core.env.PropertyResolver

/**
 * A Factory for creating MongoDB connections
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class MongoConnectionSourceFactory implements ConnectionSourceFactory<MongoClient, MongoConnectionSourceSettings> {
    @Override
    ConnectionSource<MongoClient, MongoConnectionSourceSettings> create(String name, PropertyResolver configuration, MongoConnectionSourceSettings fallback = null) {
        String prefix = ConnectionSource.DEFAULT == name ? MongoSettings.PREFIX : MongoSettings.SETTING_CONNECTIONS + ".$name"
        MongoConnectionSourceSettingsBuilder settingsBuilder = new MongoConnectionSourceSettingsBuilder(configuration, prefix, fallback)
        MongoConnectionSourceSettings settings = settingsBuilder.build()

        MongoClient client
        if(settings.observableAdapter != null) {
            client = MongoClients.create(settings.options.build(), settings.observableAdapter)
        }
        else {
            client = MongoClients.create(settings.options.build())
        }
        return new DefaultConnectionSource<MongoClient, MongoConnectionSourceSettings>(name, client, settings);
    }

    @Override
    Serializable getConnectionSourcesConfigurationKey() {
        return MongoSettings.SETTING_CONNECTIONS
    }
}
