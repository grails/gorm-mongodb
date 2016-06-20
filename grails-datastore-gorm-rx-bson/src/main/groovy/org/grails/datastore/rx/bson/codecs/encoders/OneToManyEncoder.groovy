package org.grails.datastore.rx.bson.codecs.encoders

import groovy.transform.CompileStatic
import org.bson.BsonWriter
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.grails.datastore.bson.codecs.PropertyEncoder
import org.grails.datastore.mapping.dirty.checking.DirtyCheckableCollection
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.internal.MappingUtils
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.ManyToMany
import org.grails.datastore.mapping.model.types.OneToMany
import org.grails.datastore.mapping.reflect.FieldEntityAccess

/**
 * An encoder that encodes the identifiers directly in the document for a unidirectional association
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class OneToManyEncoder implements PropertyEncoder<OneToMany> {

    @Override
    void encode(BsonWriter writer, OneToMany property, Object value, EntityAccess parentAccess, EncoderContext encoderContext, CodecRegistry codecRegistry) {
        boolean shouldEncodeIds = !property.isBidirectional() || (property instanceof ManyToMany)
        if(shouldEncodeIds) {
            if(value instanceof Collection) {
                boolean updateCollection = false
                if((value instanceof DirtyCheckableCollection)) {
                    def persistentCollection = (DirtyCheckableCollection) value
                    updateCollection = persistentCollection.hasChanged()
                }
                else {
                    // write new collection
                    updateCollection = true
                }

                if(updateCollection) {
                    // update existing collection
                    Collection identifiers
                    def entityReflector = FieldEntityAccess.getOrIntializeReflector(property.associatedEntity)
                    identifiers = ((Collection)value).collect() {
                        entityReflector.getIdentifier(it)
                    }.findAll() { it != null }

                    writer.writeName MappingUtils.getTargetKey((PersistentProperty)property)
                    def listCodec = codecRegistry.get(List)

                    def identifierList = identifiers.toList()
                    listCodec.encode writer, identifierList, encoderContext
                }
            }
        }
    }
}