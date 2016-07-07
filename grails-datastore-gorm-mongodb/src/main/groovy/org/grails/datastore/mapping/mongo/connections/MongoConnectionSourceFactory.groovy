package org.grails.datastore.mapping.mongo.connections

import com.mongodb.MongoClient
import com.mongodb.MongoClientOptions
import com.mongodb.MongoClientURI
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.connections.AbstractConnectionSourceFactory
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.core.connections.ConnectionSourceFactory
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings
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
class MongoConnectionSourceFactory extends AbstractConnectionSourceFactory<MongoClient, MongoConnectionSourceSettings> {

    MongoClientOptions.Builder clientOptionsBuilder

    @Override
    Serializable getConnectionSourcesConfigurationKey() {
        return MongoSettings.SETTING_CONNECTIONS
    }

    @Override
    protected <F extends ConnectionSourceSettings> MongoConnectionSourceSettings buildSettings(String name, PropertyResolver configuration, F fallbackSettings, boolean isDefaultDataSource) {
        String prefix = isDefaultDataSource ? MongoSettings.PREFIX : MongoSettings.SETTING_CONNECTIONS + ".$name"
        MongoConnectionSourceSettingsBuilder settingsBuilder = new MongoConnectionSourceSettingsBuilder(configuration, prefix, fallbackSettings)
        MongoClientOptions.Builder builder = clientOptionsBuilder ?: settingsBuilder.clientOptionsBuilder
        MongoConnectionSourceSettings settings = settingsBuilder.build()
        if(builder != null) {
            settings.options = builder
        }
        return settings
    }

    @Override
    ConnectionSource<MongoClient, MongoConnectionSourceSettings> create(String name, MongoConnectionSourceSettings settings) {
        MongoClientOptions.Builder builder = settings.options
        MongoClient client = builder != null ? new MongoClient(new MongoClientURI(settings.url.toString(), builder)) : new MongoClient(new MongoClientURI(settings.url.toString()))
        return new DefaultConnectionSource<MongoClient, MongoConnectionSourceSettings>(name, client, settings);
    }
}

