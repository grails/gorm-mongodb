package grails.test.mongodb

import com.mongodb.MongoClient
import grails.config.Config
import grails.persistence.Entity
import groovy.transform.CompileStatic
import org.grails.config.PropertySourcesConfig
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.grails.io.support.DefaultResourceLoader
import org.grails.io.support.ResourceLoader
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.boot.env.PropertySourcesLoader
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.io.Resource
import org.springframework.core.type.filter.AnnotationTypeFilter
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

    @Shared
    MappingContext mappingContext = {
        def ctx = new KeyValueMappingContext("test")
        ctx.setCanInitializeEntities(true)
        return ctx
    }()

    abstract MongoClient getMongoClient()

    /**
     * @return The domain classes
     */
    protected List<Class> getDomainClasses() { [] }

    void setupSpec() {
        MongoClient mongoClient = getMongoClient()
        PropertySourcesLoader loader = new PropertySourcesLoader()
        ResourceLoader resourceLoader = new DefaultResourceLoader()
        loader.load resourceLoader.getResource("application.yml") as Resource
        loader.load resourceLoader.getResource("application.groovy") as Resource
        Config config = new PropertySourcesConfig(loader.propertySources)
        List<Class> domainClasses = getDomainClasses()
        if (!domainClasses) {
            ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(false)
            componentProvider.addIncludeFilter(new AnnotationTypeFilter(Entity))

            for (BeanDefinition candidate in componentProvider.findCandidateComponents(config.getProperty('grails.codegen.defaultPackage'))) {
                Class persistentEntity = Class.forName(candidate.beanClassName)
                domainClasses << persistentEntity
                mappingContext.addPersistentEntity(persistentEntity)
            }
        }
        mongoDatastore = new MongoDatastore(mongoClient, config, (Class[])domainClasses.toArray())
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

}
