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
import uk.ac.ox.softeng.maurodatamapper.dita.DitaProject
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.TopicRef
import uk.ac.ox.softeng.maurodatamapper.dita.enums.Toc

class DataSpecificationDitaBuilder {
    static DitaProject buildDitaProject(DataModel dataModel) {
        new DitaProject(dataModel.label, dataModel.label
        ).tap {
            registerTopic("", Topic.build(id: "datamodel") {
                title dataModel.label
                topic(buildDescriptionTopic(dataModel))
                topic(QueryDitaBuilder.buildQueriesTopic(dataModel))
                topic(SchemaDitaBuilder.buildDataSchemaTopic(dataModel))

            })
            mainMap.topicRef(new TopicRef(keyRef: "datamodel", toc: Toc.YES))
        }
    }

    private static Topic buildDescriptionTopic(DataModel dataModel) {
        Topic.build(id: 'queries') {
            title 'Description'
            shortdesc dataModel.description ?: 'No description provided'
        }
    }
}
