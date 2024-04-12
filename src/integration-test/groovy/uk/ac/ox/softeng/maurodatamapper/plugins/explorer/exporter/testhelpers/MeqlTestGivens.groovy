/*
 * Copyright 2020-2024 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers

import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRule
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRuleBase
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRuleSet
import groovy.util.logging.Slf4j

@Slf4j
class MeqlTestGivens {

    static MeqlRule "there is a MeqlRule"(String entity, String field, String dataType, String operator, String value) {
        new MeqlRule(entity, field, dataType, operator, value)
    }

    static MeqlRule "there is a basic MeqlRule"(String entity, String field) {
        "there is a MeqlRule"(entity, field, "int", "=", "1")
    }

    static MeqlRuleSet "there is a MeqlRuleSet"(String condition, String entity, List<MeqlRuleBase> rules) {
         new MeqlRuleSet(condition, entity, rules)
    }

}
