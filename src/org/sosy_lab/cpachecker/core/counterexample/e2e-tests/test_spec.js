// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2018 Lokesh Nandanwar
// SPDX-FileCopyrightText: 2018-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

describe('Report.js test', function() {
  it('Title test', function() {
    var dirname =  __dirname + '/Counterexample.1.html';
    dirname = dirname.replace(/\\/g, "/");
    browser.waitForAngularEnabled(false);
    browser.get(dirname);
    expect(browser.getTitle()).toEqual('CPAchecker Report');
  });
});