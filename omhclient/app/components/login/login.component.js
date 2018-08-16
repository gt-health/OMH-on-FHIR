'use strict';

angular.
module('login').
component('login', {

    templateUrl: 'components/login/login.template.html',
    controller: ['$http', '$routeParams', 'OmhOnFhirApi', '__env', function LoginController($http, $routeParams, OmhOnFhirApi, env){
        var self = this;
        self.env = env;
        self.omhOnFhirApi = OmhOnFhirApi;

        self.pageMsg = 'TODO make login page';

        console.log("Params passed to login");
        console.log($routeParams);

        //===================================================================================
        // Initialization
        //===================================================================================
        console.log("Checking for oauth ready state");
        FHIR.oauth2.ready(function(smart) {
            console.log("Looking at patient");
            console.log(smart);
            smart.patient.read().then(function (pt) {
                self.omhOnFhirApi.setPatientResourceObj(pt);
                self.pageMsg = "Login to desired application to retrieve step count data for " + self.omhOnFhirApi.getPatientName();
            });
        });

        //===================================================================================
        // Functions
        //===================================================================================
        self.loginWithFitbit = function loginWithFitbit() {
            console.log("Logging in with Fitbit");
            self.omhOnFhirApi.login(self.env.fitbitShim);
        };

        self.loginWithGoogleFit = function loginWithGoogleFit(){
            console.log("Logging in with Google fit");
            self.omhOnFhirApi.login(self.env.googleFitShim);
        };

    }]
});