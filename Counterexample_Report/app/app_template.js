/**
 * Created by magdalena on 06.07.15.
 */
//(function(){})() --> this notation makes a function in JS "self-invoking"
(function() {
    var app = angular.module('report', []);
    app.controller('ReportController', function(){
        this.date = date;
        this.logo = logo;

        //the tab that is shown
        this.tab = 1;

        //the line (errorpath) that is selected
        this.selected_ErrLine = null;

        //available functions (cfa-graphs)
        this.functions = functions;

        //available sourcefiles
        this.sourceFiles = sourceFiles;
        this.selectedSourceFile = 0;

        //help-button-content
        this.help_errorpath = help_errorpath;
        this.help_externalFiles = help_externalFiles;

        //selected cfa-graph (index = -1 means the function does not exist)
        if (functions.indexOf("main" != -1)) {
            this.selectedCFAFunction = functions.indexOf("main");
        } else {
            this.selectedCFAFunction = 0;
        }

        this.zoomFactorCFA = 100;
        this.zoomFactorARG = 100;

        //preprocess errorpath-data for left content
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

        var level = 0;
        for(var a = 0; a<errorPathData.length; a++) {
            var errPathElem = errorPathData[a];
            //do not show start, return and blank edges
            if (errPathElem.desc.substring(0, "Return edge from".length) != "Return edge from" && errPathElem.desc != "Function start dummy edge" && errPathElem.desc != "") {
                if (errPathElem.source in fCallEdges) {
                    errPathElem.target = fCallEdges[errPathElem.source][0];
                }
                var newValues = this.getValues(errPathElem.val);
                errPathElem["newValDict"] = newValues;
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
                // if I do it in one of the for-loops before, I get the new values doubled
                for (key in errPathElem.valDict){
                    errPathElem.valString = errPathElem.valString + key + ":  " + errPathElem.valDict[key] + "\n";
                }
                for(var b = 1; b <= level; b++) {
                    errPathElem.desc = "   " + errPathElem.desc;
                }
                this.errorPathData.push(errPathElem);
            } else if(errPathElem.desc.substring(0, "Return edge from".length) == "Return edge from"){
                level -= 1;
            } else if(errPathElem.desc == "Function start dummy edge"){
                level += 1;
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
            } else if (button == "Start" || button == "Next" && this.selected_ErrLine == null) {
                document.getElementById("err_table").parentNode.scrollTop = 0;
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

        //gets called when '-V-'button in errorpath is clicked
        this.showValues = function($event){
            var element = $event.currentTarget;
            if (element.classList.contains("markedTableElement")) {
                element.classList.remove("markedTableElement");
            } else {
                element.classList.add("markedTableElement");
            }
        };

        //this is for the search-functionality
        this.checkIfEnter = function($event){
            if($event.keyCode == 13){
                this.searchFor();
            }
        }

        this.numOfValueMatches = 0;
        this.numOfDescriptionMatches = 0;
        this.searchFor = function(){
            //you have to get the element this way, because display:none does not allow direct search
            var matchesDiv = document.getElementsByClassName("markedValues")[0].parentNode;
            if(matchesDiv.style.display != "inline"){
                matchesDiv.style.display = "inline";
                document.getElementById("err_table").parentNode.style.height = "calc(100% - 160px)";
            }
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

            if (searchedString.trim() != "" && !document.getElementById("optionExactMatch").checked) {
                for (var l = 0; l < this.errorPathData.length; l++) {
                    var errorPathElem = this.errorPathData[l];
                    if (errorPathElem.val.contains(searchedString) && errorPathElem.desc.contains(searchedString)) {
                        this.numOfValueMatches = this.numOfValueMatches + 1;
                        this.numOfDescriptionMatches = this.numOfDescriptionMatches + 1;
                        document.getElementById("errpath-" + l).getElementsByTagName("td")[1].classList.add("markedValueDescElement");
                    } else if (errorPathElem.val.contains(searchedString)) {
                        this.numOfValueMatches = this.numOfValueMatches + 1;
                        document.getElementById("errpath-" + l).getElementsByTagName("td")[1].classList.add("markedValueElement");
                    } else if (errorPathElem.desc.contains(searchedString)) {
                        this.numOfDescriptionMatches = this.numOfDescriptionMatches + 1;
                        document.getElementById("errpath-" + l).getElementsByTagName("td")[1].classList.add("markedDescElement");
                    }
                }
            } else if (searchedString.trim() != "" && document.getElementById("optionExactMatch").checked) {
                for (var l = 0; l < this.errorPathData.length; l++) {
                    var errorPathElem = this.errorPathData[l];
                    if (searchedString in errorPathElem.newValDict && errorPathElem.desc == searchedString) {
                        this.numOfValueMatches = this.numOfValueMatches + 1;
                        this.numOfDescriptionMatches = this.numOfDescriptionMatches + 1;
                        document.getElementById("errpath-" + l).getElementsByTagName("td")[1].classList.add("markedValueDescElement");
                    } else if (searchedString in errorPathElem.newValDict) {
                        this.numOfValueMatches = this.numOfValueMatches + 1;
                        document.getElementById("errpath-" + l).getElementsByTagName("td")[1].classList.add("markedValueElement");
                    } else if (errorPathElem.desc == searchedString) {
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
            var element = document.getElementById("source-" + line);
            var box = document.getElementsByClassName("sourceContent")[0].parentNode.getBoundingClientRect();
            var bcr = element.getBoundingClientRect();
            var sourceContent = document.getElementsByClassName("sourceContent")[0];
            var yScroll =  sourceContent.parentNode.scrollTop;
            var yMargin = Math.round((document.getElementById("externalFiles_section").offsetHeight/ 2)-50);
            sourceContent.parentNode.scrollTop = bcr.top + yScroll - box.top - yMargin;
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
            if(funcIndex != this.selectedCFAFunction) {
                this.setCFAFunction(funcIndex);
            }
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
            /*if (funcChanged) {
             xScroll = 0;
             yScroll = 0;
             }
             *///PROBLEM: Coordinates of element are 0, if it is not visible
            var bcr = element.getBoundingClientRect();
            var cfaContent = document.getElementsByClassName("cfaContent")[0];
            var xScroll = cfaContent.parentNode.scrollLeft;
            var yScroll =  cfaContent.parentNode.scrollTop;
            var xMargin = (cfaContent.style.marginLeft).split("px")[0];
            var yMargin = (cfaContent.style.marginTop).split("px")[0];
            cfaContent.parentNode.scrollLeft = bcr.left + xScroll - box.left - xMargin;
            cfaContent.parentNode.scrollTop = bcr.top + yScroll - box.top - yMargin;
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
            var argContent = document.getElementsByClassName("argContent")[0];
            var box = argContent.parentNode.getBoundingClientRect();
            var xScroll = argContent.parentNode.scrollLeft;
            var yScroll = argContent.parentNode.scrollTop;
            var bcr = element.getBoundingClientRect();
            var xMargin = (argContent.style.marginLeft).split("px")[0];
            var yMargin = (argContent.style.marginTop).split("px")[0];
            argContent.parentNode.scrollLeft = bcr.left + xScroll - box.left - xMargin;
            argContent.parentNode.scrollTop =  bcr.top + yScroll - box.top - yMargin;
        };

        //CFA-Controller
        this.setCFAFunction = function(value){
            document.getElementsByClassName("cfaContent")[0].parentNode.scrollTop = 0;
            document.getElementsByClassName("cfaContent")[0].parentNode.scrollLeft = 0;
            this.clearZoomCFA();
            this.selectedCFAFunction = value;
        };
        this.cfaFunctionIsSet = function(value){
            return value === this.selectedCFAFunction;
        };

        //Source-Controller
        this.setSourceFile = function(value){
            this.selectedSourceFile = value;
        };
        this.sourceFileIsSet = function(value){
            return value === this.selectedSourceFile;
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
            if(this.selected_ErrLine != null) {
              this.setMarginForGraphs();
            }
        };

        //Tab-Controller
        this.setTab = function(value){
            this.tab = value;
        };
        this.tabIsSet = function(value){
            return this.tab === value;
        };

        //Code-Controller
        this.setMouseDown = function(){
            mouseDown = true;
            //we need this so that no text gets marked when moving middleline
            document.onselectstart = function(){return false;};
            document.onmousedown = function(){return false;};
        };

        this.setLine = function(id){
            if (this.selected_ErrLine != null) {
                document.getElementById(this.selected_ErrLine).style.outline = "none";
            } else {
              //The first time a line is selected
              this.setMarginForGraphs();
            }
            document.getElementById(id).style.outline = "#df80ff solid 1px";
            this.selected_ErrLine = id;
        };

        this.setMarginForGraphs = function(){
            var width = (Math.round((document.getElementById("externalFiles_section").offsetWidth/ 2)-10) + "px");
            var height = (Math.round(document.getElementById("externalFiles_section").offsetHeight/ 2) + "px");
            var cfaContent = document.getElementsByClassName("cfaContent")[0];
            var argContent = document.getElementsByClassName("argContent")[0];
            cfaContent.style.marginLeft = (width);
            cfaContent.style.marginRight = (width);
            cfaContent.style.marginTop = (height);
            cfaContent.style.marginBottom = (height);
            argContent.style.marginLeft = (width);
            argContent.style.marginRight = (width);
            argContent.style.marginTop = (height);
            argContent.style.marginBottom = (height);
        };

        this.setZoom = function(id){
            if (id.contains("cfa")) {
                document.getElementById("cfaGraph-" + this.selectedCFAFunction).transform.baseVal.getItem(0).setScale(this.zoomFactorCFA / 100, this.zoomFactorCFA / 100);
            } else if (id.contains("arg")){
                document.getElementById("argGraph-" + this.functions.length).transform.baseVal.getItem(0).setScale(this.zoomFactorARG / 100, this.zoomFactorARG / 100);
            }
        };
        this.clearZoomCFA = function(){
            this.zoomFactorCFA = 100;
            document.getElementById("cfaGraph-" + this.selectedCFAFunction).transform.baseVal.getItem(0).setScale(this.zoomFactorCFA/100, this.zoomFactorCFA/100);
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
var sourceFiles = []; //SOURCEFILES

var help_externalFiles = "<p><b>CFA</b> (control flow automaton) shows the control flow of the program (one cfa for one function in the source-code)</p>" +
    "<p>- the errorpath is highlighted in red</p>" +
    "<p>- click on the 'function'-nodes to jump to CFA of this function</p>" +
    "<p>- click on edges to jump to the relating line in the source-code</p>" +
    "<p><img src='app/circle.png' width='17px' height='17px'> normal element  <img src='app/box.png' width='25px' height='17px'> more normal elements</p>" +
    "<p><img src='app/diamond.png' width='25px' height='17px'> condition  <img src='app/function.png' width='25px' height='17px'> function (different CFA)</p>" +
    "<p><img src='app/doubleCircle.png' width='17px' height='17px'> loop head (graph-based)  <img src='app/doubleOctagon.png' width='25px' height='17px'> loop head (syntactic)</p>\n" +
    "<p><b>ARG</b> shows the errorpath as a graph</p>" +
    "<p>- the errorpath is highlighted in red</p>" +
    "<p>- click on nodes to jump to the relating node in the CFA</p>" +
    "<p>- click on edge to jump to the relating line in the source-code</p>" +
    "<p><img src='app/green.png' width='8px' height='8px'> covered state  <img src='app/orange.png' width='8px' height='8px'> not yet processed</p>" +
    "<p><img src='app/cornflowerblue.png' width='8px' height='8px'> important (depending on used analysis)  <img src='app/red.png' width='8px' height='8px'> target state</p>";
var help_errorpath = "The errorpath leads to the error 'line by line' (Source) or 'edge by edge' (CFA)\n\n" +
    "left column: the source-node of the relating edge in the CFA\n" +
    " - click on the number : show all initialized variables and their values at that point\n\n" +
    "right column: the description (what is executed at this point)\n" +
    " - click on the text : jump to the relating edge in the CFA or node in the ARG or line in Source (depending on active tab)\n\n" +
    "Buttons (Prev, Start, Next) : click to navigate through the errorpath and jump to the relating position in the active tab\n\n" +
    "Search : you can search for words or numbers in the 'description'-part (blue)\n  and you can search for variables and their value - it will only show you, where the variable has been initialized or where it has changed its value (green)\n" +
    "tip: if you search for the (full) name of a variable, add a blank space at the end";

$(function () {
  $('[data-toggle="popover"]').popover({ html : true })
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
      if(nodes[s].getElementsByTagName("title")[0].innerHTML > 100000){
        nodes[s].classList.add("functionNode");
      }
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

  //If we do not have an Errorpath
  if(errorPathData.length == undefined){
    document.getElementById("errorpath_section").style.display = "none";
    document.getElementById("externalFiles_section").style.width = "100%";
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
    } else if(source in combinedNodes){
      errPathDataForCFA.push(combinedNodes[source] + "->" + target);
    } else if (source in fCallEdges){
      errPathDataForCFA.push(source + "->" + fCallEdges[source][0]);
    } else if (target in returnedges){
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