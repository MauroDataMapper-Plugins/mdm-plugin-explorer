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

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRuleSet
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportCohortTableOrView
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportForeignKeyProfileFields
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportJoin
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportPairJoinAndCohortColumnNames
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportPairOnAndCohortColumnName
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportPreparedJoins
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.reader.DataModelReaderService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.reader.ProfileReaderService

import org.springframework.beans.factory.annotation.Autowired

class SqlExportJoinPreparerService {

    @Autowired
    ProfileReaderService profileReaderService

    /**
     *
     * @param dataModel
     * @param cohortRuleSet
     * @param schemaName
     * @param cohortTableOrView
     * @return
     */
    SqlExportPreparedJoins getCohortJoin(DataModel dataModel, MeqlRuleSet cohortRuleSet, String schemaName, SqlExportCohortTableOrView cohortTableOrView) {

        // Work out if we need to join to other tables
        def cohortRuleEntities = MeqlPreparerService.getDistinctEntitiesReferencedByRules(cohortRuleSet)

        // Remove the main entity, we don't want to join to itself
        cohortRuleEntities = cohortRuleEntities.findAll {entity -> entity != cohortRuleSet.entity}

        // If we have no entities to join to then return
        if (cohortRuleEntities.size() <= 0) {
            return
        }

        // Create a list of Joins to Return
        SqlExportPreparedJoins preparedJoins = new SqlExportPreparedJoins()

        // Gather foreign key references
        cohortRuleEntities.each(referenceEntity -> {
            def referenceSchemaLabel = referenceEntity.replace("$schemaName.", "")
            def referenceParts = referenceSchemaLabel.split('\\.')
            def referenceDataClass = DataModelReaderService.getDataClass(dataModel, referenceParts[0], referenceParts[1])

            def sqlExportJoinKeysPair = getJoinOnForeignKeyDataElements("[${referenceParts[0]}].[${referenceParts[1]}]",
                                            referenceDataClass,
                                            cohortTableOrView,
                                            true)

            populatePreparedJoins(preparedJoins, sqlExportJoinKeysPair)

        })

        preparedJoins
    }

    SqlExportPreparedJoins getDataTableJoinToCohort(DataModel dataModel,
                                                    DataClass dataTableOrView,
                                                    SqlExportCohortTableOrView cohortTableOrView,
                                                    DataClass cohortDataClass) {
        //If we don't have a cohort table we have nothing to join to so exit the function
        if (!cohortTableOrView) {
            return
        }

        // Get the schema
        DataClass schema = dataTableOrView.parentDataClass

        if (!schema) {
            return
        }

        // Create a list of Joins to Return
        SqlExportPreparedJoins preparedJoins = new SqlExportPreparedJoins()

        def referenceDataClass = DataModelReaderService.getDataClass(dataModel, schema.label, dataTableOrView.label)
        def sqlExportJoinKeysPair = getJoinOnPrimaryKeyOrForeignKey(
            "[${cohortTableOrView.tempTableName}]",
            referenceDataClass,
            cohortTableOrView,
            cohortDataClass)

        populatePreparedJoins(preparedJoins, sqlExportJoinKeysPair)
        preparedJoins
    }

    private SqlExportPairJoinAndCohortColumnNames getJoinOnForeignKeyDataElements(String tableName, DataClass dataClass, SqlExportCohortTableOrView cohortTableOrView, boolean buildingCohortQuery = false) {
        def joinAndColumnNamesPair = createJoinAndCohortColumnNamesPair(tableName)
        dataClass.dataElements.each(dataElement -> {
            def onAndColumnNamePair = createOnClauseForJoin(dataElement, cohortTableOrView, buildingCohortQuery)
            processOnAndCohortColumnNamePair(onAndColumnNamePair, joinAndColumnNamesPair)
        })
        joinAndColumnNamesPair
    }

    private static SqlExportPairJoinAndCohortColumnNames createJoinAndCohortColumnNamesPair(String tableName) {
        def sqlExportJoin = new SqlExportJoin(tableName)
        new SqlExportPairJoinAndCohortColumnNames(sqlExportJoin)
    }

    private static void populatePreparedJoins(SqlExportPreparedJoins preparedJoins, SqlExportPairJoinAndCohortColumnNames sqlExportJoinKeysPair) {
        preparedJoins.sqlExportJoins.push(sqlExportJoinKeysPair.sqlExportJoin)
        sqlExportJoinKeysPair.cohortColumnNames.each(cohortColumnName -> preparedJoins.cohortColumnNames.push(cohortColumnName))
    }

    // Push the "on" to the list of join ons and push the "key" to the list of cohort keys
    private static processOnAndCohortColumnNamePair(SqlExportPairOnAndCohortColumnName onColumnNamePair, SqlExportPairJoinAndCohortColumnNames joinKeysPair) {
        if (!joinKeysPair.sqlExportJoin) {
            return
        }

        if (!onColumnNamePair) {
            return
        }

        joinKeysPair.sqlExportJoin.on.push(onColumnNamePair.on)
        joinKeysPair.cohortColumnNames.push(onColumnNamePair.cohortColumnName)
    }

    private SqlExportPairJoinAndCohortColumnNames getJoinOnPrimaryKeyOrForeignKey(String tableName,
                                                                                  DataClass dataClass,
                                                                                  SqlExportCohortTableOrView cohortTableOrView,
                                                                                  DataClass cohortDataClass
                                                                                 ) {
        if (dataClass == cohortDataClass) {
            // We are selecting from the cohort table so join to the cohort temp table on primary key
            getJoinOnPrimaryKey(tableName,  cohortTableOrView, cohortDataClass)
        }
        else {
            // We are not the cohort table so use foreign keys
            return getJoinOnForeignKeyDataElements(tableName, dataClass, cohortTableOrView)
        }
    }

    private SqlExportPairOnAndCohortColumnName createOnClauseForJoin(DataElement dataElement,
                                                                     SqlExportCohortTableOrView cohortTableOrView,
                                                                     Boolean buildingCohortQuery = false) {
        // Get foreign keys from columns
        def foreignKeyProfileFields = profileReaderService.getForeignKeyProfileFields(dataElement)
        if (!foreignKeyProfileFields || !foreignKeyProfileFields?.columns?.currentValue) {
            return
        }

        String onString = (buildingCohortQuery)
            ? getJoinForCohort(dataElement, foreignKeyProfileFields)
            : getJoinToCohort(dataElement, foreignKeyProfileFields, cohortTableOrView)

        if (!onString) {
            return
        }

        return new SqlExportPairOnAndCohortColumnName(onString, foreignKeyProfileFields.columns?.currentValue)
    }

    private static SqlExportPairJoinAndCohortColumnNames getJoinOnPrimaryKey(String tableName,
                                                                             SqlExportCohortTableOrView cohortTableOrView,
                                                                             DataClass cohortDataClass) {
        def joinAndCohortColumnNamesPair = createJoinAndCohortColumnNamesPair(tableName)

       cohortTableOrView.columns
            .findAll(column -> column.primaryKey)
            .each(column -> {
                def dataElement = DataModelReaderService.getDataElement(cohortDataClass, column.labelColumnName)
                String onString = getJoinToCohortString(column.labelColumnName, dataElement, cohortTableOrView)
                if (onString) {
                    def onAndCohortColumnNamePair = new SqlExportPairOnAndCohortColumnName(onString, column.labelColumnName)
                    processOnAndCohortColumnNamePair(onAndCohortColumnNamePair, joinAndCohortColumnNamesPair)
                }
        })

        joinAndCohortColumnNamesPair
    }

    private static String getJoinToCohort(DataElement dataElement, SqlExportForeignKeyProfileFields foreignKeyProfileFields, SqlExportCohortTableOrView cohortTableOrView){
        if (!cohortTableOrView ||
            "[${foreignKeyProfileFields.schema.currentValue}].[${foreignKeyProfileFields.table.currentValue}]" != cohortTableOrView.label ||
            !foreignKeyProfileFields.columns.currentValue) {
            return null
        }

        getJoinToCohortString(foreignKeyProfileFields.columns.currentValue, dataElement, cohortTableOrView)
    }

    private static String getJoinForCohort(DataElement dataElement, SqlExportForeignKeyProfileFields foreignKeyProfileFields) {
        def foreignKeyColumn = "[${foreignKeyProfileFields.schema.currentValue}].[${foreignKeyProfileFields.table.currentValue}].[${foreignKeyProfileFields.columns.currentValue}]"
        def referenceKeyColumn =  "[${dataElement.dataClass.parentDataClass.label}].[${dataElement.dataClass.label}].[${dataElement.label}]"
        return "${referenceKeyColumn} = ${foreignKeyColumn}"
    }

    private static String getJoinToCohortString(String cohortColumn, DataElement dataElement, SqlExportCohortTableOrView cohortTableOrView) {
        "[${cohortTableOrView.tempTableName}].[${cohortColumn}] = [${dataElement.dataClass.parentDataClass.label}].[${dataElement.dataClass.label}].[${dataElement.label}]"
    }

}