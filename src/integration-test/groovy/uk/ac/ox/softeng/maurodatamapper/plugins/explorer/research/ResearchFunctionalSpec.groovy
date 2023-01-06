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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.research

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import spock.lang.Shared

import static io.micronaut.http.HttpStatus.FORBIDDEN
import static io.micronaut.http.HttpStatus.OK

/**
 * @see ResearchController
 *  | PUT   | /api/researchAccessRequest       | Action: submit   |
 */
@Integration
@Slf4j
class ResearchFunctionalSpec extends BaseFunctionalSpec {

    @Shared
    UUID dataModelId

    @Shared
    UUID folderId

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        sessionFactory.currentSession.flush()
        Folder folder = Folder.findByLabel('Functional Test Folder')
        folderId = folder.id
        assert folderId

        DataModel dataModel = DataModel.findByLabel('Functional Test Data Model')
        dataModelId = dataModel.id
        assert dataModelId
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec ResearchFunctionalSpec')
        cleanUpResources(DataModel, Folder)
    }

    @Override
    String getResourcePath() {
        'researchAccessRequest'
    }

    void 'test submit'() {
        when: 'submit the data model'
        PUT("/${dataModelId}", [:])

        then:
        verifyResponse OK, response

        when: 'submit the data model again'
        PUT("/${dataModelId}", [:])

        then:
        verifyResponse FORBIDDEN, response
        responseBody().additional == 'Cannot submit a finalised Model'
    }

    void 'test contact'() {
        when: 'submit a contact form'
        POST("contact", [
            firstName: "My first name",
            lastName: "My last name",
            organisation: "My organisation",
            subject: "My subject",
            message: "My message",
            emailAddress: "my.email@example.com"
        ], MAP_ARG, true)

        then:
        verifyResponse OK, response
    }
}
