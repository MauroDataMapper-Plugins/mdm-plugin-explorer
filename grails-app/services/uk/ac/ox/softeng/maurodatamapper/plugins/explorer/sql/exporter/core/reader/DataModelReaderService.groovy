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
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement

class DataModelReaderService {
    /**
     * Find and return a DataClass from the DataModel by finding a DataClass that matches the schema name
     * and then then finding a child of that DataClass that matches the table name.
     */
    static DataClass getDataClass(DataModel dataModel, String schemaName, String tableName) {
        def referenceSchema = dataModel.childDataClasses.find {it.label == schemaName.trim()}
        if (!referenceSchema) {
            return null
        }
        return referenceSchema.findDataClass(tableName)
    }

    /**
     * Return a DataElement from a DataClass that's label matches the passed in date element name
     */
    static DataElement getDataElement(DataClass dataClass, String dataElementName) {
        return dataClass.dataElements.find{it.label == dataElementName.trim()}
    }
}
