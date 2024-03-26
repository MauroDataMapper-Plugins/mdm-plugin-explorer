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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter

import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.IntegrationTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportForeignKeyProfileFields
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExporterTestDataModel
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.reader.ProfileReaderService
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
class ProfileReaderServiceSpec extends BaseIntegrationSpec {

    IntegrationTestGivens given
    SqlExporterTestDataModel givenDataModel

    User testUser

    @Autowired
    ProfileReaderService profileReaderService

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

    void "should get primary key if exists"(boolean hasPrimaryKey, boolean hasForeignKey, boolean expectedKeyIsFound) {

        given: "there is initial test data"
        setupData()

        and: "a suitable data model exists"
        def dataModel = given."there is a data model"("sample-data_onboarded", folder)

        // Primitive data types
        def intDataType = given."there is a primitive data type"("int", dataModel)
        def varcharDataType = given."there is a primitive data type"("varchar", dataModel)

        // Schemas
        def dboSchemaDataClassPeople = given."there is a data class"("people", dataModel)

        // Table - "patients"
        def patientsTableDataClass = given."there is a data class"("patients", dataModel, dboSchemaDataClassPeople)
        given."there is a data element"("id", dataModel, patientsTableDataClass, intDataType)  // Primary key
        given."there is a data element"("forename", dataModel, patientsTableDataClass, varcharDataType)

        given."there is a data class sql table profile"(patientsTableDataClass, testUser, {section ->
            given."a profile field value is set"(section, "primary_key_columns", "id")
        })

        // Relationship - "patients" <--> "episodes"
        def patientEpisodesForeignKeyRefType = given."there is a reference data type"("patients_ref", dataModel, patientsTableDataClass)

        // Table - "episodes"
        def episodesTableDataClass = given."there is a data class"("episodes", dataModel, dboSchemaDataClassPeople)
        given."there is a data element"("id", dataModel, episodesTableDataClass, intDataType)    // Primary key
        def referenceElement = given."there is a data element"("patientId", dataModel, episodesTableDataClass, patientEpisodesForeignKeyRefType)    // Foreign key to "users.id"
        if (hasForeignKey) {
            given."there is a data element sql column profile"(referenceElement, testUser, {section ->
                given."a profile field value is set"(section, "character_maximum_length", "50") // varchar(50)
                given."a profile field value is set"(section, "ordinal_position", "3")
                given."a profile field value is set"(section, "column_name", "patientId")
                given."a profile field value is set"(section, "original_data_type", "int")
                given."a profile field value is set"(section, "foreign_key_name", "FK__Location__Patien__6C190EBB	")
                given."a profile field value is set"(section, "foreign_key_schema", "people")
                given."a profile field value is set"(section, "foreign_key_table", "patients")
                given."a profile field value is set"(section, "foreign_key_columns", "id")
                given."a profile field value is set"(section, "numeric_precision", "10")
                given."a profile field value is set"(section, "numeric_precision_radix", "10")
                given."a profile field value is set"(section, "ordinal_position", "3")
            })
        }

        if (hasPrimaryKey) {
            given."there is a data class sql table profile"(episodesTableDataClass, testUser, {section ->
                given."a profile field value is set"(section, "primary_key_columns", "id")
            })
        }

        checkAndSave(dataModel)

        when: "the data model is exported"
        String[] primaryKeys = profileReaderService.getPrimaryKeys(episodesTableDataClass)

        then: "the expected sql script is returned"
        def expectedResult = (expectedKeyIsFound) ? ["id"] as String[] : [] as String[]

        with {
            primaryKeys == expectedResult
        }

        where:
        hasPrimaryKey   | hasForeignKey     | expectedKeyIsFound
        false           | false             | false
        true            | false             | true
        false           | true              | false
        true            | true              | true

    }

    void "should get foreign key if exists"(boolean hasPrimaryKey, boolean hasForeignKey, boolean expectedKeyIsFound) {

        given: "there is initial test data"
        setupData()

        and: "a suitable data model exists"
        def dataModel = given."there is a data model"("sample-data_onboarded", folder)

        // Primitive data types
        def intDataType = given."there is a primitive data type"("int", dataModel)
        def varcharDataType = given."there is a primitive data type"("varchar", dataModel)

        // Schemas
        def dboSchemaDataClassPeople = given."there is a data class"("people", dataModel)

        // Table - "patients"
        def patientsTableDataClass = given."there is a data class"("patients", dataModel, dboSchemaDataClassPeople)
        given."there is a data element"("id", dataModel, patientsTableDataClass, intDataType)  // Primary key
        given."there is a data element"("forename", dataModel, patientsTableDataClass, varcharDataType)

        given."there is a data class sql table profile"(patientsTableDataClass, testUser, {section ->
            given."a profile field value is set"(section, "primary_key_columns", "id")
        })

        // Relationship - "patients" <--> "episodes"
        def patientEpisodesForeignKeyRefType = given."there is a reference data type"("patients_ref", dataModel, patientsTableDataClass)

        // Table - "episodes"
        def episodesTableDataClass = given."there is a data class"("episodes", dataModel, dboSchemaDataClassPeople)
        given."there is a data element"("id", dataModel, episodesTableDataClass, intDataType)    // Primary key
        def referenceElement = given."there is a data element"("patientId", dataModel, episodesTableDataClass, patientEpisodesForeignKeyRefType)    // Foreign key to "users.id"
        if (hasForeignKey) {
            given."there is a data element sql column profile"(referenceElement, testUser, {section ->
                given."a profile field value is set"(section, "character_maximum_length", "50") // varchar(50)
                given."a profile field value is set"(section, "ordinal_position", "3")
                given."a profile field value is set"(section, "column_name", "patientId")
                given."a profile field value is set"(section, "original_data_type", "int")
                given."a profile field value is set"(section, "foreign_key_name", "FK__Location__Patien__6C190EBB	")
                given."a profile field value is set"(section, "foreign_key_schema", "people")
                given."a profile field value is set"(section, "foreign_key_table", "patients")
                given."a profile field value is set"(section, "foreign_key_columns", "id")
                given."a profile field value is set"(section, "numeric_precision", "10")
                given."a profile field value is set"(section, "numeric_precision_radix", "10")
                given."a profile field value is set"(section, "ordinal_position", "3")
            })
        }

        if (hasPrimaryKey) {
            given."there is a data class sql table profile"(episodesTableDataClass, testUser, {section ->
                given."a profile field value is set"(section, "primary_key_columns", "id")
            })
        }

        checkAndSave(dataModel)

        when: "the foreign key is got"
        SqlExportForeignKeyProfileFields foreignKeys = profileReaderService.getForeignKeyProfileFields(referenceElement)


        then: "the expected foreign key is found"
        def expectedResult = (expectedKeyIsFound) ? ["people", "patients", "id"] : [null, null, null]

        with {
            foreignKeys?.schema?.currentValue == expectedResult[0]
            foreignKeys?.table?.currentValue == expectedResult[1]
            foreignKeys?.columns?.currentValue == expectedResult[2]
        }

        where:
        hasPrimaryKey   | hasForeignKey     | expectedKeyIsFound
        false           | false             | false
        true            | false             | false
        false           | true              | true
        true            | true              | true

    }
}
