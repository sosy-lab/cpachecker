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
            controller = $injector.get('$controller')("ReportController", {
                $scope: $scope
            });
        })
        jasmine.getFixtures().fixturesPath = 'base/';
        jasmine.getFixtures().load('testReport.html');
    })

    describe("Logo initialization", function () {
        it("Should instantiate logo", function () {
            expect($scope.logo).toEqual("https://cpachecker.sosy-lab.org/logo.svg");
        })
    })

    describe(" help_content initialization", function () {
        it("Should instantiate logo", function () {
            expect($scope.help_content).toEqual("<div class=\"container \" style=\"font-family: Arial\"><p><b>CFA</b> (Control Flow Automaton) shows the control flow of the program. <br> For each function in the source code one CFA graph is created. <br>" +
                "Initially all CFA's are displayed below one another beginning with the CFA for the program entry function.</p>" + "<p> If an error path is detected by the analysis the edges leading to it will appear red.</p>" +
                "<p>&#9675; &nbsp; normal element</p>" +
                "<p>&#9634; &nbsp; combined normal elements</p>" +
                "<p>&#9645; &nbsp; function node</p>" +
                "<p>&#9671; &nbsp; loop head</p>" +
                "<p>- doubleclick on a function node to select the CFA for this function</p>" +
                "<p>- doubleclick on edges to jump to the relating line in the Source tab</p>" +
                "<p>- use the Displayed CFA select box to display only the CFA for the desired function </p>" +
                "<p>- use the Mouse Wheel Zoom checkbox to alter between scroll and zoom behaviour on mouse wheel</p>" +
                "<p>- use Split Threshold and 'Refresh button' to redraw the graph (values between 500 and 900)</p>" +
                "<p><b>ARG</b> (Abstract Reachability Graph) shows the explored abstract state space</p>" +
                "<p> If an error path is detected by the analysis the edges leading to it will appear red.</p>" +
                "<p><span style=\"background-color:green;\">&#9645;</span> covered state</p>" +
                "<p><span style=\"background-color:orange;\">&#9645;</span> not yet processed state</p>" +
                "<p><span style=\"background-color:cornflowerblue;\">&#9645;</span> important state (depending on used analysis)</p>" +
                "<p><span style=\"background-color:red;\">&#9645;</span> target state</p>" +
                "<p>- doubleclick on node to jump to relating node in CFA</p>" +
                "<p>- use the Displayed ARG select box to select between the complete ARG and ARG containing only the error path (only in case an error was found) </p>" +
                "<p>- use the Mouse Wheel Zoom checkbox to alter between scroll and zoom behaviour on mouse wheel</p>" +
                "<p>- use Split Threshold and 'Refresh button' to redraw the graph (values between 500 and 900)</p>" +
                "<p><b>In case of split graph (applies to both CFA and ARG)</b><br> -- doubleclick on labelless node to jump to target node<br> -- doubleclick on 'split edge' to jump to initial edge </p></div>")
        });
    });

    describe("help_errorpath initialization", function () {
        it("Should instantiate logo", function () {
            expect($scope.help_errorpath).toEqual("<div style=\"font-family: Arial\"><p>The errorpath leads to the error 'edge by edge' (CFA) or 'node by node' (ARG) or 'line by line' (Source)</p>" +
                "<p><b>-V- (Value Assignments)</b> Click to show all initialized variables and their values at that point in the programm.</p>" +
                "<p><b>Edge-Description (Source-Code-View)</b> Click to jump to the relating edge in the CFA / node in the ARG / line in Source (depending on active tab).\n If non of the mentioned tabs is currently set, the ARG tab will be selected.</p>" +
                "<p><b>Buttons (Prev, Start, Next)</b> Click to navigate through the errorpath and jump to the relating position in the active tab</p>" +
                "<p><b>Search</b>\n - You can search for words or numbers in the edge-descriptions (matches appear blue)\n" +
                "- You can search for value-assignments (variable names or their value) - it will highlight only where a variable has been initialized or where it has changed its value (matches appear green)\n" +
                "- An 'exact matches' search will look for a variable declarator matching exactly the provided text considering both, edge descriptions and value assignments</p></div>");
        });
    });

    describe("tab initialization", function () {
        it("Should instantiate tab to 1", function () {
            expect($scope.tab).toEqual(1);
        })
    })

    describe("ChangeTab Watcher", function () {
        it("Should instantiate tab to 1", function () {
            var spyEvent = spyOnEvent('#full_screen_mode', 'click');
            $('#full_screen_mode').click()
            expect('click').toHaveBeenTriggeredOn('#full_screen_mode');
        })
    })


    describe("makeFullScreen action Handler", function () {

        it("Should be defined", function () {
            expect($scope.makeFullScreen).not.toBeUndefined();
        });

    });

    describe("setTab action Handler", function () {

        it("Should be defined", function () {
            expect($scope.setTab).not.toBeUndefined();
        });

    });

    describe("tabIsSet action Handler", function () {

        it("Should be defined", function () {
            expect($scope.tabIsSet()).not.toBeUndefined();
        });

        it("Tab must be set to 1 initially", function () {
            expect($scope.tabIsSet(1)).toEqual(true);
        });

    });

    describe("getTabSet action Handler", function () {

        it("Should be defined", function () {
            expect($scope.getTabSet()).not.toBeUndefined();
        });

        it("Tab must be set to 1 initially", function () {
            expect($scope.getTabSet()).toEqual(1);
        });

        it("Tab set to 3, must return 3", function () {
            $scope.tab = 3
            expect($scope.getTabSet()).toEqual(3);
        });
    });



})
