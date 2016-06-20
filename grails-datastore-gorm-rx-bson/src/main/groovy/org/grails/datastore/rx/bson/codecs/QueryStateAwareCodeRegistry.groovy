package org.grails.datastore.rx.bson.codecs

import groovy.transform.CompileStatic
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.rx.bson.CodecsRxDatastoreClient
import org.grails.datastore.rx.query.QueryState
/**
 * A {@link CodecRegistry} that maintains a query state of loaded entities to avoid repeated loading of entities in association graphs
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class QueryStateAwareCodeRegistry implements CodecRegistry {

    final CodecRegistry parent
    final QueryState queryState
    final CodecsRxDatastoreClient datastoreClient

    QueryStateAwareCodeRegistry(CodecRegistry parent, QueryState queryState, CodecsRxDatastoreClient datastoreClient) {
        this.parent = parent
        this.queryState = queryState
        this.datastoreClient = datastoreClient
    }

    @Override
    def <T> Codec<T> get(Class<T> aClass) {

        def entity = datastoreClient.getMappingContext().getPersistentEntity(aClass.name)
        if(entity != null) {
            return createEntityCodec(entity)
        }
        else {
            return parent.get(aClass)
        }
    }

    protected RxBsonPersistentEntityCodec createEntityCodec(PersistentEntity entity) {
        new RxBsonPersistentEntityCodec(entity, datastoreClient.codecRegistry, queryState)
    }

}
