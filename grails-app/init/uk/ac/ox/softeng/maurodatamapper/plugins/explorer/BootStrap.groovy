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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer

import uk.ac.ox.softeng.maurodatamapper.core.MdmCoreGrailsPlugin
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
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
