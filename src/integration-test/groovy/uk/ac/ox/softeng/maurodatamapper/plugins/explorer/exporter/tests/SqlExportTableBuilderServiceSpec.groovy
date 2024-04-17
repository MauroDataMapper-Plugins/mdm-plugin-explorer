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

import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.IntegrationTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.SqlExporterTestDataModel
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.SqlExporterTestHelper
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.SqlExportTableBuilderService
import uk.ac.ox.softeng.maurodatamapper.profile.ProfileService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Integration
@Slf4j
@Rollback
class SqlExportTableBuilderServiceSpec extends BaseIntegrationSpec {

    IntegrationTestGivens given
    SqlExporterTestDataModel givenDataModel

    User testUser

    @Autowired
    SqlExportTableBuilderService sqlExportDataService

    @Autowired
    ProfileService profileService

    def setup() {
        given = new IntegrationTestGivens(messageSource, profileService)
        givenDataModel = new SqlExporterTestDataModel(messageSource, profileService)
    }

    @Override
    void setupDomainData() {
        testUser = given."there is a user"()
        folder = given."there is a folder"('test folder')
    }

    void "should build a sql export json for a data model: #testName"(String testName) {

        given: "there is initial test data"
        setupData()

        def dataModel = givenDataModel."baseline data for testing sql exports"(testUser, folder)

        // Load queries
        InputStream cohortQueryStream = this.class.getResourceAsStream("/exporter/${testName}/cohort-query.json")
        def cohortQuery = new BufferedReader(new InputStreamReader(cohortQueryStream))
            .readLines()
            .join("\n")
            .trim()
        given."there is a rule with a representation"("cohort",dataModel,"json-meql",cohortQuery)

        InputStream dataQueryStream = this.class.getResourceAsStream("/exporter/${testName}/data-query.json")
        def dataQuery = new BufferedReader(new InputStreamReader(dataQueryStream))
            .readLines()
            .join("\n")
            .trim()
        given."there is a rule with a representation"("data",dataModel,"json-meql",dataQuery)

        checkAndSave(dataModel)

        when: "the data model is exported"
        def sqlExportTables = sqlExportDataService.prepareSqlExport(dataModel)
        def sqlExportJson = JsonOutput.toJson(sqlExportTables)
        def sqlExportJsonFormatted = JsonOutput.prettyPrint(sqlExportJson)

        then: "the expected sql script is returned"
        SqlExporterTestHelper sqlExporterTestHelper = new SqlExporterTestHelper()
        def expectedOutput = sqlExporterTestHelper.loadJsonFile(testName, "sql-export.json")

        with {
            sqlExportJsonFormatted == expectedOutput
        }

        where:
        testName                                    | _
        "cohort and data queries"                   | _
        "no queries"                                | _
        "cohort query only (bit only)"              | _
        "cohort query only (date only)"             | _
        "cohort query only (decimal only)"          | _
        "cohort query only (int only)"              | _
        "cohort query only (varchar only)"          | _
        "cohort query only (simple AND)"            | _
        "cohort query only (simple OR)"             | _
        "cohort query only (simple JOIN)"           | _
        "cohort query only (multiple table JOIN)"   | _
        "cohort query only (null date)"             | _
        "cohort query only (multiple ORs)"          | _
        "data query only (int only)"                | _
    }
}