/* Copyright (C) 2010 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grails.datastore.mapping.mongo;


import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.mongodb.*;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.grails.datastore.gorm.GormEnhancer;
import org.grails.datastore.gorm.events.AutoTimestampEventListener;
import org.grails.datastore.gorm.events.ConfigurableApplicationEventPublisher;
import org.grails.datastore.gorm.events.DefaultApplicationEventPublisher;
import org.grails.datastore.gorm.events.DomainEventListener;
import org.grails.datastore.gorm.mongo.MongoGormEnhancer;
import org.grails.datastore.gorm.mongo.bean.factory.MongoClientFactoryBean;
import org.grails.datastore.gorm.validation.constraints.MappingContextAwareConstraintFactory;
import org.grails.datastore.gorm.validation.constraints.builtin.UniqueConstraint;
import org.grails.datastore.gorm.validation.constraints.registry.DefaultValidatorRegistry;
import org.grails.datastore.mapping.core.*;
import org.grails.datastore.mapping.document.config.DocumentMappingContext;
import org.grails.datastore.mapping.model.*;
import org.grails.datastore.mapping.mongo.config.MongoAttribute;
import org.grails.datastore.mapping.mongo.config.MongoClientOptionsBuilder;
import org.grails.datastore.mapping.mongo.config.MongoCollection;
import org.grails.datastore.mapping.mongo.config.MongoMappingContext;
import org.grails.datastore.bson.codecs.CodecExtensions;
import org.grails.datastore.mapping.mongo.engine.codecs.PersistentEntityCodec;
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.transaction.PlatformTransactionManager;

import javax.annotation.PreDestroy;

/**
 * A Datastore implementation for the Mongo document store.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class MongoDatastore extends AbstractDatastore implements MappingContext.Listener, Closeable, StatelessDatastore {

    public static final String SETTING_DATABASE_NAME = MongoConstants.SETTING_DATABASE_NAME;
    public static final String SETTING_CONNECTION_STRING = MongoConstants.SETTING_CONNECTION_STRING;
    public static final String SETTING_URL = MongoConstants.SETTING_URL;
    public static final String SETTING_DEFAULT_MAPPING = MongoConstants.SETTING_DEFAULT_MAPPING;
    public static final String SETTING_OPTIONS = MongoConstants.SETTING_OPTIONS;
    public static final String SETTING_HOST = MongoConstants.SETTING_HOST;
    public static final String SETTING_PORT = MongoConstants.SETTING_PORT;
    public static final String SETTING_USERNAME = MongoConstants.SETTING_USERNAME;
    public static final String SETTING_PASSWORD = MongoConstants.SETTING_PASSWORD;
    public static final String SETTING_STATELESS = "grails.mongodb.stateless";
    public static final String SETTING_ENGINE = "grails.mongodb.engine";
    public static final String INDEX_ATTRIBUTES = "indexAttributes";
    public static final String CODEC_ENGINE = "codec";

    protected final MongoClient mongo;
    protected final String defaultDatabase;
    protected final Map<PersistentEntity, String> mongoCollections = new ConcurrentHashMap<PersistentEntity, String>();
    protected final Map<PersistentEntity, String> mongoDatabases = new ConcurrentHashMap<PersistentEntity, String>();
    protected boolean stateless = false;
    protected boolean codecEngine = true;
    protected CodecRegistry codecRegistry;
    protected final ApplicationEventPublisher eventPublisher;
    protected final PlatformTransactionManager transactionManager;
    protected final GormEnhancer gormEnhancer;
    /**
     * Configures a new {@link MongoDatastore} for the given arguments
     *
     * @param mongoClient The {@link MongoClient} instance
     * @param eventPublisher The Spring ApplicationContext
     * @param mappingContext The mapping context
     */
    public MongoDatastore(MongoClient mongoClient, PropertyResolver configuration, MongoMappingContext mappingContext, ConfigurableApplicationEventPublisher eventPublisher) {
        super(mappingContext, configuration, null);
        this.mongo = mongoClient;
        this.eventPublisher = eventPublisher;
        this.defaultDatabase = mappingContext.getDefaultDatabaseName();
        if (mappingContext != null) {
            mappingContext.addMappingContextListener(this);
        }

        this.stateless = connectionDetails.getProperty(SETTING_STATELESS, Boolean.class, false);
        this.codecEngine = connectionDetails.getProperty(SETTING_ENGINE, String.class, CODEC_ENGINE).equals(CODEC_ENGINE);

        initializeConverters(mappingContext);

        final ConverterRegistry converterRegistry = mappingContext.getConverterRegistry();
        converterRegistry.addConverter(new Converter<String, ObjectId>() {
            public ObjectId convert(String source) {
                return new ObjectId(source);
            }
        });

        converterRegistry.addConverter(new Converter<ObjectId, String>() {
            public String convert(ObjectId source) {
                return source.toString();
            }
        });

        converterRegistry.addConverter(new Converter<byte[], Binary>() {
            public Binary convert(byte[] source) {
                return new Binary(source);
            }
        });

        converterRegistry.addConverter(new Converter<Binary, byte[]>() {
            public byte[] convert(Binary source) {
                return source.getData();
            }
        });

        for (Converter converter : CodecExtensions.getBsonConverters()) {
            converterRegistry.addConverter(converter);
        }

        codecRegistry = CodecRegistries.fromRegistries(
                MongoClient.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(new CodecExtensions(), new PersistentEntityCodeRegistry())
        );

        DatastoreTransactionManager datastoreTransactionManager = new DatastoreTransactionManager();
        datastoreTransactionManager.setDatastore(this);
        transactionManager = datastoreTransactionManager;

        gormEnhancer = new MongoGormEnhancer(this, transactionManager);

        registerEventListeners(eventPublisher);

        mappingContext.addMappingContextListener(new MappingContext.Listener() {
            @Override
            public void persistentEntityAdded(PersistentEntity entity) {
                gormEnhancer.registerEntity(entity);
            }
        });

        buildIndex();

    }

    public void buildIndex() {
        for (PersistentEntity entity : this.mappingContext.getPersistentEntities()) {
            // Only create Mongo templates for entities that are mapped with Mongo
            if (!entity.isExternal()) {
                initializeIndices(entity);
            }
        }
    }


    /**
     * Configures a new {@link MongoDatastore} for the given arguments
     *
     * @param mongoClient The {@link MongoClient} instance
     * @param eventPublisher The Spring ApplicationContext
     * @param classes The persistent classes
     */
    public MongoDatastore(MongoClient mongoClient, PropertyResolver configuration, ConfigurableApplicationEventPublisher eventPublisher, Class...classes) {
        this(mongoClient, configuration, createMappingContext(configuration, classes), eventPublisher);
    }

    /**
     * Configures a new {@link MongoDatastore} for the given arguments
     *
     * @param mongoClient The {@link MongoClient} instance
     * @param classes The persistent classes
     */
    public MongoDatastore(MongoClient mongoClient, PropertyResolver configuration, Class...classes) {
        this(mongoClient, configuration, createMappingContext(configuration, classes), new DefaultApplicationEventPublisher());
    }

    /**
     * Configures a new {@link MongoDatastore} for the given arguments
     *
     * @param mongoClient The {@link MongoClient} instance
     * @param classes The persistent classes
     */
    public MongoDatastore(MongoClient mongoClient, Class...classes) {
        this(mongoClient, createPropertyResolver(null), createMappingContext(createPropertyResolver(null), classes), new DefaultApplicationEventPublisher());
    }

    /**
     * Configures a new {@link MongoDatastore} for the given arguments
     *
     * @param clientOptions The {@link MongoClientOptions} instance
     * @param configuration The configuration
     * @param eventPublisher The Spring ApplicationContext
     * @param mappingContext The mapping context
     */
    public MongoDatastore(MongoClientOptions.Builder clientOptions, PropertyResolver configuration, MongoMappingContext mappingContext, ConfigurableApplicationEventPublisher eventPublisher) {
        this(createMongoClient(configuration, clientOptions, mappingContext),  configuration, mappingContext,  eventPublisher);
    }

    /**
     * Configures a new {@link MongoDatastore} for the given arguments
     *
     * @param clientOptions The {@link MongoClientOptions} instance
     * @param configuration The configuration
     * @param mappingContext The mapping context
     */
    public MongoDatastore(MongoClientOptions.Builder clientOptions, PropertyResolver configuration, MongoMappingContext mappingContext) {
        this(createMongoClient(configuration, clientOptions, mappingContext),  configuration, mappingContext,  new DefaultApplicationEventPublisher());
    }


    /**
     * Configures a new {@link MongoDatastore} for the given arguments
     *
     * @param configuration The configuration for the datastore
     * @param eventPublisher The Spring ApplicationContext
     * @param mappingContext The mapping context
     */
    public MongoDatastore(PropertyResolver configuration, MongoMappingContext mappingContext, ConfigurableApplicationEventPublisher eventPublisher) {
        this(createMongoClientOptions(configuration),  configuration, mappingContext,  eventPublisher);
    }

    /**
     * Configures a new {@link MongoDatastore} for the given arguments
     *
     * @param configuration The configuration for the datastore
     * @param mappingContext The mapping context
     */
    public MongoDatastore(PropertyResolver configuration, MongoMappingContext mappingContext) {
        this(configuration, mappingContext, new DefaultApplicationEventPublisher());
    }

    /**
     * Configures a new {@link MongoDatastore} for the given arguments
     *
     * @param configuration The configuration for the datastore
     * @param eventPublisher The Spring ApplicationContext
     * @param classes The persistent classes
     */
    public MongoDatastore(PropertyResolver configuration, ConfigurableApplicationEventPublisher eventPublisher, Class...classes) {
        this(configuration, createMappingContext(configuration, classes), eventPublisher);
    }

    /**
     * Configures a new {@link MongoDatastore} for the given arguments
     *
     * @param configuration The configuration
     * @param eventPublisher The event publisher
     * @param classes The persistent classes
     */
    public MongoDatastore(Map<String, Object> configuration, ConfigurableApplicationEventPublisher eventPublisher, Class...classes) {
        this(createPropertyResolver(configuration),eventPublisher, classes);
    }

    /**
     * Configures a new {@link MongoDatastore} for the given arguments
     *
     * @param configuration The configuration
     * @param classes The persistent classes
     */
    public MongoDatastore(Map<String, Object> configuration, Class...classes) {
        this(createPropertyResolver(configuration),new DefaultApplicationEventPublisher(), classes);
    }

    /**
     * Configures a new {@link MongoDatastore} for the given arguments
     *
     * @param configuration The configuration
     * @param mappingContext The {@link MongoMappingContext}
     */

    public MongoDatastore(Map<String, Object> configuration, MongoMappingContext mappingContext) {
        this(createPropertyResolver(configuration), mappingContext, new DefaultApplicationEventPublisher());
    }

    /**
     * Configures a new {@link MongoDatastore} for the given arguments
     *
     * @param mappingContext The {@link MongoMappingContext}
     */
    public MongoDatastore(MongoMappingContext mappingContext) {
        this(createPropertyResolver(null), mappingContext, new DefaultApplicationEventPublisher());
    }

    /**
     * Configures a new {@link MongoDatastore} for the given arguments
     *
     * @param classes The persistent classes
     */
    public MongoDatastore(Class...classes) {
        this(createMappingContext(createPropertyResolver(null), classes));
    }

    /**
     * Creates the {@link MongoClientOptions} instance from the given configuration
     *
     * @param configuration The configuration
     *
     * @return A {@link MongoClientOptions} instance
     */
    protected static MongoClientOptions.Builder createMongoClientOptions(PropertyResolver configuration) {
        MongoClientOptionsBuilder builder = new MongoClientOptionsBuilder(configuration, MongoMappingContext.getDefaultDatabaseName(configuration));
        MongoClientOptions.Builder optionsBuilder = builder.build();
        optionsBuilder.codecRegistry(
                CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(), new MongoClientFactoryBean.DefaultGrailsCodecRegistry())
        );

        return optionsBuilder;
    }

    protected static MongoClient createMongoClient(PropertyResolver configuration, MongoClientOptions.Builder mongoOptions, MongoMappingContext mappingContext) {
        String connectionString = configuration.getProperty(SETTING_CONNECTION_STRING, String.class,
                                    configuration.getProperty(SETTING_URL, String.class, null) );

        if(connectionString != null) {
            MongoClientURI mongoClientURI = new MongoClientURI(connectionString, mongoOptions);
            return new MongoClient(mongoClientURI);
        }
        else {

            ServerAddress defaults = new ServerAddress();
            String username = configuration.getProperty(SETTING_USERNAME, (String)null);
            String password = configuration.getProperty(SETTING_PASSWORD, (String)null);
            String databaseName = mappingContext.getDefaultDatabaseName();

            List<MongoCredential> credentials = new ArrayList<MongoCredential>();
            if (username != null && password != null) {
                credentials.add(MongoCredential.createCredential(username, databaseName, password.toCharArray()));
            }

            String host = configuration.getProperty(SETTING_HOST, defaults.getHost());
            int port = configuration.getProperty(SETTING_PORT, Integer.class, defaults.getPort());
            ServerAddress serverAddress = new ServerAddress(host,port);
            MongoClient mongo;
            if (mongoOptions != null) {
                mongo = new MongoClient(serverAddress, credentials, mongoOptions.build());
            } else {
                mongo = new MongoClient(serverAddress, credentials, mongoOptions.build());
            }
            return mongo;
        }
    }

    protected static MongoMappingContext createMappingContext(PropertyResolver configuration, Class[] classes) {
        MongoMappingContext mongoMappingContext = new MongoMappingContext(configuration,
                                                                            classes);

        DefaultValidatorRegistry defaultValidatorRegistry = new DefaultValidatorRegistry(mongoMappingContext, configuration);
        defaultValidatorRegistry.addConstraintFactory(
                new MappingContextAwareConstraintFactory(UniqueConstraint.class, defaultValidatorRegistry.getMessageSource(), mongoMappingContext)
        );
        mongoMappingContext.setValidatorRegistry(
                defaultValidatorRegistry
        );
        return mongoMappingContext;
    }

    protected static PropertyResolver createPropertyResolver(Map<String,Object> configuration) {
        StandardEnvironment env = new StandardEnvironment();
        if(configuration == null) {
            // create defaults
            Map defaultConfig = new LinkedHashMap();
            defaultConfig.put(SETTING_HOST, "mongodb://localhost");
            defaultConfig.put(SETTING_DATABASE_NAME, "test");
        }
        else {
            env.getPropertySources().addFirst(new MapPropertySource("mongodb", configuration));
        }
        return env;
    }

    protected void registerEventListeners(ConfigurableApplicationEventPublisher eventPublisher) {
        eventPublisher.addApplicationListener(new DomainEventListener(this));
        eventPublisher.addApplicationListener(new AutoTimestampEventListener(this));
    }

    public String getDefaultDatabase() {
        return defaultDatabase;
    }

    @Autowired(required = false)
    public void setCodecRegistries(List<CodecRegistry> codecRegistries) {
        this.codecRegistry = CodecRegistries.fromRegistries(
                this.codecRegistry,
                CodecRegistries.fromRegistries(codecRegistries));
    }

    @Autowired(required = false)
    public void setCodecProviders(List<CodecProvider> codecProviders) {
        this.codecRegistry = CodecRegistries.fromRegistries(
                this.codecRegistry,
                CodecRegistries.fromProviders(codecProviders));
    }

    @Autowired(required = false)
    public void setCodecs(List<Codec<?>> codecs) {
        this.codecRegistry = CodecRegistries.fromRegistries(
                this.codecRegistry,
                CodecRegistries.fromCodecs(codecs));
    }

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public CodecRegistry getCodecRegistry() {
        return codecRegistry;
    }

    public PersistentEntityCodec getPersistentEntityCodec(PersistentEntity entity) {
        if (entity instanceof EmbeddedPersistentEntity) {
            return new PersistentEntityCodec(codecRegistry, entity);
        } else {
            return getPersistentEntityCodec(entity.getJavaClass());
        }
    }

    public PersistentEntityCodec getPersistentEntityCodec(Class entityClass) {
        if (entityClass == null) {
            throw new IllegalArgumentException("Argument [entityClass] cannot be null");
        }

        final PersistentEntity entity = getMappingContext().getPersistentEntity(entityClass.getName());
        if (entity == null) {
            throw new IllegalArgumentException("Argument [" + entityClass + "] is not an entity");
        }

        return (PersistentEntityCodec) getCodecRegistry().get(entity.getJavaClass());
    }

    @Override
    public ConfigurableApplicationEventPublisher getApplicationEventPublisher() {
        return (ConfigurableApplicationEventPublisher) this.eventPublisher;
    }

    public MongoClient getMongoClient() {
        return mongo;
    }

    public String getCollectionName(PersistentEntity entity) {
        final String collectionName = mongoCollections.get(entity);
        if(collectionName == null) {
            final String decapitalizedName = entity.getDecapitalizedName();
            mongoCollections.put(entity, decapitalizedName);
            return decapitalizedName;
        }
        return collectionName;
    }

    @Override
    protected Session createSession(PropertyResolver connDetails) {
        if (stateless) {
            return createStatelessSession(connDetails);
        } else {
            if (codecEngine) {
                return new MongoCodecSession(this, getMappingContext(), getApplicationEventPublisher(), false);
            } else {
                return new MongoSession(this, getMappingContext(), getApplicationEventPublisher(), false);
            }
        }
    }

    @Override
    protected Session createStatelessSession(PropertyResolver connectionDetails) {
        if (codecEngine) {
            return new MongoCodecSession(this, getMappingContext(), getApplicationEventPublisher(), true);
        } else {
            return new MongoSession(this, getMappingContext(), getApplicationEventPublisher(), true);
        }
    }


    @Override
    public DocumentMappingContext getMappingContext() {
        return (DocumentMappingContext) super.getMappingContext();
    }

    /**
     * Indexes any properties that are mapped with index:true
     *
     * @param entity The entity
     */
    protected void initializeIndices(final PersistentEntity entity) {
        String collectionName = entity.getDecapitalizedName();
        String databaseName = getMappingContext().getDefaultDatabaseName();

        MongoCollection collectionMapping = (MongoCollection)entity.getMapping().getMappedForm();
        if(collectionMapping.getCollection() != null) {
            collectionName = collectionMapping.getCollection();
        }
        if(collectionMapping.getDatabase() != null) {
            databaseName = collectionMapping.getDatabase();
        }

        mongoCollections.put(entity, collectionName);
        mongoDatabases.put(entity,databaseName);

        final com.mongodb.client.MongoCollection<Document> collection = getMongoClient().getDatabase(databaseName)
                .getCollection(collectionName);


        final ClassMapping<MongoCollection> classMapping = entity.getMapping();
        if (classMapping != null) {
            final MongoCollection mappedForm = classMapping.getMappedForm();
            if (mappedForm != null) {
                List<MongoCollection.Index> indices = mappedForm.getIndices();
                for (MongoCollection.Index index : indices) {
                    final Map<String, Object> options = index.getOptions();
                    final IndexOptions indexOptions = MongoConstants.mapToObject(IndexOptions.class, options);
                    collection.createIndex(new Document(index.getDefinition()), indexOptions);
                }

                for (Map compoundIndex : mappedForm.getCompoundIndices()) {

                    Map indexAttributes = null;
                    if (compoundIndex.containsKey(INDEX_ATTRIBUTES)) {
                        Object o = compoundIndex.remove(INDEX_ATTRIBUTES);
                        if (o instanceof Map) {
                            indexAttributes = (Map) o;
                        }
                    }
                    Document indexDef = new Document(compoundIndex);
                    if (indexAttributes != null) {
                        final IndexOptions indexOptions = MongoConstants.mapToObject(IndexOptions.class, indexAttributes);
                        collection.createIndex(indexDef, indexOptions);
                    } else {
                        collection.createIndex(indexDef);
                    }
                }
            }
        }

        for (PersistentProperty<MongoAttribute> property : entity.getPersistentProperties()) {
            final boolean indexed = isIndexed(property);

            if (indexed) {
                final MongoAttribute mongoAttributeMapping = property.getMapping().getMappedForm();
                Document dbObject = new Document();
                final String fieldName = getMongoFieldNameForProperty(property);
                dbObject.put(fieldName, 1);
                Document options = new Document();
                if (mongoAttributeMapping != null) {
                    Map attributes = mongoAttributeMapping.getIndexAttributes();
                    if (attributes != null) {
                        attributes = new HashMap(attributes);
                        if (attributes.containsKey(MongoAttribute.INDEX_TYPE)) {
                            dbObject.put(fieldName, attributes.remove(MongoAttribute.INDEX_TYPE));
                        }
                        options.putAll(attributes);
                    }
                }
                // continue using deprecated method to support older versions of MongoDB
                if (options.isEmpty()) {
                    collection.createIndex(dbObject);
                } else {
                    final IndexOptions indexOptions = MongoConstants.mapToObject(IndexOptions.class, options);
                    collection.createIndex(dbObject, indexOptions);
                }
            }
        }


    }

    String getMongoFieldNameForProperty(PersistentProperty<MongoAttribute> property) {
        PropertyMapping<MongoAttribute> pm = property.getMapping();
        String propKey = null;
        if (pm.getMappedForm() != null) {
            propKey = pm.getMappedForm().getField();
        }
        if (propKey == null) {
            propKey = property.getName();
        }
        return propKey;
    }

    public void persistentEntityAdded(PersistentEntity entity) {
        initializeIndices(entity);
    }

    @Override
    @PreDestroy
    public void close() throws IOException {
        try {
            super.destroy();
        } catch (Exception e) {
            // ignore
        }
        try {
            if (mongo != null) {
                mongo.close();
            }
        } finally {

            if(gormEnhancer != null) {
                try {
                    gormEnhancer.close();
                } catch (Throwable e) {
                    // Ignore
                }
            }
        }
    }

    @Override
    public boolean isSchemaless() {
        return true;
    }


    public String getDatabaseName(PersistentEntity entity) {
        final String databaseName = mongoDatabases.get(entity);
        if(databaseName == null) {
            mongoDatabases.put(entity, defaultDatabase);
            return defaultDatabase;
        }
        return databaseName;
    }

    class PersistentEntityCodeRegistry implements CodecProvider {

        Map<String, PersistentEntityCodec> codecs = new HashMap<String, PersistentEntityCodec>();

        @Override
        public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
            final String entityName = clazz.getName();
            PersistentEntityCodec codec = codecs.get(entityName);
            if (codec == null) {
                final PersistentEntity entity = getMappingContext().getPersistentEntity(entityName);
                if (entity != null) {
                    codec = new PersistentEntityCodec(codecRegistry, entity);
                    codecs.put(entityName, codec);
                }
            }
            return codec;
        }
    }
}
