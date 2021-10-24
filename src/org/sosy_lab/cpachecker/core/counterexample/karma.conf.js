// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2018 Lokesh Nandanwar
// SPDX-FileCopyrightText: 2018-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Refer doc/JavascriptTesting for quikstart guide

// Karma configuration
// Generated on Fri Jun 29 2018 17:07:20 GMT+0530 (India Standard Time)
const fs = require("fs");
const isDocker = require("is-docker")();
const webpackConfig = require("./webpack.config");

// Replace scripts tag from HTML file and generate testReport.html for testing:
// Remove everything from the line with REPORT_CSS to the line after REPORT_JS
// (".*" matches a single line, "[^]*" arbitrarily many lines).
fs.writeFileSync(
  "testReport.html",
  fs
    .readFileSync("report.html", "utf8")
    .replace(/^.*REPORT_CSS[^]*REPORT_JS.*\n.*$/m, "")
);

module.exports = (config) => {
  config.set({
    // frameworks to use
    // available frameworks: https://npmjs.org/browse/keyword/karma-adapter
    frameworks: ["jasmine", "jquery-3.4.0", "detectBrowsers"],

    // list of files / patterns to load in the browser
    files: [
      { pattern: "report.js", watched: false },
      { pattern: "./node_modules/angular-mocks/ngMock.js", watched: false },
      {
        pattern: "./node_modules/jasmine-jquery/lib/jasmine-jquery.js",
        watched: false,
      },
      { pattern: "testReport.html" },
      { pattern: "test/*.js", watched: false },
    ],

    // preprocess matching files before serving them to the browser
    // available preprocessors: https://npmjs.org/browse/keyword/karma-preprocessor
    preprocessors: {
      "report.js": ["webpack"],
      "./node_modules/angular-mocks/ngMock.js": ["webpack"],
      "./node_modules/jasmine-jquery/lib/jasmine-jquery.js": ["webpack"],
      "test/*.js": ["webpack"],
    },

    // list of files / patterns to exclude
    exclude: ["test/initFunctionTest.js"],

    // test results reporter to use
    // possible values: 'dots', 'progress'
    // available reporters: https://npmjs.org/browse/keyword/karma-reporter
    reporters: ["progress", "html"],

    htmlReporter: {
      outputFile: "unit_testing_report.html",
      useLegacyStyle: true,
    },

    // add to plugins
    plugins: [
      // other plugins
      "karma-htmlfile-reporter",
      "karma-firefox-launcher",
      "karma-chrome-launcher",
      "karma-detect-browsers",
      "karma-jasmine",
      "karma-jquery",
      "karma-webpack",
    ],

    detectBrowsers: {
      enabled: true,
      usePhantomJS: false,
      preferHeadless: true,
      // Start Chromium and Chrome with the custom launcher instead and remove all browsers except for Chromium, Chrome and Firefox
      postDetection: (availableBrowsers) => {
        const newBrowsers = availableBrowsers;
        if (isDocker && newBrowsers.includes("ChromiumHeadless")) {
          newBrowsers[newBrowsers.indexOf("ChromiumHeadless")] =
            "ChromiumHeadlessNoSandbox";
        }
        if (isDocker && newBrowsers.includes("ChromeHeadless")) {
          newBrowsers[newBrowsers.indexOf("ChromeHeadless")] =
            "ChromeHeadlessNoSandbox";
        }
        return newBrowsers.filter(
          (browser) =>
            browser.includes("Chromium") ||
            browser.includes("Chrome") ||
            browser.includes("Firefox")
        );
      },
    },

    // web server port
    port: 9876,

    // enable / disable colors in the output (reporters and logs)
    colors: true,

    // level of logging
    // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel: config.LOG_INFO,

    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: false,

    // to make Chrome run in Docker container
    // https://github.com/karma-runner/karma-chrome-launcher/issues/158
    customLaunchers: {
      ChromiumHeadlessNoSandbox: {
        base: "ChromiumHeadless",
        flags: ["--no-sandbox"],
      },
      ChromeHeadlessNoSandbox: {
        base: "ChromeHeadless",
        flags: ["--no-sandbox"],
      },
    },

    // Continuous Integration mode
    // if true, Karma captures browsers, runs the tests and exits
    singleRun: true,

    // Concurrency level
    // how many browser should be started simultaneous
    concurrency: Infinity,

    webpack: {
      mode: "development",
      plugins: webpackConfig.plugins,
      module: webpackConfig.module,
      optimization: webpackConfig.optimization,
      resolve: webpackConfig.resolve,
    },
  });
};
