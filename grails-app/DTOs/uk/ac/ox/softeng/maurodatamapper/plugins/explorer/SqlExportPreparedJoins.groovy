package uk.ac.ox.softeng.maurodatamapper.plugins.explorer

class SqlExportPreparedJoins {
    private List<SqlExportJoin> sqlExportJoins = []
    private List<String> cohortColumnNames = []

    List<SqlExportJoin>  getSqlExportJoins() {
        sqlExportJoins
    }

    List<String> getCohortColumnNames() {
        cohortColumnNames
    }
}
