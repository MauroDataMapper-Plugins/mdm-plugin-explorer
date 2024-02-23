<#import "sqlExport-where.ftlh" as sqlExportWhere>

<#macro tableOrViewSelect tableOrView>
SELECT
    <#list tableOrView.columns?sort_by("ordinal") as column>
    ${column.label}<#sep>,
    </#list>
${'\n'}FROM
    ${tableOrView.label}
<@sqlExportWhere.whereClause tableOrView=tableOrView/>
GO

</#macro>

