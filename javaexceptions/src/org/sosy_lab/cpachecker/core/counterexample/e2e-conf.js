// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2018 Lokesh Nandanwar
// SPDX-FileCopyrightText: 2018-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Refer doc/JavascriptTesting for quikstart guide

const fs = require("fs");

fs.createReadStream("../../../../../../output/Counterexample.1.html").pipe(
  fs.createWriteStream("e2e-tests/Counterexample.1.html")
);

const HtmlScreenshotReporter = require("protractor-jasmine2-screenshot-reporter");

const reporter = new HtmlScreenshotReporter({
  dest: "e2e-tests-report/screenshots",
  filename: "e2e-tests-report.html",
});

exports.config = {
  // Capabilities to be passed to the webdriver instance.
  capabilities: {
    browserName: "chrome",
    chromeOptions: {
      args: [
        "--headless",
        "--disable-gpu",
        "--window-size=1920,1080",
        "--no-sandbox",
      ],
    },
  },

  baseUrl: "file:///",
  // Framework to use. Jasmine is recommended.
  framework: "jasmine",

  // Spec patterns are relative to the current working directory when
  // protractor is called.
  specs: ["e2e-tests/*_spec.js"],

  // Options to be passed to Jasmine.

  jasmineNodeOpts: {
    showColors: true,
    includeStackTrace: true,
    defaultTimeoutInterval: 60000,
  },

  beforeLaunch() {
    return new Promise((resolve) => {
      reporter.beforeLaunch(resolve);
    });
  },

  // Assign the test reporter to each running instance
  onPrepare() {
    jasmine.getEnv().addReporter(reporter);
    browser.manage().window().setSize(1600, 1000);
  },

  // Close the report after all tests finish
  afterLaunch(exitCode) {
    return new Promise((resolve) => {
      reporter.afterLaunch(resolve.bind(exitCode));
    });
  },
};
