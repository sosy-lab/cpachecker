// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2018 Lokesh Nandanwar
// SPDX-FileCopyrightText: 2018-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

describe("ReportController", () => {
  let $rootScope;
  let $scope;

  beforeEach(() => {
    angular.mock.module("report");

    angular.mock.inject(($injector) => {
      $rootScope = $injector.get("$rootScope");
      $scope = $rootScope.$new();
      $injector.get("$controller")("ErrorpathController", {
        $scope,
      });
    });
    jasmine.getFixtures().fixturesPath = "base/";
    jasmine.getFixtures().load("testReport.html");
  });

  describe("errorPath initialization", () => {
    it("Should instantiate errorPath", () => {
      expect($scope.errorPath).not.toBeUndefined();
    });
  });

  describe("errPathPrevClicked action handler", () => {
    it("Should instantiate errPathPrevClicked", () => {
      expect($scope.errPathPrevClicked).not.toBeUndefined();
    });
  });

  describe("errPathNextClicked action handler", () => {
    it("Should instantiate errPathNextClicked", () => {
      expect($scope.errPathNextClicked).not.toBeUndefined();
    });
  });

  describe("errPathStartClicked action handler", () => {
    it("Should instantiate errPathStartClicked", () => {
      expect($scope.errPathStartClicked).not.toBeUndefined();
    });
  });

  describe("clickedErrpathElement action handler", () => {
    it("Should instantiate clickedErrpathElement", () => {
      expect($scope.clickedErrpathElement).not.toBeUndefined();
    });
  });
});
