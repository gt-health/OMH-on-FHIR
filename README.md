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