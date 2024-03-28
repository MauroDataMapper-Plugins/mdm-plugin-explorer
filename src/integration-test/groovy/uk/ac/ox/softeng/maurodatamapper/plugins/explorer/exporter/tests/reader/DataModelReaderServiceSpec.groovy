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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.tests.reader

import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.IntegrationTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.reader.DataModelReaderService
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
class DataModelReaderServiceSpec extends BaseIntegrationSpec {

    IntegrationTestGivens given

    User testUser

    @Autowired
    ProfileService profileService

    def setup() {
        given = new IntegrationTestGivens(messageSource, profileService)
    }

    @Override
    void setupDomainData() {
        testUser = given."there is a user"()
        folder = given."there is a folder"('test folder')
    }

    void "should get a DataClass for schema and table labels when a matching class exists"() {

        given: "there is initial test data"
        setupData()

        and: "a suitable data model exists"
        def dataModel = given."there is a data model"("sample-data_onboarded", folder)

        // Schemas
        def dboSchemaDataClassPeople = given."there is a data class"("people", dataModel)
        def dboSchemaDataClassMedical = given."there is a data class"("medical", dataModel)

        // Tables
        given."there is a data class"("patients", dataModel, dboSchemaDataClassPeople)
        def episodesTableDataClass = given."there is a data class"("episodes", dataModel, dboSchemaDataClassMedical)

        checkAndSave(dataModel)

        when: "the data class is got"
        def retrievedDataClass = DataModelReaderService.getDataClass(dataModel, "medical", "episodes")

        then: "the expected data class is returned"
        with {
            retrievedDataClass == episodesTableDataClass
        }
    }

    void "should not get a DataClass for schema and table labels when a matching schema class does not exist"() {

        given: "there is initial test data"
        setupData()

        and: "a suitable data model exists"
        def dataModel = given."there is a data model"("sample-data_onboarded", folder)

        checkAndSave(dataModel)

        when: "the data class is got"
        def retrievedDataClass = DataModelReaderService.getDataClass(dataModel, "medical", "episodes")

        then: "the expected sql script is returned"
        with {
            retrievedDataClass == null
        }
    }

    void "should not get a DataClass for schema and table labels when no table classes exist"() {

        given: "there is initial test data"
        setupData()

        and: "a suitable data model exists"
        def dataModel = given."there is a data model"("sample-data_onboarded", folder)

        // Schemas
        def dboSchemaDataClassPeople= given."there is a data class"("people", dataModel)
        def dboSchemaDataClassMedical= given."there is a data class"("medical", dataModel)

        // Tables
        given."there is a data class"("patients", dataModel, dboSchemaDataClassPeople)
        given."there is a data class"("episodes", dataModel, dboSchemaDataClassMedical)


        checkAndSave(dataModel)

        when: "the data class is got"
        def retrievedDataClass = DataModelReaderService.getDataClass(dataModel, "medical", "procedures")

        then: "the expected sql script is returned"
        with {
            retrievedDataClass == null
        }
    }

    void "should not get a DataClass for schema and table labels when no matching table class exists"() {

        given: "there is initial test data"
        setupData()

        and: "a suitable data model exists"
        def dataModel = given."there is a data model"("sample-data_onboarded", folder)

        // Schemas
        given."there is a data class"("people", dataModel)
        given."there is a data class"("medical", dataModel)

        checkAndSave(dataModel)

        when: "the data class is got"
        def retrievedDataClass = DataModelReaderService.getDataClass(dataModel, "medical", "episodes")

        then: "the expected sql script is returned"
        with {
            retrievedDataClass == null
        }
    }

    void "should get a DataElement for a label when a matching data element exists"() {

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
        def forenameDataElement = given."there is a data element"("forename", dataModel, patientsTableDataClass, varcharDataType)
        given."there is a data element"("surname", dataModel, patientsTableDataClass, varcharDataType)

        checkAndSave(dataModel)

        when: "the data element is got"
        def dataElement = DataModelReaderService.getDataElement(patientsTableDataClass, "forename")

        then: "the expected data class is returned"
        with {
            dataElement == forenameDataElement
        }
    }

    void "should not get a DataElement for a label when a matching data element does not exist"() {

        given: "there is initial test data"
        setupData()

        and: "a suitable data model exists"
        def dataModel = given."there is a data model"("sample-data_onboarded", folder)

        // Primitive data types
        given."there is a primitive data type"("int", dataModel)
        given."there is a primitive data type"("varchar", dataModel)

        // Schemas
        def dboSchemaDataClassPeople = given."there is a data class"("people", dataModel)

        // Table - "patients"
        def patientsTableDataClass = given."there is a data class"("patients", dataModel, dboSchemaDataClassPeople)

        checkAndSave(dataModel)

        when: "the data element is got"
        def dataElement = DataModelReaderService.getDataElement(patientsTableDataClass, "forename")

        then: "the expected data class is returned"
        with {
            dataElement == null
        }
    }

    void "should not get a DataElement for a label when a matching data element does not match"() {

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
        def forenameDataElement = given."there is a data element"("forename", dataModel, patientsTableDataClass, varcharDataType)
        given."there is a data element"("surname", dataModel, patientsTableDataClass, varcharDataType)

        checkAndSave(dataModel)

        when: "the data element is got"
        def dataElement = DataModelReaderService.getDataElement(patientsTableDataClass, "age")

        then: "the expected data class is returned"
        with {
            dataElement == null
        }
    }
}
