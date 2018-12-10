package functional.tests

import grails.test.mongodb.MongoSpec

class TeamUnitSpec extends MongoSpec implements EmbeddedMongoClient {

    void "get() doesn't throw NPE"() {
        when:
        Team team = Team.get("123")

        then:
        !team
    }
}
