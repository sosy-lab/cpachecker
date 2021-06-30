// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import org.sosy_lab.cpachecker.util.dependencegraph.DependenceGraph.DependenceType;

/** Formats a dependence graph edge for GraphViz dot output. */
class DGEdgeDotFormatter {

  private final DGNodeDotFormatter nodeFormatter = new DGNodeDotFormatter();

  public String format(DGNode pStart, DGNode pEnd, DependenceType pDependenceType) {
    String rep = getNodeRepresentation(pStart) + " -> " + getNodeRepresentation(pEnd);
    if (pDependenceType == DependenceType.CONTROL) {
      rep += " [style=dashed]";
    }
    return rep;
  }


  private String getNodeRepresentation(final DGNode pNode) {
    return nodeFormatter.getNodeRepresentation(pNode);
  }
}
