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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core;

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass;
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportCohortColumn
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportColumn;
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportData
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportTableOrView
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.reader.DataModelReaderService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.reader.ProfileReaderService;

import org.springframework.beans.factory.annotation.Autowired;

class SqlExportFieldService {
    @Autowired
    ProfileReaderService profileReaderService;

    void setCohortPrimaryKey(SqlExportData sqlExportData) {
        def primaryKeys = profileReaderService.getPrimaryKeys(sqlExportData.cohortDataClass)

        primaryKeys.each(primaryKeyColumnName -> {
            addColumnToCohort(sqlExportData, "${primaryKeyColumnName}", true)
        })

    }

    static void addColumnToCohort(SqlExportData sqlExportData, String columnName, Boolean primaryKey = false) {
        // If the table or view is not defined return without doing anything
        if (!sqlExportData.sqlExportTables.cohortTableOrView) {
            return
        }

        // Build the prefixed column name
        def schemaName = sqlExportData.cohortDataClass.parentDataClass.label
        def tableName = sqlExportData.cohortDataClass.label
        def prefixedColumnName = "[${schemaName}].[${tableName}].[${columnName}]"

        // If the column already exists then return without doing anything
        def columnExists = sqlExportData.sqlExportTables.cohortTableOrView.columns.find(column -> column.label == prefixedColumnName)
        if (columnExists) {
            return
        }

        // Add the column to the list of Cohort columns
        def dataElement = DataModelReaderService.getDataElement(sqlExportData.cohortDataClass, columnName)

        sqlExportData.sqlExportTables.cohortTableOrView.columns.push(
            new SqlExportCohortColumn(prefixedColumnName, sqlExportData.sqlExportTables.cohortTableOrView.columns.size(), dataElement.dataType.label, primaryKey))

    }

    static void addDataTableField(DataClass schema, DataClass tableOrView, SqlExportTableOrView sqlExportTableOrView) {
        tableOrView.dataElements.each((dataElement) -> {
            sqlExportTableOrView.columns.push(
                new SqlExportColumn("[${schema.label}].[${tableOrView.label}].[${dataElement.label}]", sqlExportTableOrView.columns.size()))
        })
    }
}
