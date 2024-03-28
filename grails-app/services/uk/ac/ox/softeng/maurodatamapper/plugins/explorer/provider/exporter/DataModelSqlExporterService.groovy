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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter.DataModelExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportTables
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.sql.exporter.core.SqlExportTableBuilderService
import uk.ac.ox.softeng.maurodatamapper.security.User

import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateExceptionHandler
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class DataModelSqlExporterService extends DataModelExporterProviderService {

    public static final CONTENT_TYPE = 'application/mauro.datamodel+sql'

    @Autowired
    SqlExportTableBuilderService sqlExportDataService

    @Override
    String getDisplayName() {
        'SQL DataModel Exporter'
    }

    @Override
    String getVersion() {
        '1.0'
    }

    @Override
    String getFileExtension() {
        'sql'
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
        def sqlExportTables = sqlExportDataService.prepareSqlExport(dataModel)
        generateDdl(sqlExportTables)
    }

    @Override
    ByteArrayOutputStream exportDataModels(User currentUser, List<DataModel> dataModels, Map<String, Object> parameters) throws ApiException {
        throw new Exception('Multiple dataModels cannot be exported')
    }

    ByteArrayOutputStream generateDdl(SqlExportTables sqlExportTables) {
        Configuration cfg = configureFreeMarker()
        Template dataModelTemplate = cfg.getTemplate("sqlExport.ftlh")

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        Writer out = new OutputStreamWriter(outputStream)
        try {
            dataModelTemplate.process(["sqlExportTables": sqlExportTables, "strip_whitespace": true], out)
        }
        catch (Exception e) {
            log.error(e.message)
            throw e
        }

        return outputStream

    }

    Configuration configureFreeMarker() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32)

        // Specify the source where the template files come from. Here I set a
        // plain directory for it, but non-file-system sources are possible too:
        //cfg.setDirectoryForTemplateLoading(new File("/where/you/store/templates"))
        cfg.setClassForTemplateLoading(this.getClass(), "/dataModel/sql")

        // From here we will set the settings recommended for new projects. These
        // aren't the defaults for backward compatibility.

        // Set the preferred charset template files are stored in. UTF-8 is
        // a good choice in most applications:
        cfg.setDefaultEncoding("UTF-8")

        // Sets how errors will appear.
        // During web page *development* TemplateExceptionHandler.HTML_DEBUG_HANDLER is better.
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER)

        // Don't log exceptions inside FreeMarker that it will thrown at you anyway:
        cfg.setLogTemplateExceptions(false)

        // Wrap unchecked exceptions thrown during template processing into TemplateException-s:
        cfg.setWrapUncheckedExceptions(true)

        // Do not fall back to higher scopes when reading a null loop variable:
        cfg.setFallbackOnNullLoopVariable(false)

        // To accommodate to how JDBC returns values; see Javadoc!
        cfg.setSQLDateAndTimeTimeZone(TimeZone.getDefault())

        return cfg

    }

}
