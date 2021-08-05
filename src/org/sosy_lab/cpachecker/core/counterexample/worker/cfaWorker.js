// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * The CFA Worker. Contains the logic for building a single or a multi CFA graph.
 * The graph(s) is/are returned to the main script once created
 * */
let json;
let nodes;
let mainNodes;
let edges;
let functions;
let combinedNodes;
let combinedNodesLabels;
let mergedNodes;
let functionCallEdges;
let errorPath;
let graphSplitThreshold = 700;
let graphMap = [];
let graphCounter = 0;

onmessage = (msg) => {
  const { data } = msg;
  const { cfaSplit } = data;
  const { argTabDisabled } = data;
  let workerResult = {};

  /* ################## Helper functions ################## */

  // Prepare Error Path array to be used in edge class decider
  function prepareCfaErrorPath() {
    const returnedEdges = {};
    Object.keys(functionCallEdges).forEach((key) => {
      returnedEdges[functionCallEdges[key][1]] = functionCallEdges[key][0];
    });
    json.errorPath.forEach((errPathElem) => {
      if (errPathElem.source in functionCallEdges) {
        errPathElem.target = functionCallEdges[errPathElem.source][0];
      }
      if (errPathElem.target in returnedEdges) {
        errorPath.push({
          source: returnedEdges[errPathElem.target],
          target: errPathElem.target,
        });
      }
      errorPath.push(errPathElem);
    });
  }

  // Extract information from the cfaJson
  function extractletiables() {
    nodes = json.nodes;
    functions = json.functionNames;
    mainNodes = nodes.filter((n) => n.func === functions[0]);
    edges = json.edges;
    combinedNodes = json.combinedNodes;
    combinedNodesLabels = json.combinedNodesLabels;
    mergedNodes = json.mergedNodes;
    functionCallEdges = json.functionCallEdges;
    if (Object.prototype.hasOwnProperty.call(json, "errorPath")) {
      errorPath = [];
      prepareCfaErrorPath();
    }
  }

  // create and return a graph element with a set transition
  function createGraph() {
    return new dagreD3.graphlib.Graph()
      .setGraph({})
      .setDefaultEdgeLabel(() => {});
  }

  // Return the graph in which the nodeNumber is present
  function getGraphForNode(nodeNumber) {
    return graphMap.findIndex((graph) =>
      graph.nodes().includes(`${nodeNumber}`)
    );
  }

  // Node label, the label from combined nodes or a simple label
  function setNodeLabel(node) {
    const nodeIndex = `${node.index}`;
    if (Object.keys(combinedNodesLabels).includes(nodeIndex))
      return combinedNodesLabels[nodeIndex];
    return `N${nodeIndex}`;
  }

  // Decide the shape of the nodes based on type
  function nodeShapeDecider(n) {
    if (n.loop) {
      return "diamond";
    }
    if (Object.keys(combinedNodes).includes(`${n.index}`)) {
      return "rect";
    }
    return "circle";
  }

  // Set nodes for the graph contained in the json nodes
  function setGraphNodes(graph, nodesToSet) {
    nodesToSet.forEach((n) => {
      if (!mergedNodes.includes(n.index)) {
        graph.setNode(n.index, {
          label: setNodeLabel(n),
          labelStyle: "font-family: 'Courier New', Courier, monospace",
          class: "cfa-node",
          id: `cfa-node${n.index}`,
          shape: nodeShapeDecider(n),
        });
      }
    });
  }

  // Round the corners of rectangle shaped nodes
  function roundNodeCorners(graph) {
    graph.nodes().forEach((it) => {
      const item = graph.node(it);
      item.rx = 5;
      item.ry = 5;
    });
  }

  // Retrieve the node in which this node was merged
  function getMergingNode(index) {
    const result = Object.keys(combinedNodes).find((key) =>
      combinedNodes[key].includes(index)
    );
    return parseInt(result, 10);
  }

  // Check if edge is eligible to place in graph. Same node edge only if it is not combined node
  function checkEligibleEdge(source, target) {
    if (mergedNodes.includes(source) && mergedNodes.includes(target))
      return false;
    if (
      mergedNodes.includes(source) &&
      Object.keys(combinedNodes).includes(`${target}`)
    ) {
      if (combinedNodes[`${target}`].includes(source)) return false;
      return true;
    }
    if (
      Object.keys(combinedNodes).includes(`${source}`) &&
      mergedNodes.includes(target)
    ) {
      if (combinedNodes[`${source}`].includes(target)) return false;
      return true;
    }
    if (
      Object.keys(combinedNodes).includes(`${source}`) &&
      Object.keys(combinedNodes).includes(`${target}`) &&
      source === target
    )
      return false;
    return true;
  }

  // If edge is part of error path, give it a representative class
  function edgeClassDecider(edge, source, target) {
    if (errorPath === undefined) {
      return "cfa-edge";
    }
    const mergedMatch = errorPath.find(
      (entry) => entry.source === source && entry.target === target
    );
    const initialMatch = errorPath.find(
      (entry) => entry.source === edge.source && entry.target === edge.target
    );
    if (mergedMatch !== undefined || initialMatch !== undefined) {
      return "cfa-edge error-edge";
    }
    return "cfa-edge";
  }

  // If edge is part of error path, give its label a representative class
  function labelStyleDecider(edge, source, target) {
    const edgeClass = edgeClassDecider(edge, source, target);
    if (edgeClass.indexOf("error") !== -1) {
      return "font-size: 12px; fill: red";
    }
    return "font-size: 12px";
  }

  // Decide the weight for the edges based on type
  function edgeWeightDecider(source, target) {
    const sourceNode = nodes.find((it) => it.index === source);
    const targetNode = nodes.find((it) => it.index === target);
    if (source === target) {
      return 2;
    }
    if (sourceNode.rpid < targetNode.rpid) {
      return 0;
    }
    return 1;
  }

  // Get node label for functionCall node by providing the edge statement
  function getNodeLabelFCall(stmt) {
    let result = "";
    if (stmt.includes("=")) {
      result = stmt.split("=")[1].split("(")[0].trim();
    } else {
      result = stmt.split("(")[0].trim();
    }
    return result;
  }

  // Set the edges for a single graph while considering merged nodes and edges between them
  function setGraphEdges(graph, edgesToSet, multigraph) {
    edgesToSet.forEach((e) => {
      let source;
      let target;
      if (!mergedNodes.includes(e.source) && !mergedNodes.includes(e.target)) {
        source = e.source;
        target = e.target;
      } else if (
        !mergedNodes.includes(e.source) &&
        mergedNodes.includes(e.target)
      ) {
        source = e.source;
        target = getMergingNode(e.target);
      } else if (
        mergedNodes.includes(e.source) &&
        !mergedNodes.includes(e.target)
      ) {
        source = getMergingNode(e.source);
        target = e.target;
      }
      if (
        multigraph &&
        (!graph.nodes().includes(`${source}`) ||
          !graph.nodes().includes(`${target}`))
      )
        source = undefined;
      if (
        source !== undefined &&
        target !== undefined &&
        checkEligibleEdge(source, target)
      ) {
        if (Object.keys(functionCallEdges).includes(`${source}`)) {
          const funcCallNodeId = functionCallEdges[`${source}`][0];
          graph.setNode(funcCallNodeId, {
            label: getNodeLabelFCall(e.stmt),
            class: "cfa-node fcall",
            id: `cfa-node${funcCallNodeId}`,
            shape: "rect",
          });
          graph.setEdge(source, funcCallNodeId, {
            label: e.stmt,
            labelStyle: labelStyleDecider(e, source, funcCallNodeId),
            class: edgeClassDecider(e, source, funcCallNodeId),
            id: `cfa-edge_${source}-${funcCallNodeId}`,
            weight: edgeWeightDecider(source, target),
          });
          graph.setEdge(funcCallNodeId, target, {
            label: "",
            labelStyle: labelStyleDecider(e, funcCallNodeId, target),
            class: edgeClassDecider(e, funcCallNodeId, target),
            id: `cfa-edge_${funcCallNodeId}-${target}`,
            weight: edgeWeightDecider(source, target),
          });
        } else {
          graph.setEdge(source, target, {
            label: e.stmt,
            labelStyle: labelStyleDecider(e, source, target),
            lineInterpolate: "basis",
            class: edgeClassDecider(e, source, target),
            id: `cfa-edge_${source}-${target}`,
            weight: edgeWeightDecider(source, target),
          });
        }
      }
    });
  }

  // Handle Edges that connect Graphs
  function buildCrossgraphEdges(crossGraphNodes) {
    const nodesIndices = [];
    crossGraphNodes.forEach((n) => {
      nodesIndices.push(n.index);
    });
    const edgesToConsider = edges.filter(
      (e) => nodesIndices.includes(e.source) && nodesIndices.includes(e.target)
    );
    edgesToConsider.forEach((edge) => {
      let { source } = edge;
      let { target } = edge;
      if (mergedNodes.includes(source) && mergedNodes.includes(target)) return;
      if (mergedNodes.includes(source)) source = getMergingNode(source);
      if (mergedNodes.includes(target)) target = getMergingNode(target);
      const sourceGraph = getGraphForNode(source);
      const targetGraph = getGraphForNode(target);
      if (sourceGraph < targetGraph) {
        if (Object.keys(functionCallEdges).includes(`${source}`)) {
          const funcCallNodeId = functionCallEdges[`${source}`][0];
          graphMap[sourceGraph].setNode(funcCallNodeId, {
            label: getNodeLabelFCall(edge.stmt),
            class: "cfa-node fcall",
            id: `cfa-node${funcCallNodeId}`,
            shape: "rect",
          });
          graphMap[sourceGraph].setEdge(source, funcCallNodeId, {
            label: edge.stmt,
            labelStyle: labelStyleDecider(edge, source, funcCallNodeId),
            class: edgeClassDecider(edge, source, funcCallNodeId),
            id: `cfa-edge_${source}-${funcCallNodeId}`,
          });
          graphMap[sourceGraph].setNode(`${source}${target}${sourceGraph}`, {
            label: "",
            class: "cfa-dummy",
            id: `dummy-${target}`,
            shape: "rect",
          });
          graphMap[sourceGraph].setEdge(
            funcCallNodeId,
            `${source}${target}${sourceGraph}`,
            {
              label: `${source}->${target}`,
              style: "stroke-dasharray: 5, 5;",
            }
          );
        } else {
          graphMap[sourceGraph].setNode(`${source}${target}${sourceGraph}`, {
            label: "",
            class: "cfa-dummy",
            id: `dummy-${target}`,
            shape: "rect",
          });
          graphMap[sourceGraph].setEdge(
            source,
            `${source}${target}${sourceGraph}`,
            {
              label: edge.stmt,
              labelStyle: labelStyleDecider(
                edge,
                source,
                `${source}${target}${sourceGraph}`
              ),
              id: `cfa-edge_${source}-${target}`,
              class: edgeClassDecider(
                edge,
                source,
                `${source}${target}${sourceGraph}`
              ),
              style: "stroke-dasharray: 5, 5;",
            }
          );
        }
        graphMap[targetGraph].setNode(`${target}${source}${targetGraph}`, {
          label: "",
          class: "dummy",
        });
        graphMap[targetGraph].setEdge(
          `${target}${source}${targetGraph}`,
          target,
          {
            label: "",
            labelStyle: "font-size: 12px;",
            id: `cfa-split-edge_${source}-${target}`,
            class: "cfa-split-edge",
            style: "stroke-dasharray: 5, 5;",
          }
        );
      } else if (sourceGraph > targetGraph) {
        graphMap[sourceGraph].setNode(`${source}${target}${sourceGraph}`, {
          label: "",
          class: "cfa-dummy",
          id: `dummy-${target}`,
        });
        graphMap[sourceGraph].setEdge(
          source,
          `${source}${target}${sourceGraph}`,
          {
            label: edge.stmt,
            labelStyle: labelStyleDecider(
              edge,
              `${source}${target}${sourceGraph}`,
              source
            ),
            id: `cfa-edge_${source}-${target}`,
            class: edgeClassDecider(
              edge,
              `${source}${target}${sourceGraph}`,
              source
            ),
            arrowhead: "undirected",
            style: "stroke-dasharray: 5, 5;",
          }
        );
        graphMap[targetGraph].setNode(`${target}${source}${targetGraph}`, {
          label: "",
          class: "dummy",
          id: `node${source}`,
        });
        graphMap[targetGraph].setEdge(
          `${target}${source}${targetGraph}`,
          target,
          {
            label: "",
            labelStyle: "font-size: 12px;",
            id: `cfa-split-edge_${source}-${target}`,
            class: "cfa-split-edge",
            arrowhead: "undirected",
            style: "stroke-dasharray: 5, 5;",
          }
        );
      }
    });
  }

  function buildMultipleGraphs(nodesToSet, funcName) {
    const requiredGraphs = Math.ceil(nodesToSet.length / graphSplitThreshold);
    let firstGraphBuild = false;
    for (let i = 1; i <= requiredGraphs; i += 1) {
      let nodesPerGraph = [];
      if (!firstGraphBuild) {
        nodesPerGraph = nodesToSet.slice(0, graphSplitThreshold);
        firstGraphBuild = true;
      } else if (nodesToSet[graphSplitThreshold * i - 1] !== undefined)
        nodesPerGraph = nodesToSet.slice(
          graphSplitThreshold * (i - 1),
          graphSplitThreshold * i
        );
      else nodesPerGraph = nodesToSet.slice(graphSplitThreshold * (i - 1));
      const graph = createGraph();
      setGraphNodes(graph, nodesPerGraph);
      if (graph.nodes().length > 0) {
        graphMap.push(graph);
        roundNodeCorners(graph);
        const graphEdges = edges.filter(
          (e) =>
            nodesPerGraph[0].index <= e.source &&
            e.source <= nodesPerGraph[nodesPerGraph.length - 1].index &&
            nodesPerGraph[0].index <= e.target &&
            e.target <= nodesPerGraph[nodesPerGraph.length - 1].index
        );
        setGraphEdges(graph, graphEdges, true);
      }
    }
    buildCrossgraphEdges(nodesToSet);
    if (funcName === functions[0]) {
      workerResult = {
        graph: JSON.stringify(graphMap[graphCounter]),
        id: funcName + graphCounter,
        func: funcName,
        cfaSplit,
        argTabDisabled,
      };
      graphCounter += 1;
    }
  }

  function buildSingleGraph(nodesToSet, funcName) {
    const g = createGraph();
    setGraphNodes(g, nodesToSet);
    roundNodeCorners(g);
    const nodesIndices = [];
    nodesToSet.forEach((n) => {
      nodesIndices.push(n.index);
    });
    const edgesToSet = edges.filter(
      (e) => nodesIndices.includes(e.source) && nodesIndices.includes(e.target)
    );
    setGraphEdges(g, edgesToSet, false);
    if (funcName === functions[0]) {
      workerResult = {
        graph: JSON.stringify(g),
        id: funcName + graphCounter,
        func: funcName,
        cfaSplit,
        argTabDisabled,
      };
      graphMap.push(g);
    } else {
      graphMap.push(g);
    }
  }

  function buildGraphsAndPostResults() {
    if (mainNodes.length > graphSplitThreshold) {
      buildMultipleGraphs(mainNodes, functions[0]);
    } else {
      buildSingleGraph(mainNodes, functions[0]);
    }
    if (functions.length > 1) {
      const functionsToProcess = functions.filter((f) => f !== functions[0]);
      functionsToProcess.forEach((func) => {
        const funcNodes = nodes.filter((n) => n.func === func);
        if (funcNodes.length > graphSplitThreshold) {
          buildMultipleGraphs(funcNodes, func);
        } else {
          buildSingleGraph(funcNodes, func);
        }
      });
    }
  }

  /* ################## Helper functions end ################## */

  if (data.json !== undefined) {
    json = JSON.parse(data.json);
    extractletiables();
    buildGraphsAndPostResults();
  } else if (data.renderer !== undefined) {
    if (graphMap[graphCounter] !== undefined) {
      const node = nodes.find(
        (n) => n.index === parseInt(graphMap[graphCounter].nodes()[0], 10)
      );
      workerResult = {
        graph: JSON.stringify(graphMap[graphCounter]),
        id: node.func + graphCounter,
        func: node.func,
        cfaSplit,
        argTabDisabled,
      };

      graphCounter += 1;
    } else {
      workerResult = {
        status: "done",
        cfaSplit,
        argTabDisabled,
      };
      graphMap = [];
      graphCounter = 0;
    }
  } else if (data.toSplit !== undefined) {
    graphSplitThreshold = data.toSplit;
    buildGraphsAndPostResults();
  }

  postMessage(workerResult);
};
