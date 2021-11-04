// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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

    for (DGNode n : pDg.getAllNodes()) {
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
