// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2018 Lokesh Nandanwar
// SPDX-FileCopyrightText: 2018-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

describe("ReportController", function () {
    var $rootScope,
        $scope,
        controller;

    beforeEach(function () {
        module('report');

        inject(function ($injector) {
            $rootScope = $injector.get('$rootScope');
            $scope = $rootScope.$new();
            controller = $injector.get('$controller')("SearchController", {
                $scope: $scope
            });
        })
        jasmine.getFixtures().fixturesPath = 'base/';
        jasmine.getFixtures().load('testReport.html');
    })

    describe("numOfValueMatches initialization", function () {
        it("Should be defined", function () {
            expect($scope.numOfValueMatches).not.toBeUndefined();
        })

        it("Should instantiate value equal to 0", function () {
            expect($scope.numOfValueMatches).toEqual(0);
        })
    })

    describe("numOfDescriptionMatches initialization", function () {
        it("Should be defined", function () {
            expect($scope.numOfDescriptionMatches).not.toBeUndefined();
        })

        it("Should instantiate value equal to 0", function () {
            expect($scope.numOfDescriptionMatches).toEqual(0);
        })
    })

    describe("checkIfEnter action handler", function () {
        it("Should be defined", function () {
            expect($scope.checkIfEnter).not.toBeUndefined();
        })
    })

    describe("searchFor action handler", function () {
        it("Should be defined", function () {
            expect($scope.searchFor).not.toBeUndefined();
        })
    })
});