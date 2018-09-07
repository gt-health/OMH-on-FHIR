'use strict';

angular.
module('login').
component('login', {

    templateUrl: 'components/login/login.template.html',
    controller: ['$scope', '$http', '$window', '$location', '$routeParams', 'OmhOnFhirApi', '__env', function LoginController($scope, $http, $window, $location, $routeParams, OmhOnFhirApi, env){
        var self = this;
        self.env = env;
        self.omhOnFhirApi = OmhOnFhirApi;
        self.loginSuccessful;

        self.pageMsg = 'TODO make login page';
        self.googleOauthUrl;

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

        //When the window gets focus check to see if authentication through OAuth was successful.
        //If so, forward to activity page, otherwise remain on login page.
        $window.onfocus = function (){
            console.log("Login window has focus");
            if( self.omhOnFhirApi.getLoginSuccessful() == true ){
                //forward to the activity page
                console.log("Authentication successful redirecting to " + self.env.baseUrl + "activity");
                $location.path(self.env.baseUrl + "activity");
            }
            else{
                console.log("Login not successful");
            }
        };

        //===================================================================================
        // Functions
        //===================================================================================
        self.loginWithFitbit = function loginWithFitbit() {
            console.log("Logging in with Fitbit");
            self.omhOnFhirApi.login(self.env.fitbitShim);
        };

        self.loginWithGoogleFit = function loginWithGoogleFit(){
            console.log("Logging in with Google fit");
            //self.googleOauthUrl =
            //    self.env.omhOnFhirAPIBase +
            //    self.env.omhOnFhirAPIShimmerAuth +
            //    "?ehrId=" + self.omhOnFhirApi.getPatientId() +
            //    "&shimkey=" + self.env.googleFitShim;
            //console.log("Opening Google OAuth URL: " + self.googleOauthUrl);
            self.omhOnFhirApi.login(self.env.googleFitShim);
        };

    }]
});