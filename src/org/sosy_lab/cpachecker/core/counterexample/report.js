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
				$scope.$on("ChangeTab", function(event, tabIndex) {
					$scope.setTab(tabIndex);
				});
				$scope.setTab = function(tabIndex) {
					$scope.tab = tabIndex;
				};
				$scope.tabIsSet = function(tabIndex) {
					return $scope.tab === tabIndex;
				};
			}]);

	app.controller('SourceController', [ '$rootScope', '$scope', '$location',
			'$anchorScroll',
			function($rootScope, $scope, $location, $anchorScroll) {
				// available sourcefiles
				$scope.sourceFiles = sourceFiles;
				$scope.selectedSourceFile = 0;
		        $scope.setSourceFile = function(value){
		            $scope.selectedSourceFile = value;
		        };
		        $scope.sourceFileIsSet = function(value){
		            return value === $scope.selectedSourceFile;
		        };				
			}]);
	
	app.controller('CFAController', ['$rootScope', '$scope',
		function($rootScope, $scope) {
			$scope.nodes = nodes;
			$scope.edges = edges;
			$scope.functions = functions;
			$scope.combinedNodes = combinedNodes;
			$scope.inversedCombinedNodes = inversedCombinedNodes;
			$scope.combinedNodesLabels = combinedNodesLabels;
			$scope.mergedNodes = mergedNodes;
			$scope.functionCallEdges = functionCallEdges;
			$scope.errorPath = errorPath;
			$scope.requiredGraphs = requiredGraphs;
			$scope.graphSplitThreshold = graphSplitThreshold
			$scope.zoomEnabled = zoomEnabled;
			$scope.graphMap = graphMap;
			$scope.constants = constants;
		}]);
	
})();

//ARG_JSON_INPUT

var sourceFiles = []; //SOURCE_FILES
//CFA_JSON_INPUT

// CFA graph variable declarations
var nodes = cfaJson.nodes;
var edges = cfaJson.edges;
var functions = cfaJson.functionNames; //TODO: display only the graph for selected function
var combinedNodes = cfaJson.combinedNodes;
var inversedCombinedNodes = cfaJson.inversedCombinedNodes;
var combinedNodesLabels = cfaJson.combinedNodesLabels;
var mergedNodes = cfaJson.mergedNodes;
var functionCallEdges = cfaJson.functionCallEdges;
var errorPath;
if (cfaJson.hasOwnProperty("errorPath"))
	erroPath = cfaJson.errorPath;
var requiredGraphs = 1;
var graphSplitThreshold = 700; // TODO: allow user input with max value 900
var zoomEnabled = false; // TODO: allow user to switch between zoom possibilities
// Map holding last element in a graph and the graph itself used to handle edges between graphs
var graphMap = {};

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

function init() {
	if (nodes.length > graphSplitThreshold) {
		buildMultipleGraphs();
	} else {
		buildSingleGraph();
	}
	
	function buildMultipleGraphs() {
		requiredGraphs = Math.ceil(nodes.length/graphSplitThreshold);
		var firstGraphBuild = false;
		var nodesPerGraph = [];
		var render = new dagreD3.render();
		for (var i = 1; i <= requiredGraphs; i++) {
			d3.select("#cfa-container").append("div").attr("id", "graph" + i);
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
			roundNodeCorners(graph);
			var graphEdges = edges.filter(function(e) {
				if ( (nodesPerGraph[0].index <= e.source && e.source <= nodesPerGraph[nodesPerGraph.length - 1].index ) &&
						(nodesPerGraph[0].index <= e.target && e.target <= nodesPerGraph[nodesPerGraph.length - 1].index) ) {
					return e;
				}
			});
			setGraphEdges(graph, graphEdges, true);
			var svg = d3.select("#graph" + i).append("svg").attr("id", "svg" + i), svgGroup = svg
					.append("g");
			render(d3.select("#svg" + i + " g"), graph);
			svg.attr("height", graph.graph().height + constants.margin * 2);
			svg.attr("width", graph.graph().width * 2 + constants.margin * 2);
			var xCenterOffset = (svg.attr("width") / 2);
			svgGroup.attr("transform", "translate(" + xCenterOffset + ", 20)");
		}
		addEventsToNodes();
		addEventsToEdges();
		
		// TODO: reverseArrowHead + addEvents + styles
		edges.forEach(function(edge){
			var source = edge.source;
			var target = edge.target;
			if (mergedNodes.includes(source) && mergedNodes.includes(target)) return;
			if (mergedNodes.includes(source)) source = getMergingNode(source);
			if (mergedNodes.includes(target)) target = getMergingNode(target);
			var sourceGraph = getGraphForNode(source);
			var targetGraph = getGraphForNode(target);
			if (sourceGraph !== targetGraph && sourceGraph < targetGraph) {
				graphMap[sourceGraph].setNode(source + "" + sourceGraph, {label: "D", class: "dummy", id: "node" + target});
				graphMap[sourceGraph].setEdge(source, source + "" + sourceGraph, {label: source + "->" + target, style: "stroke-dasharray: 5, 5;"});
				graphMap[targetGraph].setNode(target + "" + targetGraph, {label: "D", class: "dummy", id: "node" + source});
				graphMap[targetGraph].setEdge(target + "" + targetGraph, target, {label: source + "->" + target, arrowhead: "undirected", style: "stroke-dasharray: 5, 5;"});
			}
		});
		// Re-render + resize SVG after graph connecting edges have been placed
		for (var i = 1; i <= requiredGraphs; i++) {
			var graph = graphMap[Object.keys(graphMap)[i - 1]];
			render(d3.select("#svg" + i + " g"), graph);
			var width = graph.graph().width * 2 + constants.margin * 2;
			d3.select("#svg" + i).attr("height", graph.graph().height + constants.margin * 2).attr("width", width);
			d3.select("#svg" + i + "> g").attr("transform", "translate(" + width / 2 + ", 20)");
		}
	}
	
	// Return the graph in which the nodeNumber is present
	function getGraphForNode(nodeNumber) {
		return Object.keys(graphMap).find(function(key) { return key >= nodeNumber;});
	}
	
	// build single graph for all contained nodes
	function buildSingleGraph() {
		d3.select("#cfa-container").append("div").attr("id", "graph");
		// Create the graph
		var g = createGraph();
		setGraphNodes(g, nodes);
		roundNodeCorners(g);
		setGraphEdges(g, edges, false);
		// Create the renderer
		var render = new dagreD3.render();
		// Set up an SVG group so that we can translate the final graph.
		var svg = d3.select("#graph").append("svg").attr("id", "svg"), svgGroup = svg
				.append("g");
		// Set up zoom support 
		//TODO: own function pass the svgGroup -> zoom can be set later (after svg is rendered)
		var zoom = d3.behavior.zoom().on(
				"zoom",
				function() {
					svgGroup.attr("transform", "translate("
							+ d3.event.translate + ")" + "scale("
							+ d3.event.scale + ")");
				});
		svg.call(zoom);
		// Run the renderer. This is what draws the final graph.
		render(d3.select("#svg g"), g);
		// Center the graph - calculate svg.attributes
		// TODO: own function for svg size -> compare with parent size after bootstrap is used. i.e. col-lg-12 etc.
		svg.attr("height", g.graph().height + constants.margin * 2);
		svg.attr("width", g.graph().width * 2 + constants.margin * 2);
		var xCenterOffset = (svg.attr("width") / 2);
		svgGroup.attr("transform", "translate(" + xCenterOffset + ", 20)");
		addEventsToNodes();
		addEventsToEdges();
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
					id : "node" + n.index,
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

	// Add desired events to the nodes
	function addEventsToNodes() {
		d3.selectAll("svg g.node").on("mouseover", function(d){showInfoBoxNode(d3.event, d);})
			.on("mouseout", function(){hideInfoBoxNode();})
		// TODO: node.on("click") needed?
	}

	// Round the corners of rectangle shaped nodes
	function roundNodeCorners(graph) {
		graph.nodes().forEach(function(it) {
			var item = graph.node(it);
			// TODO: check if shapes/labels etc. can be updated here
			item.rx = item.ry = 5;
		});
	}

	// Decide the shape of the nodes based on type
	function nodeShapeDecider(n) {
		if (n.type === constants.entryNode)
			return "diamond";
		else
			return "rect";
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
			if ((source !== undefined && target !== undefined) && (source !== target)) {
				graph.setEdge(source, target, {
					label: e.stmt, 
					class: e.type, 
					id: "edge"+ source + target,
					lineInterpolate: 'basis',
					weight: edgeWeightDecider(e)
				});
			}
		});
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

	// Add desired events to edge
	function addEventsToEdges() {
		d3.selectAll("svg g.edgePath").on("mouseover", function(d){showInfoBoxEdge(d3.event, d);})
			.on("mouseout", function(){hideInfoBoxEdge();})
		// TODO: edge.on("click")
	}

	// Decide the weight for the edges based on type
	function edgeWeightDecider(edge) {
		if (edge.type === constants.functionCallEdge)
			return 1;
		else
			return 10;
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
