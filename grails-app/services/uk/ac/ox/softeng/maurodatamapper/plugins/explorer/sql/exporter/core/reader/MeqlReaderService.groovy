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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.reader

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRule
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRuleBase
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRuleSet
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlType

import groovy.json.JsonSlurper

import javax.xml.crypto.Data


class MeqlReaderService {

    /**
     * Transform all MeqlRules in a DataModel into strongly typed objects so that they are
     * easier to process.
     * @param dataModel
     * @return
     */
    static getMeqlRuleSets(DataModel dataModel) {
        def ruleSets = dataModel.rules
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

        return new Tuple(ruleSets.get('cohort'), ruleSets.get('data'))
    }

    /**
     * Transform a json rule representation to a strongly typed object.
     * @param representation
     * @param dataModel
     * @return
     */
    static private MeqlRuleSet transformJsonStringToMeql(String representation, DataModel dataModel) {
        def meqlJsonObject = new JsonSlurper().parseText(representation)
        def rootRuleSet = convertToTypedObject(meqlJsonObject, dataModel)
        rootRuleSet as MeqlRuleSet
    }

    /**
     * Convert Meql to a strongly typed object
     * @param json
     * @param dataModel
     * @return
     */
    static private MeqlRuleBase convertToTypedObject(def json, DataModel dataModel) {
        if (!(json instanceof Map)) {
            return null
        }

        if (json.containsKey('condition')) {
            // Convert to MeqlRuleSet
            return new MeqlRuleSet(
                json.condition as String,
                json.entity as String,
                json.rules.collect {convertToTypedObject(it, dataModel)}.flatten() as List
            )
        }

        if (json.containsKey('field') && json.containsKey('operator') && json.containsKey('value')) {
            // Get the type
            def dataType = getSQLDataType(json, dataModel)
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

    /**
     * Get the SQL data type from the data model. This is required by the templating code
     * to identify how to format data when building the where clause.
     * @param json
     * @param dataModel
     * @return
     */
    static private String getSQLDataType(def json, DataModel dataModel) {
        def entityParts = (json.entity as String).split('\\.')
        def dataTable = DataModelReaderService.getDataClass(dataModel, entityParts[0], entityParts[1])
        def dataElement = DataModelReaderService.getDataElement(dataTable, json.field as String)
        def dataType =  (dataElement.dataType.domainType == "PrimitiveType") ? dataElement.dataType.label : 'NOT_PRIMITIVE'
        dataType
    }
}
