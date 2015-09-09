/**
 * Created by magdalena on 06.07.15.
 */
(function() {
    var app = angular.module('report', []);
    app.controller('ReportController', function(){
        this.date = date;
        this.logo = logo;
        this.clickedElement = function($event){
            var x = $event.currentTarget;
            var y = x.getElementsByTagName('title')[0].innerHTML;
            var z = x.parentElement.parentElement.getElementsByTagName("title")[0].innerHTML;
            window.alert(y + z);
        };
        this.lineMarked = false;
        this.markSource = function(index){
            if(this.lineMarked) {
                document.getElementsByClassName("markedSourceLine")[0].className = "prettyprint";
            }
            document.getElementsByClassName("prettyprint")[errorPath[index].line].className = "markedSourceLine";
            this.lineMarked = true;
        };
        this.setReferences = function(){
            var cfa_nodes = {};
            var arg_nodes = {};
            var nodeReferences = document.getElementsByClassName("node");
            for(var i=0; i<nodeReferences.length; i++){
                var node = nodeReferences[i];
                var nodeTitle = node.getElementsByTagName("title")[0].innerHTML;
                if(node.parentElement.parentElement.getElementsByTagName("title")[0].innerHTML === "ARG"){
                    arg_nodes[nodeTitle] = node;
                }
                else {
                    cfa_nodes[nodeTitle] = node;
                }
            }
            var cfa_edges = {};
            var arg_edges = {};
            var edgeReferences = document.getElementsByClassName("edge");
            for(var j=0; j<edgeReferences.length; j++){
                var edge = edgeReferences[j];
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
                var edgeTitle = fromto[0] + "->" + fromto[1];
                if(edge.parentElement.parentElement.getElementsByTagName("title")[0].innerHTML === "ARG"){
                    arg_edges[edgeTitle] = edge;
                }
                else{
                    cfa_edges[edgeTitle] = edge;
                }
            }

            var errorPath_extended = [];
            n = 0;
            for (m = 0; m < errorPath.length; m++) {
                if (errorPath[m].source in fCallEdges) {
                    errorPath_extended[n] = {
                        "source": errorPath[m].source,
                        "target": fCallEdges[errorPath[m].source][0]
                    };
                    n += 1;
                }
            }

            for (var k = 0; k < errorPath.length; k++) {
                if ((errorPath[k].source + "->" + errorPath[k].target) in cfa_edges) {
                    var errEdge = cfa_edges[errorPath[k].source + "->" + errorPath[k].target];
                    var path = errEdge.getElementsByTagName("path")[0];
                    var poly = errEdge.getElementsByTagName("polygon")[0];
                    path.setAttribute("stroke", "red");
                    poly.setAttribute("stroke", "red");
                    poly.setAttribute("fill", "red");
                }
            }
            for(var l=0; l<errorPath_extended.length; l++){
                if ((errorPath_extended[l].source + "->" + errorPath_extended[l].target) in cfa_edges) {
                    var errEdge1 = cfa_edges[errorPath_extended[l].source + "->" + errorPath_extended[l].target];
                    var path1 = errEdge1.getElementsByTagName("path")[0];
                    var poly1 = errEdge1.getElementsByTagName("polygon")[0];
                    path1.setAttribute("stroke", "red");
                    poly1.setAttribute("stroke", "red");
                    poly1.setAttribute("fill", "red");
                }
            }
        };

    });

    app.controller('SectionsController', function(){
        this.setWidth = function(event) {
            if (mouseDown) {
                wholeWidth = document.getElementById("pictures").offsetWidth + document.getElementById("errorpath").offsetWidth;
                document.getElementById("errorpath").style.width = (Math.round(event.clientX/wholeWidth*100) + "%");
                document.getElementById("pictures").style.width = (Math.round((wholeWidth - event.clientX)/wholeWidth*100) + "%");
            }
        };
        this.setMouseUp = function(){
            mouseDown = false;
        };
    });

    app.controller('TabController', function(){
        this.tab = 1;
        this.setTab = function(value){
            this.tab = value;
        };
        this.isSet = function(value){
            return this.tab === value;
        };
    });

    app.controller('CodeController', function(){
        this.errorPath = errorPath;
        this.selected_ErrLine = -1;
        this.setMouseDown = function(){
            mouseDown = true;
        };
        this.setLine = function(line){
            if(this.selected_ErrLine != -1) {
                document.getElementById("err_table").getElementsByTagName('tr')[this.selected_ErrLine].style.outline = "none";
            }
            document.getElementById("err_table").getElementsByTagName('tr')[line].style.outline = "red solid 1px";
            this.selected_ErrLine = line;
        }

    });

    app.controller('CFAController', function(){
        this.functions = functions;
        if(functions.indexOf("main" != -1)) {
            this.selectedOption = functions.indexOf("main");
        }
        else{
            this.selectedOption = 0;
        }
        this.setOption = function(value){
            this.selectedOption = value;
        };
        this.isSet = function(value){
            return value === this.selectedOption;
        };
    });


    var wholeWidth;
    var mouseDown = false;
    var logo = "http://cpachecker.sosy-lab.org/logo.svg";
    var date = Date.now();
    var functions = []; //FUNCTIONS
    var fCallEdges = {}; //FCALLEDGES
    var cfaInfo = {}; //CFAINFO
    var errorPath = {}; //ERRORPATH
    var combinedNodes = {}; //COMBINEDNODES

})();
