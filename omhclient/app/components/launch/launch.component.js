'use strict';

angular.
module('launch').
component('launch', {

    templateUrl: 'components/launch/launch.template.html',
    controller: ['$http', '$routeParams','__env', function LaunchController($http, $routeParams, env){
        var self = this;

        //controller initialization
        console.log("Launching application and authorizing");
        console.log($routeParams);
        console.log($routeParams.launch);
        console.log($routeParams.iss);

        FHIR.oauth2.authorize({
            "client":{
                "client_id": env.omhOnFhirClientId,
                "scope":  env.omhOnFhirScope,
                "redirect_url": env.omhOnFhirRedirectUri,
                "redirect_uri": env.omhOnFhirRedirectUri,
                "launch": $routeParams.launch
            },
            "server": $routeParams.iss
        });
    }]
});