// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2018 Lokesh Nandanwar
// SPDX-FileCopyrightText: 2018-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

const EC = protractor.ExpectedConditions;

describe("ARG testing", () => {
  let dirname = `${__dirname}/Counterexample.1.html`;
  dirname = dirname.replace(/\\/g, "/");
  browser.waitForAngularEnabled(false);
  browser.get(dirname);
  browser.driver.sleep(100);

  describe("Display ARG dropdown test", () => {
    it("Display ARG dropdown test-1", () => {
      browser.wait(EC.presenceOf(element(by.id("set-tab-2"))));
      element(by.id("set-tab-2")).click();
      browser.wait(
        element(by.xpath('//*[@id="arg-toolbar"]/nav/div[1]/select'))
      );
      element(by.xpath('//*[@id="arg-toolbar"]/nav/div[1]/select')).click();
      element(
        by.xpath('//*[@id="arg-toolbar"]/nav/div[1]/select/option[2]')
      ).click();
      expect(
        element(by.xpath('//*[@id="arg-svgarg-error-graph0"]')).isDisplayed()
      ).toBeTruthy();
      expect(
        element(by.xpath('//*[@id="arg-graph0"]')).isDisplayed()
      ).toBeFalsy();
    });

    it("Display ARG dropdown test-2", () => {
      element(by.id("set-tab-2")).click();
      browser.wait(
        element(by.xpath('//*[@id="arg-toolbar"]/nav/div[1]/select'))
      );
      element(by.xpath('//*[@id="arg-toolbar"]/nav/div[1]/select')).click();
      element(
        by.xpath('//*[@id="arg-toolbar"]/nav/div[1]/select/option[1]')
      ).click();
      expect(
        element(by.xpath('//*[@id="arg-svgarg-error-graph0"]')).isDisplayed()
      ).toBeFalsy();
      expect(
        element(by.xpath('//*[@id="arg-graph0"]')).isDisplayed()
      ).toBeTruthy();
    });
  });

  describe("Hover over node", () => {
    it("Display popover dialoag box", () => {
      browser.wait(EC.presenceOf(element(by.xpath('//*[@id="arg-node0"]'))));
      browser
        .actions()
        .mouseMove(element(by.xpath('//*[@id="arg-node0"]')))
        .perform();
      browser.wait(EC.presenceOf(element(by.xpath('//*[@id="infoBox"]'))));
      expect(
        element(by.xpath('//*[@id="infoBox"]')).isDisplayed()
      ).toBeTruthy();
    });
  });

  describe("Double click on node function", () => {
    // Double Click not working
    it("Jump to CFA node", () => {
      // element(by.id('set-tab-1')).click();
      // browser.actions().mouseMove(element(by.xpath('//*[@id="cfa-node100001"]'))).click();
      // browser.actions().doubleClick(element(by.xpath('//*[@id="cfa-node100001"]'))).click();
      // expect(element(by.xpath('//*[@id="cfa-svg-main0"]')).isDisplayed()).toBeFalsy();
      // expect(element(by.xpath('//*[@id="cfa-svg-__Main1"]')).isDisplayed()).toBeTruthy();
    });
  });
});
