package functional.tests

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import de.bwaldvogel.mongo.MongoServer
import de.bwaldvogel.mongo.backend.memory.MemoryBackend
import groovy.transform.CompileStatic

@CompileStatic
trait EmbeddedMongoClient {

    MongoClient createMongoClient() {
        MongoServer server = new MongoServer(new MemoryBackend())
        // bind on a random local port
        InetSocketAddress serverAddress = server.bind()
        return MongoClients.create("mongodb://" + serverAddress.getHostName() + ":" + serverAddress.getPort())
    }
}
