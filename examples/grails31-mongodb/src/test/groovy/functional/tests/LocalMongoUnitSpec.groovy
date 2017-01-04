package functional.tests

//tag::structure[]
import grails.test.mongodb.MongoSpec

class LocalMongoUnitSpec extends MongoSpec {

    // Specs ...

//end::structure[]
    void "Test GORM access"(){
        setup:
        Book.DB.drop()

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
}
//end::structure[]
