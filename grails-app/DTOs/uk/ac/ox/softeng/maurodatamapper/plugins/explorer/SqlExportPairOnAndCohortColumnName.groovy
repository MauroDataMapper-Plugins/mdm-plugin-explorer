package uk.ac.ox.softeng.maurodatamapper.plugins.explorer

class SqlExportPairOnAndCohortColumnName {
    private String on
    private String cohortColumnName

    SqlExportPairOnAndCohortColumnName(String on, String cohortColumnName)
    {
        this.on = on
        this.cohortColumnName = cohortColumnName
    }

    String getOn() {
        on
    }

    String getCohortColumnName() {
        cohortColumnName
    }
}
