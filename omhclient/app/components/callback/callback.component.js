'use strict';

angular.
module('callback').
component('callback', {

    templateUrl: 'components/callback/callback.template.html',
    controller: ['$scope', '$rootScope', '$http', '$routeParams', 'OmhOnFhirApi', '__env', function LoginController($scope, $rootScope, $http, $routeParams, OmhOnFhirApi, env){
        var self = this;
        self.omhOnFhirApi = OmhOnFhirApi;

        //===================================================================================
        // Initialization
        //===================================================================================
        if($routeParams.loginSuccess){
            console.log("Setting login success to: " + $routeParams.loginSuccess);
            $rootScope.loginSuccess = ($routeParams.loginSuccess == 'true');
        }
        if($routeParams.shimmerId){
            console.log("Setting shimmer id to: " + $routeParams.shimmerId);
            $rootScope.shimmerId = $routeParams.shimmerId;
        }

    }]
});