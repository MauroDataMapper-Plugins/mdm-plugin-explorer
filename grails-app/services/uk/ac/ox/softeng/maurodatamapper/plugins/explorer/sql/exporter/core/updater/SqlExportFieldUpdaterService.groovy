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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.updater;

import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportCohortColumn
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportCohortTableOrView
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportColumn
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportTableOrView

class SqlExportFieldUpdaterService {

    /**
     * Add each primary key in the passed in list to the list of primary keys for the passed in table or view.
     * @param cohortTableOrView
     * @param cohortPrimaryKeys
     */
    static void setCohortPrimaryKey(SqlExportCohortTableOrView cohortTableOrView, SqlExportCohortColumn[] cohortPrimaryKeys) {
        cohortPrimaryKeys.each(sqlExportCohortColumn -> {
            setSqlExportColumns(cohortTableOrView, sqlExportCohortColumn)
        })
    }

    /**
     * Add each items in the passed in list to the list of columns for the passed in table or view.
     * @param sqlExportTableOrView
     * @param sqlExportColumns
     */
    static void setSqlExportColumns(SqlExportTableOrView sqlExportTableOrView, SqlExportColumn[] sqlExportColumns) {
        if (!sqlExportTableOrView) {
            return
        }

        sqlExportColumns.each((sqlExportColumn) -> {
            pushColumn(sqlExportTableOrView, sqlExportColumn)
        })
    }

    /**
     * Add a column to the passed in table or view, if the column does not already exist in that table or view.
     * @param sqlExportTableOrView
     * @param sqlExportColumn
     * @return
     */
    private static pushColumn(SqlExportTableOrView sqlExportTableOrView, SqlExportColumn sqlExportColumn) {
        // If the column already exists then return without doing anything
        def columnExists = sqlExportTableOrView.columns.find(column -> column.label == sqlExportColumn.label)
        if (columnExists) {
            return
        }

        sqlExportColumn.ordinal = sqlExportTableOrView.columns.size()
        sqlExportTableOrView.columns.push(sqlExportColumn)
    }
}
