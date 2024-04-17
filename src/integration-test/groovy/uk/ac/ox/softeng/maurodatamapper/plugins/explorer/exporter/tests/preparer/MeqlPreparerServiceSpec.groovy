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

import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers.MeqlTestGivens
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.preparer.MeqlPreparerService
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

@Integration
@Slf4j
@Rollback
class MeqlPreparerServiceSpec extends BaseIntegrationSpec {
    MeqlTestGivens given

    def setup() {
        given = new MeqlTestGivens()
    }

    @Override
    void setupDomainData() {
    }

    void "no entities are found when there are no rules"() {

        given: "there is meql data"
        def meqlRuleSet = given."there is a MeqlRuleSet"("and", "parentEntity", [])

        when: "we get distinct entities"
        def entities = MeqlPreparerService.getDistinctEntitiesReferencedByRules(meqlRuleSet)

        then: "the entities are what we expect"

        with {
            {
                entities.size() == 0
            }
        }
    }

    void "one entity is found when there is one rule"() {

        given: "there is meql data"
        def meqlRule = given."there is a basic MeqlRule"("entityA", "fieldA",)
        def meqlRuleSet = given."there is a MeqlRuleSet"("and", "parentEntity", [meqlRule])

        when: "we get distinct entities"
        def entities = MeqlPreparerService.getDistinctEntitiesReferencedByRules(meqlRuleSet)

        then: "the entities are what we expect"

        with {
            {
                entities.size() == 1
                entities[0] == "entityA"
            }
        }
    }

    void "one entity is found when there are multiple rules referencing the same entity"() {

        given: "there is meql data"
        def meqlRule = given."there is a basic MeqlRule"("entityA", "fieldA")
        def meqlRule2 = given."there is a basic MeqlRule"("entityA", "fieldB")
        def meqlRule3 = given."there is a basic MeqlRule"("entityA", "fieldC")
        def meqlRuleSet = given."there is a MeqlRuleSet"("and", "parentEntity", [meqlRule, meqlRule2, meqlRule3])

        when: "we get distinct entities"
        def entities = MeqlPreparerService.getDistinctEntitiesReferencedByRules(meqlRuleSet)

        then: "the entities are what we expect"

        with {
            {
                entities.size() == 1
                entities[0] == "entityA"
            }
        }
    }

    void "two entities are found when there are multiple rules referencing two entities"() {

        given: "there is meql data"
        def meqlRule = given."there is a basic MeqlRule"("entityA", "fieldA")
        def meqlRule2 = given."there is a basic MeqlRule"("entityA", "fieldB")
        def meqlRule3 = given."there is a basic MeqlRule"("entityA", "fieldC")
        def meqlRule4 = given."there is a basic MeqlRule"("entityB", "fieldA")
        def meqlRule5 = given."there is a basic MeqlRule"("entityB", "fieldB")
        def meqlRuleSet = given."there is a MeqlRuleSet"("and", "parentEntity", [meqlRule, meqlRule2, meqlRule3, meqlRule4, meqlRule5])

        when: "we get distinct entities"
        def entities = MeqlPreparerService.getDistinctEntitiesReferencedByRules(meqlRuleSet)

        then: "the entities are what we expect"

        with {
            {
                entities.size() == 2
                entities[0] == "entityA"
                entities[1] == "entityB"
            }
        }
    }
}
