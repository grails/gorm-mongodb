package org.grails.datastore.bson.codecs.decoders

import groovy.transform.CompileStatic
import org.bson.BsonReader
import org.bson.codecs.DecoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.bson.codecs.BsonPersistentEntityCodec
import org.grails.datastore.bson.codecs.PropertyDecoder
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Embedded

/**
 * A {@PropertyDecoder} capable of decoding {@Embedded} association types
 */
@CompileStatic
class EmbeddedDecoder implements PropertyDecoder<Embedded> {

    @Override
    void decode(BsonReader reader, Embedded property, EntityAccess entityAccess, DecoderContext decoderContext, CodecRegistry codecRegistry) {
        def associatedEntity = property.associatedEntity
        BsonPersistentEntityCodec codec = createEmbeddedEntityCodec(codecRegistry, associatedEntity)

        def decoded = codec.decode(reader, decoderContext)
        if(decoded instanceof DirtyCheckable) {
            decoded.trackChanges()
        }
        entityAccess.setPropertyNoConversion(
                property.name,
                decoded
        )

    }

    protected BsonPersistentEntityCodec createEmbeddedEntityCodec(CodecRegistry codecRegistry, PersistentEntity associatedEntity) {
        new BsonPersistentEntityCodec(codecRegistry, associatedEntity)
    }
}