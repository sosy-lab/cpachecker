/**
 * Created by magdalena on 06.07.15.
 */
//(function(){})() --> diese Schreibweise macht eine Funktion in JS "self-invoking"
(function() {
    var app = angular.module('report', []);
    app.controller('ReportController', ['$anchorScroll', '$location', function($anchorScroll, $location){
        this.date = date;
        this.logo = logo;

        //Inititalize Tab 1
        this.tab = 1;

        //Selected Line in the Errorpath
        this.selected_ErrLine = null;

        //Available Functions (CFA-Graphs)
        this.functions = functions;

        //Selected CFA-Graph
        if (functions.indexOf("main" != -1)) {
            this.selectedCFAFunction = functions.indexOf("main");
        } else {
            this.selectedCFAFunction = 0;
        }

        this.zoomFactor = 100;

        //preprocess Errorpath-Data for left content
        this.errorPathData = [];

        this.getValues = function(val){
            var values = {};
            if(val != "") {
                var singleStatements = val.split("\n");
                for (var i = 0; i < singleStatements.length-1; i++) {
                    values[singleStatements[i].split("==")[0].trim()] = singleStatements[i].split("==")[1].trim().slice(0,-1);
                }
            }
            return values;
        };

        for(var a = 0; a<errorPathData.length; a++) {
            var errPathElem = errorPathData[a];
            if (errPathElem.desc.substring(0, "Return edge from".length) != "Return edge from" && errPathElem.desc != "Function start dummy edge" && errPathElem.desc != "") {
                if (errPathElem.source in fCallEdges) {
                    errPathElem.target = fCallEdges[errPathElem.source][0];
                }
                var newValues = this.getValues(errPathElem.val);
                errPathElem["valDict"] = {};
                errPathElem["valString"] = "";
                if (a > 0) {
                    for (key in this.errorPathData[this.errorPathData.length - 1].valDict) {
                        errPathElem.valDict[key] = this.errorPathData[this.errorPathData.length - 1].valDict[key];
                    }
                }
                if (newValues != {}) {
                    for (key in newValues) {
                        errPathElem.valDict[key] = newValues[key];
                    }
                }
                // if I do it in one of the for-loops before I get the new values doubled
                for (key in errPathElem.valDict){
                    errPathElem.valString = errPathElem.valString + key + ":  " + errPathElem.valDict[key] + "\n";
                }
                this.errorPathData.push(errPathElem);
            }
        }


        //Behaviour for Click-Elements in Errorpath
        this.clickedErrpathElement = function($event){
            var y = $event.currentTarget.parentElement.id;
            this.setLine(y);
            this.markSource(this.errorPathData[y.substring("errpath-".length)].line);
            this.markCFAedge(y.substring("errpath-".length));
            this.markARGnode(y.substring("errpath-".length));
        };

        this.clickedErrpathButton = function($event){
            var button = $event.currentTarget.innerHTML;
            var line;
            if (button == "Prev" && (this.selected_ErrLine.substring("errpath-".length) == 0 || this.selected_ErrLine == null)) {

            } else if (button == "Prev") {
                line = parseInt(this.selected_ErrLine.substring("errpath-".length)) - 1;
                this.setLine("errpath-" + line);
                this.markSource(this.errorPathData[line].line);
                this.markCFAedge(line);
                this.markARGnode(line);
            } else if (button == "Start") {
                this.setLine("errpath-" + 0);
                this.markSource(this.errorPathData[0].line);
                this.markCFAedge(0);
                this.markARGnode(0);
            } else if (button == "Next" && (this.selected_ErrLine.substring("errpath-".length) == this.errorPathData.length -1)) {

            } else if (button == "Next") {
                line = parseInt(this.selected_ErrLine.substring("errpath-".length)) + 1;
                this.setLine("errpath-" + line);
                this.markSource(this.errorPathData[line].line);
                this.markCFAedge(line);
                this.markARGnode(line);
            }
        };

        //gets called when element with source-node-number in Errorpath is clicked
        this.showValues = function($event){
            var element = $event.currentTarget;
            if (element.classList.contains("markedTableElement")) {
                element.classList.remove("markedTableElement");
            } else {
                element.classList.add("markedTableElement");
            }
        };

        this.numOfValueMatches = 0;
        this.numOfDescriptionMatches = 0;
        this.searchFor = function(){
            this.numOfValueMatches = 0;
            this.numOfDescriptionMatches = 0;
            var allMarkedValueElements = document.getElementsByClassName("markedValueElement");
            //window.alert(allMarkedValueElements.length);
            while(allMarkedValueElements.length != 0){
                allMarkedValueElements[0].classList.remove("markedValueElement");
            }
            var allMarkedDescElements = document.getElementsByClassName("markedDescElement");
            //window.alert(allMarkedDescElements.length);
            while(allMarkedDescElements.length != 0){
                allMarkedDescElements[0].classList.remove("markedDescElement");
            }
            var allMarkedValueDescElements = document.getElementsByClassName("markedValueDescElement");
            //window.alert(allMarkedValueDescElements.length);
            while(allMarkedValueDescElements.length != 0){
                allMarkedValueDescElements[0].classList.remove("markedValueDescElement");
            }
            var searchedString = document.getElementsByClassName("search-input")[0].value;
            if(searchedString.trim() != "") {
                for (var l = 0; l < this.errorPathData.length; l++) {
                    var errorPathElem = this.errorPathData[l];
                    if (errorPathElem.val.contains(searchedString) && errorPathElem.desc.contains(searchedString)) {
                        this.numOfValueMatches = this.numOfValueMatches + 1;
                        this.numOfDescriptionMatches = this.numOfDescriptionMatches + 1;
                        document.getElementById("errpath-" + l).getElementsByTagName("td")[1].classList.add("markedValueDescElement");
                    }
                    else if (errorPathElem.val.contains(searchedString)) {
                        this.numOfValueMatches = this.numOfValueMatches + 1;
                        document.getElementById("errpath-" + l).getElementsByTagName("td")[1].classList.add("markedValueElement");
                    }
                    else if (errorPathElem.desc.contains(searchedString)) {
                        this.numOfDescriptionMatches = this.numOfDescriptionMatches + 1;
                        document.getElementById("errpath-" + l).getElementsByTagName("td")[1].classList.add("markedDescElement");
                    }
                }
            }
        };

        //Behaviour for Click-Elements in CFA
        this.clickedCFAElement = function($event){
            var y = $event.currentTarget.id;
            if (document.getElementById(y).classList.contains("edge")){
                this.setTab(3);
                var line;
                var source;
                if (y.split("->")[1] > 100000 || y.split("->")[0].substring("cfa-".length) > 100000){
                    source = y.split("->")[0].substring("cfa-".length);
                    line = cfaInfo["edges"][source + "->" + fCallEdges[source][1]]["line"];
                } else if (y.split("->")[0].substring("cfa-".length) in combinedNodes){
                    var textfields = document.getElementById("cfa-" + y.split("->")[0].substring("cfa-".length)).getElementsByTagName("text");
                    source = textfields[textfields.length - 2].innerHTML;
                    line = cfaInfo["edges"][source + "->" + y.split("->")[1]]["line"];
                } else {
                    line = cfaInfo["edges"][y.substring("cfa-".length)]["line"];
                }
                this.markSource(line);
            } else if (document.getElementById(y).classList.contains("node") && (y.substring("cfa-".length) > 100000)) {
                var func = document.getElementById(y).getElementsByTagName("text")[0].innerHTML;
                this.setCFAFunction(functions.indexOf(func));
            }
        };


        //Behaviour for Click-Elements in ARG
        this.clickedARGElement = function($event){
            var y = $event.currentTarget.id;
            if (document.getElementById(y).classList.contains("edge")){
                this.setTab(3);
                var line = document.getElementById(y).getElementsByTagName("text")[0].innerHTML.split(":")[0].substring("Line ".length);
                this.markSource(line);
            } else if (document.getElementById(y).classList.contains("node")){
                var cfaNodeNumber = document.getElementById(y).getElementsByTagName("text")[0].innerHTML.split("N")[1];
                this.setTab(1);
                this.markCFANode(cfaNodeNumber);
            }
        };


        //mark correct line in the Source-Tab
        this.lineMarked = false;
        this.markSource = function(line){
            if (this.lineMarked) {
                document.getElementsByClassName("markedSourceLine")[0].className = "prettyprint";
            }
            document.getElementById("source-" + line).getElementsByTagName("pre")[1].className = "markedSourceLine";
            $location.hash("source-" + line);
            this.lineMarked = true;
        };

        //mark correct CFA-edge
        this.cfaEdgeMarked = false;
        this.markCFAedge = function(index){
            var source = this.errorPathData[index].source;
            var target = this.errorPathData[index].target;
            var funcIndex = this.functions.indexOf(cfaInfo["nodes"][source]["func"]);
            /*var funcChanged = false;
             if (!(this.cfaFunctionIsSet(funcIndex))){
             funcChanged = true;
             }*/
            this.setCFAFunction(funcIndex);
            if(!(source in combinedNodes && target in combinedNodes)) {
                if(source in combinedNodes){
                    source = combinedNodes[source];
                }
                if (this.cfaEdgeMarked) {
                    document.getElementsByClassName("markedCFAEdge")[0].classList.remove("markedCFAEdge");
                }
                if (this.cfaNodeMarked) {
                    document.getElementsByClassName("markedCFANode")[0].classList.remove("markedCFANode");
                    this.cfaNodeMarked = false;
                }
                document.getElementById("cfa-" + source + "->" + target).classList.add("markedCFAEdge");
                this.scrollToCFAElement("cfa-" + source + "->" + target);
                this.cfaEdgeMarked = true;
            } else {
                this.markCFANode(combinedNodes[source]);
            }
        };
        //mark correct CFA-node
        this.cfaNodeMarked = false;
        this.markCFANode = function(nodenumber){
            if(this.cfaNodeMarked){
                document.getElementsByClassName("markedCFANode")[0].classList.remove("markedCFANode");
            }
            if(this.cfaEdgeMarked){
                document.getElementsByClassName("markedCFAEdge")[0].classList.remove("markedCFAEdge");
                this.cfaEdgeMarked = false;
            }
            this.setCFAFunction(this.functions.indexOf(cfaInfo["nodes"][nodenumber]["func"]));
            if(!(nodenumber in combinedNodes)) {
                document.getElementById("cfa-" + nodenumber).classList.add("markedCFANode");
                this.scrollToCFAElement("cfa-" + nodenumber);
            } else {
                document.getElementById("cfa-" + combinedNodes[nodenumber]).classList.add("markedCFANode");
                this.scrollToCFAElement("cfa-" + combinedNodes[nodenumber]);
            }
            this.cfaNodeMarked = true;
        };
        //scroll to correct CFA-element
        this.scrollToCFAElement = function(id){
            var element = document.getElementById(id);
            var box = document.getElementsByClassName("cfaContent")[0].parentNode.getBoundingClientRect();
            /* FUNKTIONIERT auch nicht, weil es nachm cfa-function-wechseln nicht in Kraft tritt FALSCH: Tritt in Kraft, aber ClientRect von unsichtbar ist 0 (s.u.)
             if (funcChanged) {
             xScroll = 0;
             yScroll = 0;
             }*/
            //PROBLEM: Koordinaten des Elements sind 0,wenn es nicht sichtbar ist
            var bcr = element.getBoundingClientRect();
            document.getElementsByClassName("cfaContent")[0].parentNode.scrollLeft = bcr.left - box.left - 10;
            document.getElementsByClassName("cfaContent")[0].parentNode.scrollTop = bcr.top - box.top - 50;
        };

        //mark correct ARG-node
        this.argNodeMarked = false;
        this.markARGnode = function(index){
            var argElement = this.errorPathData[index].argelem;
            if(this.argNodeMarked){
                document.getElementsByClassName("markedARGNode")[0].classList.remove("markedARGNode");
            }
            document.getElementById("arg-" + argElement).classList.add("markedARGNode");
            this.argNodeMarked = true;
            this.scrollToARGElement("arg-" + argElement);
        };
        // scroll to correct ARG-node
        this.scrollToARGElement = function(id){
            var element = document.getElementById(id);
            var box = document.getElementsByClassName("argContent")[0].getBoundingClientRect();
            var xScroll = document.getElementsByClassName("argContent")[0].scrollLeft;
            var yScroll = document.getElementsByClassName("argContent")[0].scrollTop;
            var bcr = element.getBoundingClientRect();
            document.getElementsByClassName("argContent")[0].scrollLeft = bcr.left + xScroll - box.left - 10;
            document.getElementsByClassName("argContent")[0].scrollTop =  bcr.top + yScroll - box.top - 50;
        };


        //CFA-Controller
        this.setCFAFunction = function(value){
            document.getElementsByClassName("cfaContent")[0].parentNode.scrollTop = 0;
            document.getElementsByClassName("cfaContent")[0].parentNode.scrollLeft = 0;
            this.selectedCFAFunction = value;
        };
        this.cfaFunctionIsSet = function(value){
            return value === this.selectedCFAFunction;
        };


        //Sections-Controller
        this.setWidth = function(event) {
            if (mouseDown) {
                wholeWidth = document.getElementById("externalFiles_section").offsetWidth + document.getElementById("errorpath_section").offsetWidth;
                document.getElementById("errorpath_section").style.width = (Math.round(event.clientX/wholeWidth*100) + "%");
                document.getElementById("externalFiles_section").style.width = (Math.round((wholeWidth - event.clientX)/wholeWidth*100) + "%");
            }
        };
        this.setMouseUp = function(){
            mouseDown = false;
            document.onselectstart = null;
            document.onmousedown = null;
        };


        //Tab-Controller
        this.setTab = function(value){
            this.tab = value;
        };
        this.tabIsSet = function(value){
            $anchorScroll();
            return this.tab === value;
        };


        //Code-Controller
        this.setMouseDown = function(){
            mouseDown = true;
            //werden benötigt, damit kein Text markiert wird beim Verschieben der middleline (übernommen aus altem)
            document.onselectstart = function(){return false;};
            document.onmousedown = function(){return false;};
        };

        this.setLine = function(id){
            if (this.selected_ErrLine != null) {
                document.getElementById(this.selected_ErrLine).style.outline = "none";
            }
            document.getElementById(id).style.outline = "red solid 1px";
            this.selected_ErrLine = id;
        };

        this.setZoom = function(){
            document.getElementById("cfaGraph-" + this.selectedCFAFunction).transform.baseVal.getItem(0).setScale(this.zoomFactor/100, this.zoomFactor/100);
        };
    }]);
})();

var date = Date.now();
var logo = "http://cpachecker.sosy-lab.org/logo.svg";
var wholeWidth;
var mouseDown = false;
var functions = []; //FUNCTIONS
var fCallEdges = {}; //FCALLEDGES
var cfaInfo = {}; //CFAINFO
var errorPathData = {}; //ERRORPATH
var combinedNodes = {}; //COMBINEDNODES


$(function () {
    $('[data-toggle="popover"]').popover()
})

function init(){
    var svgElements = document.getElementsByTagName("svg");
    //set IDs for all CFA-SVGs
    for(var r = 0; r<svgElements.length-1; r++){
        var graph = svgElements[r].getElementsByTagName("g")[0];
        graph.id = "cfaGraph-" + r.toString();
        var nodes = graph.getElementsByClassName("node");
        for(var s = 0; s < nodes.length; s++){
            nodes[s].id = "cfa-" + nodes[s].getElementsByTagName("title")[0].innerHTML;
        }
        var edges = graph.getElementsByClassName("edge");
        for(var t=0; t<edges.length; t++){
            var edge = edges[t];
            var fromto = [];
            if(edge.getElementsByTagName("title")[0].innerHTML.split("&#45;&gt;")[1] != null){
                fromto = edge.getElementsByTagName("title")[0].innerHTML.split("&#45;&gt;");
            } else if (edge.getElementsByTagName("title")[0].innerHTML.split("&#45;>")[1] != null) {
                fromto = edge.getElementsByTagName("title")[0].innerHTML.split("&#45;>");
            } else if (edge.getElementsByTagName("title")[0].innerHTML.split("-&gt;")[1] != null) {
                fromto = edge.getElementsByTagName("title")[0].innerHTML.split("-&gt;");
            } else {
                fromto = edge.getElementsByTagName("title")[0].innerHTML.split("->");
            }
            edge.id = "cfa-" + fromto[0] + "->" + fromto[1];
        }
    }
    //set IDs for the ARG-SVG
    graph = svgElements[svgElements.length-1].getElementsByTagName("g")[0];
    graph.id = "argGraph-" + (svgElements.length-1).toString();
    nodes = graph.getElementsByClassName("node");
    for(var u = 0; u < nodes.length; u++){
        nodes[u].id = "arg-" + nodes[u].getElementsByTagName("title")[0].innerHTML;
    }
    edges = graph.getElementsByClassName("edge");
    for(var v=0; v<edges.length; v++){
        edge = edges[v];
        fromto = [];
        if(edge.getElementsByTagName("title")[0].innerHTML.split("&#45;&gt;")[1] != null){
            fromto = edge.getElementsByTagName("title")[0].innerHTML.split("&#45;&gt;");
        } else if (edge.getElementsByTagName("title")[0].innerHTML.split("&#45;>")[1] != null) {
            fromto = edge.getElementsByTagName("title")[0].innerHTML.split("&#45;>");
        } else if (edge.getElementsByTagName("title")[0].innerHTML.split("-&gt;")[1] != null) {
            fromto = edge.getElementsByTagName("title")[0].innerHTML.split("-&gt;");
        } else {
            fromto = edge.getElementsByTagName("title")[0].innerHTML.split("->");
        }
        edge.id = "arg-" + fromto[0] + "->" + fromto[1];
    }

    //prepare Errorpath-Data for marking in the cfa
    var returnedges = {};
    for(key in fCallEdges){
        var fcalledge = fCallEdges[key];
        returnedges[fcalledge[1]] = fcalledge[0];
    }
    var errPathDataForCFA = [];
    for(var w = 0; w < errorPathData.length; w++){
        var source = errorPathData[w].source;
        var target = errorPathData[w].target;
        if(source in combinedNodes && target in combinedNodes){

        }
        else if(source in combinedNodes){
            errPathDataForCFA.push(combinedNodes[source] + "->" + target);
        }
        else if (source in fCallEdges){
            errPathDataForCFA.push(source + "->" + fCallEdges[source][0]);
        }
        else if (target in returnedges){
            errPathDataForCFA.push(returnedges[target] + "->" + target);
        } else {
            errPathDataForCFA.push(source + "->" + target);
        }
    }
    for(var x = 0; x < errPathDataForCFA.length; x++){
        var cfaEdge = document.getElementById("cfa-" + errPathDataForCFA[x]);
        var path = cfaEdge.getElementsByTagName("path")[0];
        var poly = cfaEdge.getElementsByTagName("polygon")[0];
        var text = cfaEdge.getElementsByTagName("text")[0];
        path.setAttribute("stroke", "red");
        poly.setAttribute("stroke", "red");
        poly.setAttribute("fill", "red");
        if(text != undefined) {
            text.setAttribute("fill", "red");
        }
    }
};

