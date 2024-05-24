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
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.pdf.exporter.core.dita.DataSpecificationDitaBuilder
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.pdf.exporter.core.dita.SchemaDitaBuilder
import uk.ac.ox.softeng.maurodatamapper.profile.ProfileService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

@Integration
@Slf4j
@Rollback
class DataSpecificationDitaBuilderSpec extends BaseIntegrationSpec {

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

    void "should create a data specification project: #testName"(String testName, Boolean loadCohortQuery, Boolean loadDataQuery) {

        given: "there is initial test data"
        setupData()

        def dataModel = givenDataModel."baseline data for testing sql exports"(testUser, folder)

        // Load queries
        if (loadCohortQuery) {
            def cohortQuery = exporterTestHelper.loadTextFile(testName, "cohort-query.json")
            given."there is a rule with a representation"("cohort", dataModel, "json-meql", cohortQuery)
        }

        if (loadDataQuery) {
            def dataQuery = exporterTestHelper.loadTextFile(testName, "data-query.json")
            given."there is a rule with a representation"("data", dataModel, "json-meql", dataQuery)
        }

        checkAndSave(dataModel)

        when: "queries topic is built"
        def project = DataSpecificationDitaBuilder.buildDitaProject(dataModel)
        String actualMainMapDitaAsXml = project.mainMap.toXmlString()
        String actualTopicDitaAsXml = project.topicsById["datamodel"].toXmlString()

        def expectedMainMapDitaAsXml = exporterTestHelper.loadTextFile(testName, "ditaDataSpecification_mainMap.xml")
        def expectedTopicDitaAsXml = exporterTestHelper.loadTextFile(testName, "ditaDataSpecification_Topic.xml")

        // Standardise XML before comparisons
        actualMainMapDitaAsXml = ExporterTestHelper.standardiseXml(actualMainMapDitaAsXml)
        actualTopicDitaAsXml = ExporterTestHelper.standardiseXml(actualTopicDitaAsXml)
        expectedMainMapDitaAsXml = ExporterTestHelper.standardiseXml(expectedMainMapDitaAsXml)
        expectedTopicDitaAsXml = ExporterTestHelper.standardiseXml(expectedTopicDitaAsXml)


        then: "the expected topic is returned"
        with {
            actualMainMapDitaAsXml == expectedMainMapDitaAsXml
            actualTopicDitaAsXml == expectedTopicDitaAsXml
        }

        // We don't need to test all query types since this is about testing the schema part
        where:
        testName                                    | loadCohortQuery   | loadDataQuery
        "cohort and data queries"                   | true              | true
        "no queries"                                | false             | false
        "cohort query only (int only)"              | true              | false
        "data query only (int only)"                | false             | true

    }


}
