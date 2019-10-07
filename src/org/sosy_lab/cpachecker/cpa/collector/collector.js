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

    //var maxStep = myCount-1;


data = ("myData0").toString();
var myVar = eval(data);
var minIndex = d3.min(myVar["nodes"], function(d){return d.index})
var maxIndex = d3.max(myVar["nodes"], function(d){return d.index})
var maxStep = myVar["nodes"].length-1;
var startIndex = minIndex;


function slide(newValue)
{
    var step = document.getElementById("range2").innerHTML = newValue;
    var slider = document.getElementById("sliderRange");

    document.getElementById("sliderRange").max = maxStep;

    slider.oninput = function() {
        console.log("Maxstep" + maxStep);
        console.log("newvalue" + newValue);
       /** var x = document.getElementById("sliderRange");
        var currentVal = x.value;
        document.getElementById("range2").innerHTML = currentVal;
        var data = {};
        data = ("myData"+currentVal).toString();
        var myVar = eval(data);
        updateGraph();
        dagreGraphBuild(myVar);**/
    }

}
function buttonStart(){
    document.getElementById("range2").innerHTML = 0;
    document.getElementById("sliderRange").value = 0;
    minIndex = startIndex;
    $(document).ready(function(){
        $("g.node").addClass("wannahide");
        $("g.edgePath").addClass("wannahide");
        $("g.edgeLabel").addClass("wannahide");
    })
    $(document).ready(function(){
        $("#node" + startIndex).removeClass("wannahide");
        $("#node" + startIndex).addClass("contentshow");
    })

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


function shownode(){
    document.getElementById("sliderRange").max = maxStep;
    var newValue = document.getElementById("range2").innerHTML;
    console.log("minIndex" + minIndex);
    console.log("newvalue" + newValue);

        $(document).ready(function(){
            $("#shownode").one("click",shownode2())
        });
}

function shownode2(){
    if(minIndex >= maxIndex){
        minIndex = maxIndex;}
    else{ $("#minIndex").html(minIndex++);}

    $("#node" + minIndex).removeClass("wannahide");
    $("#node" + minIndex).addClass("contentshow");

    var edgeID = "edge"+minIndex.toString();
    var labelID = "label"+minIndex.toString();

    $("g.edgePath[id*=" + edgeID + "]" ).removeClass("wannahide");
    $("g.edgePath[id*=" + edgeID + "]" ).addClass("contentshow");
    $("g.label[id*=" + labelID + "]" ).removeClass("wannahide");
    $("g.label[id*=" + labelID + "]" ).addClass("contentshow");

     if($("#node" + minIndex).is("g.merged")){
         if($("#node" + (minIndex-1)).is("g.toMerge")) {
             $("#node"+(minIndex-1)).removeClass();
             $("#node"+(minIndex-1)).addClass("hiddenmerge");
             var edgeID1 = "edge"+(minIndex-1).toString();
             var labelID1 = "label"+(minIndex-1).toString();
             $("g.edgePath[id*=" + edgeID1 + "]" ).removeClass();
             $("g.edgePath[id*=" + edgeID1 + "]" ).addClass("hiddenmerge");
             $("g.label[id*=" + labelID1 + "]" ).removeClass();
             $("g.label[id*=" + labelID1 + "]" ).addClass("hiddenmerge");
         }
         if($("#node" + (minIndex-2)).is("g.toMerge")) {
         $("#node"+(minIndex-2)).removeClass();
         $("#node"+(minIndex-2)).addClass("hiddenmerge");
             var edgeID2 = "edge"+(minIndex-2).toString();
             var labelID2 = "label"+(minIndex-2).toString();
             $("g.edgePath[id*=" + edgeID2 + "]" ).removeClass();
             $("g.edgePath[id*=" + edgeID2 + "]" ).addClass("hiddenmerge");
             $("g.label[id*=" + labelID2 + "]" ).removeClass();
             $("g.label[id*=" + labelID2 + "]" ).addClass("hiddenmerge");
            }
    }
}


function removenode(){
    $(document).ready(function(){
            $("#removenode").one("click",removenode2())
    });
}

function removenode2(){

        $("#node" + (minIndex)).removeClass("contentshow");
        $("#node" + (minIndex)).addClass("wannahide");
        var edgeID = "edge"+minIndex.toString();
        var labelID = "label"+minIndex.toString();
    //removes all edges pointing on current node
        $("g.edgePath[id*=" + edgeID + "]" ).removeClass("contentshow");
        $("g.edgePath[id*=" + edgeID + "]" ).addClass("wannahide");
        $("g.label[id*=" + labelID + "]" ).removeClass("contentshow");
        $("g.label[id*=" + labelID + "]" ).addClass("wannahide");


    if($("#node" + minIndex).is("g.merged")){
        if($("#node" + (minIndex-1)).is("g.hiddenmerge")) {
            $("#node"+(minIndex-1)).removeClass();
            $("#node"+(minIndex-1)).addClass("node toMerge contentshow");
            var edgeID1 = "edge"+(minIndex-1).toString();
            var labelID1 = "label"+(minIndex-1).toString();
            $("g.edgePath[id*=" + edgeID1 + "]" ).removeClass();
            $("g.edgePath[id*=" + edgeID1 + "]" ).addClass("edgePath contentshow");
            $("g.label[id*=" + labelID1 + "]" ).removeClass();
            $("g.label[id*=" + labelID1 + "]" ).addClass("label contentshow");
        }
        if($("#node" + (minIndex-2)).is("g.hiddenmerge")) {
            $("#node"+(minIndex-2)).removeClass();
            $("#node"+(minIndex-2)).addClass("node toMerge contentshow");
            var edgeID2 = "edge"+(minIndex-2).toString();
            var labelID2 = "label"+(minIndex-2).toString();
            $("g.edgePath[id*=" + edgeID2 + "]" ).removeClass();
            $("g.edgePath[id*=" + edgeID2 + "]" ).addClass("edgePath contentshow");
            $("g.label[id*=" + labelID2 + "]" ).removeClass();
            $("g.label[id*=" + labelID2 + "]" ).addClass("label contentshow");
        }
    }
    if (minIndex >= startIndex){
        $("#minIndex").html(minIndex--);
    }

}

function buttonFinalGraph(){
    minIndex = maxIndex;
    $(document).ready(function(){
        $("g.node").removeClass("wannahide");
        $("g.node").addClass("contentshow");
        $("g.edgePath").removeClass("wannahide");
        $("g.edgePath").addClass("contentshow");
        $("g.edgeLabel").removeClass("wannahide");
        $("g.edgeLabel").addClass("contentshow");
    })
    updateGraph();
    dagreGraphBuild(myVar);
}


function dagreGraphBuild(json) {

    var data = json;
    var maxIndex = d3.max(data["nodes"], function(d) { return d.index; });
    console.log("Maxindex" + maxIndex);

    // Create the input graph
    var g = new dagreD3.graphlib.Graph()
        .setGraph({})
        .setDefaultEdgeLabel(function () {
            return {};
        });

// Set up nodes
        data["nodes"].forEach(function (v) {
            g.setNode(v["index"], {label: v["label"], class: v["type"], id : "node" + v["index"]});
        });

        g.nodes().forEach(function (v) {
            var node = g.node(v);
            // Round the corners of the nodes
            node.rx = node.ry = 5;
           // node.id = count;
            //count ++;
        });
    if(g.nodes(maxIndex)) {
        var node = g.node(maxIndex);
        node.style = "stroke: #1366eb; stroke-width: 10";
        //node.class = "wannahide"
    }

// Set up edges
        data["edges"].forEach(function (v) {
            //g.setEdge(v["source"], v["target"], {label: v["label"]})
            //g.setEdge(v["source"], v["target"], {label: v["label"], id:"edge"+ v["count"], labelId: "label"+ v["count"]})
           g.setEdge(v["source"], v["target"], {label: v["label"],
                id:"edge"+ v["target"]+ "pID"+v["source"], labelId: "label"+ v["target"]+ "pID"+v["source"]})
        });

// Create the renderer
        var render = new dagreD3.render();

// Set up an SVG group so that we can translate the final graph.
        var svg = d3.select("svg"),
            svgGroup = svg.append("g");

    // Set up zoom support
    var zoom = d3.zoom().on('zoom', function () {
        transform = d3.event.transform;
        svgGroup.attr("transform","translate("+ transform.x + ", " + transform.y +") scale(" + transform.k + ")");


    });
    svg.call(zoom);

// Run the renderer. This is what draws the final graph.
        render(svgGroup, g);


    var initialScale = (typeof transform != 'undefined') ? transform.k : 0.75;
    var initialX = (typeof transform != 'undefined') ? transform.x : 20;
    var initialY = (typeof transform != 'undefined') ? transform.y : 20;
    var t = d3.zoomIdentity.translate(initialX, initialY).scale(initialScale);
    svg.call(zoom.transform, t);
    /**var xOffset = svg.attr("width", width = "50%")
    var initialScale = (typeof transform != 'undefined') ? transform.k : 0.75;
     svg.call(zoom.transform,
             d3.zoomIdentity.translate(
                 50,50)
                 //(svg.attr("xOffset")) , 20)//.width(xOffset).height(g.graph().height  + 40)
        //(svg.attr("width") - g.graph().width * initialScale) / 2, 20)
                            .scale(initialScale));**/

    svg.attr("width", width = "50%");
    svg.attr('height', g.graph().height + 40)
    //svg.attr('width', g.graph().width *initialScale + 40)
    //svg.attr('height', g.graph().height *initialScale + 40)
        .style("border", "1px solid black");

}

function updateGraph() {
    d3.selectAll("g > *").remove();

}



function showCoords(canvas, event) {
    var rect = canvas.getBoundingClientRect();
    var x = event.clientX - rect.left;
    var y = event.clientY - rect.top;
    console.log("x: " + x + " y: " + y);
}


