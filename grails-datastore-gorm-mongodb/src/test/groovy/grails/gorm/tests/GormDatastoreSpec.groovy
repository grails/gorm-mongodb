package grails.gorm.tests

import com.mongodb.BasicDBObject
import com.mongodb.MongoClient
import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import org.bson.Document
import org.grails.datastore.bson.query.BsonQuery
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.mongo.Birthday
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.engine.types.AbstractMappingAwareCustomTypeMarshaller
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.mongo.AbstractMongoSession
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.grails.datastore.mapping.query.Query
import org.grails.validation.GrailsDomainClassValidator
import org.springframework.context.support.GenericApplicationContext
import org.springframework.validation.Validator
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
/**
 * Created by graemerocher on 06/06/16.
 */
abstract class GormDatastoreSpec extends Specification {

    static final CURRENT_TEST_NAME = "current.gorm.test"

    List getDomainClasses() {
        [       Book, ChildEntity, City, ClassWithListArgBeforeValidate, ClassWithNoArgBeforeValidate,
                ClassWithOverloadedBeforeValidate, CommonTypes, Country, EnumThing, Face, Highway,
                Location, ModifyPerson, Nose, OptLockNotVersioned, OptLockVersioned, Person, PersonEvent,
                Pet, PetType, Plant, PlantCategory, Publication, Task, TestEntity]
    }

    @Shared @AutoCleanup MongoDatastore mongoDatastore
    @Shared MongoClient mongoClient
    @Shared GrailsApplication grailsApplication
    @Shared MappingContext mappingContext

    AbstractMongoSession session

    void setupSpec() {
        def allClasses = getDomainClasses() as Class[]
        def ctx = new GenericApplicationContext()
        ctx.refresh()

        def databaseName = System.getProperty(GormDatastoreSpec.CURRENT_TEST_NAME) ?: 'test'


        mongoDatastore = new MongoDatastore([(MongoDatastore.SETTING_DATABASE_NAME): databaseName])
        mappingContext = mongoDatastore.mappingContext
        mappingContext.mappingFactory.registerCustomType(new AbstractMappingAwareCustomTypeMarshaller<Birthday, Document, Document>(Birthday) {
            @Override
            protected Object writeInternal(PersistentProperty property, String key, Birthday value, Document nativeTarget) {

                final converted = value.date.time
                nativeTarget.put(key, converted)
                return converted
            }

            @Override
            protected void queryInternal(PersistentProperty property, String key, Query.PropertyCriterion criterion, Document nativeQuery) {
                if (criterion instanceof Query.Between) {
                    def dbo = new BasicDBObject()
                    dbo.put(BsonQuery.GTE_OPERATOR, criterion.getFrom().date.time)
                    dbo.put(BsonQuery.LTE_OPERATOR, criterion.getTo().date.time)
                    nativeQuery.put(key, dbo)
                }
                else {
                    nativeQuery.put(key, criterion.value.date.time)
                }
            }

            @Override
            protected Birthday readInternal(PersistentProperty property, String key, Document nativeSource) {
                final num = nativeSource.get(key)
                if (num instanceof Long) {
                    return new Birthday(new Date(num))
                }
                return null
            }
        })
        mappingContext.addPersistentEntities(allClasses as Class[])
        mongoClient = mongoDatastore.getMongoClient()

        grailsApplication = new DefaultGrailsApplication(allClasses, getClass().getClassLoader())
        grailsApplication.mainContext = ctx
        grailsApplication.initialise()
    }

    void setupValidator(Class entityClass, Validator validator = null) {
        PersistentEntity entity = mappingContext.persistentEntities.find { PersistentEntity e -> e.javaClass == entityClass }
        if (entity) {
            mappingContext.addEntityValidator(entity, validator ?:
                    new GrailsDomainClassValidator(
                            grailsApplication: grailsApplication,
                            domainClass: grailsApplication.getDomainClass(entity.javaClass.name)
                    ) )
        }
    }

    void setup() {
        session = mongoDatastore.connect()
        DatastoreUtils.bindSession session
    }

    void cleanup() {
        session.disconnect()
        DatastoreUtils.unbindSession(session)
        mongoDatastore.getMongoClient().dropDatabase(mongoDatastore.defaultDatabase)
        mongoDatastore.buildIndex()
        for(cls in getDomainClasses()) {
            GormEnhancer.findValidationApi(cls).setValidator(null)
        }
    }

}
