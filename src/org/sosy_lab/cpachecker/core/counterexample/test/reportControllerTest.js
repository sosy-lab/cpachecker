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
      $injector.get("$controller")("ReportController", {
        $scope,
      });
    });
    jasmine.getFixtures().fixturesPath = "base/";
    jasmine.getFixtures().load("testReport.html");
  });

  describe("Logo initialization", () => {
    it("Should instantiate logo", () => {
      expect($scope.logo).toEqual("https://cpachecker.sosy-lab.org/logo.svg");
    });
  });

  describe(" help_content initialization", () => {
    it("Should instantiate logo", () => {
      expect($scope.help_content).toEqual(
        '<div class="container " style="font-family: Arial"><p><b>CFA</b> (Control Flow Automaton) shows the control flow of the program. <br> For each function in the source code one CFA graph is created. <br>' +
          "Initially all CFA's are displayed below one another beginning with the CFA for the program entry function.</p>" +
          "<p> If an error path is detected by the analysis the edges leading to it will appear red.</p>" +
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
          '<p><span style="background-color:green;">&#9645;</span> covered state</p>' +
          '<p><span style="background-color:orange;">&#9645;</span> not yet processed state</p>' +
          '<p><span style="background-color:cornflowerblue;">&#9645;</span> important state (depending on used analysis)</p>' +
          '<p><span style="background-color:red;">&#9645;</span> target state</p>' +
          "<p>- doubleclick on node to jump to relating node in CFA</p>" +
          "<p>- use the Displayed ARG select box to select between the complete ARG and ARG containing only the error path (only in case an error was found) </p>" +
          "<p>- use the Mouse Wheel Zoom checkbox to alter between scroll and zoom behaviour on mouse wheel</p>" +
          "<p>- use Split Threshold and 'Refresh button' to redraw the graph (values between 500 and 900)</p>" +
          "<p><b>Verification Coverage</b> Select from the selector box the coverage criteria which should be applied for coloring the CFA locations or source code lines.</p>" +
          "<p>- Following verification coverage measures are only applied for CFA locations:</p>" +
          "<p>-- Visited Locations: Marks all locations green which were visited during the analysis and white if not</p>" +
          "<p>-- Reached Locations: Marks all locations green which were reached during the analysis and white if not</p>" +
          "<p>-- Considered-Locations Heat Map:</p>" +
          '<p><span style="background-color:#3aec49;">&#9675;</span> reached state: state was visited (by the transfer relation) during the analysis and was reached (reached means that the lcoations is contained within a state of the ARG), darker green colors means relatively more often visited</p>' +
          '<p><span style="background-color:#ff6e6e;">&#9675;</span> unreachable state: state was visited during analysis, but is not considered reached</p>' +
          '<p><span style="background-color:white;">&#9675;</span> dead state: state was not visited during the analysis</p>' +
          "<p>-- Predicate-Considered Locations: Marks all locations green which were visited and in case of assume-edges, where a predicate existed containing all variables of the assume-edge formula</p>" +
          "<p>-- Predicate-Relevant-Variables Locations: Similar to Predicate-Considered Locations except a filter is applied so that only relevant variables are checked within the assume-edge formulas</p>" +
          "<p>- Following verification coverage measures are only applied for source code lines:</p>" +
          "<p>-- Visited-Lines Heat Map: Marks all lines green which were visited during the analysis and red if not, lines which are white were never considered during the analysis</p>" +
          "<p>- Variable-based: Following verification coverage measures are only applied for source code variables:</p>" +
          "<p>-- Predicate-Abstraction Variables: Marks all variables red which were considered by abstraction formulas</p>" +
          "<p><b>TDG</b> (Time Dependent Graph) depicts different coverage measures depending on time on a 2D graph, it can be also used for other data.</p>" +
          "<p>- use the Displayed TDG select box to select between different coverage measures (which ones are available depends on the used CPA configuration)</p>" +
          "<p>- hover over data points within the graph to get a more detailed view</p>" +
          "<p><b>In case of split graph (applies to both CFA and ARG)</b><br> -- doubleclick on labelless node to jump to target node<br> -- doubleclick on 'split edge' to jump to initial edge </p></div>"
      );
    });
  });

  describe("help_errorpath initialization", () => {
    it("Should instantiate logo", () => {
      expect($scope.help_errorpath).toEqual(
        "<div style=\"font-family: Arial\"><p>The errorpath leads to the error 'edge by edge' (CFA) or 'node by node' (ARG) or 'line by line' (Source)</p>" +
          "<p><b>-V- (Value Assignments)</b> Click to show all initialized variables and their values at that point in the programm.</p>" +
          "<p><b>Edge-Description (Source-Code-View)</b> Click to jump to the relating edge in the CFA / node in the ARG / line in Source (depending on active tab).\n If non of the mentioned tabs is currently set, the ARG tab will be selected.</p>" +
          "<p><b>Buttons (Prev, Start, Next)</b> Click to navigate through the errorpath and jump to the relating position in the active tab</p>" +
          "<p><b>Search</b>\n - You can search for words or numbers in the edge-descriptions (matches appear blue)\n" +
          "- You can search for value-assignments (variable names or their value) - it will highlight only where a variable has been initialized or where it has changed its value (matches appear green)\n" +
          "- An 'exact matches' search will look for a variable declarator matching exactly the provided text considering both, edge descriptions and value assignments</p></div>"
      );
    });
  });

  describe("tab initialization", () => {
    it("Should instantiate tab to 1", () => {
      expect($scope.tab).toEqual(1);
    });
  });

  describe("ChangeTab Watcher", () => {
    it("Should instantiate tab to 1", () => {
      spyOnEvent("#full_screen_mode", "click");
      $("#full_screen_mode").click();
      expect("click").toHaveBeenTriggeredOn("#full_screen_mode");
    });
  });

  describe("makeFullScreen action Handler", () => {
    it("Should be defined", () => {
      expect($scope.makeFullScreen).not.toBeUndefined();
    });
  });

  describe("setTab action Handler", () => {
    it("Should be defined", () => {
      expect($scope.setTab).not.toBeUndefined();
    });
  });

  describe("tabIsSet action Handler", () => {
    it("Should be defined", () => {
      expect($scope.tabIsSet()).not.toBeUndefined();
    });

    it("Tab must be set to 1 initially", () => {
      expect($scope.tabIsSet(1)).toEqual(true);
    });
  });

  describe("getTabSet action Handler", () => {
    it("Should be defined", () => {
      expect($scope.getTabSet()).not.toBeUndefined();
    });

    it("Tab must be set to 1 initially", () => {
      expect($scope.getTabSet()).toEqual(1);
    });

    it("Tab set to 3, must return 3", () => {
      $scope.tab = 3;
      expect($scope.getTabSet()).toEqual(3);
    });
  });
});
