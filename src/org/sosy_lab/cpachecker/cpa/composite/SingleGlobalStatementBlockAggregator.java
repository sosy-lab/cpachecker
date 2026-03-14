// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.composite;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.dependencegraph.EdgeDefUseData;

class SingleGlobalStatementBlockAggregator extends StraightLineBlockAggregator {

  private final EdgeDefUseData.Extractor memoryAccessExtractor =
      new EdgeDefUseData.CachingExtractor(EdgeDefUseData.createExtractor(true, true));

  SingleGlobalStatementBlockAggregator(CFA pCfa) {
    super(pCfa);
  }

  @Override
  public boolean isValidMultiEdgeComponent(CFANode startNode, CFAEdge edge) {
    if (!super.isValidMultiEdgeComponent(startNode, edge)) {
      return false;
    }
    if (startNode.equals(edge.getPredecessor())) {
      return true;
    }

    boolean anyGlobalStatements = false;
    CFAEdge currentEdge = edge;
    while (true) {
      var accesses = memoryAccessExtractor.extract(currentEdge);
      if (!accesses.getUses().isEmpty() ||
          !accesses.getDefs().isEmpty() ||
          !accesses.getPointeeDefs().isEmpty() ||
          !accesses.getPointeeUses().isEmpty()) {
        if (anyGlobalStatements) {
          return false;
        }
        anyGlobalStatements = true;
      }

      CFANode predecessor = currentEdge.getPredecessor();
      if (startNode.equals(predecessor)) {
        return true;
      }
      assert predecessor.getEnteringEdges().size() == 1 : "Multi-edge component must be a straight line";
      currentEdge = predecessor.getEnteringEdges().iterator().next();
    }
  }
}
