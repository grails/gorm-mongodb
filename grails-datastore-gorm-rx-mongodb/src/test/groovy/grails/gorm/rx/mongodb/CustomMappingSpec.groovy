package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.CustomMapping
import grails.mongodb.geo.Point
import org.bson.Document

/**
 * Created by graemerocher on 03/02/2017.
 */
class CustomMappingSpec extends RxGormSpec {
    void "test custom document mapping"() {
        when:"A document is saved with a custom mapping"
        new CustomMapping(name: "test", loc: Point.valueOf(10, 15) ).save(flush:true).toBlocking().first()
        Document doc = CustomMapping.DB.getCollection(CustomMapping.collection.namespace.collectionName)
                                    .find().toObservable().toBlocking().first()
        then:
        CustomMapping.collection.namespace.collectionName == 'mycoll'
        CustomMapping.collection.namespace.databaseName == 'mydb'
        doc.get("my_name") == "test"
        doc.get("loc").inspect() == '[\'type\':\'Point\', \'coordinates\':[10.0, 15.0]]'

    }

    @Override
    List<Class> getDomainClasses() {
        [CustomMapping]
    }
}
