package functional.tests

import grails.test.mixin.integration.Integration
import geb.spock.*
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@Integration
class BookSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "Test list books"() {
        expect:
        Book.list().toBlocking().first().size() == 0
    }

    void "Test save book"() {
        when:
        Book book = new Book(title:"The Stand").save().toBlocking().first()

        then:"The book is correct"
        book.title == "The Stand"
    }
}
