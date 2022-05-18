/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.email.EmailService
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.rest.transport.Contact
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUserService
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.UserGroupService
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.version.VersionChangeType

import grails.artefact.controller.RestResponder
import grails.gorm.transactions.Transactional
import grails.web.api.WebAttributes
import groovy.util.logging.Slf4j

import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.SITE_URL

@Slf4j
class ResearchController implements ResourcelessMdmController, RestResponder, WebAttributes {

    ApiPropertyService apiPropertyService
    CatalogueUserService catalogueUserService
    DataModelService dataModelService
    EmailService emailService
    FolderService folderService
    UserGroupService userGroupService

    final String APPROVAL_RECIPIENT_KEY = 'email.research.request.recipient'
    final String APPROVAL_EMAIL_SUBJECT_KEY = 'email.research.request.subject'
    final String APPROVAL_EMAIL_BODY_KEY = 'email.research.request.body'
    final String CONTACT_RECIPIENT_KEY = 'email.research.contact.recipient'
    final String CONTACT_EMAIL_SUBJECT_KEY = 'email.research.contact.subject'
    final String CONTACT_EMAIL_BODY_KEY = 'email.research.contact.body'
    final String REQUEST_FOLDER = 'explorer.config.root_request_folder'

    /**
     * 'Submit' an access request by finalising the relevant Data Model and sending an email
     * to an approver.
     */
    @Transactional
    def submit() {
        DataModel dataModel = dataModelService.get(params.dataModelId)

        if (!dataModel) {
            notFound(params.dataModelId)
        }

        if (dataModel.finalised) return forbidden('Cannot submit a finalised Model')
        if (dataModel.deleted) return forbidden('Cannot submit a deleted Model')
        if (dataModel.branchName != VersionAwareConstraints.DEFAULT_BRANCH_NAME) return forbidden('Cannot submit a non-main branch')

        ApiProperty recipientEmailAddress = apiPropertyService.findByKey(APPROVAL_RECIPIENT_KEY)
        if (!recipientEmailAddress) throw new ApiInternalException("RC01", "API Property for APPROVAL_RECIPIENT_KEY ${APPROVAL_RECIPIENT_KEY} is " +
                                                                           "not configured")
        CatalogueUser recipientUser = catalogueUserService.findByEmailAddress(recipientEmailAddress.value)
        if (!recipientUser) throw new ApiInternalException("RC02", "Could not find user with email address ${recipientEmailAddress.value}")

        dataModel = dataModelService.finaliseModel(
            dataModel,
            currentUser,
            null,
            VersionChangeType.MAJOR,
            "Requested"
        )

        emailService.sendEmailToUser(siteUrl, APPROVAL_EMAIL_SUBJECT_KEY, APPROVAL_EMAIL_BODY_KEY, recipientUser, dataModel)

        dataModel
    }

    String getSiteUrl() {
        ApiProperty property = apiPropertyService.findByApiPropertyEnum(SITE_URL)
        property ? property.value : "${webRequest.baseUrl}${webRequest.contextPath}"
    }

    def contact(Contact contact) {
        ApiProperty recipientEmailAddress = apiPropertyService.findByKey(CONTACT_RECIPIENT_KEY)
        if (!recipientEmailAddress) throw new ApiInternalException("RC03", "Sorry, a configuration error prevented your request from being processed.")

        CatalogueUser recipientUser = catalogueUserService.findByEmailAddress(recipientEmailAddress.value)
        if (!recipientUser) throw new ApiInternalException("RC04", "Sorry, a configuration error prevented your request from being processed.")

        Map<String, String> propertiesMap = [
            firstName: contact.firstName ?: "Not provided",
            lastName: contact.lastName ?: "Not provided",
            organisation: contact.organisation ?: "Not provided",
            subject: contact.subject ?: "Not provided",
            message: contact.message ?: "Not provided",
            emailAddress: contact.emailAddress ?: "Not provided"
        ]
        emailService.sendEmailToUser(CONTACT_EMAIL_SUBJECT_KEY, CONTACT_EMAIL_BODY_KEY, recipientUser, propertiesMap)

        contact
    }

    /**
     * Get or create a user folder within the 'Explorer Content' folder.
     * 1. Use API Property to determine the parent folder which holds the individual user folders
     * 2. If it doesn't already exist, create a user group with name equal to the user's email address e.g. myemail@example.com
     * 3. If it doesn't already exists, create a folder called e.g. myemail[at]example.com. Add editor role for the folder to the user group
     */
    def userFolder() {
        ApiProperty requestFolderLabel = apiPropertyService.findByKey(REQUEST_FOLDER)
        if (!requestFolderLabel) throw new ApiInternalException("RC05", "API Property for REQUEST_FOLDER ${REQUEST_FOLDER} is " +
                                                                        "not configured")

        Folder requestFolder = folderService.findDomainByLabel(requestFolderLabel.value)
        if (!requestFolder) throw new ApiInternalException("RC06", "Folder ${requestFolderLabel.value} not available")

        // Create a user group from the user's email address if not already exists
        String userGroupName = currentUser.emailAddress
        UserGroup userGroup = userGroupService.findByName(userGroupName)

        if (!userGroup) {
            userGroup = userGroupService.generateAndSaveNewGroup(currentUser as CatalogueUser, userGroupName, 'User group for Explorer',
                                                                 [currentUser] as List<CatalogueUser>)
        }

        // Does user folder exist?
        String userFolderLabel = currentUser.emailAddress.replace("@", "[at]")
        Folder userFolder = folderService.findByParentIdAndLabel(requestFolder.id, userFolderLabel)

        if (!userFolder) {
            // Create a folder
            userFolder = new Folder(label: userFolderLabel, createdBy: currentUser.emailAddress)
            requestFolder.addToChildFolders(userFolder)

            folderService.save(requestFolder)

            // Give the user's group editor access on the folder
            userGroup.addToSecurableResourceGroupRoles(new SecurableResourceGroupRole(groupRole: GroupRole.findByName('editor'),
                                                                                      securableResourceDomainType: userFolder.domainType,
                                                                                      securableResourceId: userFolder.id,
                                                                                      createdBy: currentUser.emailAddress))

            userGroupService.save(userGroup)
        }

        respond(folder: userFolder, userSecurityPolicyManager: currentUserSecurityPolicyManager)
    }
}
