package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity
import org.bson.types.ObjectId
import spock.lang.Issue

/**
 * Created by graemerocher on 25/03/14.
 */
class SimpleHasManySpec extends GormDatastoreSpec{

    @Issue('GPMONGODB-337')
    void "Test save and retrieve one-to-many"() {

        when:"A domain model is persisted"
            def c1 = new Chapter(title: "first");
            def c2 = new Chapter(title: "second");
            c1.save();
            c2.save();

            def book = new Book(name: "mybook");
            book.save(flush:true);
            if(!book.chapters) {
                book.chapters = new HashSet();
            }
            book.chapters.add(c1);
            book.chapters.add(c2);
            book.save(flush:true);
            session.clear()


            book = Book.get(book.id);
            def chapters = [] as List;
            book.chapters.each { it ->
                def chapter = [:] as Map;
                chapter.title = it.title;
                chapters << chapter;
            }

            then:"The retrieved data is correct"
                chapters.find { it.title == 'first'}
                chapters.find { it.title == 'second'}

    }

    void "Test save unidirectional one-to-many with cascade:none does not cascade changes to its children"() {
        when:"A domain model is persisted with a dirty child instance"
        String origTitle = "first"
        def c1 = new Chapter(title: origTitle)
        def c1Persisted = c1.save(flush:true)
        Long origVersion = c1Persisted.version
        session.clear()

        def book = new BookNoCascade(name: "mybook")
        book.chapters = new HashSet()
        c1.title = 'firstUpdated'
        book.chapters.add(c1)
        book.save(flush:true)
        session.clear()

        book = BookNoCascade.get(book.id)

        then:"The retrieved data is correct"
        def c1AfterSave = book.chapters.find { it.id == c1.id }
        c1AfterSave != null
        c1AfterSave.version == origVersion  // the version should stay at 0
        c1AfterSave.title == origTitle // the title should stay "first"
    }

    @Override
    List getDomainClasses() {
        [Book, Chapter, BookNoCascade]
    }
}

@Entity
class Book implements Serializable {
    ObjectId id
    Long version
    static hasMany = [chapters: Chapter]
    String name
    Set<Chapter> chapters
}

@Entity
class Chapter implements Serializable {
    ObjectId id
    Long version
    String title
}

@Entity
class BookNoCascade implements Serializable {
    ObjectId id
    Long version
    static hasMany = [chapters: Chapter]
    String name
    Set<Chapter> chapters
    static mapping = {
        chapters cascade: "none"
    }
}