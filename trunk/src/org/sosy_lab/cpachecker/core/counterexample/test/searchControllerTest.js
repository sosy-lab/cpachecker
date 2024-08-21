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
      $injector.get("$controller")("SearchController", {
        $scope,
      });
    });
    jasmine.getFixtures().fixturesPath = "base/";
    jasmine.getFixtures().load("testReport.html");
  });

  describe("numOfValueMatches initialization", () => {
    it("Should be defined", () => {
      expect($scope.numOfValueMatches).not.toBeUndefined();
    });

    it("Should instantiate value equal to 0", () => {
      expect($scope.numOfValueMatches).toEqual(0);
    });
  });

  describe("numOfDescriptionMatches initialization", () => {
    it("Should be defined", () => {
      expect($scope.numOfDescriptionMatches).not.toBeUndefined();
    });

    it("Should instantiate value equal to 0", () => {
      expect($scope.numOfDescriptionMatches).toEqual(0);
    });
  });

  describe("checkIfEnter action handler", () => {
    it("Should be defined", () => {
      expect($scope.checkIfEnter).not.toBeUndefined();
    });
  });

  describe("searchFor action handler", () => {
    it("Should be defined", () => {
      expect($scope.searchFor).not.toBeUndefined();
    });
  });
});
