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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.IntegrationTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.SqlExporterTestDataModel
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.ExporterTestHelper
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.provider.exporter.DataModelPdfExporterService
import uk.ac.ox.softeng.maurodatamapper.profile.ProfileService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Integration
@Slf4j
@Rollback
class DataModelPdfExporterIntegrationSpec extends BaseIntegrationSpec {

    private DataModelPdfExporterService pdfExporterService

    IntegrationTestGivens given
    SqlExporterTestDataModel givenDataModel
    ExporterTestHelper exporterTestHelper

    User testUser

    @Autowired
    DataModelService dataModelService

    @Autowired
    ProfileService profileService

    def setup() {
        given = new IntegrationTestGivens(messageSource, profileService)
        givenDataModel = new SqlExporterTestDataModel(messageSource, profileService)
        exporterTestHelper = new ExporterTestHelper()

        pdfExporterService = new DataModelPdfExporterService()
        pdfExporterService.dataModelService = dataModelService
    }

    @Override
    void setupDomainData() {
        testUser = given."there is a user"()
        folder = given."there is a folder"('test folder')
    }

    void "should export a pdf file for a data model: #testName"(String testName) {

        given: "there is initial test data"
        setupData()

        def dataModel = givenDataModel."baseline data for testing sql exports"(testUser, folder)

        // Load queries
        if (testName != "no queries") {
            def cohortQuery = exporterTestHelper.loadTextFile(testName, "cohort-query.json")
            given."there is a rule with a representation"("cohort", dataModel, "json-meql", cohortQuery)

            def dataQuery = exporterTestHelper.loadTextFile(testName, "data-query.json")
            given."there is a rule with a representation"("data", dataModel, "json-meql", dataQuery)
        }

        checkAndSave(dataModel)

        when: "the data model is exported"
        ByteArrayOutputStream outputStream = pdfExporterService.exportDomain(testUser, dataModel.id, [:])
        def actualByteSize = writeToFile(outputStream, "${testName}.pdf")

        then: "the generated odf is of the expected size"
        with {
            actualByteSize == expectedBytesSize
        }


        // These tests are slow to run and we can only check the bytes size
        // so it is not worth running all the combinations of queries.
        where:
        testName                                    | expectedBytesSize
        "no queries"                                | 16077
        "cohort and data queries"                   | 16329
        "cohort query only (int only)"              | 16145
        "data query only (int only)"                | 16157

    }

    int writeToFile(ByteArrayOutputStream byteArrayOutputStream, String filenameToSaveAs) {
        byte[] bytes = byteArrayOutputStream.toByteArray()
        Path outputPath = Paths.get('build/tmp', filenameToSaveAs)
        log.warn('File written to {}', outputPath)
        Files.write(outputPath, bytes)
        bytes.size()
    }
}
