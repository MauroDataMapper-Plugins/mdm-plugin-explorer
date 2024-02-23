/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
import uk.ac.ox.softeng.maurodatamapper.plugins.database.sqlserver.column.SqlServerColumnProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlType
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRule
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRuleBase
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.MeqlRuleSet
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportColumn
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportRule
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportTableOrView
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.SqlExportTables
import uk.ac.ox.softeng.maurodatamapper.security.User

import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateExceptionHandler
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class DataModelSqlExporterService extends DataModelExporterProviderService {
    @Autowired
    SqlServerColumnProfileProviderService sqlServerColumnProfileProviderService
    public static final CONTENT_TYPE = 'application/mauro.datamodel+sql'


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
        def dataModelWithRules = sortAndGroupRules(dataModel)
        generateDdl(dataModelWithRules)
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

    SqlExportTables sortAndGroupRules(DataModel dataModel) {
        SqlExportTables sqlExportTables = new SqlExportTables()

        MeqlRuleSet cohortRuleGroup = null
        MeqlRuleSet dataRuleGroup = null

        // What we are trying to achieve is to manipulate the DataModel into a form that the template
        // can easily process. Each table is represented by a DataClass. We want to add the associated rules
        // from the MEQL as a new rule to each DataClass. Once these are in place we can export the model,
        // and the associated rules will be exported as where clauses.

        // First get a strongly typed model representation of the meql rules
        dataModel.rules.each {dataModelRule ->
            // Only process rules that have the expected name
            if (dataModelRule.name == 'cohort' || dataModelRule.name == 'data') {
                if (dataModelRule.ruleRepresentations) {
                    dataModelRule.ruleRepresentations.each {ruleRepresentation ->
                        // Only process rules that have the expected language type
                        if (ruleRepresentation.language == 'json-meql') {
                            if (dataModelRule.name == 'cohort') {
                                cohortRuleGroup = transformJsonStringToMeql((String) ruleRepresentation.representation, dataModel)
                            } else {
                                dataRuleGroup = transformJsonStringToMeql((String) ruleRepresentation.representation, dataModel)
                            }
                        }
                    }
                }
            }
        }

        // Now group and sort
        dataModel.childDataClasses.each {schema ->
            schema.dataClasses.each {tableOrView ->
                def dataTableOrViewClone = new SqlExportTableOrView(schema.label, tableOrView.label)

                def entity = "$schema.label.$tableOrView.label"
                MeqlRuleSet entityRules = null
                if (cohortRuleGroup?.entity == entity) {
                    entityRules = cohortRuleGroup
                }

                def ruleForEntity = dataRuleGroup?.rules?.find {rule ->
                    (rule as MeqlRuleSet).entity == entity
                }

                if (ruleForEntity) {
                    // If we already have rules for this table (a cohort query)
                    // then add the data query rules to the cohort query rules.
                    // Otherwise the data query is the ruleset.
                    if (entityRules) {
                        entityRules.rules.add(ruleForEntity)

                    } else {
                        entityRules = ruleForEntity as MeqlRuleSet
                    }
                }

                if (entityRules) {
                    // Work out if we need to join to other tables
                    def distinctEntities = getDistinctEntities(entityRules)

                    // Remove the main entity, we don't want to join to itself
                    distinctEntities = distinctEntities.findAll {it != entity}

                    if (distinctEntities.size() > 0) {
                        // We have more than one entity in the ruleSet tree so gather foreign key references
                        distinctEntities.each(referenceEntity -> {
                            def referenceSchemaLabel = referenceEntity.replace("$schema.label.", "")
                            def referenceParts = referenceSchemaLabel.split('\\.')
                            def referenceSchemaName = referenceParts[0]
                            def referenceTableName = referenceParts[1]
                            def referenceSchema = dataModel.childDataClasses.find {it.label == referenceSchemaName.trim()}
                            def referenceDataClass = referenceSchema.findDataClass(referenceTableName)
                            referenceDataClass.dataElements.each(dataElement -> {
                                // Get foreign keys from columns
                                def sqlProfile = sqlServerColumnProfileProviderService.createProfileFromEntity(dataElement)
                                if (sqlProfile?.sections?.fields?.size() > 0) {
                                    def sqlProfileFields = sqlProfile.sections.fields[0]
                                    def foreignKey = sqlProfileFields.find((x) -> {x.metadataPropertyName == "foreign_key_name"})
                                    if (foreignKey?.currentValue) {
                                        def foreignKeySchema = sqlProfileFields.find((profileField) -> {profileField.metadataPropertyName == "foreign_key_schema"})
                                        def foreignKeyTable = sqlProfileFields.find((profileField) -> {profileField.metadataPropertyName == "foreign_key_table"})
                                        def foreignKeyColumns = sqlProfileFields.find((profileField) -> {profileField.metadataPropertyName == "foreign_key_columns"})
                                        if (foreignKeySchema && foreignKeyTable && foreignKeyColumns) {
                                            def join = "{ \"join\": \"[${dataElement.dataClass.parentDataClass.label}].[${dataElement.dataClass.label}]\", \"on\": \"[${dataElement.dataClass.parentDataClass.label}].[${dataElement.dataClass.label}].[${dataElement.label}] = [${foreignKeySchema.currentValue}].[${foreignKeyTable.currentValue}].[${foreignKeyColumns.currentValue}]\" }" +
                                                       ""
                                            // addRuleToTableOrView(tableOrView, 'sqlJoin', 'json-sql-join', join)
                                            dataTableOrViewClone.rules.add(new SqlExportRule('join', join))
                                        }
                                    }
                                }
                            })
                        })

                    }

                    def entityRulesJson = JsonOutput.toJson(entityRules)
                    dataTableOrViewClone.rules.add(new SqlExportRule('where', entityRulesJson))
                }

                tableOrView.dataElements.each((dataElement) -> {
                    dataTableOrViewClone.columns.push(
                        new SqlExportColumn("[${schema.label}].[${tableOrView.label}].[${dataElement.label}]", dataTableOrViewClone.columns.size()))
                }
                )
                sqlExportTables.tableOrViews.add(dataTableOrViewClone)
            }

        }

        // Return the updated DataModel
        sqlExportTables
    }

    MeqlRuleSet processMeqlRuleGroup(MeqlRuleSet sourceRuleGroup) {
        MeqlRuleSet ruleGroup = new MeqlRuleSet(sourceRuleGroup.condition, sourceRuleGroup.entity, [])
        sourceRuleGroup.rules.each {meqlRule ->
            if (meqlRule.hasProperty("condition")) {
                MeqlRuleSet childMeqlRuleGroup = processMeqlRuleGroup(meqlRule as MeqlRuleSet)
                if (childMeqlRuleGroup.rules.size() > 0) {
                    ruleGroup.rules.push(childMeqlRuleGroup)
                }
            } else {
                ruleGroup.rules.push(meqlRule)
            }
        }
        return ruleGroup
    }

    MeqlRuleSet transformJsonStringToMeql(String representation, DataModel dataModel) {
        def meqlJsonObject = new JsonSlurper().parseText(representation)
        def rootRuleGroup = convertToTypedObject(meqlJsonObject, dataModel)
        rootRuleGroup as MeqlRuleSet
    }

    MeqlRuleBase convertToTypedObject(def json, DataModel dataModel) {
        if (json instanceof Map) {
            if (json.containsKey('condition')) {
                // Convert to MeqlRuleGroup
                return new MeqlRuleSet(
                    json.condition as String,
                    json.entity as String,
                    json.rules.collect {convertToTypedObject(it, dataModel)}.flatten() as List
                )
            } else if (json.containsKey('field') && json.containsKey('operator') && json.containsKey('value')) {
                // Get the type
                def entityParts = (json.entity as String).split('\\.')
                def dataSchema = dataModel.dataClasses.find((dataClass) -> {dataClass.label == entityParts[0]})
                def dataTable = dataSchema.dataClasses.find((dataClass) -> {dataClass.label == entityParts[1]})
                def dataElement = dataTable.dataElements.find((dataElement) -> {dataElement.label == json.field as String})
                def dataType =  (dataElement.dataType.domainType == "PrimitiveType") ? dataElement.dataType.label : 'NOT_PRIMITIVE'

                // Convert to MeqlRule
                return new MeqlRule(
                    json.entity as String,
                    json.field as String,
                    dataType,
                    json.operator as String,
                    json.value as String
                )
            }
        }
        return null
    }

    // Function to extract distinct "entity" values from a rules object
    List<String> extractEntities(List<MeqlRuleBase> entityRules) {
        List<String> entities = []
        entityRules.each { ruleObject ->
            entities << ruleObject.entity
            if (ruleObject.meqlType == MeqlType.RuleSet) {
                entities.addAll(extractEntities((ruleObject as MeqlRuleSet).rules))
            }
        }
        return entities
    }

    List<String> getDistinctEntities(MeqlRuleSet entityRules) {

        // Extract distinct "entity" values from the JSON object
        def distinctEntities = extractEntities(entityRules.rules)

        // Remove duplicates and print the distinct "entity" values
        distinctEntities.unique()
    }
}
