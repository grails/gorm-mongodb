package org.grails.datastore.bson.codecs.decoders

import groovy.transform.CompileStatic
import org.bson.BsonReader
import org.bson.codecs.DecoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.bson.types.ObjectId
import org.grails.datastore.bson.codecs.PropertyDecoder
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.types.Identity


/**
 * A {@PropertyDecoder} capable of decoding the {@link org.grails.datastore.mapping.model.types.Identity}
 */
@CompileStatic
class IdentityDecoder implements PropertyDecoder<Identity> {

    @Override
    void decode(BsonReader bsonReader, Identity property, EntityAccess access, DecoderContext decoderContext, CodecRegistry codecRegistry) {
        switch(property.type) {
            case ObjectId:
                access.setIdentifierNoConversion( bsonReader.readObjectId() )
                break
            case Long:
                access.setIdentifierNoConversion( bsonReader.readInt64() )
                break
            case Integer:
                access.setIdentifierNoConversion( bsonReader.readInt32() )
                break
            default:
                access.setIdentifierNoConversion( bsonReader.readString())
        }

    }
}