package org.grails.datastore.rx.mongodb.engine.codecs

import groovy.transform.CompileStatic
import org.bson.BsonReader
import org.bson.codecs.DecoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.bson.codecs.PropertyDecoder
import org.grails.datastore.bson.codecs.PropertyEncoder
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.*
import org.grails.datastore.mapping.mongo.engine.codecs.PersistentEntityCodec
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.datastore.rx.bson.CodecsRxDatastoreClient
import org.grails.datastore.rx.bson.codecs.decoders.IdentityDecoder
import org.grails.datastore.rx.collection.RxPersistentList
import org.grails.datastore.rx.collection.RxPersistentSet
import org.grails.datastore.rx.collection.RxPersistentSortedSet
import org.grails.datastore.rx.query.QueryState
import org.grails.gorm.rx.api.RxGormEnhancer

import javax.persistence.FetchType

/**
 * Overrides the default PersistentEntity codecs for associations with reactive implementation for RxGORM
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class RxPersistentEntityCodec extends PersistentEntityCodec {

    private static final Map<Class, PropertyEncoder> RX_ENCODERS = [:]
    private static final Map<Class, PropertyDecoder> RX_DECODERS = [:]

    static {
        RX_ENCODERS[OneToMany] = new org.grails.datastore.rx.bson.codecs.encoders.OneToManyEncoder()
        RX_DECODERS[OneToMany] = new org.grails.datastore.rx.bson.codecs.decoders.OneToManyDecoder()
        RX_ENCODERS[ManyToMany] = new org.grails.datastore.rx.bson.codecs.encoders.OneToManyEncoder()
        RX_DECODERS[ManyToMany] = new org.grails.datastore.rx.bson.codecs.decoders.OneToManyDecoder()
        RX_ENCODERS[Embedded] = new EmbeddedEncoder()
        RX_DECODERS[Embedded] = new EmbeddedDecoder()
        RX_ENCODERS[EmbeddedCollection] = new EmbeddedCollectionEncoder()
        RX_DECODERS[EmbeddedCollection] = new EmbeddedCollectionDecoder()
        RX_DECODERS[OneToOne] = new org.grails.datastore.rx.bson.codecs.decoders.ToOneDecoder()
        RX_DECODERS[ManyToOne] = new org.grails.datastore.rx.bson.codecs.decoders.ToOneDecoder()
    }

    final RxDatastoreClient datastoreClient


    private final Map<Class, PropertyDecoder> localDecoders = [:]
    private final QueryState queryState


    RxPersistentEntityCodec(PersistentEntity entity, CodecsRxDatastoreClient datastoreClient, QueryState queryState = null) {
        super(datastoreClient.codecRegistry, entity, false)
        this.datastoreClient = datastoreClient
        this.queryState = queryState
        if(queryState != null) {
            def toOneDecoder = new org.grails.datastore.rx.bson.codecs.decoders.ToOneDecoder(queryState)
            localDecoders.put(OneToOne, toOneDecoder)
            localDecoders.put(ManyToOne, toOneDecoder)
            localDecoders.put(Identity, new IdentityDecoder(queryState))
            def oneToManyDecoder = new org.grails.datastore.rx.bson.codecs.decoders.OneToManyDecoder(queryState)
            localDecoders.put OneToMany, oneToManyDecoder
            localDecoders.put ManyToMany, oneToManyDecoder
        }
    }

    @Override
    protected Object retrieveCachedInstance(EntityAccess access) {
        return null
    }

    @Override
    Object decode(BsonReader bsonReader, DecoderContext decoderContext) {
        def decoded = super.decode(bsonReader, decoderContext)
        if(decoded instanceof DirtyCheckable) {
            ((DirtyCheckable)decoded).trackChanges()
        }
        return decoded
    }

    @Override
    protected void readingComplete(EntityAccess access) {
        decodeAssociations(null, access)
    }

    @Override
    protected void decodeAssociations(Session mongoSession, EntityAccess access) {
        // session argument will be null, so we read from client

        PersistentEntity persistentEntity = access.persistentEntity
        for(association in persistentEntity.associations) {
            if(association.isBidirectional()) {
                // create inverse lookup collection
                if(association instanceof ToMany) {
                    def foreignKey = (Serializable) access.getIdentifier()

                    access.setPropertyNoConversion(association.name, createConcreteCollection(association, foreignKey))
                }
                else if (association instanceof OneToOne) {
                    if (((ToOne) association).isForeignKeyInChild()) {
                        def associatedClass = association.associatedEntity.javaClass
                        boolean lazy = association.mapping.mappedForm.fetchStrategy == FetchType.LAZY
                        if(lazy) {

                            def proxy = datastoreClient.proxy(
                                    datastoreClient.createQuery(associatedClass)
                                            .eq(association.inverseSide.name, access.identifier)
                            )
                            access.setPropertyNoConversion(association.name, proxy)
                        }
                    }
                }
            }
        }
    }

    protected Collection createConcreteCollection(Association association, Serializable foreignKey) {
        switch(association.type) {
            case SortedSet:
                return new RxPersistentSortedSet(datastoreClient, association, foreignKey, queryState)
            case List:
                return new RxPersistentList(datastoreClient, association, foreignKey, queryState)
            default:
                return new RxPersistentSet(datastoreClient, association, foreignKey, queryState)
        }
    }

    @Override
    protected <T extends PersistentProperty> PropertyEncoder<T> getPropertyEncoder(Class<T> type) {
        return RX_ENCODERS.get(type) ?: super.getPropertyEncoder(type)
    }

    @Override
    protected <T extends PersistentProperty> PropertyDecoder<T> getPropertyDecoder(Class<T> type) {
        return localDecoders.get(type) ?: RX_DECODERS.get(type) ?: super.getPropertyDecoder(type)
    }

    static class EmbeddedEncoder extends org.grails.datastore.bson.codecs.encoders.EmbeddedEncoder {
        @Override
        protected PersistentEntityCodec createEmbeddedEntityCodec(CodecRegistry codecRegistry, PersistentEntity associatedEntity) {
            return new RxPersistentEntityCodec(associatedEntity, (CodecsRxDatastoreClient)RxGormEnhancer.findStaticApi(associatedEntity.javaClass).getDatastoreClient())
        }
    }

    static class EmbeddedDecoder extends org.grails.datastore.bson.codecs.decoders.EmbeddedDecoder {
        @Override
        protected PersistentEntityCodec createEmbeddedEntityCodec(CodecRegistry codecRegistry, PersistentEntity associatedEntity) {
            return new RxPersistentEntityCodec(associatedEntity, (CodecsRxDatastoreClient)RxGormEnhancer.findStaticApi(associatedEntity.javaClass).getDatastoreClient())
        }
    }

    static class EmbeddedCollectionEncoder extends org.grails.datastore.bson.codecs.encoders.EmbeddedCollectionEncoder {
        @Override
        protected PersistentEntityCodec createEmbeddedEntityCodec(CodecRegistry codecRegistry, PersistentEntity associatedEntity) {
            return new RxPersistentEntityCodec(associatedEntity, (CodecsRxDatastoreClient)RxGormEnhancer.findStaticApi(associatedEntity.javaClass).getDatastoreClient())
        }
    }

    static class EmbeddedCollectionDecoder extends org.grails.datastore.bson.codecs.decoders.EmbeddedCollectionDecoder {
        @Override
        protected PersistentEntityCodec createEmbeddedEntityCodec(CodecRegistry codecRegistry, PersistentEntity associatedEntity) {
            return new RxPersistentEntityCodec(associatedEntity, (CodecsRxDatastoreClient)RxGormEnhancer.findStaticApi(associatedEntity.javaClass).getDatastoreClient())
        }
    }


}
