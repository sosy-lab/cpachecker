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
      $injector.get("$controller")("ARGToolbarController", {
        $scope,
      });
    });
    jasmine.getFixtures().fixturesPath = "base/";
    jasmine.getFixtures().load("testReport.html");
  });

  describe("zoomEnabled initialization", () => {
    it("Should be defined", () => {
      expect($scope.zoomEnabled).not.toBeUndefined();
    });
  });

  describe("argSelections initialization", () => {
    it("Should be defined", () => {
      expect($scope.argSelections).not.toBeUndefined();
    });
  });

  describe("displayedARG initialization", () => {
    it("Should be defined", () => {
      expect($scope.displayedARG).not.toBeUndefined();
    });
  });

  describe("displayARG action handler", () => {
    it("Should be defined", () => {
      expect($scope.displayARG).not.toBeUndefined();
    });
  });

  describe("argZoomControl action handler", () => {
    it("Should be defined", () => {
      expect($scope.argZoomControl).not.toBeUndefined();
    });
  });

  describe("argRedraw action handler", () => {
    it("Should be defined", () => {
      expect($scope.argRedraw).not.toBeUndefined();
    });
  });

  describe("validateInput action handler", () => {
    it("Should be defined", () => {
      expect($scope.validateInput).not.toBeUndefined();
    });
  });
});
