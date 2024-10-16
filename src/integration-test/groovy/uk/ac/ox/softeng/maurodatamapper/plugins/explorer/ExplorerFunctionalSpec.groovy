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

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUserService
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getFUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.FORBIDDEN
import static io.micronaut.http.HttpStatus.NOT_FOUND
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR

/**
 * @see uk.ac.ox.softeng.maurodatamapper.plugins.explorer.ExplorerController*
 * | POST   | /api/explorer/userFolder      | Action: userFolder
 * |
 */
@Integration
@Slf4j
class ExplorerFunctionalSpec extends BaseFunctionalSpec {

    @Shared
    Folder rootFolder

    @Shared
    Folder childFolder

    @Shared
    DataModel rootDataModel

    ApiPropertyService apiPropertyService
    CatalogueUserService catalogueUserService

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        rootFolder = new Folder(label: 'Explorer Functional Folder', createdBy: FUNCTIONAL_TEST).save()
        childFolder = new Folder(label: 'fake root model', createdBy: FUNCTIONAL_TEST, parentFolder: rootFolder).save()
        rootDataModel = new DataModel(createdBy: FUNCTIONAL_TEST,
                                      label: 'Explorer Functional Root Data Model',
                                      folder: rootFolder,
                                      type: DataModelType.DATA_ASSET,
                                      authority: Authority.findByDefaultAuthority(true)).save()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec ExplorerFunctionalSpec')
        Folder.findByLabel('admin[at]maurodatamapper.com')?.delete(flush: true)
    }

    @Override
    String getResourcePath() {
        'explorer'
    }

    void logout() {
        log.trace('Logging out')
        GET('authentication/logout', MAP_ARG, true)
        verifyResponse(NO_CONTENT, response)
        currentCookie = null
    }

    HttpResponse<Map> loginUser(String emailAddress, String userPassword) {
        logout()
        log.trace('Logging in as {}', emailAddress)
        POST('authentication/login', [
            username: emailAddress,
            password: userPassword
        ], MAP_ARG, true)
        verifyResponse(OK, response)
        response
    }

    void updateApiProperty(String key, String value) {
        log.trace("Setting API property '${key}' to '${value}'")
        def adminUser = catalogueUserService.findByEmailAddress('admin@maurodatamapper.com')
        apiPropertyService.findAndUpdateByKey(key, value, adminUser)
    }

    void 'user folder: should be forbidden from getting folder when logged out'() {
        given:
        logout()

        when: 'create a folder'
        POST("/userFolder", [:], MAP_ARG)

        then:
        verifyResponse FORBIDDEN, response
    }

    void 'user folder: should create and get folder when logged in'() {
        given:
        loginUser('admin@maurodatamapper.com', 'password')

        when: 'create a folder'
        POST("/userFolder", [:], MAP_ARG)

        then:
        verifyResponse OK, response
        response.body().id
        response.body().label == 'admin[at]maurodatamapper.com'

        when: 'do it again'
        POST("/userFolder", [:], MAP_ARG)

        then:
        verifyResponse OK, response
        response.body().id
        response.body().label == 'admin[at]maurodatamapper.com'
    }

    void 'template folder: should be forbidden from getting folder when logged out'() {
        given:
        logout()

        when: 'get the folder'
        GET("/templateFolder")

        then:
        verifyResponse FORBIDDEN, response
    }

    void 'template folder: should get folder when logged in'() {
        given:
        loginUser('admin@maurodatamapper.com', 'password')

        when: 'get the folder'
        GET("/templateFolder")

        then:
        verifyResponse OK, response
        response.body().id
        response.body().label == 'Mauro Data Explorer Templates'
    }

    void 'shared specifications: Should return empty array if there are no data specifications'() {
        given:
        loginUser('admin@maurodatamapper.com', 'password')

        when: 'get shared specifications'
        GET("/sharedDataSpecifications")

        then:
        verifyResponse OK, response
        response.body().count == 0
        response.body().items == []
    }

    void 'shared specifications: Should return only shared specifications'() {
        given: 'There is an authenticated user, and there are 3 data specs, 2 of them being shared'
        loginUser('admin@maurodatamapper.com', 'password')
        def expectedLabelPrefix = 'Shared Data Specification'

        // Get data specs folder id
        GET('folders', MAP_ARG, true)
        verifyResponse OK, response
        def dataSpecFolder = response.body().items.find({it.label==('Mauro Data Explorer Data Specifications')})

        // Create new folder within
        def newFolderResponse = POST("folders/${dataSpecFolder.id}/folders", [
                label: 'test[at]test',
                description: 'new folder description'
        ], MAP_ARG, true)
        verifyResponse CREATED, newFolderResponse
        def newFolderId = newFolderResponse.body().id

        // Create 3 data specifications within the folder 2 of them shared
        for (i in 0..<3) {
            String dataModelLabel = expectedLabelPrefix + i
            boolean shared = true

            if(i==0){
                dataModelLabel = 'Not shared data specification'
                shared = false
            }
            String description = dataModelLabel + ' description'

            POST("folders/${newFolderId}/dataModels", [
                    label: dataModelLabel,
                    description: description,
                    modelType: 'DATA_STANDARD',
                    readableByAuthenticatedUsers: shared
            ], MAP_ARG, true)
            verifyResponse CREATED, response
        }

        when: 'getting shared specifications'
        GET("/sharedDataSpecifications")

        then: 'Only the 2 shared specifications are in the response'
        verifyResponse OK, response
        response.body().count == 2
        response.body().items[0].label.startsWith(expectedLabelPrefix)
        response.body().items[1].label.startsWith(expectedLabelPrefix)

        cleanup:
        DELETE("folders/${newFolderId}?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
    }

    void 'shared specification: should be forbidden from getting specifications when logged out'() {
        given:
        logout()

        when: 'listing shared data specifications'
        GET("/sharedDataSpecifications")

        then:
        verifyResponse FORBIDDEN, response
    }

    void 'list latest model data specification: should be forbidden when logged out'(){
        given:
        logout()

        when: 'listing data specifications'
        GET("/getLatestModelDataSpecifications")

        then:
        verifyResponse FORBIDDEN, response
    }

    void 'list latest model data specification: should return only the latest version'(){
        given: 'There is an authenticated user, and there are 2 data specs, 1 of them is the latest version'
        loginUser('admin@maurodatamapper.com', 'password')

        // Get data specs folder id
        POST("/userFolder", [:], MAP_ARG)
        verifyResponse OK, response
        def userFolderId = response.body().id;

        // Create data specifications
        def dataSpecCreateResponse = POST("folders/${userFolderId}/dataModels", [
                label: 'Plugin Explorer functional test',
                description: 'Plugin Explorer functional test',
                modelType: 'DATA_STANDARD',
        ], MAP_ARG, true)
        verifyResponse CREATED, response
        def newDataSpecId = dataSpecCreateResponse.body().id

        PUT("dataModels/${newDataSpecId}/finalise", [
                versionChangeType: 'Major'
        ], MAP_ARG, true)
        verifyResponse OK, response

        def newVersionResponse = PUT("dataModels/${newDataSpecId}/newBranchModelVersion", [
                asynchronous: 'false'
        ], MAP_ARG, true)
        verifyResponse CREATED, response

        def latestVersionId = newVersionResponse.body().id

        when: 'listing data specifications'
        GET("/getLatestModelDataSpecifications")

        then: 'Only the latest specifications is in the response'
        verifyResponse OK, response
        response.body().count == 1
        response.body().items[0].id == latestVersionId

        cleanup:
        DELETE("dataModels/${latestVersionId}?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response

        DELETE("dataModels/${newDataSpecId}?permanent=true", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
    }

    void 'template folder: should have correct securable resource group role'() {
        given:
        loginUser('admin@maurodatamapper.com', 'password')

        when: 'get the folder'
        GET("/templateFolder")

        then:
        verifyResponse OK, response
        def folder = response.body()

        when: 'get the securable resource group role'
        GET("${folder.domainType}/${folder.id}/securableResourceGroupRoles", MAP_ARG, true)

        then:
        verifyResponse OK, response
        //noinspection GroovyAssignabilityCheck
        def securableResource = response.body().items[0]
        securableResource.groupRole.name == 'reader'
        securableResource.userGroup.name == 'Explorer Readers'
    }

    void 'root data model: should be forbidden from getting model when logged out'() {
        given:
        logout()

        when: 'get the root data model'
        GET("/rootDataModel")

        then:
        verifyResponse FORBIDDEN, response
    }

    void 'root data model: should throw error if not configured'() {
        given:
        loginUser('admin@maurodatamapper.com', 'password')

        when: 'get the root data model'
        GET("/rootDataModel")

        then:
        verifyResponse INTERNAL_SERVER_ERROR, response
        response.body().exception.message.contains('has no value')
    }

    void 'root data model: should return not found if model is missing'() {
        given:
        loginUser('admin@maurodatamapper.com', 'password')
        updateApiProperty('explorer.config.root_data_model_path', "fo:${rootFolder.label}|dm:root model")

        when: 'get the root data model'
        GET("/rootDataModel")

        then:
        verifyResponse NOT_FOUND, response
    }

    void 'root data model: should throw error if not a data model'() {
        given:
        loginUser('admin@maurodatamapper.com', 'password')
        updateApiProperty('explorer.config.root_data_model_path', "fo:${rootFolder.label}|fo:${childFolder.label}")

        when: 'get the root data model'
        GET("/rootDataModel")

        then:
        verifyResponse INTERNAL_SERVER_ERROR, response
        response.body().exception.message.contains('is not a Data Model')
    }

    void 'root data model: should get root data model'() {
        given:
        loginUser('admin@maurodatamapper.com', 'password')
        updateApiProperty('explorer.config.root_data_model_path', "fo:${rootFolder.label}|dm:${rootDataModel.label}")

        when: 'get the root data model'
        GET("/rootDataModel")

        then:
        verifyResponse OK, response
        response.body().id
        response.body().label == rootDataModel.label
    }

    void 'theme: should return values when not logged in'() {
        given:
        logout()

        when: 'get the theme'
        GET("/theme")

        then:
        verifyResponse OK, response
    }

    void 'theme: should return values when logged in'() {
        given:
        loginUser('admin@maurodatamapper.com', 'password')

        when: 'get the theme'
        GET("/theme")

        then:
        verifyResponse OK, response
    }

    void 'theme: should return default theme from bootstrap'() {
        given:
        logout()

        when: 'get the theme'
        GET("/theme")

        then:
        verifyResponse OK, response

        def body = response.body()
        verifyProperty(body.items, 'material.colors.primary', '#003186')
        verifyProperty(body.items, 'material.colors.accent', '#5677B2')
        verifyProperty(body.items, 'material.colors.warn', '#a5122a')
        verifyProperty(body.items, 'material.typography.fontfamily', 'Roboto, "Helvetica Neue", sans-serif')
        verifyProperty(body.items, 'material.typography.bodyone', '14px, 20px, 400')
        verifyProperty(body.items, 'material.typography.bodytwo', '14px, 24px, 500')
        verifyProperty(body.items, 'material.typography.headline', '24px, 32px, 400')
        verifyProperty(body.items, 'material.typography.title', '20px, 32px, 500')
        verifyProperty(body.items, 'material.typography.subheadingtwo', '16px, 28px, 400')
        verifyProperty(body.items, 'material.typography.subheadingone', '15px, 24px, 400')
        verifyProperty(body.items, 'material.typography.button', '14px, 14px, 400')
        verifyProperty(body.items, 'regularcolors.hyperlink', '#003752')
        verifyProperty(body.items, 'regularcolors.data_specification_count', '#ffe603')
        verifyProperty(body.items, 'contrastcolors.page', '#fff')
        verifyProperty(body.items, 'contrastcolors.draft_data_specification', '#add8e6')
        verifyProperty(body.items, 'contrastcolors.finalised_data_specification', '#ffa500')
        verifyProperty(body.items, 'contrastcolors.submitted_data_specification', '#32cd32')
        verifyProperty(body.items, 'contrastcolors.classrow', '#c4c4c4')
        verifyProperty(body.items, 'images.header.logo', 'NOT SET')
    }

    void verifyProperty(props, key, expectedValue) {
        def prop = props.find { it -> it.key == key }
        assert prop.value == expectedValue
    }
}
