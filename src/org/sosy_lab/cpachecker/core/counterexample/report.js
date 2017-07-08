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
				$scope.help_content = "<p>I am currently being developed</p>";
		        $scope.help_errorpath = "<p>The errorpath leads to the error 'line by line' (Source) or 'edge by edge' (CFA)</p>\n" +
	            "<p><b>-V-</b> Click to show all initialized variables and their values at that point</p>\n" +
	            "<p><b>Edge-Description (Source-Code-View)</b> Click to jump to the relating edge in the CFA / node in the ARG / line in Source (depending on active tab)</p>\n" +
	            "<p><b>Buttons (Prev, Start, Next)</b> Click to navigate through the errorpath and jump to the relating position in the active tab</p>\n" +
	            "<p><b>Search</b>\n - You can search for words or numbers in the edge-descriptions (matches appear blue)\n" +
	            "- You can search for value-assignments (variable names or their value) - it will highlight only where a variable has been initialized or where it has changed its value (matches appear green)</p>";
				$scope.tab = 1;
				$scope.cfaLoaderDone = cfaLoaderDone;
				$scope.argLoaderDone = argLoaderDone;
				$scope.$on("ChangeTab", function(event, tabIndex) {
					$scope.setTab(tabIndex);
				});
				$scope.setTab = function(tabIndex) {
					if (tabIndex === 1) {
						if (d3.select("#arg-loader").style("display") !== "none") {
							d3.select("#arg-loader").style("display", "none");
						}
						if (d3.select("#arg-toolbar").style("visibility") !== "hidden") {
							d3.select("#arg-toolbar").style("visibility", "hidden");
							d3.selectAll(".arg-graph").style("visibility", "hidden");
							d3.selectAll(".arg-error-graph").style("visibility", "hidden");
							if (d3.select("#arg-container").classed("arg-content")) {
								d3.select("#arg-container").classed("arg-content", false);
							}
						}
						if (cfaLoaderDone) {
							d3.select("#cfa-toolbar").style("visibility", "visible");
							if (!d3.select("#cfa-container").classed("cfa-content")) {
								d3.select("#cfa-container").classed("cfa-content", true);
							}
							d3.selectAll(".cfa-graph").style("visibility", "visible");
						} else {
							d3.select("#cfa-loader").style("display", "inline");
						}
					} else if (tabIndex === 2) {
						if (d3.select("#cfa-loader").style("display") !== "none") {
							d3.select("#cfa-loader").style("display", "none");
						}
						if (d3.select("#cfa-toolbar").style("visibility") !== "hidden") {
							d3.select("#cfa-toolbar").style("visibility", "hidden");
							d3.selectAll(".cfa-graph").style("visibility", "hidden");
							if (d3.select("#cfa-container").classed("cfa-content")) {
								d3.select("#cfa-container").classed("cfa-content", false);
							}							
						}
						if (argLoaderDone) {
							d3.select("#arg-toolbar").style("visibility", "visible");
							if (!d3.select("#arg-container").classed("arg-content")) {
								d3.select("#arg-container").classed("arg-content", true);
							}
							if ($rootScope.displayedARG.indexOf("error") !== -1) {
								d3.selectAll(".arg-error-graph").style("visibility", "visible");
								$("#arg-container").scrollTop(0).scrollLeft(0);
							} else {
								d3.selectAll(".arg-graph").style("visibility", "visible");
								$("#arg-container").scrollLeft(d3.select(".arg-svg").attr("width")/4);
							}
						} else {
							d3.select("#arg-loader").style("display", "inline");
						}
					} else {
						if (d3.select("#cfa-loader").style("display") !== "none") {
							d3.select("#cfa-loader").style("display", "none");
						}
						if (d3.select("#cfa-toolbar").style("visibility") !== "hidden") {
							d3.select("#cfa-toolbar").style("visibility", "hidden");
							d3.selectAll(".cfa-graph").style("visibility", "hidden");
							if (d3.select("#cfa-container").classed("cfa-content")) {
								d3.select("#cfa-container").classed("cfa-content", false);
							}							
						}						
						if (d3.select("#arg-loader").style("display") !== "none") {
							d3.select("#arg-loader").style("display", "none");
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
		
		function getValues(val) {
            var values = {};
            if(val != "") {
                var singleStatements = val.split("\n");
                for (var i = 0; i < singleStatements.length - 1; i++) {
                    values[singleStatements[i].split("==")[0].trim()] = singleStatements[i].split("==")[1].trim().slice(0,-1);
                }
            }
            return values;
        };
        
        if (errorPath !== undefined) {
        	var indentationlevel = 0;
            for(var i = 0; i < errorPath.length; i++) {
                var errPathElem = errorPath[i];
                //do not show start, return and blank edges
                if (errPathElem.desc.substring(0, "Return edge from".length) != "Return edge from" && errPathElem.desc != "Function start dummy edge" && errPathElem.desc != "") {
                	var newValues = getValues(errPathElem.val);
                    errPathElem["newValDict"] = newValues;
                    errPathElem["valDict"] = {};
                    errPathElem["valString"] = "";
                    if (i > 0) {
                        for (key in $rootScope.errorPath[$rootScope.errorPath.length - 1].valDict) {
                            errPathElem.valDict[key] = $rootScope.errorPath[$rootScope.errorPath.length - 1].valDict[key];
                        }
                    }
                    if (newValues != {}) {
                        for (key in newValues) {
                            errPathElem.valDict[key] = newValues[key];
                        }
                    }
                    // if I do it in one of the for-loops before, I get the new values doubled
                    for (key in errPathElem.valDict){
                        errPathElem.valString = errPathElem.valString + key + ":  " + errPathElem.valDict[key] + "\n";
                    }
                    //add indentation
                    for(var j = 1; j <= indentationlevel; j++) {
                        errPathElem.desc = "   " + errPathElem.desc;
                    }
                    $rootScope.errorPath.push(errPathElem);
                } else if(errPathElem.desc.substring(0, "Return edge from".length) == "Return edge from"){
                    indentationlevel -= 1;
                } else if(errPathElem.desc == "Function start dummy edge"){
                    indentationlevel += 1;
                }
            }        	
        }
		
        $scope.errPathPrevClicked = function() {
        	console.log("Prev button clicked");
        };
        
        $scope.errPathStartClicked = function() {
        	console.log("Start button clicked");
        };
        
        $scope.errPathNextClicked = function() {
        	console.log("Next button clicked");
        };
        
        $scope.clickedErrpathElement = function($event){
        	console.log("clicked error path element");
        	console.log($event);
        };
		
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
        	var searchInput = $(".search-input").val();
        	if (searchInput.trim() != "") {
        		if ($("#optionExactMatch").prop("checked")) {
        			$rootScope.errorPath.forEach(function(it, i) {
                        if (searchInput in it.newValDict && it.desc.trim() == searchInput) {
                            $scope.numOfValueMatches++;
                            $scope.numOfDescriptionMatches++;
                            $("#errpath-" + i + " td")[1].classList.add("markedValueDescElement");
                        } else if (searchInput in it.newValDict) {
                            $scope.numOfValueMatches++;
                            $("#errpath-" + i + " td")[1].classList.add("markedValueElement");
                        } else if (it.desc.trim() == searchInput) {
                            $scope.numOfDescriptionMatches++;
                            $("#errpath-" + i + " td")[1].classList.add("markedDescElement");
                        }
        			})
        		} else {
        			console.log("else")
        			$rootScope.errorPath.forEach(function(it, i) {
        				if (it.val.indexOf(searchInput) !== -1 && it.desc.indexOf(searchInput) !== -1) {
							$scope.numOfValueMatches++;
							$scope.numOfDescriptionMatches++;
							$("#errpath-" + i + " td")[1].classList.add("markedValueDescElement");
        				} else if (it.val.indexOf(searchInput) !== -1) {
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
    		$scope.originalTranslations = {};
    	
    		$scope.setCFAFunction = function() {
    			$("#cfa-container").scrollTop(0).scrollLeft(0);
    			if ($scope.zoomEnabled) {
    				$scope.zoomControl();
    			}
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
    		};
        
    		$scope.cfaFunctionIsSet = function(value){
    			return value === $scope.selectedCFAFunction;
    		};
        
    		$scope.zoomControl = function() {
    			if ($scope.zoomEnabled) {
    				$scope.zoomEnabled = false;
    				d3.select("#cfa-zoom-button").html("<i class='glyphicon glyphicon-unchecked'></i>");
    				d3.select("#cfa-zoom-label").text(" Zoom Disabled ");
    				// revert zoom and remove listeners
    				d3.selectAll(".cfa-svg").each(function(d, i) {
    					var zoom = d3.behavior.zoom().on("zoom", null).on("wheel.zoom", null);
    					d3.select(this).call(zoom);
    					d3.select(this.firstChild).attr("transform", $scope.originalTranslations[i]);
    				});
    			} else {
    				$scope.zoomEnabled = true;
    				d3.select("#cfa-zoom-button").html("<i class='glyphicon glyphicon-ok'></i>");
    				d3.select("#cfa-zoom-label").text(" Zoom Enabled ");
    				d3.selectAll(".cfa-svg").each(function(d, i) {
    					var svg = d3.select(this), svgGroup = d3.select(this.firstChild);
    					$scope.originalTranslations[i] = svgGroup.attr("transform");
    					var zoom = d3.behavior.zoom().on("zoom", function() {
    						svgGroup.attr("transform", "translate("
    								+ d3.event.translate + ")" + "scale("
    								+ d3.event.scale + ")");
    					});        			
    					svg.call(zoom);
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
    			d3.select("#cfa-toolbar").style("visibility", "hidden");
    			cfaLoaderDone = false;
    			d3.select("#cfa-loader").style("display", "inline");
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
	
	app.controller('CFAController', ['$rootScope', '$scope',
		function($rootScope, $scope) {
            
		}]);
	
	app.controller('ARGToolbarController', ['$rootScope', '$scope',
		function($rootScope, $scope) {
			$scope.zoomEnabled = false;
			$scope.argSelections = ["complete"];
			if (errorPath !== undefined) {
				$scope.argSelections.push("error path");
			}
			$rootScope.displayedARG = $scope.argSelections[0];
			$scope.originalTranslations = {};
			
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
    				d3.select("#arg-zoom-label").text(" Zoom Disabled ");
    				// revert zoom and remove listeners
    				d3.selectAll(".arg-svg").each(function(d, i) {
    					var zoom = d3.behavior.zoom().on("zoom", null).on("wheel.zoom", null);
    					d3.select(this).call(zoom);
    					d3.select(this.firstChild).attr("transform", $scope.originalTranslations[i]);
    				});
    			} else {
    				$scope.zoomEnabled = true;
    				d3.select("#arg-zoom-button").html("<i class='glyphicon glyphicon-ok'></i>");
    				d3.select("#arg-zoom-label").text(" Zoom Enabled ");
    				d3.selectAll(".arg-svg").each(function(d, i) {
    					var svg = d3.select(this), svgGroup = d3.select(this.firstChild);
    					$scope.originalTranslations[i] = svgGroup.attr("transform");
    					var zoom = d3.behavior.zoom().on("zoom", function() {
    						svgGroup.attr("transform", "translate("
    								+ d3.event.translate + ")" + "scale("
    								+ d3.event.scale + ")");
    					});        			
    					svg.call(zoom);
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
    			d3.select("#arg-toolbar").style("visibility", "hidden");
    			d3.select("#arg-loader").style("display", "inline");
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
	
	app.controller('ARGController', ['$rootScope', '$scope',
		function($rootScope, $scope) {
			$scope.printIt = function(value) {
				console.log(value);
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
// TODO: different edge weights based on type?
const constants = {
	blankEdge : "BlankEdge",
	assumeEdge : "AssumeEdge",
	statementEdge : "StatementEdge",
	declarationEdge : "DeclarationEdge",
	returnStatementEdge : "ReturnStatementEdge",
	functionCallEdge : "FunctionCallEdge",
	functionReturnEdge : "FunctionReturnEdge",
	callToReturnEdge : "CallToReturnEdge",
	entryNode : "entry",
	exitNode : "exit",
	afterNode : "after",
	beforeNode : "before",
	margin : 20,
}
var cfaWorker, argWorker;
var cfaLoaderDone = false;
var argLoaderDone = false;

function init() {
	
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
        self.importScripts("http://d3js.org/d3.v3.min.js", "https://cdn.rawgit.com/cpettitt/dagre-d3/2f394af7/dist/dagre-d3.min.js");
        var json, nodes, mainNodes, edges, functions, combinedNodes, inversedCombinedNodes, combinedNodesLabels, mergedNodes, functionCallEdges, errorPath;
        var graphSplitThreshold = 700; // default value
        var graphMap = [];
        var graphCounter = 0;
        // TODO: different edge weights based on type?
        const constants = {
            blankEdge : "BlankEdge",
            assumeEdge : "AssumeEdge",
            statementEdge : "StatementEdge",
            declarationEdge : "DeclarationEdge",
            returnStatementEdge : "ReturnStatementEdge",
            functionCallEdge : "FunctionCallEdge",
            functionReturnEdge : "FunctionReturnEdge",
            callToReturnEdge : "CallToReturnEdge",
            entryNode : "entry",
            exitNode : "exit",
            afterNode : "after",
            beforeNode : "before",
            margin : 20,
        }
        // The first posted message will include the cfaJson
        self.addEventListener('message', function(m) {
            if (m.data.json !== undefined) {
                json = JSON.parse(m.data.json);
                extractVariables();
                buildGraphsAndPostResults();
            } else if (m.data.renderer !== undefined) {
                if (graphMap.length > 0) {
                    var node = nodes.find(function(n) {
                        return n.index === parseInt(graphMap[0].nodes()[0]);
                    });
                    self.postMessage({"graph" : JSON.stringify(graphMap[0]), "id" : node.func + graphCounter, "func" : node.func});
                    graphMap.shift();
                    graphCounter++;
                } else {
                    self.postMessage({"status": "done"});
                }
            } else if (m.data.split !== undefined) {
                graphSplitThreshold = m.data.split;
                buildGraphsAndPostResults();
            }
        }, false);
        
        // Extract information from the cfaJson
        function extractVariables() {
            nodes = json.nodes;
            mainNodes = nodes.filter(function(n) {
                return n.func === "main";
            });
            edges = json.edges;
            functions = json.functionNames;
            combinedNodes = json.combinedNodes;
            inversedCombinedNodes = json.inversedCombinedNodes;
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
                buildMultipleGraphs(mainNodes, "main");
            } else {
                buildSingleGraph(mainNodes, "main");
            }
            if (functions.length > 1) {
                var functionsToProcess = functions.filter(function(f){
                    return f !== "main";
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
            if (funcName === "main") {
                self.postMessage({"graph" : JSON.stringify(g), "id" : funcName, "func" : funcName});
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
            if (funcName === "main") {
                self.postMessage({"graph" : JSON.stringify(graphMap[0]), "id" : funcName + graphCounter, "func" : funcName});
                graphMap.shift();
                graphCounter++;
            }
        }
        
        // TODO: reverseArrowHead + styles
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
                        graphMap[sourceGraph].setNode(funcCallNodeId, {label: getNodeLabelFCall(edge.stmt), class: "fcall", id: "cfa-node" + funcCallNodeId, shape: "rect"});
                        graphMap[sourceGraph].setEdge(source, funcCallNodeId, {label: edge.stmt, labelStyle: labelStyleDecider(edge, source, funcCallNodeId), class: edgeClassDecider(edge, source, funcCallNodeId), id: "cfa-edge" + source + funcCallNodeId, weight: edgeWeightDecider(edge)});
                        graphMap[sourceGraph].setNode("" + source + target + sourceGraph, {label: "D", class: "dummy", id: "node" + target});
                        graphMap[sourceGraph].setEdge(funcCallNodeId, "" + source + target + sourceGraph, {label: source + "->" + target, style: "stroke-dasharray: 5, 5;"});
                    } else {
                        graphMap[sourceGraph].setNode("" + source + target + sourceGraph, {label: "D", class: "dummy", id: "node" + target});
                        graphMap[sourceGraph].setEdge(source, "" + source + target + sourceGraph, {label: source + "->" + target, labelStyle: labelStyleDecider(edge, source, "" + source + target + sourceGraph), class: edgeClassDecider(edge, source, "" + source + target + sourceGraph), style: "stroke-dasharray: 5, 5;"});
                    }
                    graphMap[targetGraph].setNode("" + target + source + targetGraph, {label: "D", class: "dummy", id: "node" + source});
                    graphMap[targetGraph].setEdge("" + target + source + targetGraph, target, {label: source + "->" + target, labelStyle: labelStyleDecider(edge, "" + target + source + targetGraph, target), class: edgeClassDecider(edge, "" + target + source + targetGraph, target), style: "stroke-dasharray: 5, 5;"});
                } else if(sourceGraph > targetGraph) { 
                    graphMap[sourceGraph].setNode("" + source + target + sourceGraph, {label: "D", class: "dummy", id: "node" + target});
                    graphMap[sourceGraph].setEdge("" + source + target + sourceGraph, source, {label: source + "->" + target, labelStyle: labelStyleDecider(edge, "" + source + target + sourceGraph, source), class: edgeClassDecider(edge, "" + source + target + sourceGraph, source), arrowhead: "undirected", style: "stroke-dasharray: 5, 5;"})
                    graphMap[targetGraph].setNode("" + target + source + targetGraph, {label: "D", class: "dummy", id: "node" + source});
                    graphMap[targetGraph].setEdge(target, "" + target + source + targetGraph, {label: source + "->" + target, labelStyle: labelStyleDecider(edge, target, "" + target + source + targetGraph), class: edgeClassDecider(edge, target, "" + target + source + targetGraph), arrowhead: "undirected", style: "stroke-dasharray: 5, 5;"});
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
                        class : "cfa-node " + n.type,
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
            else return "N" + nodeIndex + "\n" + node.rpid;
        }
        
        // Decide the shape of the nodes based on type
        function nodeShapeDecider(n) {
            if (n.type === constants.entryNode)
                return "diamond";
            else
                return "rect";
        }
        
        // Round the corners of rectangle shaped nodes
        function roundNodeCorners(graph) {
            graph.nodes().forEach(function(it) {
                var item = graph.node(it);
                item.rx = item.ry = 5;
            });
        }
        
        // Set the edges for a single graph while considering merged nodes and edges between them
        // TODO: different arrowhead for different type + class function AND errorPath
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
                            class : "fcall",
                            id : "cfa-node" + funcCallNodeId,
                            shape : "rect"
                        });
                        graph.setEdge(source, funcCallNodeId, {
                            label: e.stmt,
                            labelStyle: labelStyleDecider(e, source, funcCallNodeId),
                            class: edgeClassDecider(e, source, funcCallNodeId), 
                            id: "cfa-edge"+ source + funcCallNodeId,
                            weight: edgeWeightDecider(e)
                        });
                        graph.setEdge(funcCallNodeId, target, {
                            label: "",
                            labelStyle: labelStyleDecider(e, funcCallNodeId, target),
                            class: edgeClassDecider(e, funcCallNodeId, target), 
                            id: "cfa-edge"+ funcCallNodeId + target,
                            weight: edgeWeightDecider(e)
                        });                     
                    } else {
                        graph.setEdge(source, target, {
                            label: e.stmt,
                            labelStyle: labelStyleDecider(e, source, target),	
                            class: edgeClassDecider(e, source, target), 
                            id: "cfa-edge"+ source + target,
                            weight: edgeWeightDecider(e)
                        });
                    }
                }
            });
        }
        
        // If edge is part of error path, give it a representative class
    	function edgeClassDecider(edge, source, target) {
    		if (errorPath === undefined) {
    			return edge.type;
    		}
			var mergedMatch = errorPath.find(function(entry) {
				return entry.source === source && entry.target === target;
			})
 			var initialMatch = errorPath.find(function(entry) {
				return entry.source === edge.source && entry.target === edge.target;
			}) 
			if (mergedMatch !== undefined || initialMatch !== undefined) {
				return "error-edge";
			} else {
				return edge.type;
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
            Object.keys(inversedCombinedNodes).some(function(key){
                if (key.includes(index)) {
                    result = inversedCombinedNodes[key];
                    return result;
                }
            })
            return result;
        }
        
        // Decide the weight for the edges based on type
        function edgeWeightDecider(edge) {
            if (edge.source === edge.target)
                return 2;
            else
                return 1;
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
	 */
	function argWorker_function() {
		self.importScripts("http://d3js.org/d3.v3.min.js", "https://cdn.rawgit.com/cpettitt/dagre-d3/2f394af7/dist/dagre-d3.min.js");
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
		
		// TODO: reverseArrowHead + styles, Covered by label!
		// Handle graph connecting edges
    	function buildCrossgraphEdges(edges, errorGraph) {
    		edges.forEach(function(edge) {
    			var sourceGraph, targetGraph;
    			if (errorGraph) {
    				sourceGraph = getGraphForErrorNode(edge.source);
    				targetGraph = getGraphForErrorNode(edge.target);
        			if (sourceGraph < targetGraph) { 
        				errorGraphMap[sourceGraph].setNode("" + edge.source + edge.target + sourceGraph, {label: "D", class: "dummy", id: "node" + edge.target});
        				errorGraphMap[sourceGraph].setEdge(edge.source, "" + edge.source + edge.target + sourceGraph, {label: edge.source + "->" + edge.target, style: "stroke-dasharray: 5, 5;", class: edgeClassDecider(edge)});
        				errorGraphMap[targetGraph].setNode("" + edge.target + edge.source + targetGraph, {label: "D", class: "dummy", id: "node" + edge.source});
        				errorGraphMap[targetGraph].setEdge("" + edge.target + edge.source + targetGraph, edge.target, {label: edge.source + "->" + edge.target, style: "stroke-dasharray: 5, 5;", class: edgeClassDecider(edge)});
        			} else if (sourceGraph > targetGraph) {
        				errorGraphMap[sourceGraph].setNode("" + edge.source + edge.target + sourceGraph, {label: "D", class: "dummy", id: "node" + edge.target});
        				errorGraphMap[sourceGraph].setEdge("" + edge.source + edge.target + sourceGraph, edge.source, {label: edge.source + "->" + edge.target, arrowhead: "undirected", style: "stroke-dasharray: 5, 5;", class: edgeClassDecider(edge)})
        				errorGraphMap[targetGraph].setNode("" + edge.target + edge.source + targetGraph, {label: "D", class: "dummy", id: "node" + edge.source});
        				errorGraphMap[targetGraph].setEdge(edge.target, "" + edge.target + edge.source + targetGraph, {label: edge.source + "->" + edge.target, arrowhead: "undirected", style: "stroke-dasharray: 5, 5;", class: edgeClassDecider(edge)});
        			}
    			} else {
        			sourceGraph = getGraphForNode(edge.source);
        			targetGraph = getGraphForNode(edge.target);
        			if (sourceGraph < targetGraph) { 
            			graphMap[sourceGraph].setNode("" + edge.source + edge.target + sourceGraph, {label: "D", class: "dummy", id: "node" + edge.target});
            			graphMap[sourceGraph].setEdge(edge.source, "" + edge.source + edge.target + sourceGraph, {label: edge.source + "->" + edge.target, style: "stroke-dasharray: 5, 5;", class: edgeClassDecider(edge)});
            			graphMap[targetGraph].setNode("" + edge.target + edge.source + targetGraph, {label: "D", class: "dummy", id: "node" + edge.source});
            			graphMap[targetGraph].setEdge("" + edge.target + edge.source + targetGraph, edge.target, {label: edge.source + "->" + edge.target, style: "stroke-dasharray: 5, 5;", class: edgeClassDecider(edge)});
        			} else if (sourceGraph > targetGraph) {
        				graphMap[sourceGraph].setNode("" + edge.source + edge.target + sourceGraph, {label: "D", class: "dummy", id: "node" + edge.target});
        				graphMap[sourceGraph].setEdge("" + edge.source + edge.target + sourceGraph, edge.source, {label: edge.source + "->" + edge.target, arrowhead: "undirected", style: "stroke-dasharray: 5, 5;", class: edgeClassDecider(edge)})
        				graphMap[targetGraph].setNode("" + edge.target + edge.source + targetGraph, {label: "D", class: "dummy", id: "node" + edge.source});
        				graphMap[targetGraph].setEdge(edge.target, "" + edge.target + edge.source + targetGraph, {label: edge.source + "->" + edge.target, arrowhead: "undirected", style: "stroke-dasharray: 5, 5;", class: edgeClassDecider(edge)});
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
    				id : "arg-node" + n.index
    			});
    		});
    	}
    	
    	// Set the graph edges 
    	function setGraphEdges(graph, edgesToSet, multigraph) {
        	edgesToSet.forEach(function(e) {
        		if (!multigraph || (graph.nodes().includes("" + e.source) && graph.nodes().includes("" + e.target))) {
            		graph.setEdge(e.source, e.target, {
            			label: e.label,
            			class: edgeClassDecider(e),
            			id: "arg-edge"+ e.source + e.target,
            			weight: edgeWeightDecider(e)
            		});
        		}
        	});
    	}
    	
    	function edgeClassDecider(edge) {
    		if (errorPath !== undefined && errorPath.includes(edge.source) && errorPath.includes(edge.target)) {
    			return "error-edge";
    		} else {
    			return edge.type;
    		}
    	}
    	
    	// Decide the weight for the edges based on type
    	function edgeWeightDecider(edge) {
    		if (edge.type === "covered") return 0;
    		return 1;
    	}
		
	}
	// ======================= Create CFA and ARG Worker Listeners =======================
	/**
	 * Create workers using blobs due to Chrome's default security policy and 
	 * the need of having a single file at the end that can be send i.e. via e-mail
	 */
	cfaWorker = new Worker(URL.createObjectURL(new Blob(["("+cfaWorker_function.toString()+")()"], {type: 'text/javascript'})));
	argWorker = new Worker(URL.createObjectURL(new Blob(["("+argWorker_function.toString()+")()"], {type: "text/javascript"})));

	cfaWorker.addEventListener("message", function(m) {
		if (m.data.graph !== undefined) {
			var id = m.data.id;
			d3.select("#cfa-container").append("div").attr("id", "cfa-graph-" + id).attr("class", "cfa-graph");
			var g = createGraph();
			g = Object.assign(g, JSON.parse(m.data.graph));
			var svg = d3.select("#cfa-graph-" + id).append("svg").attr("id", "cfa-svg-" + id).attr("class", "cfa-svg " + "cfa-svg-" + m.data.func);
			var svgGroup = svg.append("g");
			render(d3.select("#cfa-svg-" + id + " g"), g);
			// Center the graph - calculate svg.attributes
			svg.attr("height", g.graph().height + constants.margin * 2);
			svg.attr("width", g.graph().width + constants.margin * 4);
			svgGroup.attr("transform", "translate(" + constants.margin * 2 + ", " + constants.margin + ")");
			cfaWorker.postMessage({"renderer" : "ready"});
		} else if (m.data.status !== undefined) {
			cfaLoaderDone = true;
			d3.select("#cfa-loader").style("display", "none");
			if($("#report-controller").scope().getTabSet() === 1) {
				d3.select("#cfa-toolbar").style("visibility", "visible");
				d3.select("#cfa-container").classed("cfa-content", true);
				d3.selectAll(".cfa-graph").style("visibility", "visible");
			}
			addEventsToNodes(); // TODO: CFA specific!
			addEventsToEdges();
			argWorker.postMessage({"renderer" : "ready"});
		}
	}, false);
	
	cfaWorker.addEventListener("error", function(e) {
		alert("CFA Worker failed in line " + e.lineno + " with message " + e.message)
	}, false);

	// Initial postMessage to the CFA worker to trigger CFA graph(s) creation
	cfaWorker.postMessage({"json" : JSON.stringify(cfaJson)});

	argWorker.addEventListener('message', function(m) {
		if (m.data.graph !== undefined) {
			var id = "arg-graph" + m.data.id; 
			var argClass = "arg-graph";
			if (m.data.errorGraph !== undefined) {
				id = "arg-error-graph" + m.data.id;
				argClass = "arg-error-graph";
			}
			var g = createGraph();
			g = Object.assign(g, JSON.parse(m.data.graph));
			d3.select("#arg-container").append("div").attr("id", id).attr("class", argClass);
			var svg = d3.select("#" + id).append("svg").attr("id", "arg-svg" + id).attr("class", "arg-svg");
			var svgGroup = svg.append("g");
			render(d3.select("#arg-svg" + id + " g"), g);
			// Center the graph - calculate svg.attributes
			svg.attr("height", g.graph().height + constants.margin * 2);
			svg.attr("width", g.graph().width + constants.margin * 4);
			svgGroup.attr("transform", "translate(" + constants.margin * 2 + ", " + constants.margin + ")");
			// FIXME: until https://github.com/cpettitt/dagre-d3/issues/169 is not resolved, label centering like so:
			d3.selectAll(".arg-node tspan").each(function(d,i) {
				d3.select(this).attr("dx", Math.abs(d3.transform(d3.select(this.parentNode.parentNode).attr("transform")).translate[0]));
			})
			if (m.data.errorGraph !== undefined) {
				argWorker.postMessage({"errorGraph": true});
			} else {
				argWorker.postMessage({"renderer" : "ready"});
			}
		} else if (m.data.status !== undefined) {
			argLoaderDone = true;
			d3.select("#arg-loader").attr("display", "none");
			if ($("#report-controller").scope().getTabSet() === 2) {
				d3.select("#arg-toolbar").style("visibility", "visible");
				d3.select("#arg-container").classed("arg-content", true);
				d3.selectAll(".arg-graph").style("visibility", "visible");
			}
			addEventsToNodes(); // TODO: ARG specific!
			addEventsToEdges();
			if (errorPath !== undefined) {
				d3.selectAll("td.disabled").classed("disabled", false);
			}
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
	
	// create and return a graph element with a set transition
	function createGraph() {
		var g = new dagreD3.graphlib.Graph().setGraph({}).setDefaultEdgeLabel(
				function() {
					return {};
				});
		return g;
	}
	
	// Add desired events to the nodes
	function addEventsToNodes() {
		d3.selectAll("svg g.node").on("mouseover", function(d){showInfoBoxNode(d3.event, d);})
			.on("mouseout", function(){hideInfoBoxNode();})
			.on("click", function(d) {
				var scope = angular.element($("#arg-container")).scope();
				scope.$apply(function(){scope.printIt(d)})
			})
		// TODO: node.on("click") needed?
	}

	// Add desired events to edge
	function addEventsToEdges() {
		d3.selectAll("svg g.edgePath").on("mouseover", function(d){showInfoBoxEdge(d3.event, d);})
			.on("mouseout", function(){hideInfoBoxEdge();})
		// TODO: edge.on("click")
	}	
	
	// on mouse over display info box for node
	function showInfoBoxNode(e, nodeIndex) {
		var offsetX = 20;
		var offsetY = 0;
		var positionX = e.pageX;
		var positionY = e.pageY;
//		var node = nodes.find(function(n) {return n.index == nodeIndex;});
//		var displayInfo = "function: " + node.func + "<br/>" + "type: " + node.type;
		var displayInfo = "Tooltip tbd";
		d3.select("#boxContentNode").html("<p>" + displayInfo + "</p>");
		d3.select("#infoBoxNode").style("left", function() {
			return positionX + offsetX + "px";
		}).style("top", function() {
			return positionY + offsetY + "px";
		}).style("visibility", "visible");
	}

	// on mouse out hide the info box for node
	function hideInfoBoxNode() {
		d3.select("#infoBoxNode").style("visibility", "hidden");
	}

	// on mouse over display info box for edge
	function showInfoBoxEdge(e, edgeIndex) {
		var offsetX = 20;
		var offsetY = 0;
		var positionX = e.pageX;
		var positionY = e.pageY;
		// line, file, stmt, type
//		var edge = edges.find(function(e){return e.source == edgeIndex.v && e.target == edgeIndex.w;});
//		var displayInfo = "line: " + edge.line+ "<br/>" + "file: " + edge.file + "<br/>" + "stmt: " + edge.stmt + "<br/>" + "type: " + edge.type;
		var displayInfo = "Tooltip tbd";
		d3.select("#boxContentEdge").html("<p>" + displayInfo + "</p>");
		d3.select("#infoBoxEdge").style("left", function() {
			return positionX + offsetX + "px";
		}).style("top", function() {
			return positionY + offsetY + "px";
		}).style("visibility", "visible");
	}

	// on mouse out hide info box for edge
	function hideInfoBoxEdge() {
		d3.select("#infoBoxEdge").style("visibility", "hidden");
	}		
}
