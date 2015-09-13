/**
 * Created by magdalena on 06.07.15.
 */
//(function(){})() --> diese Schreibweise macht eine Funktion in JS "self-invoking"
(function() {
    var app = angular.module('report', []);
    app.controller('ReportController', function(){
        this.date = date;
        this.logo = logo;

        //preprocess Errorpath-Data for left content
        this.errorPathData = [];
        for(var j = 0; j<errorPathData.length; j++){
            if (errorPathData[j].desc.substring(0, "Return edge from".length) != "Return edge from" && errorPathData[j].desc != "Function start dummy edge") {
                this.errorPathData.push(errorPathData[j]);
            }
        }

        //Behaviour for Click-Elements in Errorpath, CFA, ARG
        this.clickedCFAElement = function($event){
            var y = $event.currentTarget.id;
            var z = y.parentElement.parentElement.getElementsByTagName("title")[0].innerHTML;
            window.alert(y + z);
        };
        this.clickedARGElement = function($event){
            var y = $event.currentTarget.id;
            var z = y.parentElement.parentElement.getElementsByTagName("title")[0].innerHTML;
            window.alert(y + z);
        };
        this.clickedErrpathElement = function($event){
            var y = $event.currentTarget.id;
            this.setLine(y);
        };

        this.lineMarked = false;
        this.markSource = function(index){
            if (this.lineMarked) {
                document.getElementsByClassName("markedSourceLine")[0].className = "prettyprint";
            }
            document.getElementsByClassName("prettyprint")[errorPathData[index].line].className = "markedSourceLine";
            this.lineMarked = true;
        };


        //CFA-Controller
        this.functions = functions;

        if (functions.indexOf("main" != -1)) {
            this.selectedOption = functions.indexOf("main");
        } else {
            this.selectedOption = 0;
        }

        this.setCFAFunction = function(value){
            this.selectedOption = value;
        };

        this.cfaFunctionIsSet = function(value){
            return value === this.selectedOption;
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
        this.tab = 1;

        this.setTab = function(value){
            this.tab = value;
        };

        this.tabIsSet = function(value){
            return this.tab === value;
        };


        //Code-Controller
        this.selected_ErrLine = null;
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
    });
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


function init(){
    var svgElements = document.getElementsByTagName("svg");
    //set IDs for all CFA-SVGs
    for(var i = 0; i<svgElements.length-1; i++){
        var graph = svgElements[i].getElementsByTagName("g")[0];
        graph.id = "cfaGraph-" + i.toString();
        var nodes = graph.getElementsByClassName("node");
        for(k = 0; k < nodes.length; k++){
            nodes[k].id = "cfa-" + nodes[k].getElementsByTagName("title")[0].innerHTML;
        }
        var edges = graph.getElementsByClassName("edge");
        for(var l=0; l<edges.length; l++){
            var edge = edges[l];
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

    //prepare Errorpath-Data for marking in the cfa
    var returnedges = {};
    for(key in fCallEdges){
        var fcalledge = fCallEdges[key];
        returnedges[fcalledge[1]] = fcalledge[0];
    }
    var errPathDataForCFA = [];
    for(var j = 0; j < errorPathData.length; j++){
        var source = errorPathData[j].source;
        var target = errorPathData[j].target;
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
    for(var m = 0; m < errPathDataForCFA.length; m++){
        var cfaEdge = document.getElementById("cfa-" + errPathDataForCFA[m]);
        var path = cfaEdge.getElementsByTagName("path")[0];
        var poly = cfaEdge.getElementsByTagName("polygon")[0];
        path.setAttribute("stroke", "red");
        poly.setAttribute("stroke", "red");
        poly.setAttribute("fill", "red");
    }

};
