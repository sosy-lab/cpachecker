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
      $injector.get("$controller")("ValueAssignmentsController", {
        $scope,
      });
    });
    jasmine.getFixtures().fixturesPath = "base/";
    jasmine.getFixtures().load("testReport.html");
  });

  describe("showValues action handler", () => {
    it("Should be defined", () => {
      expect($scope.showValues).not.toBeUndefined();
    });
  });
});
