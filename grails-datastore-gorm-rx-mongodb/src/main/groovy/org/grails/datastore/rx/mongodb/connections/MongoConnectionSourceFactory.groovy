package org.grails.datastore.rx.mongodb.connections

import com.mongodb.async.client.MongoClientSettings
import com.mongodb.rx.client.MongoClient
import com.mongodb.rx.client.MongoClients
import com.mongodb.rx.client.ObservableAdapter
import groovy.transform.CompileStatic
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.mapping.core.connections.AbstractConnectionSourceFactory
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.core.connections.ConnectionSourceFactory
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings
import org.grails.datastore.mapping.core.connections.DefaultConnectionSource
import org.grails.datastore.mapping.mongo.config.MongoSettings
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.PropertyResolver

/**
 * A Factory for creating MongoDB connections
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class MongoConnectionSourceFactory extends AbstractConnectionSourceFactory<MongoClient, MongoConnectionSourceSettings> {

    @Autowired(required = false)
    ObservableAdapter observableAdapter

    @Autowired(required = false)
    MongoClientSettings mongoClientSettings

    String databaseName

    /**
     * An optional additional registry
     */
    @Autowired(required = false)
    CodecRegistry codecRegistry

    /**
     * Optional additional codecs
     */
    @Autowired(required = false)
    List<Codec> codecs = []

    @Override
    Serializable getConnectionSourcesConfigurationKey() {
        return MongoSettings.SETTING_CONNECTIONS
    }

    @Override
    protected <F extends ConnectionSourceSettings> MongoConnectionSourceSettings buildSettings(String name, PropertyResolver configuration, F fallbackSettings, boolean isDefaultDataSource) {
        String prefix = isDefaultDataSource ? MongoSettings.PREFIX : MongoSettings.SETTING_CONNECTIONS + ".$name"
        MongoConnectionSourceSettingsBuilder settingsBuilder = new MongoConnectionSourceSettingsBuilder(configuration, prefix, fallbackSettings)


        MongoConnectionSourceSettings settings = settingsBuilder.build()
        if(observableAdapter != null) {
            settings.observableAdapter(observableAdapter)
        }
        if(databaseName != null) {
            if(settings.getDatabase().equals(MongoSettings.DEFAULT_DATABASE_NAME)) {
                settings.databaseName(databaseName)
            }
        }

        if(isDefaultDataSource) {
            CodecRegistry codecRegistry = CodecRegistries.fromCodecs(codecs as List<? extends Codec<?>>)
            if(this.codecRegistry != null) {
                codecRegistry = CodecRegistries.fromRegistries(codecRegistry, this.codecRegistry)
            }
            settings.codecRegistry = codecRegistry
        }
        return settings
    }

    @Override
    ConnectionSource<MongoClient, MongoConnectionSourceSettings> create(String name, MongoConnectionSourceSettings settings) {
        MongoClient client
        MongoClientSettings mongoClientSettings = mongoClientSettings ?: settings.options.build()

        if(settings.observableAdapter != null) {
            client = MongoClients.create(mongoClientSettings, settings.observableAdapter)
        }
        else {
            client = MongoClients.create(mongoClientSettings)
        }
        return new DefaultConnectionSource<MongoClient, MongoConnectionSourceSettings>(name, client, settings);

    }

    @Override
    def <F extends ConnectionSourceSettings> MongoConnectionSourceSettings buildRuntimeSettings(String name, PropertyResolver configuration, F fallbackSettings) {
        MongoConnectionSourceSettingsBuilder settingsBuilder = new MongoConnectionSourceSettingsBuilder(configuration, "", fallbackSettings)

        MongoConnectionSourceSettings settings = settingsBuilder.build()
        if(observableAdapter != null) {
            settings.observableAdapter(observableAdapter)
        }
        if(databaseName != null) {
            settings.databaseName(databaseName)
        }
        return settings
    }
}
