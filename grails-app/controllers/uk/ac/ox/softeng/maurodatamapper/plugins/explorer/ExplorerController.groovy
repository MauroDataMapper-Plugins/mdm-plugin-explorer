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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUserService
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.UserGroupService
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRoleService
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRoleService

import grails.artefact.DomainClass
import grails.artefact.controller.RestResponder
import grails.web.api.WebAttributes
import groovy.util.logging.Slf4j

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class ExplorerController implements ResourcelessMdmController, RestResponder, WebAttributes {

    ApiPropertyService apiPropertyService
    CatalogueUserService catalogueUserService
    DataModelService dataModelService
    FolderService folderService
    GroupRoleService groupRoleService
    UserGroupService userGroupService
    SecurableResourceGroupRoleService securableResourceGroupRoleService
    PathService pathService

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    final String CATEGORY = 'Mauro Data Explorer'
    final String THEME_PROPERTY_PREFIX = 'explorer.theme'
    final String DATA_SPECIFICATION_FOLDER = 'explorer.config.root_data_specification_folder'
    final String TEMPLATE_FOLDER = 'explorer.config.root_template_folder'
    final String ROOT_DATA_MODEL = 'explorer.config.root_data_model_path'

    /**
     * Get all properties required for theming the Mauro Data Explorer UI
     * @return An index list of key/value pairs
     */
    def theme() {
        List<ApiProperty> themeProperties = apiPropertyService
            .findAllByPubliclyVisible([:])
            .findAll {it.category == CATEGORY && it.key.startsWith(THEME_PROPERTY_PREFIX)}

        respond themeProperties, model: [userSecurityPolicyManager: currentUserSecurityPolicyManager]
    }

    /**
     * Get or create a user folder within the 'Explorer Content' folder.
     * 1. Use API Property to determine the parent folder which holds the individual user folders
     * 2. If it doesn't already exist, create a user group with name equal to the user's email address e.g. myemail@example.com
     * 3. If it doesn't already exists, create a folder called e.g. myemail[at]example.com. Add editor role for the folder to the user group
     */
    @Transactional
    def userFolder() {
        def userFolder = getOrCreateUserFolder()

        respond userFolder, view: '/folder/show', model: [folder: userFolder, userSecurityPolicyManager:
            currentUserSecurityPolicyManager]
    }

    /**
     * Get the root folder holding template data specifications
     * @return A Folder object
     */
    def templateFolder() {
        ApiProperty templateFolderLabel = apiPropertyService.findByKey(TEMPLATE_FOLDER)
        if (!templateFolderLabel) throw new ApiInternalException("RC05", "API Property for TEMPLATE_FOLDER ${TEMPLATE_FOLDER} is " +
                                                                        "not configured")

        Folder templateFolder = folderService.findDomainByLabel(templateFolderLabel.value)
        if (!templateFolder) throw new ApiInternalException("RC06", "Folder ${templateFolderLabel.value} not available")

        respond templateFolder, view: '/folder/show', model: [folder: templateFolder, userSecurityPolicyManager:
            currentUserSecurityPolicyManager]
    }

    /**
     * Get the data specifications that are shared with the community (i.e.
     * readable by any authenticated user).
     * @return A list of DataModel objects
     */
    def sharedDataSpecifications() {
        ApiProperty dataSpecificationFolderLabel = apiPropertyService.findByKey(DATA_SPECIFICATION_FOLDER)
        if (!dataSpecificationFolderLabel) throw new ApiInternalException("RC05", "API Property for DATA_SPECIFICATION_FOLDER ${DATA_SPECIFICATION_FOLDER} is " +
                "not configured")

        Folder dataSpecificationFolder = folderService.findDomainByLabel(dataSpecificationFolderLabel.value)
        if (!dataSpecificationFolder) throw new ApiInternalException("RC06", "Folder ${dataSpecificationFolderLabel.value} not available")

        def userFolders = dataSpecificationFolder.getChildFolders()

        def userFoldersIds = userFolders.collect{it.id}

        List<DataModel> sharedSpecifications = []

        // findAllByFolderIdInList will error if an empty list is passed
        if(userFoldersIds.size() >0 ){
            def allModelsFromFolders =  dataModelService.findAllByFolderIdInList(userFoldersIds)

            sharedSpecifications = allModelsFromFolders.findAll{it.readableByAuthenticatedUsers}
        }

        def lastModelVersionSharedSpecifications = getLatestModelFromListOfDataSpecification(sharedSpecifications)

        respond lastModelVersionSharedSpecifications, view: '/dataModel/index', model: [items: lastModelVersionSharedSpecifications, userSecurityPolicyManager: currentUserSecurityPolicyManager]
    }

    /**
     * Get the root data model for the explorer. This is defined by the API property 'explorer.config.root_data_model_path'
     * @return A data model resource
     */
    def rootDataModel() {
        ApiProperty rootDataModelPath = apiPropertyService.findByKey(ROOT_DATA_MODEL)
        if (!rootDataModelPath) throw new ApiInternalException("RC05", "API Property for ROOT_DATA_MODEL ${ROOT_DATA_MODEL} is " +
                                                                       "not configured")

        if (!rootDataModelPath.value || rootDataModelPath.value == 'NOT SET')
            throw new ApiInternalException("RC07", "API Property for ROOT_DATA_MODEL ${ROOT_DATA_MODEL} has no value")

        def pathAsString = rootDataModelPath.value

        // This gives us the opportunity to override the setting and reference an open root model
        // by including '?finalised=false' at the end of the configured path.
        def params = [:]

        if (pathAsString.contains("?")) {
            def pathAndParams = pathAsString.split("\\?")
            pathAsString = pathAndParams[0]

            def queryParams = pathAndParams[1].split("&")
            params = queryParams.collectEntries {
                param -> param.split("=").collect {
                    URLDecoder.decode(it, "UTF-8")
                }
            }
        }

        params.putIfAbsent('finalised', true)
        def path = Path.from(pathAsString)

        def rootModel = pathService.findResourceByPathFromRootClass(Folder, path, currentUserSecurityPolicyManager, params)

        if (!rootModel) return notFound(DomainClass, rootDataModelPath.value)

        if (rootModel.domainType != 'DataModel') throw new ApiInternalException("RC08", "ROOT_DATA_MODEL ${ROOT_DATA_MODEL} is not a Data Model")

        respond rootModel, view: '/dataModel/show', model: [dataModel: rootModel, userSecurityPolicyManager: currentUserSecurityPolicyManager]
    }

    def getLatestModelDataSpecifications(){
        def userFolder = getOrCreateUserFolder()

        def dataSpecifications = dataModelService.findAllByFolderId(userFolder.id)

        def latestModelDataSpecifications = getLatestModelFromListOfDataSpecification(dataSpecifications)

        respond latestModelDataSpecifications, view: '/dataModel/index', model: [items: latestModelDataSpecifications, userSecurityPolicyManager: currentUserSecurityPolicyManager]
    }

    private List<DataModel> getLatestModelFromListOfDataSpecification(List<DataModel> dataSpecificationList){

        // For each different label, find the latest model version.
        List<DataModel> lastModelVersionSharedSpecifications = []

        if(dataSpecificationList != null && dataSpecificationList.size() > 0) {
            List<String> uniqueSharedSpecNames = dataSpecificationList.collect { it.label }.unique();

            uniqueSharedSpecNames.forEach {
                def latestModel = dataModelService.findLatestModelByLabel(it)
                lastModelVersionSharedSpecifications.add(latestModel)
            }
        }

        return lastModelVersionSharedSpecifications;
    }

    private Folder getOrCreateUserFolder(){
        ApiProperty dataSpecificationFolderLabel = apiPropertyService.findByKey(DATA_SPECIFICATION_FOLDER)
        if (!dataSpecificationFolderLabel) throw new ApiInternalException("RC05", "API Property for DATA_SPECIFICATION_FOLDER ${DATA_SPECIFICATION_FOLDER} is " +
                "not configured")

        Folder dataSpecificationFolder = folderService.findDomainByLabel(dataSpecificationFolderLabel.value)
        if (!dataSpecificationFolder) throw new ApiInternalException("RC06", "Folder ${dataSpecificationFolderLabel.value} not available")

        // Does user folder exist?
        String userFolderLabel = currentUser.emailAddress.replace("@", "[at]")
        Folder userFolder = folderService.findByParentIdAndLabel(dataSpecificationFolder.id, userFolderLabel)

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
            userFolder = new Folder(label: userFolderLabel, createdBy: currentUser.emailAddress, parentFolder: dataSpecificationFolder)
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

        return userFolder
    }
}