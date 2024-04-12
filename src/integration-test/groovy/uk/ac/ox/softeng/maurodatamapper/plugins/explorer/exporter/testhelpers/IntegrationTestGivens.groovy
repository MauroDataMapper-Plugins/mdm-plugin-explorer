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

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.rule.RuleRepresentation
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.profile.ProfileService
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileField
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection
import uk.ac.ox.softeng.maurodatamapper.profile.object.Profile
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.test.unit.security.TestUser
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.validation.ValidationException
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.spockframework.util.Assert
import org.springframework.context.MessageSource

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getFUNCTIONAL_TEST

@Slf4j
class IntegrationTestGivens {
    private MessageSource messageSource
    private ProfileService profileService

    IntegrationTestGivens(MessageSource messageSource, ProfileService profileService) {
        this.messageSource = messageSource
        this.profileService = profileService
    }

    User "there is a user"(String emailAddress = FUNCTIONAL_TEST, String firstName = 'Test', String lastName = 'User') {
        User user = new TestUser(emailAddress: emailAddress, firstName: firstName, lastName: lastName, id: UUID.randomUUID())
        user
    }

    Folder "there is a folder"(String label) {
        Folder folder = Folder.findByLabel(label)
        if (folder) {
            return folder
        }

        log.debug("Creating folder '$label'")
        folder = new Folder(label: label, createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)

        folder
    }

    DataModel "there is a data model"(String label, Folder folder) {
        DataModel dataModel = DataModel.findByLabel(label)
        if (dataModel) {
            return dataModel
        }

        log.debug("Creating datamodel '$label'")
        Authority authority = Authority.findByDefaultAuthority(true)

        dataModel = new DataModel(createdBy: FUNCTIONAL_TEST, label: label, folder: folder, authority: authority)
        checkAndSave(dataModel)

        dataModel
    }

    DataClass "there is a data class"(String label, DataModel dataModel, DataClass parentDataClass = null) {
        DataClass dataClass = DataClass.findByLabel(label)
        if (dataClass) {
            return dataClass
        }

        log.debug("Creating dataclass '$label'")
        dataClass = new DataClass(createdBy: FUNCTIONAL_TEST, label: label, dataModel: dataModel, parentDataClass: parentDataClass)

        if (!dataModel.dataClasses) {
            dataModel.dataClasses = new LinkedHashSet<>()
        }

        dataModel.dataClasses.add(dataClass)

        if (parentDataClass) {
            if (!parentDataClass.dataClasses) {
                parentDataClass.dataClasses = new LinkedHashSet<>()
            }

            parentDataClass.dataClasses.add(dataClass)
        }

        checkAndSave(dataClass)

        dataClass
    }

    DataElement "there is a data element"(String label, DataModel dataModel, DataClass dataClass, DataType dataType) {
        DataElement dataElement = DataElement.findByLabel(label)
        if (dataElement) {
            dataElement
        }

        log.debug("Creating dataelement '$label'")
        dataElement = new DataElement(createdBy: FUNCTIONAL_TEST, label: label, dataModel: dataModel, dataClass: dataClass, dataType: dataType)

        if (!dataClass.dataElements) {
            dataClass.dataElements = new LinkedHashSet<>()
        }

        dataClass.dataElements.add(dataElement)

        checkAndSave(dataElement)

        dataElement
    }

    DataType "there is a primitive data type"(String label, DataModel dataModel) {
        PrimitiveType dataType = PrimitiveType.findByLabel(label)
        if (dataType) {
            return dataType
        }

        dataType = new PrimitiveType(createdBy: FUNCTIONAL_TEST, label: label, dataModel: dataModel)

        dataModel.addToDataTypes(dataType)

        checkAndSave(dataType)

        dataType
    }

    DataType "there is a reference data type"(String label, DataModel dataModel, DataClass referenceClass) {
        ReferenceType dataType = ReferenceType.findByLabel(label)
        if (dataType) {
            return dataType
        }

        dataType = new ReferenceType(createdBy: FUNCTIONAL_TEST, label: label, dataModel: dataModel, referenceClass: referenceClass)

        dataModel.addToDataTypes(dataType)

        checkAndSave(dataType)

        dataType
    }

    Rule "there is a rule with a representation"(String label, DataModel dataModel, String language, String representation) {

        def rule = new Rule()
        rule.name = label

        def ruleRepresentation = new RuleRepresentation()
        ruleRepresentation.language = language
        ruleRepresentation.representation = representation
        ruleRepresentation.rule = rule
        ruleRepresentation.createdBy = 'test-user@test.com'

        rule.ruleRepresentations = []
        rule.ruleRepresentations.add(ruleRepresentation)

        if (!dataModel.rules) {
            dataModel.rules = []
        }
        dataModel.rules.add(rule)
        rule
    }

    ProfileField "a profile field value is set"(ProfileSection profileSection, String metadataPropertyName, String value) {
        def profileField = profileSection.find {field -> field.metadataPropertyName == metadataPropertyName }
        if (profileField) {
            profileField.currentValue = value
        }

        profileField
    }

    Profile "there is a data element sql column profile"(
        DataElement entity,
        User user,
        @ClosureParams(
            value = SimpleType.class,
            options = 'uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection'
        ) Closure modifyFields) {
        updateAndStoreProfile(
            "uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.column",
            "SqlServerColumnProfileProviderService",
            "Column Properties",
            entity,
            user,
            modifyFields)
    }

    Profile "there is a data class sql table profile"(
        DataClass entity,
        User user,
        @ClosureParams(
            value = SimpleType.class,
            options = 'uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection'
        ) Closure modifyFields) {
        updateAndStoreProfile(
            "uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.table",
            "SqlServerTableProfileProviderService",
            "Table / View Properties",
            entity,
            user,
            modifyFields)
    }

    Profile "there is a profile identifying a foreign key"(DataElement entity,
                                                                    User user,
                                                                    String foreignKeySchemaName,
                                                                    String foreignKeyTableName,
                                                                    String foreignKeyColumnName) {
        "there is a data element sql column profile"(entity, user, { section ->
            "a profile field value is set"(section, "column_name",entity.label)
            "a profile field value is set"(section, "foreign_key_schema",foreignKeySchemaName)
            "a profile field value is set"(section, "foreign_key_table",foreignKeyTableName)
            "a profile field value is set"(section, "foreign_key_columns",foreignKeyColumnName)
        })
    }

    private Profile updateAndStoreProfile(
        String providerNamespace,
        String providerName,
        String sectionName,
        MultiFacetAware entity,
        User user,
        @ClosureParams(
            value = SimpleType.class,
            options = 'uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection'
        ) Closure modifyFields) {
        def profileProvider = profileService.findProfileProviderService(providerNamespace, providerName)
        Profile profile = profileService.createProfile(profileProvider, entity)
        ProfileSection section = profile.sections.find {sec -> sec.name == sectionName }
        modifyFields(section)
        profileService.storeProfile(profileProvider, entity, profile, user)
    }

    void checkAndSave(GormEntity domainObj) {
        try {
            GormUtils.checkAndSave(messageSource, domainObj)
        } catch (ValidationException ex) {
            Assert.fail(ex.message)
        }
    }
}
