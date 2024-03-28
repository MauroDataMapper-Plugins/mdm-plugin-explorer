package uk.ac.ox.softeng.maurodatamapper.plugins.explorer

class SqlExportPairJoinAndCohortColumnNames {
    private SqlExportJoin sqlExportJoin
    private List<String> cohortColumnNames = []

    SqlExportPairJoinAndCohortColumnNames(SqlExportJoin sqlExportJoin)
    {
        this.sqlExportJoin = sqlExportJoin
    }

    SqlExportJoin getSqlExportJoin() {
        sqlExportJoin
    }

    List<String> getCohortColumnNames() {
        cohortColumnNames
    }
}
