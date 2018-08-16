'use strict';

angular.
module('login').
component('login', {

    templateUrl: 'components/login/login.template.html',
    controller: ['$scope', '$http', '$routeParams', 'OmhOnFhirApi', '__env', function LoginController($scope, $http, $routeParams, OmhOnFhirApi, env){
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
                self.pageMsg = "Link patient " + self.omhOnFhirApi.getPatientName() + " to an existing account.";
                //update the scope so the variables are updated in the view
                $scope.$apply();
            });
        },
        function(error){
            console.log("Error");
            console.log(error);
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