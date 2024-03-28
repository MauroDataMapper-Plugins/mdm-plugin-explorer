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
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRuleSet
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportCohortTableOrView
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportPairCohortTableAndCohortDataClass
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportPreparedJoins
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportTableOrView
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportTables
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.preparer.SqlExportFieldPreparerService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.preparer.SqlExportJoinPreparerService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.preparer.SqlExportRulePreparerService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.reader.DataModelReaderService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.reader.MeqlReaderService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.updater.SqlExportFieldUpdaterService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.updater.SqlExportJoinUpdaterService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.updater.SqlExportRuleUpdaterService

import org.springframework.beans.factory.annotation.Autowired

class SqlExportTableBuilderService {

    @Autowired
    SqlExportJoinPreparerService sqlExportJoinPreparerService

    @Autowired
    SqlExportFieldPreparerService sqlExportFieldPreparerService

    /**
     * Process the DataModel and massage the data within so that it is easily readable by
     * the templating engine
     * @param dataModel
     * @return
     */
    SqlExportTables prepareSqlExport(DataModel dataModel) {
        SqlExportTables sqlExportTables = new SqlExportTables()

        // Get a strongly typed model representation of the meql rules
        def (MeqlRuleSet cohortRuleSet, MeqlRuleSet dataRuleSet) = MeqlReaderService.getMeqlRuleSets(dataModel)

        // Build the export data sets
        def cohortPair = processCohortRuleSet(cohortRuleSet, dataModel)

        // Only set the cohortTableOrView and cohortDataClass if cohortPair has been defined.
        DataClass cohortDataClass = null
        if (cohortPair) {
            sqlExportTables.cohortTableOrView = cohortPair.sqlExportCohortTableOrView
            cohortDataClass = cohortPair.cohortDataClass
        }

        // Process the data tables
        processDataTables(dataRuleSet, dataModel, cohortDataClass, sqlExportTables)

        // Return the SqlExport data
        sqlExportTables
    }

    /**
     * Prepare the cohort table for export
     * @param cohortRuleSet
     * @param cohortTableOrView
     * @param dataModel
     * @return
     */
    private SqlExportPairCohortTableAndCohortDataClass processCohortRuleSet(MeqlRuleSet cohortRuleSet, DataModel dataModel) {
        if (!cohortRuleSet) {
            return
        }

        // First create cohort table
        def cohortDataClassNameParts = cohortRuleSet.entity.split('\\.');
        def schemaName = cohortDataClassNameParts[0]
        def tableName = cohortDataClassNameParts[1]
        SqlExportCohortTableOrView cohortTableOrView = new SqlExportCohortTableOrView(schemaName, tableName);

        // Get the cohort data class
        def cohortDataClass = DataModelReaderService.getDataClass(dataModel, schemaName, tableName)

        setCohortPrimaryKeys(cohortTableOrView, cohortDataClass)
        addCohortJoins(dataModel, cohortRuleSet, schemaName, cohortTableOrView, cohortDataClass)

        // Now add any rules (where clause)
        SqlExportRuleUpdaterService.addCohortRules(cohortRuleSet, cohortTableOrView)

        // Return the cohort Data Class
        new SqlExportPairCohortTableAndCohortDataClass(cohortTableOrView, cohortDataClass)
    }

    /**
     * Prepare the data tables for export
     * @param dataRuleSet
     * @param dataModel
     * @param cohortTableOrView
     * @param cohortDataClass
     * @return
     */
    private processDataTables(MeqlRuleSet dataRuleSet,
                              DataModel dataModel,
                              DataClass cohortDataClass,
                              SqlExportTables sqlExportTables) {
        // Now group and sort
        dataModel.childDataClasses.each {schema ->
            schema.dataClasses.each {dataClass ->
                def sqlExportTableOrView = new SqlExportTableOrView(schema.label, dataClass.label)

                // Add fields (select statement)
                addSqlExportColumns(dataClass, sqlExportTableOrView)

                // Add Join to cohort query (join statement)
                addDataTableJoinsToCohort(dataModel, sqlExportTableOrView, dataClass, sqlExportTables.cohortTableOrView, cohortDataClass)

                // Add Rules (where clause)
                addDataTableRule("$schema.label.$dataClass.label", dataRuleSet, sqlExportTableOrView)

                // Add the table to the things to be exported
                sqlExportTables.tableOrViews.add(sqlExportTableOrView)
            }

        }
    }

    /**
     * Set the primary key(s) on the cohort table
     * @param cohortTableOrView
     * @param cohortDataClass
     * @return
     */
    private setCohortPrimaryKeys(SqlExportCohortTableOrView cohortTableOrView, DataClass cohortDataClass) {
        def cohortPrimaryKeys = sqlExportFieldPreparerService.getCohortPrimaryKeys(cohortTableOrView, cohortDataClass )
        SqlExportFieldUpdaterService.setCohortPrimaryKey(cohortTableOrView, cohortPrimaryKeys)
    }

    /**
     * Copy column information from the tableOrView DataClass to the SqlExportTableOrView object
     * @param tableOrView
     * @param sqlExportTableOrView
     * @return
     */
    private addSqlExportColumns(DataClass tableOrView, SqlExportTableOrView sqlExportTableOrView) {
        def sqlExportColumns = sqlExportFieldPreparerService.getSqlExportColumns(tableOrView)
        SqlExportFieldUpdaterService.setSqlExportColumns(sqlExportTableOrView, sqlExportColumns)
    }

    /**
     * Add SQL joins that have been defined in cohort rules to the cohort export table
     * @param dataModel
     * @param cohortRuleSet
     * @param schemaName
     * @param cohortTableOrView
     * @return
     */
    private addCohortJoins(DataModel dataModel, MeqlRuleSet cohortRuleSet, String schemaName, SqlExportCohortTableOrView cohortTableOrView, DataClass cohortDataClass) {
        def preparedJoins = sqlExportJoinPreparerService.getCohortJoin(dataModel, cohortRuleSet, schemaName, cohortTableOrView)
        addJoins(preparedJoins, cohortTableOrView, cohortTableOrView, cohortDataClass)
     }

    /**
     * Add joins from each data table back to the cohort temporary table
     * @param dataModel
     * @param tableOrView
     * @param dataTableOrView
     * @param cohortTableOrView
     * @param cohortDataClass
     * @return
     */
    private addDataTableJoinsToCohort(DataModel dataModel,
                                      SqlExportTableOrView dataTableOrView,
                                      DataClass dataClass,
                                      SqlExportCohortTableOrView cohortTableOrView,
                                      DataClass cohortDataClass) {
        def preparedJoins = sqlExportJoinPreparerService.getDataTableJoinToCohort(dataModel, dataClass, cohortTableOrView, cohortDataClass)
        addJoins(preparedJoins, dataTableOrView, cohortTableOrView, cohortDataClass)
    }

    /**
     * Add joins to the passed in sqlExportTableOrView and also add the column names referenced in the join to the
     * list of columns that need to be included in the cohort temporary table
     * @param preparedJoins
     * @param sqlExportTableOrView
     * @param cohortTableOrView
     * @param cohortDataClass
     * @return
     */
    private addJoins(SqlExportPreparedJoins preparedJoins, SqlExportTableOrView sqlExportTableOrView, SqlExportCohortTableOrView cohortTableOrView, DataClass cohortDataClass) {
        if (!preparedJoins) {
            return
        }

        preparedJoins.sqlExportJoins.each(sqlExportJoin -> {
            SqlExportJoinUpdaterService.addJoinToTableOrView(sqlExportJoin, sqlExportTableOrView)
        })

        preparedJoins.cohortColumnNames.each(cohortColumnName -> {
            def cohortColumn = sqlExportFieldPreparerService.getCohortColumn(cohortTableOrView, cohortDataClass, cohortColumnName)
            SqlExportFieldUpdaterService.addColumnToCohort(cohortTableOrView, cohortColumn)
        })
    }

    /**
     * Add a rule to a DataTable. These rules are used to build the where clause in the generated SQL
     * @param entity
     * @param dataRuleSet
     * @param sqlExportTableOrView
     * @return
     */
    private static addDataTableRule(String entity, MeqlRuleSet dataRuleSet, SqlExportTableOrView sqlExportTableOrView) {
        def rule = SqlExportRulePreparerService.getRuleForEntity(entity, dataRuleSet)
        SqlExportRuleUpdaterService.addRule(rule, sqlExportTableOrView)
    }

}
