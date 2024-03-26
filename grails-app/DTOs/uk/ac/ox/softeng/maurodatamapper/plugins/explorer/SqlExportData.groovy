package uk.ac.ox.softeng.maurodatamapper.plugins.explorer

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass

class SqlExportData {
    private DataModel dataModel
    private DataClass cohortDataClass

    private MeqlRuleSet cohortRuleGroup
    private MeqlRuleSet dataRuleGroup

    private SqlExportTables sqlExportTables

    SqlExportData(DataModel dataModel, MeqlRuleSet cohortRuleGroup, MeqlRuleSet dataRuleGroup, SqlExportTables sqlExportTables) {
        // sqlExportTables = new SqlExportTables()
        this.dataModel = dataModel
        this.cohortRuleGroup = cohortRuleGroup
        this.dataRuleGroup = dataRuleGroup
        this.sqlExportTables = sqlExportTables
    }

    SqlExportTables getSqlExportTables() {
        sqlExportTables
    }

    DataModel getDataModel() {
        dataModel
    }

    DataClass getCohortDataClass() {
        cohortDataClass
    }

    DataClass setCohortDataClass(DataClass cohortDataClass) {
        this.cohortDataClass = cohortDataClass
    }

    MeqlRuleSet getCohortRuleGroup() {
        cohortRuleGroup
    }

    MeqlRuleSet setCohortRuleGroup(MeqlRuleSet cohortRuleGroup) {
        this.cohortRuleGroup = cohortRuleGroup
    }

    MeqlRuleSet getDataRuleGroup() {
        dataRuleGroup
    }

    MeqlRuleSet setDataRuleGroup(MeqlRuleSet dataRuleGroup) {
        this.dataRuleGroup = dataRuleGroup
    }

}
