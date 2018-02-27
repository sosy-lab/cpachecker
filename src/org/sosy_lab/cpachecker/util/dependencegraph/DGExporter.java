/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.base.Joiner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Util methods for exporting {@link DependenceGraph DependenceGraphs}. */
public class DGExporter {

  public static void generateDOT(final Appendable pW, final DependenceGraph pDg)
      throws IOException {
    List<String> nodes = new ArrayList<>();
    List<String> edges = new ArrayList<>();
    DGNodeDotFormatter nodeFormatter = new DGNodeDotFormatter();
    List<DGNode> sortedNodes = new ArrayList<>(pDg.getNodes());
    List<DGEdge> sortedEdges = new ArrayList<>(pDg.getEdges());

    sortedNodes.sort(Comparator.comparing(DGNode::toString));
    sortedEdges.sort(Comparator.comparing(DGEdge::toString));
    for (DGNode n : sortedNodes) {
      nodes.add(nodeFormatter.getNodeString(n));
    }

    DGEdgeVisitor<String> edgeFormatter = new DGEdgeDotFormatter();
    for (DGEdge e : sortedEdges) {
      edges.add(e.accept(edgeFormatter));
    }

    pW.append("digraph " + "DependenceGraph" + " {\n");
    Joiner.on("\n").appendTo(pW, nodes);
    pW.append('\n');
    Joiner.on("\n").appendTo(pW, edges);
    pW.append("\n}");
  }
}
