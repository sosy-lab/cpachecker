var fs = require('fs');

fs.createReadStream('../../../../../../output/Counterexample.1.html').pipe(fs.createWriteStream('e2e-tests/Counterexample.1.html'));

exports.config = {

    // Capabilities to be passed to the webdriver instance.
    capabilities: {
        'browserName': 'chrome',
        chromeOptions: {
            args: ["allow-file-access-from-files", "disable-web-security", "allow-file-access", "--disable-gpu", "--window-size=800x600", ]
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
        defaultTimeoutInterval: 30000
    },

    onPrepare: function () {
        browser.manage().window().setSize(1600, 1000);
    }
};