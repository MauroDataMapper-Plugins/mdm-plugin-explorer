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
        verifyProperty(body.items, 'material.colors.primary', '#19381f')
        verifyProperty(body.items, 'material.colors.accent', '#cdb980')
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
        verifyProperty(body.items, 'contrastcolors.unsent_data_specification', '#008bce')
        verifyProperty(body.items, 'contrastcolors.submitted_data_specification', '#0e8f77')
        verifyProperty(body.items, 'contrastcolors.classrow', '#c4c4c4')
        verifyProperty(body.items, 'images.header.logo', 'NOT SET')
    }

    void verifyProperty(props, key, expectedValue) {
        def prop = props.find { it -> it.key == key }
        assert prop.value == expectedValue
    }
}
