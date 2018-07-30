let EC = protractor.ExpectedConditions;

describe('Error path section in Report.js', function () {
    var dirname = __dirname + '/Counterexample.1.html';
    dirname = dirname.replace(/\\/g, "/");
    browser.waitForAngularEnabled(false);
    browser.get(dirname);
    browser.driver.sleep(100);
    it('Prev button tooltip test', function () {
        tooltipText = element(by.xpath('//div[@class="tooltip-inner"]'));
        browser.actions().mouseMove(element(by.xpath('//*[@id="errorpath_section"]/header/div[1]/button[1]'))).perform();
        browser.wait(EC.presenceOf(tooltipText))
        expect(tooltipText.isDisplayed()).toBeTruthy();
    });
    browser.driver.sleep(100);

    // it('help button popover test', function () {
    //     element(by.id('error_path_help_button')).click();
    //     popover = element(by.xpath('//*[@id="errorpath_section"]/header/div[2]/a'));
    //     browser.wait(EC.presenceOf(popover))
    //     expect(popover.isDisplayed()).toBeTruthy();
    //     browser.actions().mouseMove(element(by.css('section'))).click();
    // });
    // browser.driver.sleep(100);
    it('help button tooltip test', function () {
        tooltipText = element(by.xpath('//div[@class="tooltip-inner"]'));
        browser.actions().mouseMove(element(by.id('error_path_help_button'))).perform();
        browser.wait(EC.presenceOf(tooltipText))
        expect(tooltipText.isDisplayed()).toBeTruthy();
    });

    it('Next button tooltip test', function () {
        tooltipText = element(by.xpath('//div[@class="tooltip-inner"]'));
        browser.actions().mouseMove(element(by.xpath('//*[@id="errorpath_section"]/header/div[1]/button[3]'))).perform();
        browser.wait(EC.presenceOf(tooltipText))
        expect(tooltipText.isDisplayed()).toBeTruthy();
    });
});