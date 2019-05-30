#OMH-on-FHIR
Spring Boot Implementation of the OMH on FHIR application described here: https://healthedata1.github.io/mFHIR/#smart-app-workflow

It makes web service endpoints available for:
1) authentication via Shimmer
2) DocumentReference query to query data via Shimmer
3) Binary query to retrieve Shimmer data returned by a DocumentReference query
4) Observation query to retrieve Shimmer OMH data as FHIR Observations

## Project Structure
```
application
    src                                                                 --> Contains project source code
        main                                                            --> Contains main project code
            java
                org
                    gtri
                        hdap
                            mdata
                                conf                                    --> Contains project configuration
                                    JacksonConfig.java                  --> Configures Jackson for JSON serialization/deserialization
                                    SwaggerConfig.java                  --> Configures swagger
                                controller
                                    FhirTemplateController.java         --> Defines controller with endpoints for FHIR template endpoints
                                    ResourceConfigController.java       --> Defines controller with endpoints for resource config endpoints
                                    SessionMetaData.java                --> Defines an object to store session metadata
                                    ShimmerAuthController.java          --> Defines controller with endpoints for Shimmer authentication endpoints
                                    Stu3PatientDataController.java      --> Defines controller with endpoints for patient endpoints
                                jackson
                                    BindingResultSerializer.java        --> Serializer for BeanPropertyBindingResult
                                    HapiStu3Serializer.java             --> Serializer for HAPI FHIR STU3 Domain Objects
                                jpa                                     --> Contains objects to configure database interactions
                                    entity                              --> Contains objects for data to persist to the database
                                        ApplicationUser.java            --> Defines data structure for an application users
                                        ApplicationUserId.java          --> Defines data structure user ids
                                        FhirTemplate.java               --> Defines data structure for FHIR templates
                                        ResourceConfig.java             --> Defines data structure for resource configs
                                        ShimmerData.java                --> Defines data structure for shimmer data
                                    repository                          --> Exposes methods to search the database
                                        ApplicationUserRepository.java  --> Defines CRUD methods for the application_user database table
                                        FhirTemplateRepository.java     --> Defines CRUD methods for the fhir_template database table
                                        ResourceConfigRepository.java   --> Defines CRUD methods for the resource_config database table
                                        ShimmerDataRepository.java      --> Defines CRUD methods for the shimmer_data database table
                                service
                                    ShimmerAuthenticationException.java --> Provides an authentication exception.
                                    ShimmerAuthService.java             --> Provides functionality to authenticate to Shimmer.
                                    ShimmerQueryParsingException.java   --> Provides a query parsing exception.
                                    ShimmerResponse.java                --> Provides an object to represent a response from Shimmer.
                                    ShimmerService.java                 --> Provides methods to interact with the Shimmer web service. It is used by the controllers.
                                    Stu3ResponseService.java            --> Provides functionality to generate STU3 Responses.
                                    UnsupportedFhirDatePrefixException.java --> Provides an unsupported FHIR date exception.
                                util
                                    ShimmerUtil.java                    --> Provides utility constants and methods.
                                InitDatabase.java                       --> Handles database initialization.
                                MdataServerApplication.java             --> Launches the application
            resources                                                   --> Contains application resources
                application.properties                                  --> Defines properties for the application
                logback.xml                                             --> Configures application logging
        test                                                            --> Contains project test code
            java
                org
                    gtri
                        hdap
                            mdata
                                controller
                                    PatientDataControllerIntegrationTest--> Integration test for the PatientDataController
                                    PatientDataControllerTest           --> Unit test for the PatientDataController
                                jpa
                                    repository
                                        TestApplicationUserRepository   --> Unit test for the ApplicationUserRepository
                                service
                                    ShimmerServiceTest                  --> Unit test for the ShimmerService
                                    Stu3ResponseServiceTest             --> Unit test for the Stu3ResponseService
                                MdataServerApplicationTests             --> Unit test for the MdataServerApplication
            resources                                                   --> Contains testing resources
                test.application.properties                             --> Properties to use for testing
config                                                                  --> Contains the project with configuration for mapping OMH to FHIR resources
    src                                                                 --> Contains project source code
        main                                                            --> Contains main project code
        resoruces                                                       --> Contains project resources
            fhirTemplates                                               --> Contains templates files to use for FHIR resources
                stu3_Observation.json                                   --> Template to use for STU3 Observatisons
            resourceConfigs                                             --> Contains mappings for OMH to FHIR resources
                stu3_step_count.json                                    --> Mapping for OMH step_count to FHIR STU3 Observation
Dockerfile                                                          --> Configures the Docker image for this project
pom.xml                                                             --> Configures Maven
README.md                                                           --> This file.
```

## Constraints

- DocumentReference search can *ONLY* support two date parameters, one for the start date and one for the end date.
If the start date uses a prefix it must be `ge`. If the end date uses a prefix it must be `le`. The application only searches
between for documents between the specified date ranges.

##Configuration

The following environment variables need to be set for the `mdata-app` container
- `SHIMMER_SERVER_URL` the URL to the Shimmer console container
- `SHIMMER_REDIRECT_URL` the URL to the mdata-app /authorize/fitbit/callback endpoint
- `OMH_ON_FHIR_CALLBACK` the URL to the OMH on FHIR UI application that should be used after successful authentication
- `OMH_ON_FHIR_LOGIN` the URL to the OMH on FHIR UI login page

The following environment variables need to be set for the `mdata-db` container
- `POSTGRES_DB` the database to create/use
- `POSTGRES_USER` the database user
- `POSTGRES_PASSWORD` the password for the database user

##To Run

1) Create a `./shimmer-resource-server.env` file with environment variable to configure the Shimmer server, https://github.com/openmhealth/shimmer/blob/e3fef06d4d7d5f93d2a45e7656a823889f247499/resource-server.env, Place the file in the root directory of the project.
2) Create a `./postgres.env` file with environment variables to configure the Postgress database.
3) Create a `./omh-server.env` file with environment variables to configure the OMH on Fhir web service.
3) From the root directory of the project run `docker-compose up -d`

##API Details
Swagger is used to document the endpoints made available by the mdata-app. Use the following URLs to view details on the web service endpoints.
- *JSON* - <server_dns_name>/mdata/v2/api-docs
- *UI* - <server_dns_name>/mdata/swagger-ui.html#/

## Editing the Configurations
In order to transform OMH data into FHIR data, the OMH-on-FHIR service requires two things:
- **FhirTemplate**, which is the FHIR resource template that we can use to represent a specific OMH schema, and which we can transform into a FHIR object using OMH data. These have a one-to-many relationship with ResourceConfig.
- **ResourceConfig**, the configuration object that contains instructions to transform the template using OMH data, as well as the reference to which FhirTemplate to use.

For example, if we query the OMH-on-FHIR service for step count data in FHIR version STU3, it will refer to the ResourceConfig with id “stu3_step_count”, which contains a reference to the FhirTemplate “stu3_Observation”.

### Editing the ResourceConfig object
Here is a sample ResourceConfig (stu3\_step\_count) which maps the OMH resource step\_count to a FHIR Observation resource.
```
{
  "fhirTemplate": "stu3\_Observation",
  "shimKeys": [
      "fitbit"
  ],
  "versions" : {
      "fhir" : "stu3",
      "openmhealth" : "1.0"
  },
  "patches": {
      "omh": [
          {
              "op": "replace",
              "fhirPath": "/identifier/0/value",
              "omhPath": "/header/id",
              "required" : true
          },
          {
              "op": "replace",
              "fhirPath": "/subject/identifier/value",
              "omhPath": "/header/user\_id",
              "required" :  false
          },
          {
              "op": "replace",
              "fhirPath": "/effectiveDateTime",
              "omhPath": "/body/effective\_time\_frame/date\_time",
              "required" :  false
          },
		  ...
      ],
      "other": [
          {
              "op" : "remove",
              "path" : "/effectiveDateTime"
          },
          {
              "op": "replace",
              "path": "/category/0/coding/0/code",
              "value": "physical-activity"
          },
          {
              "op": "replace",
              "path": "/category/0/coding/0/display",
              "value": "Physical Activity"
          },
		  ...
      ]
  }
}
```
The required fields for this file are as follows:
- fhirTemplate
	* The ID of the FhirTemplate to transform into the desired resource. A list of objects and their corresponding FHIR element can be found [here](https://healthedata1.github.io/mFHIR/mapping.html#omh-datapoint-schema-mappings-to-fhir-resources) in the 3rd column. Most of these OMH schemas fall under the Observation profile. It follows that most of them will make use of the Observation template.
- shimKeys
	* The list of shims to tell the shimmer to make requests to. Some data is made available through multiple third parties, but this will depend on the type of OMH resource requested. A list of shim keys can be found on the [Shimmer Github page](https://github.com/openmhealth/shimmer).
- versions
	* The version of FHIR and Open mHealth that the ResourceConfig is configured to handle.
- patches
	* Contains the list of transformations to perform on the FHIR Template.
	* Each one contains an “op” type, which should be either “replace”, “remove”, or “add”.
		- These should primarily be specified as “replace” or “remove”. Using “add” implies an unusual edge case. For consistency, we should aim to add these fields to the FhirTemplate we are using, and perform a “remove” operation on them when they are not necessary for a particular OMH schema.
	* OMH patches are listed under “omh”, and are used to inject a data point found on an OMH resource to its corresponding field on the FHIR resource.
		- Some of these data points might be absent in the OMH object, but are not required. In this case, “required” should be set to false for that patch. If “required” is true and a data point is missing, the service will throw an error.
	* Miscellaneous patches are listed under “other”. This is used mostly to reshape the FhirTemplate or remove fields that have no mapping from the OMH schema and are not required in the FHIR resource as per the FHIR spec. These “other” patches will be processed before the OMH patches.

### Editing the FhirTemplate object
Below is the FhirTemplate for the Observation resource (dstu3\_Observation):
```
{
    "resourceType": "Observation",
    "meta": {
      "profile": [
        "http://www.fhir.org/mfhir/StructureDefinition/omh\_fhir\_profile\_quantitative\_observation"
      ]
    },
    "identifier": [
      {
        "system": "https://omh.org/shimmer/ids",
        "value" : ""
      }
    ],
    "status": "unknown",
    "category": [
      {
        "coding": [
          {
            "system": "http://hl7.org/fhir/observation-category",
            "code" : "",
            "display" : ""
          }
        ]
      }
    ],
    "code": {
      "coding": [
        {
          "system": "",
          "code": "",
          "display": ""
        }
      ]
    },
    "subject": {
      "identifier": {
        "system": "https://omh.org/shimmer/patient\_ids",
        "value" : ""
      }
    },
    "effectiveDateTime" : "",
    "effectivePeriod" : {
      "start" : "",
      "end" : ""
    },
    "issued" : "",
    "valueQuantity": {
      "code": "",
      "system": "",
      "unit": "",
      "value" : ""
    },
    "comment": "",
    "device": {
      "display": ""
    },
    "comment" : "",
    "device" : {
      "display" : ""
    }
}
```
This particular FhirTemplate (in both stu3 and r4 versions of FHIR) will be used as the base for most of the OMH resource transformations. The required fields for each type of FHIR resource are listed in the [FHIR documentation](http://hl7.org/fhir/STU3/resourcelist.html) under the link for that resource type. To reiterate a previous point, while many fields on many of these objects are not required (0..1 or 0..\*) a FhirTemplate should contain all the fields that are mappable from any OMH schema that depends on it, and the ResourceConfig for an individual schema should contain remove operations for the fields that don’t apply to that schema.

As an example, the ResourceConfig for the step\_count OMH schema (the example provided in 2.1) removes the field “/effectiveDateTime” in its miscellaneous patches, since the OMH response for step\_count captures data over a range of time, and should use “/effectivePeriod” instead. As a counterexample, the ResourceConfig for something like blood\_pressure, which is taken at a singular point in time, should inject that time into “/effectiveDateTime” and remove the “/effectivePeriod” field altogether.

Any field that is left blank after being mapped from an OMH schema should be slated for removal in that schema’s ResourceConfig in accordance with the FHIR spec, which does not allow blank fields.

### Conformance
Objects pertaining to a specific FHIR version will be prefaced with the name of that version and an underscore. This applies to both FhirTemplate and ResourceConfig.

FhirTemplate names:
	- {fhir-version}\_{fhir-resource-name}
	- examples:
		* r4\_Observation
		* stu3\_MedicationStatement

ResourceConfig:
	- {fhir-version}\_{omh-schema-name}
	- Use underscores, **not** hyphens.
	- examples:
		* dstu2\_blood\_pressure
		* stu3\_step\_count
		* r4\_body\_temperature

These names are the keys with which the objects will be stored in the database. When pushing changes to them in the project or adding new ones, they should be saved as “.json” files in the root OMH-on-FHIR project under the omh-mapping-config/src/main/resources directory.

Great care should be taken to ensure correctness and thorough testing, since these configurations have limited validation and issues will not make themselves known until the OMH-on-FHIR service makes use of them.
