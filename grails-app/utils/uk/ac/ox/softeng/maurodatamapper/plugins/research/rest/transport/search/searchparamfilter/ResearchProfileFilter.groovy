/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.plugins.research.rest.transport.search.searchparamfilter

import uk.ac.ox.softeng.maurodatamapper.core.hibernate.search.mapper.pojo.bridge.MetadataBridge
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.DateTimeSearchParamFilter
import uk.ac.ox.softeng.maurodatamapper.plugins.research.ResearchDataElementProfileProviderService

import grails.plugins.hibernate.search.HibernateSearchApi
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 19/04/2022
 */
@Slf4j
class ResearchProfileFilter extends DateTimeSearchParamFilter {

    @Autowired
    ResearchDataElementProfileProviderService researchDataElementProfileProviderService

    @Override
    boolean doesApply(SearchParams searchParams) {
        searchParams.containsKey('researchFields')
    }

    @Override
    Closure getClosure(SearchParams searchParams) {
        Map<String, Object> profileFields = searchParams.getValue('researchFields')
        HibernateSearchApi.defineSearchQuery {
            must {
                profileFields.findAll {it.value}.each {key, filterTerm ->
                    String namespace = researchDataElementProfileProviderService.metadataNamespace
                    switch (key) {
                        case 'identifiableData':
                            String value = filterTerm
                            if (value.toLowerCase() == 'identifying') {
                                simpleQueryString('identifying + -maybe', MetadataBridge.makeSafeFieldName("${namespace}|${key}"))
                            } else {
                                phrase(MetadataBridge.makeSafeFieldName("${namespace}|${key}"), filterTerm)
                            }
                            break
                        case 'sourceSystem': case 'targetDataset': case 'terminology': case 'dataDictionaryItem': case 'databaseName':
                            phrase(MetadataBridge.makeSafeFieldName("${namespace}|${key}"), filterTerm)
                            break
                            // Date searching is not possible as the value is stored as a string and therefore indexed as a string which means we can't perform the desired
                            // index search on it
                            //                        case 'lastUpdatedAfter':
                            //                            above(MetadataBridge.makeSafeFieldName("${namespace}|${key}"), filterTerm)
                            //                            break
                            //                        case 'lastUpdatedBefore':
                            //                            below(MetadataBridge.makeSafeFieldName("${namespace}|${key}"), filterTerm)
                            //                            break
                        default:
                            log.warn('Unknown search field {}', key)
                    }
                }
            }
        }
    }
}
