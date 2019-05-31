'use strict';

angular.
module('activity').
component('activity', {

    templateUrl: 'components/activity/activity.template.html',
    controller: ['$scope', '$http', '$window', '$routeParams', 'OmhOnFhirApi', function ActivityController($scope, $http, $window, $routeParams, OmhOnFhirApi){
        var self = this;
        self.omhOnFhirApi = OmhOnFhirApi;

        console.log("Params passed to login");
        console.log($routeParams);
        self.waitingForSearch = false;
        self.waitingForHeartRateSearch = false;
        self.waitingForGraph = false;
        self.startDate;
        self.endDate;
        self.shimmerId;
        self.activityDocumentRef;
        self.activityResourceType= "application/json";
        self.activityDataType = "OMH JSON";
        self.activityBinaryUrl;
        self.omhActivity;

        self.heartRateDocumentRef;
        self.heartRateResourceType= "application/json";
        self.heartRateDataType = "OMH JSON";
        self.heartRateBinaryUrl;
        self.omhHeartRate;

        self.prunedObservationResponse = "";
        self.observationResponse = "";
        self.prunedHeartRateObservationResponse = "";
        self.heartRateObservationResponse = "";

        self.patientName;
        self.observationVisible = false;
        self.heartRateObservationVisible = false;
        self.docReferenceVisible = false;
        self.heartRateReferenceVisible = false;
        self.startDateGap = 60;
        self.showStepDataTable = false;
        self.stepCountVisible = true;
        self.toolsVisible = false;

        //===================================================================================
        // Variables for chart
        //===================================================================================
        self.chart = null;
        self.clickInteraction = null;
        self.clickInteractionComponent = null;
        self.options = {
            'userInterface':{
                'axes': {
                    'yAxis':{
                        'visible': true
                    },
                    'xAxis':{
                        'visible': true
                    }
                }
            },
            'measures': {
                'distance': {
                    'valueKeyPath': 'body.distance.value',
                    'range': { 'min':0, 'max':10000 },
                    'units': 'm',
                    'timeQuantizationLevel': OMHWebVisualizations.QUANTIZE_MONTH,
                    'seriesName': 'Distance',
                    'chart': {
                        'type' : 'clustered_bar',
                        'daysShownOnTimeline': { 'min': 90, 'max': 365 }
                    }
                },
                'systolic_blood_pressure': {
                    'thresholds': { 'max': 120 },
                    'range': undefined,
                    'chart': {
                        'daysShownOnTimeline': { 'min': 0, 'max': Infinity }
                    }
                },
                'diastolic_blood_pressure': {
                    'thresholds': undefined
                },
                'step_count': {
                    'range': undefined,
                    'timeQuantizationLevel': OMHWebVisualizations.QUANTIZE_DAY,
                    'chart': {
                        'type' : 'line',
                        'daysShownOnTimeline': undefined
                    }
                },
                'heart_rate': {
                    'range': undefined,
                    'timeQuantizationLevel': OMHWebVisualizations.QUANTIZE_DAY,
                    'chart': {
                        'type' : 'line',
                        'daysShownOnTimeline': undefined
                    }
                }
            }
        };

        //===================================================================================
        // Initialization
        //===================================================================================
        if($routeParams.shimmerId){
            self.shimmerId = $routeParams.shimmerId;
        }
        if($routeParams.patientName){
            self.patientName = $routeParams.patientName;
        }
        var date = new Date();
        self.startDate = new Date();
        self.startDate.setDate(date.getDate() - self.startDateGap);
        self.endDate = date;
        console.log("Start Date " + self.startDate);
        console.log("End Date " + self.endDate);

        //===================================================================================
        // Handlers
        //===================================================================================
        $scope.$on('requestBinary', function(){ self.queryBinary(); });
        $scope.$on('requestHeartRateBinary', function(){ self.queryHeartRateBinary(); });

        //initialize graph in UI
        self.$onInit = function(){
            self.retrieveStepCountAndHeartRate();
        };

        self.$onChanges = function(changesObj){
            console.log("Something changed");
            console.log(changesObj);
        };

        //===================================================================================
        // Functions
        //===================================================================================

        self.retrieveStepCountAndHeartRate = function retrieveStepCountAndHeartRate(){
            console.log("Retrieve step count");
            self.requestDocumentReference(true);
            //go ahead and get the data as an observation as well
            self.queryObservations();
        };
        self.queryActivity = function queryActivity(){
            console.log("Querying Action");
            self.requestDocumentReference(false);
        };

        self.requestDocumentReference = function requestDocumentReference(requestBinary){
            console.log("Querying patient " + self.shimmerId+ "activity from " + self.startDate + " to " + self.endDate);
            self.waitingForSearch = true;
            self.waitingForHeartRateSearch = true;
            self.activityDocumentRef = null;
            self.heartRateDocumentRef = null;
            self.omhActivity = null;
            self.omhHeartRate = null;
            //request step count data
            self.omhOnFhirApi.requestStepCountDocumentReference(self.shimmerId, self.startDate, self.endDate)
                .then(function(response){
                    console.log("Activity Response");
                    console.log(response);

                    //sample response data
                    //{
                    // "resourceType":"DocumentReference",
                    // "status":"current",
                    // "type":{
                    //    "text":"OMH fitbit data"
                    // },
                    // "indexed":"2018-07-31T22:02:11.408+00:00",
                    // "content":[
                    //     {
                    //       "attachment":{
                    //        "contentType":"application/json",
                    //        "url":"Binary/1d1ddd60-0c42-4ed2-b0e3-8b43876ceb9b",
                    //        "title":"OMH fitbit data",
                    //        "creation":"2018-07-31T22:02:11+00:00"
                    //       }
                    //     }
                    // ]
                    //}
                    console.log("Processing response");
                    //at the moment we are returning a single entry in the response
                    var currDocRef = response.data;
                    self.activityDocumentRef = currDocRef;

                    //make title
                    self.activityResourceType = currDocRef.resourceType;
                    //make type
                    self.activityDataType = currDocRef.type;
                    //make url
                    self.activityBinaryUrl = currDocRef.entry[0].resource.content[0].attachment.url;
                    self.waitingForSearch = false;
                    self.docReferenceVisible = true;
                    if(requestBinary){
                        console.log("Requesting Binary");
                        $scope.$emit('requestBinary');
                    }
                });

            //request heart rate data
            self.omhOnFhirApi.requestHeartRateDocumentReference(self.shimmerId, self.startDate, self.endDate)
                .then(function(response){
                    console.log("Heart Rate Response");
                    console.log(response);

                    //sample response data
                    //{
                    // "resourceType":"DocumentReference",
                    // "status":"current",
                    // "type":{
                    //    "text":"OMH fitbit data"
                    // },
                    // "indexed":"2018-07-31T22:02:11.408+00:00",
                    // "content":[
                    //     {
                    //       "attachment":{
                    //        "contentType":"application/json",
                    //        "url":"Binary/1d1ddd60-0c42-4ed2-b0e3-8b43876ceb9b",
                    //        "title":"OMH fitbit data",
                    //        "creation":"2018-07-31T22:02:11+00:00"
                    //       }
                    //     }
                    // ]
                    //}
                    console.log("Processing response");
                    //at the moment we are returning a single entry in the response
                    var currDocRef = response.data;
                    self.heartRateDocumentRef = currDocRef;

                    //make title
                    self.heartRateResourceType = currDocRef.resourceType;
                    //make type
                    self.heartRateDataType = currDocRef.type;
                    //make url
                    self.heartRateBinaryUrl = currDocRef.entry[0].resource.content[0].attachment.url;
                    self.waitingForHeartRateSearch = false;
                    self.heartRateReferenceVisible = true;
                    if(requestBinary){
                        console.log("Requesting HeartRate Binary");
                        $scope.$emit('requestHeartRateBinary');
                    }
                });
        };

        self.queryBinary = function queryBinary(){
            console.log("Querying binary " + self.activityBinaryUrl);
            self.omhOnFhirApi.requestBinaryAsJson(self.activityBinaryUrl)
                .then(function(response){
                    //sample response
                    //{
                    //    "shim": "googlefit",
                    //    "timeStamp": 1534251049,
                    //    "body": [
                    //    {
                    //        "header": {
                    //            "id": "3b9b68a2-e0fd-4bdd-ba85-4127a4e8bcee",
                    //            "creation_date_time": "2018-08-14T12:50:49.383Z",
                    //            "acquisition_provenance": {
                    //                "source_name": "some source",
                    //                "source_origin_id": "somedevice"
                    //            },
                    //            "schema_id": {
                    //                "namespace": "omh",
                    //                "name": "step-count",
                    //                "version": "2.0"
                    //            }
                    //        },
                    //        "body": {
                    //            "effective_time_frame": {
                    //                "time_interval": {
                    //                    "start_date_time": "2018-08-14T00:00:17.805Z",
                    //                    "end_date_time": "2018-08-14T00:01:17.805Z"
                    //                }
                    //            },
                    //            "step_count": 7
                    //        }
                    //    },
                    // ...
                    //    ]
                    //}
                    console.log("Processing Binary Response");
                    console.log(response);
                    self.omhActivity = response.data; //to convert OmhActivity to JSON string use JSON.stringify(omhActivity)
                    self.makeChart(self.omhActivity.body, d3.select('.chart-container'), "step_count", self.options);
                    console.log("processed response");
                });
        };

        self.queryHeartRateBinary = function queryHeartRateBinary(){
            console.log("Querying Heart Rate binary " + self.heartRateBinaryUrl);
            //self.omhOnFhirApi.requestBinaryAsJson(self.heartRateBinaryUrl)
            //    .then(function(response){
            //        //sample response
            //        //{
            //        //    "shim": "googlefit",
            //        //    "timeStamp": 1534251049,
            //        //    "body": [
            //        //    {
            //        //        "header" : {
            //        //            "id" : "3b9b68a2-e0fd-4bdd-ba85-4127a4e8gggg",
            //        //            "creation_date_time" : "2018-08-14T12:50:49.383Z",
            //        //            "acquisition_provenance" : {
            //        //                "source_name" : "some source",
            //        //                "source_origin_id" : "some device"
            //        //            },
            //        //            "schema_id" : {
            //        //                "namespace" : "omh",
            //        //                "name" : "heart-rate",
            //        //                "version" : "2.0"
            //        //            }
            //        //        },
            //        //        "body" : {
            //        //            "heart_rate": {
            //        //                "value": 50,
            //        //                "unit": "beats/min"
            //        //            },
            //        //            "effective_time_frame": {
            //        //                "date_time": "2013-02-05T07:25:00Z"
            //        //            },
            //        //            "temporal_relationship_to_physical_activity": "at rest",
            //        //            "user_notes": "I felt quite dizzy"
            //        //        }
            //        //    },
            //        // ...
            //        //    ]
            //        //}
            //        console.log("Processing Heart Rate Binary Response");
            //        console.log(response);
            //        self.omhHeartRate = response.data; //to convert omhHeartRate to JSON string use JSON.stringify(omhHeartRate)
            //        self.makeChart(self.omhHeartRate.body, d3.select('.chart-container'), "heart_rate", self.options);
            //        console.log("processed response");
            //    });

            //sample for testing
            console.log("Processing Heart Rate Binary Response");
            self.omhHeartRate =
                "{" +
                "\"shim\" : \"googlefit\"," +
                "\"timeStamp\" : 1534251049," +
                "\"body\" : [" +
                "{" +
                "\"header\" : {" +
                "\"id\" : \"3b9b68a2-e0fd-4bdd-ba85-4127a4e8gggg\"," +
                "\"creation_date_time\" : \"2019-05-14T12:50:49.383Z\"," +
                "\"acquisition_provenance\" : {" +
                "\"source_name\" : \"some source\"," +
                "\"source_origin_id\" : \"some device\"" +
                "}," +
                "\"schema_id\" : {" +
                "\"namespace\" : \"omh\"," +
                "\"name\" : \"heart-rate\"," +
                "\"version\" : \"2.0\"" +
                "}" +
                "," +
                "\"body\" : {" +
                "\"heart_rate\": {" +
                "\"value\": 50," +
                "\"unit\": \"beats/min\"" +
                "}," +
                "\"effective_time_frame\": {" +
                "\"date_time\": \"2019-05-05T07:25:00Z\"" +
                "}," +
                "\"temporal_relationship_to_physical_activity\": \"at rest\"," +
                "\"user_notes\": \"I felt quite dizzy\"" +
                "}" +
                "}," +
                "{" +
                "\"header\" : {" +
                "\"id\" : \"3b9b68a2-e0fd-4bdd-ba85-4127a4e8gggg\"," +
                "\"creation_date_time\" : \"2019-05-14T12:50:49.383Z\"," +
                "\"acquisition_provenance\" : {" +
                "\"source_name\" : \"some source\"," +
                "\"source_origin_id\" : \"some device\"" +
                "}," +
                "\"schema_id\" : {" +
                "\"namespace\" : \"omh\"," +
                "\"name\" : \"heart-rate\"," +
                "\"version\" : \"2.0\"" +
                "}" +
                "," +
                "\"body\" : {" +
                "\"heart_rate\": {" +
                "\"value\": 40," +
                "\"unit\": \"beats/min\"" +
                "}," +
                "\"effective_time_frame\": {" +
                "\"date_time\": \"2019-05-06T08:25:00Z\"" +
                "}," +
                "\"temporal_relationship_to_physical_activity\": \"at rest\"," +
                "\"user_notes\": \"I felt quite dizzy\"" +
                "}" +
                "}" +
                "]" +
                "}";
            self.makeChart(self.omhHeartRate.body, d3.select('.chart-container'), "heart_rate", self.options);
            console.log("processed response");
        };

        self.queryObservations = function queryObservations(){
            self.queryStepCountObservation();
            self.queryHeartRateObservation();
        };

        self.queryStepCountObservation = function queryStepCountObservation(){
            console.log("Querying Observation " + self.shimmerId+ "activity from " + self.startDate + " to " + self.endDate);
            self.omhOnFhirApi.requestStepCount(self.shimmerId, self.startDate, self.endDate)
                .then(function(response){
                    console.log("Observation Response");
                    console.log(response);

                    //sample response data
                    //{
                    //    "resourceType": "Bundle",
                    //    "id": "55f690f4-d5e5-4f26-8fa2-026be61019",
                    //    "meta": {
                    //    "lastUpdated": "2018-05-23T23:48:12Z"
                    //},
                    //    "type": "searchset",
                    //    "total": 1,
                    //    "link": [
                    //    {
                    //        "relation": "self",
                    //        "url": "http://test.fhir.org/r3/Observation?_format=application/fhir+json&search-id=7db95351-c995-4cbc-b990-1760a91987&&patient.identifier=some%2Duser&_sort=_id"
                    //    }
                    //],
                    //    "entry": [
                    //    {
                    //        "fullUrl": "http://test.fhir.org/r3/Observation/stepcount-example",
                    //        "resource": {
                    //            "resourceType": "Observation",
                    //            "id": "stepcount-example",
                    //            "meta": {
                    //                "versionId": "5",
                    //                "lastUpdated": "2018-05-23T21:56:09Z"
                    //            },
                    //            "contained": [
                    //                {
                    //                    "resourceType": "Patient",
                    //                    "id": "p",
                    //                    "identifier": [
                    //                        {
                    //                            "system": "https://omh.org/shimmer/patient_ids",
                    //                            "value": "some-user"
                    //                        }
                    //                    ]
                    //                }
                    //            ],
                    //            "identifier": [
                    //                {
                    //                    "system": "https://omh.org/shimmer/ids",
                    //                    "value": "12341567"
                    //                }
                    //            ],
                    //            "status": "unknown",
                    //            "category": [
                    //                {
                    //                    "coding": [
                    //                        {
                    //                            "system": "http://snomed.info/sct",
                    //                            "code": "68130003",
                    //                            "display": "Physical activity (observable entity)"
                    //                        }
                    //                    ]
                    //                }
                    //            ],
                    //            "code": {
                    //                "coding": [
                    //                    {
                    //                        "system": "http://loinc.org",
                    //                        "code": "55423-8",
                    //                        "display": "Number of steps in unspecified time Pedometer"
                    //                    }
                    //                ],
                    //                "text": "Step count"
                    //            },
                    //            "subject": {
                    //                "reference": "#p"
                    //            },
                    //            "effectivePeriod": {
                    //                "start": "2018-04-17T00:00:00Z",
                    //                "end": "2018-04-24T00:00:00Z"
                    //            },
                    //            "issued": "2018-04-24T17:13:50Z",
                    //            "device": {
                    //                "display": "Jawbone UP API, modality =sensed, sourceCreationDateTime = 2018-04-17T17:13:50Z"
                    //            },
                    //            "component": [
                    //                {
                    //                    "code": {
                    //                        "coding": [
                    //                            {
                    //                                "system": "http://hl7.org/fhir/observation-statistics",
                    //                                "code": "maximum",
                    //                                "display": "Maximum"
                    //                            }
                    //                        ],
                    //                        "text": "Maximum"
                    //                    },
                    //                    "valueQuantity": {
                    //                        "value": 7939,
                    //                        "unit": "steps/day",
                    //                        "system": "http://unitsofmeasure.org",
                    //                        "code": "{steps}/d"
                    //                    }
                    //                }
                    //            ]
                    //        },
                    //        "search": {
                    //            "mode": "match"
                    //        }
                    //    }
                    //]
                    //}
                    console.log("Processing response");
                    //at the moment we are returning a single entry in the response
                    self.observationResponse = response.data;
                    self.prunedObservationResponse = JSON.stringify(self.observationResponse, null, 2).substring(0,1000);
                    self.observationVisible = true;
                });
        };

        self.queryHeartRateObservation = function queryHeartRateObservation(){
            console.log("Querying Observation " + self.shimmerId+ "activity from " + self.startDate + " to " + self.endDate);
            self.omhOnFhirApi.requestHeartRate(self.shimmerId, self.startDate, self.endDate)
                .then(function(response){
                    console.log("Observation Response");
                    console.log(response);

                    //sample response data
                    //{
                    //    "resourceType": "Bundle",
                    //    "id": "55f690f4-d5e5-4f26-8fa2-026be61019",
                    //    "meta": {
                    //    "lastUpdated": "2018-05-23T23:48:12Z"
                    //},
                    //    "type": "searchset",
                    //    "total": 1,
                    //    "link": [
                    //    {
                    //        "relation": "self",
                    //        "url": "http://test.fhir.org/r3/Observation?_format=application/fhir+json&search-id=7db95351-c995-4cbc-b990-1760a91987&&patient.identifier=some%2Duser&_sort=_id"
                    //    }
                    //],
                    //    "entry": [
                    //    {
                    //        "fullUrl": "http://test.fhir.org/r3/Observation/stepcount-example",
                    //        "resource": {
                    //            "resourceType":"Observation",
                    //            "id":"omh-heart-rate-example",
                    //            "meta":{
                    //                "source":"generator",
                    //                "profile":[
                    //                    "http://www.fhir.org/guides/mfhir/StructureDefinition/heart-rate"
                    //                ]
                    //            },
                    //            "identifier":[
                    //                {
                    //                    "system":"https://omh.org/shimmer/ids",
                    //                    "value":"87ca4312-fbe3-4b24-bab4-17d47ba54e2a"
                    //                }
                    //            ],
                    //            "status":"unknown",
                    //            "category":[
                    //                {
                    //                    "coding":[
                    //                        {
                    //                            "system":"http://hl7.org/fhir/observation-category",
                    //                            "code":"vital-signs",
                    //                            "display":"Vital Signs"
                    //                        }
                    //                    ]
                    //                }
                    //            ],
                    //            "code":{
                    //                "coding":[
                    //                    {
                    //                        "system":"http://loinc.org",
                    //                        "code":"8867-4",
                    //                        "display":"Heart rate"
                    //                    }
                    //                ],
                    //                "text":"Heart Rate"
                    //            },
                    //            "subject":{
                    //                "identifier":{
                    //                    "system":"https://omh.org/shimmer/patient_ids",
                    //                    "value":"some-user"
                    //                },
                    //                "effectiveDateTime":"2014-01-03T09:13:41Z",
                    //                "issued":"2014-01-03T09:14:41Z",
                    //                "valueQuantity":{
                    //                    "value":79.88711511574905,
                    //                    "unit":"beats/min",
                    //                    "system":"http://unitsofmeasure.org",
                    //                    "code":"/min"
                    //                }
                    //            }
                    //        },
                    //        "search": {
                    //            "mode": "match"
                    //        }
                    //    }
                    //]
                    //}
                    console.log("Processing response");
                    //at the moment we are returning a single entry in the response
                    self.heartRateObservationResponse = response.data;
                    self.prunedHeartRateObservationResponse = JSON.stringify(self.heartRateObservationResponse, null, 2).substring(0,1000);
                    self.heartRateObservationVisible = true;
                });
        };

        self.saveJsonObservation = function saveJsonObservation(){
            var fileName = "observation-step-count.json";
            self.saveJsonAsFile(fileName, angular.toJson(self.observationResponse, true));
        };

        /*
         * Saves the passed in text to a file and saves it to the client machine.
         * @param fileNameToSaveAs - the name to use for the file.
         * @param textToWrite - the text to write to the file.
         */
        self.saveJsonAsFile = function (fileNameToSaveAs, textToWrite) {
            /* Saves a text string as a blob file*/
            var ie = navigator.userAgent.match(/MSIE\s([\d.]+)/),
                ie11 = navigator.userAgent.match(/Trident\/7.0/) && navigator.userAgent.match(/rv:11/),
                ieEDGE = navigator.userAgent.match(/Edge/g),
                ieVer=(ie ? ie[1] : (ie11 ? 11 : (ieEDGE ? 12 : -1)));

            if (ie && ieVer<10) {
                console.log("No blobs on IE ver<10");
                return;
            }

            var textFileAsBlob = new Blob([textToWrite], {
                type: 'application/json;charset=utf-8'
            });

            if (ieVer>-1) {
                window.navigator.msSaveBlob(textFileAsBlob, fileNameToSaveAs);

            } else {
                var downloadLink = document.createElement("a");
                downloadLink.download = fileNameToSaveAs;
                downloadLink.href = window.URL.createObjectURL(textFileAsBlob);
                downloadLink.onclick = function(e) { document.body.removeChild(e.target); };
                downloadLink.style.display = "none";
                document.body.appendChild(downloadLink);
                downloadLink.click();
            }
        };

        self.toggleObservationVisibility = function toggleObservationVisibility(){
            self.observationVisible = !self.observationVisible;
            console.log("Toggled Observation Visibility to: " + self.observationVisible);
        };

        self.toggleDocReferenceVisibility = function toggleDocReferenceVisibility(){
            self.docReferenceVisible = !self.docReferenceVisible;
            console.log("Toggled DocReference Visibility to: " + self.docReferenceVisible);
        };

        self.toggleHeartRateReferenceVisibility = function toggleHeartRateReferenceVisibility(){
            self.heartRateReferenceVisible = !self.heartRateReferenceVisible;
            console.log("Toggled HeartRateReference Visibility to: " + self.heartRateReferenceVisible);
        };

        self.toggleStepCountVisibility = function toggleStepCountVisibility(){
            self.stepCountVisible = !self.stepCountVisible;
        };
        self.toggleToolsVisibility = function toggleToolsVisibility(){
            self.toolsVisible = !self.toolsVisible;
        };
        self.toggleShowStepDataTable = function toggleShowStepDataTable(){
            self.showStepDataTable = !self.showStepDataTable;
            console.log("Toggled showStepDataTable to: " + self.showStepDataTable);
        };
        //===================================================================================
        // D3 Config
        //===================================================================================
        self.hideChart = function hideChart(){
            d3.select('.chart').classed('hidden', true);
        };

        self.showChart = function showChart(){
            d3.select('.chart').classed('hidden', false);
        };

        self.customizeChartComponents = function customizeChartComponents( components ){
            console.log("Setting Chart components");
            //move any label overlayed on the bottom right
            //of the chart up to the top left
            var plots = components.plots;

            plots.forEach(function( component ){

                if ( component instanceof Plottable.Components.Label &&
                    component.yAlignment() === 'bottom' &&
                    component.xAlignment() === 'right' ){

                    component.yAlignment('top');
                    component.xAlignment('left');

                }
                if ( component instanceof Plottable.Plots.Scatter && component.datasets().length > 0 ) {

                    var scatterPlot = component;

                    if (! self.clickInteraction ){
                        self.clickInteraction = new Plottable.Interactions.Click()
                            .onClick( function(point) {
                                var nearestEntity;
                                try {
                                    nearestEntity = scatterPlot.entityNearest(point);
                                } catch (e) {
                                    return;
                                }
                            });
                    }

                    self.clickInteraction.attachTo( scatterPlot );
                    self.clickInteractionComponent = scatterPlot;
                }

            });

        };

        self.makeChart = function makeChart( data, element, measureList, configOptions ) {
            console.log("Making chart");
            self.waitingForGraph = true;
            //if data is from shimmer, the points are in an array called 'body'

            if ( self.chart ){
                console.log("Chart exists recreating");
                self.chart.destroy();
                if ( self.clickInteraction && self.clickInteractionComponent ){
                    self.clickInteraction.detachFrom( self.clickInteractionComponent );
                }
            }

            //builds a new plottable chart
            self.chart = new OMHWebVisualizations.Chart( data, element, measureList, configOptions );

            if ( self.chart.initialized ){
                console.log("Chart initialized");
                //customizes the chart's components
                self.customizeChartComponents( self.chart.getComponents() );

                //renders the chart to an svg element
                self.showChart();
                self.chart.renderTo( element.select("svg").node() );
            } else {
                console.log("Could not initialize chart");
                self.hideChart();
            }
            self.waitingForGraph = false;
            console.log("Finished making chart");
        };


        //this is a test
        //console.log("Making Chart");
        //self.omhActivity = {
        //    "shim": "googlefit",
        //    "timeStamp": 1534251049,
        //    "body": [
        //        {
        //            "header": {
        //                "id": "3b9b68a2-e0fd-4bdd-ba85-4127a4e8bcee",
        //                "creation_date_time": "2018-08-14T12:50:49.383Z",
        //                "acquisition_provenance": {
        //                    "source_name": "some source",
        //                    "source_origin_id": "some device"
        //                },
        //                "schema_id": {
        //                    "namespace": "omh",
        //                    "name": "step-count",
        //                    "version": "2.0"
        //                }
        //            },
        //            "body": {
        //                "effective_time_frame": {
        //                    "time_interval": {
        //                        "start_date_time": "2018-08-14T00:00:17.805Z",
        //                        "end_date_time": "2018-08-14T00:01:17.805Z"
        //                    }
        //                },
        //                "step_count": 7
        //            }
        //        },
        //        {
        //            "header": {
        //                "id": "3b9b68a2-e0fd-4bdd-ba85-4127a4e8bcff",
        //                "creation_date_time": "2018-08-14T12:50:49.383Z",
        //                "acquisition_provenance": {
        //                    "source_name": "some source",
        //                    "source_origin_id": "some device"
        //                },
        //                "schema_id": {
        //                    "namespace": "omh",
        //                    "name": "step-count",
        //                    "version": "2.0"
        //                }
        //            },
        //            "body": {
        //                "effective_time_frame": {
        //                    "time_interval": {
        //                        "start_date_time": "2018-08-14T00:03:17.805Z",
        //                        "end_date_time": "2018-08-14T00:04:17.805Z"
        //                    }
        //                },
        //                "step_count": 27
        //            }
        //        }
        //    ]
        //};
        //self.makeChart(self.omhActivity.body, d3.select('.chart-container'), "step_count", self.options);
        //console.log("Finished Making Chart");

    }]
});
