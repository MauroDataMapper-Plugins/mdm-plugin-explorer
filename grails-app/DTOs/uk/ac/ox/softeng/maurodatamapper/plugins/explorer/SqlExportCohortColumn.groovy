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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer

class SqlExportCohortColumn extends SqlExportColumn {

    protected String dataType
    protected Boolean primaryKey = false

    SqlExportCohortColumn(String label, int ordinal, String dataType, Boolean primaryKey = false) {
        super(label, ordinal)
        this.dataType = dataType
        this.primaryKey = primaryKey
    }

    String getDataType() {
        dataType
    }

    Boolean getPrimaryKey() {
        primaryKey
    }

    String getLabelColumnName() {
        def nameParts = label.split('\\.')
        def extractColumnName = nameParts.last()
        if (!extractColumnName) {
            return null
        }
        extractColumnName = extractColumnName.replace('[','').replace(']','')
        extractColumnName
    }
}