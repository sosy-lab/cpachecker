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
      $injector.get("$controller")("SourceController", {
        $scope,
      });
    });
    jasmine.getFixtures().fixturesPath = "base/";
    jasmine.getFixtures().load("testReport.html");
  });

  describe("sourceFiles initialization", () => {
    it("Should be defined", () => {
      expect($scope.sourceFiles).not.toBeUndefined();
    });
  });

  describe("selectedSourceFile initialization", () => {
    it("Should be defined", () => {
      expect($scope.selectedSourceFile).not.toBeUndefined();
    });
  });

  describe("setSourceFile action handler", () => {
    it("Should be defined", () => {
      expect($scope.setSourceFile).not.toBeUndefined();
    });
  });

  describe("sourceFileIsSet action handler", () => {
    it("Should be defined", () => {
      expect($scope.sourceFileIsSet).not.toBeUndefined();
    });
  });
});
