package org.grails.datastore.gorm.mongo

import groovy.transform.NotYetImplemented
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.grails.datastore.mapping.mongo.MongoDatastore
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by graemerocher on 27/09/2016.
 */
class CustomCodecSpec extends Specification {

    @AutoCleanup @Shared MongoDatastore datastore = new MongoDatastore(['grails.mongodb.codecs':[BirthdayCodec]], Person)

    @NotYetImplemented
    void "Test custom codecs"() {
        when:"A new person is saved"
        new Person(name: "Fred", birthday: new Birthday(new Date())).save(flush:true)

        Person p = Person.first()

        then:"The result is correct"
        p.name == "Fred"
        p.birthday
    }
}

class BirthdayCodec implements Codec<Birthday> {
    Birthday decode(BsonReader reader, DecoderContext decoderContext) {
        return new Birthday(date: new Date(reader.readDateTime()))
    }
    void encode(BsonWriter writer, Birthday value, EncoderContext encoderContext) {
        writer.writeDateTime(value.date.time)
    }
    Class<Birthday> getEncoderClass() { Birthday }
}