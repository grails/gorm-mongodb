package org.grails.datastore.rx.bson.codecs.decoders

import groovy.transform.CompileStatic
import org.bson.BsonReader
import org.bson.BsonType
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.bson.codecs.BsonPersistentEntityCodec
import org.grails.datastore.bson.codecs.PropertyDecoder
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.dirty.checking.DirtyCheckingSupport
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.internal.MappingUtils
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ManyToMany
import org.grails.datastore.mapping.model.types.ToMany
import org.grails.datastore.mapping.reflect.EntityReflector
import org.grails.datastore.rx.collection.RxCollectionUtils
import org.grails.datastore.rx.collection.RxPersistentList
import org.grails.datastore.rx.collection.RxPersistentSet
import org.grails.datastore.rx.collection.RxPersistentSortedSet
import org.grails.datastore.rx.query.QueryState
import org.grails.gorm.rx.api.RxGormEnhancer

/**
 * A Decoder that decodes a one-to-many
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class OneToManyDecoder implements PropertyDecoder<ToMany> {
    final QueryState queryState

    OneToManyDecoder(QueryState queryState) {
        this.queryState = queryState
    }

    OneToManyDecoder() {
        this.queryState = null
    }

    protected Collection createConcreteCollection(Association association, Serializable foreignKey) {
        return RxCollectionUtils.createConcreteCollection(association, foreignKey, queryState)
    }

    @Override
    void decode(BsonReader reader, ToMany property, EntityAccess entityAccess, DecoderContext decoderContext, CodecRegistry codecRegistry) {
        if(property.isBidirectional() && !(property instanceof ManyToMany)) {
            BsonType bsonType = reader.currentBsonType
            def parent = entityAccess.entity
            if(bsonType == BsonType.ARRAY) {
                reader.readStartArray()
                Collection allDecoded = MappingUtils.createConcreteCollection(property.type)

                PersistentEntity associatedEntity = property.getAssociatedEntity()
                Codec codec = codecRegistry.get(associatedEntity.javaClass)
                EntityReflector associationReflector = associatedEntity.reflector
                Association inverseSide = property.inverseSide
                while (bsonType != BsonType.END_OF_DOCUMENT) {
                    def decoded = codec.decode(reader, BsonPersistentEntityCodec.DEFAULT_DECODER_CONTEXT)

                    if(inverseSide != null) {
                        associationReflector.setProperty(decoded, inverseSide.name, parent)
                    }
                    allDecoded.add(decoded)
                    bsonType = reader.readBsonType()
                }

                reader.readEndArray()
                entityAccess.setPropertyNoConversion(property.name, DirtyCheckingSupport.wrap(allDecoded, (DirtyCheckable) parent, property.name))
            }
            else {
                reader.skipValue()
                def foreignKey = (Serializable) entityAccess.getIdentifier()
                entityAccess.setPropertyNoConversion(property.name, createConcreteCollection(property, foreignKey))
            }
        }
        else {
            def type = property.type
            def propertyName = property.name

            def listCodec = codecRegistry.get(List)
            def identifiers = listCodec.decode(reader, decoderContext)
            def associatedType = property.associatedEntity.javaClass
            def datastoreClient = RxGormEnhancer.findStaticApi(associatedType).datastoreClient
            if(SortedSet.isAssignableFrom(type)) {
                entityAccess.setPropertyNoConversion(
                        propertyName,
                        new RxPersistentSortedSet( datastoreClient, property, identifiers, queryState)
                )
            }
            else if(Set.isAssignableFrom(type)) {
                entityAccess.setPropertyNoConversion(
                        propertyName,
                        new RxPersistentSet( datastoreClient, property, identifiers, queryState)
                )
            }
            else {
                entityAccess.setPropertyNoConversion(
                        propertyName,
                        new RxPersistentList( datastoreClient, property, identifiers, queryState )
                )
            }
        }
    }
}
