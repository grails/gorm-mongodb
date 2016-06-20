package org.grails.datastore.rx.bson.codecs

import groovy.transform.CompileStatic
import org.bson.BsonReader
import org.bson.codecs.DecoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.bson.codecs.BsonPersistentEntityCodec
import org.grails.datastore.bson.codecs.PropertyDecoder
import org.grails.datastore.bson.codecs.PropertyEncoder
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.*
import org.grails.datastore.rx.bson.CodecsRxDatastoreClient
import org.grails.datastore.rx.bson.codecs.decoders.IdentityDecoder
import org.grails.datastore.rx.bson.codecs.decoders.OneToManyDecoder
import org.grails.datastore.rx.bson.codecs.decoders.ToOneDecoder
import org.grails.datastore.rx.bson.codecs.encoders.OneToManyEncoder
import org.grails.datastore.rx.query.QueryState
import org.grails.gorm.rx.api.RxGormEnhancer

/**
 * Overrides the default PersistentEntity codecs for associations with reactive implementation for RxGORM
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class RxBsonPersistentEntityCodec extends BsonPersistentEntityCodec {

    private static final Map<Class, PropertyEncoder> RX_ENCODERS = [:]
    private static final Map<Class, PropertyDecoder> RX_DECODERS = [:]

    static {
        RX_ENCODERS[OneToMany] = new OneToManyEncoder()
        RX_DECODERS[OneToMany] = new OneToManyDecoder()
        RX_ENCODERS[ManyToMany] = new OneToManyEncoder()
        RX_DECODERS[ManyToMany] = new OneToManyDecoder()
        RX_ENCODERS[Embedded] = new EmbeddedEncoder()
        RX_DECODERS[Embedded] = new EmbeddedDecoder()
        RX_ENCODERS[EmbeddedCollection] = new EmbeddedCollectionEncoder()
        RX_DECODERS[EmbeddedCollection] = new EmbeddedCollectionDecoder()
        RX_DECODERS[OneToOne] = new ToOneDecoder()
        RX_DECODERS[ManyToOne] = new ToOneDecoder()
    }

    protected final QueryState queryState
    private final Map<Class, PropertyDecoder> localDecoders = [:]


    RxBsonPersistentEntityCodec(PersistentEntity entity, CodecRegistry codecRegistry, QueryState queryState = null) {
        super(codecRegistry, entity, false)
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
    Object decode(BsonReader bsonReader, DecoderContext decoderContext) {
        def decoded = super.decode(bsonReader, decoderContext)
        if(decoded instanceof DirtyCheckable) {
            ((DirtyCheckable)decoded).trackChanges()
        }
        return decoded
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
        protected RxBsonPersistentEntityCodec createEmbeddedEntityCodec(CodecRegistry codecRegistry, PersistentEntity associatedEntity) {
            return new RxBsonPersistentEntityCodec(associatedEntity, ((CodecsRxDatastoreClient)RxGormEnhancer.findStaticApi(associatedEntity.javaClass).getDatastoreClient()).getCodecRegistry())
        }
    }

    static class EmbeddedDecoder extends org.grails.datastore.bson.codecs.decoders.EmbeddedDecoder {
        @Override
        protected RxBsonPersistentEntityCodec createEmbeddedEntityCodec(CodecRegistry codecRegistry, PersistentEntity associatedEntity) {
            return new RxBsonPersistentEntityCodec(associatedEntity, ((CodecsRxDatastoreClient)RxGormEnhancer.findStaticApi(associatedEntity.javaClass).getDatastoreClient()).getCodecRegistry())
        }
    }

    static class EmbeddedCollectionEncoder extends org.grails.datastore.bson.codecs.encoders.EmbeddedCollectionEncoder {
        @Override
        protected RxBsonPersistentEntityCodec createEmbeddedEntityCodec(CodecRegistry codecRegistry, PersistentEntity associatedEntity) {
            return new RxBsonPersistentEntityCodec(associatedEntity, ((CodecsRxDatastoreClient)RxGormEnhancer.findStaticApi(associatedEntity.javaClass).getDatastoreClient()).getCodecRegistry())
        }
    }

    static class EmbeddedCollectionDecoder extends org.grails.datastore.bson.codecs.decoders.EmbeddedCollectionDecoder {
        @Override
        protected RxBsonPersistentEntityCodec createEmbeddedEntityCodec(CodecRegistry codecRegistry, PersistentEntity associatedEntity) {
            return new RxBsonPersistentEntityCodec(associatedEntity, ((CodecsRxDatastoreClient)RxGormEnhancer.findStaticApi(associatedEntity.javaClass).getDatastoreClient()).getCodecRegistry())
        }
    }


}

