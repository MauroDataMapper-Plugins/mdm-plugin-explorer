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

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.version.VersionChangeType

@Slf4j
class ResearchController implements ResourcelessMdmController {

    DataModelService dataModelService

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

        dataModel = dataModelService.finaliseModel(
                dataModel,
                currentUser,
                null,
                VersionChangeType.MAJOR,
                "Requested"
        )

        dataModel
    }
}
