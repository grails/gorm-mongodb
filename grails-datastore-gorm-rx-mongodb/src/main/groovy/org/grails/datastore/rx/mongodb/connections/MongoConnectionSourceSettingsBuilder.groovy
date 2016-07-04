package org.grails.datastore.rx.mongodb.connections

import com.mongodb.ConnectionString
import com.mongodb.MongoCredential
import com.mongodb.async.client.MongoClientSettings
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.config.ConfigurationBuilder
import org.grails.datastore.mapping.mongo.config.MongoSettings
import org.springframework.core.env.PropertyResolver
import org.springframework.util.ReflectionUtils

/**
 * A builder for {@link MongoConnectionSourceSettings}
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class MongoConnectionSourceSettingsBuilder extends ConfigurationBuilder<MongoConnectionSourceSettings, MongoConnectionSourceSettings> {

    MongoConnectionSourceSettingsBuilder(PropertyResolver propertyResolver, String configurationPrefix, MongoConnectionSourceSettings fallback) {
        super(propertyResolver, configurationPrefix, fallback)
    }

    MongoConnectionSourceSettingsBuilder(PropertyResolver propertyResolver) {
        super(propertyResolver, MongoSettings.PREFIX)
    }

    MongoConnectionSourceSettingsBuilder(PropertyResolver propertyResolver, MongoConnectionSourceSettings fallback) {
        super(propertyResolver, MongoSettings.PREFIX, fallback)
    }

    @Override
    protected MongoConnectionSourceSettings createBuilder() {
        return new MongoConnectionSourceSettings()
    }

    @Override
    protected MongoConnectionSourceSettings toConfiguration(MongoConnectionSourceSettings builder) {
        return builder
    }

    @Override
    protected void newChildBuilder(Object builder, String configurationPath) {
        applyCredentials(builder)
        applyConnectionString(builder)
    }

    @Override
    protected void startBuild(Object builder, String configurationPath) {
        applyCredentials(builder)
        applyConnectionString(builder)
    }

    @Override
    Object newChildBuilderForFallback(Object childBuilder, Object fallbackConfig) {
        if(( childBuilder instanceof MongoClientSettings.Builder) && (fallbackConfig instanceof MongoClientSettings.Builder)) {
            return MongoClientSettings.builder(((MongoClientSettings.Builder)fallbackConfig).build())
        }
        return childBuilder
    }

    protected void applyCredentials(builder) {
        def credentialListMethod = ReflectionUtils.findMethod(builder.getClass(), 'credentialList', List)
        if (credentialListMethod != null) {
            def username = rootBuilder.username
            def password = rootBuilder.password
            def databaseName = rootBuilder.database

            if (username != null && password != null) {
                def credential = MongoCredential.createCredential(username, databaseName, password.toCharArray())
                credentialListMethod.invoke(builder, Arrays.asList(credential))
            }
        }
    }

    protected void applyConnectionString(builder) {
        def applyConnectionStringMethod = ReflectionUtils.findMethod(builder.getClass(), 'applyConnectionString', ConnectionString)
        if (applyConnectionStringMethod != null) {
            ConnectionString connectionString = rootBuilder.url
            if (connectionString == null) {

                def username = rootBuilder.username
                def password = rootBuilder.password
                def host = rootBuilder.host
                def port = rootBuilder.port
                def databaseName = rootBuilder.databaseName
                String uAndP = username && password ? "$username:$password@" : ''
                connectionString = new ConnectionString("mongodb://${uAndP}${host}:${port}/$databaseName")
            }
            applyConnectionStringMethod.invoke(builder, connectionString)
        }
    }
}
