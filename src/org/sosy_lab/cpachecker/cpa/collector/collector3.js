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
var data = {}; //ARG_JSON_INPUT

var myVar = myData0;

var maxStep = myVar["nodes"].length-1;
var step = 0;
var startIndex = step;

if(window.attachEvent){window.attachEvent("onload", start);}
else if(window.addEventListener){window.addEventListener("load",start, false);}
else{document.addEventListener("load",start, false);}

window.addEventListener('load',slide,false);

function slide(){
    slider = document.getElementById("myRange");
    document.getElementById("myRange").max = maxStep+1;
    var output = document.getElementById("demo");
    output.innerHTML = slider.value;


    slider.oninput = function() {
        output.innerHTML = this.value;
        var curVal = this.value;
        var nodeIndexStart = myVar["nodes"][startIndex].index;
        document.getElementById("demo").innerHTML = curVal ;
        document.getElementById("myRange").value = curVal ;

        $("g.node").addClass("wannahide");
        $("g.edgePath").addClass("wannahide");
        $("g.edgeLabel").addClass("wannahide");
        $("g.edgeLabel.label").addClass("wannahide");
        $("g.node").removeClass("contentshow");
        $("g.edgePath").removeClass("contentshow");
        $("g.edgeLabel").removeClass("contentshow");
        $("g.edgeLabel.label").removeClass("contentshow");
        $("#node" + nodeIndexStart).removeClass("wannahide");
        $("#node" + nodeIndexStart).addClass("contentshow");
        $("g.label[id*=label]" ).removeClass("contentshow");

        for(i=0;i<=(curVal);i++){
            shownode3(i);
            if(curVal > maxStep){
                shownode();
            }
        }
        step=curVal;
    }
}

function start(){

    var nodeIndexStart = myVar["nodes"][startIndex].index;

    $(document).ready(function(){
        $("g.node").addClass("wannahide");
        $("g.edgePath").addClass("wannahide");
        $("g.edgeLabel").addClass("wannahide");
    })
    $(document).ready(function(){
        $("#node" + nodeIndexStart).removeClass("wannahide");
        $("#node" + nodeIndexStart).addClass("contentshow");
    })

    updateGraph();
    dagreGraphBuild(myVar);
}

function reset(){
    $(document).ready(function(){
    })
    window.location.href = window.location;
    start();
}

function shownode(){
   if(step > maxStep){
        //step = maxStep;
   }
    else{ $("#step").html(step++);}
    $(document).ready(function(){
        $("#shownode").one("click",shownode3(step));
        document.getElementById("demo").innerHTML = step ;
        document.getElementById("myRange").value = step ;
    })
}

function removenode(){

    var nodeIndexStart = myVar["nodes"][startIndex].index;
    if(step <= 0){
        step = 0;}
    else{ $("#step").html(step--);}
    $(document).ready(function(){
        $("g.node").removeClass("contentshow");
        $("g.node").addClass("wannahide");
        $(".stayalive").removeClass("highlight");
        $("#node" + nodeIndexStart).removeClass("wannahide");
        $("#node" + nodeIndexStart).addClass("contentshow");
        $("g.edgePath").addClass("wannahide");
        $("g.edgeLabel").addClass("wannahide");
        $("g.edgeLabel.label").addClass("wannahide");
        $("g.edgePath").removeClass("contentshow");
        $("g.edgeLabel").removeClass("contentshow");
        $("g.edgeLabel.label").removeClass("contentshow");
        $("g.label[id*=label]" ).removeClass("contentshow");
        $("#shownode").one("click",shownode3(step))
        document.getElementById("demo").innerHTML = step ;
        document.getElementById("myRange").value = step ;
    })
}

function shownode3(step) {
    var step = step;
    var start = -1;

    if(step <= maxStep){
        for (count = 0; count <= step; count++) {
            var nodeIndexCount = myVar["nodes"][count].index;
            var intervalStart = myVar["nodes"][count].intervalStart;
            var intervalStop = myVar["nodes"][count].intervalStop;
            var nodeType = myVar["nodes"][count].type;

            if (!intervalStop) {
                //do nothing
            }
            else if (intervalStop > start) {
                if (nodeType == "toMerge") {
                    $("#node" + nodeIndexCount).addClass("wannahide");
                    $("#node" + nodeIndexCount).addClass("bullshitsoon");
                }
            }

            if (intervalStart > start) {
                if (nodeType == "none") {
                    $("#node" + nodeIndexCount).removeClass("wannahide");
                    $("#node" + nodeIndexCount).addClass("contentshow");
                    var edgeID = "edge" + nodeIndexCount.toString()+"marker";
                    var labelID = "label" + nodeIndexCount.toString()+"marker";
                    $("g.edgePath[id*=" + edgeID + "]" ).removeClass("wannahide");
                    $("g.edgePath[id*=" + edgeID + "]" ).addClass("contentshow");
                    $("g.label[id*=" + labelID + "]" ).removeClass("wannahide");
                    $("g.label[id*=" + labelID + "]" ).addClass("contentshow");
                    if ($("g.node").hasClass("bullshitsoon")) {
                        $(".bullshitsoon").removeClass("highlight");
                        $(".bullshitsoon").removeClass("contentshow");
                        $(".bullshitsoon").addClass("wannahide");
                        $(".bullshitsoon").removeClass("stayalive");
                        $(".bullshitsoon").removeClass("bullshitsoon");
                        $(".stayalive").removeClass("highlight");
                        $(".currentMerge").removeClass("currentMerge");
                        $("g.edgePath[id$=destroyed]" ).removeClass("contentshow");
                        $("g.edgePath[id$=destroyed]" ).addClass("wannahide");
                        $("g.label[id$=destroyed]" ).removeClass("contentshow");
                        $("g.label[id$=destroyed]" ).addClass("wannahide");
                    }

                }
                else if (nodeType == "toMerge") {
                    $("#node" + nodeIndexCount).addClass("highlight");
                    $("#node" + nodeIndexCount).addClass("contentshow");
                    $("#node" + nodeIndexCount).addClass("stayalive");
                    $("#node" + nodeIndexCount).removeClass("wannahide");
                    $(".currentMerge").removeClass("currentMerge");
                    var edgeID = "edge" + nodeIndexCount.toString()+"marker";
                    var labelID = "label" + nodeIndexCount.toString()+"marker";
                    $("g.edgePath[id*=" + edgeID + "]" ).removeClass("wannahide");
                    $("g.edgePath[id*=" + edgeID + "]" ).addClass("contentshow");
                    $("g.label[id*=" + labelID + "]" ).removeClass("wannahide");
                    $("g.label[id*=" + labelID + "]" ).addClass("contentshow");

                }
                else if (nodeType == "merged") {
                    $("#node" + nodeIndexCount).addClass("currentMerge");
                    $("#node" + nodeIndexCount).addClass("contentshow");
                    $("#node" + nodeIndexCount).addClass("stayalive");
                    $("#node" + nodeIndexCount).removeClass("wannahide");
                    $(".currentMerge").removeClass("highlight");
                    var edgeID = "edge" + nodeIndexCount.toString()+"marker";
                    var labelID = "label" + nodeIndexCount.toString()+"marker";
                    $("g.edgePath[id*=" + edgeID + "]" ).removeClass("wannahide");
                    $("g.edgePath[id*=" + edgeID + "]" ).addClass("contentshow");
                    $("g.label[id*=" + labelID + "]" ).removeClass("wannahide");
                    $("g.label[id*=" + labelID + "]" ).addClass("contentshow");

                }
                start = intervalStart;
            }
        }
    }
    else{
        lastshow();
    }
}

function lastshow() {
    if ($("g.node").hasClass("bullshitsoon")) {
        $(".bullshitsoon").removeClass("highlight");
        $(".bullshitsoon").removeClass("contentshow");
        $(".bullshitsoon").addClass("wannahide");
        $(".bullshitsoon").removeClass("stayalive");
        $(".bullshitsoon").removeClass("bullshitsoon");
        $(".stayalive").removeClass("highlight");
        $(".currentMerge").removeClass("currentMerge");
        $("g.edgePath[id$=destroyed]" ).removeClass("contentshow");
        $("g.edgePath[id$=destroyed]" ).addClass("wannahide");
        $("g.label[id$=destroyed]" ).removeClass("contentshow");
        $("g.label[id$=destroyed]" ).addClass("wannahide");
    }
}

function buttonFinalGraph(){
    $(document).ready(function(){

        $("g.toMerge").addClass("highlight");
        $("g.merged").addClass("currentMerge");
        $("g.node").removeClass("wannahide");
        $("g.node").addClass("contentshow");
        $("g.edgePath").removeClass("wannahide");
        $("g.edgePath").addClass("contentshow");
        $("g.edgeLabel").removeClass("wannahide");
        $("g.edgeLabel").addClass("contentshow");
    })
    document.getElementById("myRange").max = maxStep;
    document.getElementById("myRange").value = maxStep;
    document.getElementById("demo").innerHTML = maxStep;
    step=maxStep;
}

function buttonFinalGraph2(){
    $(document).ready(function(){
        var i;
        for(i =0; i <= maxStep; i++ ) {
            shownode3(i);
            lastshow();
        }
    })
    document.getElementById("myRange").max = maxStep;
    document.getElementById("myRange").value = maxStep;
    document.getElementById("demo").innerHTML = maxStep;
    step=maxStep;
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
    });
    if(g.nodes(maxIndex)) {
        var node = g.node(maxIndex);
        node.style = "stroke: #1366eb; stroke-width: 10";
        //node.class = "wannahide"
    }

// Set up edges
    data["edges"].forEach(function (v) {
        g.setEdge(v["source"], v["target"], {label: v["label"],
            id:"edge"+ v["target"] +"marker"+ v["destroyed"] , labelId: "label"+ v["target"]+"marker"+ v["destroyed"]})
    });

// Create the renderer
    var render = new dagreD3.render();



// Set up an SVG group so that we can translate the final graph.
    var svg = d3.select("svg"),
        svgGroup = svg.append("g").attr("id", "theG");

    // Set up zoom support
    var zoom = d3.zoom().on('zoom', function () {
        transform = d3.event.transform;
        svgGroup.attr("transform","translate("+ transform.x + ", " + transform.y +") scale(" + transform.k + ")");


    });
    svg.call(zoom);

// Run the renderer. This is what draws the final graph.
    render(svgGroup, g);

    var widthQuarter = -(g.graph().width)/4;

    var initialScale = (typeof transform != 'undefined') ? transform.k : 0.75;
    var initialX = (typeof transform != 'undefined') ? transform.x :widthQuarter  ; //850
    var initialY = (typeof transform != 'undefined') ? transform.y : 20;
    var t = d3.zoomIdentity.translate(initialX, initialY).scale(initialScale);
    svg.call(zoom.transform, t);

    svg.attr("width", width = "50%");
    svg.attr('height', g.graph().height + 40)
        .style("border", "1px solid black");
    addToolTip();
}

function updateGraph() {
    d3.selectAll("g > *").remove();

}
// On mouse over display tool tip box
function showToolTipBox(e, displayInfo) {
    var offsetX = 20;
    var offsetY = 0;
    var positionX = e.pageX;
    var positionY = e.pageY;
    d3.select("#boxContent").html("<p>" + displayInfo + "</p>");
    d3.select("#infoBox").style("left", function () {
        return positionX + offsetX + "px";
    }).style("top", function () {
        return positionY + offsetY + "px";
    }).style("visibility", "visible");
}

// On mouse out hide the tool tip box
function hideToolTipBox() {
    d3.select("#infoBox").style("visibility", "hidden");
}

function addToolTip(){
    d3.selectAll(".node").on("mouseover", function (d) {
        var message;
        if (parseInt(d) > 100000) {
            //message = "<span class=\" bold \">type</span>: function call node <br>" + "<span class=\" bold \">dblclick</span>: Select function";
            message = "Test1";
        } else {
            var node = myVar.nodes.find(function (n) {
                return n.index === parseInt(d);
            });
            message = "<span class=\" bold \">ARG_ID: </span><span class=\"standard\">" + node.index + "</span>";
            message += "<br> <span class=\" bold \">Label: </span><span class=\"standard\">" + node.label + "</span>";
        }
        showToolTipBox(d3.event, message);
    }).on("mouseout", function () {
        hideToolTipBox();
    });

}

/* When the user clicks on the button,
toggle between hiding and showing the dropdown content */
function myFunction() {
    document.getElementById("myDropdown").classList.toggle("show");
}

// Close the dropdown if the user clicks outside of it
window.onclick = function(event) {
    if (!event.target.matches('.dropbtn')) {
        var dropdowns = document.getElementsByClassName("dropdown-content");
        var i;
        for (i = 0; i < dropdowns.length; i++) {
            var openDropdown = dropdowns[i];
            if (openDropdown.classList.contains('show')) {
                openDropdown.classList.remove('show');
            }
        }
    }
}