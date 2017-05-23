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
			$scope.functionCallEdges = functionCallEdges;
			$scope.errorPath = errorPath;
			$scope.graphSplitThreshold = graphSplitThreshold
			$scope.zoomEnabled = zoomEnabled;
			$scope.constants = constants;
		}]);
	
})();

var argJson={};//ARG_JSON_INPUT

var sourceFiles = []; //SOURCE_FILES
var cfaJson={};//CFA_JSON_INPUT

// CFA graph variable declarations
var nodes = cfaJson.nodes;
var edges = cfaJson.edges;
var functions = cfaJson.functionNames; //TODO: display only the graph for selected function
var functionCallEdges = cfaJson.functionCallEdges;
var errorPath;
if (cfaJson.hasOwnProperty("errorPath"))
	erroPath = cfaJson.errorPath;
var graphSplitThreshold = 700; // TODO: allow user input with max value 900
var zoomEnabled = false; // TODO: allow user to switch between zoom possibilities
// Graphs already rendered in the master script
var renderedGraphs = {};
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

function init() {
	// ======================= Define CFA and ARG Workers logic =======================
	/**
	 * The CFA Worker. Contains the logic for building a single or a multi CFA graph.
	 * The graph(s) is/are returned to the main script once created
	 */
	function cfaWorker_function() {
		self.importScripts("http://d3js.org/d3.v3.min.js", "https://cdn.rawgit.com/cpettitt/dagre-d3/2f394af7/dist/dagre-d3.min.js");
		var json, nodes, edges, functions, combinedNodes, inversedCombinedNodes, combinedNodesLabels, mergedNodes, functionCallEdges, errorPath;
		var requiredGraphs = 1;
		var graphMap = {};
		var graphSplitThreshold = 700; // default value
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
    			if (nodes.length > graphSplitThreshold) {
    				buildMultipleGraphs();
    			} else {
    				buildSingleGraph();
    			}
    		}
		}, false);
    	
		// Extract information from the cfaJson
    	function extractVariables() {
			nodes = json.nodes;
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
    	
    	function buildSingleGraph() {
    		var g = createGraph();
    		setGraphNodes(g, nodes);
    		roundNodeCorners(g);
    		setGraphEdges(g, edges, false);
    		self.postMessage({"graph" : JSON.stringify(g), "id" : 1});
    	}
    	
    	function buildMultipleGraphs() {
    		requiredGraphs = Math.ceil(nodes.length/graphSplitThreshold);
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
    			roundNodeCorners(graph);
    			var graphEdges = edges.filter(function(e) {
    				if ( (nodesPerGraph[0].index <= e.source && e.source <= nodesPerGraph[nodesPerGraph.length - 1].index ) &&
    						(nodesPerGraph[0].index <= e.target && e.target <= nodesPerGraph[nodesPerGraph.length - 1].index) ) {
    					return e;
    				}
    			});
    			setGraphEdges(graph, graphEdges, true);
    		}
    		// TODO: reverseArrowHead + styles
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
    		// Send each graph to the main script
    		for (var i = 1; i <= requiredGraphs; i++) {
    			self.postMessage({"graph" : JSON.stringify(graphMap[Object.keys(graphMap)[i - 1]]), "id" : i});
    		}
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
    			if ((source !== undefined && target !== undefined) && (source !== target)) {
    				graph.setEdge(source, target, {
    					label: e.stmt, 
    					class: e.type, 
    					id: "edge"+ source + target,
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
    	
    	// Decide the weight for the edges based on type
    	function edgeWeightDecider(edge) {
    		if (edge.type === constants.functionCallEdge)
    			return 1;
    		else
    			return 10;
    	}
    	
	}

	/**
	 * The ARG Worker. Contains the logic for creating a single or multi ARG graph.
	 * Once the graph(s) is/are created they are returned to the main script. 
	 */
	function argWorker_function() {
		self.importScripts("http://d3js.org/d3.v3.min.js", "https://cdn.rawgit.com/cpettitt/dagre-d3/2f394af7/dist/dagre-d3.min.js");
		self.addEventListener("message", function(m) {
			console.log("from inside arg worker");
			self.postMessage(m.data);
		}, false);
	}

	// ======================= Create CFA and ARG Workers =======================
	/**
	 * Create workers using blobs due to Chrome's default security policy and 
	 * the need of having a single file at the end that can be send i.e. via e-mail
	 */
	var cfaWorker = new Worker(URL.createObjectURL(new Blob(["("+cfaWorker_function.toString()+")()"], {type: 'text/javascript'})));
	var argWorker = new Worker(URL.createObjectURL(new Blob(["("+argWorker_function.toString()+")()"], {type: "text/javascript"})));

	cfaWorker.addEventListener("message", function(m) {
		if (m.data.graph !== undefined) {
			if (renderedGraphs[m.data.id] === undefined) {
				var id = m.data.id;
				d3.select("#cfa-container").append("div").attr("id", "cfa-graph" + id).attr("class", "cfa-graph");
				var g = createGraph();
				g = Object.assign(g, JSON.parse(m.data.graph));
				renderedGraphs[m.data.id] = g;
				var svg = d3.select("#cfa-graph" + id).append("svg").attr("id", "cfa-svg" + id), svgGroup = svg.append("g");
				render(d3.select("#cfa-svg" + id + " g"), g);
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
				addEventsToNodes();
				addEventsToEdges();
			} else {
				// This might be needed for re-rendering after user interaction
				renderedGraphs[m.data.id] = Object.assign(renderedGraphs[m.data.id], JSON.parse(m.data.graph));
				render(d3.select("#cfa-svg" + id + " g"), renderedGraphs[m.data.id]);
				var width = renderedGraphs[m.data.id].graph().width * 2 + constants.margin * 2;
				d3.select("#cfa-svg" + id).attr("height", renderedGraphs[m.data.id].graph().height + constants.margin * 2).attr("width", width);
				d3.select("#cfa-svg" + id + "> g").attr("transform", "translate(" + width / 2 + ", 20)");
				addEventsToNodes();
				addEventsToEdges();
			}
		}
	}, false);
	
	cfaWorker.addEventListener("error", function(e) {
		alert("CFA Worker failed in line " + e.lineno + " with message " + e.message)
	}, false);

	// Initial postMessage to the CFA worker to trigger CFA graph(s) creation
	cfaWorker.postMessage({"json" : JSON.stringify(cfaJson)});

	argWorker.addEventListener('message', function(m) {
		console.log("ARG Worker said: ", m.data);
	}, false);
	argWorker.addEventListener("error", function(e) {
		alert("ARG Worker failed in line " + e.lineno + " with message " + e.message)
	}, false);

	argWorker.postMessage("Hello ARG Worker");
	
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

	// Add desired events to the nodes
	function addEventsToNodes() {
		d3.selectAll("svg g.node").on("mouseover", function(d){showInfoBoxNode(d3.event, d);})
			.on("mouseout", function(){hideInfoBoxNode();})
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
