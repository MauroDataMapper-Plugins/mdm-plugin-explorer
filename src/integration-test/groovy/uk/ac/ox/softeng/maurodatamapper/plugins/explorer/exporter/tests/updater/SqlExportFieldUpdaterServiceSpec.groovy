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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.tests.updater

import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRuleSet
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportCohortColumn
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.MeqlTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.SqlExportTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.preparer.SqlExportRulePreparerService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.updater.SqlExportFieldUpdaterService
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

@Integration
@Slf4j
@Rollback
class SqlExportFieldUpdaterServiceSpec extends BaseIntegrationSpec {
    SqlExportTestGivens given

    def setup() {
        given = new SqlExportTestGivens()
    }

    @Override
    void setupDomainData() {
    }

    void "add a primary key column to a cohort table"() {

        given: "there is meql data"
        def cohortTableOrView = given."there is a SqlExportCohortTableOrView"("schema","label")
        def primaryKeys = given."there is a SqlExportCohortColumn"("col1", "int", true)

        when: "we set primary keys"
        SqlExportFieldUpdaterService.setCohortPrimaryKey(cohortTableOrView, primaryKeys)

        then: "the expected primary keys are set"
        with {
            {
                cohortTableOrView.columns.find(column -> column.label == "col1")
            }
        }
    }

    void "add a primary key column that already exists to a cohort table"() {

        given: "there is meql data"
        def cohortTableOrView = given."there is a SqlExportCohortTableOrView"("schema","label")
        def primaryKeys = given."there is a SqlExportCohortColumn"("col1", "int", true)
        def primaryKeys2 = given."there is a SqlExportCohortColumn"("col1", "int", true)

        when: "we set primary keys"
        SqlExportFieldUpdaterService.setCohortPrimaryKey(cohortTableOrView, primaryKeys)
        SqlExportFieldUpdaterService.setCohortPrimaryKey(cohortTableOrView, primaryKeys2)

        then: "the expected primary keys are set"
        with {
            {
                def foundColumns = cohortTableOrView.columns.findAll(column -> column.label == "col1")
                foundColumns.size() == 1
            }
        }
    }

    void "add three primary key columns to the cohort table"() {

        given: "there is meql data"
        def cohortTableOrView = given."there is a SqlExportCohortTableOrView"("schema","label")
        def primaryKeys = given."there is a SqlExportCohortColumn"("col1", "int", true)
        def primaryKeys2 = given."there is a SqlExportCohortColumn"("col2", "int", true)
        def primaryKeys3 = given."there is a SqlExportCohortColumn"("col3", "int", true)

        when: "we set primary keys"
        SqlExportFieldUpdaterService.setCohortPrimaryKey(cohortTableOrView, [primaryKeys, primaryKeys2, primaryKeys3] as SqlExportCohortColumn[])

        then: "the expected primary keys are set"
        with {
            {
                cohortTableOrView.columns.size() == 3
                cohortTableOrView.columns.findAll(column -> column.label == "col1")
                cohortTableOrView.columns.findAll(column -> column.label == "col2")
                cohortTableOrView.columns.findAll(column -> column.label == "col3")
            }
        }
    }

    void "add column to table"() {
        given: "there is meql data"
        def cohortTableOrView = given."there is a SqlExportCohortTableOrView"("schema","label")

        when: "a column is pushed"
        SqlExportFieldUpdaterService.setSqlExportColumns(cohortTableOrView,  given."there is a SqlExportCohortColumn"("col1", "int", true))

        then: "it is added to the table"
        with {
            {
                cohortTableOrView.columns.size() == 1
                cohortTableOrView.columns.findAll(column -> column.label == "col1")
            }
        }
    }

    void "add column to table when column already exists"() {
        given: "there is meql data"
        def cohortTableOrView = given."there is a SqlExportCohortTableOrView"("schema","label")

        when: "two columns are pushed"
        SqlExportFieldUpdaterService.setSqlExportColumns(cohortTableOrView,  given."there is a SqlExportCohortColumn"("col1", "int", true))
        SqlExportFieldUpdaterService.setSqlExportColumns(cohortTableOrView,  given."there is a SqlExportCohortColumn"("col1", "int", true))

        then: "it is added to the table"
        with {
            {
                cohortTableOrView.columns.size() == 1
                cohortTableOrView.columns.findAll(column -> column.label == "col1")
            }
        }
    }
}
