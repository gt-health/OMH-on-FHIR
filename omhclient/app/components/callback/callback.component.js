'use strict';

angular.
module('callback').
component('callback', {

    templateUrl: 'components/callback/callback.template.html',
    controller: ['$scope', '$http', '$routeParams', 'OmhOnFhirApi', '__env', function LoginController($scope, $http, $routeParams, OmhOnFhirApi, env){
        var self = this;
        self.omhOnFhirApi = OmhOnFhirApi;

        //===================================================================================
        // Initialization
        //===================================================================================
        if($routeParams.loginSuccess){
            self.omhOnFhirApi.setLoginSuccessful(($routeParams.loginSuccess == 'true'));
        }
        if($routeParams.shimmerId){
            self.omhOnFhirApi.setShimmerId($routeParams.shimmerId);
        }

    }]
});