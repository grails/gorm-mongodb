package org.grails.datastore.rx.bson.codecs.decoders

import groovy.transform.CompileStatic
import org.bson.BsonReader
import org.bson.codecs.DecoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.types.Identity
import org.grails.datastore.rx.query.QueryState

/**
 * A decoder that adds a loaded entity to the query state
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class IdentityDecoder extends org.grails.datastore.bson.codecs.decoders.IdentityDecoder {
    final QueryState queryState

    IdentityDecoder(QueryState queryState) {
        this.queryState = queryState
    }

    @Override
    void decode(BsonReader bsonReader, Identity property, EntityAccess access, DecoderContext decoderContext, CodecRegistry codecRegistry) {
        super.decode(bsonReader, property, access, decoderContext, codecRegistry)
        def identifier = access.identifier
        if(identifier != null) {
            queryState.addLoadedEntity(access.persistentEntity.javaClass,(Serializable) identifier, access.entity)
        }
    }
}