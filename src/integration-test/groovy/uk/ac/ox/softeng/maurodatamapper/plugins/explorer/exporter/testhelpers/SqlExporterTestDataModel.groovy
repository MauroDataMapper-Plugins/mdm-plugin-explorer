/*
 * Copyright 2020-2024 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.IntegrationTestGivens
import uk.ac.ox.softeng.maurodatamapper.profile.ProfileService
import uk.ac.ox.softeng.maurodatamapper.security.User


import groovy.util.logging.Slf4j
import org.springframework.context.MessageSource

@Slf4j
class SqlExporterTestDataModel {
    IntegrationTestGivens given
    MessageSource messageSource
    ProfileService profileService

    SqlExporterTestDataModel(MessageSource messageSource, ProfileService profileService) {
        this.messageSource = messageSource
        this.profileService = profileService
        given = new IntegrationTestGivens(messageSource, profileService)
    }

    DataModel "baseline data for testing sql exports"(User testUser, Folder folder) {
        and: "a suitable data model exists"
        def dataModel = given."there is a data model"("sample-data_onboarded", folder)

        // Primitive data types
        def intDataType = given."there is a primitive data type"("int", dataModel)
        def decimalDataType = given."there is a primitive data type"("decimal", dataModel)
        def bitDataType = given."there is a primitive data type"("bit", dataModel)
        def dateDataType = given."there is a primitive data type"("date", dataModel)
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
        given."there is a data element"("dateOfBirth", dataModel, patientsTableDataClass, dateDataType)
        given."there is a data element"("age", dataModel, patientsTableDataClass, intDataType)
        given."there is a data element"("height", dataModel, patientsTableDataClass, decimalDataType)
        given."there is a data element"("exercisesRegularly", dataModel, patientsTableDataClass, bitDataType)

        given."there is a data class sql table profile"(patientsTableDataClass, testUser, { section ->
            given."a profile field value is set"(section, "primary_key_columns", "id")
        })

        // Relationship - "patients" <--> "episodes"
        def patientEpisodesForeignKeyRefType = given."there is a reference data type"("patients_ref", dataModel, patientsTableDataClass)

        // Table - "episodes"
        def episodesTableDataClass = given."there is a data class"("episodes", dataModel, dboSchemaDataClassMedical)
        given."there is a data element"("id", dataModel, episodesTableDataClass, intDataType)    // Primary key
        def referenceElement = given."there is a data element"("patientId", dataModel, episodesTableDataClass, patientEpisodesForeignKeyRefType)    // Foreign key to "users.id"
        given."there is a profile identifying a foreign key"(referenceElement, testUser, "people", "patients", "id")
        given."there is a data element"("description", dataModel, episodesTableDataClass, varcharDataType)
        given."there is a data element"("score", dataModel, episodesTableDataClass, decimalDataType)
        given."there is a data element"("occurredAt", dataModel, episodesTableDataClass, dateTimeDataType)
        given."there is a data element"("do_not_include", dataModel, episodesTableDataClass, bitDataType)

        // Relationship - "patients" <--> "treatments"
        def patientTreatmentForeignKeyRefType = given."there is a reference data type"("treatment_to_patients_ref", dataModel, patientsTableDataClass)

        // Relationship - "episodes" <--> "treatments"
        def episodeTreatmentForeignKeyRefType = given."there is a reference data type"("treatment_to_patients_ref", dataModel, episodesTableDataClass)

        // Table - "treatments"
        def treatmentsTableDataClass = given."there is a data class"("treatments", dataModel, dboSchemaDataClassMedical)
        given."there is a data element"("id", dataModel, treatmentsTableDataClass, intDataType)    // Primary key
        def patientTreatmentReferenceElement = given."there is a data element"("patientId", dataModel, treatmentsTableDataClass, patientTreatmentForeignKeyRefType)
        given."there is a profile identifying a foreign key"(patientTreatmentReferenceElement, testUser, "people", "patients", "id")

        def episodeTreatmentReferenceElement = given."there is a data element"("episodeId", dataModel, treatmentsTableDataClass, episodeTreatmentForeignKeyRefType)
        given."there is a profile identifying a foreign key"(episodeTreatmentReferenceElement, testUser, "medical", "episodes", "id")
        given."there is a data element"("description", dataModel, treatmentsTableDataClass, varcharDataType)
        given."there is a data element"("givenOn", dataModel, treatmentsTableDataClass, dateTimeDataType)
        given."there is a data element"("do_not_include", dataModel, treatmentsTableDataClass, intDataType)

        dataModel
    }
}
