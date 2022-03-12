// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.blocking;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

class ReducedEdge {
  private final Set<CFAEdge> summarizes = new HashSet<>();
  private final ReducedNode pointsTo;

  public void addEdge(CFAEdge pEdge) {
    summarizes.add(pEdge);
  }

  public void addEdge(ReducedEdge pEdge) {
    summarizes.addAll(pEdge.summarizes);
  }

  public ReducedEdge(ReducedNode pPointsTo) {
    pointsTo = pPointsTo;
  }

  public ReducedNode getPointsTo() {
    return pointsTo;
  }
}
