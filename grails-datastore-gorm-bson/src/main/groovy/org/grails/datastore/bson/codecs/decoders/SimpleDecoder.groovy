package org.grails.datastore.bson.codecs.decoders

import groovy.transform.CompileStatic
import org.bson.BsonReader
import org.bson.codecs.DecoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.bson.types.Binary
import org.bson.types.ObjectId
import org.grails.datastore.bson.codecs.PropertyDecoder
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.types.Simple

/**
 * A {@PropertyDecoder} capable of decoding {@link org.grails.datastore.mapping.model.types.Simple} properties
 */
@CompileStatic
class SimpleDecoder implements PropertyDecoder<Simple> {
    public static final Map<Class, TypeDecoder> SIMPLE_TYPE_DECODERS
    public static final TypeDecoder DEFAULT_DECODER = new TypeDecoder() {
        @Override
        void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
            entityAccess.setProperty( property.name, reader.readString())
        }
    }
    static interface TypeDecoder {
        void decode(BsonReader reader, Simple property, EntityAccess entityAccess)
    }

    static {
        SIMPLE_TYPE_DECODERS = new HashMap<Class, TypeDecoder>().withDefault { Class ->
            DEFAULT_DECODER
        }

        def convertingIntReader = new TypeDecoder() {
            @Override
            void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                entityAccess.setProperty( property.name, reader.readInt32() )
            }
        }
        SIMPLE_TYPE_DECODERS[Short] = convertingIntReader
        SIMPLE_TYPE_DECODERS[short.class] = convertingIntReader
        SIMPLE_TYPE_DECODERS[Byte] = convertingIntReader
        SIMPLE_TYPE_DECODERS[byte.class] = convertingIntReader
        def intDecoder = new TypeDecoder() {
            @Override
            void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                entityAccess.setPropertyNoConversion( property.name, reader.readInt32() )
            }
        }
        SIMPLE_TYPE_DECODERS[Integer] = intDecoder
        SIMPLE_TYPE_DECODERS[int.class] = intDecoder
        def longDecoder = new TypeDecoder() {
            @Override
            void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                entityAccess.setPropertyNoConversion( property.name, reader.readInt64() )
            }
        }
        SIMPLE_TYPE_DECODERS[Long] = longDecoder
        SIMPLE_TYPE_DECODERS[long.class] = longDecoder
        def doubleDecoder = new TypeDecoder() {
            @Override
            void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                entityAccess.setPropertyNoConversion( property.name, reader.readDouble() )
            }
        }
        SIMPLE_TYPE_DECODERS[Double] = doubleDecoder
        SIMPLE_TYPE_DECODERS[double.class] = doubleDecoder
        def booleanDecoder = new TypeDecoder() {
            @Override
            void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                entityAccess.setPropertyNoConversion( property.name, reader.readBoolean() )
            }
        }
        SIMPLE_TYPE_DECODERS[Boolean] = booleanDecoder
        SIMPLE_TYPE_DECODERS[boolean.class] = booleanDecoder

        SIMPLE_TYPE_DECODERS[([] as byte[]).getClass()] = new TypeDecoder() {
            @Override
            void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                def binary = reader.readBinaryData()
                entityAccess.setPropertyNoConversion( property.name, binary.data )
            }
        }
        SIMPLE_TYPE_DECODERS[Date] = new TypeDecoder() {
            @Override
            void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                def time = reader.readDateTime()
                entityAccess.setPropertyNoConversion( property.name, new Date(time))
            }
        }
        SIMPLE_TYPE_DECODERS[Calendar] = new TypeDecoder() {
            @Override
            void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {
                def time = reader.readDateTime()
                def calendar = new GregorianCalendar()
                calendar.setTimeInMillis(time)
                entityAccess.setPropertyNoConversion( property.name, calendar)
            }
        }

        SIMPLE_TYPE_DECODERS[Binary] = new TypeDecoder() {
            @Override
            void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {

                entityAccess.setPropertyNoConversion(
                        property.name,
                        new Binary(reader.readBinaryData().data)
                )
            }
        }
        SIMPLE_TYPE_DECODERS[ObjectId] = new TypeDecoder() {
            @Override
            void decode(BsonReader reader, Simple property, EntityAccess entityAccess) {

                entityAccess.setPropertyNoConversion(
                        property.name,
                        reader.readObjectId()
                )
            }
        }
    }

    @Override
    void decode(BsonReader reader, Simple property, EntityAccess entityAccess, DecoderContext decoderContext, CodecRegistry codecRegistry) {
        def type = property.type
        def decoder = SIMPLE_TYPE_DECODERS[type]
        if(type.isArray()) {
            if(!decoder.is(DEFAULT_DECODER)) {
                decoder.decode reader, property, entityAccess
            }
            else {
                def arrayDecoder = codecRegistry.get(List)
                def bsonArray = arrayDecoder.decode(reader, decoderContext)
                entityAccess.setProperty(property.name, bsonArray)
            }
        }
        else {
            decoder.decode reader, property, entityAccess
        }
    }
}
