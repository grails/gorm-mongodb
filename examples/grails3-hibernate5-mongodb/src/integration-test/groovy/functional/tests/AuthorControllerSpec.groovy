package functional.tests

import geb.spock.GebSpec
import grails.testing.mixin.integration.Integration

@Integration
class AuthorControllerSpec extends GebSpec {

    def setup() {
    }

    def cleanup() {
    }

    void "Test list authors"() {
        when:"The home page is visited"
        go '/author/index'

        then:"The name is correct"
        title == "Author List"
    }

    void "Test save author"() {
        when:
        go "/author/create"
        $('form').name = "Stephen King"
        $('input.save').click()

        then:"The author is correct"
        title == "Show Author"
        $('li.fieldcontain div').text() == 'Stephen King'

    }
}
