(function() {
	// initialize all popovers
	$(function() {
		$('[data-toggle="popover"]').popover({
			html : true
		})
	});

	var app = angular.module('report', []);

	app.controller('ReportController', [ '$rootScope', '$scope',
			function($rootScope, $scope) {
				$scope.logo = "https://cpachecker.sosy-lab.org/logo.svg";
				$scope.help_content = "<p><b>CFA</b> (Control Flow Automaton) shows the control flow of the program. <br> For each function in the source code one CFA graph is created. <br>" + 
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
	            "<p><b>In case of split graph (applies to both CFA and ARG)</b><br> -- doubleclick on labelless node to jump to target node<br> -- doubleclick on 'split edge' to jump to initial edge </p>";
		        $scope.help_errorpath = "<p>The errorpath leads to the error 'edge by edge' (CFA) or 'node by node' (ARG) or 'line by line' (Source)</p>\n" +
	            "<p><b>-V- (Value Assignments)</b> Click to show all initialized variables and their values at that point in the programm.</p>\n" +
	            "<p><b>Edge-Description (Source-Code-View)</b> Click to jump to the relating edge in the CFA / node in the ARG / line in Source (depending on active tab).\n If non of the mentioned tabs is currently set, the ARG tab will be selected.</p>\n" +
	            "<p><b>Buttons (Prev, Start, Next)</b> Click to navigate through the errorpath and jump to the relating position in the active tab</p>\n" +
	            "<p><b>Search</b>\n - You can search for words or numbers in the edge-descriptions (matches appear blue)\n" +
	            "- You can search for value-assignments (variable names or their value) - it will highlight only where a variable has been initialized or where it has changed its value (matches appear green)\n" + 
	            "- An 'exact matches' search will look for a variable declarator matching exactly the provided text considering both, edge descriptions and value assignments</p>";
				$scope.tab = 1;
				$scope.$on("ChangeTab", function(event, tabIndex) {
					$scope.setTab(tabIndex);
				});
				$scope.setTab = function(tabIndex) {
					if (tabIndex === 1) {
						if (d3.select("#arg-toolbar").style("visibility") !== "hidden") {
							d3.select("#arg-toolbar").style("visibility", "hidden");
							d3.selectAll(".arg-graph").style("visibility", "hidden");
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
						if ($rootScope.displayedARG.indexOf("error") !== -1) {
							d3.selectAll(".arg-error-graph").style("visibility", "visible");
							if ($("#arg-container").scrollTop() === 0) {
								$("#arg-container").scrollTop(0).scrollLeft(0);
							}
						} else {
							d3.selectAll(".arg-graph").style("visibility", "visible");
							if ($("#arg-container").scrollTop() === 0) {
								var boundingRect = d3.select(".arg-node").node().getBoundingClientRect();
								$("#arg-container").scrollTop(boundingRect.top + $("#arg-container").scrollTop() - 200).scrollLeft(boundingRect.left + $("#arg-container").scrollLeft() - 500);
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
							d3.selectAll(".arg-error-graph").style("visibility", "hidden");
							if (d3.select("#arg-container").classed("arg-content")) {
								d3.select("#arg-container").classed("arg-content", false);
							}
						}						
					}
					$scope.tab = tabIndex;
				};
				$scope.tabIsSet = function(tabIndex) {
					return $scope.tab === tabIndex;
				};
				
				$scope.getTabSet = function() {
					return $scope.tab;
				};
			}]);
	
	app.controller("ErrorpathController", ['$rootScope', '$scope', function($rootScope, $scope) {
		$rootScope.errorPath = [];
		
		function getValues(val, prevValDict) {
            var values = {};
            if(val != "") {
                var singleStatements = val.split("\n");
                for (var i = 0; i < singleStatements.length - 1; i++) {
                	if (!Object.keys(prevValDict).includes(singleStatements[i].split("==")[0].trim())) {
                		// previous dictionary does not include the statement
                		values[singleStatements[i].split("==")[0].trim()] = singleStatements[i].split("==")[1].trim().slice(0,-1);
                	} else if (prevValDict[singleStatements[i].split("==")[0].trim()] !== singleStatements[i].split("==")[1].trim().slice(0,-1)) {
                		// statement is included but with different value
                		values[singleStatements[i].split("==")[0].trim()] = singleStatements[i].split("==")[1].trim().slice(0,-1);
                	}
                }
            }
            return values;
        };
        
        if (errorPath !== undefined) {
        	var indentationlevel = 0;
            for(var i = 0; i < errorPath.length; i++) {
                var errPathElem = errorPath[i];
                // do not show start, return and blank edges
                if (errPathElem.desc.indexOf("Return edge from") === -1 && errPathElem.desc != "Function start dummy edge" && errPathElem.desc != "") {
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
                    	$.extend(errPathElem.valDict, newValues)
                    }
                    for (key in errPathElem.valDict){
                    	errPathElem.valString += key + ":  " + errPathElem.valDict[key] + "\n";
                    }
                    // add indentation
                    for(var j = 1; j <= indentationlevel; j++) {
                        errPathElem.desc = "   " + errPathElem.desc;
                    }
                    $rootScope.errorPath.push(errPathElem);
                } else if(errPathElem.desc.indexOf("Return edge from") !== -1) {
                    indentationlevel -= 1;
                } else if(errPathElem.desc.indexOf("Function start dummy") !== -1) {
                    indentationlevel += 1;
                }
            }        	
        }
		
        $scope.errPathPrevClicked = function($event) {
        	var selection = d3.select("tr.clickedErrPathElement");
        	if (!selection.empty()) {
        		var prevId = parseInt(selection.attr("id").substring("errpath-".length)) - 1;
        		selection.classed("clickedErrPathElement", false);
        		d3.select("#errpath-" + prevId).classed("clickedErrPathElement", true);
        		$("#value-assignment").scrollTop($("#value-assignment").scrollTop() - 18);
        		markErrorPathElementInTab("Prev", prevId);
        	}
        };
        
        $scope.errPathStartClicked = function() {
        	d3.select("tr.clickedErrPathElement").classed("clickedErrPathElement", false);
        	d3.select("#errpath-0").classed("clickedErrPathElement", true);
        	$("#value-assignment").scrollTop(0);
        	markErrorPathElementInTab("Start", 0);
        };
        
        $scope.errPathNextClicked = function($event) {
        	var selection = d3.select("tr.clickedErrPathElement");
        	if (!selection.empty()) {
        		var nextId = parseInt(selection.attr("id").substring("errpath-".length)) + 1;
        		selection.classed("clickedErrPathElement", false);
        		d3.select("#errpath-" + nextId).classed("clickedErrPathElement", true);
            	$("#value-assignment").scrollTop($("#value-assignment").scrollTop() + 18);
        		markErrorPathElementInTab("Next", nextId);
        	}
        };
        
        $scope.clickedErrpathElement = function($event) {
        	d3.select("tr.clickedErrPathElement").classed("clickedErrPathElement", false);
        	var clickedElement = d3.select($event.currentTarget.parentNode);
        	clickedElement.classed("clickedErrPathElement", true);
        	markErrorPathElementInTab("", clickedElement.attr("id").substring("errpath-".length));
        };
        
        function markErrorPathElementInTab(buttonId, selectedErrPathElemId) {
        	if ($rootScope.errorPath[selectedErrPathElemId] === undefined) {
        		return;
        	}
        	var currentTab = $("#report-controller").scope().getTabSet();
        	// when the current tab is not one of CFA, ARG, source, set the tab to ARG
        	if (buttonId === "") {
        		handleErrorPathElemClick(currentTab, selectedErrPathElemId);
        	} else if (buttonId === "Start") {
        		handleStartButtonClick(currentTab);
        	} else if (buttonId === "Prev") {
        		handlePrevButtonClick(currentTab, selectedErrPathElemId);
        	} else { // "Next"
        		handleNextButtonClick(currentTab, selectedErrPathElemId);
        	}
        }
        
        function handleErrorPathElemClick(currentTab, errPathElemIndex) {
    		if (currentTab === 1) {
    			markCfaEdge($rootScope.errorPath[errPathElemIndex]);
    		} else if (currentTab === 2) {
    			markArgNode($rootScope.errorPath[errPathElemIndex]);
    		} else if (currentTab === 3) {
    			markSourceLine($rootScope.errorPath[errPathElemIndex]);
    		} else {
    			$("#report-controller").scope().setTab(2);
    			markArgNode($rootScope.errorPath[errPathElemIndex]);
    		}
        }
        
        function handleStartButtonClick(currentTab) {
    		if (currentTab === 1) {
    			markCfaEdge($rootScope.errorPath[0]);
    		} else if (currentTab === 2) {
    			markArgNode($rootScope.errorPath[0]);
    		} else if (currentTab === 3) {
    			markSourceLine($rootScope.errorPath[0]);
    		} else {
    			$("#report-controller").scope().setTab(2);
    			markArgNode($rootScope.errorPath[0]);
    		}
        }
        
        function handlePrevButtonClick(currentTab, elementId) {
    		if (currentTab === 1) {
    			markCfaEdge($rootScope.errorPath[elementId]);
    		} else if (currentTab === 2) {
    			markArgNode($rootScope.errorPath[elementId]);
    		} else if (currentTab === 3) {
    			markSourceLine($rootScope.errorPath[elementId]);
    		} else {
    			$("#report-controller").scope().setTab(2);
    			markArgNode($rootScope.errorPath[elementId]);
    		}
        }
        
        function handleNextButtonClick(currentTab, elementId) {
    		if (currentTab === 1) {
    			markCfaEdge($rootScope.errorPath[elementId]);
    		} else if (currentTab === 2) {
    			markArgNode($rootScope.errorPath[elementId]);
    		} else if (currentTab === 3) {
    			markSourceLine($rootScope.errorPath[elementId]);
    		} else {
    			$("#report-controller").scope().setTab(2);
    			markArgNode($rootScope.errorPath[elementId]);
    		}
        }
        
        function markCfaEdge(errPathEntry) {
        	var actualSourceAndTarget = getActualSourceAndTarget(errPathEntry);
        	if ($.isEmptyObject(actualSourceAndTarget)) return;
        	if (actualSourceAndTarget.target === undefined) {
        		var selection = d3.select("#cfa-node" + actualSourceAndTarget.source);
    			selection.classed("marked-cfa-node", true);
    			var boundingRect = selection.node().getBoundingClientRect();
    			$("#cfa-container").scrollTop(boundingRect.top + $("#cfa-container").scrollTop() - 200).scrollLeft(boundingRect.left - d3.select("#cfa-container").style("width").split("px")[0] - $("#cfa-container").scrollLeft());
    			if (actualSourceAndTarget.source in cfaJson.combinedNodes) {
    				d3.selectAll(".marked-cfa-node-label").classed("marked-cfa-node-label", false);
                    selection.selectAll("tspan").each(function(d,i) {
                    	if (d3.select(this).html().includes(errPathEntry.source)) {
                    		d3.select(this).classed("marked-cfa-node-label", true);
                    	}
                    });
    			}
    			return;
        	}
			if (!d3.select(".marked-cfa-edge").empty()) {
				d3.select(".marked-cfa-edge").classed("marked-cfa-edge", false);
			}
			d3.selectAll(".marked-cfa-node").classed("marked-cfa-node", false);
			d3.selectAll(".marked-cfa-node-label").classed("marked-cfa-node-label", false);
			var selection = d3.select("#cfa-edge_" + actualSourceAndTarget.source + "-" + actualSourceAndTarget.target);
			selection.classed("marked-cfa-edge", true);
			var boundingRect = selection.node().getBoundingClientRect();
			$("#cfa-container").scrollTop(boundingRect.top + $("#cfa-container").scrollTop() - 200).scrollLeft(boundingRect.left - d3.select("#cfa-container").style("width").split("px")[0] - $("#cfa-container").scrollLeft());
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
            Object.keys(cfaJson.combinedNodes).some(function(key) {
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
			if (!d3.select(".marked-arg-node").empty()) {
				d3.select(".marked-arg-node").classed("marked-arg-node", false);
			}
			var idToSelect;
			if (d3.select("#arg-graph0").style("display") !== "none")
				idToSelect = "#arg-node";
			else
				idToSelect = "#arg-error-node";
			var selection = d3.select(idToSelect + errPathEntry.argelem);
			selection.classed("marked-arg-node", true);
			var boundingRect = selection.node().getBoundingClientRect();
			$("#arg-container").scrollTop(boundingRect.top + $("#arg-container").scrollTop() - 200).scrollLeft(boundingRect.left - d3.select("#arg-container").style("width").split("px")[0] - $("#arg-container").scrollLeft());
        }
        
        function markSourceLine(errPathEntry) {
        	if (!d3.select(".marked-source-line").empty()) {
        		d3.select(".marked-source-line").classed("marked-source-line", false);
        	}
        	if (errPathEntry.line === 0) {
        		errPathEntry.line = 1;
        	}
			var selection = d3.select("#source-" + errPathEntry.line + " td pre.prettyprint");
			selection.classed("marked-source-line", true);
			$(".sourceContent").scrollTop(selection.node().getBoundingClientRect().top + $(".sourceContent").scrollTop() - 200);
        }
		
	}]);
	
	app.controller("SearchController", ['$rootScope', '$scope', function($rootScope, $scope) {
        $scope.numOfValueMatches = 0;
        $scope.numOfDescriptionMatches = 0;

        $scope.checkIfEnter = function($event){
            if($event.keyCode == 13){
                $scope.searchFor();
            }
        };
        
        $scope.searchFor = function() {
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
        			$rootScope.errorPath.forEach(function(it, i) {
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
        			$rootScope.errorPath.forEach(function(it, i) {
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
        		match = Object.keys(values).find(function(v) {
        			return v === searchInput;
        		})
        	} else {
            	match = Object.keys(values).find(function(v) {
            		return v.indexOf(searchInput) !== -1;
            	})
        	}
        	if (match) return true;
        	else return false;
        }
	}]);
	
	app.controller("ValueAssignmentsController", ['$rootScope', '$scope', function($rootScope, $scope) {
		$scope.showValues = function($event){
            var element = $event.currentTarget;
            if (element.classList.contains("markedTableElement")) {
                element.classList.remove("markedTableElement");
            } else {
                element.classList.add("markedTableElement");
            }
		};
	}]);
	
	app.controller('CFAToolbarController', ['$scope', 
		function($scope) {
			if (functions.length > 1) {
	    		$scope.functions = ["all"].concat(functions);
			} else {
				$scope.functions = functions;
			}
			$scope.selectedCFAFunction = $scope.functions[0];
    		$scope.zoomEnabled = false;
    	
    		$scope.setCFAFunction = function() {
    			if ($scope.zoomEnabled) {
    				$scope.zoomControl();
    			}
    			// FIXME: two-way binding does not update the selected option
    			d3.selectAll("#cfa-toolbar option").attr("selected", null).attr("disabled", null);
    			d3.select("#cfa-toolbar [label=" + $scope.selectedCFAFunction + "]").attr("selected", "selected").attr("disabled", true);
    			if ($scope.selectedCFAFunction === "all") {
    				functions.forEach(function(func) {
    					d3.selectAll(".cfa-svg-" + func).attr("display", "inline-block");
    				});
    			} else {
    				var funcToHide = $scope.functions.filter(function(it){
    					return it !== $scope.selectedCFAFunction && it !== "all";
    				});
    				funcToHide.forEach(function(func){
    					d3.selectAll(".cfa-svg-" + func).attr("display", "none");
    				});
    				d3.selectAll(".cfa-svg-" + $scope.selectedCFAFunction).attr("display", "inline-block");
    			}
    			var firstElRect = d3.select("[display=inline-block] .cfa-node:nth-child(2)").node().getBoundingClientRect();
    			if (d3.select("#errorpath_section").style("display") !== "none") {
    				$("#cfa-container").scrollTop(firstElRect.top + $("#cfa-container").scrollTop() - 200).scrollLeft(firstElRect.left - $("#cfa-container").scrollLeft() - d3.select("#externalFiles_section").style("width"));
    			} else {
    				$("#cfa-container").scrollTop(firstElRect.top + $("#cfa-container").scrollTop() - 200).scrollLeft(firstElRect.left - $("#cfa-container").scrollLeft());
    			}
    		};
        
    		$scope.cfaFunctionIsSet = function(value){
    			return value === $scope.selectedCFAFunction;
    		};
        
    		$scope.zoomControl = function() {
    			if ($scope.zoomEnabled) {
    				$scope.zoomEnabled = false;
    				d3.select("#cfa-zoom-button").html("<i class='glyphicon glyphicon-unchecked'></i>");
    				// revert zoom and remove listeners
    				d3.selectAll(".cfa-svg").each(function(d, i) {
    					d3.select(this).on("zoom", null).on("wheel.zoom", null).on("dblclick.zoom", null).on("touchstart.zoom", null);
    				});
    			} else {
    				$scope.zoomEnabled = true;
    				d3.select("#cfa-zoom-button").html("<i class='glyphicon glyphicon-ok'></i>");
    				d3.selectAll(".cfa-svg").each(function(d, i) {
    					var svg = d3.select(this), svgGroup = d3.select(this.firstChild);
    					var zoom = d3.behavior.zoom().on("zoom", function() {
    						svgGroup.attr("transform", "translate("
    								+ d3.event.translate + ")" + "scale("
    								+ d3.event.scale + ")");
    					});        			
    					svg.call(zoom);
    					svg.on("dblclick.zoom", null).on("touchstart.zoom", null);
    				});
    			}
    		};
    		
    		$scope.redraw = function() {
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
    			cfaJson.functionNames.forEach(function(f) {
    				var fNodes = cfaJson.nodes.filter(function(n) {
    					return n.func === f;
    				})
    				graphCount += Math.ceil(fNodes.length/input);
    			});
    			$("#cfa-modal").text("0/" + graphCount);
    			graphCount = null;
    			$("#renderStateModal").modal("show");
    			if (cfaWorker === undefined) {
    				cfaWorker = new Worker(URL.createObjectURL(new Blob(["("+cfaWorker_function.toString()+")()"], {type: "text/javascript"})));
    			}
    			cfaWorker.postMessage({"split" : input});
    			cfaWorker.postMessage({"renderer" : "ready"});
    		};
    		
    		$scope.validateInput = function(input) {
    			if (input % 1 !== 0) return false;
    			if (input < 500 || input > 900) return false;
    			return true;
    		}
	}]);
	
	app.controller('ARGToolbarController', ['$rootScope', '$scope',
		function($rootScope, $scope) {
			$scope.zoomEnabled = false;
			$scope.argSelections = ["complete"];
			if (errorPath !== undefined) {
				$scope.argSelections.push("error path");
			}
			$rootScope.displayedARG = $scope.argSelections[0];
			
			$scope.displayARG = function() {
				if ($scope.argSelections.length > 1) {
					if ($rootScope.displayedARG.indexOf("error") !== -1) {
						d3.selectAll(".arg-graph").style("display", "none");
						$("#arg-container").scrollTop(0).scrollLeft(0);
						if (d3.select(".arg-error-graph").empty()) {
							argWorker.postMessage({"errorGraph": true});
						} else {
							d3.selectAll(".arg-error-graph").style("display", "inline-block").style("visibility", "visible");
						}
					} else {
						if (!d3.select(".arg-error-graph").empty()) {
							d3.selectAll(".arg-error-graph").style("display", "none");
						}
						d3.selectAll(".arg-graph").style("display", "inline-block").style("visibility", "visible");
						$("#arg-container").scrollLeft(d3.select(".arg-svg").attr("width")/4);
					}
				}
			};
			
    		$scope.argZoomControl = function() {
    			if ($scope.zoomEnabled) {
    				$scope.zoomEnabled = false;
    				d3.select("#arg-zoom-button").html("<i class='glyphicon glyphicon-unchecked'></i>");
    				// revert zoom and remove listeners
    				d3.selectAll(".arg-svg").each(function(d, i) {
    					d3.select(this).on("zoom", null).on("wheel.zoom", null).on("dblclick.zoom", null).on("touchstart.zoom", null);
    				});
    			} else {
    				$scope.zoomEnabled = true;
    				d3.select("#arg-zoom-button").html("<i class='glyphicon glyphicon-ok'></i>");
    				d3.selectAll(".arg-svg").each(function(d, i) {
    					var svg = d3.select(this), svgGroup = d3.select(this.firstChild);
    					var zoom = d3.behavior.zoom().on("zoom", function() {
    						svgGroup.attr("transform", "translate("
    								+ d3.event.translate + ")" + "scale("
    								+ d3.event.scale + ")");
    					});        			
    					svg.call(zoom);
    					svg.on("dblclick.zoom", null).on("touchstart.zoom", null);
    				});
    			}
    		};
    		
    		$scope.argRedraw = function() {
    			var input = $("#arg-split-threshold").val();
    			if (!$scope.validateInput(input)) {
    				alert("Invalid input!");
    				return;
    			}
    			d3.selectAll(".arg-graph").remove();
    			d3.selectAll(".arg-error-graph").remove();
    			if ($scope.zoomEnabled) {
    				$scope.argZoomControl();
    			}
    			var graphCount = Math.ceil(argJson.nodes.length/input);
    			$("#arg-modal").text("0/" + graphCount);
    			graphCount = null;
    			$("#renderStateModal").modal("show");
    			if (argWorker === undefined) {
    				argWorker = new Worker(URL.createObjectURL(new Blob(["("+argWorker_function.toString()+")()"], {type: "text/javascript"})));
    			}
    			argWorker.postMessage({"split" : input});
    			argWorker.postMessage({"renderer" : "ready"});
    		};
    		
    		$scope.validateInput = function(input) {
    			if (input % 1 !== 0) return false;
    			if (input < 500 || input > 900) return false;
    			return true;
    		}    		
	}]);

	app.controller('SourceController', [ '$rootScope', '$scope', '$location',
			'$anchorScroll',
			function($rootScope, $scope, $location, $anchorScroll) {
				// available sourcefiles
				$scope.sourceFiles = sourceFiles;
				$scope.selectedSourceFile = 0;
		        $scope.setSourceFile = function(value) {
		            $scope.selectedSourceFile = value;
		        };
		        $scope.sourceFileIsSet = function(value) {
		            return value === $scope.selectedSourceFile;
		        };
			}]);

})();

var argJson={};//ARG_JSON_INPUT

var sourceFiles = []; //SOURCE_FILES
var cfaJson={};//CFA_JSON_INPUT

// CFA graph variable declarations
var functions = cfaJson.functionNames;
var functionCallEdges = cfaJson.functionCallEdges;
var errorPath;
if (cfaJson.hasOwnProperty("errorPath")) {
	errorPath = cfaJson.errorPath;
}
var graphSplitThreshold = 700;
var zoomEnabled = false;
// A Dagre D3 Renderer
var render = new dagreD3.render();
const margin = 20;
var cfaWorker, argWorker;
var cfaSplit = false, argTabDisabled = false;

function init() {
	
	// Calculate total count of graphs to display in modal
	var argTotalGraphCount;
	if (argJson.nodes) {
		argTotalGraphCount = Math.ceil(argJson.nodes.length/graphSplitThreshold);
		$("#arg-modal").text("0/" + argTotalGraphCount);
	} else { // No ARG data -> happens if the AbstractStates are not ARGStates
		$("#arg-modal").text("0/0");
		$("#set-tab-2").parent().addClass("disabled");
		argTabDisabled = true;
	}
	var cfaTotalGraphCount = 0;
	cfaJson.functionNames.forEach(function(f) {
		var fNodes = cfaJson.nodes.filter(function(n) {
			return n.func === f;
		})
		cfaTotalGraphCount += Math.ceil(fNodes.length/graphSplitThreshold);
	});
	$("#cfa-modal").text("0/" + cfaTotalGraphCount);
	cfaTotalGraphCount = null;
	
	// Display modal window containing current rendering state
	$("#renderStateModal").modal("show");
	
	// Setup section widths accordingly 
	if (errorPath === undefined) {
		d3.select("#errorpath_section").style("display", "none");
	} else {
		d3.select("#externalFiles_section").style("width", "75%");
		d3.select("#cfa-toolbar").style("width", "70%");
	}
	
	// ======================= Define CFA and ARG Workers logic =======================
	/**
	 * The CFA Worker. Contains the logic for building a single or a multi CFA graph.
	 * The graph(s) is/are returned to the main script once created
	 */
    function cfaWorker_function() {
        self.importScripts("https://www.sosy-lab.org/lib/d3js/3.5.17/d3.min.js", "https://www.sosy-lab.org/lib/dagre-d3/0.4.17/dagre-d3.min.js");
        var json, nodes, mainNodes, edges, functions, combinedNodes, combinedNodesLabels, mergedNodes, functionCallEdges, errorPath;
        var graphSplitThreshold = 700; // default value
        var graphMap = [];
        var graphCounter = 0;
        
        // The first posted message will include the cfaJson
        self.addEventListener('message', function(m) {
            if (m.data.json !== undefined) {
                json = JSON.parse(m.data.json);
                extractVariables();
                buildGraphsAndPostResults();
            } else if (m.data.renderer !== undefined) {
                if (graphMap[graphCounter] !== undefined) {
                    var node = nodes.find(function(n) {
                        return n.index === parseInt(graphMap[graphCounter].nodes()[0]);
                    });
                    self.postMessage({"graph" : JSON.stringify(graphMap[graphCounter]), "id" : node.func + graphCounter, "func" : node.func});
                    graphCounter++;
                } else {
                    self.postMessage({"status": "done"});
                    graphMap = [];
                    graphCounter = 0;
                }
            } else if (m.data.split !== undefined) {
                graphSplitThreshold = m.data.split;
                buildGraphsAndPostResults();
            }
        }, false);
        
        // Extract information from the cfaJson
        function extractVariables() {
            nodes = json.nodes;
            functions = json.functionNames;
            mainNodes = nodes.filter(function(n) {
                return n.func === functions[0];
            });
            edges = json.edges;
            combinedNodes = json.combinedNodes;
            combinedNodesLabels = json.combinedNodesLabels;
            mergedNodes = json.mergedNodes;
            functionCallEdges = json.functionCallEdges;
            if (json.hasOwnProperty("errorPath")) {
            	errorPath = [];
            	prepareCfaErrorPath();
            }
        }
        
        // Prepare Error Path array to be used in edge class decider
        function prepareCfaErrorPath() {
        	var returnedEdges = {};
        	for (key in functionCallEdges) {
        		returnedEdges[functionCallEdges[key][1]] = functionCallEdges[key][0]
        	}
        	json.errorPath.forEach(function(errPathElem) {
            	if (errPathElem.source in functionCallEdges) {
                    errPathElem.target = functionCallEdges[errPathElem.source][0];
                }
            	if (errPathElem.target in returnedEdges) {
            		errorPath.push({"source":returnedEdges[errPathElem.target], "target": errPathElem.target})
            	}
            	errorPath.push(errPathElem);
        	})
        }
        
        function buildGraphsAndPostResults() {
            if (mainNodes.length > graphSplitThreshold) {
                buildMultipleGraphs(mainNodes, functions[0]);
            } else {
                buildSingleGraph(mainNodes, functions[0]);
            }
            if (functions.length > 1) {
                var functionsToProcess = functions.filter(function(f){
                    return f !== functions[0];
                });
                functionsToProcess.forEach(function(func) {
                    var funcNodes = nodes.filter(function(n){
                        return n.func === func;
                    });
                    if (funcNodes.length > graphSplitThreshold) {
                        buildMultipleGraphs(funcNodes, func);
                    } else {
                        buildSingleGraph(funcNodes, func);
                    }
                });
            }
        }
        
        function buildSingleGraph(nodesToSet, funcName) {
            var g = createGraph();
            setGraphNodes(g, nodesToSet);
            roundNodeCorners(g);
            var nodesIndices = [];
            nodesToSet.forEach(function(n){
            	nodesIndices.push(n.index);
            });
            var edgesToSet = edges.filter(function(e){
                return nodesIndices.includes(e.source) && nodesIndices.includes(e.target);
            });
            setGraphEdges(g, edgesToSet, false);
            if (funcName === functions[0]) {
                self.postMessage({"graph" : JSON.stringify(g), "id" : funcName + graphCounter, "func" : funcName});
                graphMap.push(g);
            } else {
                graphMap.push(g);
            }
        }
        
        function buildMultipleGraphs(nodesToSet, funcName) {
            var requiredGraphs = Math.ceil(nodesToSet.length/graphSplitThreshold);
            var firstGraphBuild = false;
            var nodesPerGraph = [];
            for (var i = 1; i <= requiredGraphs; i++) {
                if (!firstGraphBuild) {
                    nodesPerGraph = nodesToSet.slice(0, graphSplitThreshold);
                    firstGraphBuild = true;
                } else {
                    if (nodesToSet[graphSplitThreshold * i - 1] !== undefined)
                        nodesPerGraph = nodesToSet.slice(graphSplitThreshold * (i - 1), graphSplitThreshold * i);
                    else
                        nodesPerGraph = nodesToSet.slice(graphSplitThreshold * (i - 1));
                }
                var graph = createGraph();
                setGraphNodes(graph, nodesPerGraph);
                if (graph.nodes().length > 0) {
                    graphMap.push(graph);
                    roundNodeCorners(graph);
                    var graphEdges = edges.filter(function(e) {
                        if ( (nodesPerGraph[0].index <= e.source && e.source <= nodesPerGraph[nodesPerGraph.length - 1].index ) &&
                                (nodesPerGraph[0].index <= e.target && e.target <= nodesPerGraph[nodesPerGraph.length - 1].index) ) {
                            return e;
                        }
                    });
                    setGraphEdges(graph, graphEdges, true);
                }
            }
            buildCrossgraphEdges(nodesToSet);
            if (funcName === functions[0]) {
                self.postMessage({"graph" : JSON.stringify(graphMap[graphCounter]), "id" : funcName + graphCounter, "func" : funcName});
                graphCounter++;
            }
        }
        
        // Handle Edges that connect Graphs
        function buildCrossgraphEdges(crossGraphNodes) {
            var nodesIndices = [];
            crossGraphNodes.forEach(function(n) {
            	nodesIndices.push(n.index);
            });
            var edgesToConsider = edges.filter(function(e) {
                return nodesIndices.includes(e.source) && nodesIndices.includes(e.target);
            });
            edgesToConsider.forEach(function(edge){
                var source = edge.source;
                var target = edge.target;
                if (mergedNodes.includes(source) && mergedNodes.includes(target)) return;
                if (mergedNodes.includes(source)) source = getMergingNode(source);
                if (mergedNodes.includes(target)) target = getMergingNode(target);
                var sourceGraph = getGraphForNode(source);
                var targetGraph = getGraphForNode(target);
                if (sourceGraph < targetGraph) {
                    if (Object.keys(functionCallEdges).includes("" + source)) {
                        var funcCallNodeId = functionCallEdges["" + source][0];
                        graphMap[sourceGraph].setNode(funcCallNodeId, {label: getNodeLabelFCall(edge.stmt), class: "cfa-node fcall", id: "cfa-node" + funcCallNodeId, shape: "rect"});
                        graphMap[sourceGraph].setEdge(source, funcCallNodeId, {label: edge.stmt, labelStyle: labelStyleDecider(edge, source, funcCallNodeId), class: edgeClassDecider(edge, source, funcCallNodeId), id: "cfa-edge_" + source + "-" + funcCallNodeId});
                        graphMap[sourceGraph].setNode("" + source + target + sourceGraph, {label: "", class: "cfa-dummy", id: "dummy-" + target, shape: "rect"});
                        graphMap[sourceGraph].setEdge(funcCallNodeId, "" + source + target + sourceGraph, {label: source + "->" + target, style: "stroke-dasharray: 5, 5;"});
                    } else {
                        graphMap[sourceGraph].setNode("" + source + target + sourceGraph, {label: "", class: "cfa-dummy", id: "dummy-" + target, shape: "rect"});
                        graphMap[sourceGraph].setEdge(source, "" + source + target + sourceGraph, {label: edge.stmt, labelStyle: labelStyleDecider(edge, source, "" + source + target + sourceGraph), id: "cfa-edge_" + source + "-" + target, class: edgeClassDecider(edge, source, "" + source + target + sourceGraph), style: "stroke-dasharray: 5, 5;"});
                    }
                    graphMap[targetGraph].setNode("" + target + source + targetGraph, {label: "", class: "dummy"});
                    graphMap[targetGraph].setEdge("" + target + source + targetGraph, target, {label: "", labelStyle: "font-size: 12px;", id: "cfa-split-edge_"+ source + "-" + target , class: "cfa-split-edge", style: "stroke-dasharray: 5, 5;"});
                } else if (sourceGraph > targetGraph) {
                    graphMap[sourceGraph].setNode("" + source + target + sourceGraph, {label: "", class: "cfa-dummy", id: "dummy-" + target});
                    graphMap[sourceGraph].setEdge(source, "" + source + target + sourceGraph, {label: edge.stmt, labelStyle: labelStyleDecider(edge, "" + source + target + sourceGraph, source), id:"cfa-edge_" + source + "-" + target, class: edgeClassDecider(edge, "" + source + target + sourceGraph, source), arrowhead: "undirected", style: "stroke-dasharray: 5, 5;"})
                    graphMap[targetGraph].setNode("" + target + source + targetGraph, {label: "", class: "dummy", id: "node" + source});
                    graphMap[targetGraph].setEdge("" + target + source + targetGraph, target, {label: "", labelStyle: "font-size: 12px;", id:"cfa-split-edge_" + source + "-" + target, class: "cfa-split-edge", arrowhead: "undirected", style: "stroke-dasharray: 5, 5;"});
                }
            });
        }

        // Return the graph in which the nodeNumber is present
    	function getGraphForNode(nodeNumber) {
    		return graphMap.findIndex(function(graph) {
    			return graph.nodes().includes("" + nodeNumber);
    		})
    	}
        
        // create and return a graph element with a set transition
        function createGraph() {
            var g = new dagreD3.graphlib.Graph().setGraph({}).setDefaultEdgeLabel(
                    function() {
                        return {};
                    });
            return g;
        }
        
        // Set nodes for the graph contained in the json nodes
        function setGraphNodes(graph, nodesToSet) {
            nodesToSet.forEach(function(n) {
                if (!mergedNodes.includes(n.index)) {
                    graph.setNode(n.index, {
                        label : setNodeLabel(n),
                        labelStyle: "font-family: 'Courier New', Courier, monospace",
                        class : "cfa-node",
                        id : "cfa-node" + n.index,
                        shape : nodeShapeDecider(n)
                    });
                } 
            });
        }
        
        // Node label, the label from combined nodes or a simple label
        function setNodeLabel(node) {
            var nodeIndex = "" + node.index;
            if (Object.keys(combinedNodesLabels).includes(nodeIndex))
                return combinedNodesLabels[nodeIndex];
            else return "N" + nodeIndex;
        }
        
        // Decide the shape of the nodes based on type
        function nodeShapeDecider(n) {
        	if (n.loop) {
        		return "diamond";
        	} else if (Object.keys(combinedNodes).includes("" + n.index)) {
        		return "rect";
        	} else {
        		return "circle";
        	}
        }
        
        // Round the corners of rectangle shaped nodes
        function roundNodeCorners(graph) {
            graph.nodes().forEach(function(it) {
                var item = graph.node(it);
                item.rx = item.ry = 5;
            });
        }
        
        // Set the edges for a single graph while considering merged nodes and edges between them
        function setGraphEdges(graph, edgesToSet, multigraph) {
            edgesToSet.forEach(function(e) {
                var source, target;
                if (!mergedNodes.includes(e.source) && !mergedNodes.includes(e.target)) {
                    source = e.source;
                    target = e.target;
                } else if (!mergedNodes.includes(e.source) && mergedNodes.includes(e.target)) {
                    source = e.source;
                    target = getMergingNode(e.target);
                } else if (mergedNodes.includes(e.source) && !mergedNodes.includes(e.target)) {
                    source = getMergingNode(e.source);
                    target = e.target;
                }
                if (multigraph && (!graph.nodes().includes("" + source) || !graph.nodes().includes("" + target))) 
                    source = undefined;
                if (source !== undefined && target !== undefined && checkEligibleEdge(source, target)) {
                    if (Object.keys(functionCallEdges).includes("" + source)) {
                        var funcCallNodeId = functionCallEdges["" + source][0];
                        graph.setNode(funcCallNodeId, {
                            label : getNodeLabelFCall(e.stmt),
                            class : "cfa-node fcall",
                            id : "cfa-node" + funcCallNodeId,
                            shape : "rect"
                        });
                        graph.setEdge(source, funcCallNodeId, {
                            label: e.stmt,
                            labelStyle: labelStyleDecider(e, source, funcCallNodeId),
                            class: edgeClassDecider(e, source, funcCallNodeId), 
                            id: "cfa-edge_"+ source + "-" + funcCallNodeId,
                            weight: edgeWeightDecider(source, target)
                        });
                        graph.setEdge(funcCallNodeId, target, {
                            label: "",
                            labelStyle: labelStyleDecider(e, funcCallNodeId, target),
                            class: edgeClassDecider(e, funcCallNodeId, target), 
                            id: "cfa-edge_"+ funcCallNodeId + "-" + target,
                            weight: edgeWeightDecider(source, target)
                        });                     
                    } else {
                        graph.setEdge(source, target, {
                            label: e.stmt,
                            labelStyle: labelStyleDecider(e, source, target),
                            lineInterpolate: "basis",
                            class: edgeClassDecider(e, source, target), 
                            id: "cfa-edge_"+ source + "-" + target,
                            weight: edgeWeightDecider(source, target)
                        });
                    }
                }
            });
        }
        
        // If edge is part of error path, give it a representative class
    	function edgeClassDecider(edge, source, target) {
    		if (errorPath === undefined) {
    			return "cfa-edge";
    		}
			var mergedMatch = errorPath.find(function(entry) {
				return entry.source === source && entry.target === target;
			})
 			var initialMatch = errorPath.find(function(entry) {
				return entry.source === edge.source && entry.target === edge.target;
			}) 
			if (mergedMatch !== undefined || initialMatch !== undefined) {
				return "cfa-edge error-edge";
			} else {
				return "cfa-edge";
			}
    	}
    	
    	// If edge is part of error path, give its label a representative class
    	function labelStyleDecider(edge, source, target) {
    		var edgeClass = edgeClassDecider(edge, source, target);
    		if (edgeClass.indexOf("error") !== -1) {
    			return "font-size: 12px; fill: red";
    		}
    		return "font-size: 12px";
    	}
        
        // Check if edge is eligible to place in graph. Same node edge only if it is not combined node
        function checkEligibleEdge(source, target) {
            if (mergedNodes.includes(source) && mergedNodes.includes(target)) return false;
            if (mergedNodes.includes(source) && Object.keys(combinedNodes).includes("" + target)) {
                if (combinedNodes["" + target].includes(source)) return false;
                else return true;
            }
            if (Object.keys(combinedNodes).includes("" + source) && mergedNodes.includes(target)){
                if (combinedNodes["" + source].includes(target)) return false;
                else return true;               
            }
            if ((Object.keys(combinedNodes).includes("" + source) && Object.keys(combinedNodes).includes("" + target)) && source == target) return false; 
            return true;
        }
        
        // Retrieve the node in which this node was merged
        function getMergingNode(index) {
            var result = "";
            Object.keys(combinedNodes).some(function(key) {
                if (combinedNodes[key].includes(index)) {
                    result = key;
                    return result;
                }
            })
            return parseInt(result);
        }
        
        // Decide the weight for the edges based on type
        function edgeWeightDecider(source, target) {
        	var sourceNode = nodes.find(function(it) {
        		return it.index === source;
        	})
        	var targetNode = nodes.find(function(it) {
        		return it.index === target;
        	})
            if (source === target) {
            	return 2;
            } else if (sourceNode.rpid < targetNode.rpid) {
            	return 0;
            } else {
            	return 1;
            }
        }
        
        // Get node label for functionCall node by providing the edge statement
        function getNodeLabelFCall(stmt) {
            var result = "";
            if (stmt.includes("=")) {
                result = stmt.split("=")[1].split("(")[0].trim();
            } else {
                result = stmt.split("(")[0].trim();
            }
            return result;
        }
        
    }

	/**
	 * The ARG Worker. Contains the logic for creating a single or multi ARG graph.
	 * Once the graph(s) is/are created they are returned to the main script.
	 * ONLY if ARG data is available!
	 */
    if (argJson.nodes) {
    	function argWorker_function() {
    		self.importScripts("https://www.sosy-lab.org/lib/d3js/3.5.17/d3.min.js", "https://www.sosy-lab.org/lib/dagre-d3/0.4.17/dagre-d3.min.js");
    		var json, nodes, edges, errorPath, errorGraphMap;
    		var graphSplitThreshold = 700;
    		var graphMap = [], graphCounter = 0;
    		self.addEventListener("message", function(m) {
    			if (m.data.json !== undefined) {
    				json = JSON.parse(m.data.json);
    				nodes = json.nodes;
    				edges = json.edges;
    				buildGraphsAndPrepareResults()
    			} else if (m.data.errorPath !== undefined) {
    				errorPath = [];
    				JSON.parse(m.data.errorPath).forEach(function(d) {
    					if (d.argelem !== undefined) {
    						errorPath.push(d.argelem);
    					}
    				});
    			} else if (m.data.renderer !== undefined) {
    				if (graphMap.length > 0) {
                        self.postMessage({"graph" : JSON.stringify(graphMap[0]), "id" : graphCounter});
                        graphMap.shift();
                        graphCounter++;
                    } else {
                        self.postMessage({"status": "done"});
                        if (errorPath !== undefined) {
                        	errorGraphMap = [];
                        	graphCounter = 0;
                        	prepareErrorGraph();
                        }
                    }
    			} else if (m.data.errorGraph !== undefined) {
    				if (errorGraphMap.length > 0) {
    					self.postMessage({"graph" : JSON.stringify(errorGraphMap[0]), "id": graphCounter, "errorGraph": true});
    					errorGraphMap.shift();
    					graphCounter++;
    				}
    			} else if (m.data.split !== undefined) {
        			graphSplitThreshold = m.data.split;
        			if (errorGraphMap !== undefined && errorGraphMap.length > 0) {
        				errorGraphMap = [];
        			}
        			buildGraphsAndPrepareResults();
        		}
    		}, false);
    		
    		function buildGraphsAndPrepareResults() {
    			if (nodes.length > graphSplitThreshold) {
    				buildMultipleGraphs();
    			} else {
    				buildSingleGraph();
    			}			
    		}
    		
            // After the initial ARG graph has been send to the master script, prepare ARG containing only error path		
    		function prepareErrorGraph() {
    			var errorNodes = [], errorEdges = [];
    			nodes.forEach(function(n) {
    				if (errorPath.includes(n.index)) {
    					errorNodes.push(n);
    				}
    			});
    			edges.forEach(function(e) {
    				if (errorPath.includes(e.source) && errorPath.includes(e.target)) {
    					errorEdges.push(e);
    				}
    			});
    			if (errorNodes.length > graphSplitThreshold) {
    				buildMultipleErrorGraphs(errorNodes, errorEdges);
    			} else {
    				var g = createGraph();
    				setGraphNodes(g, errorNodes);
    				setGraphEdges(g, errorEdges, false);
    				errorGraphMap.push(g);
    			}
    		}
    		
    		function buildSingleGraph() {
    			var g = createGraph();
    			setGraphNodes(g, nodes);
    			setGraphEdges(g, edges, false);
                graphMap.push(g);
    		}
    		
    		// Split the ARG graph honoring the split threshold
    		function buildMultipleGraphs() {
    			nodes.sort(function(firstNode, secondNode) {
    				return firstNode.index - secondNode.index;
    			})
        		var requiredGraphs = Math.ceil(nodes.length/graphSplitThreshold);
        		var firstGraphBuild = false;
        		var nodesPerGraph = [];
        		for (var i = 1; i <= requiredGraphs; i++) {
        			if (!firstGraphBuild) {
        				nodesPerGraph = nodes.slice(0, graphSplitThreshold);
        				firstGraphBuild = true;
        			} else {
        				if (nodes[graphSplitThreshold * i - 1] !== undefined) {
        					nodesPerGraph = nodes.slice(graphSplitThreshold * (i - 1), graphSplitThreshold * i);
        				} else {
        					nodesPerGraph = nodes.slice(graphSplitThreshold * (i - 1));
        				}
        			}
        			var graph = createGraph();
        			graphMap.push(graph);
        			setGraphNodes(graph, nodesPerGraph);
        			var nodesIndices = []
        			nodesPerGraph.forEach(function(n) {
        				nodesIndices.push(n.index);
        			});
        			var graphEdges = edges.filter(function(e) {
        				if (nodesIndices.includes(e.source) && nodesIndices.includes(e.target)) {
        					return e;
        				}
        			});
        			setGraphEdges(graph, graphEdges, true);
        		}
        		buildCrossgraphEdges(edges, false);
    		}
    		
    		// Split the ARG error graph honoring the split threshold
            function buildMultipleErrorGraphs(errorNodes, errorEdges) {
    			errorNodes.sort(function(firstNode, secondNode) {
    				return firstNode.index - secondNode.index;
    			})
                var requiredGraphs = Math.ceil(errorNodes.length/graphSplitThreshold);
                var firstGraphBuild = false;
                var nodesPerGraph = [];
                for (var i = 1; i <= requiredGraphs; i++) {
                    if (!firstGraphBuild) {
                        nodesPerGraph = errorNodes.slice(0, graphSplitThreshold);
                        firstGraphBuild = true;
                    } else {
                        if (nodes[graphSplitThreshold * i - 1] !== undefined) {
                            nodesPerGraph = errorNodes.slice(graphSplitThreshold * (i - 1), graphSplitThreshold * i);
                        } else {
                            nodesPerGraph = errorNodes.slice(graphSplitThreshold * (i - 1));
                        }
                    }
                    var graph = createGraph();
                    errorGraphMap.push(graph);
                    setGraphNodes(graph, nodesPerGraph);
                    var nodesIndices = []
                    nodesPerGraph.forEach(function(n) {
                        nodesIndices.push(n.index);
                    });
                    var graphEdges = errorEdges.filter(function(e) {
                        if (nodesIndices.includes(e.source) && nodesIndices.includes(e.target)) {
                            return e;
                        }
                    });
                    setGraphEdges(graph, graphEdges, true);
                }
                buildCrossgraphEdges(errorEdges, true);            
            }
    		
    		// Handle graph connecting edges
        	function buildCrossgraphEdges(edges, errorGraph) {
        		edges.forEach(function(edge) {
        			var sourceGraph, targetGraph;
        			if (errorGraph) {
        				sourceGraph = getGraphForErrorNode(edge.source);
        				targetGraph = getGraphForErrorNode(edge.target);
            			if (sourceGraph < targetGraph) { 
            				errorGraphMap[sourceGraph].setNode("" + edge.source + edge.target + sourceGraph, {label: "", class: "arg-dummy", id: "dummy-" + edge.target});
            				errorGraphMap[sourceGraph].setEdge(edge.source, "" + edge.source + edge.target + sourceGraph, {label: edge.label, id: "arg-edge" + edge.source + edge.target, style: "stroke-dasharray: 5, 5;", class: edgeClassDecider(edge)});
            				errorGraphMap[targetGraph].setNode("" + edge.target + edge.source + targetGraph, {label: "", class: "dummy"});
            				errorGraphMap[targetGraph].setEdge("" + edge.target + edge.source + targetGraph, edge.target, {label: "", labelStyle: "font-size: 12px;", id: "arg-edge_" + edge.source + "-" + edge.target, style: "stroke-dasharray: 5, 5;", class: "arg-split-edge"});
            			} else if (sourceGraph > targetGraph) {
            				errorGraphMap[sourceGraph].setNode("" + edge.source + edge.target + sourceGraph, {label: "", class: "arg-dummy", id: "dummy-" + edge.target});
            				errorGraphMap[sourceGraph].setEdge(edge.source, "" + edge.source + edge.target + sourceGraph, {label: edge.label, id: "arg-edge" + edge.source + edge.target, arrowhead: "undirected", style: "stroke-dasharray: 5, 5;", class: edgeClassDecider(edge)})
            				errorGraphMap[targetGraph].setNode("" + edge.target + edge.source + targetGraph, {label: "", class: "dummy"});
            				errorGraphMap[targetGraph].setEdge("" + edge.target + edge.source + targetGraph, edge.target, {label: "", labelStyle: "font-size: 12px;", id: "arg-edge_" + edge.source + "-" + edge.target, arrowhead: "undirected", style: "stroke-dasharray: 5, 5;", class: "arg-split-edge"});
            			}
        			} else {
            			sourceGraph = getGraphForNode(edge.source);
            			targetGraph = getGraphForNode(edge.target);
            			if (sourceGraph < targetGraph) { 
                			graphMap[sourceGraph].setNode("" + edge.source + edge.target + sourceGraph, {label: "", class: "arg-dummy", id: "dummy-" + edge.target});
                			graphMap[sourceGraph].setEdge(edge.source, "" + edge.source + edge.target + sourceGraph, {label: edge.label, id: "arg-edge" + edge.source + edge.target, style: "stroke-dasharray: 5, 5;", class: edgeClassDecider(edge)});
                			graphMap[targetGraph].setNode("" + edge.target + edge.source + targetGraph, {label: "", class: "dummy"});
                			graphMap[targetGraph].setEdge("" + edge.target + edge.source + targetGraph, edge.target, {label: "", labelStyle: "font-size: 12px;", id: "arg-edge_" + edge.source + "-" + edge.target, style: "stroke-dasharray: 5, 5;", class: "arg-split-edge"});
            			} else if (sourceGraph > targetGraph) {
            				graphMap[sourceGraph].setNode("" + edge.source + edge.target + sourceGraph, {label: "", class: "arg-dummy", id: "dummy-" + edge.target});
            				graphMap[sourceGraph].setEdge(edge.source, "" + edge.source + edge.target + sourceGraph, {label: edge.label, id: "arg-edge" + edge.source + edge.target, arrowhead: "undirected", style: "stroke-dasharray: 5, 5;", class: edgeClassDecider(edge)})
            				graphMap[targetGraph].setNode("" + edge.target + edge.source + targetGraph, {label: "", class: "dummy"});
            				graphMap[targetGraph].setEdge("" + edge.target + edge.source + targetGraph, edge.target, {label: "", labelStyle: "font-size: 12px;", id: "arg-edge_" + edge.source + "-" + edge.target, arrowhead: "undirected", style: "stroke-dasharray: 5, 5;", class: "arg-split-edge"});
            			}
        			}
        		});
        	}
        	
        	// Return the graph in which the nodeNumber is present
        	function getGraphForNode(nodeNumber) {
        		return graphMap.findIndex(function(graph) {
        			return graph.nodes().includes("" + nodeNumber);
        		})
        	}
        	
        	// Return the graph in which the nodeNumber is present for an error node
        	function getGraphForErrorNode(nodeNumber) {
        		return errorGraphMap.findIndex(function(graph) {
        			return graph.nodes().includes("" + nodeNumber);
        		})
        	}
    		
        	// create and return a graph element with a set transition
        	function createGraph() {
        		var g = new dagreD3.graphlib.Graph().setGraph({}).setDefaultEdgeLabel(
        				function() {
        					return {};
        				});
        		return g;
        	}
        	
        	// Set nodes for the graph contained in the json nodes
        	function setGraphNodes(graph, nodesToSet) {
        		nodesToSet.forEach(function(n) {
        			if (n.type === "target" && errorPath !== undefined && !errorPath.includes(n.index)) {
        				errorPath.push(n.index);
        			}
        			graph.setNode(n.index, {
        				label : n.label,
        				class : "arg-node " + n.type,
        				id : nodeIdDecider(n)
        			});
        		});
        	}

            function nodeIdDecider(node) {
                if (errorGraphMap === undefined)
                    return "arg-node" + node.index;
                else 
                    return "arg-error-node" + node.index;
            }
        	
        	// Set the graph edges 
        	function setGraphEdges(graph, edgesToSet, multigraph) {
            	edgesToSet.forEach(function(e) {
            		if (!multigraph || (graph.nodes().includes("" + e.source) && graph.nodes().includes("" + e.target))) {
                		graph.setEdge(e.source, e.target, {
                			label: e.label,
                			lineInterpolate: "basis",
                			class: edgeClassDecider(e),
                			id: "arg-edge"+ e.source + e.target,
                			weight: edgeWeightDecider(e)
                		});
            		}
            	});
        	}
        	
        	// Set class for passed edge
        	function edgeClassDecider(edge) {
        		if (errorPath !== undefined && errorPath.includes(edge.source) && errorPath.includes(edge.target)) {
        			return "arg-edge error-edge";
        		} else {
        			return "arg-edge";
        		}
        	}
        	
        	// Decide the weight for the edges based on type
        	function edgeWeightDecider(edge) {
        		if (edge.type === "covered") return 0;
        		return 1;
        	}
    		
    	}
    }

	// ======================= Create CFA and ARG Worker Listeners =======================
	/**
	 * Create workers using blobs due to Chrome's default security policy and 
	 * the need of having a single file at the end that can be send i.e. via e-mail
	 */
	cfaWorker = new Worker(URL.createObjectURL(new Blob(["("+cfaWorker_function.toString()+")()"], {type: 'text/javascript'})));
	if (argJson.nodes) {
		argWorker = new Worker(URL.createObjectURL(new Blob(["("+argWorker_function.toString()+")()"], {type: "text/javascript"})));
	}

	cfaWorker.addEventListener("message", function(m) {
		if (m.data.graph !== undefined) {
			// id was already processed
			if (!d3.select("#cfa-graph-" + m.data.id).empty()) {
				cfaWorker.postMessage({"renderer" : "ready"});
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
			cfaWorker.postMessage({"renderer" : "ready"});
		} else if (m.data.status !== undefined) {
			addEventsToCfa();
			d3.select("#cfa-toolbar").style("visibility", "visible");
			d3.select("#cfa-container").classed("cfa-content", true);
			d3.selectAll(".cfa-svg").each(function(d, i) {
				d3.select(this).attr("width", Math.max(d3.select(this).attr("width"), d3.select(this.parentNode).style("width").split("px")[0]));
			});
			d3.selectAll(".cfa-graph").style("visibility", "visible");
			if (cfaSplit) {
				$("#renderStateModal").modal("hide");
			} else {
				if (!argTabDisabled) {
					argWorker.postMessage({"renderer" : "ready"});
				} else {
					$("#renderStateModal").modal("hide");
				}
			}
		}
	}, false);
	
	cfaWorker.addEventListener("error", function(e) {
		alert("CFA Worker failed in line " + e.lineno + " with message " + e.message)
	}, false);

	// Initial postMessage to the CFA worker to trigger CFA graph(s) creation
	cfaWorker.postMessage({"json" : JSON.stringify(cfaJson)});

	// ONLY if ARG data is available
	if (argJson.nodes) {
		argWorker.addEventListener('message', function(m) {
			if (m.data.graph !== undefined) {
				var id = "arg-graph" + m.data.id; 
				var argClass = "arg-graph";
				if (m.data.errorGraph !== undefined) {
					id = "arg-error-graph" + m.data.id;
					argClass = "arg-error-graph";
					d3.select("#arg-modal-error").style("display", "inline");
					$("#renderStateModal").modal("show");
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
				d3.selectAll(".arg-node tspan").each(function(d,i) {
					d3.select(this).attr("dx", Math.abs(d3.transform(d3.select(this.parentNode.parentNode).attr("transform")).translate[0]));
				})
				if (m.data.errorGraph !== undefined) {
					addEventsToArg();
					$("#renderStateModal").modal("hide");
					argWorker.postMessage({"errorGraph": true});
				} else {
					$("#arg-modal").text(parseInt($("#arg-modal").text().split("/")[0]) + 1 + "/" + $("#arg-modal").text().split("/")[1]);
					argWorker.postMessage({"renderer" : "ready"});
				}
			} else if (m.data.status !== undefined) {
				if ($("#report-controller").scope().getTabSet() === 2) {
					d3.select("#arg-toolbar").style("visibility", "visible");
					d3.select("#arg-container").classed("arg-content", true);
					d3.selectAll(".arg-graph").style("visibility", "visible");
					$("#arg-container").scrollLeft(d3.select(".arg-svg").attr("width")/4);
				}
				addEventsToArg();
				if (errorPath !== undefined) {
					d3.selectAll("td.disabled").classed("disabled", false);
					if (!d3.select(".make-pretty").classed("prettyprint")) {
						d3.selectAll(".make-pretty").classed("prettyprint", true);
						PR.prettyPrint();
					}
				}
				$("#renderStateModal").modal("hide");
			}
		}, false);
		
		argWorker.addEventListener("error", function(e) {
			alert("ARG Worker failed in line " + e.lineno + " with message " + e.message)
		}, false);
		
		// Initial postMessage to the ARG worker to trigger ARG graph(s) creation
		if (errorPath !== undefined) {
			argWorker.postMessage({"errorPath" : JSON.stringify(errorPath)});
		}
		argWorker.postMessage({"json" : JSON.stringify(argJson)});
	}
	
	// create and return a graph element with a set transition
	function createGraph() {
		var g = new dagreD3.graphlib.Graph().setGraph({}).setDefaultEdgeLabel(
				function() {
					return {};
				});
		return g;
	}
	
    // Retrieve the node in which this node was merged - used for the node events
	// FIXME: this is a duplicate function already contained in error path controller, currently no better way to call it
    function getMergingNode(index) {
        var result = "";
        Object.keys(cfaJson.combinedNodes).some(function(key) {
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
		d3.selectAll(".cfa-node").on("mouseover", function(d) { 
			var message;
			if (parseInt(d) > 100000) {
				message = "type: function call node <br>" + "dblclick: Select function";
			} else {
				var node = cfaJson.nodes.find(function(n) {
					return n.index === parseInt(d);
				});
				message = "function: " + node.func;
				if (d in cfaJson.combinedNodes) {
					message += "<br> combines nodes: " + Math.min.apply(null, cfaJson.combinedNodes[d]) + "-" + Math.max.apply(null, cfaJson.combinedNodes[d]);
				}
				message += "<br> reverse postorder Id: " + node.rpid;
			}
			showToolTipBox(d3.event, message); 
		}).on("mouseout", function() { 
			hideToolTipBox(); 
		});
		d3.selectAll(".fcall").on("dblclick", function(d) {
			$("#cfa-toolbar").scope().selectedCFAFunction = d3.select("#cfa-node" + d + " text").text();
			$("#cfa-toolbar").scope().setCFAFunction();
		});
		d3.selectAll(".cfa-dummy").on("mouseover", function(d) {
			showToolTipBox(d3.event, "type: placeholder <br> dblclick: jump to Target node");
		}).on("mouseout", function() {
			hideToolTipBox();
		}).on("dblclick", function() {
			if (!d3.select(".marked-cfa-node").empty()) {
				d3.select(".marked-cfa-node").classed("marked-cfa-node", false);
			}
			var selection = d3.select("#cfa-node" + d3.select(this).attr("id").split("-")[1]);
			selection.classed("marked-cfa-node", true);
			var boundingRect = selection.node().getBoundingClientRect();
			$("#cfa-container").scrollTop(boundingRect.top + $("#cfa-container").scrollTop() - 200).scrollLeft(boundingRect.left + $("#cfa-container").scrollLeft() - $("#errorpath_section").width() - 2 * boundingRect.width);
		})
		d3.selectAll(".cfa-edge")
			.on("mouseover", function(d) { 
				d3.select(this).select("path").style("stroke-width", "3px");
				showToolTipBox(d3.event, "dblclick: jump to Source line"); 
			}).on("mouseout", function() { 
				d3.select(this).select("path").style("stroke-width", "1.5px");
				hideToolTipBox(); 
			}).on("dblclick", function(d) {
				var edge = findCfaEdge(d);
				if (edge === undefined) { // this occurs for edges between graphs - splitting edges
					var thisEdgeData = d3.select(this).attr("id").split("_")[1];
					edge = findCfaEdge({ v: thisEdgeData.split("-")[0], w: thisEdgeData.split("-")[1]})
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
			.on("mouseover", function(d) { 
				d3.select(this).select("path").style("stroke-width", "3px");
				showToolTipBox(d3.event, "type: place holder <br> dblclick: jump to Original edge");
			}).on("mouseout", function() { 
			d3.select(this).select("path").style("stroke-width", "1.5px"); 
				hideToolTipBox();
			}).on("dblclick", function() {
				var edgeSourceTarget = d3.select(this).attr("id").split("_")[1];
				if (!d3.select(".marked-cfa-edge").empty()) {
					d3.select(".marked-cfa-edge").classed("marked-cfa-edge", false);
				}
				var selection = d3.select("#cfa-edge_" + edgeSourceTarget.split("-")[0] + "-" + edgeSourceTarget.split("-")[1]);
				selection.classed("marked-cfa-edge", true);
				var boundingRect = selection.node().getBoundingClientRect();
				$("#cfa-container").scrollTop(boundingRect.top + $("#cfa-container").scrollTop() - 200).scrollLeft(boundingRect.left + $("#cfa-container").scrollLeft() - $("#errorpath_section").width() - 2 * boundingRect.width);
			})
	}
	
	// Find and return the actual edge element from cfaJson.edges array by considering funcCallEdges and combinedNodes
	function findCfaEdge(eventElement) {
		var source = parseInt(eventElement.v);
		var target = parseInt(eventElement.w);
		if (source > 100000) {
			source = Object.keys(cfaJson.functionCallEdges).find(function(key) {
				if (cfaJson.functionCallEdges[key].includes(source)) {
					return key;
				}
			});
		}
		if (target > 100000) {
			target = cfaJson.functionCallEdges[eventElement.v][1];
		}
		if (source in cfaJson.combinedNodes) {
			source = cfaJson.combinedNodes[source][cfaJson.combinedNodes[source].length -1];
		}
		return cfaJson.edges.find(function(e) {
			return e.source === parseInt(source) && e.target === target;
		})
	}
	
	// Add desired events to ARG the nodes
	function addEventsToArg() {
		addPanEvent(".arg-svg");
		d3.selectAll(".arg-node")
		.on("mouseover", function(d) {
			var node = argJson.nodes.find(function(it) {
				return it.index === parseInt(d);
			})
			var message = "function: " + node.func + "<br>";
			if (node.type) {
				message += "type: " + node.type + "<br>";
			}
			message += "dblclick: jump to CFA node";
			showToolTipBox(d3.event, message); 
		}).on("mouseout", function() { 
			hideToolTipBox(); 
		}).on("dblclick", function() {
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
			$("#cfa-container").scrollTop(boundingRect.top + $("#cfa-container").scrollTop() - 200).scrollLeft(boundingRect.left + $("#cfa-container").scrollLeft() - $("#errorpath_section").width() - 2 * boundingRect.width);
		});
		d3.selectAll(".arg-dummy")
			.on("mouseover", function(d) {
				showToolTipBox(d3.event, "type: placeholder <br> dblclick: jump to Target node");
			}).on("mouseout", function() {
				hideToolTipBox();
			}).on("dblclick", function() {
				if (!d3.select(".marked-arg-node").empty()) {
					d3.select(".marked-arg-node").classed("marked-arg-node", false);
				}
				var selection = d3.select("#arg-node" + d3.select(this).attr("id").split("-")[1]);
				selection.classed("marked-arg-node", true);
				var boundingRect = selection.node().getBoundingClientRect();
				$("#arg-container").scrollTop(boundingRect.top + $("#arg-container").scrollTop() - 200).scrollLeft(boundingRect.left + $("#arg-container").scrollLeft() - $("#errorpath_section").width() - 2 * boundingRect.width);
			});
		d3.selectAll(".arg-edge")
			.on("mouseover", function(d) {
				d3.select(this).select("path").style("stroke-width", "3px");
				var edge = argJson.edges.find(function(it) {
					return it.source === parseInt(d.v) && it.target === parseInt(d.w);
				})
				if (edge) {
					showToolTipBox(d3.event, "type: " + edge.type);
				} else {
					showToolTipBox(d3.event, "type: graph connecting edge")
				}
			}).on("mouseout", function() {
				d3.select(this).select("path").style("stroke-width", "1.5px");
				hideToolTipBox();
			});
		d3.selectAll(".arg-split-edge")
			.on("mouseover", function(d) { 
				d3.select(this).select("path").style("stroke-width", "3px");
				showToolTipBox(d3.event, "type: place holder <br> dblclick: jump to Original edge");
			}).on("mouseout", function() { 
				d3.select(this).select("path").style("stroke-width", "1.5px"); 
				hideToolTipBox();
			}).on("dblclick", function() {
				var edgeSourceTarget = d3.select(this).attr("id").split("_")[1];
				if (!d3.select(".marked-arg-edge").empty()) {
					d3.select(".marked-arg-edge").classed("marked-arg-edge", false);
				}
				var selection = d3.select("#arg-edge" + edgeSourceTarget.split("-")[0] + edgeSourceTarget.split("-")[1]);
				selection.classed("marked-arg-edge", true);
				var boundingRect = selection.node().getBoundingClientRect();
				$("#arg-container").scrollTop(boundingRect.top + $("#arg-container").scrollTop() - 200).scrollLeft(boundingRect.left + $("#arg-container").scrollLeft() - $("#errorpath_section").width() - 2 * boundingRect.width);
			});
	}
	
	// Use D3 zoom behavior to add pan event
	function addPanEvent(itemsToSelect) {
		d3.selectAll(itemsToSelect).each(function(d, i) {
			var svg = d3.select(this), svgGroup = d3.select(this.firstChild);
			var zoom = d3.behavior.zoom().on("zoom", function() {
				svgGroup.attr("transform", "translate("
						+ d3.event.translate + ")" + "scale("
						+ d3.event.scale + ")");
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
		d3.select("#infoBox").style("left", function() {
			return positionX + offsetX + "px";
		}).style("top", function() {
			return positionY + offsetY + "px";
		}).style("visibility", "visible");
	}

	// On mouse out hide the tool tip box
	function hideToolTipBox() {
		d3.select("#infoBox").style("visibility", "hidden");
	}

}
