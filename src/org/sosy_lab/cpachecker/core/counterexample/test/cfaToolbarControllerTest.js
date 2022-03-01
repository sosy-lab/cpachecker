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
            controller = $injector.get('$controller')("CFAToolbarController", {
                $scope: $scope
            });
        })
        jasmine.getFixtures().fixturesPath = 'base/';
        jasmine.getFixtures().load('testReport.html');

    })

    describe("selectedCFAFunction initialization", function () {
        it("Should be defined", function () {
            if ($scope.functions) {
                expect($scope.selectedCFAFunction).not.toBeUndefined();
            }
        })
    })

    describe("zoomEnabled initialization", function () {
        it("Should be defined", function () {
            if ($scope.functions) {
                expect($scope.zoomEnabled).not.toBeUndefined();
            }
        })
    })


    describe("showValues action handler", function () {
        it("Should be defined", function () {
            expect($scope.setCFAFunction).not.toBeUndefined();
        })
    })

    describe("cfaFunctionIsSet action handler", function () {
        it("Should be defined", function () {
            expect($scope.cfaFunctionIsSet).not.toBeUndefined();
        })
    })

    describe("zoomControl action handler", function () {
        it("Should be defined", function () {
            expect($scope.zoomControl).not.toBeUndefined();
        })
    })

    describe("redraw action handler", function () {
        it("Should be defined", function () {
            expect($scope.redraw).not.toBeUndefined();
        })
    })

    describe("validateInput action handler", function () {
        it("Should be defined", function () {
            expect($scope.validateInput).not.toBeUndefined();
        })
    })

});