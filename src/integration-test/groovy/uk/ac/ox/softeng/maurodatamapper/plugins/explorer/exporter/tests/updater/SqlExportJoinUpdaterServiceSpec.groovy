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

import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportJoin
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.SqlExportTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.updater.SqlExportJoinUpdaterService
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

@Integration
@Slf4j
@Rollback
class SqlExportJoinUpdaterServiceSpec extends BaseIntegrationSpec {

    SqlExportTestGivens given

    def setup() {
        given = new SqlExportTestGivens()
    }

    @Override
    void setupDomainData() {
    }

    void "add a join to a table"() {

        given: "there is meql data"
        def cohortTableOrView = given."there is a SqlExportCohortTableOrView"("schema","label")
        def join = given."there is a SqlExportJoin"("table", "[schema1].[table1].[id] = [schema2].[table2].[id]")

        when: "a join is added"
        SqlExportJoinUpdaterService.addJoinToTableOrView(join, cohortTableOrView)

        then: "the expected primary keys are set"
        with {
            {
                cohortTableOrView.rules.size() == 1
                cohortTableOrView.rules[0].json == "{\"table\":\"table\",\"on\":[\"[schema1].[table1].[id] = [schema2].[table2].[id]\"]}"
            }
        }
    }

    void "join is not added if on clause is not defined"() {

        given: "there is meql data"
        def cohortTableOrView = given."there is a SqlExportCohortTableOrView"("schema","label")
        def join = given."there is a SqlExportJoin"("table")

        when: "a join is added"
        SqlExportJoinUpdaterService.addJoinToTableOrView(join, cohortTableOrView)

        then: "the expected primary keys are set"
        with {
            {
                cohortTableOrView.rules.size() == 0
            }
        }
    }

    void "join is not added if null"() {

        given: "there is meql data"
        def cohortTableOrView = given."there is a SqlExportCohortTableOrView"("schema","label")
        SqlExportJoin join = null

        when: "a join is added"
        SqlExportJoinUpdaterService.addJoinToTableOrView(join, cohortTableOrView)

        then: "the expected primary keys are set"
        with {
            {
                cohortTableOrView.rules.size() == 0
            }
        }
    }


}
