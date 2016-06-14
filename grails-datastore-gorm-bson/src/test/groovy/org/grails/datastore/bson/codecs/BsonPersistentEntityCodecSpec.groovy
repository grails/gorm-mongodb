package org.grails.datastore.bson.codecs

import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings
import org.grails.datastore.bson.JsonWriter
import org.grails.datastore.bson.codecs.domain.Person
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import spock.lang.Specification

/**
 * Created by graemerocher on 14/06/16.
 */
class BsonPersistentEntityCodecSpec extends Specification {

    void "test marshall entity to JSON"() {
        given:"A mapping context"
        MappingContext mappingContext = new KeyValueMappingContext("test")
        PersistentEntity entity = mappingContext.addPersistentEntity(Person)

        CodecRegistry codecRegistry = CodecRegistries.fromProviders(new CodecExtensions())

        when:"An entity is is marshaled"

        BsonPersistentEntityCodec codec = new BsonPersistentEntityCodec(codecRegistry, entity)

        def sw = new StringWriter()
        def date = new Date().parse('yyyy/MM/dd', '1973/07/09')
        codec.encode(new JsonWriter(sw,new JsonWriterSettings(JsonMode.STRICT)), new Person(name: "Fred", age: 12, dateOfBirth: date))


        then:"The result is encoded JSON"
        sw.toString() == '{"age":12,"dateOfBirth":"1973-07-09T00:00+0000","name":"Fred"}'

    }
}


