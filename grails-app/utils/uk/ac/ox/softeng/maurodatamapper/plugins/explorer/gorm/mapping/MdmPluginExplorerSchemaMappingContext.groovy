package uk.ac.ox.softeng.maurodatamapper.plugins.explorer.gorm.mapping

import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.PluginSchemaHibernateMappingContext

/**
 * @since 16/05/2022
 */
class MdmPluginExplorerSchemaMappingContext extends PluginSchemaHibernateMappingContext {

    @Override
    String getPluginName() {
        'mdmPluginExplorer'
    }

    @Override
    String getSchemaName() {
        'explorer'
    }
}
