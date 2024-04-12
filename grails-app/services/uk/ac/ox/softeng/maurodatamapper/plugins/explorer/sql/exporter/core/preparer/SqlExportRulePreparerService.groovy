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

import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRuleBase
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRuleSet

class SqlExportRulePreparerService {

    /**
     * Get a rule for the passed in entity name from a dataRuleset
     * @param entity
     * @param dataRuleSet
     * @return
     */
    static MeqlRuleBase getRuleSetFromDataRuleSetForEntity(String entity, MeqlRuleSet dataRuleSet) {
        // A data Ruleset must have a unique list of entities.
        def nonUniqueGroups = findNonUniqueEntities(dataRuleSet)
        if (nonUniqueGroups) {
            def nonUniqueEntityErrorMessage = getNonUniqueEntityErrorMessage(nonUniqueGroups)
            throw new Exception(nonUniqueEntityErrorMessage)
        }

        // a data Ruleset must only reference MeqlRuleSets in it's list of rules.
        if (dataRuleSet?.rules?.find(rule -> rule.getClass().simpleName != "MeqlRuleSet")) {
            throw new Exception("A dataRuleSet is expected to only have MeqlRuleSet objects in it's rules list. A MeqlRule object has been found")
        }

        dataRuleSet?.rules?.find {rule ->
            (rule as MeqlRuleSet).entity == entity
        }
    }

    /**
     * Find non unique entities
     * @param dataRuleSet
     * @return
     */
    private static Map<String, List<MeqlRuleBase>> findNonUniqueEntities(MeqlRuleSet dataRuleSet) {
        def ruleGroups = dataRuleSet?.rules?.groupBy { it.entity }
        if (!ruleGroups) {
            return null
        }
        ruleGroups.findAll { it.value.size() > 1 }
    }

    /**
     * Create an error message about non unique errors
     * @param nonUniqueGroups
     */
    private static void getNonUniqueEntityErrorMessage(Map<String, List<MeqlRuleBase>> nonUniqueGroups) {
        def ruleNameErrors = ""
        nonUniqueGroups.each {duplicatedEntity, group ->
            def errorMessage = "Entity '$duplicatedEntity' is not unique."
            ruleNameErrors += (!ruleNameErrors.contains(errorMessage)) ? " ${errorMessage}" : ""
        }
        throw new Exception("A dataRuleSet is expected to only have one ruleset for each entity. ${ruleNameErrors}")
    }

}
