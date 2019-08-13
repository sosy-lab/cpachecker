/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */

//DATA
    var myData = {}; //ARG_JSON_INPUT
    /**myJSON = JSON.stringify(myData);
    localStorage.setItem("localStoredJSON", myJSON);
    text = localStorage.getItem("localStoredJSON");
    states = JSON.parse(text);**/

    var maxStep = myCount-1;

function slide(newValue)
{
    var step = document.getElementById("range2").innerHTML = newValue;
    var slider = document.getElementById("sliderRange");

    document.getElementById("sliderRange").max = maxStep;

    slider.oninput = function() {
        var x = document.getElementById("sliderRange");
        var currentVal = x.value;
        document.getElementById("range2").innerHTML = currentVal;
        var data = {};
        data = ("myData"+currentVal).toString();
        var myVar = eval(data);
        updateGraph();
        dagreGraphBuild(myVar);
    }

}
function buttonStart(){
    document.getElementById("range2").innerHTML = 0;
    document.getElementById("sliderRange").value = 0;
    var data = {};
    data = ("myData0").toString();
    var myVar = eval(data);
    updateGraph();
    dagreGraphBuild(myVar);
}
function buttonUP()
{
    document.getElementById("sliderRange").max = maxStep;
    var newValue = document.getElementById("range2").innerHTML;
    newValue++;
    if(newValue > maxStep) newValue = maxStep;
    document.getElementById("range2").innerHTML = newValue;
    document.getElementById("sliderRange").value = newValue;
    var data = {};
    data = ("myData"+newValue).toString();
    var myVar = eval(data);
    updateGraph();
    dagreGraphBuild(myVar);
}

function buttonDN()
{
    var newValue = document.getElementById("range2").innerHTML;
    newValue--;
    if(newValue < 0) newValue = 0;
    document.getElementById("range2").innerHTML = newValue;
    document.getElementById("sliderRange").value = newValue;
    var data = {};
    data = ("myData"+newValue).toString();
    var myVar = eval(data);
    updateGraph();
    dagreGraphBuild(myVar);
}

function buttonFinalGraph(){
    document.getElementById("sliderRange").max = maxStep;
    var newValue = maxStep;
    document.getElementById("range2").innerHTML = newValue;
    document.getElementById("sliderRange").value = newValue;
    var data = {};
    data = ("myData"+newValue).toString();
    var myVar = eval(data);
    updateGraph();
    dagreGraphBuild(myVar);
}


function dagreGraphBuild(json) {

    var data = json;
    var maxIndex = d3.max(data["nodes"], function(d) { return d.index; });


    // Create the input graph
    var g = new dagreD3.graphlib.Graph()
        .setGraph({})
        .setDefaultEdgeLabel(function () {
            return {};
        });

// Set up nodes
        data["nodes"].forEach(function (v) {
            g.setNode(v["index"], {label: v["label"], class: v["type"]});
        });
        g.nodes().forEach(function (v) {
            var node = g.node(v);
            // Round the corners of the nodes
            node.rx = node.ry = 5;
        });
    if(g.nodes(maxIndex)) {
        var node = g.node(maxIndex);
        node.style = "stroke: #1366eb; stroke-width: 10";
    }

// Set up edges

        data["edges"].forEach(function (v) {
            g.setEdge(v["source"], v["target"], {label: v["label"]})
        });

// Create the renderer
        var render = new dagreD3.render();

// Set up an SVG group so that we can translate the final graph.
        var svg = d3.select("svg"),
            svgGroup = svg.append("g");

        // Set up zoom support
        var zoom = d3.zoom().on("zoom", function () {
            transform = d3.event.transform
            svgGroup.attr("transform", transform);
            //svgGroup.attr("transform", d3.event.transform);
        });
        svg.call(zoom);

// Run the renderer. This is what draws the final graph.
        render(svgGroup, g);

// Center the graph
       /** var xCenterOffset = svg.attr("width", width = "100%");

        svgGroup.attr("transform", "translate(" + xCenterOffset + ", 20)");

        svg.attr("height", g.graph().height + 40)
            .style("border", "1px solid black");**/
       var xOffset = svg.attr("width", width = "50%");
       var initialScale = (typeof transform != 'undefined') ? transform.k : 0.75;

    svg.call(zoom.transform, d3.zoomIdentity.translate(
        (svg.attr("xOffset")) , 20)
            .scale(initialScale));

    svg.attr('height', g.graph().height  + 40)
        .style("border", "1px solid black");
}

function updateGraph() {
    d3.selectAll("g > *").remove();
}
