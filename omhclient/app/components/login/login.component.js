'use strict';

angular.
module('login').
component('login', {

    templateUrl: 'components/login/login.template.html',
    controller: ['$scope', '$http', '$window', '$location', '$routeParams', 'OmhOnFhirApi', '__env', function LoginController($scope, $http, $window, $location, $routeParams, OmhOnFhirApi, env){
        var self = this;
        self.env = env;
        self.omhOnFhirApi = OmhOnFhirApi;
        self.shimmerId;
        self.loginSuccess;
        self.alertMsg = "";
        self.pageMsg = 'TODO make login page';
        self.googleOauthUrl;
        self.patientName;

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
                self.patientName = self.omhOnFhirApi.getPatientName();
                self.pageMsg = "Link patient " + self.patientName + " to an existing account.";
                $scope.patientName = self.omhOnFhirApi.getPatientName();
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
            console.log("Login in successful " + this.loginSuccess);
            console.log("Shimmer Id: " + this.shimmerId);
            this.patientName = $scope.patientName;
            console.log("Patient Name: " + this.patientName);
            if( this.loginSuccess == true ){
                //forward to the activity page
                console.log("Authentication successful redirecting to " + self.env.baseUrl + "activity?shimmerId=" + this.shimmerId + "&patientName=" + this.patientName);
                $window.location.href = self.env.baseUrl + "activity?shimmerId=" + this.shimmerId + "&patientName=" + this.patientName;
            }
            else{
                console.log("Login not successful");
                self.alertMsg = "Authentication not successful"
            }
        };

        //===================================================================================
        // Functions
        //===================================================================================
        self.setShimmerId = function setShimmerId(shimmerId){
            console.log("LOGIN SETTING SHIMMER ID " + shimmerId);
            self.shimmerId = shimmerId;
        };
        self.setLoginSuccess = function setLoginSuccess(loginSuccess){
            console.log("LOGIN SETTING SUCCESS " + loginSuccess);
            self.loginSuccess = loginSuccess;
        };
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