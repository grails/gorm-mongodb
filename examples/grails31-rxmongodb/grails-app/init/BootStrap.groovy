import functional.tests.*

class BootStrap {

    def init = { servletContext ->
    	Book.DB.drop().toBlocking().first()
    }
    def destroy = {
    }
}
