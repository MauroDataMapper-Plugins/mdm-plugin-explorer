# mdm-plugin-explorer

| Branch  | Build Status                                                                                                                                                                                                                                         |
| ------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| master  | [![Build Status](https://jenkins.cs.ox.ac.uk/buildStatus/icon?job=Mauro+Data+Mapper+Plugins%2Fmdm-plugin-explorer%2Fmain)](https://jenkins.cs.ox.ac.uk/blue/organizations/jenkins/Mauro%20Data%20Mapper%20Plugins%2Fmdm-plugin-explorer/branches)    |
| develop | [![Build Status](https://jenkins.cs.ox.ac.uk/buildStatus/icon?job=Mauro+Data+Mapper+Plugins%2Fmdm-plugin-explorer%2Fdevelop)](https://jenkins.cs.ox.ac.uk/blue/organizations/jenkins/Mauro%20Data%20Mapper%20Plugins%2Fmdm-plugin-explorer/branches) |

## Requirements

- Java 17 (Temurin)
- Grails 5.1.2+
- Gradle 7.3.3+

All of the above can be installed and easily maintained by using [SDKMAN!](https://sdkman.io/install).

## Applying the Plugin

The preferred way of running Mauro Data Mapper is using the [mdm-docker](https://github.com/MauroDataMapper/mdm-docker) deployment. However you can
also run the backend on its own from [mdm-application-build](https://github.com/MauroDataMapper/mdm-application-build).

## Usage

### Source Data and User Data Specifications Folder

An API property `explorer.config.root_data_model_path` defines the location of the source Data Model i.e. the Data Model which the user browses, and from
which they select Data Elements. For example, if the source Data Model is called 'Source' and is located in a folder 'F', this property
must have a value of 'fo:F|dm:Source'. You can retrieve this root data model using the endpoint `GET /explorer/rootDataModel`.

The following API properties are also required:

- `explorer.config.root_data_specification_folder` The name of a folder in the catalogue where all user data specifications will be stored.
- `explorer.config.profile_namespace` The namespace of the profile to use.
- `explorer.config.profile_service_name` The profile name to use.

### Data Specifications Templates Folder

A folder is automatically bootstrapped called "Mauro Data Explorer Templates", which is provided to store optional template data specifications to base user data specifications
on. The folder is also setup to use the user group "Explorer Readers" with the "readers" access level to ensure a correct securable resource. No standard
template data specifications are provided though, these must be created by an administrator.

The location of this template folder is stored in the `explorer.config.root_template_folder` API property. If you use a different folder, ensure that this
API property is also updated.

### Submitting data specifications

To successfully send data specifications to an organisation, these API properties must be defined in your Mauro instance:

- `email.research.data_specification.recipient` Email address of the user who should receive a notification email when a research access data specification is
  submitted. This must match the email address of a registered Catalogue User.
- `email.research.data_specification.subject` Subject line of the above email
- `email.research.data_specification.body` Body of the above email

### Contact

To successfully send a contact form to an organistion, these API properties must be defined in your Mauro instance:

- `email.research.contact.recipient` Email address of the user who should receive a notification email when a contact form is
  submitted. This must match the email address of a registered Catalogue User.
- `email.research.contact.subject` Subject line of the above email
- `email.research.contact.body` Body of the above email

### Theming

The plugin will automatically bootstrap a set of API properties related to theming an explorer user interface, all prefixed with `explorer.theme.*`. Altering these
values can then affect the appearance of the explorer user interface, such as colour scheme.

Below are the core colour properties that can be set to define the overall colour scheme:

| Key                                    | Description                                         | Default Value |
| -------------------------------------- | --------------------------------------------------- | ------------- |
| explorer.theme.material.colors.primary | The primary, most prominent colour of the interface | #19381f       |
| explorer.theme.material.colors.accent  | The secondary colour of the interface               | #cdb980       |
| explorer.theme.material.colors.warn    | The colour to use for errors, warnings or alerts    | #a5122a       |

Additional colours can be set as follows. They are split into two types:

1. Regular colours - for screen elements that just require one colour value.
2. Contrasting colours - for screen elements that need an accompanying colour value for contrast. These colours are typically background colours, so a suitable foreground colour can automatically be determined.

| Key                                                        | Description                                                  | Default Value |
| ---------------------------------------------------------- | ------------------------------------------------------------ | ------------- |
| explorer.theme.regularcolors.hyperlink                     | Colour for hyperlinks                                        | #003752       |
| explorer.theme.regularcolors.data-specification-count      | Colour for the counter in the header bar                     | #ffe603       |
| explorer.theme.contrastcolors.page                         | Overall background page colour                               | #fff          |
| explorer.theme.contrastcolors.unsent-data-specification    | Colour of status tags for unsent specifications              | #008bce       |
| explorer.theme.contrastcolors.submitted-data-specification | Colour of status tags for submitted specifications           | #0e8f77       |
| explorer.theme.contrastcolors.classrow                     | Colour of data class rows when listed in data specifications | #c4c4c4       |

Finally, typography can be adjusted for fonts and text sizes. Apart from `fontfamily`, all properties below define the font size, line height and font weight (in order, comma-separated):

| Key                                              | Description                                      | Default Value                        |
| ------------------------------------------------ | ------------------------------------------------ | ------------------------------------ |
| explorer.theme.material.typography.fontfamily    | Define the font family to use for all text       | Roboto, "Helvetica Neue", sans-serif |
| explorer.theme.material.typography.bodyone       | Base body text                                   | 14px, 20px, 400                      |
| explorer.theme.material.typography.bodytwo       | Bolder body text                                 | 14px, 24px, 500                      |
| explorer.theme.material.typography.headline      | Section heading corresponding to the `<h1>` tag. | 24px, 32px, 400                      |
| explorer.theme.material.typography.title         | Section heading corresponding to the `<h2>` tag. | 20px, 32px, 500                      |
| explorer.theme.material.typography.subheadingtwo | Section heading corresponding to the `<h3>` tag. | 16px, 28px, 400                      |
| explorer.theme.material.typography.subheadingone | Section heading corresponding to the `<h4>` tag. | 15px, 24px, 400                      |
| explorer.theme.material.typography.button        | Buttons and anchors.                             | 14px, 14px, 400                      |

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
