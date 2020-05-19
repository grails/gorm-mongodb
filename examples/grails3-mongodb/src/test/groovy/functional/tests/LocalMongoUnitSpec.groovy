package functional.tests

//tag::structure[]
import grails.test.mongodb.MongoSpec
import grails.validation.ValidationException
import spock.lang.Ignore

class LocalMongoUnitSpec extends MongoSpec implements EmbeddedMongoClient {

//end::structure[]
    void "Test GORM access"(){
        when:
        Book book = new Book(title: 'El Quijote').save(flush: true)

        then:
        Book.count() == 1

        when:
        book = Book.findByTitle('El Quijote')

        then:
        book.id
    }

//tag::structure[]
    @Ignore
    void "test fail on error"() {

        when:
        def invalid = new Book(title: "")
        invalid.save()

        then:
        thrown ValidationException
        invalid.hasErrors()
    }
}
//end::structure[]
