package functional.tests

import grails.test.mixin.integration.Integration
import spock.lang.Specification

@Integration
class TeamSpec extends Specification {

    void "get() doesn't throw NPE"() {
        when:
        Team team = Team.get("12345")

        then:
        !team
    }

}
