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
var myVarNodes = myVar["nodes"];
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


        for(i=0;i<=(curVal);i++){
            shownode3(i);
        }
        step=curVal;
    }
}

function start(){

    var nodeIndexStart = myVar["nodes"][startIndex].index;

    $(document).ready(function(){
        shownode3(nodeIndexStart);
    });
    updateGraph();
    dagreGraphBuild(myVar);
}

function reset(){
    $(document).ready(function(){
    });
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
        $("#node" + nodeIndexStart).addClass("contentshow");
        $("#shownode").one("click",shownode3(step));
        document.getElementById("demo").innerHTML = step ;
        document.getElementById("myRange").value = step ;
    })
}

function getDataByNodeIndex(index) {
    return myVarNodes.filter(
        function(myVarNodes) {
            return myVarNodes.index == index
        }
    );
}

function shownode3(step) {
    var step = step;
    var start = -1;

    $("g.node").removeClass("contentshow");
    $("g.edgePath").removeClass("contentshow");
    $("g.edgePath").removeClass("sourceAlive");
    $("g.edgePath").removeClass("targetAlive");
    $("g.label").removeClass("contentshow");
    $("g.label").removeClass("sourceAlive");
    $("g.label").removeClass("targetAlive");
    $("g.edgeLabel").removeClass("contentshow");

    if(step <= maxStep){
        for (count = 0; count <= step; count++) {

            var nodeDataIndex = getDataByNodeIndex(count);
            var nodeIndexCount = nodeDataIndex[0].index;
            //var nodeIndexCount = count;
            var intervalStart = nodeDataIndex[0].intervalStart;
            var intervalStop = nodeDataIndex[0].intervalStop;
            var source= "source"+ (nodeIndexCount-1).toString();
            var source2= "source"+ (nodeIndexCount).toString();
            var target= "target"+ nodeIndexCount.toString();
            var source3= "source"+ (nodeIndexCount)+"target";

            if(intervalStop){
                stop = intervalStop;
                $("#node" + nodeIndexCount).addClass("stop" + intervalStop);
            }
            else {
                stop = maxStep+1;
                $("#node" + nodeIndexCount).addClass("stop" + stop);
            }

            if (intervalStart > start){

                $("#node" + nodeIndexCount).addClass("contentshow");
                if ($("#node" + nodeIndexCount).hasClass("merged")){
                    $("#node" + nodeIndexCount).addClass("mergedColored");
                }

                if ($("g.edgePath").hasClass(source2)) {
                    $("." + source2 + "").addClass("sourceAlive");
                    $("." + source2 + "").addClass("stop" + stop);
                    $("g.label[id^="+source3+"]").addClass("sourceAlive");
                    $("g.label[id^="+source3+"]").addClass("stop" + stop);


                }
                if ($("g.edgePath").hasClass(target)) {
                    $("." + target + "").addClass("targetAlive");
                    $("." + target + "").addClass("stop" + stop);
                    $("g.label[id$="+target+"]").addClass("targetAlive");
                    $("g.label[id$="+target+"]").addClass("stop" + stop);

                }


                $(".targetAlive.sourceAlive" ).addClass("contentshow");


                $(".stop" + nodeIndexCount ).removeClass("contentshow");
                $(".stop" + nodeIndexCount ).removeClass("targetAlive");
                $(".stop" + nodeIndexCount ).removeClass("sourceAlive");

                if ($("#node" + nodeIndexCount).hasClass("none")) {
                    $(".mergedColored" ).removeClass("mergedColored");
                }
                if ($("#node" + nodeIndexCount).hasClass("toMerge")) {
                    $(".mergedColored" ).removeClass("mergedColored");
                }
            }
        }
    }
    else {
        shownode3(maxStep);
        if ($("#node" + maxStep).hasClass("mergedColored")) {
            $("#node" + maxStep).removeClass("mergedColored");
        }
    }
}

function buttonFinalGraph(){
    $(document).ready(function(){
        $("g.node").addClass("contentshow");
        $("g.edgePath").addClass("contentshow");
        $("g.edgeLabel").addClass("contentshow");
        $(".merged").addClass("mergedColored");

    });
    document.getElementById("myRange").max = maxStep+1;
    document.getElementById("myRange").value = maxStep+1;
    document.getElementById("demo").innerHTML = maxStep+1;
    step=maxStep+1;
}

function buttonFinalGraph2(){
    $(document).ready(function(){
        var i;
        for(i =0; i <= maxStep; i++ ) {
            shownode3(i);
        }
        $(".merged").removeClass("mergedColored");
    });
    document.getElementById("myRange").max = maxStep+1;
    document.getElementById("myRange").value = maxStep+1;
    document.getElementById("demo").innerHTML = maxStep+1;
    step=maxStep+1;
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
    }

// Set up edges
    data["edges"].forEach(function (v) {
        g.setEdge(v["source"], v["target"], {label: v["label"] , class: "source" + v["source"]+ " "+ "target" + v["target"],
            id:"source"+ v["source"] + "target" + v["target"], labelId: "source"+ v["source"]+"target"+ v["target"]})
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
};