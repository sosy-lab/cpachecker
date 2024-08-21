// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2018 Lokesh Nandanwar
//
// SPDX-License-Identifier: Apache-2.0

/* Refer to the doc/ReportTemplateStyleGuide.md for Coding and Style Guide. They will let you write better code
with considerably less effort */
import "./report.css";
import "bootstrap/dist/css/bootstrap.min.css";
import "datatables.net-dt/css/jquery.dataTables.min.css";
import "jquery-ui-dist/jquery-ui.min.css";

import "@fortawesome/fontawesome-free/js/all.min";
import $ from "jquery";
import "jquery-ui-dist/jquery-ui.min";
import "angular";
import "angular-sanitize";
import "bootstrap";
import "datatables.net";
import "code-prettify";
import enqueue from "./worker/workerDirector";
import {
  argWorkerCallback,
  argWorkerErrorCallback,
} from "./worker/argWorkerUtils";
import {
  cfaWorkerCallback,
  cfaWorkerErrorCallback,
} from "./worker/cfaWorkerUtils";

const d3 = require("d3");

const isDevEnv = process.env.NODE_ENV !== "production";
const dependencies = require("dependencies");

// Load example data into the window when using development mode
if (isDevEnv) {
  const data = require("devData");
  window.argJson = data.argJson;
  window.sourceFiles = data.sourceFiles;
  window.cfaJson = data.cfaJson;
}

const { argJson, sourceFiles, cfaJson } = window;

// CFA graph variable declarations
const functions = cfaJson.functionNames;
let errorPath;
if (Object.prototype.hasOwnProperty.call(cfaJson, "errorPath")) {
  errorPath = cfaJson.errorPath;
}
let relevantEdges;
if (Object.prototype.hasOwnProperty.call(argJson, "relevantedges")) {
  relevantEdges = argJson.relevantedges;
}
let reducedEdges;
if (Object.prototype.hasOwnProperty.call(argJson, "reducededges")) {
  reducedEdges = argJson.reducededges;
}

const graphSplitThreshold = 700;
let cfaSplit = false;
let argTabDisabled = false;

(() => {
  $(() => {
    // initialize all popovers
    $('[data-toggle="popover"]').popover({
      html: true,
      sanitize: false,
    });
    // initialize all tooltips
    $("[data-toggle=tooltip]").tooltip({
      trigger: "hover",
    });
    $(document).on("hover", "[data-toggle=tooltip]", (event) =>
      $(event.currentTarget).tooltip("show")
    );

    // hide tooltip after 5 seconds
    let timeout;
    $("[data-toggle=tooltip]").on("shown.bs.tooltip", (e) => {
      if (timeout) {
        clearTimeout(timeout);
      }
      timeout = setTimeout(() => {
        $(e.target).tooltip("hide");
      }, 5000);
    });

    // Statistics table initialization
    $(document).ready(() => {
      $("#statistics_table").DataTable({
        order: [],
        aLengthMenu: [
          [25, 50, 100, 200, -1],
          [25, 50, 100, 200, "All"],
        ],
        iDisplayLength: -1, // Default display all entries
        columnDefs: [
          {
            orderable: false, // No ordering
            targets: 0,
          },
          {
            orderable: false, // No Ordering
            targets: 1,
          },
          {
            orderable: false, // No ordering
            targets: 2,
          },
        ],
      });
    });

    // Initialize Google pretiffy code
    $(document).ready(() => {
      PR.prettyPrint();
    });

    // Configuration table initialization
    $(document).ready(() => {
      $("#config_table").DataTable({
        order: [[1, "asc"]],
        aLengthMenu: [
          [25, 50, 100, 200, -1],
          [25, 50, 100, 200, "All"],
        ],
        iDisplayLength: -1, // Default display all entries
      });
    });

    // Log table initialization
    $(document).ready(() => {
      $("#log_table").DataTable({
        order: [[0, "asc"]],
        autoWidth: false,
        aoColumns: [
          {
            sWidth: "12%",
          },
          {
            sWidth: "10%",
          },
          {
            sWidth: "10%",
          },
          {
            sWidth: "25%",
          },
          {
            sWidth: "43%",
          },
        ],
        aLengthMenu: [
          [25, 50, 100, 200, -1],
          [25, 50, 100, 200, "All"],
        ],
        iDisplayLength: -1,
      });
    });

    // Draggable divider between error path section and file section
    $(() => {
      $("#errorpath_section").resizable({
        autoHide: true,
        handles: "e",
        resize(e, ui) {
          const parent = ui.element.parent();
          // alert(parent.attr('class'));
          const remainingSpace = parent.width() - ui.element.outerWidth();
          const divTwo = ui.element.next();
          const divTwoWidth = `${
            ((remainingSpace - (divTwo.outerWidth() - divTwo.width())) /
              parent.width()) *
            100
          }%`;
          divTwo.width(divTwoWidth);
        },
        stop(e, ui) {
          const parent = ui.element.parent();
          ui.element.css({
            width: `${(ui.element.width() / parent.width()) * 100}%`,
          });
        },
      });
    });
  });

  const app = angular.module("report", ["ngSanitize"]);

  app.controller("ReportController", [
    "$rootScope",
    "$scope",
    function reportController($rootScope, $scope) {
      $scope.logo = "https://cpachecker.sosy-lab.org/logo.svg";
      $scope.help_content =
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
        "<p><b>In case of split graph (applies to both CFA and ARG)</b><br> -- doubleclick on labelless node to jump to target node<br> -- doubleclick on 'split edge' to jump to initial edge </p></div>";
      $scope.help_fault_localization =
        '<div class="container " style="font-family: Arial"> <b>Change view</b> Toggle between two different views: The default counter example and the information provided by the selected fault localization algorithm. ' +
        "In the second view, every entry consists of a header with three elements and a bigger section with more details." +
        '<ul><li>The <span style="color: #28a745"><b>first element</b></span> of the header shows the rank.</li>' +
        '<li>The <span style="color: #ffc107"><b>second element</b></span> shows the score, which is computed by several heuristics.</li></ul>' +
        "<p>Usually, faults with a higher score have a higher rank, too. " +
        "The Error-Invariants-Algorithm does not sort the faults by score, but by hierarchical order.</p>" +
        "<p>Every fault localization technique provides information shown in the details section. " +
        "In the details section, variables are displayed as follows function::variable-name@ssa-index. " +
        "In some cases, a program contains calls to <code>__VERIFIER_nondet_X()</code> where <code>X</code> " +
        "stands for all supported data types. CPAchecker creates temporary variables for every call to <code>__VERIFIER_nondet_X()</code>. " +
        "Starting with <code>!2</code> CPAchecker sequentially increments the counter for every new call to <code>__VERIFIER_nondet_X()</code>.</p>" +
        "<p><b>Example:</b><br> A possible formula for the given program<br>" +
        "<code>int x = __VERIFIER_nondet_int();</code><br>" +
        "<code>int y = __VERIFIER_nondet_int();</code><br>" +
        "<code>if (x == 1 && y == 2) goto ERROR;</code><br>" +
        "may look like this:<br>" +
        "<code>__VERIFIER_nondet_int!2@ = 1 && __VERIFIER_nondet_int!3@ = 2</code></p>" +
        "</div>";
      $scope.help_errorpath =
        "<div style=\"font-family: Arial\"><p>The errorpath leads to the error 'edge by edge' (CFA) or 'node by node' (ARG) or 'line by line' (Source)</p>" +
        "<p><b>-V- (Value Assignments)</b> Click to show all initialized variables and their values at that point in the programm.</p>" +
        "<p><b>Edge-Description (Source-Code-View)</b> Click to jump to the relating edge in the CFA / node in the ARG / line in Source (depending on active tab).\n If non of the mentioned tabs is currently set, the ARG tab will be selected.</p>" +
        "<p><b>Buttons (Prev, Start, Next)</b> Click to navigate through the errorpath and jump to the relating position in the active tab</p>" +
        "<p><b>Search</b>\n - You can search for words or numbers in the edge-descriptions (matches appear blue)\n" +
        "- You can search for value-assignments (variable names or their value) - it will highlight only where a variable has been initialized or where it has changed its value (matches appear green)\n" +
        "- An 'exact matches' search will look for a variable declarator matching exactly the provided text considering both, edge descriptions and value assignments</p></div>";
      $scope.tab = 1;
      $scope.$on("ChangeTab", (event, tabIndex) => {
        $scope.setTab(tabIndex);
      });

      // Toggle button to hide the error path section
      $scope.toggleErrorPathSection = (event) => {
        $("#toggle_error_path").on("change", () => {
          console.log(event);
          if ($(event.target).is(":checked")) {
            d3.select("#errorpath_section").style("display", "inline");
            d3.select("#errorpath_section").style("width", "25%");
            d3.select("#externalFiles_section").style("width", "75%");
            d3.select("#cfa-toolbar").style("width", "auto");
          } else {
            d3.select("#errorpath_section").style("display", "none");
            d3.select("#externalFiles_section").style("width", "100%");
            d3.select("#cfa-toolbar").style("width", "95%");
          }
        });
      };

      // Full screen mode function to view the report in full screen
      $("#full_screen_mode").click((event) => {
        $(event.currentTarget).find("i").toggleClass("fa-compress fa-expand");
      });

      $scope.makeFullScreen = () => {
        if (
          (document.fullScreenElement && document.fullScreenElement !== null) ||
          (!document.mozFullScreen && !document.webkitIsFullScreen)
        ) {
          if (document.documentElement.requestFullScreen) {
            document.documentElement.requestFullScreen();
          } else if (document.documentElement.mozRequestFullScreen) {
            document.documentElement.mozRequestFullScreen();
          } else if (document.documentElement.webkitRequestFullScreen) {
            document.documentElement.webkitRequestFullScreen(
              Element.ALLOW_KEYBOARD_INPUT
            );
          }
        } else if (document.cancelFullScreen) {
          document.cancelFullScreen();
        } else if (document.mozCancelFullScreen) {
          document.mozCancelFullScreen();
        } else if (document.webkitCancelFullScreen) {
          document.webkitCancelFullScreen();
        }
      };

      $scope.setTab = (tabIndex) => {
        if (tabIndex === 1) {
          if (d3.select("#arg-toolbar").style("visibility") !== "hidden") {
            d3.select("#arg-toolbar").style("visibility", "hidden");
            d3.selectAll(".arg-graph").style("visibility", "hidden");
            d3.selectAll(".arg-simplified-graph").style("visibility", "hidden");
            d3.selectAll(".arg-reduced-graph").style("visibility", "hidden");
            d3.selectAll(".arg-error-graph").style("visibility", "hidden");
            if (d3.select("#arg-container").classed("arg-content")) {
              d3.select("#arg-container").classed("arg-content", false);
            }
          }
          d3.select("#cfa-toolbar").style("visibility", "visible");
          if (!d3.select("#cfa-container").classed("cfa-content")) {
            d3.select("#cfa-container").classed("cfa-content", true);
          }
          d3.selectAll(".cfa-graph").style("visibility", "visible");
        } else if (tabIndex === 2) {
          if (argTabDisabled) return;
          if (d3.select("#cfa-toolbar").style("visibility") !== "hidden") {
            d3.select("#cfa-toolbar").style("visibility", "hidden");
            d3.selectAll(".cfa-graph").style("visibility", "hidden");
            if (d3.select("#cfa-container").classed("cfa-content")) {
              d3.select("#cfa-container").classed("cfa-content", false);
            }
          }
          d3.select("#arg-toolbar").style("visibility", "visible");
          if (!d3.select("#arg-container").classed("arg-content")) {
            d3.select("#arg-container").classed("arg-content", true);
          }
          d3.selectAll(".arg-simplified-graph").style("display", "none");
          d3.selectAll(".arg-reduced-graph").style("display", "none");
          if ($rootScope.displayedARG.indexOf("error") !== -1) {
            d3.selectAll(".arg-error-graph").style("visibility", "visible");
            if ($("#arg-container").scrollTop() === 0) {
              $("#arg-container").scrollTop(200).scrollLeft(0);
            }
          } else {
            d3.selectAll(".arg-graph").style("visibility", "visible");
            if ($("#arg-container").scrollTop() === 0) {
              const boundingRect = d3
                .select(".arg-node")
                .node()
                .getBoundingClientRect();
              $("#arg-container")
                .scrollTop(
                  boundingRect.top + $("#arg-container").scrollTop() - 300
                )
                .scrollLeft(
                  boundingRect.left + $("#arg-container").scrollLeft() - 500
                );
            }
          }
        } else {
          if (d3.select("#cfa-toolbar").style("visibility") !== "hidden") {
            d3.select("#cfa-toolbar").style("visibility", "hidden");
            d3.selectAll(".cfa-graph").style("visibility", "hidden");
            if (d3.select("#cfa-container").classed("cfa-content")) {
              d3.select("#cfa-container").classed("cfa-content", false);
            }
          }
          if (d3.select("#arg-toolbar").style("visibility") !== "hidden") {
            d3.select("#arg-toolbar").style("visibility", "hidden");
            d3.selectAll(".arg-graph").style("visibility", "hidden");
            d3.selectAll(".arg-simplified-graph").style("visibility", "hidden");
            d3.selectAll(".arg-reduced-graph").style("visibility", "hidden");
            d3.selectAll(".arg-error-graph").style("visibility", "hidden");
            if (d3.select("#arg-container").classed("arg-content")) {
              d3.select("#arg-container").classed("arg-content", false);
            }
          }
        }
        $scope.tab = tabIndex;
      };
      $scope.tabIsSet = (tabIndex) => $scope.tab === tabIndex;

      $scope.getTabSet = () => $scope.tab;
    },
  ]);

  app.controller("ErrorpathController", [
    "$rootScope",
    "$scope",
    function errorpathController($rootScope, $scope) {
      $rootScope.errorPath = [];
      // initialize array that stores the important edges. Index counts only, when edges appear in the report.
      const importantEdges = [];
      let importantIndex = -1;
      const faultEdges = [];

      // Fault Localization
      function getLinesOfFault(fault) {
        const lines = {};
        for (let i = 0; i < fault.errPathIds.length; i += 1) {
          const errorPathIdx = fault.errPathIds[i];
          const errorPathElem = $rootScope.errorPath[errorPathIdx];
          const line = {
            line: errorPathElem.line,
            desc: errorPathElem.desc,
          };
          const lineKey = line.line + line.desc;
          lines[lineKey] = line;
        }
        return Object.values(lines);
      }

      function getValues(val, prevValDict) {
        const values = {};
        if (val !== "") {
          const singleStatements = val.split("\n");
          for (let i = 0; i < singleStatements.length - 1; i += 1) {
            if (
              !Object.keys(prevValDict).includes(
                singleStatements[i].split("==")[0].trim()
              )
            ) {
              // previous dictionary does not include the statement
              values[singleStatements[i].split("==")[0].trim()] =
                singleStatements[i].split("==")[1].trim().slice(0, -1);
            } else if (
              prevValDict[singleStatements[i].split("==")[0].trim()] !==
              singleStatements[i].split("==")[1].trim().slice(0, -1)
            ) {
              // statement is included but with different value
              values[singleStatements[i].split("==")[0].trim()] =
                singleStatements[i].split("==")[1].trim().slice(0, -1);
            }
          }
        }
        return values;
      }

      function addFaultLocalizationInfo() {
        if (faultEdges !== undefined && faultEdges.length > 0) {
          for (let j = 0; j < faultEdges.length; j += 1) {
            d3.selectAll(
              `#errpath-${faultEdges[j].importantindex} td pre`
            ).classed("fault", true);
          }
          d3.selectAll("#errpath-header td pre").classed("tableheader", true);
        }
      }

      // this function puts the important edges into a CSS class that highlights them
      function highlightEdges(impEdges) {
        for (let j = 0; j < impEdges.length; j += 1) {
          d3.selectAll(`#errpath-${impEdges[j]} td pre`).classed(
            "important",
            true
          );
        }
      }

      if (errorPath !== undefined) {
        let indentationlevel = 0;
        for (let i = 0; i < errorPath.length; i += 1) {
          const errPathElem = errorPath[i];

          // do not show start, return and blank edges
          if (
            errPathElem.desc.indexOf("Return edge from") === -1 &&
            errPathElem.desc !== "Function start dummy edge" &&
            errPathElem.desc !== ""
          ) {
            importantIndex += 1;
            let previousValueDictionary = {};
            errPathElem.valDict = {};
            errPathElem.valString = "";
            if (i > 0) {
              $.extend(
                errPathElem.valDict,
                $rootScope.errorPath[$rootScope.errorPath.length - 1].valDict
              );
              previousValueDictionary =
                $rootScope.errorPath[$rootScope.errorPath.length - 1].valDict;
            }
            const newValues = getValues(
              errPathElem.val,
              previousValueDictionary
            );
            errPathElem.newValDict = newValues;
            if (!$.isEmptyObject(newValues)) {
              $.extend(errPathElem.valDict, newValues);
            }

            Object.keys(errPathElem.valDict).forEach((key) => {
              errPathElem.valString += `${key}:  ${errPathElem.valDict[key]}\n`;
            });
            // add indentation
            for (let j = 1; j <= indentationlevel; j += 1) {
              errPathElem.desc = `   ${errPathElem.desc}`;
            }
            $rootScope.errorPath.push(errPathElem);
          } else if (errPathElem.desc.indexOf("Return edge from") !== -1) {
            indentationlevel -= 1;
          } else if (errPathElem.desc.indexOf("Function start dummy") !== -1) {
            indentationlevel += 1;
          }

          if (
            errPathElem.faults !== undefined &&
            errPathElem.faults.length > 0
          ) {
            errPathElem.importantindex = importantIndex;
            errPathElem.bestrank = cfaJson.faults[errPathElem.faults[0]].rank;
            errPathElem.bestreason =
              cfaJson.faults[errPathElem.faults[0]].reason;
            if (
              errPathElem.additional !== undefined &&
              errPathElem.additional !== ""
            ) {
              errPathElem.bestreason += errPathElem.additional;
            }
            faultEdges.push(errPathElem);
          }

          // store the important edges
          if (errPathElem.importance === 1) {
            importantEdges.push(importantIndex);
          }
        }

        angular.element(document).ready(() => {
          highlightEdges(importantEdges);
          addFaultLocalizationInfo();
        });
      }

      // make faults visible to angular
      $rootScope.faults = [];
      $rootScope.precondition =
        cfaJson.precondition === undefined
          ? ""
          : cfaJson.precondition["fl-precondition"];
      $rootScope.hasPrecondition = $rootScope.precondition !== "";
      if (cfaJson.faults !== undefined) {
        for (let i = 0; i < cfaJson.faults.length; i += 1) {
          const fault = cfaJson.faults[i];
          const fInfo = { ...fault };
          // store all error-path elements related to this fault.
          // we can't do  this in the Java backend because
          // we can't be sure to have the full error-path elements in the FaultLocalizationInfo
          // when the faults-code is generated.
          fInfo.errPathIds = [];
          for (let j = 0; j < $rootScope.errorPath.length; j += 1) {
            const element = $rootScope.errorPath[j];
            if (element.faults.includes(i)) {
              fInfo.errPathIds.push(j);
              fInfo.valDict = element.valDict;
            }
            fInfo.lines = getLinesOfFault(fInfo);
          }
          $rootScope.faults.push(fInfo);
        }
      }

      $scope.hideFaults =
        $rootScope.faults === undefined || $rootScope.faults.length === 0;

      $scope.faultClicked = () => {
        $scope.hideErrorTable = !$scope.hideErrorTable;
      };

      function markSourceLine(errPathEntry) {
        if (errPathEntry.line === 0) {
          errPathEntry.line = 1;
        }
        const selection = d3.select(
          `#source-${errPathEntry.line} td pre.prettyprint`
        );
        selection.classed("marked-source-line", true);
        $(".sourceContent").scrollTop(
          selection.node().getBoundingClientRect().top +
            $(".sourceContent").scrollTop() -
            200
        );
      }

      // Retrieve the node in which this node was merged
      function getMergingNode(index) {
        return Object.keys(cfaJson.combinedNodes).find((key) =>
          cfaJson.combinedNodes[key].includes(index)
        );
      }

      function getActualSourceAndTarget(element) {
        const result = {};
        if (
          cfaJson.mergedNodes.includes(element.source) &&
          cfaJson.mergedNodes.includes(element.target)
        ) {
          result.source = getMergingNode(element.source);
          return result;
        }
        if (element.source in cfaJson.combinedNodes) {
          result.source = element.source;
          return result;
        }
        if (
          !cfaJson.mergedNodes.includes(element.source) &&
          !cfaJson.mergedNodes.includes(element.target)
        ) {
          result.source = element.source;
          result.target = element.target;
        } else if (
          !cfaJson.mergedNodes.includes(element.source) &&
          cfaJson.mergedNodes.includes(element.target)
        ) {
          result.source = element.source;
          result.target = getMergingNode(element.target);
        } else if (
          cfaJson.mergedNodes.includes(element.source) &&
          !cfaJson.mergedNodes.includes(element.target)
        ) {
          result.source = getMergingNode(element.source);
          result.target = element.target;
        }
        if (
          Object.keys(cfaJson.functionCallEdges).includes(`${result.source}`)
        ) {
          result.target = cfaJson.functionCallEdges[`${result.source}`][0];
        }
        // Ensure empty object is returned if source = target (edge non existent)
        if (result.source === result.target) {
          delete result.source;
          delete result.target;
        }
        return result;
      }

      function markCfaEdge(errPathEntry) {
        const actualSourceAndTarget = getActualSourceAndTarget(errPathEntry);
        if ($.isEmptyObject(actualSourceAndTarget)) return;
        if (actualSourceAndTarget.target === undefined) {
          const selection = d3.select(
            `#cfa-node${actualSourceAndTarget.source}`
          );
          selection.classed("marked-cfa-node", true);
          const boundingRect = selection.node().getBoundingClientRect();
          $("#cfa-container")
            .scrollTop(boundingRect.top + $("#cfa-container").scrollTop() - 300)
            .scrollLeft(
              boundingRect.left -
                d3.select("#cfa-container").style("width").split("px")[0] -
                $("#cfa-container")
            );
          if (actualSourceAndTarget.source in cfaJson.combinedNodes) {
            selection.selectAll("tspan").each(
              /* @this HTMLElement */ function tspan() {
                if (d3.select(this).html().includes(errPathEntry.source)) {
                  d3.select(this).classed("marked-cfa-node-label", true);
                }
              }
            );
          }
          return;
        }
        const selection = d3.select(
          `#cfa-edge_${actualSourceAndTarget.source}-${actualSourceAndTarget.target}`
        );
        selection.classed("marked-cfa-edge", true);
        const boundingRect = selection.node().getBoundingClientRect();
        $("#cfa-container")
          .scrollTop(boundingRect.top + $("#cfa-container").scrollTop() - 300)
          .scrollLeft(
            boundingRect.left -
              d3.select("#cfa-container").style("width").split("px")[0] -
              $("#cfa-container")
          );
      }

      function markArgNode(errPathEntry) {
        if (errPathEntry.argelem === undefined) {
          return;
        }
        let idToSelect;
        if (d3.select("#arg-graph0").style("display") !== "none")
          idToSelect = "#arg-node";
        else idToSelect = "#arg-error-node";
        const selection = d3.select(idToSelect + errPathEntry.argelem);
        selection.classed("marked-arg-node", true);
        const boundingRect = selection.node().getBoundingClientRect();
        $("#arg-container")
          .scrollTop(boundingRect.top + $("#arg-container").scrollTop() - 300)
          .scrollLeft(
            boundingRect.left -
              d3.select("#arg-container").style("width").split("px")[0] -
              $("#arg-container")
          );
      }

      function handleErrorPathElemClick(currentTab, errPathElemIndex) {
        markCfaEdge.bind(this)($rootScope.errorPath[errPathElemIndex]);
        markArgNode($rootScope.errorPath[errPathElemIndex]);
        markSourceLine($rootScope.errorPath[errPathElemIndex]);
        if (![1, 2, 3].includes(currentTab)) {
          angular.element($("#report-controller")).scope().setTab(2);
        }
      }

      function unmarkEverything() {
        [
          "marked-cfa-edge",
          "marked-cfa-node",
          "marked-cfa-node-label",
          "marked-arg-node",
          "marked-source-line",
        ].forEach((c) => {
          d3.selectAll(`.${c}`).classed(c, false);
        });
      }

      function markErrorPathElementInTab(selectedErrPathElemId) {
        const currentTab = angular
          .element($("#report-controller"))
          .scope()
          .getTabSet();
        if (!Array.isArray(selectedErrPathElemId)) {
          selectedErrPathElemId = [selectedErrPathElemId];
        }
        unmarkEverything();
        for (let k = 0; k < selectedErrPathElemId.length; k += 1) {
          const id = selectedErrPathElemId[k];
          if ($rootScope.errorPath[id] === undefined) {
            return;
          }
          handleErrorPathElemClick.bind(this)(currentTab, id);
        }
      }

      $scope.clickedFaultLocElement = function clickedFaultLocElement($event) {
        d3.selectAll(".clickedFaultLocElement").classed(
          "clickedFaultLocElement",
          false
        );
        const clickedElement = d3.select($event.currentTarget);
        clickedElement.classed("clickedFaultLocElement", true);
        const faultElementIdx = clickedElement
          .attr("id")
          .substring("fault-".length);
        const faultElement = $rootScope.faults[faultElementIdx];
        markErrorPathElementInTab.bind(this)(faultElement.errPathIds);
      };

      $scope.errPathPrevClicked = function errPathPrevClicked() {
        const selection = d3.select("tr.clickedErrPathElement");
        if (!selection.empty()) {
          const prevId =
            parseInt(selection.attr("id").substring("errpath-".length), 10) - 1;
          selection.classed("clickedErrPathElement", false);
          d3.select(`#errpath-${prevId}`).classed(
            "clickedErrPathElement",
            true
          );
          $("#value-assignment").scrollTop(
            $("#value-assignment").scrollTop() - 18
          );
          markErrorPathElementInTab.bind(this)(prevId);
        }
      };

      $scope.errPathStartClicked = function errPathStartClicked() {
        d3.select("tr.clickedErrPathElement").classed(
          "clickedErrPathElement",
          false
        );
        d3.select("#errpath-0").classed("clickedErrPathElement", true);
        $("#value-assignment").scrollTop(0);
        markErrorPathElementInTab.bind(this)(0);
      };

      $scope.errPathNextClicked = function errPathNextClicked() {
        const selection = d3.select("tr.clickedErrPathElement");
        if (!selection.empty()) {
          const nextId =
            parseInt(selection.attr("id").substring("errpath-".length), 10) + 1;
          selection.classed("clickedErrPathElement", false);
          d3.select(`#errpath-${nextId}`).classed(
            "clickedErrPathElement",
            true
          );
          $("#value-assignment").scrollTop(
            $("#value-assignment").scrollTop() + 18
          );
          markErrorPathElementInTab.bind(this)(nextId);
        }
      };

      $scope.clickedErrpathElement = function clickedErrpathElement($event) {
        d3.select("tr.clickedErrPathElement").classed(
          "clickedErrPathElement",
          false
        );
        const clickedElement = d3.select($event.currentTarget.parentNode);
        clickedElement.classed("clickedErrPathElement", true);
        markErrorPathElementInTab.bind(this)(
          clickedElement.attr("id").substring("errpath-".length)
        );
      };
    },
  ]);

  app.controller("SearchController", [
    "$rootScope",
    "$scope",
    function searchController($rootScope, $scope) {
      $scope.numOfValueMatches = 0;
      $scope.numOfDescriptionMatches = 0;

      $scope.checkIfEnter = ($event) => {
        if ($event.keyCode === 13) {
          $scope.searchFor();
        }
      };

      // Search for input in the description by using only words. A word is defined by a-zA-Z0-9 and underscore
      function searchInDescription(desc, searchInput) {
        const descStatements = desc.split(" ");
        for (let i = 0; i < descStatements.length; i += 1) {
          if (descStatements[i].replace(/[^\w.]/g, "") === searchInput) {
            return true;
          }
        }
        return false;
      }

      // Search for input in object, either exact match or a match containing the input
      function searchInValues(values, searchInput, exact) {
        if ($.isEmptyObject(values)) return false;
        let match;
        if (exact) {
          match = Object.keys(values).find((v) => v === searchInput);
        } else {
          match = Object.keys(values).find(
            (v) => v.indexOf(searchInput) !== -1
          );
        }
        if (match) return true;
        return false;
      }

      $scope.searchFor = () => {
        $scope.numOfValueMatches = 0;
        $scope.numOfDescriptionMatches = 0;
        if (d3.select("#matches").style("display") === "none") {
          d3.select("#matches").style("display", "inline");
        }
        d3.selectAll(".markedValueDescElement").classed(
          "markedValueDescElement",
          false
        );
        d3.selectAll(".markedValueElement").classed(
          "markedValueElement",
          false
        );
        d3.selectAll(".markedDescElement").classed("markedDescElement", false);
        const searchInput = $(".search-input").val().trim();
        if (searchInput !== "") {
          if ($("#optionExactMatch").prop("checked")) {
            $rootScope.errorPath.forEach((it, i) => {
              const exactMatchInValues = searchInValues(
                it.newValDict,
                searchInput,
                true
              );
              if (
                exactMatchInValues &&
                searchInDescription(it.desc.trim(), searchInput)
              ) {
                $scope.numOfValueMatches += 1;
                $scope.numOfDescriptionMatches += 1;
                $(`#errpath-${i} td`)[1].classList.add(
                  "markedValueDescElement"
                );
              } else if (exactMatchInValues) {
                $scope.numOfValueMatches += 1;
                $(`#errpath-${i} td`)[1].classList.add("markedValueElement");
              } else if (searchInDescription(it.desc.trim(), searchInput)) {
                $scope.numOfDescriptionMatches += 1;
                $(`#errpath-${i} td`)[1].classList.add("markedDescElement");
              }
            });
          } else {
            $rootScope.errorPath.forEach((it, i) => {
              const matchInValues = searchInValues(
                it.newValDict,
                searchInput,
                false
              );
              if (matchInValues && it.desc.indexOf(searchInput) !== -1) {
                $scope.numOfValueMatches += 1;
                $scope.numOfDescriptionMatches += 1;
                $(`#errpath-${i} td`)[1].classList.add(
                  "markedValueDescElement"
                );
              } else if (matchInValues) {
                $scope.numOfValueMatches += 1;
                $(`#errpath-${i} td`)[1].classList.add("markedValueElement");
              } else if (it.desc.indexOf(searchInput) !== -1) {
                $scope.numOfDescriptionMatches += 1;
                $(`#errpath-${i} td`)[1].classList.add("markedDescElement");
              }
            });
          }
        }
      };
    },
  ]);

  app.controller("ValueAssignmentsController", [
    "$rootScope",
    "$sce",
    "$scope",
    function valueAssignmentsController($rootScope, $sce, $scope) {
      $scope.showValues = ($event) => {
        const element = $event.currentTarget;
        if (element.classList.contains("markedTableElement")) {
          element.classList.remove("markedTableElement");
        } else {
          element.classList.add("markedTableElement");
        }
      };

      $scope.htmlTrusted = (html) => $sce.trustAsHtml(html);
    },
  ]);

  app.controller("CFAToolbarController", [
    "$scope",
    function cfaToolbarController($scope) {
      if (functions) {
        if (functions.length > 1) {
          $scope.functions = ["all"].concat(functions);
        } else {
          $scope.functions = functions;
        }
        $scope.selectedCFAFunction = $scope.functions[0];
        $scope.zoomEnabled = false;
      }

      $scope.setCFAFunction = () => {
        if ($scope.zoomEnabled) {
          $scope.zoomControl();
        }
        // FIXME: two-way binding does not update the selected option
        d3.selectAll("#cfa-toolbar option")
          .attr("selected", null)
          .attr("disabled", null);
        d3.select(`#cfa-toolbar [label=${$scope.selectedCFAFunction}]`)
          .attr("selected", "selected")
          .attr("disabled", true);
        if ($scope.selectedCFAFunction === "all") {
          functions.forEach((func) => {
            d3.selectAll(`.cfa-svg-${func}`).attr("display", "inline-block");
          });
        } else {
          const funcToHide = $scope.functions.filter(
            (it) => it !== $scope.selectedCFAFunction && it !== "all"
          );
          funcToHide.forEach((func) => {
            d3.selectAll(`.cfa-svg-${func}`).attr("display", "none");
          });
          d3.selectAll(`.cfa-svg-${$scope.selectedCFAFunction}`).attr(
            "display",
            "inline-block"
          );
        }
        const firstElRect = d3
          .select("[display=inline-block] .cfa-node:nth-child(2)")
          .node()
          .getBoundingClientRect();
        if (d3.select("#errorpath_section").style("display") !== "none") {
          $("#cfa-container")
            .scrollTop(firstElRect.top + $("#cfa-container").scrollTop() - 300)
            .scrollLeft(
              firstElRect.left -
                $("#cfa-container").scrollLeft() -
                d3.select("#externalFiles_section").style("width")
            );
        } else {
          $("#cfa-container")
            .scrollTop(firstElRect.top + $("#cfa-container").scrollTop() - 300)
            .scrollLeft(firstElRect.left - $("#cfa-container"));
        }
      };

      $scope.cfaFunctionIsSet = (value) => value === $scope.selectedCFAFunction;

      $scope.zoomControl = function zoomControl() {
        if ($scope.zoomEnabled) {
          $scope.zoomEnabled = false;
          d3.select("#cfa-zoom-button").html("<i class='far fa-square'></i>");
          // revert zoom and remove listeners
          d3.selectAll(".cfa-svg").each(
            /* @this HTMLElement */ function cfaSvg() {
              d3.select(this)
                .on("zoom", null)
                .on("wheel.zoom", null)
                .on("dblclick.zoom", null)
                .on("touchstart.zoom", null);
            }
          );
        } else {
          $scope.zoomEnabled = true;
          d3.select("#cfa-zoom-button").html(
            "<i class='far fa-check-square'></i>"
          );
          d3.selectAll(".cfa-svg").each(
            /* @this HTMLElement */ function cfaSvg() {
              const svg = d3.select(this);
              const svgGroup = d3.select(this.firstChild);
              const zoom = d3.zoom().on("zoom", () => {
                svgGroup.attr("transform", d3.event.transform);
              });
              svg.call(zoom);
              svg.on("dblclick.zoom", null).on("touchstart.zoom", null);
            }
          );
        }
      };

      $scope.redraw = () => {
        const input = $("#cfa-split-threshold").val();
        if (!$scope.validateInput(input)) {
          alert("Invalid input!");
          return;
        }
        d3.selectAll(".cfa-graph").remove();
        if ($scope.zoomEnabled) {
          $scope.zoomControl();
        }
        $scope.selectedCFAFunction = $scope.functions[0];
        cfaSplit = true;
        let graphCount = 0;
        cfaJson.functionNames.forEach((f) => {
          const fNodes = cfaJson.nodes.filter((n) => n.func === f);
          graphCount += Math.ceil(fNodes.length / input);
        });
        $("#cfa-modal").text(`0/${graphCount}`);
        graphCount = null;
        $("#renderStateModal").modal("show");
        enqueue("cfaWorker", {
          toSplit: input,
          cfaSplit,
          argTabDisabled,
        }).then(
          (result) => cfaWorkerCallback(result),
          (error) => cfaWorkerErrorCallback(error)
        );
        enqueue("cfaWorker", {
          rendered: "ready",
          cfaSplit,
          argTabDisabled,
        }).then(
          (result) => cfaWorkerCallback(result),
          (error) => cfaWorkerErrorCallback(error)
        );
      };

      $scope.validateInput = (input) => {
        if (input % 1 !== 0) return false;
        if (input < 500 || input > 900) return false;
        return true;
      };
    },
  ]);

  app.controller("ARGToolbarController", [
    "$rootScope",
    "$scope",
    function argToolbarController($rootScope, $scope) {
      $scope.zoomEnabled = false;
      $scope.argSelections = ["complete"];
      if (errorPath !== undefined) {
        $scope.argSelections.push("error path");
      }
      if (relevantEdges !== undefined) {
        $scope.argSelections.push("simplified");
      }
      if (reducedEdges !== undefined) {
        $scope.argSelections.push("witness");
      }
      $rootScope.displayedARG = $scope.argSelections[0];

      $scope.displayARG = () => {
        if ($scope.argSelections.length > 1) {
          if ($rootScope.displayedARG.indexOf("error") !== -1) {
            d3.selectAll(".arg-graph").style("display", "none");
            if (!d3.select(".arg-simplified-graph").empty()) {
              d3.selectAll(".arg-simplified-graph").style("display", "none");
            }
            if (!d3.select(".arg-reduced-graph").empty()) {
              d3.selectAll(".arg-reduced-graph").style("display", "none");
            }
            $("#arg-container").scrollTop(0).scrollLeft(0);
            if (d3.select(".arg-error-graph").empty()) {
              enqueue("argWorker", {
                errorGraph: true,
              }).then(
                (result) => argWorkerCallback(result),
                (error) => argWorkerErrorCallback(error)
              );
            } else {
              d3.selectAll(".arg-error-graph")
                .style("display", "inline-block")
                .style("visibility", "visible");
            }
          } else if ($rootScope.displayedARG.indexOf("simplified") !== -1) {
            d3.selectAll(".arg-graph").style("display", "none");
            d3.selectAll(".arg-reduced-graph").style("display", "none");
            $("#arg-container").scrollTop(0).scrollLeft(0);
            if (!d3.select(".arg-error-graph").empty()) {
              d3.selectAll(".arg-error-graph").style("display", "none");
            }
            if (d3.select(".arg-simplified-graph").empty()) {
              enqueue("argWorker", {
                simplifiedGraph: true,
              }).then(
                (result) => argWorkerCallback(result),
                (error) => argWorkerErrorCallback(error)
              );
            } else {
              d3.selectAll(".arg-simplified-graph")
                .style("display", "inline-block")
                .style("visibility", "visible");
            }
          } else if ($rootScope.displayedARG.indexOf("witness") !== -1) {
            d3.selectAll(".arg-graph").style("display", "none");
            d3.selectAll(".arg-simplified-graph").style("display", "none");
            $("#arg-container").scrollTop(0).scrollLeft(0);
            if (!d3.select(".arg-error-graph").empty()) {
              d3.selectAll(".arg-error-graph").style("display", "none");
            }
            if (d3.select(".arg-reduced-graph").empty()) {
              enqueue("argWorker", {
                reducedGraph: true,
              }).then(
                (result) => argWorkerCallback(result),
                (error) => argWorkerErrorCallback(error)
              );
            } else {
              d3.selectAll(".arg-reduced-graph")
                .style("display", "inline-block")
                .style("visibility", "visible");
            }
          } else {
            if (!d3.select(".arg-error-graph").empty()) {
              d3.selectAll(".arg-error-graph").style("display", "none");
            }
            if (!d3.select(".arg-simplified-graph").empty()) {
              d3.selectAll(".arg-simplified-graph").style("display", "none");
            }
            if (!d3.select(".arg-reduced-graph").empty()) {
              d3.selectAll(".arg-reduced-graph").style("display", "none");
            }
            d3.selectAll(".arg-graph")
              .style("display", "inline-block")
              .style("visibility", "visible");
            $("#arg-container").scrollLeft(
              d3.select(".arg-svg").attr("width") / 4
            );
          }
        }
      };

      $scope.argZoomControl = function argZoomControl() {
        if ($scope.zoomEnabled) {
          $scope.zoomEnabled = false;
          d3.select("#arg-zoom-button").html("<i class='far fa-square'></i>");
          // revert zoom and remove listeners
          d3.selectAll(".arg-svg").each(
            /* @this HTMLElement */ function argSvg() {
              d3.select(this)
                .on("zoom", null)
                .on("wheel.zoom", null)
                .on("dblclick.zoom", null)
                .on("touchstart.zoom", null);
            }
          );
        } else {
          $scope.zoomEnabled = true;
          d3.select("#arg-zoom-button").html(
            "<i class='far fa-check-square'></i>"
          );
          d3.selectAll(".arg-svg").each(
            /* @this HTMLElement */ function argSvg() {
              const svg = d3.select(this);
              const svgGroup = d3.select(this.firstChild);
              const zoom = d3.zoom().on("zoom", () => {
                svgGroup.attr("transform", d3.event.transform);
              });
              svg.call(zoom);
              svg.on("dblclick.zoom", null).on("touchstart.zoom", null);
            }
          );
        }
      };

      $scope.argRedraw = () => {
        const input = $("#arg-split-threshold").val();
        if (!$scope.validateInput(input)) {
          alert("Invalid input!");
          return;
        }
        d3.selectAll(".arg-graph").remove();
        d3.selectAll(".arg-simplified-graph").remove();
        d3.selectAll(".arg-reduced-graph").remove();
        d3.selectAll(".arg-error-graph").remove();
        if ($scope.zoomEnabled) {
          $scope.argZoomControl();
        }
        let graphCount = Math.ceil(argJson.nodes.length / input);
        $("#arg-modal").text(`0/${graphCount}`);
        graphCount = null;
        $("#renderStateModal").modal("show");
        enqueue("argWorker", {
          toSplit: input,
        }).then(
          (result) => argWorkerCallback(result),
          (error) => argWorkerErrorCallback(error)
        );
        enqueue("argWorker", {
          renderer: "ready",
        }).then(
          (result) => argWorkerCallback(result),
          (error) => argWorkerErrorCallback(error)
        );
      };

      $scope.validateInput = (input) => {
        if (input % 1 !== 0) return false;
        if (input < 500 || input > 900) return false;
        return true;
      };
    },
  ]);

  app.controller("SourceController", [
    "$rootScope",
    "$scope",
    function sourceController($rootScope, $scope) {
      // available sourcefiles
      $scope.sourceFiles = sourceFiles;
      $scope.selectedSourceFile = 0;
      $scope.setSourceFile = (value) => {
        $scope.selectedSourceFile = value;
      };
      $scope.sourceFileIsSet = (value) => value === $scope.selectedSourceFile;
    },
  ]);

  app.controller("AboutController", [
    "$rootScope",
    "$scope",
    function aboutController($rootScope, $scope) {
      $scope.dependencies = dependencies;
      $scope.knownLicenses = [
        "0BSD",
        "Apache-2.0",
        "BSD-2-Clause",
        "BSD-3-Clause",
        "CC0-1.0",
        "CC-BY-3.0",
        "CC-BY-4.0",
        "ISC",
        "MIT",
        "OFL-1.1",
      ];
      $scope.linkifyLicenses = (licenses) =>
        licenses
          .split(/([A-Za-z0-9.-]+)/)
          .filter((license) => license)
          .map((s) =>
            $scope.knownLicenses.includes(s) ? $scope.linkifyLicense(s) : s
          );
      $scope.linkifyLicense = (license) =>
        `<a href="https://spdx.org/licenses/${license}" target="_blank" rel="noopener noreferrer">${license}</a>`;
    },
  ]);
})();

window.init = () => {
  // Calculate total count of graphs to display in modal
  let argTotalGraphCount;
  if (argJson.nodes) {
    argTotalGraphCount = Math.ceil(argJson.nodes.length / graphSplitThreshold);
    $("#arg-modal").text(`0/${argTotalGraphCount}`);
  } else {
    // No ARG data -> happens if the AbstractStates are not ARGStates
    $("#arg-modal").text("0/0");
    $("#set-tab-2").addClass("disabled-btn");
    argTabDisabled = true;
    angular.element(document).ready(() => {
      $("#set-tab-2")
        .parent()
        .attr(
          "data-original-title",
          "ARG not available for this configuration of CPAchecker"
        );
      $("#set-tab-2").attr("data-toggle", "");
    });
  }
  let cfaTotalGraphCount = 0;
  cfaJson.functionNames.forEach((f) => {
    const fNodes = cfaJson.nodes.filter((n) => n.func === f);
    cfaTotalGraphCount += Math.ceil(fNodes.length / graphSplitThreshold);
  });
  $("#cfa-modal").text(`0/${cfaTotalGraphCount}`);
  cfaTotalGraphCount = null;

  // Display modal window containing current rendering state
  $("#renderStateModal").modal("show");

  // Setup section widths accordingly
  if (errorPath === undefined) {
    d3.select("#errorpath_section").style("display", "none");
    $("#toggle_button_error_path").hide();
    $("#toggle_button_error_path_placeholder").hide();
  } else {
    d3.select("#externalFiles_section").style("width", "75%");
    d3.select("#cfa-toolbar").style("width", "auto");
  }

  // Initial postMessage to the CFA worker to trigger CFA graph(s) creation
  enqueue("cfaWorker", {
    json: JSON.stringify(cfaJson),
    cfaSplit,
    argTabDisabled,
  }).then(
    (result) => cfaWorkerCallback(result),
    (error) => cfaWorkerErrorCallback(error)
  );

  // ONLY if ARG data is available
  if (argJson.nodes) {
    // Initial postMessage to the ARG worker to trigger ARG graph(s) creation
    if (errorPath !== undefined) {
      enqueue("argWorker", {
        errorPath: JSON.stringify(errorPath),
      }).then(
        (result) => argWorkerCallback(result),
        (error) => argWorkerErrorCallback(error)
      );
    }
    enqueue("argWorker", {
      json: JSON.stringify(argJson),
    }).then(
      (result) => argWorkerCallback(result),
      (error) => argWorkerErrorCallback(error)
    );
  }
};
