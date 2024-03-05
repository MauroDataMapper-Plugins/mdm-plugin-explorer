/*
 * Copyright 2020-2024 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.plugins.explorer

import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.gorm.mapping.MdmPluginExplorerSchemaMappingContext
import uk.ac.ox.softeng.maurodatamapper.plugins.explorer.rest.transport.search.searchparamfilter.ResearchProfileFilter

import grails.plugins.Plugin

class MdmPluginExplorerGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = '5.1.7 > *'
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Mauro Data Explorer Rest Api Plugin"
    // Headline display name of the plugin
    def author = "Aaron Forshaw"
    def authorEmail = "aaron@janda.org.uk"
    def description = '''\
Controller for endpoints specific to the mauro data explorer.
'''

    // URL to the plugin's documentation
    def documentation = ""

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
    def organization = [name: "Oxford University BRC Informatics", url: "www.ox.ac.uk"]

    // Any additional developers beyond the author specified above.
    def developers = [[name: 'Oliver Freeman', email: 'oliver.freeman@bdi.ox.ac.uk'],]

    // Location of the plugin's issue tracker.
    def issueManagement = [system: "YouTrack", url: "https://maurodatamapper.myjetbrains.com"]

    // Online location of the plugin's browseable source code.
    def scm = [url: "https://github.com/MauroDataMapper-Plugins/mdm-plugin-explorer"]

    def dependsOn = [
        mdmCore           : '5.2.0-SNAPSHOT > *',
        mdmPluginDatamodel: '5.2.0-SNAPSHOT > *',
        mdmPluginProfile  : '5.2.0-SNAPSHOT > *',
    ]

    Closure doWithSpring() {
        {->
            researchProfileFilter ResearchProfileFilter

            mdmPluginExplorerSchemaMappingContext MdmPluginExplorerSchemaMappingContext
        }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
