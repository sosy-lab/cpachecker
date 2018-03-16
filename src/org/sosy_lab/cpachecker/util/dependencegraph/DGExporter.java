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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.util.dependencegraph.DependenceGraph.DependenceType;

/** Util methods for exporting {@link DependenceGraph DependenceGraphs}. */
public class DGExporter {

  public static void generateDOT(final Appendable pW, final DependenceGraph pDg)
      throws IOException {
    List<String> nodes = new ArrayList<>();
    List<String> edges = new ArrayList<>();
    DGNodeDotFormatter nodeFormatter = new DGNodeDotFormatter();
    Table<DGNode, DGNode, DependenceType> adjacencyMatrix = pDg.getMatrix();

    for (DGNode n : pDg.getNodes()) {
      nodes.add(nodeFormatter.getNodeString(n));
    }

    DGEdgeDotFormatter edgeFormatter = new DGEdgeDotFormatter();
    for (Cell<DGNode, DGNode, DependenceType> e : adjacencyMatrix.cellSet()) {
      DGNode dependentOn = checkNotNull(e.getRowKey());
      DGNode dependingOn = checkNotNull(e.getColumnKey());
      DependenceType type = checkNotNull(e.getValue());
      edges.add(edgeFormatter.format(dependentOn, dependingOn, type));
    }

    pW.append("digraph " + "DependenceGraph" + " {\n");
    Joiner.on("\n").appendTo(pW, nodes);
    pW.append('\n');
    Joiner.on("\n").appendTo(pW, edges);
    pW.append("\n}");
  }
}
