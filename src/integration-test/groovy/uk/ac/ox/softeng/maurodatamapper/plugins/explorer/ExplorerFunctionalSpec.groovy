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

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import static io.micronaut.http.HttpStatus.FORBIDDEN
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

/**
 * @see uk.ac.ox.softeng.maurodatamapper.plugins.explorer.ExplorerController*
 * | POST   | /api/explorer/userFolder      | Action: userFolder
 * |
 */
@Integration
@Slf4j
class ExplorerFunctionalSpec extends BaseFunctionalSpec {


    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec ExplorerFunctionalSpec')
        Folder.findByLabel('admin[at]maurodatamapper.com').delete(flush: true)
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

    void 'test create and get user folder when logged out'() {
        given:
        logout()

        when: 'create a folder'
        POST("/userFolder", [:], MAP_ARG)

        then:
        verifyResponse FORBIDDEN, response
    }

    void 'test create and get user folder when logged in'() {
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
}
