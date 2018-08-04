var fs = require('fs');

fs.createReadStream('../../../../../../output/Counterexample.1.html').pipe(fs.createWriteStream('e2e-tests/Counterexample.1.html'));

var HtmlScreenshotReporter = require('protractor-jasmine2-screenshot-reporter');

var reporter = new HtmlScreenshotReporter({
    dest: 'e2e-tests-report/screenshots',
    filename: 'e2e-tests-report.html'
});

exports.config = {

    // Capabilities to be passed to the webdriver instance.
    capabilities: {
        'browserName': 'chrome',
        chromeOptions: {
            args: ["allow-file-access-from-files", "--headless", "disable-web-security", "allow-file-access", "--disable-gpu", "--window-size=800x600", ]
        }
    },

    directConnect: true,

    baseUrl: 'file:///',
    // Framework to use. Jasmine is recommended.
    framework: 'jasmine',

    // Spec patterns are relative to the current working directory when
    // protractor is called.
    specs: ['e2e-tests/*_spec.js'],

    // Options to be passed to Jasmine.

    jasmineNodeOpts: {
        showColors: true,
        includeStackTrace: true,
        defaultTimeoutInterval: 1440000
    },

    beforeLaunch: function () {
        return new Promise(function (resolve) {
            reporter.beforeLaunch(resolve);
        });
    },

    // Assign the test reporter to each running instance
    onPrepare: function () {
        jasmine.getEnv().addReporter(reporter);
        browser.manage().window().setSize(1600, 1000);
    },

    // Close the report after all tests finish
    afterLaunch: function (exitCode) {
        return new Promise(function (resolve) {
            reporter.afterLaunch(resolve.bind(this, exitCode));
        });
    }
};