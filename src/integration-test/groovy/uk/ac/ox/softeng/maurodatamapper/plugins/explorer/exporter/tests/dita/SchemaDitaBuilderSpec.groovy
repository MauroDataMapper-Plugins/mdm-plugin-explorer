/*
 * Copyright 2020-2024 University of Oxford and NHS England
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.tests.dita

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.ExporterTestHelper
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.IntegrationTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.SqlExporterTestDataModel
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.pdf.exporter.core.dita.SchemaDitaBuilder
import uk.ac.ox.softeng.maurodatamapper.profile.ProfileService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

@Integration
@Slf4j
@Rollback
class SchemaDitaBuilderSpec extends BaseIntegrationSpec {

    IntegrationTestGivens given
    SqlExporterTestDataModel givenDataModel
    ExporterTestHelper exporterTestHelper

    User testUser

    @Autowired
    ProfileService profileService

    def setup() {
        given = new IntegrationTestGivens(messageSource, profileService)
        givenDataModel = new SqlExporterTestDataModel(messageSource, profileService)
        exporterTestHelper = new ExporterTestHelper()
    }

    @Override
    void setupDomainData() {
        testUser = given."there is a user"()
        folder = given."there is a folder"('test folder')
    }

    void "should create a schema topic: #testName"(String testName) {

        given: "there is initial test data"
        setupData()

        def dataModel = givenDataModel."baseline data for testing sql exports"(testUser, folder)
        checkAndSave(dataModel)

        when: "queries topic is built"
        def topic = SchemaDitaBuilder.buildDataSchemaTopic(dataModel)
        String actualDitaAsXml = topic.toXmlString()
        def expectedDitaAsXml = exporterTestHelper.loadTextFile(testName, "ditaSchema.xml")

        // Make sure xml formatting is consistent before comparison
        actualDitaAsXml = ExporterTestHelper.standardiseXml(actualDitaAsXml)
        expectedDitaAsXml = ExporterTestHelper.standardiseXml(expectedDitaAsXml)

        then: "the expected topic is returned"
        with {
            actualDitaAsXml == expectedDitaAsXml
        }

        // We don't need to test all query types since this is about testing the schema part
        where:
        testName                                    | _
        "cohort and data queries"                   | _

    }


}
