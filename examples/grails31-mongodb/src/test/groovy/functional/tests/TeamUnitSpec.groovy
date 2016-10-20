package functional.tests

import grails.test.mongodb.MongoSpec

class TeamUnitSpec extends MongoSpec {

    void "get() doesn't throw NPE"() {
        when:
        Team team = Team.get("123")

        then:
        !team
    }
}
