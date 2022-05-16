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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.research

import uk.ac.ox.softeng.maurodatamapper.core.interceptor.TieredAccessSecurableResourceInterceptor
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.util.Utils

class ResearchInterceptor extends TieredAccessSecurableResourceInterceptor {

    @Override
    <S extends SecurableResource> Class<S> getSecuredClass() {
        DataModel as Class<S>
    }

    @Override
    void checkIds() {
        Utils.toUuid(params, 'dataModelId')
    }

    @Override
    UUID getId() {
        params.dataModelId
    }

    /**
     * submit is like finalise, so check user has write access for finalise
     * @return
     */
    boolean before() {

        // Contact form available for any user
        if (['contact'].contains(actionName)) {
            return true
        }

        securableResourceChecks()

        boolean canReadId = currentUserSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, getId())

        if (!currentUserSecurityPolicyManager.userCanWriteSecuredResourceId(getSecuredClass(), getId(), 'finalise')) {
            return forbiddenOrNotFound(canReadId, getSecuredClass(), getId())
        }

        true
    }
}
