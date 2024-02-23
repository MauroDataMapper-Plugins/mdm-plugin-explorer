/*
 * Copyright 2020-2023 University of Oxford and NHS England
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

class SqlExportColumn {

    private String label
    private int ordinal

    SqlExportColumn(String label, int ordinal) {
        this.label = label
        this.ordinal = ordinal
    }

    String getLabel() {
        label
    }

    String getOrdinal() {
        ordinal
    }
}