<#--------------------------------------------------------------
Indent spaces
--------------------------------------------------------------->
<#function indent spaces>
    <#assign indentation = "" />
    <#list 1..spaces as _>
        <#assign indentation = "${indentation} " />
    </#list>
    <#return indentation>
</#function>

<#--------------------------------------------------------------
Indent single line
--------------------------------------------------------------->
<#macro indentSingleLine line>
    <#assign indentSpaces = indentSpaces + indentSize>
    ${indent(indentSpaces)}${line}<#lt>
    <#assign indentSpaces = indentSpaces - indentSize>
</#macro>

<#--------------------------------------------------------------
Format Value Function
--------------------------------------------------------------->
<#function formatValue value dataType>
	<#local formattedValue = "">
    <#switch dataType >
		<#case "decimal">
		<#case "int">
			<#local formattedValue = "${value}">
			<#break>
		<#case "datetime">
			<#local formattedValue = "convert(datetime, '${value}', 103)">
			<#break>
		<#case "bit">
		    <#if value?upper_case == "TRUE" >
		        <#local formattedValue = "1">
		    <#else>
		        <#local formattedValue = "0">
		    </#if>
		    <#break>
        <#case "varchar">
		<#default>
			 <#local formattedValue = "'${value}'">
    </#switch>
	<#return formattedValue>
</#function>


<#--------------------------------------------------------------
Translate Operator Function
--------------------------------------------------------------->
<#function translateOperator operator value, dataType>
	<#local operatorLine = "">
    <#switch operator >
		<#case "endswith">
			<#local operatorLine = "LIKE '%${value}'">
			<#break>
		<#case "startswith">
			<#local operatorLine = "LIKE '${value}%'">
			<#break>
		<#case "in">
			<#local value = value?replace("[","(")?replace("]",")")>
			<#local operatorLine = "IN ${value}">
			<#break>
		<#case "not in">
			<#local value = value?replace("[","(")?replace("]",")")>
			<#local operatorLine = "IN ${value}">
			<#break>
		<#case "like">
		<#case "contains">
			<#local operatorLine = "LIKE '%${value}%'">
			<#break>
		<#default>
			 <#local operatorLine = "${operator?trim} ${formatValue(value, dataType)}">
    </#switch>
	<#return operatorLine>
</#function>

<#--------------------------------------------------------------
Where clause line function
--------------------------------------------------------------->
<#function whereClauseLine queryRule>
    <#local schemaAndTable = queryRule.entity?replace(".","].[") />
	<#local whereLine = "[${schemaAndTable}].[${queryRule.field}] ${translateOperator(queryRule.operator,queryRule.value, queryRule.dataType)?no_esc}">
	<#return whereLine>
</#function>

<#--------------------------------------------------------------
Add Where Clause Line Macro
--------------------------------------------------------------->
<#macro addWhereClauseLine queryRule condition>
    <#if firstWhereClauseLine == false>
		${indent(indentSpaces)}<#lt>${condition?upper_case}<#lt> ${whereClauseLine(queryRule)}
    <#else>
		${indent(indentSpaces)}${whereClauseLine(queryRule)}<#lt>
	</#if>
</#macro>

<#--------------------------------------------------------------
Process Rule
--------------------------------------------------------------->
<#macro processRule queryRule condition>
    <#if queryRule.condition??>
		<#assign indentSpaces = indentSpaces + indentSize>
		<#if firstWhereClauseLine == true>
		    ${indent(indentSpaces)}(<#lt>
		<#else>
		    ${indent(indentSpaces)}${queryRule.condition?upper_case} (<#lt>
		    <#assign firstWhereClauseLine = true>
		</#if>
        <#list queryRule.rules as queryRuleInternal>
            <@processRule queryRule=queryRuleInternal condition=queryRule.condition/>
			<#if firstWhereClauseLine == true>
				<#assign firstWhereClauseLine = false>
			</#if>
        </#list>
		${indent(indentSpaces)})<#lt>
		<#assign indentSpaces = indentSpaces - indentSize>
    <#else>
		<#assign indentSpaces = indentSpaces + indentSize>
        <@addWhereClauseLine queryRule=queryRule condition=condition/>
		<#assign indentSpaces = indentSpaces - indentSize>
        <#if firstWhereClauseLine == true>
            <#assign firstWhereClauseLine = false>
        </#if>
    </#if>
</#macro>

<#--------------------------------------------------------------
The Main template
--------------------------------------------------------------->
<#macro whereClause tableOrView>
    <#assign firstWhereClauseLine = true>
    <#assign indentSize = 4>
    <#assign indentSpaces = 0>
    <#if tableOrView.rules??>
        <#-- Make sure the JOIN is before the WHERE so separate JOIN and WHERE rules -->
        <#local joinList = []>
        <#local whereList = []>
        <#list tableOrView.rules as tableOrViewRule>
            <#if tableOrViewRule.label == 'join'>
                <#local joinList = joinList + [tableOrViewRule]>
            </#if>
            <#if tableOrViewRule.label == 'where'>
                <#local whereList = whereList + [tableOrViewRule]>
            </#if>
        </#list>
        <#list joinList as join>
            <#assign join_json = join.json?eval_json>
            JOIN<#lt>
            <@indentSingleLine line = join_json.join/>
            ON<#lt>
            <@indentSingleLine line = join_json.on/>
        </#list>
        <#list whereList as where>
            <#assign where_json = where.json?eval_json>
            <#assign firstWhereClauseLine = true>
            WHERE<#lt>
            <#list where_json.rules as queryRule>
                <@processRule queryRule=queryRule condition=where_json.condition/>
            </#list>
        </#list>
    </#if>
</#macro>