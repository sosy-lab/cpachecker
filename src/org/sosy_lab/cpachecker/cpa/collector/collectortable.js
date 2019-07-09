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


function dagretest() {

    var myData = {}; //ARG_JSON_INPUT
    myJSON = JSON.stringify(myData);
    localStorage.setItem("localStoredJSON", myJSON);
    text = localStorage.getItem("localStoredJSON");
    states = JSON.parse(text);


    // Create the input graph
    var g = new dagreD3.graphlib.Graph()
        .setGraph({})
        .setDefaultEdgeLabel(function() { return {}; });


//for testing purposes
  /** var myshit = {"nodes":[{"func":"mainbullshit","index":32,"label":"32 @ N5\nmainbullshit\nValueAnalysisState: []\\","type":"not-expanded"},
            {"func":"main","index":33,"label":"33 @ N5\nmain\nValueAnalysisState: [main::x=NumericValue [number=1] (int), main::y=NumericValue [number=1] (int), main::z=NumericValue [number=0] (int)]\\","type":""},
            {"func":"main","index":36,"label":"36 @ N0\nmain exit\nValueAnalysisState: []\\","type":"not-expanded"},
            {"func":"main","index":39,"label":"39 @ N0\nmain exit\nValueAnalysisState: []\\","type":"not-expanded"},
            {"func":"main","index":40,"label":"40 @ N0\nmain exit\nValueAnalysisState: []\\","type":""},
            {"func":"main","index":21,"label":"21 @ N1\nmain entry\nValueAnalysisState: []\\","type":""},
            {"func":"main","index":22,"label":"22 @ N12\nmain\nValueAnalysisState: []\\","type":""},
            {"func":"main","index":23,"label":"23 @ N2\nmain\nValueAnalysisState: []\\","type":""},
            {"func":"main","index":24,"label":"24 @ N3\nmain\nValueAnalysisState: [main::x=NumericValue [number=0] (int)]\\","type":""},
            {"func":"main","index":25,"label":"25 @ N4\nmain\nValueAnalysisState: [main::x=NumericValue [number=0] (int), main::z=NumericValue [number=0] (int)]\\","type":""},
            {"func":"main","index":26,"label":"26 @ N6\nmain\nValueAnalysisState: [main::x=NumericValue [number=0] (int), main::y=NumericValue [number=1] (int), main::z=NumericValue [number=0] (int)]\\","type":""},
            {"func":"main","index":27,"label":"27 @ N7\nmain\nValueAnalysisState: [main::x=NumericValue [number=0] (int), main::z=NumericValue [number=0] (int)]\\","type":""},
            {"func":"main","index":28,"label":"28 @ N9\nmain\nValueAnalysisState: [main::x=NumericValue [number=0] (int), main::z=NumericValue [number=1] (int)]\\","type":""},
            {"func":"main","index":29,"label":"29 @ N8\nmain\nValueAnalysisState: [main::x=NumericValue [number=1] (int), main::y=NumericValue [number=1] (int), main::z=NumericValue [number=0] (int)]\\","type":""}],
        "edges":[{"file":"doc\/examples\/exampleSM.c","line":"2","source":23,"label":"Line 2\nint x = 0;","type":"DeclarationEdge","target":24},
            {"file":"<none>","source":21,"label":"Lines 0 - 1:\nINIT GLOBAL VARS\nint main(int y);","type":"BlankEdge","lines":"0 - 1:","target":22},
            {"file":"doc\/examples\/exampleSM.c","line":"5","source":25,"label":"Line 5\n[y == 1]","type":"AssumeEdge","target":26},
            {"file":"<none>","line":"0","source":28,"label":"Line 0\n","type":"BlankEdge","target":32},
            {"file":"doc\/examples\/exampleSM.c","line":"5","source":25,"label":"Line 5\n[!(y == 1)]","type":"AssumeEdge","target":27},
            {"file":"doc\/examples\/exampleSM.c","line":"10","source":32,"label":"Line 10\nreturn 10 \/ (x - y);","type":"ReturnStatementEdge","target":39},
            {"file":"doc\/examples\/exampleSM.c","line":"10","source":32,"label":"Line 10\nreturn 10 \/ (x - y);","type":"ReturnStatementEdge","target":36},
            {"file":"doc\/examples\/exampleSM.c","line":"10","source":32,"label":"Line 10\nreturn 10 \/ (x - y);","type":"ReturnStatementEdge","target":40},
            {"file":"doc\/examples\/exampleSM.c","line":"3","source":24,"label":"Line 3\nint z = 0;","type":"DeclarationEdge","target":25},
            {"file":"doc\/examples\/exampleSM.c","line":"8","source":27,"label":"Line 8\nz = 1;","type":"StatementEdge","target":28},
            {"file":"<none>","line":"0","source":29,"label":"Line 0\n","type":"BlankEdge","target":33},
            {"file":"<none>","line":"0","source":22,"label":"Line 0\nFunction start dummy edge","type":"BlankEdge","target":23},
            {"file":"doc\/examples\/exampleSM.c","line":"6","source":26,"label":"Line 6\nx = 1;","type":"StatementEdge","target":29},
            {"file":"doc\/examples\/exampleSM.c","line":"10","source":33,"label":"Line 10\nreturn 10 \/ (x - y);","type":"ReturnStatementEdge","target":39},
            {"file":"<none>","line":"0","source":29,"label":"Line 0\n","type":"BlankEdge","target":32}] }**/

// Set up nodes
   states["nodes"].forEach(function(v) { g.setNode(v["index"], { label: v["label"], class:v["type"] }); });


    g.nodes().forEach(function(v) {
        var node = g.node(v);
        // Round the corners of the nodes
        node.rx = node.ry = 5;
    });

// Set up edges

  states["edges"].forEach(function(v) { g.setEdge(v["source"], v["target"],{label: v["label"]})});


// Create the renderer
    var render = new dagreD3.render();

// Set up an SVG group so that we can translate the final graph.
    var svg = d3.select("svg"),
        svgGroup = svg.append("g");

    // Set up zoom support
    var zoom = d3.zoom().on("zoom", function() {
        svgGroup.attr("transform", d3.event.transform);
    });
    svg.call(zoom);

// Run the renderer. This is what draws the final graph.
    render(svgGroup, g);

// Center the graph
    var xCenterOffset = svg.attr("width",width="100%");

    svgGroup.attr("transform", "translate(" + xCenterOffset + ", 20)");

    svg.attr("height", g.graph().height + 40)
        .style("border", "1px solid black");
}

