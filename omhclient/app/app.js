'use strict';

var env = {};

//Import variables if present from env.js
if(window){
    Object.assign(env, window.__env);
}

// Declare app level module which depends on views, and components
var ngModule = angular.module('myApp', [
  'ngRoute',
  'myApp.version',
  'launch',
  'activity',
  'login',
  'omhOnFhirService'
]).
config(['$locationProvider', '$routeProvider', function($locationProvider, $routeProvider) {
  $locationProvider.html5Mode(true);
  $locationProvider.hashPrefix('!');

  $routeProvider.
      when('/launch',{
        template: '<launch></launch>'
      }).
      when('/activity', {
        template: '<activity></activity>'
      }).
      when('/login', {
        template: '<login></login>'
      }).
      otherwise({redirectTo: '/login'});
}])
.constant('__env', env);//Register environment in AngularJS as constant