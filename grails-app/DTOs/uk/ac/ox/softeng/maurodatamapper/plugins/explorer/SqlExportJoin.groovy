package uk.ac.ox.softeng.maurodatamapper.plugins.explorer

class SqlExportJoin {
    private String table
    private List<String> on = []

    SqlExportJoin(String table)
    {
        this.table = table
    }

    String getTable() {
        table
    }

    List<String> getOn() {
        on
    }
}
