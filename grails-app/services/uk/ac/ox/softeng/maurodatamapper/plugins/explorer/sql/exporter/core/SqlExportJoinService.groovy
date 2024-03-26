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

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportData
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportForeignKeyProfileFields
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportJoin
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportRule
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportTableOrView
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.reader.DataModelReaderService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.reader.ProfileReaderService

import groovy.json.JsonOutput
import org.springframework.beans.factory.annotation.Autowired

class SqlExportJoinService {

    @Autowired
    SqlExportFieldService sqlExportFieldService

    @Autowired
    ProfileReaderService profileReaderService

    def addCohortJoins(SqlExportData sqlExportData, String schemaName) {
        // Work out if we need to join to other tables
        def cohortRuleEntities = MeqlService.getDistinctEntities(sqlExportData.cohortRuleGroup)

        // Remove the main entity, we don't want to join to itself
        cohortRuleEntities = cohortRuleEntities.findAll {entity -> entity != sqlExportData.cohortRuleGroup.entity}

        // If we have no entities to join to then return
        if (cohortRuleEntities.size() <= 0) {
            return
        }

        // Gather foreign key references
        cohortRuleEntities.each(referenceEntity -> {
            def referenceSchemaLabel = referenceEntity.replace("$schemaName.", "")
            def referenceParts = referenceSchemaLabel.split('\\.')
            def referenceDataClass = DataModelReaderService.getDataClass(sqlExportData.dataModel, referenceParts[0], referenceParts[1])

            createJoin("[${referenceParts[0]}].[${referenceParts[1]}]",
                       sqlExportData.sqlExportTables.cohortTableOrView,
                       referenceDataClass,
                       sqlExportData,
                       true)

        })
    }

    def addDataTableJoins(DataClass schema, DataClass tableOrView, SqlExportTableOrView sqlExportTableOrView, SqlExportData sqlExportData) {
        //If we don't have a cohort table we have nothing to join to so exit the function
        if (!sqlExportData.sqlExportTables.cohortTableOrView) {
            return
        }

        def referenceDataClass = DataModelReaderService.getDataClass(sqlExportData.dataModel, schema.label, tableOrView.label)
        createJoin("[${sqlExportData.sqlExportTables.cohortTableOrView.tempTableName}]",
                   sqlExportTableOrView,
                   referenceDataClass,
                   sqlExportData,
                   false)
    }

    private void createJoin(String tableName, SqlExportTableOrView sqlExportTableOrView, DataClass dataClass, SqlExportData sqlExportData, boolean buildingCohortQuery = false) {
        def sqlExportJoin = new SqlExportJoin(tableName)
        switch (buildingCohortQuery) {
            case true:
                joinOnForeignKeyDataElements(dataClass, sqlExportJoin, sqlExportData, true)
                break;
            default:
                joinOnPrimaryKeyOrForeignKey(dataClass, sqlExportJoin, sqlExportData)
        }

        addJoinToTableOrView(sqlExportJoin, sqlExportTableOrView)
    }

    private void joinOnForeignKeyDataElements(DataClass dataClass, SqlExportJoin sqlExportJoin, SqlExportData sqlExportData, boolean buildingCohortQuery = false) {
        dataClass.dataElements.each(dataElement -> {
            createJoinIfForeignKeyField(sqlExportJoin, dataElement, sqlExportData, buildingCohortQuery)
        })
    }

    private void joinOnPrimaryKeyOrForeignKey(DataClass dataClass, SqlExportJoin sqlExportJoin, SqlExportData sqlExportData) {
        if (dataClass == sqlExportData.cohortDataClass) {
            // We are selecting from the cohort table so join to the cohort temp table on primary key
            createJoinOnPrimaryKey(sqlExportJoin, sqlExportData)
        }
        else {
            // We are not the cohort table so use foreign keys
            joinOnForeignKeyDataElements(dataClass, sqlExportJoin, sqlExportData)
        }
    }

    private void addJoinToTableOrView(SqlExportJoin sqlExportJoin, SqlExportTableOrView sqlExportTableOrView){
        if (sqlExportJoin.on.size() <= 0) {
            return
        }
        def sqlExportJoinJson = JsonOutput.toJson(sqlExportJoin)
        sqlExportTableOrView.rules.add(new SqlExportRule('join', sqlExportJoinJson))
    }

    private void createJoinIfForeignKeyField(SqlExportJoin sqlExportJoin, DataElement dataElement, SqlExportData sqlExportData, Boolean buildingCohortQuery = false) {
        // Get foreign keys from columns
        def foreignKeyProfileFields = profileReaderService.getForeignKeyProfileFields(dataElement)
        if (!foreignKeyProfileFields || !foreignKeyProfileFields?.columns?.currentValue) {
            return
        }

        String onString = (buildingCohortQuery)
            ? getJoinForCohort(dataElement, foreignKeyProfileFields)
            : getJoinToCohort(dataElement, foreignKeyProfileFields, sqlExportData)

        if (onString) {
            sqlExportJoin.on.push(onString)
            sqlExportFieldService.addColumnToCohort(sqlExportData, foreignKeyProfileFields.columns?.currentValue)
        }

    }

    private createJoinOnPrimaryKey(SqlExportJoin sqlExportJoin, SqlExportData sqlExportData) {
        sqlExportData.sqlExportTables.cohortTableOrView.columns
            .findAll(column -> column.primaryKey)
            .each(column -> {
                def dataElement = DataModelReaderService.getDataElement(sqlExportData.cohortDataClass, column.labelColumnName)
                String onString = getJoinToCohortString(column.labelColumnName, dataElement, sqlExportData)
                if (onString) {
                    sqlExportJoin.on.push(onString)
                }
        })
    }

    private String getJoinToCohort(DataElement dataElement, SqlExportForeignKeyProfileFields foreignKeyProfileFields, SqlExportData sqlExportData){
        def cohortTableOrView = sqlExportData.sqlExportTables.cohortTableOrView

        if (!cohortTableOrView ||
            "[${foreignKeyProfileFields.schema.currentValue}].[${foreignKeyProfileFields.table.currentValue}]" != cohortTableOrView.label ||
            !foreignKeyProfileFields.columns.currentValue) {
            return null
        }

        getJoinToCohortString(foreignKeyProfileFields.columns.currentValue, dataElement, sqlExportData)
    }

    private String getJoinToCohortString(String cohortColumn, DataElement dataElement, SqlExportData sqlExportData) {
        "[${sqlExportData.sqlExportTables.cohortTableOrView.tempTableName}].[${cohortColumn}] = [${dataElement.dataClass.parentDataClass.label}].[${dataElement.dataClass.label}].[${dataElement.label}]"
    }

    private String getJoinForCohort(DataElement dataElement, SqlExportForeignKeyProfileFields foreignKeyProfileFields) {
        def foreignKeyColumn = "[${foreignKeyProfileFields.schema.currentValue}].[${foreignKeyProfileFields.table.currentValue}].[${foreignKeyProfileFields.columns.currentValue}]"
        def referenceKeyColumn =  "[${dataElement.dataClass.parentDataClass.label}].[${dataElement.dataClass.label}].[${dataElement.label}]"
        return "${referenceKeyColumn} = ${foreignKeyColumn}"
    }



}
