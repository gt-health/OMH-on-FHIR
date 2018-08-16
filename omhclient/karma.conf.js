//jshint strict: false
module.exports = function(config) {
  config.set({

    basePath: './app',

    files: [
      'bower_components/angular/angular.js',
      'bower_components/angular-route/angular-route.js',
      'bower_components/angular-mocks/angular-mocks.js',

      //files to run application
      //'components/**/*.js',
      'components/omh-on-fhir-service/omh-on-fhir-service.js',
      'components/activity/activity.module.js',
      'components/activity/activity.component.js',
      'components/launch/launch.module.js',
      'components/launch/launch.component.js',
      'components/login/login.module.js',
      'components/login/login.component.js',

       //test files
      'components/activity/activity.spec.js'
    ],

    autoWatch: true,

    frameworks: ['jasmine'],

    browsers: ['Chrome'],

    plugins: [
      'karma-chrome-launcher',
      'karma-firefox-launcher',
      'karma-jasmine',
      'karma-junit-reporter'
    ],

    junitReporter: {
      outputFile: 'test_out/unit.xml',
      suite: 'unit'
    }

  });
};
