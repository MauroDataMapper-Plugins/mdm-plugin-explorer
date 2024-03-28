<#import "sqlExport-select.ftlh" as sqlExportSelect>

<#--------------------------------------------------------------
Drop the temp table if it exists
--------------------------------------------------------------->
<#macro dropTempTable cohortTableOrView>
IF OBJECT_ID(N'tempdb..${cohortTableOrView.tempTableName}') IS NOT NULL
    DROP TABLE [${cohortTableOrView.tempTableName}]
GO
</#macro>

<#--------------------------------------------------------------
Create the temporary table and populate with cohort data
--------------------------------------------------------------->
<#macro createAndPopulateTempTable cohortTableOrView>
<@dropTempTable cohortTableOrView=cohortTableOrView/>

CREATE TABLE [${cohortTableOrView.tempTableName}] (
    <#list cohortTableOrView.columns?sort_by("ordinal") as column>
    [${column.labelColumnName}] ${column.dataType} NOT NULL<#sep>,
    </#list>
)
GO

INSERT INTO [${cohortTableOrView.tempTableName}] (
    <#list cohortTableOrView.columns?sort_by("ordinal") as column>
    [${column.labelColumnName}]<#sep>,
    </#list>
)
<@sqlExportSelect.tableOrViewSelectQuery tableOrView=cohortTableOrView/>
</#macro>
