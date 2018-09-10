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
            $rootScope.loginSuccess = ($routeParams.loginSuccess == 'true');
        }
        if($routeParams.shimmerId){
            console.log("Setting shimmer id to: " + $routeParams.shimmerId);
            $rootScope.shimmerId = $routeParams.shimmerId;
        }

        //$window.opener.setShimmerId($routeParams.shimmerId);
        //$window.opener.setLoginSuccess(($routeParams.loginSuccess == 'true'));
        //
        $window.opener.shimmerId = $routeParams.shimmerId;
        $window.opener.loginSuccess = ($routeParams.loginSuccess == 'true');

        console.log("WINDOW OPENER");
        console.log($window.opener);

        console.log("WINDOW PARENT");
        console.log($window.parent);

        console.log("Window SCOPE");
        console.log($scope);

        console.log("WINDOW THIS");
        console.log(this);

    }]
});