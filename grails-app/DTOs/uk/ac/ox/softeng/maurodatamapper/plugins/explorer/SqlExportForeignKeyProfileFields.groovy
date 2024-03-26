package uk.ac.ox.softeng.maurodatamapper.plugins.explorer

import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileField

class SqlExportForeignKeyProfileFields {
    private ProfileField schema;
    private ProfileField table;
    private ProfileField columns;

    SqlExportForeignKeyProfileFields(ProfileField schema, ProfileField table, ProfileField columns) {
        this.schema = schema
        this.table = table
        this.columns = columns
    }

    ProfileField getSchema() {
        schema
    }

    ProfileField getTable() {
        table
    }

    ProfileField getColumns() {
        columns
    }
}