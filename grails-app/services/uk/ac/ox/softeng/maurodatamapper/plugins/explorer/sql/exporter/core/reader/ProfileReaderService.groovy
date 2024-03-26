/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.reader

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.column.SqlServerColumnProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.table.SqlServerTableProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportForeignKeyProfileFields
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileField

import org.springframework.beans.factory.annotation.Autowired

class ProfileReaderService {

    @Autowired
    SqlServerColumnProfileProviderService sqlServerColumnProfileProviderService

    @Autowired
    SqlServerTableProfileProviderService sqlServerTableProfileProviderService

    SqlExportForeignKeyProfileFields getForeignKeyProfileFields(DataElement dataElement) {
        def sqlProfile = sqlServerColumnProfileProviderService.createProfileFromEntity(dataElement)
        if (sqlProfile?.sections?.fields?.size() <= 0) {
            return null
        }

        def sqlProfileFields = sqlProfile.sections.fields[0]
        def foreignKeySchema = getProfileField(sqlProfileFields, "foreign_key_schema")
        def foreignKeyTable = getProfileField(sqlProfileFields, "foreign_key_table")
        def foreignKeyColumns = getProfileField(sqlProfileFields, "foreign_key_columns")

        if (!foreignKeySchema || !foreignKeyTable || !foreignKeyColumns) {
            return null
        }

        new SqlExportForeignKeyProfileFields(foreignKeySchema, foreignKeyTable, foreignKeyColumns)
    }

    String[] getPrimaryKeys(DataClass dataClass) {
        // Find the primary keys for the cohort table. We need to load a profile to find this information
        def sqlServerTableProfile = sqlServerTableProfileProviderService.createProfileFromEntity(dataClass)
        if (sqlServerTableProfile?.sections?.fields?.size() <= 0) {
            return []
        }

        def sqlProfileFields = sqlServerTableProfile.sections.fields[0]
        def primaryKeys = getProfileField(sqlProfileFields, "primary_key_columns");
        if (!primaryKeys.currentValue) {
            return []
        }

        def primaryKeysList = primaryKeys.currentValue.split(',')
        return primaryKeysList
    }

    private static ProfileField getProfileField(List<ProfileField> profileFields, String metadataPropertyName) {
        return profileFields.find((profileField) -> {profileField.metadataPropertyName == metadataPropertyName})
    }
}
