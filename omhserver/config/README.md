OMH-on-FHIR-Configuration
==
This project contains the configuration files needed to have OMH on FHIR map an OMH resource to FHIR resources.

Structure
--
- **src/main/resources/fhirTemplates** - This directory contains template files to use to generate FHIR resources
Files in this directory must use the following naming convention `<FHIR_VERSION>_<FHIR_RESOURCE>`. For example, stu3_Observation.

- **src/main/resources/resourceConfigs** - This directory contains configuration files that specify how to map an OMH resource to a FHIR resource defined by one of the templates in the fhirTemplates directory.
Files in this directory must use the following naming conventions `<FHIR_VERSION>_<OMH_RESOURCE>`. For example, stu3_step_count. You can see a list of OMH resources here: https://github.com/openmhealth/shimmer#supported-apis-and-endpoints. Use the names listed as `endpoint` in the table.
