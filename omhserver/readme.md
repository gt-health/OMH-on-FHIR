#OMH-on-FHIR
Spring Boot Implementation of the OMH on FHIR application described here: https://healthedata1.github.io/mFHIR/#smart-app-workflow

It makes web service endpoints available for:
1) authentication via Shimmer
2) DocumentReference query to query data via Shimmer
3) Binary query to retrieve Shimmer data returned by a DocumentReference query
4) Observation query to retrieve Shimmer OMH data as FHIR Observations

## Project Structure
```
src                                                                 --> Contains project source code
    main                                                            --> Contains main project code
        java
            org
                gtri
                    hdap
                        mdata
                            conf                                    --> Contains project configuration
                                JacksonConfig.java                  --> Configures Jackson for JSON serialization/deserialization
                            controller
                                PatientDataController.java          --> Defines controller with endpoints for patient endpoints
                            jackson
                                BindingResultSerializer.java        --> Serializer for BeanPropertyBindingResult
                                HapiStu3Serializer.java             --> Serializer for HAPI FHIR STU3 Domain Objects
                            jpa                                     --> Contains objects to configure database interactions
                                entity                              --> Contains objects for data to persist to the database
                                    ApplicationUser.java            --> Defines data structure for an application users
                                    ApplicationUserId.java          --> Defines data structre user ids
                                    ShimmerData.java                --> Defines data structure for shimmer data
                                repository                          --> Exposes methods to search the database
                                    ApplicationUserRepository.java  --> Defines CRUD methdos for the application_user database table
                                    ShimmerDataRepository.java      --> Defines CRUD methods for the shimmer_data database table
                            service
                                ShimmerService.java                 --> Provides methods to interact with the Shimmer web service. It is used by the controllers.
                            util
                                ShimmerUtil.java                    --> Provides utility constants and methods.
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
                            service
                                ShimmerServiceTest                  --> Unit test for the ShimmerService
                            MdataServerApplicationTests             --> Unit test for the MdataServerApplication
        resources                                                   --> Contains testing resources
            test.application.properties                             --> Properties to use for testing
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