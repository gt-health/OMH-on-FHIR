'use strict';

angular.
module('callback').
component('callback', {

    templateUrl: 'components/callback/callback.template.html',
    controller: ['$scope', '$rootScope', '$http', '$window', '$routeParams', 'OmhOnFhirApi', '__env', function LoginController($scope, $rootScope, $http, $window, $routeParams, OmhOnFhirApi, env){
        var self = this;
        self.omhOnFhirApi = OmhOnFhirApi;

        //===================================================================================
        // Initialization
        //===================================================================================
        if($routeParams.loginSuccess){
            console.log("Setting login success to: " + $routeParams.loginSuccess);
            //set the variable in the scope for this window
            $rootScope.loginSuccess = ($routeParams.loginSuccess == 'true');
            //set the variable in the scope of the parent window
            $window.opener.self.loginSuccess = ($routeParams.loginSuccess == 'true');
        }
        if($routeParams.shimmerId){
            console.log("Setting shimmer id to: " + $routeParams.shimmerId);
            //set the variable in the scope for this window
            $rootScope.shimmerId = $routeParams.shimmerId;
            //set the variable in the scope of the parent window
            $window.opener.self.shimmerId = $routeParams.shimmerId;
        }
    }]
});