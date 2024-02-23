/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.column.SqlServerColumnProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.IntegrationTestGivens
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

    User testUser

    @Autowired
    DataModelService dataModelService

    @Autowired
    SqlServerColumnProfileProviderService sqlServerColumnProfileProviderService

    @Autowired
    ProfileService profileService

    def setup() {
        given = new IntegrationTestGivens(messageSource, profileService)

        sut = new DataModelSqlExporterService()
        sut.dataModelService = dataModelService
        sut.sqlServerColumnProfileProviderService = sqlServerColumnProfileProviderService
    }

    @Override
    void setupDomainData() {
        testUser = given."there is a user"()
        folder = given."there is a folder"('test folder')
    }

    void "should export a sql script for a data model: #testName"(String testName) {
        given: "there is initial test data"
        setupData()

        and: "a suitable data model exists"
        def dataModel = given."there is a data model"("sample-data_onboarded", folder)

        // Primitive data types
        def intDataType = given."there is a primitive data type"("int", dataModel)
        def decimalDataType = given."there is a primitive data type"("decimal", dataModel)
        def bitDataType = given."there is a primitive data type"("bit", dataModel)
        def dateTimeDataType = given."there is a primitive data type"("datetime", dataModel)
        def varcharDataType = given."there is a primitive data type"("varchar", dataModel)

        // Schemas
        def dboSchemaDataClassPeople = given."there is a data class"("people", dataModel)
        def dboSchemaDataClassMedical = given."there is a data class"("medical", dataModel)

        // Table - "patients"
        def patientsTableDataClass = given."there is a data class"("patients", dataModel, dboSchemaDataClassPeople)
        def patientId = given."there is a data element"("id", dataModel, patientsTableDataClass, intDataType)  // Primary key
        given."there is a data element"("forename", dataModel, patientsTableDataClass, varcharDataType)
        given."there is a data element"("surname", dataModel, patientsTableDataClass, varcharDataType)
        given."there is a data element"("dateOfBirth", dataModel, patientsTableDataClass, dateTimeDataType)
        given."there is a data element"("age", dataModel, patientsTableDataClass, intDataType)
        given."there is a data element"("height", dataModel, patientsTableDataClass, decimalDataType)
        given."there is a data element"("exercisesRegularly", dataModel, patientsTableDataClass, bitDataType)

        // Relationship - "patients" <--> "episodes"
        def patientEpisodesForeignKeyRefType = given."there is a reference data type"("patients_ref", dataModel, patientsTableDataClass)


        // Table - "episodes"
        def episodesTableDataClass = given."there is a data class"("episodes", dataModel, dboSchemaDataClassMedical)
        given."there is a data element"("id", dataModel, episodesTableDataClass, intDataType)    // Primary key
        def referenceElement = given."there is a data element"("patientId", dataModel, episodesTableDataClass, patientEpisodesForeignKeyRefType)    // Foreign key to "users.id"
        // referenceElement.addToSemanticLinks(linkType: SemanticLinkType.IS_FROM, createdBy: StandardEmailAddress.INTEGRATION_TEST,
        //                                    targetMultiFacetAwareItem: patientId)
        def columnProfile = given."there is a data element sql column profile"(referenceElement, testUser, { section ->
            given."a profile field value is set"(section, "character_maximum_length", "50") // varchar(50)
            given."a profile field value is set"(section, "ordinal_position", "3")
            given."a profile field value is set"(section, "column_name","patientId")
            given."a profile field value is set"(section, "original_data_type","int")
            given."a profile field value is set"(section, "foreign_key_name","FK__Location__Patien__6C190EBB	")
            given."a profile field value is set"(section, "foreign_key_schema","people")
            given."a profile field value is set"(section, "foreign_key_table","patients")
            given."a profile field value is set"(section, "foreign_key_columns","id")
            given."a profile field value is set"(section, "numeric_precision","10")
            given."a profile field value is set"(section, "numeric_precision_radix","10")
            given."a profile field value is set"(section, "ordinal_position","3")
        })
        given."there is a data element"("description", dataModel, episodesTableDataClass, varcharDataType)
        given."there is a data element"("score", dataModel, episodesTableDataClass, decimalDataType)
        given."there is a data element"("occurredAt", dataModel, episodesTableDataClass, dateTimeDataType)
        given."there is a data element"("do_not_include", dataModel, episodesTableDataClass, bitDataType)

        // Relationship - "patients" <--> "treatments"
        def patientTreatmentForeignKeyRefType = given."there is a reference data type"("patients_ref", dataModel, patientsTableDataClass)

        // Table - "treatments"
        def treatmentsTableDataClass = given."there is a data class"("treatments", dataModel, dboSchemaDataClassMedical)
        given."there is a data element"("id", dataModel, treatmentsTableDataClass, intDataType)    // Primary key
        given."there is a data element"("patientId", dataModel, treatmentsTableDataClass, patientTreatmentForeignKeyRefType)    // Foreign key to "users.id"
        given."there is a data element"("description", dataModel, treatmentsTableDataClass, varcharDataType)
        given."there is a data element"("givenOn", dataModel, treatmentsTableDataClass, dateTimeDataType)
        given."there is a data element"("do_not_include", dataModel, treatmentsTableDataClass, intDataType)

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
        ByteArrayOutputStream outputStream = sut.exportDomain(testUser, dataModel.id, [:])

        then: "the expected sql script is returned"
        InputStream sampleDdlScriptStream = this.class.getResourceAsStream("/exporter/${testName}/sql-export.sql")
        def expectedOutput = new BufferedReader(new InputStreamReader(sampleDdlScriptStream))
            .readLines()
            .join("\n")
            .trim()

        // Make sure line endings are consistent before comparison
        String actualOutput = outputStream.toString("UTF-8").trim().replaceAll("\r\n", "\n")

        with {
            actualOutput == expectedOutput
        }

        where:
        testName                            | _
        "general test"                      | _
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
