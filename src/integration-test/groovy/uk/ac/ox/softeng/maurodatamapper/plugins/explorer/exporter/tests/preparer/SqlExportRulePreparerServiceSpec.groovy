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

import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRuleSet
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.MeqlTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.preparer.SqlExportRulePreparerService
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

@Integration
@Slf4j
@Rollback
class SqlExportRulePreparerServiceSpec extends BaseIntegrationSpec {
    MeqlTestGivens given

    def setup() {
        given = new MeqlTestGivens()
    }

    @Override
    void setupDomainData() {
    }
    void "no entities are found and no errors occur when there is no data ruleset"() {

        given: "there is meql data"
        MeqlRuleSet dataMeqlRuleSet = null
        def ruleSet = given."there is a MeqlRuleSet"("and", "notPartOfData", [])

        when: "we get distinct entities"
        def actualErrorMessage = "No error occurred"
        try {
            ruleSet = SqlExportRulePreparerService.getRuleSetFromDataRuleSetForEntity("entityB", dataMeqlRuleSet)
        }
        catch (Exception e) {
            actualErrorMessage = e.message
        }

        then: "the entities are what we expect"

        with {
            {
                ruleSet == null
                actualErrorMessage == "No error occurred"
            }
        }
    }

    void "no entities are found when there are no rules"() {

        given: "there is meql data"
        def dataMeqlRuleSet = given."there is a MeqlRuleSet"("and", "parentEntity", [])

        when: "we get distinct entities"
        def ruleSet = SqlExportRulePreparerService.getRuleSetFromDataRuleSetForEntity("entity", dataMeqlRuleSet)

        then: "the entities are what we expect"

        with {
            {
                ruleSet == null
            }
        }
    }

    void "no entities are found when the searched for entity is not present"() {

        given: "there is meql data"
        def meqlRule = given."there is a basic MeqlRule"("entityA", "fieldA",)
        def meqlRuleSet = given."there is a MeqlRuleSet"("and", "entityA", [meqlRule])
        def dataMeqlRuleSet = given."there is a MeqlRuleSet"("and", "parentEntity", [meqlRuleSet])

        when: "we get distinct entities"
        def ruleSet = SqlExportRulePreparerService.getRuleSetFromDataRuleSetForEntity("entityB", dataMeqlRuleSet)

        then: "the entities are what we expect"

        with {
            {
                ruleSet == null
            }
        }
    }

    void "one entity is found when there is a rule set with a matching entity"() {

        given: "there is meql data"
        def meqlRule = given."there is a basic MeqlRule"("entityA", "fieldA",)
        def meqlRuleSet = given."there is a MeqlRuleSet"("and", "entityA", [meqlRule])
        def dataMeqlRuleSet = given."there is a MeqlRuleSet"("and", "parentEntity", [meqlRuleSet])

        when: "we get distinct entities"
        def ruleSet = SqlExportRulePreparerService.getRuleSetFromDataRuleSetForEntity("entityA", dataMeqlRuleSet)

        then: "the entities are what we expect"

        with {
            {
                ruleSet == meqlRuleSet
            }
        }
    }

    void "the matching entity is found when there are many entities"() {

        given: "there is meql data"
        def meqlRuleA = given."there is a basic MeqlRule"("entityA", "fieldA",)
        def meqlRuleSetA = given."there is a MeqlRuleSet"("and", "entityA", [meqlRuleA])
        def meqlRuleB = given."there is a basic MeqlRule"("entityB", "fieldB",)
        def meqlRuleSetB = given."there is a MeqlRuleSet"("and", "entityB", [meqlRuleB])
        def meqlRuleC = given."there is a basic MeqlRule"("entityC", "fieldC",)
        def meqlRuleSetC = given."there is a MeqlRuleSet"("and", "entityC", [meqlRuleC])

        def dataMeqlRuleSet = given."there is a MeqlRuleSet"("and", "parentEntity", [meqlRuleSetA, meqlRuleSetB, meqlRuleSetC])

        when: "we get distinct entities"
        def ruleSet = SqlExportRulePreparerService.getRuleSetFromDataRuleSetForEntity("entityB", dataMeqlRuleSet)

        then: "the entities are what we expect"

        with {
            {
                ruleSet == meqlRuleSetB
            }
        }
    }

    void "an appropriate error is thrown when a MeqlRule is present"() {

        given: "there is meql data"
        def meqlRule = given."there is a basic MeqlRule"("entityA", "fieldA",)
        def meqlRuleSet = given."there is a MeqlRuleSet"("and", "parentEntity", [meqlRule])

        when: "we get distinct entities"
        def actualErrorMessage = "No error occurred"
        try {
            SqlExportRulePreparerService.getRuleSetFromDataRuleSetForEntity("entityB", meqlRuleSet)
        }
        catch (Exception e) {
            actualErrorMessage = e.message
        }

        then: "the expected error message was thrown"
        with {
            {
                actualErrorMessage == "A dataRuleSet is expected to only have MeqlRuleSet objects in it's rules list. A MeqlRule object has been found"
            }
        }
    }

    void "an appropriate error is thrown when there are repeated entity names"() {

        given: "there is meql data"
        def meqlRuleA = given."there is a basic MeqlRule"("entityA", "fieldA",)
        def meqlRuleSetA = given."there is a MeqlRuleSet"("and", "entityA", [meqlRuleA])
        def meqlRuleB = given."there is a basic MeqlRule"("entityA", "fieldB",)
        def meqlRuleSetB = given."there is a MeqlRuleSet"("and", "entityA", [meqlRuleB])
        def dataMeqlRuleSet = given."there is a MeqlRuleSet"("and", "parentEntity", [meqlRuleSetA, meqlRuleSetB])

        when: "we get distinct entities"
        def actualErrorMessage = "No error occurred"
        try {
            SqlExportRulePreparerService.getRuleSetFromDataRuleSetForEntity("entityA", dataMeqlRuleSet)
        }
        catch (Exception e) {
            actualErrorMessage = e.message
        }

        then: "the expected error message was thrown"
        with {
            {
                actualErrorMessage == "A dataRuleSet is expected to only have one ruleset for each entity.  Entity 'entityA' is not unique."
            }
        }
    }
}
