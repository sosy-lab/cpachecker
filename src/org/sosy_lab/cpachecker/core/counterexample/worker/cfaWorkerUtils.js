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
import { argWorkerCallback, argWorkerErrorCallback } from "./argWorkerUtils";

const d3 = require("d3");
const DagreD3 = require("dagre-d3");

const Render = new DagreD3.render();

// Find and return the actual edge element from cfaJson.edges array by considering funcCallEdges and combinedNodes
function findCfaEdge(eventElement) {
  let source = Number(eventElement.v);
  let target = Number(eventElement.w);
  if (source > 100000) {
    source = Object.keys(cfaJson.functionCallEdges).find((key) =>
      cfaJson.functionCallEdges[key].includes(source)
    );
  }
  if (target > 100000) {
    target = cfaJson.functionCallEdges[eventElement.v][1];
  }
  if (source in cfaJson.combinedNodes) {
    source =
      cfaJson.combinedNodes[source][cfaJson.combinedNodes[source].length - 1];
  }
  return cfaJson.edges.find(
    (e) => e.source === Number(source) && e.target === target
  );
}

const cfaWorkerErrorCallback = (e) => {
  alert(`CFA Worker failed with message ${e}`);
};

// Add desired events to CFA nodes and edges
function addEventsToCfa() {
  addPanEvent(".cfa-svg");
  d3.selectAll(".cfa-node")
    .on("mouseover", (d) => {
      let message;
      if (Number(d) > 100000) {
        message =
          '<span class=" bold ">type</span>: function call node <br>' +
          '<span class=" bold ">dblclick</span>: Select function';
      } else {
        const node = cfaJson.nodes.find((n) => n.index === Number(d));
        message = `<span class=" bold ">function</span>: ${node.func}`;
        if (d in cfaJson.combinedNodes) {
          message += `<br><span class=" bold ">combines nodes</span> : ${Math.min.apply(
            null,
            cfaJson.combinedNodes[d]
          )}-${Math.max.apply(null, cfaJson.combinedNodes[d])}`;
        }
        message += `<br> <span class=" bold ">reverse postorder Id</span>: ${node.rpid}`;
      }
      showToolTipBox(d3.event, message);
    })
    .on("mouseout", () => {
      hideToolTipBox();
    });
  d3.selectAll(".fcall").on("dblclick", (d, i) => {
    angular.element($("#cfa-toolbar")).scope().selectedCFAFunction = d3
      .select(`#cfa-node${i} text`)
      .text();
    angular.element($("#cfa-toolbar").scope()).setCFAFunction();
  });
  d3.selectAll(".cfa-dummy")
    .on("mouseover", () => {
      showToolTipBox(
        d3.event,
        '<span class=" bold ">type</span>: placeholder <br> <span class=" bold ">dblclick</span>: jump to Target node'
      );
    })
    .on("mouseout", () => {
      hideToolTipBox();
    })
    .on("dblclick", () => {
      if (!d3.select(".marked-cfa-node").empty()) {
        d3.select(".marked-cfa-node").classed("marked-cfa-node", false);
      }
      const selection = d3.select(
        `#cfa-node${d3.select(this).attr("id").split("-")[1]}`
      );
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
  d3.selectAll(".cfa-edge")
    .on("mouseover", function mouseover() {
      d3.select(this).select("path").style("stroke-width", "3px");
      showToolTipBox(
        d3.event,
        '<span class=" bold ">dblclick</span>: jump to Source line'
      );
    })
    .on("mouseout", function mouseout() {
      d3.select(this).select("path").style("stroke-width", "1.5px");
      hideToolTipBox();
    })
    .on("dblclick", function dblclick(d, i) {
      let edge = findCfaEdge(i);
      if (edge === undefined) {
        // this occurs for edges between graphs - splitting edges
        const thisEdgeData = d3.select(this).attr("id").split("_")[1];
        edge = findCfaEdge({
          v: thisEdgeData.split("-")[0],
          w: thisEdgeData.split("-")[1],
        });
      }
      document.querySelector("#set-tab-3").click();
      let { line } = edge;
      if (line === 0) {
        line = 1;
      }
      if (!d3.select(".marked-source-line").empty()) {
        d3.select(".marked-source-line").classed("marked-source-line", false);
      }
      const selection = d3.select(`#source-${line} td pre.prettyprint`);
      selection.classed("marked-source-line", true);
      $(".sourceContent").scrollTop(
        selection.node().getBoundingClientRect().top +
          $(".sourceContent").scrollTop() -
          200
      );
    });
  d3.selectAll(".cfa-split-edge")
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
      if (!d3.select(".marked-cfa-edge").empty()) {
        d3.select(".marked-cfa-edge").classed("marked-cfa-edge", false);
      }
      const selection = d3.select(
        `#cfa-edge_${edgeSourceTarget.split("-")[0]}-${
          edgeSourceTarget.split("-")[1]
        }`
      );
      selection.classed("marked-cfa-edge", true);
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
}

const cfaWorkerCallback = (data) => {
  if (data.graph !== undefined) {
    // id was already processed
    if (!d3.select(`#cfa-graph-${data.id}`).empty()) {
      enqueue("cfaWorker", {
        renderer: "ready",
        cfaSplit: data.cfaSplit,
        argTabDisabled: data.argTabDisabled,
      }).then(
        (result) => cfaWorkerCallback(result),
        (error) => cfaWorkerErrorCallback(error)
      );
      return;
    }
    const { id } = data;
    d3.select("#cfa-container")
      .append("div")
      .attr("id", `cfa-graph-${id}`)
      .attr("class", "cfa-graph");
    let g = createGraph();
    g = Object.assign(g, JSON.parse(data.graph));
    const svg = d3
      .select(`#cfa-graph-${id}`)
      .append("svg")
      .attr("id", `cfa-svg-${id}`)
      .attr("class", `${"cfa-svg cfa-svg-"}${data.func}`);
    const svgGroup = svg.append("g");
    Render(d3.select(`#cfa-svg-${id} g`), g);
    // Center the graph - calculate svg.attributes
    svg.attr("height", g.graph().height + margin * 2);
    svg.attr("width", g.graph().width + margin * 10);
    svgGroup.attr("transform", `translate(${margin * 2}, ${margin})`);
    $("#cfa-modal").text(
      `${Number($("#cfa-modal").text().split("/")[0]) + 1}/${
        $("#cfa-modal").text().split("/")[1]
      }`
    );
    enqueue("cfaWorker", {
      renderer: "ready",
      cfaSplit: data.cfaSplit,
      argTabDisabled: data.argTabDisabled,
    }).then(
      (result) => cfaWorkerCallback(result),
      (error) => cfaWorkerErrorCallback(error)
    );
  } else if (data.status !== undefined) {
    addEventsToCfa();
    d3.select("#cfa-toolbar").style("visibility", "visible");
    d3.select("#cfa-container").classed("cfa-content", true);
    d3.selectAll(".cfa-svg").each(function setWidth() {
      d3.select(this).attr(
        "width",
        Math.max(
          d3.select(this).attr("width"),
          d3.select(this.parentNode).style("width").split("px")[0]
        )
      );
    });
    d3.selectAll(".cfa-graph").style("visibility", "visible");
    if (data.cfaSplit) {
      $("#renderStateModal").modal("hide");
    } else if (!data.argTabDisabled) {
      enqueue("argWorker", {
        renderer: "ready",
      }).then(
        (result) => argWorkerCallback(result),
        (error) => argWorkerErrorCallback(error)
      );
    } else {
      $("#renderStateModal").modal("hide");
    }
  }
};

export { cfaWorkerCallback, cfaWorkerErrorCallback };
