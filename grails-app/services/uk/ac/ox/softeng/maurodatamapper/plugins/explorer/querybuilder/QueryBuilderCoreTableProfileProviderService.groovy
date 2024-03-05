/*
 * Copyright 2020-2023 University of Oxford and NHS England
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.querybuilder

import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileField
import uk.ac.ox.softeng.maurodatamapper.profile.object.JsonProfile
import uk.ac.ox.softeng.maurodatamapper.profile.provider.EmptyJsonProfileFactory
import uk.ac.ox.softeng.maurodatamapper.profile.provider.JsonProfileProviderService

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class QueryBuilderCoreTableProfileProviderService extends JsonProfileProviderService {

    private UUID dataModelId

    @Autowired
    DataModelService dataModelService


    @Override
    String getMetadataNamespace() {
        'uk.ac.ox.softeng.maurodatamapper.plugins.explorer.querybuilder'
    }

    @Override
    String getDisplayName() {
        'Mauro Data Explorer - Query Builder Core Table'
    }

    @Override
    String getVersion() {
        '1.0.0'
    }

    @Override
    String getJsonResourceFile() {
        return 'queryBuilderCoreTableProfile.json'
    }

    @Override
    List<String> profileApplicableForDomains() {
        return ['DataModel']
    }

    @Override
    JsonProfile createProfileFromEntity(MultiFacetAware entity) {
        dataModelId = entity.id
        super.createProfileFromEntity(entity)
    }

    @Override
    JsonProfile getNewProfile() {
        JsonProfile emptyProfile = EmptyJsonProfileFactory.instance.getEmptyProfile(this)
        emptyProfile.sections[0].customFieldsValidation {fields, errors ->
                String errorLabel = 'Query Builder core table'
                String errorField = 'queryBuilderCoreTable'
                String coreTable = fields.find {it.metadataPropertyName == 'queryBuilderCoreTable'}.currentValue
                String[] stringParts = coreTable.split('\\.')
                if (stringParts.size() != 2) return errors.rejectValue("fields[0].currentValue", 'schema.table.format.must.be.valid',
                                                                       new Object[]{'currentValue', ProfileField.simpleName, null, errorLabel, errorField},
                                                                       'Format needs to be [Schema].[Table] e.g: people.patient')
                String schemaName = stringParts[0]
                String tableName = stringParts[1]

                DataModel dataModel = dataModelService.get(dataModelId)
                if (dataModel) {
                    DataClass dataSchema = dataModel.dataClasses.find({it.label == schemaName})
                    if (dataSchema) {
                        DataClass dataTable = dataSchema.dataClasses.find({it.label == tableName})
                        if (!dataTable) {
                            errors.rejectValue("fields[0].currentValue", 'table.must.belong.to.schema',
                                               new Object[]{'currentValue', 'ProfileField.simpleName', null, errorLabel, errorField},
                                               "Table \"${tableName}\" cannot be found in schema \"${schemaName}\"")
                        }
                    } else {
                        errors.rejectValue("fields[0].currentValue", 'schema.must.belong.to.datamodel',
                                           new Object[]{'currentValue', 'ProfileField.simpleName', null, errorLabel, errorField},
                                           "Schema \"${schemaName}\" cannot be found")
                    }
                }
                else {
                    // We should never reach this point
                    errors.rejectValue("fields[0].currentValue", 'datamodel.not.found',
                                       new Object[]{'currentValue', 'ProfileField.simpleName', null, errorLabel, errorField},
                                       "DataModel \"${dataModelId}\" cannot be found")
                }

                return
            }

        emptyProfile
    }
}
