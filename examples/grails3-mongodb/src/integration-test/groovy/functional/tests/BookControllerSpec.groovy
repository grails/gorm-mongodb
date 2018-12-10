package functional.tests

import grails.testing.mixin.integration.Integration

import geb.spock.*

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@Integration
class BookControllerSpec extends GebSpec {

    def setup() {
    }

    def cleanup() {
    }

    void "Test list books"() {
        when:"The home page is visited"
            go '/book/index'

        then:"The title is correct"
        	title == "Book List"
    }

    void "Test save book"() {
        when:
        go "/book/create"
        $('form').title = "The Stand"
        $('input.save').click()

        then:"The book is correct"
        title == "Show Book"
        $('li.fieldcontain div').text() == 'The Stand'

    }
}
