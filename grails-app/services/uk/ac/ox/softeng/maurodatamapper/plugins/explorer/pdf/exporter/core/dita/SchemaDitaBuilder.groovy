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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.pdf.exporter.core.dita

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic

class SchemaDitaBuilder {
    static Topic buildDataSchemaTopic(DataModel dataModel) {
        Topic.build(id: 'dataschemas') {
            title 'Schemas'
            dataModel.childDataClasses.eachWithIndex {dc, i ->
                topic(buildDataClassBit(dc, '', i))
            }
        }
    }

    private static Topic buildDataClassTopic(DataClass dc, String dcId) {
        Topic.build(id: "${dcId}.dataclasses") {
            title 'Data Classes'
            if (dc.dataClasses) {
                dc.dataClasses.eachWithIndex {cdc, j ->
                    topic(buildDataClassBit(cdc, "${dcId}.", j))
                }
            }
        }
    }

    private static Topic buildDataClassBit(DataClass dc, String idPrefix, int i) {
        String dcId = "${idPrefix}dc[${i}]"
        return Topic.build {
            id dcId
            title dc.label
            shortdesc dc.description

            if (dc.dataClasses) {
                topic(buildDataClassTopic(dc, dcId))
            }
            if (dc.dataElements) {
                topic {
                    title 'DataElements'
                    body {
                        table {
                            tgroup(cols: 3) {
                                tHead {
                                    row {
                                        entry(colName: 'name') {txt 'Name'}
                                        entry(colName: 'datatype') {txt 'DataType'}
                                        entry(colName: 'description') {txt 'Description'}
                                    }
                                }
                                tBody {

                                    dc.dataElements.sort().each {dt ->
                                        row {
                                            entry(colName: 'name') {txt dt.label}
                                            entry(colName: 'datatype') {txt dt.dataType.label}
                                            entry(colName: 'description') {txt dt.description}
                                        }
                                    }


                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
