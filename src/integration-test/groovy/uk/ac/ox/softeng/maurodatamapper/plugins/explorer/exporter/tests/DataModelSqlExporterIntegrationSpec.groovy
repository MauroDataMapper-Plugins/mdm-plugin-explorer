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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.tests

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.IntegrationTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.SqlExporterTestDataModel
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.SqlExporterTestHelper
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.SqlExportTableBuilderService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.provider.exporter.DataModelSqlExporterService
import uk.ac.ox.softeng.maurodatamapper.profile.ProfileService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Integration
@Slf4j
@Rollback
class DataModelSqlExporterIntegrationSpec extends BaseIntegrationSpec {

    private DataModelSqlExporterService sut

    IntegrationTestGivens given
    SqlExporterTestDataModel givenDataModel
    SqlExporterTestHelper sqlExporterTestHelper

    User testUser

    @Autowired
    DataModelService dataModelService

    @Autowired
    SqlExportTableBuilderService sqlExportDataService

    @Autowired
    ProfileService profileService

    def setup() {
        given = new IntegrationTestGivens(messageSource, profileService)
        givenDataModel = new SqlExporterTestDataModel(messageSource, profileService)
        sqlExporterTestHelper = new SqlExporterTestHelper()

        sqlExportDataService

        sut = new DataModelSqlExporterService()
        sut.dataModelService = dataModelService
        sut.sqlExportDataService = sqlExportDataService
    }

    @Override
    void setupDomainData() {
        testUser = given."there is a user"()
        folder = given."there is a folder"('test folder')
    }

    void "should export a sql script for a data model: #testName"(String testName) {

        given: "there is initial test data"
        setupData()

        def dataModel = givenDataModel."baseline data for testing sql exports"(testUser, folder)

        // Load queries
        def cohortQuery = sqlExporterTestHelper.loadJsonFile(testName, "cohort-query.json")
        given."there is a rule with a representation"("cohort",dataModel,"json-meql",cohortQuery)

        def dataQuery = sqlExporterTestHelper.loadJsonFile(testName, "data-query.json")
        given."there is a rule with a representation"("data",dataModel,"json-meql",dataQuery)

        checkAndSave(dataModel)

        when: "the data model is exported"
        ByteArrayOutputStream outputStream = sut.exportDomain(testUser, dataModel.id, [:])

        then: "the expected sql script is returned"
        def expectedOutput = sqlExporterTestHelper.loadJsonFile(testName, "sql-export.sql")

        // Make sure line endings are consistent before comparison
        String actualOutput = outputStream.toString("UTF-8").trim().replaceAll("\r\n", "\n")

        with {
            actualOutput == expectedOutput
        }

        where:
        testName                            | _
        "cohort and data queries"           | _
        "no queries"                        | _
        "cohort query only (bit only)"      | _
        "cohort query only (date only)"     | _
        "cohort query only (decimal only)"  | _
        "cohort query only (int only)"      | _
        "cohort query only (varchar only)"  | _
        "cohort query only (simple AND)"    | _
        "cohort query only (simple OR)"     | _
        "cohort query only (simple JOIN)"   | _
        "data query only (int only)"        | _

    }
}
