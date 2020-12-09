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

import "@fortawesome/fontawesome-free/js/all.min";
import $ from "jquery";
import "angular";
import "bootstrap";
import "datatables.net";
import "code-prettify";
import "jquery-resizable-dom";
var d3 = require("d3");
var dagreD3 = require("dagre-d3");

const isDevEnv = process.env.NODE_ENV !== "production";

// Load example data into the window when using development mode
if (isDevEnv) {
  const data = require("devData");
	window.argJson = data.argJson;
	window.sourceFiles = data.sourceFiles;
	window.cfaJson = data.cfaJson;
}

var argJson = window.argJson;
var sourceFiles = window.sourceFiles;
var cfaJson = window.cfaJson;

const externalLibPathPrefix = document.location.protocol + "//" + document.location.host + "/external_libs/";
const externalLibs = [externalLibPathPrefix + "d3.min.js", externalLibPathPrefix + "dagre-d3.min.js"]

// CFA graph variable declarations
var functions = cfaJson.functionNames;
var functionCallEdges = cfaJson.functionCallEdges;
var errorPath;
if (cfaJson.hasOwnProperty("errorPath")) {
	errorPath = cfaJson.errorPath;
}
var relevantEdges;
if (argJson.hasOwnProperty("relevantedges")) {
        relevantEdges = argJson.relevantedges;
}
var reducedEdges;
if (argJson.hasOwnProperty("reducededges")) {
        reducedEdges = argJson.reducededges;
}

var graphSplitThreshold = 700;
var zoomEnabled = false;
var render = new dagreD3.render();
var margin = 20;
var cfaWorker, argWorker;
var cfaSplit = false;
var argTabDisabled = false;

(function () {
	$(function () {
		// initialize all popovers
		$('[data-toggle="popover"]').popover({
			html: true
		});
		// initialize all tooltips
		$("[data-toggle=tooltip]").tooltip();
		$(document).on('hover', '[data-toggle=tooltip]', function () {
			$(this).tooltip('show');
		});

		// hide tooltip after 5 seconds
		var timeout;
		$(document).on('shown.bs.tooltip', function (e) {
			if (timeout) {
				clearTimeout(timeout)
			}
			timeout = setTimeout(function () {
				$(e.target).tooltip('hide');
			}, 5000);
		});

		//Statistics table initialization
		$(document).ready(function () {
			$('#statistics_table').DataTable({
				"order": [],
				aLengthMenu: [
					[25, 50, 100, 200, -1],
					[25, 50, 100, 200, "All"]
				],
				iDisplayLength: -1, //Default display all entries
				"columnDefs": [{
						"orderable": false, //No ordering
						"targets": 0,
					}, {
						"orderable": false, //No Ordering
						"targets": 1,
					},
					{
						"orderable": false, //No ordering
						"targets": 2,
					},
				]
			});
		});

		// Initialize Google pretiffy code
		$(document).ready(function () {
			PR.prettyPrint();
		});

		//Configuration table initialization
		$(document).ready(function () {
			$('#config_table').DataTable({
				"order": [
					[1, "asc"]
				],
				aLengthMenu: [
					[25, 50, 100, 200, -1],
					[25, 50, 100, 200, "All"]
				],
				iDisplayLength: -1 //Default display all entries
			});
		});

		//Log table initialization
		$(document).ready(function () {
			$('#log_table').DataTable({
				"order": [
					[0, "asc"]
				],
				autoWidth: false,
				aoColumns: [{
						sWidth: '12%'
					},
					{
						sWidth: '10%'
					},
					{
						sWidth: '10%'
					},
					{
						sWidth: '25%'
					},
					{
						sWidth: '43%'
					},
				],
				aLengthMenu: [
					[25, 50, 100, 200, -1],
					[25, 50, 100, 200, "All"]
				],
				iDisplayLength: -1
			});
		});

		// Draggable divider between error path section and file section
		$(function resizeSection() {
			$("#errorpath_section").resizable({
				autoHide: true,
				handles: 'e',
				resize: function (e, ui) {
					var parent = ui.element.parent();
					//alert(parent.attr('class'));
					var remainingSpace = parent.width() - ui.element.outerWidth(),
						divTwo = ui.element.next(),
						divTwoWidth = (remainingSpace - (divTwo.outerWidth() - divTwo.width())) / parent.width() * 100 + "%";
					divTwo.width(divTwoWidth);
				},
				stop: function (e, ui) {
					var parent = ui.element.parent();
					ui.element.css({
						width: ui.element.width() / parent.width() * 100 + "%",
					});
				}
			});
		});
	});

	var app = angular.module('report', []);

	var reportController = app.controller('ReportController', ['$rootScope', '$scope',
		function ($rootScope, $scope) {
			$scope.logo = "https://cpachecker.sosy-lab.org/logo.svg";
			$scope.help_content = "<div class=\"container \" style=\"font-family: Arial\"><p><b>CFA</b> (Control Flow Automaton) shows the control flow of the program. <br> For each function in the source code one CFA graph is created. <br>" +
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
				"<p><b>In case of split graph (applies to both CFA and ARG)</b><br> -- doubleclick on labelless node to jump to target node<br> -- doubleclick on 'split edge' to jump to initial edge </p></div>";
			$scope.help_errorpath = "<div style=\"font-family: Arial\"><p>The errorpath leads to the error 'edge by edge' (CFA) or 'node by node' (ARG) or 'line by line' (Source)</p>" +
				"<p><b>-V- (Value Assignments)</b> Click to show all initialized variables and their values at that point in the programm.</p>" +
				"<p><b>Edge-Description (Source-Code-View)</b> Click to jump to the relating edge in the CFA / node in the ARG / line in Source (depending on active tab).\n If non of the mentioned tabs is currently set, the ARG tab will be selected.</p>" +
				"<p><b>Buttons (Prev, Start, Next)</b> Click to navigate through the errorpath and jump to the relating position in the active tab</p>" +
				"<p><b>Search</b>\n - You can search for words or numbers in the edge-descriptions (matches appear blue)\n" +
				"- You can search for value-assignments (variable names or their value) - it will highlight only where a variable has been initialized or where it has changed its value (matches appear green)\n" +
				"- An 'exact matches' search will look for a variable declarator matching exactly the provided text considering both, edge descriptions and value assignments</p></div>";
			$scope.tab = 1;
			$scope.$on("ChangeTab", function (event, tabIndex) {
				$scope.setTab(tabIndex);
			});

			//Toggle button to hide the error path section
			$scope.toggleErrorPathSection = function (e) {
				$('#toggle_error_path').on('change', function () {
					if ($(this).is(':checked')) {
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

			//Full screen mode function to view the report in full screen
			$('#full_screen_mode').click(function () {
				$(this).find('i').toggleClass('fa-compress fa-expand')
			});

			$scope.makeFullScreen = function () {
				if ((document.fullScreenElement && document.fullScreenElement !== null) || (!document.mozFullScreen && !
						document.webkitIsFullScreen)) {
					if (document.documentElement.requestFullScreen) {
						document.documentElement.requestFullScreen();
					} else if (document.documentElement.mozRequestFullScreen) {
						document.documentElement.mozRequestFullScreen();
					} else if (document.documentElement.webkitRequestFullScreen) {
						document.documentElement.webkitRequestFullScreen(Element.ALLOW_KEYBOARD_INPUT);
					}
				} else {
					if (document.cancelFullScreen) {
						document.cancelFullScreen();
					} else if (document.mozCancelFullScreen) {
						document.mozCancelFullScreen();
					} else if (document.webkitCancelFullScreen) {
						document.webkitCancelFullScreen();
					}
				}
			};

			$scope.setTab = function (tabIndex) {
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
							var boundingRect = d3.select(".arg-node").node().getBoundingClientRect();
							$("#arg-container").scrollTop(boundingRect.top + $("#arg-container").scrollTop() - 300).scrollLeft(boundingRect.left + $("#arg-container").scrollLeft() - 500);
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
			$scope.tabIsSet = function (tabIndex) {
				return $scope.tab === tabIndex;
			};

			$scope.getTabSet = function () {
				return $scope.tab;
			};
		}
	]);

	var errorpathController = app.controller("ErrorpathController", ['$rootScope', '$scope', function ($rootScope, $scope) {
		$rootScope.errorPath = [];

		//Fault Localization
		function getLinesOfFault(fault) {
			var lines = {};
			for (var i = 0; i < fault["errPathIds"].length; i++) {
				var errorPathIdx = fault["errPathIds"][i];
				var errorPathElem = $rootScope.errorPath[errorPathIdx];
				var line = {"line": errorPathElem["line"], "desc": errorPathElem["desc"]};
				var line_key = line["line"] + line["desc"];
				lines[line_key] = line;
			}
			return Object.values(lines);
		};

		function getValues(val, prevValDict) {
			var values = {};
			if (val != "") {
				var singleStatements = val.split("\n");
				for (var i = 0; i < singleStatements.length - 1; i++) {
					if (!Object.keys(prevValDict).includes(singleStatements[i].split("==")[0].trim())) {
						// previous dictionary does not include the statement
						values[singleStatements[i].split("==")[0].trim()] = singleStatements[i].split("==")[1].trim().slice(0, -1);
					} else if (prevValDict[singleStatements[i].split("==")[0].trim()] !== singleStatements[i].split("==")[1].trim().slice(0, -1)) {
						// statement is included but with different value
						values[singleStatements[i].split("==")[0].trim()] = singleStatements[i].split("==")[1].trim().slice(0, -1);
					}
				}
			}
			return values;
		};

		// initialize array that stores the important edges. Index counts only, when edges appear in the report.
		var importantEdges = [];
		var importantIndex = -1;
		var faultEdges = [];
		if (errorPath !== undefined) {
			var indentationlevel = 0;
			for (var i = 0; i < errorPath.length; i++) {
				var errPathElem = errorPath[i];

				// do not show start, return and blank edges
				if (errPathElem.desc.indexOf("Return edge from") === -1 && errPathElem.desc != "Function start dummy edge" && errPathElem.desc != "") {
					importantIndex += 1;
					var previousValueDictionary = {};
					errPathElem["valDict"] = {};
					errPathElem["valString"] = "";
					if (i > 0) {
						$.extend(errPathElem.valDict, $rootScope.errorPath[$rootScope.errorPath.length - 1].valDict);
						previousValueDictionary = $rootScope.errorPath[$rootScope.errorPath.length - 1].valDict;
					}
					var newValues = getValues(errPathElem.val, previousValueDictionary);
					errPathElem["newValDict"] = newValues;
					if (!$.isEmptyObject(newValues)) {
						$.extend(errPathElem.valDict, newValues);
					}
					for (var key in errPathElem.valDict) {
						errPathElem.valString += key + ":  " + errPathElem.valDict[key] + "\n";
					}
					// add indentation
					for (var j = 1; j <= indentationlevel; j++) {
						errPathElem.desc = "   " + errPathElem.desc;
					}
					$rootScope.errorPath.push(errPathElem);
				} else if (errPathElem.desc.indexOf("Return edge from") !== -1) {
					indentationlevel -= 1;
				} else if (errPathElem.desc.indexOf("Function start dummy") !== -1) {
					indentationlevel += 1;
				}

				if (errPathElem.faults !== undefined && errPathElem.faults.length > 0) {
					errPathElem["importantindex"] = importantIndex;
					errPathElem["bestrank"] = cfaJson.faults[errPathElem.faults[0]].rank;
					errPathElem["bestreason"] = cfaJson.faults[errPathElem.faults[0]].reason;
					if (errPathElem["additional"] !== undefined && errPathElem["additional"] !== "") {
						errPathElem["bestreason"] = errPathElem["bestreason"] + errPathElem["additional"];
					}
					faultEdges.push(errPathElem);
				}

				// store the important edges
                                if(errPathElem.importance == 1){
                                      importantEdges.push(importantIndex);
                                   }
			}

			function addFaultLocalizationInfo(){
				if (faultEdges !== undefined && faultEdges.length > 0) {
					for (var j = 0; j < faultEdges.length; j++) {
						d3.selectAll("#errpath-" + faultEdges[j].importantindex + " td pre").classed("fault", true);
					}
					d3.selectAll("#errpath-header td pre").classed("tableheader", true);
				}
			};

                        // this function puts the important edges into a CSS class that highlights them
			function highlightEdges(impEdges){
			    for (var j = 0; j < impEdges.length; j++){
			        d3.selectAll("#errpath-" + impEdges[j] + " td pre").classed("important", true);
			    }
                        };

                        angular.element(document).ready(function(){
				highlightEdges(importantEdges);
				addFaultLocalizationInfo();
                        });


		}

		// make faults visible to angular
		$rootScope.faults = [];
		$rootScope.precondition = cfaJson.precondition === undefined ? "" : cfaJson.precondition["fl-precondition"];
		$rootScope.hasPrecondition = $rootScope.precondition !== "";
		if (cfaJson.faults !== undefined) {
			for (var i = 0; i < cfaJson.faults.length; i++) {
				var fault = cfaJson.faults[i];
				var fInfo = Object.assign({}, fault);
				// store all error-path elements related to this fault.
				// we can't do  this in the Java backend because
				// we can't be sure to have the full error-path elements in the FaultLocalizationInfo
				// when the faults-code is generated.
				fInfo["errPathIds"] = [];
				for (var j = 0; j < $rootScope.errorPath.length; j++) {
					var element = $rootScope.errorPath[j];
					if (element.faults.includes(i)) {
						fInfo["errPathIds"].push(j);
						fInfo["valDict"] = element.valDict;
					}
					fInfo["lines"] = getLinesOfFault(fInfo);
				}
				$rootScope.faults.push(fInfo);
			}
		}


		$scope.hideFaults = ($rootScope.faults == undefined || $rootScope.faults.length == 0);

		$scope.faultClicked = function(){
			$scope.hideErrorTable = !$scope.hideErrorTable;
		};

		$scope.clickedFaultLocElement = function ($event) {
			d3.selectAll(".clickedFaultLocElement").classed("clickedFaultLocElement", false);
			var clickedElement = d3.select($event.currentTarget);
			clickedElement.classed("clickedFaultLocElement", true);
			var faultElementIdx = clickedElement.attr("id").substring("fault-".length);
			var faultElement = $rootScope.faults[faultElementIdx];
			markErrorPathElementInTab(faultElement.errPathIds);
		};

		$scope.errPathPrevClicked = function ($event) {
			var selection = d3.select("tr.clickedErrPathElement");
			if (!selection.empty()) {
				var prevId = parseInt(selection.attr("id").substring("errpath-".length)) - 1;
				selection.classed("clickedErrPathElement", false);
				d3.select("#errpath-" + prevId).classed("clickedErrPathElement", true);
				$("#value-assignment").scrollTop($("#value-assignment").scrollTop() - 18);
				markErrorPathElementInTab(prevId);
			}
		};

		$scope.errPathStartClicked = function () {
			d3.select("tr.clickedErrPathElement").classed("clickedErrPathElement", false);
			d3.select("#errpath-0").classed("clickedErrPathElement", true);
			$("#value-assignment").scrollTop(0);
			markErrorPathElementInTab(0);
		};

		$scope.errPathNextClicked = function ($event) {
			var selection = d3.select("tr.clickedErrPathElement");
			if (!selection.empty()) {
				var nextId = parseInt(selection.attr("id").substring("errpath-".length)) + 1;
				selection.classed("clickedErrPathElement", false);
				d3.select("#errpath-" + nextId).classed("clickedErrPathElement", true);
				$("#value-assignment").scrollTop($("#value-assignment").scrollTop() + 18);
				markErrorPathElementInTab(nextId);
			}
		};

		$scope.clickedErrpathElement = function ($event) {
			d3.select("tr.clickedErrPathElement").classed("clickedErrPathElement", false);
			var clickedElement = d3.select($event.currentTarget.parentNode);
			clickedElement.classed("clickedErrPathElement", true);
			markErrorPathElementInTab(clickedElement.attr("id").substring("errpath-".length));
		};

		function markErrorPathElementInTab(selectedErrPathElemId) {
			var currentTab = angular.element($("#report-controller")).scope().getTabSet();
			if (!Array.isArray(selectedErrPathElemId)) {
				selectedErrPathElemId = [selectedErrPathElemId];
			}
			unmarkEverything();
			for (var i = 0; i < selectedErrPathElemId.length; i++) {
				var id = selectedErrPathElemId[i];
				if ($rootScope.errorPath[id] === undefined) {
					return;
				}
				handleErrorPathElemClick(currentTab, id);
			}
		}

		function handleErrorPathElemClick(currentTab, errPathElemIndex) {
			markCfaEdge($rootScope.errorPath[errPathElemIndex]);
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
			].forEach(function (c) { d3.selectAll("." + c).classed(c, false) });
		}


		function markCfaEdge(errPathEntry) {
			var actualSourceAndTarget = getActualSourceAndTarget(errPathEntry);
			if ($.isEmptyObject(actualSourceAndTarget)) return;
			if (actualSourceAndTarget.target === undefined) {
				var selection = d3.select("#cfa-node" + actualSourceAndTarget.source);
				selection.classed("marked-cfa-node", true);
				var boundingRect = selection.node().getBoundingClientRect();
				$("#cfa-container").scrollTop(boundingRect.top + $("#cfa-container").scrollTop() - 300).scrollLeft(boundingRect.left - d3.select("#cfa-container").style("width").split("px")[0] - $("#cfa-container"));
				if (actualSourceAndTarget.source in cfaJson.combinedNodes) {
					selection.selectAll("tspan").each(function (d, i) {
						if (d3.select(this).html().includes(errPathEntry.source)) {
							d3.select(this).classed("marked-cfa-node-label", true);
						}
					});
				}
				return;
			}
			var selection = d3.select("#cfa-edge_" + actualSourceAndTarget.source + "-" + actualSourceAndTarget.target);
			selection.classed("marked-cfa-edge", true);
			var boundingRect = selection.node().getBoundingClientRect();
			$("#cfa-container").scrollTop(boundingRect.top + $("#cfa-container").scrollTop() - 300).scrollLeft(boundingRect.left - d3.select("#cfa-container").style("width").split("px")[0] - $("#cfa-container"));
		}

		function getActualSourceAndTarget(element) {
			var result = {};
			if (cfaJson.mergedNodes.includes(element.source) && cfaJson.mergedNodes.includes(element.target)) {
				result["source"] = getMergingNode(element.source);
				return result;
			}
			if (element.source in cfaJson.combinedNodes) {
				result["source"] = element.source;
				return result;
			}
			if (!cfaJson.mergedNodes.includes(element.source) && !cfaJson.mergedNodes.includes(element.target)) {
				result["source"] = element.source;
				result["target"] = element.target;
			} else if (!cfaJson.mergedNodes.includes(element.source) && cfaJson.mergedNodes.includes(element.target)) {
				result["source"] = element.source;
				result["target"] = getMergingNode(element.target);
			} else if (cfaJson.mergedNodes.includes(element.source) && !cfaJson.mergedNodes.includes(element.target)) {
				result["source"] = getMergingNode(element.source);
				result["target"] = element.target;
			}
			if (Object.keys(cfaJson.functionCallEdges).includes("" + result["source"])) {
				result["target"] = cfaJson.functionCallEdges["" + result["source"]][0];
			}
			// Ensure empty object is returned if source = target (edge non existent)
			if (result["source"] === result["target"]) {
				delete result["source"];
				delete result["target"];
			}
			return result;
		}

		// Retrieve the node in which this node was merged
		function getMergingNode(index) {
			var result = "";
			Object.keys(cfaJson.combinedNodes).some(function (key) {
				if (cfaJson.combinedNodes[key].includes(index)) {
					result = key;
					return result;
				}
			})
			return result;
		}

		function markArgNode(errPathEntry) {
			if (errPathEntry.argelem === undefined) {
				return;
			}
			var idToSelect;
			if (d3.select("#arg-graph0").style("display") !== "none")
				idToSelect = "#arg-node";
			else
				idToSelect = "#arg-error-node";
			var selection = d3.select(idToSelect + errPathEntry.argelem);
			selection.classed("marked-arg-node", true);
			var boundingRect = selection.node().getBoundingClientRect();
			$("#arg-container").scrollTop(boundingRect.top + $("#arg-container").scrollTop() - 300).scrollLeft(boundingRect.left - d3.select("#arg-container").style("width").split("px")[0] - $("#arg-container"));
		}

		function markSourceLine(errPathEntry) {
			if (errPathEntry.line === 0) {
				errPathEntry.line = 1;
			}
			var selection = d3.select("#source-" + errPathEntry.line + " td pre.prettyprint");
			selection.classed("marked-source-line", true);
			$(".sourceContent").scrollTop(selection.node().getBoundingClientRect().top + $(".sourceContent").scrollTop() - 200);
		}

	}]);

	var searchController = app.controller("SearchController", ['$rootScope', '$scope', function ($rootScope, $scope) {
		$scope.numOfValueMatches = 0;
		$scope.numOfDescriptionMatches = 0;

		$scope.checkIfEnter = function ($event) {
			if ($event.keyCode == 13) {
				$scope.searchFor();
			}
		};

		$scope.searchFor = function () {
			$scope.numOfValueMatches = 0;
			$scope.numOfDescriptionMatches = 0;
			if (d3.select("#matches").style("display") === "none") {
				d3.select("#matches").style("display", "inline");
			}
			d3.selectAll(".markedValueDescElement").classed("markedValueDescElement", false);
			d3.selectAll(".markedValueElement").classed("markedValueElement", false);
			d3.selectAll(".markedDescElement").classed("markedDescElement", false);
			var searchInput = $(".search-input").val().trim();
			if (searchInput != "") {
				if ($("#optionExactMatch").prop("checked")) {
					$rootScope.errorPath.forEach(function (it, i) {
						var exactMatchInValues = searchInValues(it.newValDict, searchInput, true);
						if (exactMatchInValues && searchInDescription(it.desc.trim(), searchInput)) {
							$scope.numOfValueMatches++;
							$scope.numOfDescriptionMatches++;
							$("#errpath-" + i + " td")[1].classList.add("markedValueDescElement");
						} else if (exactMatchInValues) {
							$scope.numOfValueMatches++;
							$("#errpath-" + i + " td")[1].classList.add("markedValueElement");
						} else if (searchInDescription(it.desc.trim(), searchInput)) {
							$scope.numOfDescriptionMatches++;
							$("#errpath-" + i + " td")[1].classList.add("markedDescElement");
						}
					})
				} else {
					$rootScope.errorPath.forEach(function (it, i) {
						var matchInValues = searchInValues(it.newValDict, searchInput, false);
						if (matchInValues && it.desc.indexOf(searchInput) !== -1) {
							$scope.numOfValueMatches++;
							$scope.numOfDescriptionMatches++;
							$("#errpath-" + i + " td")[1].classList.add("markedValueDescElement");
						} else if (matchInValues) {
							$scope.numOfValueMatches++;
							$("#errpath-" + i + " td")[1].classList.add("markedValueElement");
						} else if (it.desc.indexOf(searchInput) !== -1) {
							$scope.numOfDescriptionMatches++;
							$("#errpath-" + i + " td")[1].classList.add("markedDescElement");
						}
					})
				}
			}
		};

		// Search for input in the description by using only words. A word is defined by a-zA-Z0-9 and underscore
		function searchInDescription(desc, searchInput) {
			var descStatements = desc.split(" ");
			for (var i = 0; i < descStatements.length; i++) {
				if (descStatements[i].replace(/[^\w.]/g, "") === searchInput) {
					return true;
				}
			}
			return false;
		}

		// Search for input in object, either exact match or a match containing the input
		function searchInValues(values, searchInput, exact) {
			if ($.isEmptyObject(values)) return false;
			var match;
			if (exact) {
				match = Object.keys(values).find(function (v) {
					return v === searchInput;
				})
			} else {
				match = Object.keys(values).find(function (v) {
					return v.indexOf(searchInput) !== -1;
				})
			}
			if (match) return true;
			else return false;
		}
	}]);

	var valueAssignmentsController = app.controller("ValueAssignmentsController", ['$rootScope', '$sce', '$scope', function ($rootScope, $sce, $scope) {
		$scope.showValues = function ($event) {
			var element = $event.currentTarget;
			if (element.classList.contains("markedTableElement")) {
				element.classList.remove("markedTableElement");
			} else {
				element.classList.add("markedTableElement");
			}
		};

		$scope.htmlTrusted = function(html) {
			return $sce.trustAsHtml(html);
		};
	}]);

	var cfaToolbarController = app.controller('CFAToolbarController', ['$scope',
		function ($scope) {
			if (functions) {
				if (functions.length > 1) {
					$scope.functions = ["all"].concat(functions);
				} else {
					$scope.functions = functions;
				}
				$scope.selectedCFAFunction = $scope.functions[0];
				$scope.zoomEnabled = false;
			}


			$scope.setCFAFunction = function () {
				if ($scope.zoomEnabled) {
					$scope.zoomControl();
				}
				// FIXME: two-way binding does not update the selected option
				d3.selectAll("#cfa-toolbar option").attr("selected", null).attr("disabled", null);
				d3.select("#cfa-toolbar [label=" + $scope.selectedCFAFunction + "]").attr("selected", "selected").attr("disabled", true);
				if ($scope.selectedCFAFunction === "all") {
					functions.forEach(function (func) {
						d3.selectAll(".cfa-svg-" + func).attr("display", "inline-block");
					});
				} else {
					var funcToHide = $scope.functions.filter(function (it) {
						return it !== $scope.selectedCFAFunction && it !== "all";
					});
					funcToHide.forEach(function (func) {
						d3.selectAll(".cfa-svg-" + func).attr("display", "none");
					});
					d3.selectAll(".cfa-svg-" + $scope.selectedCFAFunction).attr("display", "inline-block");
				}
				var firstElRect = d3.select("[display=inline-block] .cfa-node:nth-child(2)").node().getBoundingClientRect();
				if (d3.select("#errorpath_section").style("display") !== "none") {
					$("#cfa-container").scrollTop(firstElRect.top + $("#cfa-container").scrollTop() - 300).scrollLeft(firstElRect.left - $("#cfa-container").scrollLeft() - d3.select("#externalFiles_section").style("width"));
				} else {
					$("#cfa-container").scrollTop(firstElRect.top + $("#cfa-container").scrollTop() - 300).scrollLeft(firstElRect.left - $("#cfa-container"));
				}
			};

			$scope.cfaFunctionIsSet = function (value) {
				return value === $scope.selectedCFAFunction;
			};

			$scope.zoomControl = function () {
				if ($scope.zoomEnabled) {
					$scope.zoomEnabled = false;
					d3.select("#cfa-zoom-button").html("<i class='far fa-square'></i>");
					// revert zoom and remove listeners
					d3.selectAll(".cfa-svg").each(function (d, i) {
						d3.select(this).on("zoom", null).on("wheel.zoom", null).on("dblclick.zoom", null).on("touchstart.zoom", null);
					});
				} else {
					$scope.zoomEnabled = true;
					d3.select("#cfa-zoom-button").html("<i class='far fa-check-square'></i>");
					d3.selectAll(".cfa-svg").each(function (d, i) {
						var svg = d3.select(this),
							svgGroup = d3.select(this.firstChild);
						var zoom = d3.zoom().on("zoom", function (d, i) {
							svgGroup.attr("transform", d.transform);
						});
						svg.call(zoom);
						svg.on("dblclick.zoom", null).on("touchstart.zoom", null);
					});
				}
			};

			$scope.redraw = function () {
				var input = $("#cfa-split-threshold").val();
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
				var graphCount = 0;
				cfaJson.functionNames.forEach(function (f) {
					var fNodes = cfaJson.nodes.filter(function (n) {
						return n.func === f;
					})
					graphCount += Math.ceil(fNodes.length / input);
				});
				$("#cfa-modal").text("0/" + graphCount);
				graphCount = null;
				$("#renderStateModal").modal("show");
				if (cfaWorker === undefined) {
					cfaWorker = new Worker(URL.createObjectURL(new Blob(["(" + cfaWorker_function + ")()"], {
						type: "text/javascript"
					})));
          if (isDevEnv) {
            cfaWorker.postMessage({"externalLibPaths": externalLibs});
          }
				}
				cfaWorker.postMessage({
					"split": input
				});
				cfaWorker.postMessage({
					"renderer": "ready"
				});
			};

			$scope.validateInput = function (input) {
				if (input % 1 !== 0) return false;
				if (input < 500 || input > 900) return false;
				return true;
			}
		}
	]);

	var argToolbarController = app.controller('ARGToolbarController', ['$rootScope', '$scope',
		function ($rootScope, $scope) {
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

			$scope.displayARG = function () {
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
							argWorker.postMessage({
								"errorGraph": true
							});
						} else {
							d3.selectAll(".arg-error-graph").style("display", "inline-block").style("visibility", "visible");
						}
					} else if ($rootScope.displayedARG.indexOf("simplified") !== -1) {
					        d3.selectAll(".arg-graph").style("display", "none");
					        d3.selectAll(".arg-reduced-graph").style("display", "none");
					        $("#arg-container").scrollTop(0).scrollLeft(0);
					        if (!d3.select(".arg-error-graph").empty()) {
                                                	d3.selectAll(".arg-error-graph").style("display", "none");
                                                }
                                                if (d3.select(".arg-simplified-graph").empty()) {
                                                	argWorker.postMessage({
                                                	      "simplifiedGraph": true
                                                	});
                                                } else {
                                                        d3.selectAll(".arg-simplified-graph").style("display", "inline-block").style("visibility", "visible");
                                                }
					} else if ($rootScope.displayedARG.indexOf("witness") !== -1) {
						d3.selectAll(".arg-graph").style("display", "none");
						d3.selectAll(".arg-simplified-graph").style("display", "none");
						$("#arg-container").scrollTop(0).scrollLeft(0);
						if (!d3.select(".arg-error-graph").empty()) {
							d3.selectAll(".arg-error-graph").style("display", "none");
						}
						if (d3.select(".arg-reduced-graph").empty()) {
							argWorker.postMessage({
								"reducedGraph": true
							});
						} else {
							d3.selectAll(".arg-reduced-graph").style("display", "inline-block").style("visibility", "visible");
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
						d3.selectAll(".arg-graph").style("display", "inline-block").style("visibility", "visible");
						$("#arg-container").scrollLeft(d3.select(".arg-svg").attr("width") / 4);
					}
				}
			};

			$scope.argZoomControl = function () {
				if ($scope.zoomEnabled) {
					$scope.zoomEnabled = false;
					d3.select("#arg-zoom-button").html("<i class='far fa-square'></i>");
					// revert zoom and remove listeners
					d3.selectAll(".arg-svg").each(function (d, i) {
						d3.select(this).on("zoom", null).on("wheel.zoom", null).on("dblclick.zoom", null).on("touchstart.zoom", null);
					});
				} else {
					$scope.zoomEnabled = true;
					d3.select("#arg-zoom-button").html("<i class='far fa-check-square'></i>");
					d3.selectAll(".arg-svg").each(function (d, i) {
						var svg = d3.select(this),
							svgGroup = d3.select(this.firstChild);
						var zoom = d3.zoom().on("zoom", function () {
							svgGroup.attr("transform", d.transform);
						});
						svg.call(zoom);
						svg.on("dblclick.zoom", null).on("touchstart.zoom", null);
					});
				}
			};

			$scope.argRedraw = function () {
				var input = $("#arg-split-threshold").val();
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
				var graphCount = Math.ceil(argJson.nodes.length / input);
				$("#arg-modal").text("0/" + graphCount);
				graphCount = null;
				$("#renderStateModal").modal("show");
				if (argWorker === undefined) {
					argWorker = new Worker(URL.createObjectURL(new Blob(["(" + argWorker_function + ")()"], {
						type: "text/javascript"
					})));
          if (isDevEnv) {
            argWorker.postMessage({"externalLibPaths": externalLibs});
          }
				}
				argWorker.postMessage({
					"split": input
				});
				argWorker.postMessage({
					"renderer": "ready"
				});
			};

			$scope.validateInput = function (input) {
				if (input % 1 !== 0) return false;
				if (input < 500 || input > 900) return false;
				return true;
			};
		}
	]);

	var sourceController = app.controller('SourceController', ['$rootScope', '$scope', '$location',
		'$anchorScroll',
		function ($rootScope, $scope, $location, $anchorScroll) {
			// available sourcefiles
			$scope.sourceFiles = sourceFiles;
			$scope.selectedSourceFile = 0;
			$scope.setSourceFile = function (value) {
				$scope.selectedSourceFile = value;
			};
			$scope.sourceFileIsSet = function (value) {
				return value === $scope.selectedSourceFile;
			};
		}
	]);

})();

// CFA graph variable declarations
var functions = cfaJson.functionNames;
var functionCallEdges = cfaJson.functionCallEdges;
var errorPath;
if (cfaJson.hasOwnProperty("errorPath")) {
	errorPath = cfaJson.errorPath;
}
var relevantEdges;
if (argJson.hasOwnProperty("relevantedges")) {
        relevantEdges = argJson.relevantedges;
}
function init() {
	// Calculate total count of graphs to display in modal
	var argTotalGraphCount;
	if (argJson.nodes) {
		argTotalGraphCount = Math.ceil(argJson.nodes.length / graphSplitThreshold);
		$("#arg-modal").text("0/" + argTotalGraphCount);
	} else { // No ARG data -> happens if the AbstractStates are not ARGStates
		$("#arg-modal").text("0/0");
		$("#set-tab-2").addClass("disabled-btn");
		argTabDisabled = true;
		angular.element(document).ready(function(){
                  $("#set-tab-2").parent().attr("data-original-title", "ARG not available for this configuration of CPAchecker");
                  $("#set-tab-2").attr("data-toggle", "");
                });
	}
	var cfaTotalGraphCount = 0;
	cfaJson.functionNames.forEach(function (f) {
		var fNodes = cfaJson.nodes.filter(function (n) {
			return n.func === f;
		})
		cfaTotalGraphCount += Math.ceil(fNodes.length / graphSplitThreshold);
	});
	$("#cfa-modal").text("0/" + cfaTotalGraphCount);
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

	// ======================= Create CFA and ARG Worker Listeners =======================
	/**
	 * Create workers using blobs due to Chrome's default security policy and
	 * the need of having a single file at the end that can be send i.e. via e-mail
	 */
	cfaWorker = new Worker(URL.createObjectURL(new Blob(["(" + cfaWorker_function + ")()"], {
		type: 'text/javascript'
	})));
  if (isDevEnv) {
    cfaWorker.postMessage({"externalLibPaths": externalLibs});
  }
	if (argJson.nodes) {
		argWorker = new Worker(URL.createObjectURL(new Blob(["(" + argWorker_function + ")()"], {
			type: "text/javascript"
		})));
    if (isDevEnv) {
      argWorker.postMessage({"externalLibPaths": externalLibs});
    }
	}

	cfaWorker.addEventListener("message", function (m) {
		if (m.data.graph !== undefined) {
			// id was already processed
			if (!d3.select("#cfa-graph-" + m.data.id).empty()) {
				cfaWorker.postMessage({
					"renderer": "ready"
				});
				return;
			}
			var id = m.data.id;
			d3.select("#cfa-container").append("div").attr("id", "cfa-graph-" + id).attr("class", "cfa-graph");
			var g = createGraph();
			g = Object.assign(g, JSON.parse(m.data.graph));
			var svg = d3.select("#cfa-graph-" + id).append("svg").attr("id", "cfa-svg-" + id).attr("class", "cfa-svg " + "cfa-svg-" + m.data.func);
			var svgGroup = svg.append("g");
			render(d3.select("#cfa-svg-" + id + " g"), g);
			// Center the graph - calculate svg.attributes
			svg.attr("height", g.graph().height + margin * 2);
			svg.attr("width", g.graph().width + margin * 10);
			svgGroup.attr("transform", "translate(" + margin * 2 + ", " + margin + ")");
			$("#cfa-modal").text(parseInt($("#cfa-modal").text().split("/")[0]) + 1 + "/" + $("#cfa-modal").text().split("/")[1]);
			cfaWorker.postMessage({
				"renderer": "ready"
			});
		} else if (m.data.status !== undefined) {
			addEventsToCfa();
			d3.select("#cfa-toolbar").style("visibility", "visible");
			d3.select("#cfa-container").classed("cfa-content", true);
			d3.selectAll(".cfa-svg").each(function (d, i) {
				d3.select(this).attr("width", Math.max(d3.select(this).attr("width"), d3.select(this.parentNode).style("width").split("px")[0]));
			});
			d3.selectAll(".cfa-graph").style("visibility", "visible");
			if (cfaSplit) {
				$("#renderStateModal").hide();
				$('.modal-backdrop').hide();
			} else {
				if (!argTabDisabled) {
					argWorker.postMessage({
						"renderer": "ready"
					});
				} else {
					$("#renderStateModal").hide();
					$('.modal-backdrop').hide();
				}
			}
		}
	}, false);

	cfaWorker.addEventListener("error", function (e) {
		alert("CFA Worker failed in line " + e.lineno + " with message " + e.message)
	}, false);

	// Initial postMessage to the CFA worker to trigger CFA graph(s) creation
	cfaWorker.postMessage({
		"json": JSON.stringify(cfaJson)
	});

	// ONLY if ARG data is available
	if (argJson.nodes) {
		argWorker.addEventListener('message', function (m) {
			if (m.data.graph !== undefined) {
				var id = "arg-graph" + m.data.id;
				var argClass = "arg-graph";
				if (m.data.errorGraph !== undefined) {
					id = "arg-error-graph" + m.data.id;
					argClass = "arg-error-graph";
					d3.select("#arg-modal-error").style("display", "inline");
					$("#renderStateModal").modal("show");
				}
				if(m.data.simplifiedGraph !== undefined) {
				        id = "arg-simplified-graph" + m.data.id;
				        argClass = "arg-simplified-graph";
				}
				if(m.data.reducedGraph !== undefined) {
					id = "arg-reduced-graph" + m.data.id;
					argClass = "arg-reduced-graph";
				}
				var g = createGraph();
				g = Object.assign(g, JSON.parse(m.data.graph));
				d3.select("#arg-container").append("div").attr("id", id).attr("class", argClass);
				var svg = d3.select("#" + id).append("svg").attr("id", "arg-svg" + id).attr("class", "arg-svg");
				var svgGroup = svg.append("g");
				render(d3.select("#arg-svg" + id + " g"), g);
				// Center the graph - calculate svg.attributes
				svg.attr("height", g.graph().height + margin * 2);
				svg.attr("width", g.graph().width + margin * 10);
				svgGroup.attr("transform", "translate(" + margin * 2 + ", " + margin + ")");
				// FIXME: until https://github.com/cpettitt/dagre-d3/issues/169 is not resolved, label centering like so:
				d3.selectAll(".arg-node tspan").each(function (d, i) {
					var transformation = d3.select(this.parentNode.parentNode).attr("transform")
					d3.select(this).attr("dx", Math.abs(getTransformation(transformation).translateX));
				});
				if (m.data.errorGraph !== undefined) {
					addEventsToArg();
					$("#renderStateModal").hide();
					$('.modal-backdrop').hide();
					argWorker.postMessage({
						"errorGraph": true
					});
				} else {
					$("#arg-modal").text(parseInt($("#arg-modal").text().split("/")[0]) + 1 + "/" + $("#arg-modal").text().split("/")[1]);
					argWorker.postMessage({
						"renderer": "ready"
					});
				}
			} else if (m.data.status !== undefined) {
				if (angular.element($("#report-controller")).scope().getTabSet() === 2) {
					d3.select("#arg-toolbar").style("visibility", "visible");
					d3.select("#arg-container").classed("arg-content", true);
					d3.selectAll(".arg-graph").style("visibility", "visible");
					$("#arg-container").scrollLeft(d3.select(".arg-svg").attr("width") / 4);
				}
				addEventsToArg();
				if (errorPath !== undefined) {
					d3.selectAll("td.disabled").classed("disabled", false);
					if (!d3.select(".make-pretty").classed("prettyprint")) {
						d3.selectAll(".make-pretty").classed("prettyprint", true);
						PR.prettyPrint();
					}
				}
				$("#renderStateModal").hide();
				$('.modal-backdrop').hide();
			}
		}, false);

		argWorker.addEventListener("error", function (e) {
			alert("ARG Worker failed in line " + e.lineno + " with message " + e.message)
		}, false);

		// Initial postMessage to the ARG worker to trigger ARG graph(s) creation
		if (errorPath !== undefined) {
			argWorker.postMessage({
				"errorPath": JSON.stringify(errorPath)
			});
		}
		argWorker.postMessage({
			"json": JSON.stringify(argJson)
		});
	}

	// Function to get transfromation thorugh translate as in new version of D3.js d3.transfrom is removed
	function getTransformation(transform) {
		// Create a dummy g for calculation purposes only. This will never
		// be appended to the DOM and will be discarded once this function
		// returns.
		var g = document.createElementNS("http://www.w3.org/2000/svg", "g");

		// Set the transform attribute to the provided string value.
		g.setAttributeNS(null, "transform", transform);

		// consolidate the SVGTransformList containing all transformations
		// to a single SVGTransform of type SVG_TRANSFORM_MATRIX and get
		// its SVGMatrix.
		var matrix = g.transform.baseVal.consolidate().matrix;

		// Below calculations are taken and adapted from the private function
		// transform/decompose.js of D3's module d3-interpolate.
		// var {
		// 	a,
		// 	b,
		// 	c,
		// 	d,
		// 	e,
		// 	f
		// } = matrix; // ES6, if this doesn't work, use below assignment
		var a = matrix.a,
			b = matrix.b,
			c = matrix.c,
			d = matrix.d,
			e = matrix.e,
			f = matrix.f; // ES5
		var scaleX, scaleY, skewX;
		if (scaleX = Math.sqrt(a * a + b * b)) a /= scaleX, b /= scaleX;
		if (skewX = a * c + b * d) c -= a * skewX, d -= b * skewX;
		if (scaleY = Math.sqrt(c * c + d * d)) c /= scaleY, d /= scaleY, skewX /= scaleY;
		if (a * d < b * c) a = -a, b = -b, skewX = -skewX, scaleX = -scaleX;
		return {
			translateX: e,
			translateY: f,
			rotate: Math.atan2(b, a) * 180 / Math.PI,
			skewX: Math.atan(skewX) * 180 / Math.PI,
			scaleX: scaleX,
			scaleY: scaleY
		};
	}

	// create and return a graph element with a set transition
	function createGraph() {
		var g = new dagreD3.graphlib.Graph().setGraph({}).setDefaultEdgeLabel(
			function () {
				return {};
			});
		return g;
	}

	// Retrieve the node in which this node was merged - used for the node events
	// FIXME: this is a duplicate function already contained in error path controller, currently no better way to call it
	function getMergingNode(index) {
		var result = "";
		Object.keys(cfaJson.combinedNodes).some(function (key) {
			if (cfaJson.combinedNodes[key].includes(index)) {
				result = key;
				return result;
			}
		})
		return result;
	}

	// Add desired events to CFA nodes and edges
	function addEventsToCfa() {
		addPanEvent(".cfa-svg");
		d3.selectAll(".cfa-node").on("mouseover", function (d, i) {
			var message;
			if (parseInt(i) > 100000) {
				message = "<span class=\" bold \">type</span>: function call node <br>" + "<span class=\" bold \">dblclick</span>: Select function";
			} else {
				var node = cfaJson.nodes.find(function (n) {
					return n.index === parseInt(i);
				});
				message = "<span class=\" bold \">function</span>: " + node.func;
				if (i in cfaJson.combinedNodes) {
					message += "<br><span class=\" bold \">combines nodes</span> : " + Math.min.apply(null, cfaJson.combinedNodes[i]) + "-" + Math.max.apply(null, cfaJson.combinedNodes[i]);
				}
				message += "<br> <span class=\" bold \">reverse postorder Id</span>: " + node.rpid;
			}
			showToolTipBox(d, message);
		}).on("mouseout", function () {
			hideToolTipBox();
		});
		d3.selectAll(".fcall").on("dblclick", function (d, i) {
			angular.element($("#cfa-toolbar")).scope().selectedCFAFunction = d3.select("#cfa-node" + i + " text").text();
			angular.element($("#cfa-toolbar").scope()).setCFAFunction();
		});
		d3.selectAll(".cfa-dummy").on("mouseover", function (d) {
			showToolTipBox(d, "<span class=\" bold \">type</span>: placeholder <br> <span class=\" bold \">dblclick</span>: jump to Target node");
		}).on("mouseout", function () {
			hideToolTipBox();
		}).on("dblclick", function () {
			if (!d3.select(".marked-cfa-node").empty()) {
				d3.select(".marked-cfa-node").classed("marked-cfa-node", false);
			}
			var selection = d3.select("#cfa-node" + d3.select(this).attr("id").split("-")[1]);
			selection.classed("marked-cfa-node", true);
			var boundingRect = selection.node().getBoundingClientRect();
			$("#cfa-container").scrollTop(boundingRect.top + $("#cfa-container").scrollTop() - 300).scrollLeft(boundingRect.left + $("#cfa-container").scrollLeft() - $("#errorpath_section").width() - 2 * boundingRect.width);
		})
		d3.selectAll(".cfa-edge")
			.on("mouseover", function (d) {
				d3.select(this).select("path").style("stroke-width", "3px");
				showToolTipBox(d, "<span class=\" bold \">dblclick</span>: jump to Source line");
			}).on("mouseout", function () {
				d3.select(this).select("path").style("stroke-width", "1.5px");
				hideToolTipBox();
			}).on("dblclick", function (d, i) {
				var edge = findCfaEdge(i);
				if (edge === undefined) { // this occurs for edges between graphs - splitting edges
					var thisEdgeData = d3.select(this).attr("id").split("_")[1];
					edge = findCfaEdge({
						v: thisEdgeData.split("-")[0],
						w: thisEdgeData.split("-")[1]
					});
				}
				$("#set-tab-3").click();
				var line = edge.line;
				if (line === 0) {
					line = 1;
				}
				if (!d3.select(".marked-source-line").empty()) {
					d3.select(".marked-source-line").classed("marked-source-line", false);
				}
				var selection = d3.select("#source-" + line + " td pre.prettyprint");
				selection.classed("marked-source-line", true);
				$(".sourceContent").scrollTop(selection.node().getBoundingClientRect().top + $(".sourceContent").scrollTop() - 200);
			});
		d3.selectAll(".cfa-split-edge")
			.on("mouseover", function (d) {
				d3.select(this).select("path").style("stroke-width", "3px");
				showToolTipBox(d, "<span class=\" bold \">type</span>: place holder <br> <span class=\" bold \">dblclick</span>: jump to Original edge");
			}).on("mouseout", function () {
				d3.select(this).select("path").style("stroke-width", "1.5px");
				hideToolTipBox();
			}).on("dblclick", function () {
				var edgeSourceTarget = d3.select(this).attr("id").split("_")[1];
				if (!d3.select(".marked-cfa-edge").empty()) {
					d3.select(".marked-cfa-edge").classed("marked-cfa-edge", false);
				}
				var selection = d3.select("#cfa-edge_" + edgeSourceTarget.split("-")[0] + "-" + edgeSourceTarget.split("-")[1]);
				selection.classed("marked-cfa-edge", true);
				var boundingRect = selection.node().getBoundingClientRect();
				$("#cfa-container").scrollTop(boundingRect.top + $("#cfa-container").scrollTop() - 300).scrollLeft(boundingRect.left + $("#cfa-container").scrollLeft() - $("#errorpath_section").width() - 2 * boundingRect.width);
			})
	}

	// Find and return the actual edge element from cfaJson.edges array by considering funcCallEdges and combinedNodes
	function findCfaEdge(eventElement) {
		var source = parseInt(eventElement.v);
		var target = parseInt(eventElement.w);
		if (source > 100000) {
			source = Object.keys(cfaJson.functionCallEdges).find(function (key) {
				if (cfaJson.functionCallEdges[key].includes(source)) {
					return key;
				}
			});
		}
		if (target > 100000) {
			target = cfaJson.functionCallEdges[eventElement.v][1];
		}
		if (source in cfaJson.combinedNodes) {
			source = cfaJson.combinedNodes[source][cfaJson.combinedNodes[source].length - 1];
		}
		return cfaJson.edges.find(function (e) {
			return e.source === parseInt(source) && e.target === target;
		})
	}

	// Add desired events to ARG the nodes
	function addEventsToArg() {
		addPanEvent(".arg-svg");
		d3.selectAll(".arg-node")
			.on("mouseover", function (d, i) {
				var nodesArray = Array.prototype.concat(
						argJson.nodes,
						typeof argJson.relevantnodes === "undefined" ? [] : argJson.relevantnodes,
						typeof argJson.reducednodes === "undefined" ? [] : argJson.reducednodes
				);
				var node = nodesArray.find(function (it) {
					return it.index === parseInt(i);
				})
				var message = "<span class=\" bold \">function</span>: " + node.func + "<br>";
				if (node.type) {
					message += "<span class=\" bold \">type</span>: " + node.type + "<br>";
				}
				message += "<span class=\" bold \">dblclick</span>: jump to CFA node";
				showToolTipBox(d, message);
			}).on("mouseout", function () {
				hideToolTipBox();
			}).on("dblclick", function () {
				$("#set-tab-1").click();
				if (!d3.select(".marked-cfa-node").empty()) {
					d3.select(".marked-cfa-node").classed("marked-cfa-node", false);
				}
				var nodeId = d3.select(this).select("tspan").text().split("N")[1];
				if (cfaJson.mergedNodes.includes(parseInt(nodeId))) {
					nodeId = getMergingNode(parseInt(nodeId));
				}
				var selection = d3.select("#cfa-node" + nodeId);
				selection.classed("marked-cfa-node", true);
				var boundingRect = selection.node().getBoundingClientRect();
				$("#cfa-container").scrollTop(boundingRect.top + $("#cfa-container").scrollTop() - 300).scrollLeft(boundingRect.left + $("#cfa-container").scrollLeft() - $("#errorpath_section").width() - 2 * boundingRect.width);
			});
		d3.selectAll(".arg-dummy")
			.on("mouseover", function (d) {
				showToolTipBox(d, "<span class=\" bold \">type</span>: placeholder <br> <span class=\" bold \">dblclick</span>: jump to Target node");
			}).on("mouseout", function () {
				hideToolTipBox();
			}).on("dblclick", function () {
				if (!d3.select(".marked-arg-node").empty()) {
					d3.select(".marked-arg-node").classed("marked-arg-node", false);
				}
				var selection = d3.select("#arg-node" + d3.select(this).attr("id").split("-")[1]);
				selection.classed("marked-arg-node", true);
				var boundingRect = selection.node().getBoundingClientRect();
				$("#arg-container").scrollTop(boundingRect.top + $("#arg-container").scrollTop() - 300).scrollLeft(boundingRect.left + $("#arg-container").scrollLeft() - $("#errorpath_section").width() - 2 * boundingRect.width);
			});
		d3.selectAll(".arg-edge")
			.on("mouseover", function (d, i) {
				d3.select(this).select("path").style("stroke-width", "3px");
				var edgeArray = Array.prototype.concat(
						argJson.edges,
						typeof argJson.relevantedges === "undefined" ? [] : argJson.relevantedges,
						typeof argJson.reducededges === "undefined" ? [] : argJson.reducededges
						);
				var edge = edgeArray.find(function (it) {
					return it.source === parseInt(i.v) && it.target === parseInt(i.w);
				})
				var message = "";
				Object.keys(edge).forEach(function(key,index) {
					if ($.inArray(key,["target", "source", "label", "line"])==-1) {
						message += "<span class=\" bold \">"+key+"<\span>: "+edge[key]+"<br>";
					}
				});
				if (edge) {
					showToolTipBox(d, message);
				} else {
					showToolTipBox(d, "<span class=\" bold \">type</span>: graph connecting edge")
				}
			}).on("mouseout", function () {
				d3.select(this).select("path").style("stroke-width", "1.5px");
				hideToolTipBox();
			});
		d3.selectAll(".arg-split-edge")
			.on("mouseover", function (d) {
				d3.select(this).select("path").style("stroke-width", "3px");
				showToolTipBox(d, "<span class=\" bold \">type</span>: place holder <br> <span class=\" bold \">dblclick</span>: jump to Original edge");
			}).on("mouseout", function () {
				d3.select(this).select("path").style("stroke-width", "1.5px");
				hideToolTipBox();
			}).on("dblclick", function () {
				var edgeSourceTarget = d3.select(this).attr("id").split("_")[1];
				if (!d3.select(".marked-arg-edge").empty()) {
					d3.select(".marked-arg-edge").classed("marked-arg-edge", false);
				}
				var selection = d3.select("#arg-edge" + edgeSourceTarget.split("-")[0] + edgeSourceTarget.split("-")[1]);
				selection.classed("marked-arg-edge", true);
				var boundingRect = selection.node().getBoundingClientRect();
				$("#arg-container").scrollTop(boundingRect.top + $("#arg-container").scrollTop() - 300).scrollLeft(boundingRect.left + $("#arg-container").scrollLeft() - $("#errorpath_section").width() - 2 * boundingRect.width);
			});
	}

	// Use D3 zoom behavior to add pan event
	function addPanEvent(itemsToSelect) {
		d3.selectAll(itemsToSelect).each(function (d, i) {
			var svg = d3.select(this),
				svgGroup = d3.select(this.firstChild);
			var zoom = d3.zoom().on("zoom", function (d, i) {
				svgGroup.attr("transform", d.transform)
			});
			svg.call(zoom);
			svg.on("zoom", null).on("wheel.zoom", null).on("dblclick.zoom", null).on("touchstart.zoom", null);
		});
	}

	// On mouse over display tool tip box
	function showToolTipBox(e, displayInfo) {
		var offsetX = 20;
		var offsetY = 0;
		var positionX = e.pageX;
		var positionY = e.pageY;
		d3.select("#boxContent").html("<p>" + displayInfo + "</p>");
		d3.select("#infoBox").style("left", function () {
			return positionX + offsetX + "px";
		}).style("top", function () {
			return positionY + offsetY + "px";
		}).style("visibility", "visible");
	}

	// On mouse out hide the tool tip box
	function hideToolTipBox() {
		d3.select("#infoBox").style("visibility", "hidden");
	}
}

window.init = init;
