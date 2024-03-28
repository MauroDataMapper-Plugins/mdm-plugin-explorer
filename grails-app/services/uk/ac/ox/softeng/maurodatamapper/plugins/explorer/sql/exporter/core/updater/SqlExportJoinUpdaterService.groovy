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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.updater


import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportJoin
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportRule
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportTableOrView

import groovy.json.JsonOutput

class SqlExportJoinUpdaterService {

    /**
     * Add a join to a table or view object
     * @param sqlExportJoin
     * @param sqlExportTableOrView
     */
    static void addJoinToTableOrView(SqlExportJoin sqlExportJoin, SqlExportTableOrView sqlExportTableOrView){
        if (!sqlExportJoin) {
            return
        }

        if (sqlExportJoin.on.size() <= 0) {
            return
        }

        def sqlExportJoinJson = JsonOutput.toJson(sqlExportJoin)
        sqlExportTableOrView.rules.add(new SqlExportRule('join', sqlExportJoinJson))
    }

}
