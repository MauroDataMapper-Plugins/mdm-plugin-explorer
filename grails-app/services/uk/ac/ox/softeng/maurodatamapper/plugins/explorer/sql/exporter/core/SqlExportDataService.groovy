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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRuleSet
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportCohortTableOrView
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportData
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportTableOrView
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportTables
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.reader.DataModelReaderService

import org.springframework.beans.factory.annotation.Autowired

class SqlExportDataService {

    @Autowired
    MeqlService meqlService

    @Autowired
    SqlExportJoinService sqlExportJoinService

    @Autowired
    SqlExportFieldService sqlExportFieldService

    SqlExportTables prepareSqlExport(DataModel dataModel) {
        SqlExportTables sqlExportTables = new SqlExportTables()

        // Get a strongly typed model representation of the meql rules
        def (MeqlRuleSet cohortRuleGroup, MeqlRuleSet dataRuleGroup) = meqlService.getMeqlRuleGroups(dataModel)

        SqlExportData sqlExportData = new SqlExportData(dataModel, cohortRuleGroup, dataRuleGroup, sqlExportTables)

        // Build the export data sets
        processCohortRuleGroup(sqlExportData)
        processDataTables(sqlExportData)

        // Return the SqlExport data
        sqlExportTables
    }

    private processCohortRuleGroup(SqlExportData sqlExportData) {
        if (!sqlExportData.cohortRuleGroup) {
            return
        }

        // First create cohort table
        def cohortDataClassNameParts = sqlExportData.cohortRuleGroup.entity.split('\\.');
        def schemaName = cohortDataClassNameParts[0]
        def tableName = cohortDataClassNameParts[1]
        sqlExportData.sqlExportTables.cohortTableOrView = new SqlExportCohortTableOrView(schemaName, tableName);

        // Get the cohort data class
        sqlExportData.cohortDataClass = DataModelReaderService.getDataClass(sqlExportData.dataModel, schemaName, tableName)

        sqlExportFieldService.setCohortPrimaryKey(sqlExportData)
        sqlExportJoinService.addCohortJoins(sqlExportData, schemaName)

        // Now add any rules (where clause)
        SqlExportRuleService.addCohortRules(sqlExportData)
    }

    private processDataTables(SqlExportData sqlExportData) {
        // Now group and sort
        sqlExportData.dataModel.childDataClasses.each {schema ->
            schema.dataClasses.each {tableOrView ->
                def sqlExportTableOrView = new SqlExportTableOrView(schema.label, tableOrView.label)

                // Add fields (select statement)
                sqlExportFieldService.addDataTableField(schema, tableOrView, sqlExportTableOrView)

                // Add Join to cohort query (join statement)
                sqlExportJoinService.addDataTableJoins(schema, tableOrView, sqlExportTableOrView, sqlExportData)

                // Add Rules (where clause)
                SqlExportRuleService.addDataTableRule("$schema.label.$tableOrView.label", sqlExportData.dataRuleGroup, sqlExportTableOrView)

                // Add the table to the things to be exported
                sqlExportData.sqlExportTables.tableOrViews.add(sqlExportTableOrView)
            }

        }
    }

}
