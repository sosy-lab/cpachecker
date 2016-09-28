/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.smg.refiner.interpolation.flowdependencebased;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

import java.util.Map;
import java.util.Map.Entry;

public class SMGFlowDependencePlotter {

  private final Multimap<? extends SMGFlowDependenceVertice, ? extends SMGFlowDependenceEdge<? extends SMGFlowDependenceVertice>> graph;
  private final Map<? extends SMGFlowDependenceVertice, Integer> indexOfVertices;

  public SMGFlowDependencePlotter(
      SMGFlowDependenceGraph<? extends SMGFlowDependenceVertice, ? extends SMGFlowDependenceEdge<? extends SMGFlowDependenceVertice>> pFlowDependenceGraph) {
    graph = pFlowDependenceGraph.getAdjacencyList();

    ImmutableMap.Builder<SMGFlowDependenceVertice, Integer> indexBuilder = ImmutableMap.builder();
    int c = 1;
    for (SMGFlowDependenceVertice vertice : pFlowDependenceGraph.getVertices()) {
      indexBuilder.put(vertice, c);
      c++;
    }

    indexOfVertices = indexBuilder.build();
  }

  public String toDot() {
    StringBuilder resultGraph = new StringBuilder("digraph FlowDependenceGraph {");
    lineBreak(resultGraph);
    lineBreak(resultGraph);
    appendVerticesDotLabel(resultGraph);
    lineBreak(resultGraph);
    appendEdges(resultGraph);
    resultGraph.append("}");
    return resultGraph.toString();
  }

  private void lineBreak(StringBuilder pResultGraph) {
    pResultGraph.append("\n");
  }

  private void appendEdges(StringBuilder pResultGraph) {
    for (SMGFlowDependenceEdge<? extends SMGFlowDependenceVertice> edges : graph.values()) {
      SMGFlowDependenceVertice target = edges.getTarget();
      SMGFlowDependenceVertice source = edges.getSource();
      appendEdge(pResultGraph, target, source);
    }
  }

  private void appendEdge(StringBuilder pResultGraph, SMGFlowDependenceVertice pTarget,
      SMGFlowDependenceVertice pSource) {
    int indexOfTarget = indexOfVertices.get(pTarget);
    int indexOfSource = indexOfVertices.get(pSource);

    pResultGraph.append(indexOfTarget);
    pResultGraph.append("->");
    pResultGraph.append(indexOfSource);
    lineBreak(pResultGraph);
  }

  private void appendVerticesDotLabel(StringBuilder pResultGraph) {
    for (Entry<? extends SMGFlowDependenceVertice, Integer> entry : indexOfVertices.entrySet()) {
      SMGFlowDependenceVertice vertice = entry.getKey();
      int index = entry.getValue();
      appendVerticesDotLabel(index, vertice, pResultGraph);
    }
  }

  private void appendVerticesDotLabel(int pIndex, SMGFlowDependenceVertice pVertice,
      StringBuilder pResultGraph) {
    pResultGraph.append(pIndex);
    pResultGraph.append(" [shape=\"rectangle\" label=\"");
    pResultGraph.append(pVertice.toDotNodeLabel());
    pResultGraph.append("\"]");
    lineBreak(pResultGraph);
  }
}