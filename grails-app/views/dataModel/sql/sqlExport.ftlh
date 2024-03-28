<#import "sqlExport-select.ftlh" as sqlExportSelect>
<#import "sqlExport-cohort.ftlh" as SqlExportCohort>

<#if sqlExportTables.cohortTableOrView??>
    <@SqlExportCohort.createAndPopulateTempTable cohortTableOrView=sqlExportTables.cohortTableOrView/>
</#if>
<#list sqlExportTables.tableOrViews as tableOrView>
    <@sqlExportSelect.tableOrViewSelectQuery tableOrView=tableOrView/>
</#list>
<#if sqlExportTables.cohortTableOrView??>
    <@SqlExportCohort.dropTempTable cohortTableOrView=sqlExportTables.cohortTableOrView/>
</#if>
