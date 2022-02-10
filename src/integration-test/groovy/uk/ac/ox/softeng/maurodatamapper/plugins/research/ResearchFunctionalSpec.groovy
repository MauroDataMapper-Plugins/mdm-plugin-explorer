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
package uk.ac.ox.softeng.maurodatamapper.plugins.research

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import spock.lang.Shared

import static io.micronaut.http.HttpStatus.OK
import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

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
        Folder folder = new Folder(label: 'Functional Test Folder',
                                   createdBy: FUNCTIONAL_TEST).save(flush: true)
        folderId = folder.id
        assert folderId

        DataModel dataModel = new DataModel(createdBy: FUNCTIONAL_TEST,
                                            label: 'Functional Test Data Model',
                                            folder: folder,
                                            type: DataModelType.DATA_ASSET,
                                            authority: Authority.findByDefaultAuthority(true)).save(flush: true)
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
        when:
        PUT("/${dataModelId}", [:])

        then:
        verifyResponse OK, response
    }
}
