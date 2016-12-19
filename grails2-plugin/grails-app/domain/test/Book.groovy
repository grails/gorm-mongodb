package test

import grails.persistence.Entity

@Entity
class Book {

    String title

    static constraints = {
        title blank:false
    }
}
