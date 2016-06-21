package org.grails.datastore.rx.bson.codecs

import groovy.transform.CompileStatic
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.mapping.model.MappingContext
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
class QueryStateAwareCodecRegistry implements CodecRegistry {

    final CodecRegistry parent
    final QueryState queryState
    final MappingContext mappingContext

    QueryStateAwareCodecRegistry(CodecRegistry parent, QueryState queryState, MappingContext mappingContext) {
        this.parent = parent
        this.queryState = queryState
        this.mappingContext = mappingContext
    }

    @Override
    def <T> Codec<T> get(Class<T> aClass) {

        def entity = mappingContext.getPersistentEntity(aClass.name)
        if(entity != null) {
            return createEntityCodec(entity)
        }
        else {
            return parent.get(aClass)
        }
    }

    protected RxBsonPersistentEntityCodec createEntityCodec(PersistentEntity entity) {
        new RxBsonPersistentEntityCodec(entity, parent, queryState)
    }

}
