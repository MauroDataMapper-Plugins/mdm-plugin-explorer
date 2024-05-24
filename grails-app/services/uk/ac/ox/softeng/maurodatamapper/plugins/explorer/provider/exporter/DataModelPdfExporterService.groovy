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
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.provider.exporter

import groovy.util.logging.Slf4j
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter.DataModelExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.dita.processor.DitaProcessor
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.pdf.exporter.core.dita.DataSpecificationDitaBuilder
import uk.ac.ox.softeng.maurodatamapper.security.User

@Slf4j
class DataModelPdfExporterService extends DataModelExporterProviderService {

    public static final CONTENT_TYPE = 'application/mauro.datamodel+pdf'

    @Override
    String getDisplayName() {
        'PDF Data Specification Exporter'
    }

    @Override
    String getVersion() {
        '1.0'
    }

    @Override
    String getFileExtension() {
        'pdf'
    }

    @Override
    String getContentType() {
        CONTENT_TYPE
    }

    @Override
    int getOrder() {
        HIGHEST_PRECEDENCE
    }

    @Override
    Boolean canExportMultipleDomains() {
        false
    }

    @Override
    ByteArrayOutputStream exportDataModel(User currentUser, DataModel dataModel, Map<String, Object> parameters) throws ApiException {
        try {

            DataSpecificationDitaBuilder dataSpecificationDitaBuilder = new DataSpecificationDitaBuilder()
            def ditaProject = dataSpecificationDitaBuilder.buildDitaProject(dataModel)
            byte[] fileContents = new DitaProcessor().generatePdf(ditaProject)

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
            outputStream.write(fileContents)
            outputStream
        }
        catch (Exception e) {
            log.error(e.message)
            throw e
        }
    }

    @Override
    ByteArrayOutputStream exportDataModels(User currentUser, List<DataModel> dataModels, Map<String, Object> parameters) throws ApiException {
        throw new Exception('Multiple dataModels cannot be exported')
    }

}
