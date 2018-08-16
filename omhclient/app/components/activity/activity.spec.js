'use strict';

describe('activity', function(){
    //beforeEach(function($provide){
    //    $provide.constant('__env', {
    //        "baseUrl": "/",
    //        "fitbitShim" : "fitbit",
    //        "googleFitShim" : "googlefit",
    //        "omhOnFhirClientId " : "test_client",
    //        "omhOnFhirScope" : "patient/*.read launch",
    //        "omhOnFhirRedirectUri" : "http://localhost:8000/#!/login",
    //        "omhOnFhirAPIBase" : "https://apps.hdap.gatech.edu/mdata",
    //        "omhOnFhirAPIShimmerAuth" : "/shimmerAuthentication"
    //    });
    //});
    //beforeEach(inject(function(___env_){
    //
    //}));
    //beforeEach(module('omhOnFhirService'));
    beforeEach(module('activity'));

    describe('ActivityController', function(){
        //initialize the controller and OMH on FHIR service
        var ctrl;
        var mockOmhOnFhirApi;
        var env;
        var $httpBackend;
        var $OmhOnFhirApi;
        var docRef =
            {
                "header": {
                    "id": "a035565e-9523-4bb9-934a-5d37a267451d",
                    "creation_date_time": "2018-08-14T12:50:49.384Z",
                    "acquisition_provenance": {
                        "source_name": "Google Fit API",
                        "source_origin_id": "raw:com.google.step_count.cumulative:Google:Pixel 2 XL:5f9e1b9964be5834:LSM6DSM Step Counter"
                    },
                    "schema_id": {
                        "namespace": "omh",
                        "name": "step-count",
                        "version": "2.0"
                    }
                },
                "body": {
                    "effective_time_frame": {
                        "time_interval": {
                            "start_date_time": "2018-08-14T00:01:17.805Z",
                            "end_date_time": "2018-08-14T00:02:13.123Z"
                        }
                    },
                    "step_count": 76
                }
            };
        var binary =
            {
                "shim": "googlefit",
                "timeStamp": 1534251049,
                "body": [
                    {
                        "header": {
                            "id": "3b9b68a2-e0fd-4bdd-ba85-4127a4e8bcee",
                            "creation_date_time": "2018-08-14T12:50:49.383Z",
                            "acquisition_provenance": {
                                "source_name": "Google Fit API",
                                "source_origin_id": "test device"
                            },
                            "schema_id": {
                                "namespace": "omh",
                                "name": "step-count",
                                "version": "2.0"
                            }
                        },
                        "body": {
                            "effective_time_frame": {
                                "time_interval": {
                                    "start_date_time": "2018-08-14T00:00:17.805Z",
                                    "end_date_time": "2018-08-14T00:01:17.805Z"
                                }
                            },
                            "step_count": 7
                        }
                    },
                    {
                        "header": {
                            "id": "a035565e-9523-4bb9-934a-5d37a267451d",
                            "creation_date_time": "2018-08-14T12:50:49.384Z",
                            "acquisition_provenance": {
                                "source_name": "Google Fit API",
                                "source_origin_id": "test device"
                            },
                            "schema_id": {
                                "namespace": "omh",
                                "name": "step-count",
                                "version": "2.0"
                            }
                        },
                        "body": {
                            "effective_time_frame": {
                                "time_interval": {
                                    "start_date_time": "2018-08-14T00:01:17.805Z",
                                    "end_date_time": "2018-08-14T00:02:13.123Z"
                                }
                            },
                            "step_count": 76
                        }
                    }
                ]
            };

        beforeEach(module(function($provide){
           $provide.service("omhOnFhirService", function(){
               this.requestBinaryAsJson = jasmine.createSpy('requestBinaryAsJson').and.callFake(function(param){return {"data":binary}});
               this.requestDocumentReference = jasmine.createSpy('requestDocumentReference').and.callFake(function(param){return {"data":docRef}});
           });
        }));

        //beforeEach( inject(function($componentController, _$httpBackend_, _OmhOnFhirApi_){
        beforeEach( inject(function($componentController, _$httpBackend_, omhOnFhirService){
            //$OmhOnFhirApi = _OmhOnFhirApi_;
            mockOmhOnFhirApi = omhOnFhirService;
            ctrl = $componentController('activity');
            $httpBackend = _$httpBackend_;
        }));

        it('should parse binary response', function(){
            console.log("Testing Binary");
            ctrl.queryBinary();
            //var request = /.+\/Binary\/.+/;
            //$httpBackend.whenGET(request).respond(binary);
            //$httpBackend.flush();
            expect(ctrl.omhActivity.body.length).toEqual(2);

            expect(ctrl.omhActivity.body[0].body.step_count).toEqual(7);
            expect(ctrl.omhActivity.body[0].body.effective_time_frame.time_interval.start_date_time).toEqual("2018-08-14T00:00:17.805Z");
            expect(ctrl.omhActivity.body[0].body.effective_time_frame.time_interval.end_date_time).toEqual("2018-08-14T00:01:17.805Z");
            expect(ctrl.omhActivity.body[0].header.acquisition_provenance.source_name).toEqual("Google Fit API");
            expect(ctrl.omhActivity.body[0].header.acquisition_provenance.source_origin_id).toEqual("test device");
            expect(ctrl.omhActivity.body[0].header.creation_date_time).toEqual("2018-08-14T12:50:49.383Z");
            expect(ctrl.omhActivity.body[0].header.schema_id.name).toEqual("step-count");
            expect(ctrl.omhActivity.body[0].header.schema_id.namespace).toEqual("omh");
            expect(ctrl.omhActivity.body[0].header.schema_id.version).toEqual("2.0");

            expect(ctrl.omhActivity.body[1].body.step_count).toEqual(76);
            expect(ctrl.omhActivity.body[1].body.effective_time_frame.time_interval.start_date_time).toEqual("2018-08-14T00:01:17.805Z");
            expect(ctrl.omhActivity.body[1].body.effective_time_frame.time_interval.end_date_time).toEqual("2018-08-14T00:02:13.123Z");
            expect(ctrl.omhActivity.body[1].header.acquisition_provenance.source_name).toEqual("Google Fit API");
            expect(ctrl.omhActivity.body[1].header.acquisition_provenance.source_origin_id).toEqual("test device");
            expect(ctrl.omhActivity.body[1].header.creation_date_time).toEqual("2018-08-14T12:50:49.383Z");
            expect(ctrl.omhActivity.body[1].header.schema_id.name).toEqual("step-count");
            expect(ctrl.omhActivity.body[1].header.schema_id.namespace).toEqual("omh");
            expect(ctrl.omhActivity.body[1].header.schema_id.version).toEqual("2.0");
            console.log("Finished Testing Binary");
        });
    });
});