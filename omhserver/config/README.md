OMH-on-FHIR-Configuration
==
This project contains the configuration files needed to have OMH on FHIR map an OMH resource to FHIR resources.

Structure
--
- **src/main/resources/fhirTemplates** - This directory contains template files to use to generate FHIR resources
Files in this directory must use the following naming convention `<FHIR_VERSION>_<FHIR_RESOURCE>`. For example, stu3_Observation.

- **src/main/resources/resourceConfigs** - This directory contains configuration files that specify how to map an OMH resource to a FHIR resource defined by one of the templates in the fhirTemplates directory.
Files in this directory must use the following naming conventions `<FHIR_VERSION>_<OMH_RESOURCE>`. For example, stu3_step_count. You can see a list of OMH resources here: https://github.com/openmhealth/shimmer#supported-apis-and-endpoints. Use the names listed as `endpoint` in the table.

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
