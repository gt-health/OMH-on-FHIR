'use strict';

angular.
module('activity').
component('activity', {

    templateUrl: 'components/activity/activity.template.html',
    controller: ['$scope', '$element', '$http', '$routeParams', 'OmhOnFhirApi', function ActivityController($scope, $element, $http, $routeParams, OmhOnFhirApi){
        var self = this;
        self.omhOnFhirApi = OmhOnFhirApi;

        console.log("Params passed to login");
        console.log($routeParams);

        self.waitingForSearch = false;
        self.waitingForData = false;
        self.startDate;
        self.endDate;
        self.shimmerId;
        self.activityDocumentRef;
        self.activityResourceType= "application/json";
        self.activityDataType = "OMH JSON";
        self.activityBinaryUrl;
        self.omhActivity;

        //===================================================================================
        // Variables for chart
        //===================================================================================
        self.chart = null;
        self.loadingMessage = d3.select('.loading-message');
        //self.datapointDetails = d3.select('.datapoint-details');
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

        //watch the service
        //$scope.$watch(
        //    function(){
        //        return OmhOnFhirApi.patientResourceObj;
        //    },
        //    function(newVal, oldVal, scope){
        //        if(newVal) {
        //            self.omhOnFhirApi.setPatientResourceObj(newVal);
        //        }
        //    }
        //);

        //===================================================================================
        // Functions
        //===================================================================================
        self.getPatientName = function getPatientName(){
            //self.omhOnFhirApi.getPatientName();
            self.omhOnFhirApi.getPatientName();
        };

        self.queryActivity = function queryActivity(){
            console.log("Querying patient " + self.shimmerId+ "activity from " + self.startDate + " to " + self.endDate);
            self.waitingForSearch = true;
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
                    //                "source_name": "Google Fit API",
                    //                "source_origin_id": "raw:com.google.step_count.cumulative:Google:Pixel 2 XL:5f9e1b9964be5834:LSM6DSM Step Counter"
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
                    self.makeChart(self.omhActivity, d3.select('.chart-container'), "step_count", self.options);
                    console.log("processed response");

                });
        };

        //===================================================================================
        // D3 Config
        //===================================================================================

        self.hideLoadingMessage = function hideLoadingMessage(){
            self.loadingMessage.classed('hidden',true);
        };

        self.updateLoadingMessage = function updateLoadingMessage ( amountLoaded ){
            self.loadingMessage.classed('hidden',false);
            self.loadingMessage.text('Loading data... ' + Math.round( amountLoaded * 100 ) + '%');
        };

        self.showLoadingError = function showLoadingError( error ){
            self.loadingMessage.classed('hidden',false);
            self.loadingMessage.html('There was an error while trying to load the data: <pre>' + JSON.stringify( error ) + '</pre>');
        };

        self.hideChart = function hideChart(){
            d3.select('.chart').classed('hidden', true);
        };

        self.showChart = function showChart(){
            d3.select('.chart').classed('hidden', false);
        };
        //
        //var showDatapointDetailsMessage = function( message ){
        //    datapointDetails.html('<h3>Data Point Details</h3> '+message);
        //};

        self.customizeChartComponents = function customizeChartComponents( components ){
            console.log("Setting Chart components");
            //move any label overlayed on the bottom right
            //of the chart up to the top left
            var plots = components.plots;

            //showDatapointDetailsMessage('Choose a measure that displays as a scatter plot to see details here.');

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
                                    //updateDatapointDetails( nearestEntity.datum.omhDatum );
                                } catch (e) {
                                    return;
                                }
                            });
                    }

                    self.clickInteraction.attachTo( scatterPlot );
                    self.clickInteractionComponent = scatterPlot;
                    //showDatapointDetailsMessage('Click on a point to see details here...');
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
                self.hideLoadingMessage();
                self.chart.renderTo( element.select("svg").node() );
            } else {
                console.log("Could not initialize chart");
                self.hideChart();
                self.showLoadingError( 'Chart could not be initialized with the arguments supplied.' );
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
        //                    "source_name": "Google Fit API",
        //                    "source_origin_id": "raw:com.google.step_count.cumulative:Google:Pixel 2 XL:5f9e1b9964be5834:LSM6DSM Step Counter"
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
        //                    "source_name": "Google Fit API",
        //                    "source_origin_id": "raw:com.google.step_count.cumulative:Google:Pixel 2 XL:5f9e1b9964be5834:LSM6DSM Step Counter"
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

        //TODO: call a method on OmhOnFhirApi, figure out how to do serialization in
    }]
});