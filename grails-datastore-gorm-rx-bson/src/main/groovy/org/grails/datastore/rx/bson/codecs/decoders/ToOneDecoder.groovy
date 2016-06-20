package org.grails.datastore.rx.bson.codecs.decoders

import groovy.transform.CompileStatic
import org.bson.BsonReader
import org.bson.BsonType
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.bson.types.ObjectId
import org.grails.datastore.bson.codecs.PropertyDecoder
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.types.ToOne
import org.grails.datastore.rx.internal.RxDatastoreClientImplementor
import org.grails.datastore.rx.query.QueryState
import org.grails.gorm.rx.api.RxGormEnhancer

/**
 * A ToOne decoder that creates proxies
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class ToOneDecoder implements PropertyDecoder<ToOne> {

    private static final Map<BsonType, AssociationIdDecoder> DECODERS = [:]

    static {
        DECODERS.put(BsonType.INT32, new AssociationIdDecoder() {
            @Override
            BsonType bsonType() {
                BsonType.INT32
            }

            @Override
            Serializable decode(BsonReader bsonReader) {
                return Integer.valueOf( bsonReader.readInt32() )
            }
        })
        DECODERS.put(BsonType.INT64, new AssociationIdDecoder() {
            @Override
            BsonType bsonType() {
                BsonType.INT64
            }

            @Override
            Serializable decode(BsonReader bsonReader) {
                return Long.valueOf( bsonReader.readInt64() )
            }
        })
        DECODERS.put(BsonType.OBJECT_ID, new AssociationIdDecoder() {
            @Override
            BsonType bsonType() {
                BsonType.OBJECT_ID
            }

            @Override
            Serializable decode(BsonReader bsonReader) {
                return bsonReader.readObjectId()
            }
        })
        DECODERS.put(BsonType.STRING, new AssociationIdDecoder() {
            @Override
            BsonType bsonType() {
                BsonType.STRING
            }

            @Override
            Serializable decode(BsonReader bsonReader) {
                return bsonReader.readString()
            }
        })
    }

    final QueryState queryState

    ToOneDecoder(QueryState queryState) {
        this.queryState = queryState
    }

    ToOneDecoder() {
        this.queryState = null
    }

    @Override
    void decode(BsonReader bsonReader, ToOne property, EntityAccess entityAccess, DecoderContext decoderContext, CodecRegistry codecRegistry) {
        def associatedEntity = property.associatedEntity
        BsonType bsonType = bsonReader.currentBsonType
        if(bsonType  == BsonType.DOCUMENT) {
            // a document is loaded entity so decode it
            Codec codec = codecRegistry.get(associatedEntity.javaClass)
            def decoded = codec.decode(bsonReader, decoderContext)
            entityAccess.setPropertyNoConversion(property.name, decoded)
        }
        else {

            AssociationIdDecoder decoder = DECODERS.get(bsonType)
            Serializable associationId = decoder?.decode(bsonReader)
            if(associationId != null) {
                def associationType = associatedEntity.javaClass
                def loadedEntity = queryState?.getLoadedEntity(associationType, associationId)
                if(loadedEntity != null) {
                    entityAccess.setPropertyNoConversion(property.name, loadedEntity)
                }
                else {
                    RxDatastoreClientImplementor datastoreClient = (RxDatastoreClientImplementor)RxGormEnhancer.findStaticApi(associationType).datastoreClient
                    entityAccess.setPropertyNoConversion(property.name, datastoreClient.proxy(associationType, associationId, queryState))
                }
            }
        }
    }

    static interface AssociationIdDecoder {
        BsonType bsonType()

        Serializable decode(BsonReader bsonReader)
    }
}
