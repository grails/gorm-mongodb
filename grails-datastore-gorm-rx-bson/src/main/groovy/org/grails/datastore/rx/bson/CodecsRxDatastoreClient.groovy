package org.grails.datastore.rx.bson

import org.bson.codecs.configuration.CodecProvider
import org.grails.datastore.bson.codecs.CodecRegistryProvider
import org.grails.datastore.rx.RxDatastoreClient

/**
 * @author Graeme Rocher
 * @since 6.0
 */
interface CodecsRxDatastoreClient<T> extends RxDatastoreClient<T>, CodecRegistryProvider, CodecProvider {

}