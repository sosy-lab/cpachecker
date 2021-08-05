// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/*
 * The ARG Worker. Contains the logic for creating a single or multi ARG graph.
 * Once the graph(s) is/are created they are returned to the main script.
 * Should ONLY be used if ARG data is available!
 */
let json;
let errorPath;
let nodes;
let edges;
let relevantNodes;
let relevantEdges;
let errorGraphMap;
let graphSplitThreshold = 700;
const graphMap = [];
let graphCounter = 0;
let simplifiedGraphMap;
let simplifiedGraphCounter = 0;
let reducedGraphMap;
let reducedGraphCounter = 0;

onmessage = (msg) => {
  const { data } = msg;
  let workerResult = {};

  /* ################## Helper functions ################## */

  // Return the graph in which the nodeNumber is present
  function getGraphForNode(nodeNumber) {
    return graphMap.findIndex((graph) =>
      graph.nodes().includes(`${nodeNumber}`)
    );
  }

  // create and return a graph element with a set transition
  function createGraph() {
    return new dagreD3.graphlib.Graph()
      .setGraph({})
      .setDefaultEdgeLabel(() => {});
  }

  function nodeIdDecider(node) {
    if (errorGraphMap === undefined) return `arg-node${node.index}`;
    return `arg-error-node${node.index}`;
  }

  // Set nodes for the graph contained in the json nodes
  function setGraphNodes(graph, nodesToSet) {
    nodesToSet.forEach((n) => {
      if (
        n.type === "target" &&
        errorPath !== undefined &&
        !errorPath.includes(n.index)
      ) {
        errorPath.push(n.index);
      }
      graph.setNode(n.index, {
        label: n.label,
        class: `arg-node ${n.type}`,
        id: nodeIdDecider(n),
      });
    });
  }

  // Set class for passed edge
  function edgeClassDecider(edge) {
    if (
      errorPath !== undefined &&
      errorPath.includes(edge.source) &&
      errorPath.includes(edge.target)
    ) {
      return "arg-edge error-edge";
    }
    return "arg-edge";
  }

  // Decide the weight for the edges based on type
  function edgeWeightDecider(edge) {
    if (edge.type === "covered") return 0;
    return 1;
  }

  // Set the graph edges
  function setGraphEdges(graph, edgesToSet, multigraph) {
    edgesToSet.forEach((e) => {
      if (
        !multigraph ||
        (graph.nodes().includes(`${e.source}`) &&
          graph.nodes().includes(`${e.target}`))
      ) {
        graph.setEdge(e.source, e.target, {
          label: e.label,
          lineInterpolate: "basis",
          class: edgeClassDecider(e),
          id: `arg-edge${e.source}${e.target}`,
          weight: edgeWeightDecider(e),
        });
      }
    });
  }

  // Return the graph in which the nodeNumber is present for an error node
  function getGraphForErrorNode(nodeNumber) {
    return errorGraphMap.findIndex((graph) =>
      graph.nodes().includes(`${nodeNumber}`)
    );
  }

  // Handle graph connecting edges
  function buildCrossgraphEdges(graphEdges, errorGraph) {
    graphEdges.forEach((edge) => {
      let sourceGraph;
      let targetGraph;
      if (errorGraph) {
        sourceGraph = getGraphForErrorNode(edge.source);
        targetGraph = getGraphForErrorNode(edge.target);
        if (sourceGraph < targetGraph) {
          errorGraphMap[sourceGraph].setNode(
            `${edge.source}${edge.target}${sourceGraph}`,
            {
              label: "",
              class: "arg-dummy",
              id: `dummy-${edge.target}`,
            }
          );
          errorGraphMap[sourceGraph].setEdge(
            edge.source,
            `${edge.source}${edge.target}${sourceGraph}`,
            {
              label: edge.label,
              id: `arg-edge${edge.source}${edge.target}`,
              style: "stroke-dasharray: 5, 5;",
              class: edgeClassDecider(edge),
            }
          );
          errorGraphMap[targetGraph].setNode(
            `${edge.target}${edge.source}${targetGraph}`,
            {
              label: "",
              class: "dummy",
            }
          );
          errorGraphMap[targetGraph].setEdge(
            `${edge.target}${edge.source}${targetGraph}`,
            edge.target,
            {
              label: "",
              labelStyle: "font-size: 12px;",
              id: `arg-edge_${edge.source}-${edge.target}`,
              style: "stroke-dasharray: 5, 5;",
              class: "arg-split-edge",
            }
          );
        } else if (sourceGraph > targetGraph) {
          errorGraphMap[sourceGraph].setNode(
            `${edge.source}${edge.target}${sourceGraph}`,
            {
              label: "",
              class: "arg-dummy",
              id: `dummy-${edge.target}`,
            }
          );
          errorGraphMap[sourceGraph].setEdge(
            edge.source,
            `${edge.source}${edge.target}${sourceGraph}`,
            {
              label: edge.label,
              id: `arg-edge${edge.source}${edge.target}`,
              arrowhead: "undirected",
              style: "stroke-dasharray: 5, 5;",
              class: edgeClassDecider(edge),
            }
          );
          errorGraphMap[targetGraph].setNode(
            `${edge.target}${edge.source}${targetGraph}`,
            {
              label: "",
              class: "dummy",
            }
          );
          errorGraphMap[targetGraph].setEdge(
            `${edge.target}${edge.source}${targetGraph}`,
            edge.target,
            {
              label: "",
              labelStyle: "font-size: 12px;",
              id: `arg-edge_${edge.source}-${edge.target}`,
              arrowhead: "undirected",
              style: "stroke-dasharray: 5, 5;",
              class: "arg-split-edge",
            }
          );
        }
      } else {
        sourceGraph = getGraphForNode(edge.source);
        targetGraph = getGraphForNode(edge.target);
        if (sourceGraph < targetGraph) {
          graphMap[sourceGraph].setNode(
            `${edge.source}${edge.target}${sourceGraph}`,
            {
              label: "",
              class: "arg-dummy",
              id: `dummy-${edge.target}`,
            }
          );
          graphMap[sourceGraph].setEdge(
            edge.source,
            `${edge.source}${edge.target}${sourceGraph}`,
            {
              label: edge.label,
              id: `arg-edge${edge.source}${edge.target}`,
              style: "stroke-dasharray: 5, 5;",
              class: edgeClassDecider(edge),
            }
          );
          graphMap[targetGraph].setNode(
            `${edge.target}${edge.source}${targetGraph}`,
            {
              label: "",
              class: "dummy",
            }
          );
          graphMap[targetGraph].setEdge(
            `${edge.target}${edge.source}${targetGraph}`,
            edge.target,
            {
              label: "",
              labelStyle: "font-size: 12px;",
              id: `arg-edge_${edge.source}-${edge.target}`,
              style: "stroke-dasharray: 5, 5;",
              class: "arg-split-edge",
            }
          );
        } else if (sourceGraph > targetGraph) {
          graphMap[sourceGraph].setNode(
            `${edge.source}${edge.target}${sourceGraph}`,
            {
              label: "",
              class: "arg-dummy",
              id: `dummy-${edge.target}`,
            }
          );
          graphMap[sourceGraph].setEdge(
            edge.source,
            `${edge.source}${edge.target}${sourceGraph}`,
            {
              label: edge.label,
              id: `arg-edge${edge.source}${edge.target}`,
              arrowhead: "undirected",
              style: "stroke-dasharray: 5, 5;",
              class: edgeClassDecider(edge),
            }
          );
          graphMap[targetGraph].setNode(
            `${edge.target}${edge.source}${targetGraph}`,
            {
              label: "",
              class: "dummy",
            }
          );
          graphMap[targetGraph].setEdge(
            `${edge.target}${edge.source}${targetGraph}`,
            edge.target,
            {
              label: "",
              labelStyle: "font-size: 12px;",
              id: `arg-edge_${edge.source}-${edge.target}`,
              arrowhead: "undirected",
              style: "stroke-dasharray: 5, 5;",
              class: "arg-split-edge",
            }
          );
        }
      }
    });
  }

  // Split the ARG graph honoring the split threshold
  function buildMultipleGraphs(graphNodes, graphEdges, graphLabel) {
    graphNodes.sort(
      (firstNode, secondNode) => firstNode.index - secondNode.index
    );
    const requiredGraphs = Math.ceil(graphNodes.length / graphSplitThreshold);
    let firstGraphBuild = false;
    let nodesPerGraph = [];
    for (let i = 1; i <= requiredGraphs; i += 1) {
      if (!firstGraphBuild) {
        nodesPerGraph = graphNodes.slice(0, graphSplitThreshold);
        firstGraphBuild = true;
      } else if (graphNodes[graphSplitThreshold * i - 1] !== undefined) {
        nodesPerGraph = graphNodes.slice(
          graphSplitThreshold * (i - 1),
          graphSplitThreshold * i
        );
      } else {
        nodesPerGraph = graphNodes.slice(graphSplitThreshold * (i - 1));
      }
      const graph = createGraph();
      if (graphLabel === "relevant") {
        simplifiedGraphMap.push(graph);
      } else if (graphLabel === "witness") {
        reducedGraphMap.push(graph);
      } else {
        graphMap.push(graph);
      }
      setGraphNodes(graph, nodesPerGraph);
      const nodesIndices = [];
      nodesPerGraph.forEach((n) => {
        nodesIndices.push(n.index);
      });
      const includedGraphEdges = graphEdges.filter(
        (e) =>
          nodesIndices.includes(e.source) && nodesIndices.includes(e.target)
      );
      setGraphEdges(graph, includedGraphEdges, true);
    }
    buildCrossgraphEdges(graphEdges, false);
  }

  // Split the ARG error graph honoring the split threshold
  function buildMultipleErrorGraphs(errorNodes, errorEdges) {
    errorNodes.sort(
      (firstNode, secondNode) => firstNode.index - secondNode.index
    );
    const requiredGraphs = Math.ceil(errorNodes.length / graphSplitThreshold);
    let firstGraphBuild = false;
    let nodesPerGraph = [];
    for (let i = 1; i <= requiredGraphs; i += 1) {
      if (!firstGraphBuild) {
        nodesPerGraph = errorNodes.slice(0, graphSplitThreshold);
        firstGraphBuild = true;
      } else if (nodes[graphSplitThreshold * i - 1] !== undefined) {
        nodesPerGraph = errorNodes.slice(
          graphSplitThreshold * (i - 1),
          graphSplitThreshold * i
        );
      } else {
        nodesPerGraph = errorNodes.slice(graphSplitThreshold * (i - 1));
      }
      const graph = createGraph();
      errorGraphMap.push(graph);
      setGraphNodes(graph, nodesPerGraph);
      const nodesIndices = [];
      nodesPerGraph.forEach((n) => {
        nodesIndices.push(n.index);
      });
      const graphEdges = errorEdges.filter(
        (e) =>
          nodesIndices.includes(e.source) && nodesIndices.includes(e.target)
      );
      setGraphEdges(graph, graphEdges, true);
    }
    buildCrossgraphEdges(errorEdges, true);
  }

  function buildSingleGraph(graphNodes, graphEdges, graphLabel) {
    const g = createGraph();
    setGraphNodes(g, graphNodes);
    setGraphEdges(g, graphEdges, false);
    if (graphLabel === "relevant") {
      simplifiedGraphMap.push(g);
    } else if (graphLabel === "witness") {
      reducedGraphMap.push(g);
    } else {
      graphMap.push(g);
    }
  }

  function buildGraphsAndPrepareResults(graphNodes, graphEdges, graphLabel) {
    if (graphNodes.length > graphSplitThreshold) {
      buildMultipleGraphs(graphNodes, graphEdges, graphLabel);
    } else {
      buildSingleGraph(graphNodes, graphEdges, graphLabel);
    }
  }

  // After the initial ARG graph has been send to the master script, prepare ARG containing only error path
  function prepareErrorGraph() {
    const errorNodes = [];
    const errorEdges = [];
    nodes.forEach((n) => {
      if (errorPath.includes(n.index)) {
        errorNodes.push(n);
      }
    });
    edges.forEach((e) => {
      if (errorPath.includes(e.source) && errorPath.includes(e.target)) {
        errorEdges.push(e);
      }
    });
    if (errorNodes.length > graphSplitThreshold) {
      buildMultipleErrorGraphs(errorNodes, errorEdges);
    } else {
      const g = createGraph();
      setGraphNodes(g, errorNodes);
      setGraphEdges(g, errorEdges, false);
      errorGraphMap.push(g);
    }
  }

  /* ################## Helper functions end ################## */

  if (data.json !== undefined) {
    json = JSON.parse(data.json);
    nodes = json.nodes;
    edges = json.edges;
    buildGraphsAndPrepareResults(nodes, edges, "default");
    if (json.relevantedges !== undefined && json.relevantnodes !== undefined) {
      relevantEdges = json.relevantedges;
      relevantNodes = json.relevantnodes;
      simplifiedGraphMap = [];
      buildGraphsAndPrepareResults(relevantNodes, relevantEdges, "relevant");
    }
    if (json.reducededges !== undefined && json.reducednodes !== undefined) {
      const reducedEdges = json.reducededges;
      const reducedNodes = json.reducednodes;
      reducedGraphMap = [];
      buildGraphsAndPrepareResults(reducedNodes, reducedEdges, "witness");
    }
  } else if (data.errorPath !== undefined) {
    errorPath = [];
    JSON.parse(data.errorPath).forEach((d) => {
      if (d.argelem !== undefined) {
        errorPath.push(d.argelem);
      }
    });
  } else if (data.renderer !== undefined) {
    if (graphMap.length > 0) {
      workerResult = {
        graph: JSON.stringify(graphMap[0]),
        id: graphCounter,
      };
      graphMap.shift();
      graphCounter += 1;
    } else {
      workerResult = {
        status: "done",
        errorPath,
      };
      if (simplifiedGraphMap.length > 0) {
        workerResult = {
          graph: JSON.stringify(simplifiedGraphMap[0]),
          id: simplifiedGraphCounter,
          simplifiedGraph: true,
        };
        simplifiedGraphMap.shift();
        simplifiedGraphCounter += 1;
      }
      if (
        typeof reducedGraphMap !== "undefined" &&
        reducedGraphMap.length > 0
      ) {
        workerResult = {
          graph: JSON.stringify(reducedGraphMap[0]),
          id: reducedGraphCounter,
          reducedGraph: true,
        };
        reducedGraphMap.shift();
        reducedGraphCounter += 1;
      }
      if (errorPath !== undefined) {
        errorGraphMap = [];
        graphCounter = 0;
        prepareErrorGraph();
      }
    }
  } else if (data.errorGraph !== undefined) {
    if (errorGraphMap.length > 0) {
      workerResult = {
        graph: JSON.stringify(errorGraphMap[0]),
        id: graphCounter,
        errorGraph: true,
      };
      errorGraphMap.shift();
      graphCounter += 1;
    }
  } else if (data.toSplit !== undefined) {
    graphSplitThreshold = data.toSplit;
    if (errorGraphMap !== undefined && errorGraphMap.length > 0) {
      errorGraphMap = [];
    }
    buildGraphsAndPrepareResults();
  }
  postMessage(workerResult);
};
