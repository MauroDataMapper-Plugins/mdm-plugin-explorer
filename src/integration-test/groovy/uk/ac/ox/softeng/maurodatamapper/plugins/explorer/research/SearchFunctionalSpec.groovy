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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.research

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.research.ResearchDataElementProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedSecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityDefinition
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import org.junit.jupiter.api.Tag
import spock.lang.Shared

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST
import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getDEVELOPMENT

import static io.micronaut.http.HttpStatus.OK

/**
 * @since 12/04/2022
 */
@Tag('non-parallel')
@Integration
@Slf4j
class SearchFunctionalSpec extends BaseFunctionalSpec implements SecurityDefinition {

    @Shared
    Folder folder

    @Shared
    UUID dataModelId

    ResearchDataElementProfileProviderService researchDataElementProfileProviderService

    GroupBasedSecurityPolicyManagerService groupBasedSecurityPolicyManagerService

    @Transactional
    Authority getTestAuthority() {
        Authority.findByDefaultAuthority(true)
    }

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Search Functional Test Folder', createdBy: FUNCTIONAL_TEST).save()

        DataModel dataModel = new DataModel(createdBy: FUNCTIONAL_TEST,
                                            label: 'Search Functional Test Data Model',
                                            folder: folder,
                                            type: DataModelType.DATA_ASSET,
                                            authority: Authority.findByDefaultAuthority(true),
                                            readableByEveryone: true)
        checkAndSave(dataModel)
        dataModelId = dataModel.id
        PrimitiveType stringPrimitive = new PrimitiveType(createdBy: DEVELOPMENT, label: 'string')
        dataModel.addToDataTypes(stringPrimitive)
        checkAndSave(dataModel)


        DataClass patient = new DataClass(label: 'patient', createdBy: FUNCTIONAL_TEST)
        patient.addToDataElements(
            addProfile(new DataElement(label: 'name', createdBy: FUNCTIONAL_TEST, dataType: stringPrimitive),
                       'Identifying', 'modules', null, null, null, LocalDate.of(2022, 02, 25)))
        patient.addToDataElements(
            addProfile(new DataElement(label: 'nhs_number', createdBy: FUNCTIONAL_TEST, dataType: stringPrimitive),
                       'Legally Restricted', 'modules', null, null, 'https://nhsdd/nhs', LocalDate.of(2022, 02, 25)))
        patient.addToDataElements(
            addProfile(new DataElement(label: 'date_of_birth', createdBy: FUNCTIONAL_TEST, dataType: stringPrimitive),
                       'Identifying', 'modules', null, null, 'https://nhsdd/birthdate', LocalDate.of(2022, 02, 25)))
        dataModel.addToDataClasses(patient)
        checkAndSave(dataModel)

        DataClass labTest = new DataClass(label: 'lab_test', createdBy: FUNCTIONAL_TEST)
        labTest.addToDataElements(
            addProfile(new DataElement(label: 'test_result', createdBy: FUNCTIONAL_TEST, dataType: stringPrimitive),
                       'Anonymous', 'modules', 'hic', null, null, LocalDate.of(2022, 03, 12)))
        labTest.addToDataElements(
            addProfile(new DataElement(label: 'test_code', createdBy: FUNCTIONAL_TEST, dataType: stringPrimitive),
                       'Anonymous', 'modules', 'hic', 'SNOMED', null, LocalDate.of(2022, 03, 12)))
        labTest.addToDataElements(
            addProfile(new DataElement(label: 'test_date', createdBy: FUNCTIONAL_TEST, dataType: stringPrimitive),
                       'Maybe identifying', 'modules', 'hic', null, null, LocalDate.of(2022, 03, 12)))
        dataModel.addToDataClasses(labTest)

        DataClass diagnosis = new DataClass(label: 'diagnosis', createdBy: FUNCTIONAL_TEST)
        diagnosis.addToDataElements(
            addProfile(new DataElement(label: 'diagnosis_result', createdBy: FUNCTIONAL_TEST, dataType: stringPrimitive),
                       'Anonymous', 'modules', 'hic', null, null, LocalDate.of(2022, 04, 14)))
        diagnosis.addToDataElements(
            addProfile(new DataElement(label: 'diagnosis_code', createdBy: FUNCTIONAL_TEST, dataType: stringPrimitive),
                       'Anonymous', 'modules', 'hic', 'icd10', null, LocalDate.of(2022, 04, 14)))
        diagnosis.addToDataElements(
            addProfile(new DataElement(label: 'diagnosis_date', createdBy: FUNCTIONAL_TEST, dataType: stringPrimitive),
                       'Maybe identifying', 'modules', 'hic', null, null, LocalDate.of(2022, 04, 14)))
        dataModel.addToDataClasses(diagnosis)

        checkAndSave(dataModel)
        sessionFactory.currentSession.flush()

        groupBasedSecurityPolicyManagerService.reloadUserSecurityPolicyManager(UnloggedUser.UNLOGGED_EMAIL_ADDRESS)
    }

    DataElement addProfile(DataElement dataElement, String identifiableData, String sourceSystem, String targetDataset, String terminology, String dataDictionaryItem,
                           LocalDate lastUpdated) {
        researchDataElementProfileProviderService.storeFieldInEntity(dataElement, identifiableData, 'identifiableData', FUNCTIONAL_TEST)
        researchDataElementProfileProviderService.storeFieldInEntity(dataElement, sourceSystem, 'sourceSystem', FUNCTIONAL_TEST)
        if (targetDataset) researchDataElementProfileProviderService.storeFieldInEntity(dataElement, targetDataset, 'targetDataset', FUNCTIONAL_TEST)
        if (terminology) researchDataElementProfileProviderService.storeFieldInEntity(dataElement, terminology, 'terminology', FUNCTIONAL_TEST)
        if (dataDictionaryItem) researchDataElementProfileProviderService.storeFieldInEntity(dataElement, dataDictionaryItem, 'dataDictionaryItem', FUNCTIONAL_TEST)
        researchDataElementProfileProviderService.storeFieldInEntity(dataElement, lastUpdated.format(DateTimeFormatter.ofPattern('dd/MM/yyyy')), 'lastUpdated',
                                                                     FUNCTIONAL_TEST)
        researchDataElementProfileProviderService.storeFieldInEntity(dataElement, dataElement.label, 'databaseName', FUNCTIONAL_TEST)
        dataElement
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

    void 'S01 : test searching using research filter by the identifiable data and source system fields'() {
        when:
        POST("dataModels/${dataModelId}/search", [
            searchTerm    : "result",
            domainTypes   : ["DataElement"],
            labelOnly     : true,
            researchFields: [
                identifiableData: "Anonymous",
                sourceSystem    : "modules",
            ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().items.any {it.label == 'test_result'}
        responseBody().items.any {it.label == 'diagnosis_result'}

        POST("dataModels/${dataModelId}/search", [
            searchTerm    : "result",
            domainTypes   : ["DataElement"],
            labelOnly     : true,
            researchFields: [
                identifiableData: "Pseudonymised",
                sourceSystem    : "modules",
            ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().count == 0

        POST("dataModels/${dataModelId}/search", [
            searchTerm    : "result",
            domainTypes   : ["DataElement"],
            labelOnly     : true,
            researchFields: [
                identifiableData: "Anonymous",
                sourceSystem    : "data_product",
            ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().count == 0

        when:
        POST("dataModels/${dataModelId}/search", [
            searchTerm    : "name",
            domainTypes   : ["DataElement"],
            labelOnly     : true,
            researchFields: [
                identifiableData: "Identifying",
            ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items.any {it.label == 'name'}

        when:
        POST("dataModels/${dataModelId}/search", [
            searchTerm    : "date",
            domainTypes   : ["DataElement"],
            labelOnly     : true,
            researchFields: [
                identifiableData: "Maybe identifying",
            ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().items.any {it.label == 'test_date'}
        responseBody().items.any {it.label == 'diagnosis_date'}

        when:
        POST("dataModels/${dataModelId}/search", [
            searchTerm    : "date",
            domainTypes   : ["DataElement"],
            labelOnly     : true,
            researchFields: [
                identifiableData: "Identifying",
            ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items.any {it.label == 'date_of_birth'}
    }

    void 'S02 : test searching using research filter by the target dataset field'() {
        when:
        POST("dataModels/${dataModelId}/search", [
            searchTerm    : "result",
            domainTypes   : ["DataElement"],
            labelOnly     : true,
            researchFields: [
                targetDataset: "hic",
            ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().items.any {it.label == 'diagnosis_result'}
        responseBody().items.any {it.label == 'test_result'}

        when:
        POST("dataModels/${dataModelId}/search", [
            searchTerm    : "result",
            domainTypes   : ["DataElement"],
            labelOnly     : true,
            researchFields: [
                targetDataset: "ouh",
            ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().count == 0

        when:
        POST("dataModels/${dataModelId}/search", [
            searchTerm    : "name",
            domainTypes   : ["DataElement"],
            labelOnly     : true,
            researchFields: [
                targetDataset: "ouh",
            ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().count == 0
    }

    void 'S03 : test searching using research filter by the terminology field'() {
        when:
        POST("dataModels/${dataModelId}/search", [
            searchTerm    : "code",
            domainTypes   : ["DataElement"],
            labelOnly     : true,
            researchFields: [
                terminology: "snomed",
            ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items.any {it.label == 'test_code'}

        when:
        POST("dataModels/${dataModelId}/search", [
            searchTerm    : "code",
            domainTypes   : ["DataElement"],
            labelOnly     : true,
            researchFields: [
                terminology: "icd10",
            ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items.any {it.label == 'diagnosis_code'}
    }

    void 'S04 : test searching using research filter by the dataDictionaryItem field'() {
        when:
        POST("dataModels/${dataModelId}/search", [
            searchTerm    : "date",
            domainTypes   : ["DataElement"],
            labelOnly     : true,
            researchFields: [
                dataDictionaryItem: "",
            ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().count == 3
        responseBody().items.any {it.label == 'test_date'}
        responseBody().items.any {it.label == 'diagnosis_date'}
        responseBody().items.any {it.label == 'date_of_birth'}

        when:
        POST("dataModels/${dataModelId}/search", [
            searchTerm    : "date",
            domainTypes   : ["DataElement"],
            labelOnly     : true,
            researchFields: [
                dataDictionaryItem: "https://nhsdd/birthdate",
            ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items.any {it.label == 'date_of_birth'}
    }

    void 'S05 : test searching to include profile fields in results'() {
        when:
        POST("dataModels/${dataModelId}/profiles/${researchDataElementProfileProviderService.namespace}/${researchDataElementProfileProviderService.name}/search", [
            searchTerm    : "date",
            domainTypes   : ["DataElement"],
            labelOnly     : true,
            researchFields: [
                identifiableData: "identifying",
            ]
        ], STRING_ARG)

        then:
        verifyJsonResponse(OK, '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataElement",
      "label": "date_of_birth",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Search Functional Test Data Model",
          "domainType": "DataModel",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "patient",
          "domainType": "DataClass"
        }
      ],
      "profileFields": [
      {
        "fieldName":"Identifiable Data",
        "metadataPropertyName":"identifiableData",
        "currentValue":"Identifying",
        "dataType":"enumeration",
        "allowedValues":["Legally Restricted","Identifying","Pseudonymised","Maybe identifying","Anonymous"]
      },
      {
        "fieldName":"More details...",
        "metadataPropertyName":"identifiableDetails",
        "currentValue":"",
        "dataType":"text"
      },
      {
        "fieldName":"Source System",
        "metadataPropertyName":"sourceSystem",
        "currentValue":"modules",
        "dataType":"enumeration",
        "allowedValues":["ARIA (MedOnc)","ARIA (RadOnc)","Hicom","Orbit","Solus","SEND","Philips CareVue","BloodTrack","BadgerNet","InfoFlex","Janus","Xcelera","Xcelera Echo","ENDOBASE"]
      },
      {
        "fieldName":"Target Dataset",
        "metadataPropertyName":"targetDataset",
        "currentValue":"",
        "dataType":"enumeration",
        "allowedValues":["SUS","COSD","COSD Pathology","RTDS","SACT"]
      },
      {
        "fieldName":"Terminology",
        "metadataPropertyName":"terminology",
        "currentValue":"",
        "dataType":"enumeration",
        "allowedValues":["SNOMED-CT","ICD-9","ICD-10","OPCS"]
      },
      {
        "fieldName":"Data Dictionary Item",
        "metadataPropertyName":"dataDictionaryItem",
        "currentValue":"https://nhsdd/birthdate",
        "dataType":"string"
      },
      {
        "fieldName":"Sector of Care",
        "metadataPropertyName":"careSector",
        "currentValue":"",
        "dataType":"enumeration",
        "allowedValues":["Primary","Secondary","Tertiary"]
      },
      {
        "fieldName":"Admission Route",
        "metadataPropertyName":"admissionRoute",
        "currentValue":"",
        "dataType":"enumeration",
        "allowedValues":["Emergency","Maternity","In-patient","Out-patient"]
      },
      {
        "fieldName":"Last Update",
        "metadataPropertyName":"lastUpdated",
        "currentValue":"25/02/2022",
        "dataType":"date"
      },
      {
        "fieldName":"Database Name",
        "metadataPropertyName":"databaseName",
        "currentValue":"date_of_birth",
        "dataType":"string"
      }
    ]
  }
]
}''')

        when:
        POST("dataModels/${dataModelId}/profiles/${researchDataElementProfileProviderService.namespace}/${researchDataElementProfileProviderService.name}/search", [
            searchTerm    : "date",
            domainTypes   : ["DataElement"],
            labelOnly     : true,
            researchFields: [
                identifiableData: "Maybe identifying",
            ]
        ], STRING_ARG)

        then:
        verifyJsonResponse(OK, '''{
  "count": 2,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataElement",
      "label": "diagnosis_date",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Search Functional Test Data Model",
          "domainType": "DataModel",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "diagnosis",
          "domainType": "DataClass"
        }
      ],
      "profileFields": [
        {
          "fieldName": "Identifiable Data",
          "metadataPropertyName": "identifiableData",
          "currentValue": "Maybe identifying",
          "dataType": "enumeration",
          "allowedValues": [
            "Legally Restricted",
            "Identifying",
            "Pseudonymised",
            "Maybe identifying",
            "Anonymous"
          ]
        },
        {
          "fieldName": "More details...",
          "metadataPropertyName": "identifiableDetails",
          "currentValue": "",
          "dataType": "text"
        },
        {
          "fieldName": "Source System",
          "metadataPropertyName": "sourceSystem",
          "currentValue": "modules",
          "dataType": "enumeration",
          "allowedValues": [
            "ARIA (MedOnc)",
            "ARIA (RadOnc)",
            "Hicom",
            "Orbit",
            "Solus",
            "SEND",
            "Philips CareVue",
            "BloodTrack",
            "BadgerNet",
            "InfoFlex",
            "Janus",
            "Xcelera",
            "Xcelera Echo",
            "ENDOBASE"
          ]
        },
        {
          "fieldName": "Target Dataset",
          "metadataPropertyName": "targetDataset",
          "currentValue": "hic",
          "dataType": "enumeration",
          "allowedValues": [
            "SUS",
            "COSD",
            "COSD Pathology",
            "RTDS",
            "SACT"
          ]
        },
        {
          "fieldName": "Terminology",
          "metadataPropertyName": "terminology",
          "currentValue": "",
          "dataType": "enumeration",
          "allowedValues": [
            "SNOMED-CT",
            "ICD-9",
            "ICD-10",
            "OPCS"
          ]
        },
        {
          "fieldName": "Data Dictionary Item",
          "metadataPropertyName": "dataDictionaryItem",
          "currentValue": "",
          "dataType": "string"
        },
        {
          "fieldName": "Sector of Care",
          "metadataPropertyName": "careSector",
          "currentValue": "",
          "dataType": "enumeration",
          "allowedValues": [
            "Primary",
            "Secondary",
            "Tertiary"
          ]
        },
        {
          "fieldName": "Admission Route",
          "metadataPropertyName": "admissionRoute",
          "currentValue": "",
          "dataType": "enumeration",
          "allowedValues": [
            "Emergency",
            "Maternity",
            "In-patient",
            "Out-patient"
          ]
        },
        {
          "fieldName": "Last Update",
          "metadataPropertyName": "lastUpdated",
          "currentValue": "14/04/2022",
          "dataType": "date"
        },
        {
          "fieldName": "Database Name",
          "metadataPropertyName": "databaseName",
          "currentValue": "diagnosis_date",
          "dataType": "string"
        }
      ]
    },
    {
      "id": "${json-unit.matches:id}",
      "domainType": "DataElement",
      "label": "test_date",
      "model": "${json-unit.matches:id}",
      "breadcrumbs": [
        {
          "id": "${json-unit.matches:id}",
          "label": "Search Functional Test Data Model",
          "domainType": "DataModel",
          "finalised": false
        },
        {
          "id": "${json-unit.matches:id}",
          "label": "lab_test",
          "domainType": "DataClass"
        }
      ],
      "profileFields": [
        {
          "fieldName": "Identifiable Data",
          "metadataPropertyName": "identifiableData",
          "currentValue": "Maybe identifying",
          "dataType": "enumeration",
          "allowedValues": [
            "Legally Restricted",
            "Identifying",
            "Pseudonymised",
            "Maybe identifying",
            "Anonymous"
          ]
        },
        {
          "fieldName": "More details...",
          "metadataPropertyName": "identifiableDetails",
          "currentValue": "",
          "dataType": "text"
        },
        {
          "fieldName": "Source System",
          "metadataPropertyName": "sourceSystem",
          "currentValue": "modules",
          "dataType": "enumeration",
          "allowedValues": [
            "ARIA (MedOnc)",
            "ARIA (RadOnc)",
            "Hicom",
            "Orbit",
            "Solus",
            "SEND",
            "Philips CareVue",
            "BloodTrack",
            "BadgerNet",
            "InfoFlex",
            "Janus",
            "Xcelera",
            "Xcelera Echo",
            "ENDOBASE"
          ]
        },
        {
          "fieldName": "Target Dataset",
          "metadataPropertyName": "targetDataset",
          "currentValue": "hic",
          "dataType": "enumeration",
          "allowedValues": [
            "SUS",
            "COSD",
            "COSD Pathology",
            "RTDS",
            "SACT"
          ]
        },
        {
          "fieldName": "Terminology",
          "metadataPropertyName": "terminology",
          "currentValue": "",
          "dataType": "enumeration",
          "allowedValues": [
            "SNOMED-CT",
            "ICD-9",
            "ICD-10",
            "OPCS"
          ]
        },
        {
          "fieldName": "Data Dictionary Item",
          "metadataPropertyName": "dataDictionaryItem",
          "currentValue": "",
          "dataType": "string"
        },
        {
          "fieldName": "Sector of Care",
          "metadataPropertyName": "careSector",
          "currentValue": "",
          "dataType": "enumeration",
          "allowedValues": [
            "Primary",
            "Secondary",
            "Tertiary"
          ]
        },
        {
          "fieldName": "Admission Route",
          "metadataPropertyName": "admissionRoute",
          "currentValue": "",
          "dataType": "enumeration",
          "allowedValues": [
            "Emergency",
            "Maternity",
            "In-patient",
            "Out-patient"
          ]
        },
        {
          "fieldName": "Last Update",
          "metadataPropertyName": "lastUpdated",
          "currentValue": "12/03/2022",
          "dataType": "date"
        },
        {
          "fieldName": "Database Name",
          "metadataPropertyName": "databaseName",
          "currentValue": "test_date",
          "dataType": "string"
        }
      ]
    }
  ]
}
''')

    }
}
