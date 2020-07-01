// Refer doc/JavascriptTesting for quikstart guide

// Karma configuration
// Generated on Fri Jun 29 2018 17:07:20 GMT+0530 (India Standard Time)
var fs = require('fs');

// Replace scripts tag from HTML file and generate testReport.html for testing:
// Remove everything from the line with REPORT_CSS to the line after REPORT_JS
// (".*" matches a single line, "[^]*" arbitrarily many lines).
fs.writeFileSync('testReport.html',
  fs.readFileSync('report.html', 'utf8')
    .replace(/^.*REPORT_CSS[^]*REPORT_JS.*\n.*$/m, "")
  )

module.exports = function (config) {
  config.set({

    // base path that will be used to resolve all patterns (eg. files, exclude)
    basePath: '',

    // frameworks to use
    // available frameworks: https://npmjs.org/browse/keyword/karma-adapter
    frameworks: ['jasmine'],

    // list of files / patterns to load in the browser
    files: [
      'https://cdnjs.cloudflare.com/ajax/libs/babel-core/5.8.34/browser-polyfill.js',
      'https://www.sosy-lab.org/lib/angularjs/1.7.0/angular.min.js',
      'https://cdnjs.cloudflare.com/ajax/libs/angular-mocks/1.7.2/angular-mocks.min.js',
      'https://cdnjs.cloudflare.com/ajax/libs/angular-resource/1.7.2/angular-resource.min.js',
      'https://www.sosy-lab.org/lib/jquery/3.3.1/jquery.min.js',
      'https://www.sosy-lab.org/lib/jquery-ui/1.12.1/jquery-ui.min.js',
      'https://cdn.jsdelivr.net/npm/jasmine-jquery@2.1.1/lib/jasmine-jquery.min.js',
      'https://www.sosy-lab.org/lib/bootstrap/4.1.1/js/bootstrap.min.js',
      'https://www.sosy-lab.org/lib/d3js/5.4.0/d3.min.js',
      'https://www.sosy-lab.org/lib/dagre-d3/0.5.0/dagre-d3.min.js',
      'https://www.sosy-lab.org/lib/datatables/1.10.18/datatables.min.js',
      'https://www.sosy-lab.org/lib/popper.js/1.14.3/umd/popper.min.js',
      'https://www.sosy-lab.org/lib/google-code-prettify/2018-04-29-453bd5f/prettify.js',
      'testReport.html',
      'report.js',
      'test/*.js',
    ],

    // list of files / patterns to exclude
    exclude: [],

    // preprocess matching files before serving them to the browser
    // available preprocessors: https://npmjs.org/browse/keyword/karma-preprocessor
    preprocessors: {},

    // test results reporter to use
    // possible values: 'dots', 'progress'
    // available reporters: https://npmjs.org/browse/keyword/karma-reporter
    reporters: ['progress', 'html'],

    htmlReporter: {
      outputFile: 'unit_testing_report.html',
      useLegacyStyle: true,
    },

    // add to plugins
    plugins: [
      // other plugins
      'karma-htmlfile-reporter',
      'karma-phantomjs-launcher',
      'karma-jasmine'
    ],

    // web server port
    port: 9876,

    // enable / disable colors in the output (reporters and logs)
    colors: true,

    // level of logging
    // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel: config.LOG_INFO,

    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: false,

    // start these browsers
    // available browser launchers: https://npmjs.org/browse/keyword/karma-launcher
    browsers: ['PhantomJS'],

    // Continuous Integration mode
    // if true, Karma captures browsers, runs the tests and exits
    singleRun: true,

    // Concurrency level
    // how many browser should be started simultaneous
    concurrency: Infinity
  })
}
