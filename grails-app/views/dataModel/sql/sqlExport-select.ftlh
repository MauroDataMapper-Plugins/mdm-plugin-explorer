<#import "sqlExport-where.ftlh" as sqlExportWhere>

<#--------------------------------------------------------------
Create the select query
--------------------------------------------------------------->
<#macro tableOrViewSelectQuery tableOrView>
SELECT
    <#list tableOrView.columns?sort_by("ordinal") as column>
    ${column.label}<#sep>,
    </#list>
${'\n'}FROM
    ${tableOrView.label}
<@sqlExportWhere.whereClause tableOrView=tableOrView/>
GO

</#macro>

