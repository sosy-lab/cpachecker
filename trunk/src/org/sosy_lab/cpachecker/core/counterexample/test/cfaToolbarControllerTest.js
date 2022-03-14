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
      $injector.get("$controller")("CFAToolbarController", {
        $scope,
      });
    });
    jasmine.getFixtures().fixturesPath = "base/";
    jasmine.getFixtures().load("testReport.html");
  });

  describe("selectedCFAFunction initialization", () => {
    it("Should be defined", () => {
      if ($scope.functions) {
        expect($scope.selectedCFAFunction).not.toBeUndefined();
      }
    });
  });

  describe("zoomEnabled initialization", () => {
    it("Should be defined", () => {
      if ($scope.functions) {
        expect($scope.zoomEnabled).not.toBeUndefined();
      }
    });
  });

  describe("showValues action handler", () => {
    it("Should be defined", () => {
      expect($scope.setCFAFunction).not.toBeUndefined();
    });
  });

  describe("cfaFunctionIsSet action handler", () => {
    it("Should be defined", () => {
      expect($scope.cfaFunctionIsSet).not.toBeUndefined();
    });
  });

  describe("zoomControl action handler", () => {
    it("Should be defined", () => {
      expect($scope.zoomControl).not.toBeUndefined();
    });
  });

  describe("redraw action handler", () => {
    it("Should be defined", () => {
      expect($scope.redraw).not.toBeUndefined();
    });
  });

  describe("validateInput action handler", () => {
    it("Should be defined", () => {
      expect($scope.validateInput).not.toBeUndefined();
    });
  });
});
