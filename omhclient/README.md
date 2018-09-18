# `OMH on FHIR Client` — User Interface for the OMH on FHIR Application

This project contains the User Interface for the OMH on FHIR application (https://healthedata1.github.io/mFHIR/). It provides functionality to demonstrate use cases 1 and 2 of the OMH on FHIR specification.

## Project Structure

```
app                                 --> The AngularJS application
    bower_components                --> The bower installed components
    components                      --> The application components
        activity                    --> The activity components
            activity-component.js   --> Defines the activity component. It sets the module, template, and controller.
            activity.module.js      --> Defines the activity module.
            activity.spec.js        --> Unit tests
            activity.template.html  --> The HTML template
        launch                      --> The launch components
            launch.component.js     --> Defines the launch component. It sets the module, template, and controller.
            launch.module.js        --> Defines the launch module.
            launch.template.html    --> The HTML template
        login
            login.component.js      --> Defines the login component. It sets the module, template, and controller.
            login.module.js         --> Defines the login module.
            login.template.html     --> The HTML template.
        omh-on-fhir-service
            omh-on-fhir-service.js  --> Defines the service that communicates with the OMH on FHIR web service.
    img                             --> Contains images used by the application.
    js                              --> Contains Javascript used by the application.
    app.css                         --> The stylesheet for the application.
    app.js                          --> Configures the application modules and router
    index.html                      --> The index page for the application
e2e-tests                           --> Contains end to end tests
node_modules                        --> Contains the node modules
Dockerfile                          --> Defines the Docker Image for the User Interface
karma.conf.js                       --> Contains the karma testing configuration.
LICENSE                             --> The license for the UI
nginx.conf                          --> The configuration for NGINX used by the Docker Image
package.json                        --> Defines dependencies used by the application.
```

### Prerequisites

- An OMH on FHIR web service must be running
- `./app/js/env.js` must be updated as needed for deployment. Specifically, it must be updated if the application is deployed to a context other than `omhonfhir` no the web application server.

### Configuration Details
`./app/js/env.js` contains environment variables used by the UI. It uses the following format:
```
(function (window) {
    //following environment variable pattern described here: https://www.jvandemo.com/how-to-configure-your-angularjs-application-using-environment-variables/
    window.__env = window.__env || {};
    window.__env.baseUrl = '/omhonfhir/'; //The deployment context of the application on the web server
    window.__env.fitbitShim = 'fitbit'; // the value of the Shimmer FitBit Shim
    window.__env.googleFitShim = 'googlefit'; // the value of the Shimmer Google Fit Shim
    window.__env.omhOnFhirClientId = 'SOME_CLIENT_ID'; //The SMART on FHIR client ID for the application
    window.__env.omhOnFhirScope = 'patient/*.read launch'; //SMART on FHIR scopes to use. To force provider login use the following scopes 'openid profile'
    window.__env.omhOnFhirRedirectUri = 'https://apps.hdap.gatech.edu/omhonfhir/login'; //Redirect URL for SMART on FHIR to use after successful authentication
    window.__env.omhOnFhirAPIBase = 'https://apps.hdap.gatech.edu/mdata'; //Base URL for the OMH on FHIR web service
    window.__env.omhOnFhirAPIShimmerAuth = '/shimmerAuthentication'; //URL to apped to omhOnFhirAPIBase environment variable to create the URL to the OMH on FHIR Shimmer Authentication endpoint.
}(this));
```