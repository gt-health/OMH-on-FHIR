angular.module('projectConstants',[])
.constant('Constants', (function(){
    return {
        fitbitShim: 'fitbit',
        googleFitShim: 'googlefit',
        omhOnFhirClientId: 'test_client',
        omhOnFhirScope: 'patient/*.read launch', //to force provider login use the following scopes 'openid profile'
        omhOnFhirRedirectUri: 'http://localhost:8000/#!/login',
        omhOnFhirAPIBase: 'https://apps.hdap.gatech.edu/mdata',
        omhOnFhirAPIShimmerAuth: '/shimmerAuthentication'
    }
})());