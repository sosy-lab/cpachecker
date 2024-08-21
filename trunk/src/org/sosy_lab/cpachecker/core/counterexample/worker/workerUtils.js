// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

const d3 = require("d3");
const dagreD3 = require("dagre-d3");

// Use D3 zoom behavior to add pan event
function addPanEvent(itemsToSelect) {
  d3.selectAll(itemsToSelect).each(function addEvent() {
    const svg = d3.select(this);
    const svgGroup = d3.select(this.firstChild);
    const zoom = d3.zoom().on("zoom", () => {
      svgGroup.attr("transform", d3.event.transform);
    });
    svg.call(zoom);
    svg
      .on("zoom", null)
      .on("wheel.zoom", null)
      .on("dblclick.zoom", null)
      .on("touchstart.zoom", null);
  });
}

// create and return a graph element with a set transition
function createGraph() {
  const g = new dagreD3.graphlib.Graph()
    .setGraph({})
    .setDefaultEdgeLabel(() => ({}));
  return g;
}

// On mouse over display tool tip box
function showToolTipBox(e, displayInfo) {
  const offsetX = 20;
  const offsetY = 0;
  const positionX = e.pageX;
  const positionY = e.pageY;
  d3.select("#boxContent").html(`<p>${displayInfo}</p>`);
  d3.select("#infoBox")
    .style("left", () => `${positionX + offsetX}px`)
    .style("top", () => `${positionY + offsetY}px`)
    .style("visibility", "visible");
}

// On mouse out hide the tool tip box
function hideToolTipBox() {
  d3.select("#infoBox").style("visibility", "hidden");
}

const margin = 20;

export { addPanEvent, createGraph, showToolTipBox, hideToolTipBox, margin };
