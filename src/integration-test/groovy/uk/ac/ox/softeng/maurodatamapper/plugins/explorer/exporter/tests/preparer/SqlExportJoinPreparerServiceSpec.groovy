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

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportCohortTableOrView
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.IntegrationTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.MeqlTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.SqlExportTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.preparer.SqlExportJoinPreparerService
import uk.ac.ox.softeng.maurodatamapper.profile.ProfileService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

class TestForeignKey {
    final String coreTableColumn
    final String referenceTableColumn

    TestForeignKey(String coreTableColumn,
                   String referenceTableColumn) {
        this.coreTableColumn = coreTableColumn
        this.referenceTableColumn = referenceTableColumn
    }

}

class TestReferenceTable {
    final String schema
    final String table
    final List<TestForeignKey> foreignKeys

    TestReferenceTable(String schema,
                       String table,
                       TestForeignKey[] foreignKeys) {
        this.schema = schema
        this.table = table
        this.foreignKeys = foreignKeys
    }
}

@Integration
@Slf4j
@Rollback
class SqlExportJoinPreparerServiceSpec extends BaseIntegrationSpec {
    IntegrationTestGivens given
    SqlExportTestGivens givenSqlExport
    MeqlTestGivens givenMeql

    User testUser
    DataModel dataModel
    SqlExportCohortTableOrView cohortTable

    @Autowired
    SqlExportJoinPreparerService sqlExportJoinPreparerService

    @Autowired
    ProfileService profileService

    def setup() {
        given = new IntegrationTestGivens(messageSource, profileService)
        givenSqlExport = new SqlExportTestGivens()
        givenMeql = new MeqlTestGivens()
    }

    @Override
    void setupDomainData() {
        testUser = given."there is a user"()
        folder = given."there is a folder"('test folder')
    }

    void "cohort Join is created correctly"(String primaryKey2,
                                            String referenceKey1,
                                            String referenceKey2,
                                            String expectedTable1,
                                            String expectedTable2,
                                            String expectedCohortColumn1,
                                            String expectedCohortColumn2,
                                            String expectedOn1_1,
                                            String expectedOn1_2,
                                            String expectedOn2) {
        given: "We have a core table"
        def coreSchemaName = "coreSchema"
        def coreTableName = "coreTable"
        def primaryKeys = "there is a list of primary keys"(primaryKey2)

        and: "We have reference tables defined"
        def referenceTestTables = "there is a list of reference tables"(referenceKey1, referenceKey2)

        and: "there is initial test data"
        setupData()

        and: "a suitable data model exists"
        dataModel = given."there is a data model"("sample-data_onboarded", folder)

        and: "primitive data types exist"
        def intDataType = given."there is a primitive data type"("int", dataModel)
        def varcharDataType = given."there is a primitive data type"("varchar", dataModel)

        and: "a core table exists"
        def coreSchemaDataClass = given."there is a data class"(coreSchemaName, dataModel)
        def coreTableDataClass = given."there is a data class"(coreTableName, dataModel, coreSchemaDataClass)
        primaryKeys.each {primaryKey -> given."there is a data element"(primaryKey, dataModel, coreTableDataClass, intDataType)}
        given."there is a data element"("name", dataModel, coreTableDataClass, varcharDataType)

        and: "a sql export cohort table exists"
        def cohortTable = givenSqlExport."there is a SqlExportCohortTableOrView"(coreSchemaName, coreTableName)
        cohortTable.columns.push(givenSqlExport."there is a SqlExportCohortColumn"("id", "int", true))
        cohortTable.columns.push(givenSqlExport."there is a SqlExportCohortColumn"("name", "varchar", false))

        and: "reference tables exist"
        "there are reference tables in the data model"(dataModel, referenceTestTables, coreSchemaName, coreTableName, intDataType, varcharDataType)

        and: "there is valid meql data"
        def cohortRuleSet = givenMeql."there is a MeqlRuleSet"("and", "parentEntity", [])
        referenceTestTables.each {referenceTestTable ->
            {
                cohortRuleSet.rules.push(givenMeql."there is a MeqlRule"("${referenceTestTable.schema}.${referenceTestTable.table}", "name", "string", "=", "test"))
            }
        }

        when: "The join data is created"
        def joins = sqlExportJoinPreparerService.getCohortJoin(dataModel, cohortRuleSet, cohortTable)

        then: "Join is correct"
        def expectedOn1Size = (expectedOn1_2) ? 2 : 1
        def expectedCohortColumnSize = (expectedCohortColumn2) ? 2 : 1
        def expectedSqlExportJoinsSize = (expectedOn2) ? 2 : 1
        with {
            {
                joins.cohortColumnNames.size() == expectedCohortColumnSize
                def sortedCohortColumnNames = joins.cohortColumnNames
                sortedCohortColumnNames[0] == expectedCohortColumn1
                if (expectedCohortColumn2) {
                    sortedCohortColumnNames[1] == expectedCohortColumn2
                }

                joins.sqlExportJoins.size() == expectedSqlExportJoinsSize
                def sortedJoins = joins.sqlExportJoins.sort(x -> x.table)
                sortedJoins[0].table == expectedTable1
                sortedJoins[0].on.size() == expectedOn1Size

                def sortedOn = sortedJoins[0].on.sort()
                sortedOn[0] == expectedOn1_1

                if (expectedOn1_2) {
                    sortedOn[1] == expectedOn1_2
                }

                if (expectedOn2) {
                    sortedJoins[1].table == expectedTable2
                    sortedJoins[1].on.size() == 1
                    sortedJoins[1].on[0] == expectedOn2
                }
            }
        }

        where:
        primaryKey2 | referenceKey1                        | referenceKey2                          | expectedTable1            | expectedTable2                | expectedCohortColumn1 | expectedCohortColumn2 | expectedOn1_1                                                             | expectedOn1_2                                                         | expectedOn2
        null        | "refSchema.refTable.refId -> coreId" | null                                   | "[refSchema].[refTable]"  | null                          | "coreId"              | null                  | "[refSchema].[refTable].[refId] = [coreSchema].[coreTable].[coreId]"      | null                                                                  | null
        null        | "refSchema.refTable.refId -> coreId" | "refSchema.refTable2.refId -> coreId"  | "[refSchema].[refTable2]" | "[refSchema].[refTable]"      | "coreId"              | "coreId"              | "[refSchema].[refTable2].[refId] = [coreSchema].[coreTable].[coreId]"     | null                                                                  | "[refSchema].[refTable].[refId] = [coreSchema].[coreTable].[coreId]"
        null        | "refSchema.refTable.refId -> coreId" | "refSchema2.refTable2.refId -> coreId" | "[refSchema2].[refTable2]"| "[refSchema].[refTable]"      | "coreId"              | "coreId"              | "[refSchema2].[refTable2].[refId] = [coreSchema].[coreTable].[coreId]"    | null                                                                  | "[refSchema].[refTable].[refId] = [coreSchema].[coreTable].[coreId]"
        "coreId2"   | "refSchema.refTable.refId -> coreId" | "refSchema.refTable.refId2 -> coreId2" | "[refSchema].[refTable]"  | null                          | "coreId2"             | "coreId"              | "[refSchema].[refTable].[refId2] = [coreSchema].[coreTable].[coreId2]"    | "[refSchema].[refTable].[refId] = [coreSchema].[coreTable].[coreId]"  | null

    }

    void "cohort Join creation raises an error when table names are corrupt"() {
        given: "We have a schema name, table name and column list defined"
        def coreSchemaName = "coreSchema"
        def coreTableName = "coreTable"
        def corePrimaryKey = "id"
        def referenceSchemaName = "referenceSchema"
        def referenceTableName = "referenceTable"
        def referenceForeignKey = "coreId"

        and: "there is join test data"
        setupData()

        and: "a suitable data model exists"
        dataModel = given."there is a data model"("sample-data_onboarded", folder)

        and: "a dataclass exists"
        // Primitive data types
        def intDataType = given."there is a primitive data type"("int", dataModel)
        def varcharDataType = given."there is a primitive data type"("varchar", dataModel)

        // Schemas
        def coreSchemaDataClass = given."there is a data class"(coreSchemaName, dataModel)
        def referenceSchemaDataClass = given."there is a data class"(referenceSchemaName, dataModel)

        // Table
        def coreTableDataClass = given."there is a data class"(coreTableName, dataModel, coreSchemaDataClass)
        def referenceTableDataClass = given."there is a data class"(referenceTableName, dataModel, referenceSchemaDataClass)

        // Columns
        given."there is a data element"(corePrimaryKey, dataModel, coreTableDataClass, intDataType)
        given."there is a data element"("name", dataModel, coreTableDataClass, varcharDataType)
        given."there is a data element"("id", dataModel, referenceTableDataClass, intDataType)
        def foreignKeyDataElement = given."there is a data element"(referenceForeignKey, dataModel, referenceTableDataClass, intDataType)
        given."there is a data element"("name", dataModel, referenceTableDataClass, varcharDataType)

        and: "a foreign key profile exists"
        given."there is a profile identifying a foreign key"(foreignKeyDataElement, testUser, coreSchemaName, coreTableName, corePrimaryKey)

        and: "sql export cohort table exists"
        cohortTable = givenSqlExport."there is a SqlExportCohortTableOrView"(coreSchemaName, coreTableName)
        cohortTable.columns.push(givenSqlExport."there is a SqlExportCohortColumn"("id", "int", true))
        cohortTable.columns.push(givenSqlExport."there is a SqlExportCohortColumn"("name", "varchar", false))

        and: "there is corrupt meql data"
        def meqlRule = givenMeql."there is a MeqlRule"("invalid", "name", "string", "=", "test")
        def cohortRuleSet = givenMeql."there is a MeqlRuleSet"("and", "parentEntity", [meqlRule])

        when: "The join data is created"
        String exceptionMessage
        try {
            sqlExportJoinPreparerService.getCohortJoin(dataModel, cohortRuleSet, cohortTable)
        }
        catch (Exception e) {
            exceptionMessage = e.message
        }

        then: "The expected error is returned"
        with {
            {
                exceptionMessage == "Unable to create join. Cohort rule entity has been found that is not in the expected format of \"schemaName.tableName\""
            }
        }

    }

    void "data Join is created correctly"(String primaryKey2,
                                          String referenceKey1,
                                          String referenceKey2,
                                          String viewingReferenceTable,
                                          String expectedTable,
                                          String expectedCohortColumn1,
                                          String expectedCohortColumn2,
                                          String expectedOn1,
                                          String expectedOn2) {
        given: "We have a core table"
        def coreSchemaName = "coreSchema"
        def coreTableName = "coreTable"
        def primaryKeys = "there is a list of primary keys"(primaryKey2)

        and: "We have reference tables defined"
        def referenceTestTables = "there is a list of reference tables"(referenceKey1, referenceKey2)

        and: "there is initial test data"
        setupData()

        and: "a suitable data model exists"
        dataModel = given."there is a data model"("sample-data_onboarded", folder)

        and: "primitive data types exist"
        def intDataType = given."there is a primitive data type"("int", dataModel)
        def varcharDataType = given."there is a primitive data type"("varchar", dataModel)

        and: "a core table exists"
        def coreSchemaDataClass = given."there is a data class"(coreSchemaName, dataModel)
        def coreTableDataClass = given."there is a data class"(coreTableName, dataModel, coreSchemaDataClass)
        primaryKeys.each {primaryKey -> given."there is a data element"(primaryKey, dataModel, coreTableDataClass, intDataType)}
        given."there is a data element"("name", dataModel, coreTableDataClass, varcharDataType)

        and: "a sql export cohort table exists"
        def cohortTable = givenSqlExport."there is a SqlExportCohortTableOrView"(coreSchemaName, coreTableName)
        cohortTable.columns.push(givenSqlExport."there is a SqlExportCohortColumn"("id", "int", true))
        cohortTable.columns.push(givenSqlExport."there is a SqlExportCohortColumn"("name", "varchar", false))

        and: "reference tables exist"
        def referenceDataClasses = "there are reference tables in the data model"(dataModel, referenceTestTables, coreSchemaName, coreTableName, intDataType, varcharDataType)

        and: "there is valid meql data"
        def cohortRuleSet = givenMeql."there is a MeqlRuleSet"("and", "parentEntity", [])
        referenceTestTables.each {referenceTestTable ->
            {
                cohortRuleSet.rules.push(givenMeql."there is a MeqlRule"("${referenceTestTable.schema}.${referenceTestTable.table}", "name", "string", "=", "test"))
            }
        }

        and: "the viewing data class can be found"
        def viewDataClass = "the viewing data class can be retrieved"(referenceDataClasses, viewingReferenceTable)

        when: "The join data is created"
        def joins = sqlExportJoinPreparerService.getDataTableJoinToCohort(dataModel, viewDataClass, cohortTable, coreTableDataClass)

        then: "Join is correct"
        def expectedOnSize = (expectedOn2) ? 2 : 1
        def expectedCohortColumnSize = (expectedCohortColumn2) ? 2 : 1
        with {
            {
                joins.cohortColumnNames.size() == expectedCohortColumnSize
                def sortedCohortColumnNames = joins.cohortColumnNames
                sortedCohortColumnNames[0] == expectedCohortColumn1
                if (expectedCohortColumn2) {
                    sortedCohortColumnNames[1] == expectedCohortColumn2
                }

                joins.sqlExportJoins[0].table == expectedTable
                joins.sqlExportJoins[0].on.size() == expectedOnSize

                def sortedOn = joins.sqlExportJoins[0].on.sort()
                sortedOn[0] == expectedOn1

                if (expectedOn2) {
                    sortedOn[1] == expectedOn2
                }

            }
        }

        where:
        primaryKey2 | referenceKey1                        | referenceKey2                          | viewingReferenceTable     | expectedTable | expectedCohortColumn1 | expectedCohortColumn2 | expectedOn1                                               | expectedOn2
        null        | "refSchema.refTable.refId -> coreId" | null                                   | "refSchema.refTable"      | "[#cohort]"   | "coreId"              | null                  | "[#cohort].[coreId] = [refSchema].[refTable].[refId]"     | null
        null        | "refSchema.refTable.refId -> coreId" | "refSchema.refTable2.refId -> coreId"  | "refSchema.refTable"      | "[#cohort]"   | "coreId"              | null                  | "[#cohort].[coreId] = [refSchema].[refTable].[refId]"     | null
        null        | "refSchema.refTable.refId -> coreId" | "refSchema2.refTable2.refId -> coreId" | "refSchema.refTable"      | "[#cohort]"   | "coreId"              | null                  | "[#cohort].[coreId] = [refSchema].[refTable].[refId]"     | null
        "coreId2"   | "refSchema.refTable.refId -> coreId" | "refSchema.refTable.refId2 -> coreId2" | "refSchema.refTable"      | "[#cohort]"   | "coreId2"             | "coreId"             | "[#cohort].[coreId2] = [refSchema].[refTable].[refId2]"   | "[#cohort].[coreId] = [refSchema].[refTable].[refId]"
        null        | "refSchema.refTable.refId -> coreId" | "refSchema.refTable2.refId -> coreId"  | "refSchema.refTable2"     | "[#cohort]"   | "coreId"              | null                  | "[#cohort].[coreId] = [refSchema].[refTable2].[refId]"    | null
        null        | "refSchema.refTable.refId -> coreId" | "refSchema2.refTable2.refId -> coreId" | "refSchema2.refTable2"    | "[#cohort]"   | "coreId"              | null                  | "[#cohort].[coreId] = [refSchema2].[refTable2].[refId]"   | null
    }

    private TestReferenceTable[] "there is a list of reference tables"(String referenceKey1, String referenceKey2) {
        List<TestReferenceTable> referenceTestTables = []
        if (referenceKey1) {
            def referenceParts = "there is a reference column and a core column"(referenceKey1)
            def fullyQualifiedColumnParts = "there is a fully qualified foreign key separated into it's parts"(referenceParts[0])
            referenceTestTables.push(new TestReferenceTable(fullyQualifiedColumnParts[0], fullyQualifiedColumnParts[1], [new TestForeignKey(referenceParts[1], fullyQualifiedColumnParts[2])] as TestForeignKey[]))
        }

        if (referenceKey2) {
            def referenceParts = "there is a reference column and a core column"(referenceKey2)
            def fullyQualifiedColumnParts = "there is a fully qualified foreign key separated into it's parts"(referenceParts[0])
            def tableExists = referenceTestTables.find(testTable -> testTable.schema == fullyQualifiedColumnParts[0] && testTable.table == fullyQualifiedColumnParts[1])
            if (tableExists) {
                tableExists.foreignKeys.push(new TestForeignKey(referenceParts[1], fullyQualifiedColumnParts[2]))
            } else {
                referenceTestTables.push(new TestReferenceTable(fullyQualifiedColumnParts[0], fullyQualifiedColumnParts[1], [new TestForeignKey(referenceParts[1], fullyQualifiedColumnParts[2])] as TestForeignKey[]))
            }
        }

        referenceTestTables
    }

    private String[] "there is a list of primary keys"(String primaryKey2) {
        List<String> primaryKeys = ["coreId"]
        if (primaryKey2) {
            primaryKeys.push(primaryKey2)
        }
        primaryKeys
    }

    private DataClass[] "there are reference tables in the data model"(DataModel dataModel, TestReferenceTable[] referenceTestTables, String coreSchemaName, String coreTableName, PrimitiveType keyDataType, PrimitiveType fieldDataType) {
        List<DataClass> referenceDataClasses = []
        referenceTestTables.each {referenceTestTable ->
            {
                def referenceSchemaDataClass = given."there is a data class"(referenceTestTable.schema, dataModel)
                def referenceTableDataClass = given."there is a data class"(referenceTestTable.table, dataModel, referenceSchemaDataClass)
                given."there is a data element"("id", dataModel, referenceTableDataClass, keyDataType)
                referenceTestTable.foreignKeys.each {foreignKey ->
                    {
                        def foreignKeyDataElement = given."there is a data element"(foreignKey.referenceTableColumn, dataModel, referenceTableDataClass, keyDataType)
                        given."there is a profile identifying a foreign key"(foreignKeyDataElement, testUser, coreSchemaName, coreTableName, foreignKey.coreTableColumn)
                    }
                }
                given."there is a data element"("name", dataModel, referenceTableDataClass, fieldDataType)
                referenceDataClasses.push(referenceTableDataClass)
            }
        }
        referenceDataClasses
    }

    private String[] "there is a reference column and a core column"(String referenceKey) {
        if (!referenceKey) {
            return []
        }
        def referenceSplit = referenceKey.split(" -> ")
        if (referenceSplit.size() != 2) {
            throw new Exception("Test data is expected to be in the format \"schema.table.field -> field\"")
        }
        referenceSplit
    }



    private String[] "there is a fully qualified foreign key separated into it's parts"(String fullyQualifiedColumn) {
        if (!fullyQualifiedColumn) {
            return []
        }
        def fullyQualifiedColumnParts = fullyQualifiedColumn.split("\\.")
        if (fullyQualifiedColumnParts.size() != 3) {
            throw new Exception("Test data not defined correctly")
        }
        fullyQualifiedColumnParts
    }

    private DataClass "the viewing data class can be retrieved"(DataClass[] referenceDataClasses, String viewingReferenceTable) {
        def viewingParts = viewingReferenceTable.split("\\.")
        if (viewingParts.size() != 2) {
            throw new Exception("viewingReferenceTable is not in the expected format of \"schema.table\"")
        }
        def viewingSchema = viewingParts[0]
        def viewingTable = viewingParts[1]
        def viewDataClass = referenceDataClasses.find(dataClass -> dataClass.label == viewingTable && dataClass.parentDataClass.label == viewingSchema)

        if (!viewDataClass) {
            throw new Exception("unable to find dataclass to view")
        }
        viewDataClass
    }
}
