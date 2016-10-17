import com.mongodb.MongoClient
import functional.tests.*
import org.grails.datastore.mapping.mongo.MongoDatastore
import org.springframework.beans.factory.annotation.Autowired

class BootStrap {

    @Autowired MongoDatastore mongoDatastore

    def init = { servletContext ->
    	mongoDatastore.mongoClient.getDatabase( mongoDatastore.defaultDatabase ).drop()
    }
    def destroy = {
    }
}
