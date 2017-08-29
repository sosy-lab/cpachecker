/**
 * Created by Magdalena Murr on 06.07.15.
 */
//(function(){})() --> this notation makes a function in JS "self-invoking"
(function() {
    var app = angular.module('report', []);

    app.controller('ReportController', ['$rootScope', '$scope', function($rootScope, $scope){
        $scope.date = Date.now();
        $scope.logo = "https://cpachecker.sosy-lab.org/logo.svg";
        $scope.help_externalFiles = "<p><b>CFA</b> (control flow automaton) shows the control flow of the program (one CFA for one function in the source-code)</p>" +
            "<p>- the errorpath is highlighted in red</p>" +
            "<p>- click on the function-nodes to jump to CFA of this function</p>" +
            "<p>- click on edges to jump to the relating line in the source-code</p>" +
            "<p><img src='app/circle.png' width='17px' height='17px'> normal element  <img src='app/box.png' width='25px' height='17px'> more normal elements</p>" +
            "<p><img src='app/diamond.png' width='25px' height='17px'> condition  <img src='app/function.png' width='25px' height='17px'> function (different CFA)</p>" +
            "<p><img src='app/doubleCircle.png' width='17px' height='17px'> loop head (graph-based)  <img src='app/doubleOctagon.png' width='25px' height='17px'> loop head (syntactic)</p>\n" +
            "<p><b>ARG</b> shows the errorpath as a graph</p>" +
            "<p>- the errorpath is highlighted in red</p>" +
            "<p>- click on nodes to jump to the relating node in the CFA</p>" +
            "<p>- click on edges to jump to the relating line in the source-code</p>" +
            "<p><img src='app/green.png' width='8px' height='8px'> covered state  <img src='app/orange.png' width='8px' height='8px'> not yet processed</p>" +
            "<p><img src='app/cornflowerblue.png' width='8px' height='8px'> important (depending on used analysis)  <img src='app/red.png' width='8px' height='8px'> target state</p>";
        $scope.help_errorpath = "<p>The errorpath leads to the error 'line by line' (Source) or 'edge by edge' (CFA)</p>\n" +
            "<p><b>-V-</b> Click to show all initialized variables and their values at that point</p>\n" +
            "<p><b>Edge-Description (Source-Code-View)</b> Click to jump to the relating edge in the CFA / node in the ARG / line in Source (depending on active tab)</p>\n" +
            "<p><b>Buttons (Prev, Start, Next)</b> Click to navigate through the errorpath and jump to the relating position in the active tab</p>\n" +
            "<p><b>Search</b>\n - You can search for words or numbers in the edge-descriptions (matches appear blue)\n" +
            "- You can search for value-assignments (variable names or their value) - it will highlight only where a variable has been initialized or where it has changed its value (matches appear green)</p>";
        //the tab that is shown
        $scope.tab = 1;

        $scope.$on("FirstTimeErrorpathElementIsSelected", function(event){
            $scope.setMarginForGraphs();
        });
        $scope.$on("ChangeTab", function(event, tab){
            $scope.setTab(tab);
        });

        $scope.setTab = function(tab){
            $scope.tab = tab;
        };
        $scope.tabIsSet = function(tab){
            return $scope.tab === tab;
        };

        $scope.setWidth = function(event) {
            if (mouseDown) {
                var wholeWidth = document.getElementById("externalFiles_section").offsetWidth + document.getElementById("errorpath_section").offsetWidth;
                document.getElementById("errorpath_section").style.width = (Math.round(event.clientX/wholeWidth*100) + "%");
                document.getElementById("externalFiles_section").style.width = (Math.round((wholeWidth - event.clientX)/wholeWidth*100) + "%");
            }
        };

        $scope.setMouseUp = function(){
            mouseDown = false;
            document.onselectstart = null;
            document.onmousedown = null;
            if($rootScope.selected_ErrLine != null) {
                $scope.setMarginForGraphs();
            }
        };

        $scope.setMouseDown = function(){
            mouseDown = true;
            //we need this so that no text gets marked when moving middleline
            document.onselectstart = function(){return false;};
            document.onmousedown = function(){return false;};
        };

        $scope.setMarginForGraphs = function(){
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

    }]);


    app.controller("SearchController", ['$rootScope', '$scope', function($rootScope, $scope){
        $scope.numOfValueMatches = 0;
        $scope.numOfDescriptionMatches = 0;

        $scope.checkIfEnter = function($event){
            if($event.keyCode == 13){
                $scope.searchFor();
            }
        };

        $scope.searchFor = function(){
            //you have to get the matches-element this way, because display:none does not allow direct search
            var matchesDiv = document.getElementsByClassName("markedValues")[0].parentNode;
            if(matchesDiv.style.display != "inline"){
                matchesDiv.style.display = "inline";
                document.getElementById("err_table").parentNode.style.height = "calc(100% - 160px)";
            }
            $scope.numOfValueMatches = 0;
            $scope.numOfDescriptionMatches = 0;
            var allMarkedValueElements = document.getElementsByClassName("markedValueElement");
            while(allMarkedValueElements.length != 0){
                allMarkedValueElements[0].classList.remove("markedValueElement");
            }
            var allMarkedDescElements = document.getElementsByClassName("markedDescElement");
            while(allMarkedDescElements.length != 0){
                allMarkedDescElements[0].classList.remove("markedDescElement");
            }
            var allMarkedValueDescElements = document.getElementsByClassName("markedValueDescElement");
            while(allMarkedValueDescElements.length != 0){
                allMarkedValueDescElements[0].classList.remove("markedValueDescElement");
            }
            var searchedString = document.getElementsByClassName("search-input")[0].value;

            if (searchedString.trim() != "" && !document.getElementById("optionExactMatch").checked) {
                for (var l = 0; l < $rootScope.errorPathData.length; l++) {
                    var errorPathElem = $rootScope.errorPathData[l];
                    if (errorPathElem.val.contains(searchedString) && errorPathElem.desc.contains(searchedString)) {
                        $scope.numOfValueMatches = $scope.numOfValueMatches + 1;
                        $scope.numOfDescriptionMatches = $scope.numOfDescriptionMatches + 1;
                        document.getElementById("errpath-" + l).getElementsByTagName("td")[1].classList.add("markedValueDescElement");
                    } else if (errorPathElem.val.contains(searchedString)) {
                        $scope.numOfValueMatches = $scope.numOfValueMatches + 1;
                        document.getElementById("errpath-" + l).getElementsByTagName("td")[1].classList.add("markedValueElement");
                    } else if (errorPathElem.desc.contains(searchedString)) {
                        $scope.numOfDescriptionMatches = $scope.numOfDescriptionMatches + 1;
                        document.getElementById("errpath-" + l).getElementsByTagName("td")[1].classList.add("markedDescElement");
                    }
                }
            } else if (searchedString.trim() != "" && document.getElementById("optionExactMatch").checked) {
                for (var l = 0; l < $rootScope.errorPathData.length; l++) {
                    var errorPathElem = $rootScope.errorPathData[l];
                    if (searchedString in errorPathElem.newValDict && errorPathElem.desc == searchedString) {
                        $scope.numOfValueMatches = $scope.numOfValueMatches + 1;
                        $scope.numOfDescriptionMatches = $scope.numOfDescriptionMatches + 1;
                        document.getElementById("errpath-" + l).getElementsByTagName("td")[1].classList.add("markedValueDescElement");
                    } else if (searchedString in errorPathElem.newValDict) {
                        $scope.numOfValueMatches = $scope.numOfValueMatches + 1;
                        document.getElementById("errpath-" + l).getElementsByTagName("td")[1].classList.add("markedValueElement");
                    } else if (errorPathElem.desc == searchedString) {
                        $scope.numOfDescriptionMatches = $scope.numOfDescriptionMatches + 1;
                        document.getElementById("errpath-" + l).getElementsByTagName("td")[1].classList.add("markedDescElement");
                    }
                }
            }
        };
    }]);


    app.controller("ValueAssignmentsController", ['$rootScope', '$scope', function($rootScope, $scope){
        //gets called when '-V-'button in errorpath is clicked
        $scope.showValues = function($event){
            var element = $event.currentTarget;
            if (element.classList.contains("markedTableElement")) {
                element.classList.remove("markedTableElement");
            } else {
                element.classList.add("markedTableElement");
            }
        };
    }]);


    app.controller("ErrorpathController", ['$rootScope', '$scope', function($rootScope, $scope){
        $rootScope.errorPathData = [];
        //the line (errorpath) that is selected
        $rootScope.selected_ErrLine = null;

        $scope.getValues = function(val){
            var values = {};
            if(val != "") {
                var singleStatements = val.split("\n");
                for (var i = 0; i < singleStatements.length-1; i++) {
                    values[singleStatements[i].split("==")[0].trim()] = singleStatements[i].split("==")[1].trim().slice(0,-1);
                }
            }
            return values;
        };

        var indentationlevel = 0;
        for(var a = 0; a<errorPathData.length; a++) {
            var errPathElem = errorPathData[a];
            //do not show start, return and blank edges
            if (errPathElem.desc.substring(0, "Return edge from".length) != "Return edge from" && errPathElem.desc != "Function start dummy edge" && errPathElem.desc != "") {
                if (errPathElem.source in fCallEdges) {
                    errPathElem.target = fCallEdges[errPathElem.source][0];
                }
                var newValues = $scope.getValues(errPathElem.val);
                errPathElem["newValDict"] = newValues;
                errPathElem["valDict"] = {};
                errPathElem["valString"] = "";
                if (a > 0) {
                    for (key in $rootScope.errorPathData[$rootScope.errorPathData.length - 1].valDict) {
                        errPathElem.valDict[key] = $rootScope.errorPathData[$rootScope.errorPathData.length - 1].valDict[key];
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
                //add indentation
                for(var b = 1; b <= indentationlevel; b++) {
                    errPathElem.desc = "   " + errPathElem.desc;
                }
                $rootScope.errorPathData.push(errPathElem);
            } else if(errPathElem.desc.substring(0, "Return edge from".length) == "Return edge from"){
                indentationlevel -= 1;
            } else if(errPathElem.desc == "Function start dummy edge"){
                indentationlevel += 1;
            }
        }

        $scope.clickedErrpathElement = function($event){
            var lineNumber = $event.currentTarget.parentElement.id;
            $scope.setLine(lineNumber);
            $rootScope.$broadcast('clickedErrorpathElement', lineNumber.substring("errpath-".length));
        };

        $scope.clickedErrpathButton = function($event){
            var button = $event.currentTarget.innerHTML;
            var line;
            if (button == "Prev" && ($rootScope.selected_ErrLine.substring("errpath-".length) == 0 || $rootScope.selected_ErrLine == null)) {
            } else if (button == "Prev") {
                line = parseInt($rootScope.selected_ErrLine.substring("errpath-".length)) - 1;
                $scope.setLine("errpath-" + line);
                $rootScope.$broadcast("clickedErrorpathButton", line);
            } else if (button == "Start" || button == "Next" && $rootScope.selected_ErrLine == null) {
                document.getElementById("err_table").parentNode.scrollTop = 0;
                $scope.setLine("errpath-" + 0);
                $rootScope.$broadcast("clickedErrorpathButton", 0);
            } else if (button == "Next" && ($rootScope.selected_ErrLine.substring("errpath-".length) == $rootScope.errorPathData.length -1)) {
            } else if (button == "Next") {
                line = parseInt($rootScope.selected_ErrLine.substring("errpath-".length)) + 1;
                $scope.setLine("errpath-" + line);
                $rootScope.$broadcast("clickedErrorpathButton", line);
            }
        };

        $scope.setLine = function(id){
            if ($rootScope.selected_ErrLine != null) {
                document.getElementById($rootScope.selected_ErrLine).style.outline = "none";
            } else {
                //The first time a line is selected
                $rootScope.$broadcast("FirstTimeErrorpathElementIsSelected");
            }
            document.getElementById(id).style.outline = "#df80ff solid 1px";
            $rootScope.selected_ErrLine = id;
        };
    }]);


    app.controller('ARGController', ['$rootScope', '$scope', function($rootScope, $scope){
        $scope.argNodeMarked = false;

        $scope.$on('clickedErrorpathElement', function(event, index){
            $scope.markARGNode(index);
        });
        $scope.$on("clickedErrorpathButton", function(event, index){
            $scope.markARGNode(index);
        });

        $scope.clickedARGElement = function($event){
            var y = $event.currentTarget.id;
            if (document.getElementById(y).classList.contains("edge")){
                $rootScope.$broadcast("ChangeTab", 3);
                var line = document.getElementById(y).getElementsByTagName("text")[0].innerHTML.split(":")[0].substring("Line ".length);
                $rootScope.$broadcast("clickedARGEdge", line);
            } else if (document.getElementById(y).classList.contains("node")){
                var cfaNodeNumber = document.getElementById(y).getElementsByTagName("text")[0].innerHTML.split("N")[1];
                $rootScope.$broadcast("ChangeTab", 1);
                $rootScope.$broadcast("clickedARGNode", cfaNodeNumber);
            }
        };

        $scope.markARGNode = function(index){
            var argElement = $rootScope.errorPathData[index].argelem;
            if($scope.argNodeMarked){
                document.getElementsByClassName("markedARGNode")[0].classList.remove("markedARGNode");
            }
            document.getElementById("arg-" + argElement).classList.add("markedARGNode");
            $scope.argNodeMarked = true;
            $scope.scrollToARGElement("arg-" + argElement);
        };

        $scope.scrollToARGElement = function(id){
            var element = document.getElementById(id);
            var argContent = document.getElementsByClassName("argContent")[0];
            var box = argContent.parentNode.getBoundingClientRect();
            var xScroll = argContent.parentNode.scrollLeft;
            var yScroll = argContent.parentNode.scrollTop;
            var bcr = element.getBoundingClientRect();
            var xMargin = (argContent.style.marginLeft).split("px")[0];
            var yMargin = (argContent.style.marginTop).split("px")[0];
            argContent.parentNode.scrollLeft = bcr.left + xScroll - box.left - xMargin + 20;
            argContent.parentNode.scrollTop =  bcr.top + yScroll - box.top - yMargin + 20;
        };
    }]);


    app.controller('CFAController', ["$rootScope", "$scope", function($rootScope, $scope){
        //available functions (cfa-graphs)
        $rootScope.functions = functions;
        //selected cfa-graph (index = -1 means the function does not exist)
        if (functions.indexOf("main" != -1)) {
            $rootScope.selectedCFAFunction = functions.indexOf("main");
        } else {
            $rootScope.selectedCFAFunction = 0;
        }
        $scope.cfaEdgeMarked = false;
        $scope.cfaNodeMarked = false;

        $scope.$on('clickedErrorpathElement', function(event, nodeNumber){
            $scope.markCFAedge(nodeNumber);
        });
        $scope.$on("clickedErrorpathButton", function(event, nodeNumber){
            $scope.markCFAedge(nodeNumber);
        });
        $scope.$on("clickedARGNode", function(event, nodeNumber){
            $scope.markCFANode(nodeNumber);
        });

        $scope.setCFAFunction = function(value){
            document.getElementsByClassName("cfaContent")[0].parentNode.scrollTop = 0;
            document.getElementsByClassName("cfaContent")[0].parentNode.scrollLeft = 0;
            $rootScope.$broadcast("clearCFAZoom");
            $rootScope.selectedCFAFunction = value;
        };
        $scope.cfaFunctionIsSet = function(value){
            return value === $rootScope.selectedCFAFunction;
        };

        //Behaviour for Click-Elements in CFA
        $scope.clickedCFAElement = function($event){
            var element = $event.currentTarget.id;
            if (document.getElementById(element).classList.contains("edge")){
                $rootScope.$broadcast("ChangeTab", 3);
                var line;
                var source;
                if (element.split("->")[1] > 100000 || element.split("->")[0].substring("cfa-".length) > 100000){
                    source = element.split("->")[0].substring("cfa-".length);
                    line = cfaInfo["edges"][source + "->" + fCallEdges[source][1]]["line"];
                } else if (element.split("->")[0].substring("cfa-".length) in combinedNodes){
                    var textfields = document.getElementById("cfa-" + element.split("->")[0].substring("cfa-".length)).getElementsByTagName("text");
                    source = textfields[textfields.length - 2].innerHTML;
                    line = cfaInfo["edges"][source + "->" + element.split("->")[1]]["line"];
                } else {
                    line = cfaInfo["edges"][element.substring("cfa-".length)]["line"];
                }
                $rootScope.$broadcast("clickedCFAEdge", line);
            } else if (document.getElementById(element).classList.contains("node") && (element.substring("cfa-".length) > 100000)) {
                var func = document.getElementById(element).getElementsByTagName("text")[0].innerHTML;
                $scope.setCFAFunction(functions.indexOf(func));
            }
        };

        $scope.markCFAedge = function(index){
            var source = $rootScope.errorPathData[index].source;
            var target = $rootScope.errorPathData[index].target;
            var funcIndex = $rootScope.functions.indexOf(cfaInfo["nodes"][source]["func"]);
            if(funcIndex != $rootScope.selectedCFAFunction) {
                $scope.setCFAFunction(funcIndex);
            }
            if(!(source in combinedNodes && target in combinedNodes)) {
                if(source in combinedNodes){
                    source = combinedNodes[source];
                }
                if ($scope.cfaEdgeMarked) {
                    document.getElementsByClassName("markedCFAEdge")[0].classList.remove("markedCFAEdge");
                }
                if ($scope.cfaNodeMarked) {
                    document.getElementsByClassName("markedCFANode")[0].classList.remove("markedCFANode");
                    $scope.cfaNodeMarked = false;
                }
                document.getElementById("cfa-" + source + "->" + target).classList.add("markedCFAEdge");
                $scope.scrollToCFAElement("cfa-" + source + "->" + target);
                $scope.cfaEdgeMarked = true;
            } else {
                $scope.markCFANode(combinedNodes[source]);
            }
        };

        $scope.markCFANode = function(nodenumber){
            if($scope.cfaNodeMarked){
                document.getElementsByClassName("markedCFANode")[0].classList.remove("markedCFANode");
            }
            if($scope.cfaEdgeMarked){
                document.getElementsByClassName("markedCFAEdge")[0].classList.remove("markedCFAEdge");
                $scope.cfaEdgeMarked = false;
            }
            $scope.setCFAFunction($rootScope.functions.indexOf(cfaInfo["nodes"][nodenumber]["func"]));
            if(!(nodenumber in combinedNodes)) {
                document.getElementById("cfa-" + nodenumber).classList.add("markedCFANode");
                $scope.scrollToCFAElement("cfa-" + nodenumber);
            } else {
                document.getElementById("cfa-" + combinedNodes[nodenumber]).classList.add("markedCFANode");
                $scope.scrollToCFAElement("cfa-" + combinedNodes[nodenumber]);
            }
            $scope.cfaNodeMarked = true;
        };

        $scope.scrollToCFAElement = function(id){
            var element = document.getElementById(id);
            var box = document.getElementsByClassName("cfaContent")[0].parentNode.getBoundingClientRect();
            var bcr = element.getBoundingClientRect();
            var cfaContent = document.getElementsByClassName("cfaContent")[0];
            var xScroll = cfaContent.parentNode.scrollLeft;
            var yScroll =  cfaContent.parentNode.scrollTop;
            var xMargin = (cfaContent.style.marginLeft).split("px")[0];
            var yMargin = (cfaContent.style.marginTop).split("px")[0];
            cfaContent.parentNode.scrollLeft = bcr.left + xScroll - box.left - xMargin + 20;
            cfaContent.parentNode.scrollTop = bcr.top + yScroll - box.top - yMargin + 20;
        };

    }]);


    app.controller('SourceController', ['$rootScope', '$scope', function($rootScope, $scope){
        //available sourcefiles
        $scope.sourceFiles = sourceFiles;
        $scope.selectedSourceFile = 0;
        $scope.lineMarked = false;

        $scope.$on('clickedErrorpathElement', function(event, lineNumber){
            $scope.markSource($rootScope.errorPathData[lineNumber].line);
        });
        $scope.$on('clickedErrorpathButton', function(event, lineNumber){
            $scope.markSource($rootScope.errorPathData[lineNumber].line);
        });
        $scope.$on('clickedCFAEdge', function(event, line){
            $scope.markSource(line);
        });
        $scope.$on('clickedARGEdge', function(event, line){
            $scope.markSource(line);
        });

        $scope.setSourceFile = function(value){
            $scope.selectedSourceFile = value;
        };
        $scope.sourceFileIsSet = function(value){
            return value === $scope.selectedSourceFile;
        };

        $scope.markSource = function(line){
            if ($scope.lineMarked) {
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
            $scope.lineMarked = true;
        };
    }]);


    app.controller('ZoomController', ['$rootScope', '$scope', function($rootScope, $scope){
        $scope.zoomFactorCFA = 100;
        $scope.zoomFactorARG = 100;

        $scope.$on("clearCFAZoom", function(){$scope.clearZoomCFA();});

        $scope.setZoom = function(id){
            if (id.contains("cfa")) {
                document.getElementById("cfaGraph-" + $rootScope.selectedCFAFunction).transform.baseVal.getItem(0).setScale($scope.zoomFactorCFA / 100, $scope.zoomFactorCFA / 100);
            } else if (id.contains("arg")){
                document.getElementById("argGraph-" + $rootScope.functions.length).transform.baseVal.getItem(0).setScale($scope.zoomFactorARG / 100, $scope.zoomFactorARG / 100);
            }
        };

        $scope.clearZoomCFA = function(){
            $scope.zoomFactorCFA = 100;
            document.getElementById("cfaGraph-" + $rootScope.selectedCFAFunction).transform.baseVal.getItem(0).setScale($scope.zoomFactorCFA/100, $scope.zoomFactorCFA/100);
        };
    }]);
})();


var mouseDown = false;
var functions = []; //FUNCTIONS
var fCallEdges = {}; //FCALLEDGES
var cfaInfo = {}; //CFAINFO
var errorPathData = {}; //ERRORPATH
var combinedNodes = {}; //COMBINEDNODES
var sourceFiles = []; //SOURCEFILES

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