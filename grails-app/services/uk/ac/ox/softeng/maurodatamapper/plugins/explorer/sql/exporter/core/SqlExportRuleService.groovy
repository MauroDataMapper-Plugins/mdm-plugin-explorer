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

import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRuleBase
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRuleSet
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportData
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportRule
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportTableOrView

import groovy.json.JsonOutput

class SqlExportRuleService {
    static addRule(MeqlRuleBase rule, SqlExportTableOrView sqlExportTableOrView) {
        if (!rule) {
            return
        }
        def entityRulesJson = JsonOutput.toJson(rule)
        sqlExportTableOrView.rules.add(new SqlExportRule('where', entityRulesJson))
    }

    static addCohortRules(SqlExportData sqlExportData) {
        sqlExportData.cohortRuleGroup.each(rule -> {
            addRule(rule, sqlExportData.sqlExportTables.cohortTableOrView)
        })
    }

    static addDataTableRule(String entity, MeqlRuleSet dataRuleGroup, SqlExportTableOrView sqlExportTableOrView) {
        def rule = dataRuleGroup?.rules?.find {rule ->
            (rule as MeqlRuleSet).entity == entity
        }

        addRule(rule, sqlExportTableOrView)
    }
}
