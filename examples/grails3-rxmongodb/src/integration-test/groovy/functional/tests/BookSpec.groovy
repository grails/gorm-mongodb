package functional.tests

import grails.testing.mixin.integration.Integration

import spock.lang.Specification

@Integration(applicationClass = Application)
class BookSpec extends Specification {

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
