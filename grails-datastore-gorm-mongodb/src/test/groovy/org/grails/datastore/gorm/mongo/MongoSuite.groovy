package org.grails.datastore.gorm.mongo

import grails.gorm.tests.AttachMethodSpec
import grails.gorm.tests.DetachedCriteriaSpec
import grails.gorm.tests.OneToManySpec
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses

/**
 * @author graemerocher
 */
@RunWith(Suite)
@SuiteClasses([
    DetachedCriteriaSpec
])
class MongoSuite {
}
