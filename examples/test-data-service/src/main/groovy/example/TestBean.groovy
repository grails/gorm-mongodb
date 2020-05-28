package example

import org.springframework.beans.factory.annotation.Autowired

class TestBean {

    @Autowired
    BookService bookRepo

    void doSomething() {
        assert bookRepo != null
        bookRepo.get(1l)
    }

}
