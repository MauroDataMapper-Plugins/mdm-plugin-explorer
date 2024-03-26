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

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRule
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRuleBase
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRuleSet
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlType

import groovy.json.JsonSlurper


class MeqlService {
    static getMeqlRuleGroups(DataModel dataModel) {
        def ruleGroups = dataModel.rules
            .findAll { rule -> rule.name in ['cohort', 'data'] }
            .collectMany { rule ->
                rule.ruleRepresentations
                    .findAll {ruleRepresentation -> ruleRepresentation.language == 'json-meql' }
                    .collect {ruleRepresentation ->
                        [rule.name, transformJsonStringToMeql((String) ruleRepresentation.representation, dataModel)] }
            }
            .groupBy { ruleNameAndMeqlRuleSet -> ruleNameAndMeqlRuleSet[0] }
            .collectEntries {ruleNameAndMeqlRuleSetEntries ->
                [ruleNameAndMeqlRuleSetEntries.key, ruleNameAndMeqlRuleSetEntries.value.first()[1]] }

        return new Tuple(ruleGroups.get('cohort'), ruleGroups.get('data'))
    }

    static List<String> getDistinctEntities(MeqlRuleSet entityRules) {

        // Extract distinct "entity" values from the JSON object
        def distinctEntities = extractEntities(entityRules.rules)

        // Remove duplicates and print the distinct "entity" values
        distinctEntities.unique()
    }

    // Function to extract distinct "entity" values from a rules object
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

    static private MeqlRuleSet transformJsonStringToMeql(String representation, DataModel dataModel) {
        def meqlJsonObject = new JsonSlurper().parseText(representation)
        def rootRuleGroup = convertToTypedObject(meqlJsonObject, dataModel)
        rootRuleGroup as MeqlRuleSet
    }

    static private MeqlRuleBase convertToTypedObject(def json, DataModel dataModel) {
        if (!(json instanceof Map)) {
            return null
        }

        if (json.containsKey('condition')) {
            // Convert to MeqlRuleGroup
            return new MeqlRuleSet(
                json.condition as String,
                json.entity as String,
                json.rules.collect {convertToTypedObject(it, dataModel)}.flatten() as List
            )
        }

        if (json.containsKey('field') && json.containsKey('operator') && json.containsKey('value')) {
            // Get the type
            def dataType = getMeqlDataType(json, dataModel)
            // Convert to MeqlRule
            return new MeqlRule(
                json.entity as String,
                json.field as String,
                dataType,
                json.operator as String,
                json.value as String
            )
        }

        return null
    }

    static private String getMeqlDataType(def json, DataModel dataModel) {
        def entityParts = (json.entity as String).split('\\.')
        def dataSchema = dataModel.dataClasses.find((dataClass) -> {dataClass.label == entityParts[0]})
        def dataTable = dataSchema.dataClasses.find((dataClass) -> {dataClass.label == entityParts[1]})
        def dataElement = dataTable.dataElements.find((dataElement) -> {dataElement.label == json.field as String})
        def dataType =  (dataElement.dataType.domainType == "PrimitiveType") ? dataElement.dataType.label : 'NOT_PRIMITIVE'
        dataType
    }
}
