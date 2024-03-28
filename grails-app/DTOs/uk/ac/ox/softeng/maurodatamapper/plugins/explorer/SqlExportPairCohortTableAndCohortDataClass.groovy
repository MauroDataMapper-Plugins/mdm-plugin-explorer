package uk.ac.ox.softeng.maurodatamapper.plugins.explorer

import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass

class SqlExportPairCohortTableAndCohortDataClass {
    private DataClass cohortDataClass
    private SqlExportCohortTableOrView sqlExportCohortTableOrView

    SqlExportPairCohortTableAndCohortDataClass(SqlExportCohortTableOrView sqlExportCohortTableOrView,
                                               DataClass cohortDataClass ) {
        this.cohortDataClass = cohortDataClass
        this.sqlExportCohortTableOrView = sqlExportCohortTableOrView
    }

    SqlExportCohortTableOrView getSqlExportCohortTableOrView() {
        sqlExportCohortTableOrView
    }

    DataClass getCohortDataClass() {
        cohortDataClass
    }

}
