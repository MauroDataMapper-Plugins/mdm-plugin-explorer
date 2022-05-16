# mdm-plugin-explorer

| Branch | Build Status |
| ------ | ------------ |
| master | [![Build Status](https://jenkins.cs.ox.ac.uk/buildStatus/icon?job=Mauro+Data+Mapper+Plugins%2Fmdm-plugin-explorer%2Fmain)](https://jenkins.cs.ox.ac.uk/blue/organizations/jenkins/Mauro%20Data%20Mapper%20Plugins%2Fmdm-plugin-explorer/branches) |
| develop | [![Build Status](https://jenkins.cs.ox.ac.uk/buildStatus/icon?job=Mauro+Data+Mapper+Plugins%2Fmdm-plugin-explorer%2Fdevelop)](https://jenkins.cs.ox.ac.uk/blue/organizations/jenkins/Mauro%20Data%20Mapper%20Plugins%2Fmdm-plugin-explorer/branches) |

## Requirements

* Java 17 (Temurin)
* Grails 5.1.2+
* Gradle 7.3.3+

All of the above can be installed and easily maintained by using [SDKMAN!](https://sdkman.io/install).

## Applying the Plugin

The preferred way of running Mauro Data Mapper is using the [mdm-docker](https://github.com/MauroDataMapper/mdm-docker) deployment. However you can
also run the backend on its own from [mdm-application-build](https://github.com/MauroDataMapper/mdm-application-build).

## Usage

### Source Data and User Requests Folder

An API property `explorer.config.root_data_model_path` defines the location of the source Data Model i.e. the Data Model which the user browses, and from
which they select Data Elements. For example, if the source Data Model is called 'Source' and is located in a folder 'F', this property
must have a value of 'fo:F|dm:Source'.

The following API properties are also required:
- `explorer.config.root_request_folder` The name of a folder in the catalogue where all user requests will be stored. 
- `explorer.config.profile_namespace` The namespace of the profile to use.
- `explorer.config.profile_service_name` The profile name to use.

### Submitting data requests

To successfully send data requests to an organisation, these API properties must be defined in your Mauro instance:

- `email.research.request.recipient` Email address of the user who should receive a notification email when a research access request is
  submitted. This must match the email address of a registered Catalogue User.
- `email.research.request.subject` Subject line of the above email
- `email.research.request.body` Body of the above email

### Contact

To successfully send a contact form to an organistion, these API properties must be defined in your Mauro instance:

- `email.research.contact.recipient` Email address of the user who should receive a notification email when a contact form is 
  submitted. This must match the email address of a registered Catalogue User.
- `email.research.contact.subject` Subject line of the above email
- `email.research.contact.body` Body of the above email

### mdm-docker

In the `docker-compose.yml` file add:

```yml
mauro-data-mapper:
    build:
        args:
            ADDITIONAL_PLUGINS: "uk.ac.ox.softeng.maurodatamapper.plugins:mdm-plugin-explorer:1.0.0-SNAPSHOT"
```

Please note, if adding more than one plugin, this is a semicolon-separated list

### mdm-application-build

In the `build.gradle` file add:

```groovy
grails {
    plugins {
        runtimeOnly 'uk.ac.ox.softeng.maurodatamapper.plugins:mdm-plugin-explorer:1.0.0-SNAPSHOT'
    }
}
```
