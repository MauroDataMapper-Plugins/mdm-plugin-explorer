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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer

import uk.ac.ox.softeng.maurodatamapper.core.MdmCoreGrailsPlugin
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedSecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedUserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.policy.UserSecurityPolicyService
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRoleService
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityDefinition

import grails.core.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave

class BootStrap implements SecurityDefinition {

    @Autowired
    MessageSource messageSource
    GroupRoleService groupRoleService
    GroupBasedSecurityPolicyManagerService groupBasedSecurityPolicyManagerService
    UserSecurityPolicyService userSecurityPolicyService
    GrailsApplication grailsApplication

    def init = {servletContext ->
        log.debug('Starting main bootstrap')

        def explorerReadersGroupRoleName = 'Explorer Readers'

        CatalogueUser.withNewTransaction {
            admin = CatalogueUser.findByEmailAddress(StandardEmailAddress.ADMIN)
            UserGroup explorerReaders = UserGroup.findByName(explorerReadersGroupRoleName)
            if (!explorerReaders) {
                explorerReaders = new UserGroup(createdBy: StandardEmailAddress.ADMIN,
                                                name: explorerReadersGroupRoleName,
                                                undeleteable: false)
                    .addToGroupMembers(admin)
                checkAndSave(messageSource, explorerReaders)
            }
        }

        def apiPropertyCategory = 'Mauro Data Explorer'
        def requestFolderName = 'Mauro Data Explorer Requests'
        def templateFolderName = 'Mauro Data Explorer Templates'

        Folder.withNewTransaction {
            if (Folder.countByLabel(requestFolderName) == 0) {
                Folder folder = new Folder(label: requestFolderName, createdBy: StandardEmailAddress.ADMIN)
                checkAndSave(messageSource, folder)
            }
        }

        Folder.withNewTransaction {
            if (Folder.countByLabel(templateFolderName) == 0) {
                Folder folder = new Folder(label: templateFolderName, createdBy: StandardEmailAddress.ADMIN)
                checkAndSave(messageSource, folder)
            }
        }

        SecurableResourceGroupRole.withNewTransaction {
            UserGroup explorerReaders = UserGroup.findByName(explorerReadersGroupRoleName)
            def readerGroupRole = groupRoleService.getFromCache(GroupRole.READER_ROLE_NAME).groupRole
            def templateFolder = Folder.findByLabel(templateFolderName)

            if (SecurableResourceGroupRole.bySecurableResourceAndGroupRoleIdAndUserGroupId(templateFolder, readerGroupRole.id, explorerReaders.id).count() == 0) {
                checkAndSave(messageSource, new SecurableResourceGroupRole(
                    createdBy: StandardEmailAddress.ADMIN,
                    securableResource: templateFolder,
                    userGroup: explorerReaders,
                    groupRole: readerGroupRole
                ))
            }
        }

        Tuple<String>[] configDefaults = [
                new Tuple('explorer.config.root_data_model_path', 'NOT SET'),
                new Tuple('explorer.config.root_request_folder', requestFolderName),
                new Tuple('explorer.config.root_template_folder', templateFolderName),
                new Tuple('explorer.config.profile_namespace', 'uk.ac.ox.softeng.maurodatamapper.plugins.explorer.research'),
                new Tuple('explorer.config.profile_service_name', 'ResearchDataElementProfileProviderService'),

                new Tuple('explorer.theme.material.colors.primary','#19381f'),
                new Tuple('explorer.theme.material.colors.accent','#cdb980'),
                new Tuple('explorer.theme.material.colors.warn','#a5122a'),
                new Tuple('explorer.theme.material.typography.fontfamily','Roboto, "Helvetica Neue", sans-serif'),
                new Tuple('explorer.theme.material.typography.bodyone','14px, 20px, 400'),
                new Tuple('explorer.theme.material.typography.bodytwo','14px, 24px, 500'),
                new Tuple('explorer.theme.material.typography.headline','24px, 32px, 400'),
                new Tuple('explorer.theme.material.typography.title','20px, 32px, 500'),
                new Tuple('explorer.theme.material.typography.subheadingtwo','16px, 28px, 400'),
                new Tuple('explorer.theme.material.typography.subheadingone','15px, 24px, 400'),
                new Tuple('explorer.theme.material.typography.button','14px, 14px, 400'),
                new Tuple('explorer.theme.regularcolors.hyperlink','#003752'),
                new Tuple('explorer.theme.regularcolors.requestcount','#ffe603'),
                new Tuple('explorer.theme.contrastcolors.page','#fff'),
                new Tuple('explorer.theme.contrastcolors.unsentrequest','#008bce'),
                new Tuple('explorer.theme.contrastcolors.submittedrequest','#0e8f77'),
                new Tuple('explorer.theme.contrastcolors.classrow','#c4c4c4'),
                new Tuple('explorer.theme.images.header.logo','NOT SET'),
        ]

        ApiProperty.withNewTransaction {
            for (setting in configDefaults) {
                def (String key, String value) = setting

                if (ApiProperty.countByKey(key) == 0) {
                    ApiProperty rootPath = new ApiProperty(key: key,
                            value: value,
                            publiclyVisible: true,
                            category: apiPropertyCategory,
                            lastUpdatedBy: StandardEmailAddress.ADMIN,
                            createdBy: StandardEmailAddress.ADMIN)
                    checkAndSave(messageSource, rootPath)
                }
            }
        }

        log.debug('Main bootstrap complete')

        environments {
            test {
                //test requires bootstrapped user which can finalise a data model
                CatalogueUser.withNewTransaction {

                    createModernSecurityUsers('development', false)
                    checkAndSave(messageSource,
                                 editor,
                                 pending,
                                 containerAdmin,
                                 author,
                                 reviewer,
                                 reader,
                                 authenticated,
                                 creator)

                    createBasicGroups('development', false)
                    checkAndSave(messageSource,
                                 editors,
                                 readers,
                                 reviewers,
                                 authors,
                                 containerAdmins,
                                 admins)

                    Folder folder = new Folder(label: 'Functional Test Folder',
                                        createdBy: userEmailAddresses.development)
                    checkAndSave(messageSource, folder)

                    DataModel dataModel = new DataModel(createdBy: userEmailAddresses.development,
                                                        label: 'Functional Test Data Model',
                                                        folder: folder,
                                                        type: DataModelType.DATA_ASSET,
                                                        authority: Authority.findByDefaultAuthority(true))
                    checkAndSave(messageSource, dataModel)

                    createGroupedAccessFor(folder)
                    createGroupedAccessFor(dataModel)
                }

                GroupRole.withNewTransaction {
                    CatalogueUser unloggedInUser =
                        CatalogueUser.findByEmailAddress(UnloggedUser.UNLOGGED_EMAIL_ADDRESS) ?: CatalogueUser.fromInterface(UnloggedUser.instance)
                    unloggedInUser.tempPassword = null
                    unloggedInUser.save(flush: true)

                    unloggedInUser.addToGroups(admins)
                    unloggedInUser.addToGroups(editors)

                    GroupBasedUserSecurityPolicyManager defaultUserSecurityPolicyManager = grailsApplication.mainContext.getBean(
                        MdmCoreGrailsPlugin.DEFAULT_USER_SECURITY_POLICY_MANAGER_BEAN_NAME, GroupBasedUserSecurityPolicyManager)

                    defaultUserSecurityPolicyManager
                        .inApplication(grailsApplication)
                        .withUpdatedUserPolicy(userSecurityPolicyService.buildUserSecurityPolicy(unloggedInUser, unloggedInUser.groups))
                    groupBasedSecurityPolicyManagerService.storeUserSecurityPolicyManager(defaultUserSecurityPolicyManager)
                }

                ApiProperty.withNewTransaction {
                    ApiProperty recipient = new ApiProperty(key: 'email.research.request.recipient',
                                                            value: 'admin@maurodatamapper.com',
                                                            publiclyVisible: false,
                                                            category: 'Email',
                                                            lastUpdatedBy: userEmailAddresses.development,
                                                            createdBy: userEmailAddresses.development)
                    checkAndSave(messageSource, recipient)

                    ApiProperty contactRecipient = new ApiProperty(key: 'email.research.contact.recipient',
                                                            value: 'admin@maurodatamapper.com',
                                                            publiclyVisible: false,
                                                            category: 'Email',
                                                            lastUpdatedBy: userEmailAddresses.development,
                                                            createdBy: userEmailAddresses.development)
                    checkAndSave(messageSource, contactRecipient)
                }


            }
        }
        log.debug('test environment bootstrap complete')
    }
    def destroy = {
    }

    void createGroupedAccessFor(SecurableResource securableResource) {
        checkAndSave(messageSource, new SecurableResourceGroupRole(
            createdBy: userEmailAddresses.development,
            securableResource: securableResource,
            userGroup: containerAdmins,
            groupRole: groupRoleService.getFromCache(GroupRole.CONTAINER_ADMIN_ROLE_NAME).groupRole)
        )
        checkAndSave(messageSource, new SecurableResourceGroupRole(
            createdBy: userEmailAddresses.development,
            securableResource: securableResource,
            userGroup: editors,
            groupRole: groupRoleService.getFromCache(GroupRole.EDITOR_ROLE_NAME).groupRole)
        )
        checkAndSave(messageSource, new SecurableResourceGroupRole(
            createdBy: userEmailAddresses.development,
            securableResource: securableResource,
            userGroup: authors,
            groupRole: groupRoleService.getFromCache(GroupRole.AUTHOR_ROLE_NAME).groupRole)
        )
        checkAndSave(messageSource, new SecurableResourceGroupRole(
            createdBy: userEmailAddresses.development,
            securableResource: securableResource,
            userGroup: reviewers,
            groupRole: groupRoleService.getFromCache(GroupRole.REVIEWER_ROLE_NAME).groupRole)
        )
        checkAndSave(messageSource, new SecurableResourceGroupRole(
            createdBy: userEmailAddresses.development,
            securableResource: securableResource,
            userGroup: readers,
            groupRole: groupRoleService.getFromCache(GroupRole.READER_ROLE_NAME).groupRole)
        )
    }
}
