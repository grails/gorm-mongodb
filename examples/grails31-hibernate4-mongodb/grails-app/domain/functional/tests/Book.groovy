package functional.tests

import grails.mongodb.MongoEntity

class Book {

    String title

    static mapWith = "mongo"

    static constraints = {
        title blank:false
    }
}
