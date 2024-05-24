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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.exporter.testhelpers

import groovy.xml.DOMBuilder
import groovy.xml.MarkupBuilder
import groovy.xml.StreamingMarkupBuilder

class ExporterTestHelper {
    String loadTextFile(String testName, String fileName) {
        InputStream resourceStream = this.class.getResourceAsStream("/exporter/${testName}/${fileName}")
        def fileString = new BufferedReader(new InputStreamReader(resourceStream))
            .readLines()
            .join("\n")
            .trim()
        fileString
    }

    static String standardiseLineEndings(String input) {
        def output = input.replace("\r\n","\n")
        output = output.replace("\r","\n")
        output = output.trim()
        output
    }

    static String standardiseXml(String input) {
        def xml = DOMBuilder.parse(new StringReader(input))
        xml.documentElement as String
    }
}
