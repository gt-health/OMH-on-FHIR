'use strict';

angular.
module('activity').
component('activity', {

    templateUrl: 'components/activity/activity.template.html',
    controller: ['$scope', '$rootScope', '$http', '$window', '$routeParams', 'OmhOnFhirApi', function ActivityController($scope, $rootScope, $http, $window, $routeParams, OmhOnFhirApi){
        var self = this;
        self.omhOnFhirApi = OmhOnFhirApi;

        console.log("Params passed to login");
        console.log($routeParams);
        self.waitingForSearch = false;
        self.waitingForObservationSearch = false;
        self.waitingForData = false;
        self.startDate;
        self.endDate;
        self.shimmerId;
        self.activityDocumentRef;
        self.activityResourceType= "application/json";
        self.activityDataType = "OMH JSON";
        self.activityBinaryUrl;
        self.omhActivity;
        self.observationResponse = "";

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
                    'timeQuantizationLevel': OMHWebVisualizations.QUANTIZE_NONE,
                    'chart': {
                        'type' : 'line',
                        'daysShownOnTimeline': undefined
                    }
                }
            }
        };

        ////register listener to log user out on exit
        //$window.onbeforeunload = self.onExit();

        //===================================================================================
        // Initialization
        //===================================================================================
        if($routeParams.shimmerId){
            self.shimmerId = $routeParams.shimmerId;
        }
        var date = new Date();
        var formattedDate = date.toISOString().substring(0,10);//to make format 'yyyy-MM-dd'
        //self.startDate = formattedDate;
        //self.endDate = formattedDate;
        //console.log("Set start date: " + self.startDate);
        //console.log("Set end date: " + self.endDate);
        self.startDate = date;
        self.endDate = date;

        //===================================================================================
        // Functions
        //===================================================================================

        self.onExit = function onExit(){
            console.log("Logging out user " + self.shimmerId);
            self.omhOnFhirApi.logoutShimmerUser(self.shimmerId);
        }

        self.queryActivity = function queryActivity(){
            console.log("Querying patient " + self.shimmerId+ "activity from " + self.startDate + " to " + self.endDate);
            self.waitingForSearch = true;
            self.observationResponse = "";
            self.omhOnFhirApi.requestDocumentReference(self.shimmerId, self.startDate, self.endDate)
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
                });
        };

        self.queryBinary = function queryBinary(){
            console.log("Querying binary " + self.activityBinaryUrl);
            self.waitingForData = true;
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
                    self.waitingForData = false;
                    self.makeChart(self.omhActivity.body, d3.select('.chart-container'), "step_count", self.options);
                    console.log("processed response");

                });
        };

        self.queryObservation = function queryObservation(){
            console.log("Querying Observation " + self.shimmerId+ "activity from " + self.startDate + " to " + self.endDate);
            self.waitingForObservationSearch = true;
            self.omhOnFhirApi.requestObservation(self.shimmerId, self.startDate, self.endDate)
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
                    self.observationResponse = JSON.stringify(response.data, null, 2);
                    self.waitingForObservationSearch = false;
                });
        };

        self.getPatientName = function getPatientName(){
            return $rootScope.patientName;
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