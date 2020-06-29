package grails.plugins.mongodb

import grails.boot.GrailsApp
import grails.plugins.metadata.PluginSource
import groovy.transform.CompileStatic

@CompileStatic
@PluginSource
class Application {

    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }
}