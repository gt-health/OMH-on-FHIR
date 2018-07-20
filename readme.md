OMH-on-FHIR
=
Implementation of the OMH-on-FHIR application described here: https://healthedata1.github.io/mFHIR/#smart-app-workflow

The application user Docker Compose to create a service stack for each component of the application.

Constraints
-
1) DocumentReference search can only support two date parameters, one for the start date and one for the end date.
If the start date uses a prefix it must be `ge`. If the end date uses a prefix it must be `le`. The application only searches
between for documents between the specified date ranges.

To Run
-
1) Create a `shimmer-resource-server.env` file with environment variable to configure the Shimmer server, https://github.com/openmhealth/shimmer/blob/e3fef06d4d7d5f93d2a45e7656a823889f247499/resource-server.env, Place the file in the root directory of the project.
2) Create a `postgres.env` file with environment variables to configure the Postgress database.
3) From the root directory of the project run `docker-compose up -d`