package grails.test.mongodb

import com.mongodb.MongoClient
import grails.config.Config
import groovy.transform.CompileStatic
import org.grails.config.PropertySourcesConfig
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.springframework.boot.env.PropertySourcesLoader
import org.springframework.core.env.PropertyResolver
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.ResourceLoader
import org.springframework.transaction.support.TransactionSynchronizationManager
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * Base class for MongoDB tests
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 6.0.1
 */
@CompileStatic
abstract class MongoSpec extends Specification {

    @Shared
    @AutoCleanup
    MongoDatastore mongoDatastore

    @Shared
    Session session

    /**
     * @return Obtains the mapping context
     */
    MappingContext getMappingContext() {
        mongoDatastore.getMappingContext()
    }

    /**
     * @return The default mongo client
     */
    MongoClient createMongoClient() {
        return null
    }

    /**
     * @return The domain classes
     */
    protected List<Class> getDomainClasses() { [] }

    void setupSpec() {
        PropertySourcesLoader loader = new PropertySourcesLoader()
        ResourceLoader resourceLoader = new DefaultResourceLoader()
        loader.load resourceLoader.getResource("application.yml")
        loader.load resourceLoader.getResource("application.groovy")
        Config config = new PropertySourcesConfig(loader.propertySources)
        List<Class> domainClasses = getDomainClasses()
        if (!domainClasses) {
            def packageToScan = getPackageToScan(config)
            MongoClient mongoClient = createMongoClient()
            if (mongoClient) {
                mongoDatastore = new MongoDatastore(mongoClient, config, Package.getPackage(packageToScan))
            } else {
                mongoDatastore = new MongoDatastore((PropertyResolver) config, Package.getPackage(packageToScan))
            }
        }
        else {
            MongoClient mongoClient = createMongoClient()
            if (mongoClient) {
                mongoDatastore = new MongoDatastore(mongoClient, config, (Class[])domainClasses.toArray())
            } else {
                mongoDatastore = new MongoDatastore((PropertyResolver) config, (Class[])domainClasses.toArray())
            }
        }
    }

    void setup() {
        boolean existing = mongoDatastore.hasCurrentSession()
        session = existing ? mongoDatastore.currentSession : DatastoreUtils.bindSession(mongoDatastore.connect())
    }

    void cleanup() {
        if (!mongoDatastore.hasCurrentSession()) {
            TransactionSynchronizationManager.unbindResource(mongoDatastore)
            DatastoreUtils.closeSessionOrRegisterDeferredClose(session, mongoDatastore)
        }
    }

    /**
     * Obtains the default package to scan
     *
     * @param config The configuration
     * @return The package to scan
     */
    protected String getPackageToScan(Config config) {
        config.getProperty('grails.codegen.defaultPackage', getClass().package.name)
    }
}
