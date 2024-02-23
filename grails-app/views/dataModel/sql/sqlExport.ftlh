<#import "sqlExport-select.ftlh" as sqlExportSelect>

<#list sqlExportTables.tableOrViews as tableOrView>
    <@sqlExportSelect.tableOrViewSelect tableOrView=tableOrView/>
</#list>

