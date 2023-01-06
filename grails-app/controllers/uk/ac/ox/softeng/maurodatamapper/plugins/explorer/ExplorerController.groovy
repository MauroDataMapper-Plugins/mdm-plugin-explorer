/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUserService
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.UserGroupService
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRoleService
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRoleService

import grails.artefact.controller.RestResponder
import grails.web.api.WebAttributes
import groovy.util.logging.Slf4j

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class ExplorerController implements ResourcelessMdmController, RestResponder, WebAttributes {

    ApiPropertyService apiPropertyService
    CatalogueUserService catalogueUserService
    FolderService folderService
    GroupRoleService groupRoleService
    UserGroupService userGroupService
    SecurableResourceGroupRoleService securableResourceGroupRoleService

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    final String REQUEST_FOLDER = 'explorer.config.root_request_folder'

    /**
     * Get or create a user folder within the 'Explorer Content' folder.
     * 1. Use API Property to determine the parent folder which holds the individual user folders
     * 2. If it doesn't already exist, create a user group with name equal to the user's email address e.g. myemail@example.com
     * 3. If it doesn't already exists, create a folder called e.g. myemail[at]example.com. Add editor role for the folder to the user group
     */
    @Transactional
    def userFolder() {
        ApiProperty requestFolderLabel = apiPropertyService.findByKey(REQUEST_FOLDER)
        if (!requestFolderLabel) throw new ApiInternalException("RC05", "API Property for REQUEST_FOLDER ${REQUEST_FOLDER} is " +
                                                                        "not configured")

        Folder requestFolder = folderService.findDomainByLabel(requestFolderLabel.value)
        if (!requestFolder) throw new ApiInternalException("RC06", "Folder ${requestFolderLabel.value} not available")

        // Does user folder exist?
        String userFolderLabel = currentUser.emailAddress.replace("@", "[at]")
        Folder userFolder = folderService.findByParentIdAndLabel(requestFolder.id, userFolderLabel)

        if (!userFolder) {
            // Create a user group from the user's email address if not already exists
            String userGroupName = "${userFolderLabel} Explorer Group"
            UserGroup userGroup = userGroupService.findByName(userGroupName)
            CatalogueUser catalogueUser = catalogueUserService.get(currentUser.id)

            if (!userGroup) {
                // New user group is saved and flushed
                userGroup = userGroupService.generateAndSaveNewGroup(catalogueUser, userGroupName, 'User group for Explorer')
            }

            // Create a folder
            userFolder = new Folder(label: userFolderLabel, createdBy: currentUser.emailAddress, parentFolder: requestFolder)
            folderService.save(userFolder)

            securableResourceGroupRoleService.createAndSaveSecurableResourceGroupRole(
                userFolder,
                groupRoleService.getFromCache(GroupRole.EDITOR_ROLE_NAME).groupRole,
                userGroup,
                catalogueUser)

            if (securityPolicyManagerService) {
                currentUserSecurityPolicyManager =
                    securityPolicyManagerService.addSecurityForSecurableResource(userFolder, currentUser, userFolder.label)
            }
        }

        respond userFolder, view: '/folder/show', model: [folder: userFolder, userSecurityPolicyManager:
            currentUserSecurityPolicyManager]
    }
}
