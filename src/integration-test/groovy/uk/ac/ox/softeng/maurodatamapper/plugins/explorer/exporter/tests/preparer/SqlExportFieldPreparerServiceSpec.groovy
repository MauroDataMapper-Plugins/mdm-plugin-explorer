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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.tests.preparer

import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.IntegrationTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.SqlExportTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.preparer.MeqlPreparerService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.preparer.SqlExportFieldPreparerService
import uk.ac.ox.softeng.maurodatamapper.profile.ProfileService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Integration
@Slf4j
@Rollback
class SqlExportFieldPreparerServiceSpec extends BaseIntegrationSpec {
    IntegrationTestGivens given
    SqlExportTestGivens givenSqlExport

    User testUser

    @Autowired
    SqlExportFieldPreparerService sqlExportFieldPreparerService

    @Autowired
    ProfileService profileService

    def setup() {
        given = new IntegrationTestGivens(messageSource, profileService)
        givenSqlExport = new SqlExportTestGivens()
    }

    @Override
    void setupDomainData() {
        testUser = given."there is a user"()
        folder = given."there is a folder"('test folder')
    }

    void "primary keys are identified correctly"(ArrayList<Boolean> primaryKeyColumns) {
        given: "We have a schema name, table name and column list defined"
        def schemaName = "schema"
        def tableName = "table"
        def columns = ["column1", "column2", "column3"]

        and: "there is initial test data"
        setupData()

        and: "a suitable data model exists"
        def dataModel = given."there is a data model"("sample-data_onboarded", folder)

        and: "a dataclass exists"
        // Primitive data types
        def intDataType = given."there is a primitive data type"("int", dataModel)

        // Schemas
        def schemaDataClass = given."there is a data class"(schemaName, dataModel)

        // Table
        def tableDataClass = given."there is a data class"(tableName, dataModel, schemaDataClass)

        // Columns
        given."there is a data element"(columns[0], dataModel, tableDataClass, intDataType)
        given."there is a data element"(columns[1], dataModel, tableDataClass, intDataType)
        given."there is a data element"(columns[2], dataModel, tableDataClass, intDataType)

        and: "Primary keys have been identified"
        def primaryKeyList = ""
        for (int i = 0; i < primaryKeyColumns.size(); i++) {
            if (primaryKeyColumns[i]) {
                primaryKeyList += (primaryKeyList == "") ? "" : ","
                primaryKeyList += columns[i]
            }
        }
        if (primaryKeyList != "") {
            given."there is a data class sql table profile"(tableDataClass, testUser, {section ->
                given."a profile field value is set"(section, "primary_key_columns", primaryKeyList)
            })
        }

        when: "The primary keys are retrieved"
        def retrievedPrimaryKeyColumns = sqlExportFieldPreparerService.getCohortPrimaryKeys(tableDataClass)

        then: "Only the primary keys are returned"
        List<String> expectedPrimaryKeys = []
        for (int i = 0; i < primaryKeyColumns.size(); i++) {
            if (primaryKeyColumns[i]) {
                expectedPrimaryKeys.push(columns[i])
            }
        }

        with {
            {
                retrievedPrimaryKeyColumns.size() == expectedPrimaryKeys.size()
                if (retrievedPrimaryKeyColumns.size() > 0) {
                    retrievedPrimaryKeyColumns.each(primaryKeyColumn -> {
                        expectedPrimaryKeys.contains(primaryKeyColumn.label)
                        primaryKeyColumn.dataType == "int"
                    })
                }
            }
        }

        where:
        primaryKeyColumns       | _
        [false, false, false]   | _
        [true, false, false]    | _
        [false, true, false]    | _
        [false, false, true]    | _
        [true, true, false]     | _
        [true, false, true]     | _
        [false, true, true]     | _
        [true, true, true]      | _

    }

    void "column is retrieved"(String columnName, Boolean isPrimaryKey, Boolean expectedToBeFound) {
        given: "We have a schema name, table name and column list defined"
        def schemaName = "schema"
        def tableName = "table"
        def columns = [
            [label: "column1", type: "int"],
            [label: "column2", type: "decimal"],
            [label: "column3", type: "varchar"]
        ]

        and: "there is initial test data"
        setupData()

        and: "a suitable data model exists"
        def dataModel = given."there is a data model"("sample-data_onboarded", folder)

        and: "a schema dataClass exists"
        def schemaDataClass = given."there is a data class"(schemaName, dataModel)

        and: "a table dataClass exists"
        def tableDataClass = given."there is a data class"(tableName, dataModel, schemaDataClass)

        and: "dataElements exist"
        columns.each((columnInfo) -> {
            def dataType = given."there is a primitive data type"(columnInfo.type, dataModel)
            given."there is a data element"(columnInfo.label, dataModel, tableDataClass, dataType)
        })

        when: "The column is retrieved"
        def retrievedPrimaryKeyColumn = sqlExportFieldPreparerService.getCohortColumn(tableDataClass, columnName, isPrimaryKey)

        then: "The column information is correct"
        with {
            {
                if (expectedToBeFound) {
                    def expectedColumnInfo = columns.find(columnInfo -> columnInfo.label == columnName)
                    retrievedPrimaryKeyColumn.labelColumnName == expectedColumnInfo.label
                    retrievedPrimaryKeyColumn.dataType == expectedColumnInfo.type
                    retrievedPrimaryKeyColumn.primaryKey == isPrimaryKey
                }
                else
                {
                    retrievedPrimaryKeyColumn == null
                }
            }
        }

        where:
        columnName          | isPrimaryKey  | expectedToBeFound
        "column1"           | true          | true
        "column2"           | true          | true
        "column3"           | true          | true
        "column1"           | false         | true
        "column2"           | false         | true
        "column3"           | false         | true
        "does not exist"    | false         | false
        null                | false         | false
    }

    void "no errors occur if there is no dataClass to search"() {
        when: "The column is retrieved"
        def retrievedPrimaryKeyColumn = sqlExportFieldPreparerService.getCohortColumn(null, "column1")

        then: "No errors occur"
        with {
            retrievedPrimaryKeyColumn == null
        }

    }

    void "all columns are retrieved"() {
        given: "We have a schema name, table name and column list defined"
        def schemaName = "schema"
        def tableName = "table"
        def columns = [
            [label: "column1", type: "int"],
            [label: "column2", type: "decimal"],
            [label: "column3", type: "varchar"]
        ]

        and: "there is initial test data"
        setupData()

        and: "a suitable data model exists"
        def dataModel = given."there is a data model"("sample-data_onboarded", folder)

        and: "a schema dataClass exists"
        def schemaDataClass = given."there is a data class"(schemaName, dataModel)

        and: "a table dataClass exists"
        def tableDataClass = given."there is a data class"(tableName, dataModel, schemaDataClass)

        and: "dataElements exist"
        columns.each((columnInfo) -> {
            def dataType = given."there is a primitive data type"(columnInfo.type, dataModel)
            given."there is a data element"(columnInfo.label, dataModel, tableDataClass, dataType)
        })

        when: "The column is retrieved"
        def retrievedSqlExportColumns = sqlExportFieldPreparerService.getSqlExportColumns(tableDataClass)

        then: "The column information is correct"
        with {
            {
                retrievedSqlExportColumns.size() == 3
                retrievedSqlExportColumns.find(column -> column.label == "[schema].[table].[column1]")
                retrievedSqlExportColumns.find(column -> column.label == "[schema].[table].[column2]")
                retrievedSqlExportColumns.find(column -> column.label == "[schema].[table].[column3]")
            }
        }
    }

}
