# OMH-on-FHIR

Implementation of the OMH-on-FHIR application described here: https://healthedata1.github.io/mFHIR/#smart-app-workflow

## Top Level Project Directories
```
omhclient           --> AngularJS project for the OMH on FHIR user interface
omhserver           --> Spring Boot project for the OMH on FHIR web service
docker-compose.yml  --> Docker Compose file to create the service stack for the application
Jenkinsfile         --> Jenkinsfile to tell Jenkins how to build the application
README.md           --> This file
```

## Project Containers
The application uses Docker Compose to create a service stack for each component of the application.
It creates the following containers:

| Container | Name in Docker Compose | Description |
| --------- |----------------------|-----------|
| Shimmer Resource Server | resource-server | The container running the Shimmer resource server which makes Shimmer API calls availble to other containers running in the Docker Compose service stack |
| Shimmer Database | mongo | The container running the Mongo database used by Shimmer |
| Shimmer Console | console | The container running the Shimmer console |
| OMH Server Database | mdata-db | The container running the Postgres database used by the OMH Web Service |
| OMH Web Service | mdata-app | The container running the webservice endpoints for Shimmer Authentication, DocumentReference, Binary, and Observation queries |
| OMH Client | omh-on-fhir-client | The container running the User Interface for the OMH on FHIR application |

## Environment Variables
The following environment variables need to be set for the containers

### resource-server

| Variable | Description |
| -------- | ----------- |
| OPENMHEALTH_SHIMMER_DATA_PROVIDER_REDIRECT_BASE_URL | Base URL for Shimmer to use for OAuth redirects |
| OPENMHEALTH_SHIM_FITBIT_CLIENT_ID | Client ID for Shimmer to use for FitBit Authentication |
| OPENMHEALTH_SHIM_FITBIT_CLIENT_SECRET |  Client Secret for Shimmer to use for FitBit authentication |
| OPENMHEALTH_SHIM_GOOGLEFIT_CLIENT_ID | Client ID for Shimmer to use for Google Fit authentication |
| OPENMHEALTH_SHIM_GOOGLEFIT_CLIENT_SECRET | Client secret for Shimmer to use for Google Fit authentication |
| OPENMHEALTH_SHIM_IHEALTH_CLIENT_ID | Client ID for Shimmer to use for iHealth Authentication |
| OPENMHEALTH_SHIM_IHEALTH_CLIENT_SECRET | Client Secret for Shimmer to use for iHealth authentication |
| OPENMHEALTH_SHIM_IHEALTH_SANBOXED | true or false value if sandboxed |
| OPENMHEALTH_SHIM_IHEALTH_CLIENT_SERIAL_NUMBER | iHealth serial number |
| OPENMHEALTH_SHIM_IHEALTH_ACTIVITY_ENDPOINT_SECRET | Secret for Shimmer to use for iHealth blood pressure endpoint |
| OPENMHEALTH_SHIM_IHEALTH_BLOOD_GLUCOSE_ENDPOINT_SECRET | Secret for Shimmer to use for iHealth glucose endpoint |
| OPENMHEALTH_SHIM_IHEALTH_BLOOD_PRESSURE_ENDPOINT_SECRET | Secret for Shimmer to use for iHealth blood pressure endpoint |
| OPENMHEALTH_SHIM_IHEALTH_SLEEP_ENDPOINT_SECRET | Secret for Shimmer to use for iHealth sleep endpoint |
| OPENMHEALTH_SHIM_IHEALTH_SP_O2_ENDPOINT_SECRET | Secret for Shimmer to use for iHealth O2 endpoint |
| OPENMHEALTH_SHIM_IHEALTH_SPORT_ENDPOINT_SECRET | Secret for Shimmer to use for iHealth sport endpoint |
| OPENMHEALTH_SHIM_IHEALTH_WEIGHT_ENDPOINT_SECRET | Secret for Shimmer to use for iHealth weight endpoint |

* See the Shimmer resource.env here for a list of environment variables that can be set.

For run time they can be set in the ./shimmer-resource-server.env file or explicitly in a Dockerfile or docker-compose.yml
### mongo
none

### console
none

### mdata-db

| Variable | Description |
| -------- | ----------- |
| POSTGRES_DB | The name of the database to create/use |
| POSTGRES_USER | The name of the database user |
| POSTGRES_PASSWORD | The password for the database user |

For run time they can be set in the ./omhserver/postgres.env file or explicitly in a Dockerfile or docker-compose.yml

### mdata-app

| Variable | Description |
| -------- | ----------- |
| SHIMMER_SERVER_URL | The URL to the Shimmer resource server |
| SHIMMER_REDIRECT_URL | The redirect URL to pass to the Shimmer API. It contains the URL to the mdata-app /authorize/fitbit/callback endpoint that handles successful user authentication. Shimmer only redirects to this URL after successful authentication.  |
| OMH_ON_FHIR_CALLBACK | The URL to the OMH on FHIR UI application to use after successful Shimmer authentication.  |
| OMH_ON_FHIR_LOGIN | The URL to the user interface that handles login to Fitbit and Google fit. |

For run time they can be set in the ./omhserver/omh-server.env file or explicitly in a Dockerfile or docker-compose.yml

### omh-on-fhir-client
none

## To Run
Do the following to run the application:
1) Create a `./shimmer-resource-server.env` file with environment variable to configure the Shimmer server, https://github.com/openmhealth/shimmer/blob/e3fef06d4d7d5f93d2a45e7656a823889f247499/resource-server.env, Place the file in the root directory of the project.
2) Create a `./omhserver/postgres.env` file with environment variables to configure the Postgress database.
3) Create a `./omhserver/omh-server.env` file with environment variables to configure the OMH on Fhir web service.
4) From the root directory of the project run `docker-compose up -d`

## User Interface Constraints

- DocumentReference search can only support two date parameters, one for the start date and one for the end date.
If the start date uses a prefix it must be `ge`. If the end date uses a prefix it must be `le`. The application only searches for documents between the specified date ranges.

## mdata-app web service API
Swagger is used to document the endpoints made available by the mdata-app. Use the following URLs to view details on the web service endpoints.
- *JSON* - <server_dns_name>/mdata/v2/api-docs
- *UI* - <server_dns_name>/mdata/swagger-ui.html#/

## Reference Deployment
A reference deployment of the application can be found here: https://launch.smarthealthit.org/?auth_error=&fhir_version_1=r2&fhir_version_2=r3&iss=&launch_ehr=1&launch_url=https%3A%2F%2Fapps.hdap.gatech.edu%2Fomhonfhir%2Flaunch&patient=&prov_skip_auth=1&provider=&pt_skip_auth=1&pt_skip_login=0&public_key=&sb=&sde=&sim_ehr=1&token_lifetime=15&user_pt=undefined
It uses the SMART Application Launcher to simulate an EHR launch.

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
