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

import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRule
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.MeqlTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.SqlExportTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.updater.SqlExportRuleUpdaterService
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

@Integration
@Slf4j
@Rollback
class SqlExportRuleUpdaterServiceSpec extends BaseIntegrationSpec {

    SqlExportTestGivens given
    MeqlTestGivens givenMeql

    def setup() {
        given = new SqlExportTestGivens()
        givenMeql = new MeqlTestGivens()
    }

    @Override
    void setupDomainData() {
    }

    void "add a rule to a table"() {

        given: "there is meql data"
        def cohortTableOrView = given."there is a SqlExportCohortTableOrView"("schema","label")
        def rule = givenMeql."there is a basic MeqlRule"("table", "field")

        when: "a rule is added"
        SqlExportRuleUpdaterService.addRule(rule, cohortTableOrView)

        then: "the rules have been added"
        with {
            {
                cohortTableOrView.rules.size() == 1
                cohortTableOrView.rules[0].json == "{\"value\":\"1\",\"dataType\":\"int\",\"entity\":\"table\",\"operator\":\"=\",\"meqlType\":\"Rule\",\"field\":\"field\"}"
            }
        }
    }

    void "add a ruleset to a table"() {

        given: "there is meql data"
        def cohortTableOrView = given."there is a SqlExportCohortTableOrView"("schema","label")
        def ruleSet = givenMeql."there is a MeqlRuleSet"("AND", "table", [])

        when: "a rule is added"
        SqlExportRuleUpdaterService.addRule(ruleSet, cohortTableOrView)

        then: "the rules have been added"
        with {
            {
                cohortTableOrView.rules.size() == 1
                cohortTableOrView.rules[0].json == "{\"rules\":[],\"entity\":\"table\",\"meqlType\":\"RuleSet\",\"condition\":\"AND\"}"
            }
        }
    }

    void "add a null rule to a table"() {

        given: "there is meql data"
        def cohortTableOrView = given."there is a SqlExportCohortTableOrView"("schema","label")
        MeqlRule rule = null

        when: "a rule is added"
        SqlExportRuleUpdaterService.addRule(rule, cohortTableOrView)

        then: "the rules have been added"
        with {
            {
                cohortTableOrView.rules.size() == 0
            }
        }
    }
}
