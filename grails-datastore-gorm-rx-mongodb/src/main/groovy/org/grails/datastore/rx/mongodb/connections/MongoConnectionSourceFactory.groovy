package org.grails.datastore.rx.mongodb.connections

import com.mongodb.rx.client.MongoClient
import com.mongodb.rx.client.MongoClients
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.connections.AbstractConnectionSourceFactory
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.core.connections.ConnectionSourceFactory
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings
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
class MongoConnectionSourceFactory extends AbstractConnectionSourceFactory<MongoClient, MongoConnectionSourceSettings> {

    @Override
    Serializable getConnectionSourcesConfigurationKey() {
        return MongoSettings.SETTING_CONNECTIONS
    }

    @Override
    protected <F extends ConnectionSourceSettings> MongoConnectionSourceSettings buildSettings(String name, PropertyResolver configuration, F fallbackSettings, boolean isDefaultDataSource) {
        String prefix = isDefaultDataSource ? MongoSettings.PREFIX : MongoSettings.SETTING_CONNECTIONS + ".$name"
        MongoConnectionSourceSettingsBuilder settingsBuilder = new MongoConnectionSourceSettingsBuilder(configuration, prefix, fallbackSettings)
        return settingsBuilder.build()
    }

    @Override
    ConnectionSource<MongoClient, MongoConnectionSourceSettings> create(String name, MongoConnectionSourceSettings settings) {
        MongoClient client
        if(settings.observableAdapter != null) {
            client = MongoClients.create(settings.options.build(), settings.observableAdapter)
        }
        else {
            client = MongoClients.create(settings.options.build())
        }
        return new DefaultConnectionSource<MongoClient, MongoConnectionSourceSettings>(name, client, settings);

    }
}
