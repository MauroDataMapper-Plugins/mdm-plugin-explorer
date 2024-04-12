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
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlType

class MeqlPreparerService {

    /**
     * Return a list of distinct entities that are referenced by a RuleSet
     * @param entityRules
     * @return
     */
    static List<String> getDistinctEntitiesReferencedByRules(MeqlRuleSet entityRules) {
        def distinctEntities = extractEntities(entityRules.rules)
        distinctEntities.unique()
    }

    /**
     * Extract all "entity" values from a rules object
     * @param entityRules
     * @return
     */
    static private List<String> extractEntities(List<MeqlRuleBase> entityRules) {
        List<String> entities = []
        entityRules.each { ruleObject ->
            entities << ruleObject.entity
            if (ruleObject.meqlType == MeqlType.RuleSet) {
                entities.addAll(extractEntities((ruleObject as MeqlRuleSet).rules))
            }
        }
        return entities
    }
}
