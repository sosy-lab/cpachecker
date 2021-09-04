// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2018 Lokesh Nandanwar
// SPDX-FileCopyrightText: 2018-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

const EC = protractor.ExpectedConditions;

describe("External files section in Report.js", () => {
  let dirname = `${__dirname}/Counterexample.1.html`;
  dirname = dirname.replace(/\\/g, "/");
  browser.waitForAngularEnabled(false);
  browser.get(dirname);
  browser.driver.sleep(100);
  it("Toggle error path button test", () => {
    // Hide on first click
    element(by.xpath('//*[@id="toggle_button_error_path"]/label'))
      .click()
      .then(
        () => {
          // fulfillment
          expect(
            element(by.xpath('//*[@id="errorpath_section"]')).getCssValue(
              "display"
            )
          ).toBe("none");
        },
        () => {
          // rejection
        }
      );
    // Show on second click
    element(by.xpath('//*[@id="toggle_button_error_path"]/label'))
      .click()
      .then(
        () => {
          // fulfillment
          expect(
            element(by.xpath('//*[@id="errorpath_section"]')).getCssValue(
              "display"
            )
          ).toBe("block" || "inline");
        },
        () => {
          // rejection
        }
      );
    browser.refresh();
    browser.driver.sleep(100);
  });

  it("Toggle error path button tooltip test", () => {
    const tooltipText = element(by.xpath('//div[@class="tooltip-inner"]'));
    browser
      .actions()
      .mouseMove(element(by.xpath('//*[@id="toggle_button_error_path"]/label')))
      .perform();
    browser.wait(EC.presenceOf(tooltipText));
    expect(tooltipText.isDisplayed()).toBeTruthy();
  });
  browser.driver.sleep(100);

  it("CFA tab tooltip test", () => {
    const tooltipText = element(by.xpath('//div[@class="tooltip-inner"]'));
    browser
      .actions()
      .mouseMove(element(by.id("set-tab-1")))
      .perform();
    browser.wait(EC.presenceOf(tooltipText));
    expect(tooltipText.isDisplayed()).toBeTruthy();
  });
  browser.driver.sleep(100);

  it("CFA tab click test", () => {
    element(by.id("set-tab-1")).click();
    browser.wait(EC.presenceOf(element(by.id("set-tab-1"))));
    // browser.actions().mouseMove(element(by.id('set-tab-1'))).click();
    expect(element(by.css(".cfa-content.active")).isDisplayed()).toBeTruthy();
  });
  browser.driver.sleep(100);

  it("ARG tab tooltip test", () => {
    const tooltipText = element(by.xpath('//div[@class="tooltip-inner"]'));
    browser
      .actions()
      .mouseMove(element(by.id("set-tab-2")))
      .perform();
    browser.wait(EC.presenceOf(tooltipText));
    expect(tooltipText.isDisplayed()).toBeTruthy();
  });
  browser.driver.sleep(100);

  it("ARG tab click test", () => {
    const container = element(by.css(".arg-content.active"));
    // browser.wait(EC.presenceOf(element(by.id('set-tab-2'))))
    element(by.id("set-tab-2")).click();
    browser.wait(EC.presenceOf(container));
    expect(container.isDisplayed()).toBeTruthy();
  });
  browser.driver.sleep(100);

  it("Source tab tooltip test", () => {
    const tooltipText = element(by.xpath('//div[@class="tooltip-inner"]'));
    browser
      .actions()
      .mouseMove(element(by.id("set-tab-3")))
      .perform();
    browser.wait(EC.presenceOf(tooltipText));
    expect(tooltipText.isDisplayed()).toBeTruthy();
  });
  browser.driver.sleep(100);

  it("Source tab click test", () => {
    const container = element(
      by.xpath('//*[@id="externalFiles_section"]/div[5]')
    );
    browser.wait(EC.presenceOf(element(by.id("set-tab-2"))));
    element(by.id("set-tab-3")).click();
    browser.wait(EC.presenceOf(container));
    expect(container.isDisplayed()).toBeTruthy();
  });
  browser.driver.sleep(100);

  it("Log tab click test", () => {
    const container = element(
      by.xpath('//*[@id="externalFiles_section"]/div[6]')
    );
    browser.wait(EC.presenceOf(element(by.id("set-tab-2"))));
    element(by.xpath('//*[@id="externalFiles_section"]/ul/li[6]/a')).click();
    browser.wait(EC.presenceOf(container));
    expect(container.isDisplayed()).toBeTruthy();
  });
  browser.driver.sleep(100);

  it("Statistics tab click test", () => {
    const container = element(
      by.xpath('//*[@id="externalFiles_section"]/div[7]')
    );
    browser.wait(EC.presenceOf(element(by.id("set-tab-2"))));
    element(by.xpath('//*[@id="externalFiles_section"]/ul/li[7]/a')).click();
    browser.wait(EC.presenceOf(container));
    expect(container.isDisplayed()).toBeTruthy();
  });
  browser.driver.sleep(100);

  it("Configuration tab click test", () => {
    const container = element(
      by.xpath('//*[@id="externalFiles_section"]/div[8]')
    );
    browser.wait(EC.presenceOf(element(by.id("set-tab-2"))));
    element(by.xpath('//*[@id="externalFiles_section"]/ul/li[8]/a')).click();
    browser.wait(EC.presenceOf(container));
    expect(container.isDisplayed()).toBeTruthy();
  });
  browser.driver.sleep(100);
});
