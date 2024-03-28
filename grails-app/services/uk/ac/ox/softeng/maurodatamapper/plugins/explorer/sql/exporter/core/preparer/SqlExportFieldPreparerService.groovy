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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.preparer

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportCohortColumn
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportCohortTableOrView
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportColumn
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.reader.DataModelReaderService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.reader.ProfileReaderService

import org.springframework.beans.factory.annotation.Autowired

class SqlExportFieldPreparerService {
    @Autowired
    ProfileReaderService profileReaderService;

    /**
     * Return a list of all primary keys that exist on the cohort table
     * @param cohortTableOrView
     * @param cohortDataClass
     * @return
     */
    SqlExportCohortColumn[] getCohortPrimaryKeys(SqlExportCohortTableOrView cohortTableOrView, DataClass cohortDataClass) {
        if (!cohortDataClass) {
            return []
        }

        def primaryKeys = profileReaderService.getPrimaryKeys(cohortDataClass)

        (SqlExportCohortColumn[]) primaryKeys.collect { primaryKeyColumnName ->
            getCohortColumn(cohortTableOrView, cohortDataClass, primaryKeyColumnName, true)
        }.toArray()
    }

    /**
     * Find a column in the cohort table and return the SQL Server information about that column.
     * This information is:
     *  - The SQL Server data type
     *  - Whether it is a primary key or not
     * @param cohortTableOrView
     * @param cohortDataClass
     * @param columnName
     * @param primaryKey
     * @return
     */
    static SqlExportCohortColumn getCohortColumn(SqlExportCohortTableOrView cohortTableOrView, DataClass cohortDataClass, String columnName, Boolean primaryKey = false) {

        if (!cohortTableOrView) {
            return
        }

        if (!cohortDataClass) {
            return
        }

        def prefixedColumnName = getPrefixedColumnName(columnName, cohortDataClass)
        def dataElement = DataModelReaderService.getDataElement(cohortDataClass, columnName)

        return new SqlExportCohortColumn(prefixedColumnName, dataElement.dataType.label, primaryKey)

    }

    /**
     * Return a list of all columns prefixed with schema name and class name for a passed in SQL Export table
     * @param tableOrView
     * @return
     */
    static SqlExportColumn[] getSqlExportColumns(DataClass tableOrView) {
        if (!tableOrView) {
            return []
        }

        DataClass schema = tableOrView.parentDataClass

        if (!schema) {
            return []
        }

        (SqlExportColumn[]) tableOrView.dataElements.collect { dataElement ->
            new SqlExportColumn("[${schema.label}].[${tableOrView.label}].[${dataElement.label}]")
        }.toArray()

    }

    /**
     * Return a column name that is prefixed with the schema name and table name that the column belongs to.
     * i.e: [schemaName].[tableName].[columnName]
     * @param columnName
     * @param dataClass
     * @return
     */
    private static String getPrefixedColumnName(String columnName, DataClass dataClass) {
        if (!columnName) {
            return []
        }

        if (!dataClass) {
            return []
        }

        "[${dataClass.parentDataClass.label}].[${dataClass.label}].[${columnName}]"
    }
}
