package functional.tests

class Book {

    String title

    static mapWith = "mongo"

    static constraints = {
        title blank:false
    }
}
