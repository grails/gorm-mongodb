package functional.tests

import com.github.fakemongo.Fongo
import com.mongodb.MongoClient
import grails.test.mongodb.MongoSpec

/**
 * Created by graemerocher on 17/10/16.
 */
class PersonSpec extends MongoSpec {
    @Override
    MongoClient getMongoClient() {
        return new Fongo(getClass().name).mongo
    }

    void "Test codecs work for custom properties"() {
        when:"An an instance with a custom codec is saved"
        def dob = new Date()
        new Person(name: "Fred", birthday: new Birthday(dob)).save(flush:true)
        Person person = Person.first()

        then:"The result is correct"
        person.name == "Fred"
        person.birthday.date == dob
    }
}
