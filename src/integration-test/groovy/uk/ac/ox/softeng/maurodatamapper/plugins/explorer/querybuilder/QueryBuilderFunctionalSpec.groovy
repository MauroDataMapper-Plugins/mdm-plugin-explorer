/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.querybuilder

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedSecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityDefinition
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import org.junit.Assert
import org.junit.jupiter.api.Tag
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST
import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getDEVELOPMENT

import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * @since 12/04/2022
 */
@Tag('non-parallel')
@Integration
@Slf4j
class QueryBuilderFunctionalSpec extends BaseFunctionalSpec implements SecurityDefinition {

    @Shared
    Folder folder

    @Shared
    UUID dataModelId
    String dataModelLabel = 'Query Builder Functional Test Data Model'

    QueryBuilderCoreTableProfileProviderService queryBuilderCoreTableProfileProviderService
    GroupBasedSecurityPolicyManagerService groupBasedSecurityPolicyManagerService

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Search Functional Test Folder', createdBy: FUNCTIONAL_TEST).save()

        DataModel dataModel = new DataModel(createdBy: FUNCTIONAL_TEST,
                                            label: dataModelLabel,
                                            folder: folder,
                                            type: DataModelType.DATA_ASSET,
                                            authority: Authority.findByDefaultAuthority(true),
                                            readableByEveryone: true)
        checkAndSave(dataModel)
        dataModelId = dataModel.id
        PrimitiveType stringPrimitive = new PrimitiveType(createdBy: DEVELOPMENT, label: 'string')
        dataModel.addToDataTypes(stringPrimitive)
        checkAndSave(dataModel)

        DataClass peopleSchema = new DataClass(label: 'people', createdBy: FUNCTIONAL_TEST)
        dataModel.addToDataClasses(peopleSchema)

        DataClass medicalSchema = new DataClass(label: 'medical', createdBy: FUNCTIONAL_TEST)
        dataModel.addToDataClasses(medicalSchema)
        checkAndSave(dataModel)

        DataClass patient = new DataClass(label: 'patient', createdBy: FUNCTIONAL_TEST)
        patient.addToDataElements(
            new DataElement(label: 'name', createdBy: FUNCTIONAL_TEST, dataType: stringPrimitive))
        patient.addToDataElements(
            new DataElement(label: 'nhs_number', createdBy: FUNCTIONAL_TEST, dataType: stringPrimitive))
        patient.addToDataElements(
            new DataElement(label: 'date_of_birth', createdBy: FUNCTIONAL_TEST, dataType: stringPrimitive))
        peopleSchema.addToDataClasses(patient)
        checkAndSave(dataModel)

        DataClass labTest = new DataClass(label: 'lab_test', createdBy: FUNCTIONAL_TEST)
        labTest.addToDataElements(
            new DataElement(label: 'test_result', createdBy: FUNCTIONAL_TEST, dataType: stringPrimitive))
        labTest.addToDataElements(
            new DataElement(label: 'test_code', createdBy: FUNCTIONAL_TEST, dataType: stringPrimitive))
        labTest.addToDataElements(
            new DataElement(label: 'test_date', createdBy: FUNCTIONAL_TEST, dataType: stringPrimitive))
        medicalSchema.addToDataClasses(labTest)

        DataClass diagnosis = new DataClass(label: 'diagnosis', createdBy: FUNCTIONAL_TEST)
        diagnosis.addToDataElements(
            new DataElement(label: 'diagnosis_result', createdBy: FUNCTIONAL_TEST, dataType: stringPrimitive))
        diagnosis.addToDataElements(
            new DataElement(label: 'diagnosis_code', createdBy: FUNCTIONAL_TEST, dataType: stringPrimitive))
        diagnosis.addToDataElements(
            new DataElement(label: 'diagnosis_date', createdBy: FUNCTIONAL_TEST, dataType: stringPrimitive))
        medicalSchema.addToDataClasses(diagnosis)

        checkAndSave(dataModel)
        sessionFactory.currentSession.flush()

        groupBasedSecurityPolicyManagerService.reloadUserSecurityPolicyManager(UnloggedUser.UNLOGGED_EMAIL_ADDRESS)
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec SearchFunctionalSpec')
        DataModel.get(dataModelId).delete()
        folder.delete()
    }

    @Override
    String getResourcePath() {
        ''
    }

    void 'S01: query builder profile, test validity of core table string'(String coreTable, String[] expectedErrors) {
        when:
        QueryBuilderCoreTableProfileProviderService pps = queryBuilderCoreTableProfileProviderService
        String endpointPrefix = "dataModels/${dataModelId}"
        Map profileMap =
            [
                sections: [
                    [
                        name  : 'Query Builder Core Table Profile',
                        fields: [
                            [metadataPropertyName: 'queryBuilderCoreTable', currentValue: coreTable],
                            [metadataPropertyName: 'multiplicity', currentValue: 1]
                        ]
                    ]
                ],
                id: "${dataModelId}",
                label: "${dataModelLabel}",
            ]

        // We need to run the GET first to setup the dataModelId for the validation
        // This is the way profiles are called from the Mauro UI so the validation code should be safe
        GET("${endpointPrefix}/profile/${pps.namespace}/${pps.name}/${pps.version}")
        POST("${endpointPrefix}/profile/${pps.namespace}/${pps.name}/validate", profileMap)

        then:
        if (expectedErrors.size() == 0) {
            verifyResponse(OK, response)
            Assert.assertEquals(coreTable, responseBody().sections.fields[0].currentValue[0])
        } else {
            verifyResponse(UNPROCESSABLE_ENTITY, response)
            Assert.assertEquals(expectedErrors.size(), responseBody().total)
            expectedErrors.each {String errorMessage ->
                Assert.assertTrue(responseBody().errors.any{it.message == errorMessage})
            }
        }

        where:
        coreTable           | expectedErrors
        "people.patient"    | []
        ""                  | ["This field cannot be empty","Format needs to be [Schema].[Table] e.g: people.patient"]
        "invalidformat"     | ["Format needs to be [Schema].[Table] e.g: people.patient"]
        "badschema.patient" | ["Schema \"badschema\" cannot be found"]
        "people.badtable"   | ["Table \"badtable\" cannot be found in schema \"people\""]
    }

}
