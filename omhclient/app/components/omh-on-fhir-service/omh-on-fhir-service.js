angular.module('omhOnFhirService', [])
.factory('OmhOnFhirApi', [ '$http', '__env', function($http, env){
    var factory = {};

    console.log("Looking at environments");
    console.log(env);

    //===================================================================================
    // Variables in the factory
    //===================================================================================\
    factory.patientResourceObj;
    factory.patientId;
    factory.patientName;

    //===================================================================================
    // Getters and setters for the objects in the factory
    //===================================================================================
    factory.getPatientResourceObj = function getPatientResourceObj(){
        return this.patientResourceObj;
    };
    factory.setPatientResourceObj = function setPatientResourceObj (patientResourceObj){
        console.log("OMH on FHIR Service setting Patient");
        console.log(patientResourceObj);
        this.patientResourceObj = patientResourceObj;

        this.patientId = this.patientResourceObj.id;
        console.log("PatientId: " + this.getPatientId());

        this.patientName = this.getPatientNameFromObject(this.patientResourceObj);
        console.log("PatientName: " + this.patientName);
    };

    factory.getPatientId = function getPatientId(){
        return this.patientId;
    };
    factory.setPatientId = function setPatientId(patientId){
        this.patientId = patientId;
    };

    factory.getPatientName = function getPatientName(){
        return this.patientName;
    };
    factory.setPatientName = function setPatientName(patientName){
        this.patientName = patientName;
    };

    //===================================================================================
    // Additional functions for objects in the factory
    //===================================================================================

    factory.getPatientNameFromObject = function getPatientNameFromObject(pt){
        if (pt.name) {
            var names = pt.name.map(function(name) {
                return name.given.join(" ") + " " + name.family;
            });
            return names.join(" / ");
        } else {
            return "anonymous";
        }
    };

    factory.login = function login( shimKey ){
        console.log("Logging in to Shimmer");
        var shimmerAuthUrl =
            env.omhOnFhirAPIBase +
            env.omhOnFhirAPIShimmerAuth +
            "?ehrId=" + this.getPatientId() +
            "&shimkey=" + shimKey;
        console.log("Authorizing with Shimmer " + shimmerAuthUrl);
        window.location.href = shimmerAuthUrl;
    };

    factory.requestDocumentReference = function requestDocumentReference(shimmerId, startDate, endDate){
        var shimmerDocRefUrl = env.omhOnFhirAPIBase + "/DocumentReference?subject=" + shimmerId;

        if(startDate){
            shimmerDocRefUrl = shimmerDocRefUrl + "&date=" + startDate.toISOString().substring(0,10);//to make format 'yyyy-MM-dd'
        }
        if(endDate){
            shimmerDocRefUrl = shimmerDocRefUrl + "&date=" + endDate.toISOString().substring(0,10);//to make format 'yyyy-MM-dd'
        }
        console.log("Requesting Document Reference " + shimmerDocRefUrl);

        //returns a promise that contains headers, status, and data
        return $http.get(shimmerDocRefUrl);
    };

    factory.requestObservation = function requestObservation(shimmerId, startDate, endDate){
        return this.requestOmhResource("Observation", shimmerId, startDate, endDate);
    };

    factory.requestOmhResource = function requestOmhResource(resource, shimmerId, startDate, endDate){
        var shimmerDocRefUrl = env.omhOnFhirAPIBase + "/" + resource + "?subject=" + shimmerId;

        if(startDate){
            shimmerDocRefUrl = shimmerDocRefUrl + "&date=" + startDate.toISOString().substring(0,10);//to make format 'yyyy-MM-dd'
        }
        if(endDate){
            shimmerDocRefUrl = shimmerDocRefUrl + "&date=" + endDate.toISOString().substring(0,10);//to make format 'yyyy-MM-dd'
        }
        console.log("Requesting " + resource + " "  + shimmerDocRefUrl);

        //returns a promise that contains headers, status, and data
        return $http.get(shimmerDocRefUrl);
    };

    factory.requestBinaryAsJson = function requestBinaryAsJson(binaryUrl){
        var shimmerBinaryUrl = env.omhOnFhirAPIBase + "/" + binaryUrl;
        console.log("Requesting Binary " + shimmerBinaryUrl);
        var requestOptions =
            {
                "headers":  {
                    "Accept": "application/json"
                }
            };
        //returns a promise that contains headers, status, and data
        return $http.get(shimmerBinaryUrl, requestOptions);
    };

    factory.requestBinaryAsBase64 = function requestBinaryAsBase64(binaryUrl){
        var shimmerBinaryUrl = env.omhOnFhirAPIBase + "/" + binaryUrl;
        console.log("Requesting Binary " + shimmerBinaryUrl);
        //returns a promise that contains headers, status, and data
        return $http.get(shimmerBinaryUrl);
    };
    //Return the factory object
    return factory;
}]);