// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2018 Lokesh Nandanwar
// SPDX-FileCopyrightText: 2018-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

describe("ReportController", function () {
  let $rootScope;
  let $scope;
  let controller;

  beforeEach(function () {
    angular.mock.module("report");

    angular.mock.inject(function ($injector) {
      $rootScope = $injector.get("$rootScope");
      $scope = $rootScope.$new();
      controller = $injector.get("$controller")("ARGToolbarController", {
        $scope: $scope,
      });
    });
    jasmine.getFixtures().fixturesPath = "base/";
    jasmine.getFixtures().load("testReport.html");
  });

  describe("zoomEnabled initialization", function () {
    it("Should be defined", function () {
      expect($scope.zoomEnabled).not.toBeUndefined();
    });
  });

  describe("argSelections initialization", function () {
    it("Should be defined", function () {
      expect($scope.argSelections).not.toBeUndefined();
    });
  });

  describe("displayedARG initialization", function () {
    it("Should be defined", function () {
      expect($scope.displayedARG).not.toBeUndefined();
    });
  });

  describe("displayARG action handler", function () {
    it("Should be defined", function () {
      expect($scope.displayARG).not.toBeUndefined();
    });
  });

  describe("argZoomControl action handler", function () {
    it("Should be defined", function () {
      expect($scope.argZoomControl).not.toBeUndefined();
    });
  });

  describe("argRedraw action handler", function () {
    it("Should be defined", function () {
      expect($scope.argRedraw).not.toBeUndefined();
    });
  });

  describe("validateInput action handler", function () {
    it("Should be defined", function () {
      expect($scope.validateInput).not.toBeUndefined();
    });
  });
});
