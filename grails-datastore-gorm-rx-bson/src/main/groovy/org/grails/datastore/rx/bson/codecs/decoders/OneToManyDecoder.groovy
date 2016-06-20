package org.grails.datastore.rx.bson.codecs.decoders

import groovy.transform.CompileStatic
import org.bson.BsonReader
import org.bson.codecs.DecoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.bson.codecs.PropertyDecoder
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ManyToMany
import org.grails.datastore.mapping.model.types.ToMany
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
        switch(association.type) {
            case SortedSet:
                return new RxPersistentSortedSet(RxGormEnhancer.findInstanceApi(association.associatedEntity.javaClass).datastoreClient, association, foreignKey, queryState)
            case List:
                return new RxPersistentList(RxGormEnhancer.findInstanceApi(association.associatedEntity.javaClass).datastoreClient, association, foreignKey, queryState)
            default:
                return new RxPersistentSet(RxGormEnhancer.findInstanceApi(association.associatedEntity.javaClass).datastoreClient, association, foreignKey, queryState)
        }
    }

    @Override
    void decode(BsonReader reader, ToMany property, EntityAccess entityAccess, DecoderContext decoderContext, CodecRegistry codecRegistry) {
        if(property.isBidirectional() && !(property instanceof ManyToMany)) {
            def foreignKey = (Serializable) entityAccess.getIdentifier()
            entityAccess.setPropertyNoConversion(property.name, createConcreteCollection(property, foreignKey))
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
