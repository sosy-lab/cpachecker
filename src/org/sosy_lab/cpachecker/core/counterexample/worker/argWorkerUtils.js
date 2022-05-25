// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

import $ from "jquery";
import {
  addPanEvent,
  createGraph,
  showToolTipBox,
  hideToolTipBox,
  margin,
} from "./workerUtils";
import enqueue from "./workerDirector";

const d3 = require("d3");
const dagreD3 = require("dagre-d3");

const render = new dagreD3.render();

// Retrieve the node in which this node was merged - used for the node events
// FIXME: this is a duplicate function already contained in error path controller, currently no better way to call it
function getMergingNode(index) {
  return Object.keys(cfaJson.combinedNodes).find((key) =>
    cfaJson.combinedNodes[key].includes(index)
  );
}

// Add desired events to ARG the nodes
function addEventsToArg() {
  addPanEvent(".arg-svg");
  d3.selectAll(".arg-node")
    .on("mouseover", (d) => {
      const nodesArray = Array.prototype.concat(
        argJson.nodes,
        typeof argJson.relevantnodes === "undefined"
          ? []
          : argJson.relevantnodes,
        typeof argJson.reducednodes === "undefined" ? [] : argJson.reducednodes
      );
      const node = nodesArray.find((it) => it.index === parseInt(d, 10));
      let message = `<span class=" bold ">function</span>: ${node.func}<br>`;
      if (node.type) {
        message += `<span class=" bold ">type</span>: ${node.type}<br>`;
      }
      message += '<span class=" bold ">dblclick</span>: jump to CFA node';
      showToolTipBox(d3.event, message);
    })
    .on("mouseout", () => {
      hideToolTipBox();
    })
    .on("dblclick", function dblclick() {
      document.querySelector("#set-tab-1").click();
      if (!d3.select(".marked-cfa-node").empty()) {
        d3.select(".marked-cfa-node").classed("marked-cfa-node", false);
      }
      let nodeId = d3.select(this).select("tspan").text().split("N")[1];
      if (cfaJson.mergedNodes.includes(parseInt(nodeId, 10))) {
        nodeId = getMergingNode(parseInt(nodeId, 10));
      }
      const selection = d3.select(`#cfa-node${nodeId}`);
      selection.classed("marked-cfa-node", true);
      const boundingRect = selection.node().getBoundingClientRect();
      $("#cfa-container")
        .scrollTop(boundingRect.top + $("#cfa-container").scrollTop() - 300)
        .scrollLeft(
          boundingRect.left +
            $("#cfa-container").scrollLeft() -
            $("#errorpath_section").width() -
            2 * boundingRect.width
        );
    });
  d3.selectAll(".arg-dummy")
    .on("mouseover", () => {
      showToolTipBox(
        d3.event,
        '<span class=" bold ">type</span>: placeholder <br> <span class=" bold ">dblclick</span>: jump to Target node'
      );
    })
    .on("mouseout", () => {
      hideToolTipBox();
    })
    .on("dblclick", function dblclick() {
      if (!d3.select(".marked-arg-node").empty()) {
        d3.select(".marked-arg-node").classed("marked-arg-node", false);
      }
      const selection = d3.select(
        `#arg-node${d3.select(this).attr("id").split("-")[1]}`
      );
      selection.classed("marked-arg-node", true);
      const boundingRect = selection.node().getBoundingClientRect();
      $("#arg-container")
        .scrollTop(boundingRect.top + $("#arg-container").scrollTop() - 300)
        .scrollLeft(
          boundingRect.left +
            $("#arg-container").scrollLeft() -
            $("#errorpath_section").width() -
            2 * boundingRect.width
        );
    });
  d3.selectAll(".arg-edge")
    .on("mouseover", function mouseover(d) {
      d3.select(this).select("path").style("stroke-width", "3px");
      const edgeArray = Array.prototype.concat(
        argJson.edges,
        typeof argJson.relevantedges === "undefined"
          ? []
          : argJson.relevantedges,
        typeof argJson.reducededges === "undefined" ? [] : argJson.reducededges
      );
      const edge = edgeArray.find(
        (it) =>
          it.source === parseInt(d.v, 10) && it.target === parseInt(d.w, 10)
      );
      let message = "";
      Object.keys(edge).forEach((key) => {
        if ($.inArray(key, ["target", "source", "label", "line"]) === -1) {
          message += `<span class=" bold ">${key}<span>: ${edge[key]}<br>`;
        }
      });
      if (edge) {
        showToolTipBox(d3.event, message);
      } else {
        showToolTipBox(
          d3.event,
          '<span class=" bold ">type</span>: graph connecting edge'
        );
      }
    })
    .on("mouseout", function mouseout() {
      d3.select(this).select("path").style("stroke-width", "1.5px");
      hideToolTipBox();
    });
  d3.selectAll(".arg-split-edge")
    .on("mouseover", function mouseover() {
      d3.select(this).select("path").style("stroke-width", "3px");
      showToolTipBox(
        d3.event,
        '<span class=" bold ">type</span>: place holder <br> <span class=" bold ">dblclick</span>: jump to Original edge'
      );
    })
    .on("mouseout", function mouseout() {
      d3.select(this).select("path").style("stroke-width", "1.5px");
      hideToolTipBox();
    })
    .on("dblclick", function dblclick() {
      const edgeSourceTarget = d3.select(this).attr("id").split("_")[1];
      if (!d3.select(".marked-arg-edge").empty()) {
        d3.select(".marked-arg-edge").classed("marked-arg-edge", false);
      }
      const selection = d3.select(
        `#arg-edge${edgeSourceTarget.split("-")[0]}${
          edgeSourceTarget.split("-")[1]
        }`
      );
      selection.classed("marked-arg-edge", true);
      const boundingRect = selection.node().getBoundingClientRect();
      $("#arg-container")
        .scrollTop(boundingRect.top + $("#arg-container").scrollTop() - 300)
        .scrollLeft(
          boundingRect.left +
            $("#arg-container").scrollLeft() -
            $("#errorpath_section").width() -
            2 * boundingRect.width
        );
    });
}

// Function to get transformation through translate as in new version of D3.js d3.transfrom is removed
function getTransformation(transform) {
  // Create a dummy g for calculation purposes only. This will never
  // be appended to the DOM and will be discarded once this function
  // returns.
  const g = document.createElementNS("http://www.w3.org/2000/svg", "g");

  // Set the transform attribute to the provided string value.
  g.setAttributeNS(null, "transform", transform);

  // consolidate the SVGTransformList containing all transformations
  // to a single SVGTransform of type SVG_TRANSFORM_MATRIX and get
  // its SVGMatrix.
  const { matrix } = g.transform.baseVal.consolidate();

  // Below calculations are taken and adapted from the private function
  // transform/decompose.js of D3's module d3-interpolate.
  let { a, b, c, d } = matrix;
  const { e, f } = matrix;
  let scaleX = Math.sqrt(a * a + b * b);
  const scaleY = Math.sqrt(c * c + d * d);
  let skewX = a * c + b * d;
  if (scaleX) {
    a /= scaleX;
    b /= scaleX;
  }
  if (skewX) {
    c -= a * skewX;
    d -= b * skewX;
  }
  if (scaleY) {
    c /= scaleY;
    d /= scaleY;
    skewX /= scaleY;
  }
  if (a * d < b * c) {
    a = -a;
    b = -b;
    skewX = -skewX;
    scaleX = -scaleX;
  }
  return {
    translateX: e,
    translateY: f,
    rotate: (Math.atan2(b, a) * 180) / Math.PI,
    skewX: (Math.atan(skewX) * 180) / Math.PI,
    scaleX,
    scaleY,
  };
}

const argWorkerErrorCallback = (e) => {
  alert(`ARG Worker failed with message ${e}`);
};

const argWorkerCallback = (data) => {
  if (data.graph !== undefined) {
    let id = `arg-graph${data.id}`;
    let argClass = "arg-graph";
    if (data.errorGraph !== undefined) {
      id = `arg-error-graph${data.id}`;
      argClass = "arg-error-graph";
      d3.select("#arg-modal-error").style("display", "inline");
      $("#renderStateModal").modal("show");
    }
    if (data.simplifiedGraph !== undefined) {
      id = `arg-simplified-graph${data.id}`;
      argClass = "arg-simplified-graph";
    }
    if (data.reducedGraph !== undefined) {
      id = `arg-reduced-graph${data.id}`;
      argClass = "arg-reduced-graph";
    }
    let g = createGraph();
    g = Object.assign(g, JSON.parse(data.graph));
    d3.select("#arg-container")
      .append("div")
      .attr("id", id)
      .attr("class", argClass);
    const svg = d3
      .select(`#${id}`)
      .append("svg")
      .attr("id", `arg-svg${id}`)
      .attr("class", "arg-svg");
    const svgGroup = svg.append("g");
    render(d3.select(`#arg-svg${id} g`), g);
    // Center the graph - calculate svg.attributes
    svg.attr("height", g.graph().height + margin * 2);
    svg.attr("width", g.graph().width + margin * 10);
    svgGroup.attr("transform", `translate(${margin * 2}, ${margin})`);
    // FIXME: until https://github.com/cpettitt/dagre-d3/issues/169 is not resolved, label centering like so:
    d3.selectAll(".arg-node tspan").each(function tspan() {
      const transformation = d3
        .select(this.parentNode.parentNode)
        .attr("transform");
      d3.select(this).attr(
        "dx",
        Math.abs(getTransformation(transformation).translateX)
      );
    });
    if (data.errorGraph !== undefined) {
      addEventsToArg();
      $("#renderStateModal").modal("hide");
      enqueue("argWorker", {
        errorGraph: true,
      }).then(
        (result) => argWorkerCallback(result),
        (error) => argWorkerErrorCallback(error)
      );
    } else {
      $("#arg-modal").text(
        `${parseInt($("#arg-modal").text().split("/")[0], 10) + 1}/${
          $("#arg-modal").text().split("/")[1]
        }`
      );
      enqueue("argWorker", {
        renderer: "ready",
      }).then(
        (result) => argWorkerCallback(result),
        (error) => argWorkerErrorCallback(error)
      );
    }
  } else if (data.status !== undefined) {
    if (angular.element($("#report-controller")).scope().getTabSet() === 2) {
      d3.select("#arg-toolbar").style("visibility", "visible");
      d3.select("#arg-container").classed("arg-content", true);
      d3.selectAll(".arg-graph").style("visibility", "visible");
      $("#arg-container").scrollLeft(d3.select(".arg-svg").attr("width") / 4);
    }
    addEventsToArg();
    if (data.errorPath !== undefined) {
      d3.selectAll("td.disabled").classed("disabled", false);
      if (!d3.select(".make-pretty").classed("prettyprint")) {
        d3.selectAll(".make-pretty").classed("prettyprint", true);
        PR.prettyPrint();
      }
    }
    $("#renderStateModal").modal("hide");
  }
};

export { argWorkerCallback, argWorkerErrorCallback };
