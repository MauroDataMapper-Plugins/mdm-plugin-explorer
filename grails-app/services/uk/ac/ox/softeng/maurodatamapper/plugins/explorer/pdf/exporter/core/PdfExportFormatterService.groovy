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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.pdf.exporter.core

import groovy.json.JsonSlurper

import java.text.SimpleDateFormat

class PdfExportFormatterService {
    static String capitalizeFirstLetter(String word) {
        return word[0].toUpperCase() + word[1..-1]
    }

    static String meqlJsonToMeql(String meqlJson) {
        def jsonSlurper = new JsonSlurper()
        def jsonObject = jsonSlurper.parseText(meqlJson)
        parseQuery(jsonObject)
    }

    private static String formattedValue(value, boolean quoted = false) {
        def quotes = quoted ? '"' : ''

        if (value instanceof Number) {
            return value.toString()
        } else if (value instanceof Date) {
            return formatDate(value, quotes)
        } else if (value != null && value.class.simpleName == 'AutocompleteSelectOption[]') {
            def optionsStr = value && value.length > 0 ? value*.name.join(', ') : 'null'
            return "${quotes}${optionsStr}${quotes}"
        } else if (value != null) {
            return "${quotes}${value.toString()}${quotes}"
        } else {
            return 'null'
        }
    }

    private static String formatDate(Date value, String quotes = '') {
        def date = new SimpleDateFormat('dd/MM/yyyy').format(value)
        return "${quotes}${date}${quotes}"
    }

    private static String parseQuery(value, String connective = '', int depth = 0, boolean firstRule = true, boolean lastRule = true) {
        def meql = startMeqlSection(connective, depth, firstRule)
        def (startingTabs, closingTabs) = indentMeql(depth)
        meql += startingTabs
        meql += getConnective(connective, firstRule)

        if (value.rules) {
            meql += formatRuleset(value, depth)
        }
        else {
            meql += formatRule(value)
        }

        meql += closeMeqlSection(connective, lastRule, closingTabs)

        meql
    }

    private static startMeqlSection(String connective, int depth, boolean firstRule) {
        def meql = firstRule && connective != '' ? '(' : ''
        if (depth > 0) {
            meql += '\r\n'
        }
        meql
    }

    private static closeMeqlSection(String connective, boolean lastRule, String closingTabs) {
        def meql = ''
        if (lastRule && connective != '') {
            meql += '\r\n' + closingTabs + ')'
        }
        meql
    }

    private static indentMeql(int depth) {
        def startingTabs = '\t' * depth
        def closingTabs = depth > 0 ? '\t' * (depth - 1) : ''
        return [startingTabs, closingTabs]
    }

    private static getConnective(String connective, boolean firstRule) {
        if (connective == '' || firstRule) {
            return ''
        }
        (connective.toLowerCase() != 'and' && connective.toLowerCase() != 'or') ?
                'UNKNOWN_CONNECTIVE ' : connective.toLowerCase() + ' '
    }

    private static String formatRule(rule) {
        if (!rule) {
            return ''
        }

        def meql = formattedValue(rule.field, true) + ' '
        meql += formattedValue(rule.operator) + ' '
        meql += formattedValue(rule.value, true)
        meql
    }

    private static String formatRuleset(value, int depth) {
        def meql = ''

        value.rules.eachWithIndex { rule, i ->
            int nextDepth = depth + 1
            Boolean firstRule = (i == 0)
            Boolean lastRule = (i == value.rules.size() - 1)
            meql += parseQuery(rule,
                    value.condition as String,
                    nextDepth,
                    firstRule,
                    lastRule)
        }
        meql
    }


}
