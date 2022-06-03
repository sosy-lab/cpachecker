// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2018 Lokesh Nandanwar
// SPDX-FileCopyrightText: 2018-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

const EC = protractor.ExpectedConditions;

const hasClass = (element, cls) =>
  element
    .getAttribute("class")
    .then((classes) => classes.split(" ").indexOf(cls) !== -1);

describe("Error path section in Report.js", () => {
  let dirname = `${__dirname}/Counterexample.1.html`;
  dirname = dirname.replace(/\\/g, "/");
  browser.waitForAngularEnabled(false);
  browser.get(dirname);
  browser.driver.sleep(100);

  describe("Start button click test in CFA tab", () => {
    it("Clicked Error path element in error path code", () => {
      browser
        .actions()
        .mouseMove(
          element(
            by.css(
              "#errorpath_section > header > div.btn-group > button.btn.btn-warning"
            )
          )
        )
        .click()
        .perform();
      browser.wait(EC.presenceOf(element(by.css(".clickedErrPathElement"))));
      expect(
        hasClass(element(by.id("errpath-0")), "clickedErrPathElement")
      ).toBe(true);
    });
    browser.driver.sleep(100);

    it("Mark Error path element in CFA graph", () => {
      browser.wait(EC.presenceOf(element(by.css(".marked-cfa-edge"))));
      expect(
        hasClass(
          element(by.xpath('//*[@id="cfa-edge_23-27"]')),
          "marked-cfa-edge"
        )
      ).toBe(true);
    });
    browser.driver.sleep(100);
  });

  describe("Next button click test in CFA tab", () => {
    it("Clicked Error path element in error path code", () => {
      element(
        by.css(
          "#errorpath_section > header > div.btn-group > button:nth-child(3)"
        )
      ).click();
      browser.wait(EC.presenceOf(element(by.css(".clickedErrPathElement"))));
      expect(
        hasClass(element(by.id("errpath-1")), "clickedErrPathElement")
      ).toBe(true);
    });
    browser.driver.sleep(100);

    it("Mark Error path element in CFA graph", () => {
      browser.wait(EC.presenceOf(element(by.css(".marked-cfa-node"))));
      expect(
        hasClass(element(by.xpath('//*[@id="cfa-node27"]')), "marked-cfa-node")
      ).toBe(true);
    });
    browser.driver.sleep(100);

    it("Mark Error path element node label in CFA graph", () => {
      browser.wait(EC.presenceOf(element(by.css(".marked-cfa-node-label"))));
      expect(
        hasClass(
          element(
            by.css("#cfa-node27 > g > g > text > tspan.marked-cfa-node-label")
          ),
          "marked-cfa-node-label"
        )
      ).toBe(true);
    });
    browser.driver.sleep(100);
  });

  describe("Previous button click test in CFA tab", () => {
    it("Clicked Error path element in error path code", () => {
      element(
        by.css(
          "#errorpath_section > header > div.btn-group > button:nth-child(1)"
        )
      ).click();
      browser.wait(EC.presenceOf(element(by.css(".clickedErrPathElement"))));
      expect(
        hasClass(element(by.id("errpath-0")), "clickedErrPathElement")
      ).toBe(true);
    });
    browser.driver.sleep(100);

    it("Mark Error path element in CFA graph", () => {
      browser.wait(EC.presenceOf(element(by.css(".marked-cfa-edge"))));
      expect(
        hasClass(
          element(by.xpath('//*[@id="cfa-edge_23-27"]')),
          "marked-cfa-edge"
        )
      ).toBe(true);
    });
    browser.driver.sleep(100);
  });

  describe("Error Code line click test in CFA tab", () => {
    it("Clicked Error path element in error path code", () => {
      element(by.css("#errpath-6")).click();
      browser.wait(EC.presenceOf(element(by.css(".clickedErrPathElement"))));
      expect(
        hasClass(element(by.id("errpath-6")), "clickedErrPathElement")
      ).toBe(true);
    });
    browser.driver.sleep(100);

    it("Mark Error path element in CFA graph", () => {
      element(by.css("#errpath-6")).click();
      browser.wait(EC.presenceOf(element(by.css(".marked-cfa-node"))));
      expect(
        hasClass(element(by.xpath('//*[@id="cfa-node1"]')), "marked-cfa-node")
      ).toBe(true);
    });
    browser.driver.sleep(100);
  });

  describe("Start button click test in ARG tab", () => {
    it("Clicked Error path element in error path code", () => {
      element(by.id("set-tab-2")).click();
      element(
        by.css(
          "#errorpath_section > header > div.btn-group > button.btn.btn-warning"
        )
      ).click();
      browser.wait(EC.presenceOf(element(by.css(".clickedErrPathElement"))));
      expect(
        hasClass(element(by.id("errpath-0")), "clickedErrPathElement")
      ).toBe(true);
    });
    browser.driver.sleep(100);

    it("Mark Error path element in ARG graph", () => {
      browser.wait(EC.presenceOf(element(by.css(".marked-arg-node"))));
      expect(
        hasClass(
          element(by.xpath('//*[@id="arg-graph0"]//*[@id="arg-node0"]')),
          "marked-arg-node"
        )
      ).toBe(true);
    });
    browser.driver.sleep(100);
  });

  describe("Next button click test in ARG tab", () => {
    it("Clicked Error path element in error path code", () => {
      element(
        by.css(
          "#errorpath_section > header > div.btn-group > button:nth-child(3)"
        )
      ).click();
      browser.wait(EC.presenceOf(element(by.css(".clickedErrPathElement"))));
      expect(
        hasClass(element(by.id("errpath-1")), "clickedErrPathElement")
      ).toBe(true);
    });
    browser.driver.sleep(100);

    it("Mark Error path element in ARG graph", () => {
      browser.wait(EC.presenceOf(element(by.css(".marked-arg-node"))));
      expect(
        hasClass(element(by.xpath('//*[@id="arg-node1"]')), "marked-arg-node")
      ).toBe(true);
    });
    browser.driver.sleep(100);
  });

  describe("Previous button click test in ARG tab", () => {
    it("Clicked Error path element in error path code", () => {
      element(
        by.css(
          "#errorpath_section > header > div.btn-group > button:nth-child(1)"
        )
      ).click();
      browser.wait(EC.presenceOf(element(by.css(".clickedErrPathElement"))));
      expect(
        hasClass(element(by.id("errpath-0")), "clickedErrPathElement")
      ).toBe(true);
    });
    browser.driver.sleep(100);

    it("Mark Error path element in ARG graph", () => {
      browser.wait(EC.presenceOf(element(by.css(".marked-arg-node"))));
      expect(
        hasClass(
          element(by.xpath('//*[@id="arg-graph0"]//*[@id="arg-node0"]')),
          "marked-arg-node"
        )
      ).toBe(true);
    });
    browser.driver.sleep(100);
  });

  describe("Error Code line click test in ARG tab", () => {
    it("Clicked Error path element in error path code", () => {
      element(by.css("#errpath-8")).click();
      browser.wait(EC.presenceOf(element(by.css(".clickedErrPathElement"))));
      expect(
        hasClass(element(by.id("errpath-8")), "clickedErrPathElement")
      ).toBe(true);
    });
    browser.driver.sleep(100);

    it("Mark Error path element in ARG graph", () => {
      element(by.css("#errpath-8")).click();
      browser.wait(EC.presenceOf(element(by.css(".marked-arg-node"))));
      expect(
        hasClass(element(by.xpath('//*[@id="arg-node4"]')), "marked-arg-node")
      ).toBe(true);
    });
    browser.driver.sleep(100);
  });

  describe("Start button click test in Source tab", () => {
    it("Clicked Error path element in error path code", () => {
      element(by.id("set-tab-3")).click();
      element(
        by.css(
          "#errorpath_section > header > div.btn-group > button.btn.btn-warning"
        )
      ).click();
      browser.wait(EC.presenceOf(element(by.css(".clickedErrPathElement"))));
      expect(
        hasClass(element(by.id("errpath-0")), "clickedErrPathElement")
      ).toBe(true);
    });
    browser.driver.sleep(100);

    it("Highlight selected code in source tab", () => {
      browser.wait(EC.presenceOf(element(by.css(".marked-source-line"))));
      expect(
        hasClass(
          element(by.xpath('//*[@id="source-1"]/td[2]/pre')),
          "marked-source-line"
        )
      ).toBe(true);
    });
    browser.driver.sleep(100);
  });

  describe("Next button click test in Source tab", () => {
    it("Clicked Error path element in error path code", () => {
      element(
        by.css(
          "#errorpath_section > header > div.btn-group > button:nth-child(3)"
        )
      ).click();
      browser.wait(EC.presenceOf(element(by.css(".clickedErrPathElement"))));
      element(
        by.css(
          "#errorpath_section > header > div.btn-group > button:nth-child(3)"
        )
      ).click();
      browser.wait(EC.presenceOf(element(by.css(".clickedErrPathElement"))));
      expect(
        hasClass(element(by.id("errpath-2")), "clickedErrPathElement")
      ).toBe(true);
    });
    browser.driver.sleep(100);

    it("Highlight selected code in source tab", () => {
      browser.wait(EC.presenceOf(element(by.css(".marked-source-line"))));
      expect(
        hasClass(
          element(by.xpath('//*[@id="source-10"]/td[2]/pre')),
          "marked-source-line"
        )
      ).toBe(true);
    });
    browser.driver.sleep(100);
  });

  describe("Previous button click test in Source tab", () => {
    it("Clicked Error path element in error path code", () => {
      element(
        by.css(
          "#errorpath_section > header > div.btn-group > button:nth-child(1)"
        )
      ).click();
      browser.wait(EC.presenceOf(element(by.css(".clickedErrPathElement"))));
      element(
        by.css(
          "#errorpath_section > header > div.btn-group > button:nth-child(1)"
        )
      ).click();
      browser.wait(EC.presenceOf(element(by.css(".clickedErrPathElement"))));
      expect(
        hasClass(element(by.id("errpath-0")), "clickedErrPathElement")
      ).toBe(true);
    });
    browser.driver.sleep(100);

    it("Highlight selected code in source tab", () => {
      browser.wait(EC.presenceOf(element(by.css(".marked-source-line"))));
      expect(
        hasClass(
          element(by.xpath('//*[@id="source-1"]/td[2]/pre')),
          "marked-source-line"
        )
      ).toBe(true);
    });
    browser.driver.sleep(100);
  });

  // it('Show values of Error Code line', function () {
  //     element(by.css('#errpath-6 > td:nth-child(1)')).click();
  //     browser.wait(EC.presenceOf(element(by.css('#report-controller > div.popover.fade.show.bs-popover-left'))));
  //     // element(by.xpath('//*[@id="errpath-0"]')).getAttribute('class').then(function (classes) {
  //     //     console.log(classes);
  //     //     // return classes.split(' ').indexOf(cls) !== -1;
  //     // });

  //     it('Display popover with values of line in error path code', function () {
  //         expect(element(by.css('#report-controller > div.popover.fade.show.bs-popover-left')).isDisplayed()).toBeTruthy();
  //     });
  //     browser.actions().mouseMove(element(by.xpath('//*[@id="errpath-6"]/td[1]'))).click();
  // });

  function findTooltipWithText(text) {
    return $$("div.tooltip-inner")
      .filter((elem) =>
        elem.getText().then((elementText) => elementText.includes(text))
      )
      .first();
  }

  describe("Button Group tooltip test", () => {
    it("Prev button tooltip test", async (done) => {
      browser
        .actions()
        .mouseMove(
          element(
            by.xpath('//*[@id="errorpath_section"]/header/div[1]/button[1]')
          )
        )
        .perform();
      const tooltipText = findTooltipWithText("Previous Line");
      browser.wait(EC.presenceOf(tooltipText));
      expect(tooltipText.isDisplayed()).toBeTruthy();
      done();
    });
    browser.driver.sleep(100);

    it("help button tooltip test", async (done) => {
      browser
        .actions()
        .mouseMove(
          element(by.xpath('//*[@id="errorpath_section"]/header/div[2]'))
        )
        .perform();
      const tooltipText = findTooltipWithText("help");
      browser.wait(EC.presenceOf(tooltipText));
      expect(tooltipText.isDisplayed()).toBeTruthy();
      done();
    });
    browser.driver.sleep(100);

    it("Next button tooltip test", async (done) => {
      browser
        .actions()
        .mouseMove(
          element(
            by.xpath('//*[@id="errorpath_section"]/header/div[1]/button[3]')
          )
        )
        .perform();
      const tooltipText = findTooltipWithText("Next Line");
      browser.wait(EC.presenceOf(tooltipText));
      expect(tooltipText.isDisplayed()).toBeTruthy();
      done();
    });
    browser.driver.sleep(100);
  });
});
