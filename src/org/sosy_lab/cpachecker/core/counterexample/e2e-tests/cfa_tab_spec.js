// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2018 Lokesh Nandanwar
// SPDX-FileCopyrightText: 2018-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

let EC = protractor.ExpectedConditions;

var hasClass = function (element, cls) {
    return element.getAttribute('class').then(function (classes) {
        console.log('')
        return classes.split(' ').indexOf(cls) !== -1;
    });
};

describe('CFA testing', function () {
    var dirname = __dirname + '/Counterexample.1.html';
    dirname = dirname.replace(/\\/g, "/");
    browser.waitForAngularEnabled(false);
    browser.get(dirname);
    browser.driver.sleep(100);

    describe('Display CFA dropdown test', function () {

        it('Display CFA dropdown test-1', function () {
            browser.wait(EC.presenceOf(element(by.id('set-tab-1'))))
            element(by.id('set-tab-1')).click();
            browser.wait(element(by.xpath('//*[@id="cfa-toolbar"]/nav/div[1]/select')));
            element(by.xpath('//*[@id="cfa-toolbar"]/nav/div[1]/select')).click();
            element(by.xpath('//*[@id="cfa-toolbar"]/nav/div[1]/select/option[2]')).click();
            expect(element(by.xpath('//*[@id="cfa-svg-main0"]')).isDisplayed()).toBeTruthy();
            expect(element(by.xpath('//*[@id="cfa-svg-__Main1"]')).isDisplayed()).toBeFalsy();

        })

        it('Display CFA dropdown test-2', function () {
            element(by.id('set-tab-1')).click();
            element(by.xpath('//*[@id="cfa-toolbar"]/nav/div[1]/select')).click();
            element(by.xpath('//*[@id="cfa-toolbar"]/nav/div[1]/select/option[1]')).click();
            expect(element(by.xpath('//*[@id="cfa-graph-main0"]')).isDisplayed()).toBeTruthy();
            expect(element(by.xpath('//*[@id="cfa-graph-__Main1"]')).isDisplayed()).toBeTruthy();
        })

    });

    describe('Hover over node', function () {

        it('Display popover dialoag box', function () {
            browser.wait(EC.presenceOf(element(by.xpath('//*[@id="cfa-node23"]'))));
            browser.actions().mouseMove(element(by.xpath('//*[@id="cfa-node23"]'))).perform();
            browser.wait(EC.presenceOf(element(by.xpath('//*[@id="infoBox"]'))));
            expect(element(by.xpath('//*[@id="infoBox"]')).isDisplayed()).toBeTruthy();
        })

    })

    describe('Double click on edge', function () {
        //Double Click not working
        it('Jump to source code', function () {
            // element(by.id('set-tab-1')).click();
            // browser.actions().mouseMove(element(by.xpath('//*[@id="cfa-node100001"]'))).click();
            // browser.actions().doubleClick(element(by.xpath('//*[@id="cfa-node100001"]'))).click();
            // expect(element(by.xpath('//*[@id="cfa-svg-main0"]')).isDisplayed()).toBeFalsy();
            // expect(element(by.xpath('//*[@id="cfa-svg-__Main1"]')).isDisplayed()).toBeTruthy();
        })
    })

    describe('Double click on node select function', function () {
        //Double Click not working
        it('Display selected function CFA  graph', function () {
            //  element(by.id('set-tab-1')).click();
            // browser.actions().doubleClick(element(by.xpath('//*[@id="cfa-node100001"]'))).click();
            // expect(element(by.xpath('//*[@id="cfa-svg-main0"]')).isDisplayed()).toBeFalsy();
            // expect(element(by.xpath('//*[@id="cfa-svg-__Main1"]')).isDisplayed()).toBeTruthy();
        })
    })

});