describe("ReportController", function () {
    var $rootScope,
        $scope,
        controller;

    beforeEach(function () {
        module('report');

        inject(function ($injector) {
            $rootScope = $injector.get('$rootScope');
            $scope = $rootScope.$new();
            controller = $injector.get('$controller')("SourceController", {
                $scope: $scope
            });
        })
        jasmine.getFixtures().fixturesPath = 'base/';
        jasmine.getFixtures().load('testReport.html');
    })

    describe("sourceFiles initialization", function () {
        it("Should be defined", function () {
            expect($scope.sourceFiles).not.toBeUndefined();
        })
    })

    describe("selectedSourceFile initialization", function () {
        it("Should be defined", function () {
            expect($scope.selectedSourceFile).not.toBeUndefined();
        })
    })

    describe("setSourceFile action handler", function () {
        it("Should be defined", function () {
            expect($scope.setSourceFile).not.toBeUndefined();
        })
    })

    describe("sourceFileIsSet action handler", function () {
        it("Should be defined", function () {
            expect($scope.sourceFileIsSet).not.toBeUndefined();
        })
    })
});