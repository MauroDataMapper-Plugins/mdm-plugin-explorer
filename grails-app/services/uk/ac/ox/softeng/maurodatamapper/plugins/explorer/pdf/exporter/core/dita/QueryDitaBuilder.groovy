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

import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Body
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Section
import uk.ac.ox.softeng.maurodatamapper.dita.elements.langref.base.Topic
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.pdf.exporter.core.PdfExportFormatterService

class QueryDitaBuilder {
    static Topic buildQueriesTopic(DataModel dataModel) {
        Topic.build(id: 'queries') {
            title 'Queries'
            body(buildQueriesBody(dataModel))
        }
    }

    private static Body buildQueriesBody(CatalogueItem catalogueItem) {
        Body.build {
            section buildQueriesSection(catalogueItem.rules, 'cohort')
            section buildQueriesSection(catalogueItem.rules, 'data')
        }
    }

    private static Section buildQueriesSection(Collection<Rule> rules, String queryType) {
        def queryName = "${PdfExportFormatterService.capitalizeFirstLetter(queryType)} Query"

        def query = rules?.find(rule -> rule.name == queryType )
        // MEQL rules only ever have 1 representation so we can just get the first
        def meql = (query)
                ? PdfExportFormatterService.meqlJsonToMeql(query.ruleRepresentations.first().representation)
                : "No query defined"

        Section.build {
            simpletable {
                stHead {
                    stentry {txt queryName}
                }

                strow {
                    stentry { pre { txt meql } }
                }
            }
        }
    }
}
