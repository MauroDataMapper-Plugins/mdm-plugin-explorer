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

import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportCohortColumn
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportCohortTableOrView
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportJoin

import groovy.util.logging.Slf4j

@Slf4j
class SqlExportTestGivens {

    static SqlExportCohortTableOrView "there is a SqlExportCohortTableOrView"(String schema, String label) {
        new SqlExportCohortTableOrView(schema, label)
    }

    static SqlExportCohortColumn "there is a SqlExportCohortColumn"(String label, String dataType, Boolean primaryKey) {
        new SqlExportCohortColumn(label, dataType, primaryKey)
    }

    static SqlExportJoin "there is a SqlExportJoin"(String table, String on = null) {
        def sqlExportJoin = new SqlExportJoin(table)
        if (on) {
            sqlExportJoin.on.push(on)
        }
        sqlExportJoin
    }

}
