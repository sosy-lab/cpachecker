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
				$scope.tab = 1;
				$scope.argWorker = argWorker; // TODO: move in ARGToolbarController
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
							if (d3.select("#arg-container").classed("content")) {
								d3.select("#arg-container").classed("content", false);
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
							if (!d3.select("#arg-container").classed("content")) {
								d3.select("#arg-container").classed("content", true);
							}
							d3.selectAll(".arg-graph").style("visibility", "visible");
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
							if (d3.select("#arg-container").classed("content")) {
								d3.select("#arg-container").classed("content", false);
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
	
	app.controller('CFAToolbarController', ['$scope', 
		function($scope) {
    		$scope.functions = ["all"].concat(functions);
    		$scope.selectedCFAFunction = 0;
    		$scope.zoomEnabled = false;
    		$scope.originalTranslations = {};
    	
    		$scope.setCFAFunction = function(value) {
    			$("#cfa-container").scrollTop(0).scrollLeft(0);
    			if ($scope.zoomEnabled) {
    				$scope.zoomEnabled = false;
    				$scope.zoomControl();
    			}
    			$scope.selectedCFAFunction = value;
    			if (value === 0) {
    				functions.forEach(function(func) {
    					d3.selectAll("#cfa-svg-" + func).attr("display", "inline-block");
    				});
    			} else {
    				var funcToHide = $scope.functions.filter(function(it){
    					return it !== $scope.functions[value] && it !== "all";
    				});
    				funcToHide.forEach(function(func){
    					d3.selectAll("#cfa-svg-" + func).attr("display", "none");
    				});
    				d3.selectAll("#cfa-svg-" + $scope.functions[value]).attr("display", "inline-block");
    			}
    		};
        
    		$scope.cfaFunctionIsSet = function(value){
    			return value === $scope.selectedCFAFunction;
    		};
        
    		$scope.zoomControl = function() {
    			if ($scope.zoomEnabled) {
    				$scope.zoomEnabled = false;
    				d3.select("#cfa-zoom-checkbox").html("<i class='glyphicon glyphicon-unchecked'></i>");
    				d3.select("#cfa-zoom-checkbox-label").text(" Zoom Disabled ");
    				// revert zoom and remove listeners
    				d3.selectAll(".cfa-svg").each(function(d, i) {
    					var zoom = d3.behavior.zoom().on("zoom", null);
    					d3.select(this).call(zoom);
    					d3.select(this.firstChild).attr("transform", $scope.originalTranslations[i]);
    				});
    			} else {
    				$scope.zoomEnabled = true;
    				d3.select("#cfa-zoom-checkbox").html("<i class='glyphicon glyphicon-ok'></i>");
    				d3.select("#cfa-zoom-checkbox-label").text(" Zoom Enabled ");
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
	}]);
	
	app.controller('CFAController', ['$rootScope', '$scope',
		function($rootScope, $scope) {
            
		}]);
	
	app.controller('ARGController', ['$rootScope', '$scope',
		function($rootScope, $scope) {
			$scope.printIt = function(value) {
				console.log(value);
			}
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
var graphSplitThreshold = 700; // TODO: allow user input with max value 900
var zoomEnabled = false; // TODO: allow user to switch between zoom possibilities
// Graphs already rendered in the master script
var renderedCfaGraphs = {};
var createdArgGraphs = {};
// A Dagre D3 Renderer
var render = new dagreD3.render();
// TODO: different edge weights based on type?
var constants = {
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
var cfaWorker, argWorker, cfaRefreshInterval, argRefreshInterval;
var cfaLoaderDone = false;
var argLoaderDone = false;

function init() {
	// ======================= Define CFA and ARG Workers logic =======================
	/**
	 * The CFA Worker. Contains the logic for building a single or a multi CFA graph.
	 * The graph(s) is/are returned to the main script once created
	 */
	function cfaWorker_function() {
		self.importScripts("http://d3js.org/d3.v3.min.js", "https://cdn.rawgit.com/cpettitt/dagre-d3/2f394af7/dist/dagre-d3.min.js");
		var json, nodes, mainNodes, edges, functions, combinedNodes, inversedCombinedNodes, combinedNodesLabels, mergedNodes, functionCallEdges, errorPath;
		var graphSplitThreshold = 700; // default value
		var graphMap;
		// TODO: different edge weights based on type?
		var constants = {
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
    				self.postMessage({"status": "done"});
    			} else {
    				self.postMessage({"status": "done"});
    			}
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
			if (json.hasOwnProperty("errorPath"))
				erroPath = json.errorPath;
    	}
    	
    	function buildSingleGraph(nodesToSet, funcName) {
    		var g = createGraph();
    		setGraphNodes(g, nodesToSet);
    		roundNodeCorners(g);
    		var nodesIndexes = [];
    		nodesToSet.forEach(function(n){
    			nodesIndexes.push(n.index);
    		});
    		var edgesToSet = edges.filter(function(e){
    			return nodesIndexes.includes(e.source) && nodesIndexes.includes(e.target);
    		});
    		setGraphEdges(g, edgesToSet, false);
    		self.postMessage({"graph" : JSON.stringify(g), "id" : funcName});
    	}
    	
    	function buildMultipleGraphs(nodesToSet, funcName) {
    		var requiredGraphs = Math.ceil(nodesToSet.length/graphSplitThreshold);
    		var firstGraphBuild = false;
    		var nodesPerGraph = [];
    		graphMap = {};
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
    			graphMap[nodesPerGraph[nodesPerGraph.length - 1].index] = graph;
    			setGraphNodes(graph, nodesPerGraph);
    			roundNodeCorners(graph);
    			var graphEdges = edges.filter(function(e) {
    				if ( (nodesPerGraph[0].index <= e.source && e.source <= nodesPerGraph[nodesPerGraph.length - 1].index ) &&
    						(nodesPerGraph[0].index <= e.target && e.target <= nodesPerGraph[nodesPerGraph.length - 1].index) ) {
    					return e;
    				}
    			});
    			setGraphEdges(graph, graphEdges, true);
    		}
    		buildCrossgraphEdges(nodesToSet);
    		// Send each graph to the main script
    		for (var i = 1; i <= requiredGraphs; i++) {
    			self.postMessage({"graph" : JSON.stringify(graphMap[Object.keys(graphMap)[i - 1]]), "id" : funcName + i});
    		}
    	}
    	
		// TODO: reverseArrowHead + styles
    	// Handle Edges that connect Graphs
    	function buildCrossgraphEdges(crossGraphNodes) {
    		var nodesIndexes = [];
    		crossGraphNodes.forEach(function(n) {
    			nodesIndexes.push(n.index);
    		});
    		var edgesToConsider = edges.filter(function(e) {
    			return nodesIndexes.includes(e.source) && nodesIndexes.includes(e.target);
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
        			graphMap[sourceGraph].setNode("" + source + target + sourceGraph, {label: "D", class: "dummy", id: "node" + target});
        			graphMap[sourceGraph].setEdge(source, "" + source + target + sourceGraph, {label: source + "->" + target, style: "stroke-dasharray: 5, 5;"});
        			graphMap[targetGraph].setNode("" + target + source + targetGraph, {label: "D", class: "dummy", id: "node" + source});
        			graphMap[targetGraph].setEdge("" + target + source + targetGraph, target, {label: source + "->" + target, style: "stroke-dasharray: 5, 5;"});
    			} else if(sourceGraph > targetGraph) { 
    				graphMap[sourceGraph].setNode("" + source + target + sourceGraph, {label: "D", class: "dummy", id: "node" + target});
    				graphMap[sourceGraph].setEdge("" + source + target + sourceGraph, source, {label: source + "->" + target, arrowhead: "undirected", style: "stroke-dasharray: 5, 5;"})
    				graphMap[targetGraph].setNode("" + target + source + targetGraph, {label: "D", class: "dummy", id: "node" + source});
    				graphMap[targetGraph].setEdge(target, "" + target + source + targetGraph, {label: source + "->" + target, arrowhead: "undirected", style: "stroke-dasharray: 5, 5;"});
    			}
    		});
    	}

    	// Return the graph in which the nodeNumber is present
    	function getGraphForNode(nodeNumber) {
    		return Object.keys(graphMap).find(function(key) { return key >= nodeNumber;});
    	}
    	
    	// create and return a graph element with a set transition
    	function createGraph() {
    		var g = new dagreD3.graphlib.Graph().setGraph({}).setDefaultEdgeLabel(
    				function() {
    					return {};
    				});
    		g.graph().transition = function(selection) {
    			return selection.transition().duration(500);
    		};
    		return g;
    	}
    	
    	// Set nodes for the graph contained in the json nodes
    	function setGraphNodes(graph, nodesToSet) {
    		nodesToSet.forEach(function(n) {
    			if (!mergedNodes.includes(n.index)) {
    				graph.setNode(n.index, {
    					label : setNodeLabel(n),
    					class : n.type,
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
    				graph.setEdge(source, target, {
    					label: e.stmt, 
    					class: e.type, 
    					id: "cfa-edge"+ source + target,
    					weight: edgeWeightDecider(e)
    				});
    			}
    		});
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
    		if (edge.type === constants.functionCallEdge)
    			return 0;
    		else
    			return 1;
    	}
    	
	}

	/**
	 * The ARG Worker. Contains the logic for creating a single or multi ARG graph.
	 * Once the graph(s) is/are created they are returned to the main script. 
	 */
	function argWorker_function() {
		self.importScripts("http://d3js.org/d3.v3.min.js", "https://cdn.rawgit.com/cpettitt/dagre-d3/2f394af7/dist/dagre-d3.min.js");
		var json, nodes, edges;
		var graphSplitThreshold = 700; // default TODO: enable change by user
		var graphMap = {};
		self.addEventListener("message", function(m) {
			if (m.data.json !== undefined) {
				json = JSON.parse(m.data.json);
				nodes = json.nodes;
				edges = json.edges;
    			if (nodes.length > graphSplitThreshold) {
    				buildMultipleGraphs();
    			} else {
    				buildSingleGraph();
    			}
			}
		}, false);
		
		function buildSingleGraph() {
			var g = createGraph();
			setGraphNodes(g, nodes);
			setGraphEdges(g, edges, false);
			self.postMessage({"graph" : JSON.stringify(g), "id" : 1});
			self.postMessage({"status": "done"});
		}
		
		function buildMultipleGraphs() {
    		var requiredGraphs = Math.ceil(nodes.length/graphSplitThreshold);
    		var firstGraphBuild = false;
    		var nodesPerGraph = [];
    		for (var i = 1; i <= requiredGraphs; i++) {
    			if (!firstGraphBuild) {
    				nodesPerGraph = nodes.slice(0, graphSplitThreshold);
    				firstGraphBuild = true;
    			} else {
    				if (nodes[graphSplitThreshold * i - 1] !== undefined)
    					nodesPerGraph = nodes.slice(graphSplitThreshold * (i - 1), graphSplitThreshold * i);
    				else
    					nodesPerGraph = nodes.slice(graphSplitThreshold * (i - 1));
    			}
    			var graph = createGraph();
    			graphMap[nodesPerGraph[nodesPerGraph.length - 1].index] = graph;
    			setGraphNodes(graph, nodesPerGraph);
    			var graphEdges = edges.filter(function(e) {
    				if ( (nodesPerGraph[0].index <= e.source && e.source <= nodesPerGraph[nodesPerGraph.length - 1].index ) &&
    						(nodesPerGraph[0].index <= e.target && e.target <= nodesPerGraph[nodesPerGraph.length - 1].index) ) {
    					return e;
    				}
    			});
    			setGraphEdges(graph, graphEdges, true);
    		}
    		buildCrossgraphEdges();
    		// Send each graph to the main script
    		for (var i = 1; i <= requiredGraphs; i++) {
    			self.postMessage({"graph" : JSON.stringify(graphMap[Object.keys(graphMap)[i - 1]]), "id" : i});
    		}
    		self.postMessage({"status": "done"});
		} 
		
		// TODO: reverseArrowHead + styles
		// Handle graph connecting edges
    	function buildCrossgraphEdges() {
    		edges.forEach(function(edge){
    			var sourceGraph = getGraphForNode(edge.source);
    			var targetGraph = getGraphForNode(edge.target);
    			if (sourceGraph < targetGraph) { 
        			graphMap[sourceGraph].setNode("" + edge.source + edge.target + sourceGraph, {label: "D", class: "dummy", id: "node" + edge.target});
        			graphMap[sourceGraph].setEdge(edge.source, "" + edge.source + edge.target + sourceGraph, {label: edge.source + "->" + edge.target, style: "stroke-dasharray: 5, 5;"});
        			graphMap[targetGraph].setNode("" + edge.target + edge.source + targetGraph, {label: "D", class: "dummy", id: "node" + edge.source});
        			graphMap[targetGraph].setEdge("" + edge.target + edge.source + targetGraph, edge.target, {label: edge.source + "->" + edge.target, style: "stroke-dasharray: 5, 5;"});
    			} else if(targetGraph > sourceGraph) {
    				graphMap[sourceGraph].setNode("" + edge.source + edge.target + sourceGraph, {label: "D", class: "dummy", id: "node" + edge.target});
    				graphMap[sourceGraph].setEdge("" + edge.source + edge.target + sourceGraph, edge.source, {label: edge.source + "->" + edge.target, arrowhead: "undirected", style: "stroke-dasharray: 5, 5;"})
    				graphMap[targetGraph].setNode("" + edge.target + edge.source + targetGraph, {label: "D", class: "dummy", id: "node" + edge.source});
    				graphMap[targetGraph].setEdge(edge.target, "" + edge.target + edge.source + targetGraph, {label: edge.source + "->" + edge.target, arrowhead: "undirected", style: "stroke-dasharray: 5, 5;"});
    			}
    		});
    	}
    	
    	// Return the graph in which the nodeNumber is present
    	function getGraphForNode(nodeNumber) {
    		return Object.keys(graphMap).find(function(key) { return key >= nodeNumber;});
    	}
		
    	// create and return a graph element with a set transition
    	function createGraph() {
    		var g = new dagreD3.graphlib.Graph().setGraph({}).setDefaultEdgeLabel(
    				function() {
    					return {};
    				});
    		g.graph().transition = function(selection) {
    			return selection.transition().duration(500);
    		};
    		return g;
    	}
    	
    	// Set nodes for the graph contained in the json nodes
    	function setGraphNodes(graph, nodesToSet) {
    		nodesToSet.forEach(function(n) {
    			graph.setNode(n.index, {
    				label : n.label,
    				class : n.type,
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
            			class: e.type,
            			id: "arg-edge"+ e.source + e.target,
            			weight: edgeWeightDecider(e)
            		});
        		}
        	});
    	}
    	
    	// Decide the weight for the edges based on type
    	function edgeWeightDecider(edge) {
    		if (edge.type === "covered") return 0;
    		return 1;
    	}
		
	}	
	// ======================= Create CFA and ARG Workers =======================
	/**
	 * Create workers using blobs due to Chrome's default security policy and 
	 * the need of having a single file at the end that can be send i.e. via e-mail
	 */
	cfaWorker = new Worker(URL.createObjectURL(new Blob(["("+cfaWorker_function.toString()+")()"], {type: 'text/javascript'})));
	argWorker = new Worker(URL.createObjectURL(new Blob(["("+argWorker_function.toString()+")()"], {type: "text/javascript"})));

	cfaWorker.addEventListener("message", function(m) {
		if (m.data.graph !== undefined) {
			if (renderedCfaGraphs[m.data.id] === undefined) {
				var id = m.data.id;
				d3.select("#cfa-container").append("div").attr("id", "cfa-graph-" + id).attr("class", "cfa-graph");
				var g = createGraph();
				g = Object.assign(g, JSON.parse(m.data.graph));
				renderedCfaGraphs[m.data.id] = g;
				var svg = d3.select("#cfa-graph-" + id).append("svg").attr("id", "cfa-svg-" + id).attr("class", "cfa-svg");
				var svgGroup = svg.append("g");
				render(d3.select("#cfa-svg-" + id + " g"), g);
				//TODO: own function pass the id and use d3.select() to select svg and svgGroup -> zoom can be set later (after svg is rendered)
//				var zoom = d3.behavior.zoom().on(
//						"zoom",
//						function() {
//							svgGroup.attr("transform", "translate("
//									+ d3.event.translate + ")" + "scale("
//									+ d3.event.scale + ")");
//						});
//				svg.call(zoom);			
				// Center the graph - calculate svg.attributes
				// TODO: own function for svg size -> compare with parent size after bootstrap is used. i.e. col-lg-12 etc.
				svg.attr("height", g.graph().height + constants.margin * 2);
				svg.attr("width", g.graph().width * 2 + constants.margin * 2);
				var xCenterOffset = (svg.attr("width") / 2);
				svgGroup.attr("transform", "translate(" + xCenterOffset + ", 20)");
				addEventsToNodes(); // TODO: CFA specific!
				addEventsToEdges();
			} else {
				// This is needed for re-rendering after user interaction
				renderedCfaGraphs[m.data.id] = Object.assign(renderedCfaGraphs[m.data.id], JSON.parse(m.data.graph));
				render(d3.select("#cfa-svg-" + id + " g"), renderedCfaGraphs[m.data.id]);
				var width = renderedCfaGraphs[m.data.id].graph().width * 2 + constants.margin * 2;
				d3.select("#cfa-svg-" + id).attr("height", renderedCfaGraphs[m.data.id].graph().height + constants.margin * 2).attr("width", width);
				d3.select("#cfa-svg-" + id + "> g").attr("transform", "translate(" + width / 2 + ", 20)");
				addEventsToNodes();
				addEventsToEdges();
			}
		} else if (m.data.status !== undefined) {
			clearInterval(cfaRefreshInterval);
			cfaLoaderDone = true;
			d3.select("#cfa-loader").style("display", "none");
			if($("#report-controller").scope().getTabSet() === 1) {
				d3.select("#cfa-toolbar").style("visibility", "visible");
				d3.select("#cfa-container").classed("cfa-content", true);
				d3.selectAll(".cfa-graph").style("visibility", "visible");
			}
		}
	}, false);
	
	cfaWorker.addEventListener("error", function(e) {
		alert("CFA Worker failed in line " + e.lineno + " with message " + e.message)
	}, false);

	// Initial postMessage to the CFA worker to trigger CFA graph(s) creation
	setTimeout(function() {
		cfaWorker.postMessage({"json" : JSON.stringify(cfaJson)});
	}, 2000);

	argWorker.addEventListener('message', function(m) {
		if (m.data.graph !== undefined) {
			if (createdArgGraphs[m.data.id] === undefined) {
				var id = m.data.id;
				var g = createGraph();
				g = Object.assign(g, JSON.parse(m.data.graph));
				createdArgGraphs[m.data.id] = g;
				d3.select("#arg-container").append("div").attr("id", "arg-graph" + id).attr("class", "arg-graph");
				var svg = d3.select("#arg-graph" + id).append("svg").attr("id", "arg-svg" + id).attr("class", "arg-svg");
				var svgGroup = svg.append("g");
				render(d3.select("#arg-svg" + id + " g"), g);
				//TODO: own function pass the id and use d3.select() to select svg and svgGroup -> zoom can be set later (after svg is rendered)
				var zoom = d3.behavior.zoom().on(
						"zoom",
						function() {
							svgGroup.attr("transform", "translate("
									+ d3.event.translate + ")" + "scale("
									+ d3.event.scale + ")");
						});
				svg.call(zoom);			
				// Center the graph - calculate svg.attributes
				// TODO: own function for svg size -> compare with parent size after bootstrap is used. i.e. col-lg-12 etc.
				svg.attr("height", g.graph().height + constants.margin * 2);
				svg.attr("width", g.graph().width * 2 + constants.margin * 2);
				var xCenterOffset = (svg.attr("width") / 2);
				svgGroup.attr("transform", "translate(" + xCenterOffset + ", 20)");
				addEventsToNodes(); // TODO: ARG specific!
				addEventsToEdges();
			} else {
				// This is needed for re-rendering after user interaction
				createdArgGraphs[m.data.id] = Object.assign(createdArgGraphs[m.data.id], JSON.parse(m.data.graph));
				render(d3.select("#arg-svg" + id + " g"), createdArgGraphs[m.data.id]);
				var width = createdArgGraphs[m.data.id].graph().width * 2 + constants.margin * 2;
				d3.select("#arg-svg" + id).attr("height", createdArgGraphs[m.data.id].graph().height + constants.margin * 2).attr("width", width);
				d3.select("#arg-svg" + id + "> g").attr("transform", "translate(" + width / 2 + ", 20)");
				addEventsToNodes();
				addEventsToEdges();
			}
		} else if (m.data.status !== undefined) {
			clearInterval(argRefreshInterval);
			argLoaderDone = true;
			d3.select("#arg-loader").attr("display", "none");
			if($("#report-controller").scope().getTabSet() === 2) {
				d3.select("#arg-toolbar").style("visibility", "visible");
				d3.select("#arg-container").classed("content", true);
				d3.selectAll(".arg-graph").style("visibility", "visible");
			}			
		}
	}, false);
	
	argWorker.addEventListener("error", function(e) {
		alert("ARG Worker failed in line " + e.lineno + " with message " + e.message)
	}, false);
	
	// Initial postMessage to the ARG worker to trigger ARG graph(s) creation
	setTimeout(function() {
		argWorker.postMessage({"json" : JSON.stringify(argJson)});
	}, 2000);
	
	// create and return a graph element with a set transition
	function createGraph() {
		var g = new dagreD3.graphlib.Graph().setGraph({}).setDefaultEdgeLabel(
				function() {
					return {};
				});
		g.graph().transition = function(selection) {
			return selection.transition().duration(500);
		};
		return g;
	}
	
	cfaRefreshInterval = setInterval(function() {
		var loader = $("#cfa-loader");
		if (loader.html().length > 23) {
			loader.html("Rendering CFA Graphs ");
		} else {
			loader.html(loader.html() + ".");
		}
	},150);
	
	argRefreshInterval = setInterval(function() {
		var loader = $("#arg-loader");
		if (loader.html().length > 23) {
			loader.html("Rendering ARG Graphs ");
		} else {
			loader.html(loader.html() + ".");
		}
	},150);

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
		var node = nodes.find(function(n) {return n.index == nodeIndex;});
		var displayInfo = "function: " + node.func + "<br/>" + "type: " + node.type;
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
		var edge = edges.find(function(e){return e.source == edgeIndex.v && e.target == edgeIndex.w;});
		var displayInfo = "line: " + edge.line+ "<br/>" + "file: " + edge.file + "<br/>" + "stmt: " + edge.stmt + "<br/>" + "type: " + edge.type;
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
