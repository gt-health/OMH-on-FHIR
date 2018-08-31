(function (window) {
    //following environment variable pattern described here: https://www.jvandemo.com/how-to-configure-your-angularjs-application-using-environment-variables/
    window.__env = window.__env || {};
    window.__env.baseUrl = '/omhonfhir/';
    window.__env.fitbitShim = 'fitbit';
    window.__env.googleFitShim = 'googlefit';
    window.__env.omhOnFhirClientId = 'a28985fe-625d-4cb4-8a9b-ee24dfd3a5fd';//'test_client';
    window.__env.omhOnFhirScope = 'patient/*.read launch'; //to force provider login use the following scopes 'openid profile'
    window.__env.omhOnFhirRedirectUri = 'https://apps.hdap.gatech.edu/omhonfhir/login';
    window.__env.omhOnFhirAPIBase = 'https://apps.hdap.gatech.edu/mdata';
    window.__env.omhOnFhirAPIShimmerAuth = '/shimmerAuthentication';
}(this));