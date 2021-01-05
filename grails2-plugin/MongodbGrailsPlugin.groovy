import grails.mongodb.bootstrap.MongoDbDataStoreSpringInitializer
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsClass
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import grails.util.Metadata

class MongodbGrailsPlugin {
	def version = "6.1.8.BUILD-SNAPSHOT" // added by Gradle
    def license = "Apache 2.0 License"
    def organization = [name: "Grails", url: "http://grails.org/"]
    def developers = [
        [name: "Graeme Rocher", email: "graeme@grails.org"]]
    def issueManagement = [system: "Github", url: "https://github.com/grails/grails-data-mapping"]
    def scm = [url: "https://github.com/grails/grails-data-mapping"]

    def grailsVersion = "2.5.0 > *"
    def observe = ['services', 'domainClass']
    def loadAfter = ['domainClass', 'hibernate', 'hibernate4', 'services']
    def author = "Graeme Rocher"
    def authorEmail = "graeme@grails.org"
    def title = "MongoDB GORM"
    def description = 'A plugin that integrates the MongoDB document datastore into Grails, providing a GORM API onto it'

    def documentation = "http://grails.github.io/grails-data-mapping/latest/mongodb/"

    def pluginExcludes = [
            'grails-app/domain/**'
    ]

    def doWithSpring = {
        def domainClasses = application.getArtefacts(DomainClassArtefactHandler.TYPE).collect() { GrailsClass cls -> cls.clazz }

        def initializer = new MongoDbDataStoreSpringInitializer(application.config, domainClasses)
        initializer.registerApplicationIfNotPresent = false
        initializer.databaseName = Metadata.getCurrent().getApplicationName()
        initializer.setSecondaryDatastore( manager.hasGrailsPlugin("hibernate") || manager.hasGrailsPlugin("hibernate4")  )

        def definitions = initializer.getBeanDefinitions((BeanDefinitionRegistry) springConfig.getUnrefreshedApplicationContext())
        definitions.delegate = delegate
        definitions.call()

        def currentSpringConfig = getSpringConfig()
        if (manager?.hasGrailsPlugin("controllers")) {
            mongoOpenSessionInViewInterceptor(org.grails.datastore.mapping.web.support.OpenSessionInViewInterceptor) {
                datastore = ref("mongoDatastore")
            }
            if (currentSpringConfig.containsBean("controllerHandlerMappings")) {
                controllerHandlerMappings.interceptors << ref("mongoOpenSessionInViewInterceptor")
            }
            if (currentSpringConfig.containsBean("annotationHandlerMapping")) {
                if (annotationHandlerMapping.interceptors) {
                    annotationHandlerMapping.interceptors << ref("mongoOpenSessionInViewInterceptor")
                }
                else {
                    annotationHandlerMapping.interceptors = [ref("mongoOpenSessionInViewInterceptor")]
                }
            }
        }
    }    
}
