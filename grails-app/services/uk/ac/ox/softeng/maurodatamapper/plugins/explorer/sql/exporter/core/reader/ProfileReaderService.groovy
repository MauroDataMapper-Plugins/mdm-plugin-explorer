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

import org.hibernate.SessionFactory
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.column.SqlServerColumnProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.table.SqlServerTableProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SchemaTablePair
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportForeignKeyProfileFields
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileField

import org.springframework.beans.factory.annotation.Autowired

class ProfileReaderService {

    @Autowired
    SqlServerColumnProfileProviderService sqlServerColumnProfileProviderService

    @Autowired
    SqlServerTableProfileProviderService sqlServerTableProfileProviderService

    @Autowired
    SessionFactory sessionFactory

    /**
     * Get foreign key profile fields
     * @param dataElement
     * @return
     */
    SqlExportForeignKeyProfileFields getForeignKeyProfileFields(DataElement dataElement) {
        def sqlProfile = sqlServerColumnProfileProviderService.createProfileFromEntity(dataElement)
        if (!(sqlProfile?.sections?.fields?.size() > 0)) {
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

    /**
     * Get a list of all foreign key data elements that are children of the passed in data classes
     * @param dataClassIds List of data class IDs to search for foreign key data elements
     * @return List of foreign key data element IDs
     */
    List<UUID> getIdsOfChildForeignKeyDataElements(List<UUID> dataClassIds, String coreSchema, String coreTable) {
        String hql = """
            SELECT de.id
            FROM DataElement de
            JOIN de.metadata md
            WHERE md.multiFacetAwareItemId = de.id
            AND md.multiFacetAwareItemDomainType = :domainType
            AND md.namespace = :namespace
            AND md.key IN (:keys)
            AND de.dataClass.id IN (:dataClassIds)
            AND (
                (md.key = 'foreign_key_schema' AND md.value = :coreSchema)
                OR (md.key = 'foreign_key_table' AND md.value = :coreTable)
            )
"""

        def session = sessionFactory.currentSession
        def query = session.createQuery(hql)
        query.setParameter("domainType", "DataElement")
        query.setParameter("namespace", "uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.column")
        query.setParameterList("keys", ["foreign_key_schema", "foreign_key_table", "foreign_key_columns"])
        query.setParameterList("dataClassIds", dataClassIds)
        query.setParameter("coreSchema", coreSchema)
        query.setParameter("coreTable", coreTable)

        query.list()
    }

    /**
     * Get the schema and table name for the core table of a data model
     * @param dataModelId The ID of the data model
     * @return The schema and table name
     */
    SchemaTablePair getQueryBuilderCoreTableProfileInfoForDataModel(UUID dataModelId) {
        String hql = """
            SELECT md.value
            FROM DataModel dm
            JOIN dm.metadata md
            WHERE md.multiFacetAwareItemId = dm.id
            AND md.multiFacetAwareItemDomainType = :domainType
            AND md.namespace = :namespace
            AND md.key = :key
            AND dm.id = :dataModelId
        """

        def session = sessionFactory.currentSession
        def query = session.createQuery(hql)
        query.setParameter("domainType", "DataModel")
        query.setParameter("namespace", 'uk.ac.ox.softeng.maurodatamapper.plugins.explorer.querybuilder')
        query.setParameter("key", 'queryBuilderCoreTable')
        query.setParameter("dataModelId", dataModelId)

        def result = query.uniqueResult() as String

        def parts = result.split('\\.')
        new SchemaTablePair(parts[0], parts[1])
    }
    /**
     * Get a list of all primary key names in a data class
     * @param dataClass
     * @return
     */
    String[] getPrimaryKeys(DataClass dataClass) {
        // Find the primary keys for the cohort table. We need to load a profile to find this information
        def sqlServerTableProfile = sqlServerTableProfileProviderService.createProfileFromEntity(dataClass)
        if (!(sqlServerTableProfile?.sections?.fields?.size() > 0)) {
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

    /**
     * Get a profile field that matches the passed in metadataPropertyName
     * @param profileFields
     * @param metadataPropertyName
     * @return
     */
    private static ProfileField getProfileField(List<ProfileField> profileFields, String metadataPropertyName) {
        return profileFields.find((profileField) -> {profileField.metadataPropertyName == metadataPropertyName})
    }

}
