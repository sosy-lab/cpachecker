const d3 = require("d3");
const dagreD3 = require("dagre-d3");

// Use D3 zoom behavior to add pan event
function addPanEvent(itemsToSelect) {
  d3.selectAll(itemsToSelect).each(function(d, i) {
    const svg = d3.select(this),
      svgGroup = d3.select(this.firstChild);
    const zoom = d3.zoom().on("zoom", function(d, i) {
      svgGroup.attr("transform", d.transform)
    });
    svg.call(zoom);
    svg.on("zoom", null).on("wheel.zoom", null).on("dblclick.zoom", null).on("touchstart.zoom", null);
  });
}

// create and return a graph element with a set transition
function createGraph() {
  const g = new dagreD3.graphlib.Graph().setGraph({}).setDefaultEdgeLabel(
    function() {
      return {};
    });
  return g;
}

// On mouse over display tool tip box
function showToolTipBox(e, displayInfo) {
  const offsetX = 20;
  const offsetY = 0;
  const positionX = e.pageX;
  const positionY = e.pageY;
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

const margin = 20;

export {
  addPanEvent,
  createGraph,
  showToolTipBox,
  hideToolTipBox,
  margin
}
