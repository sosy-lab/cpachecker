// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class TestTargetMinimizerBasicEssential {

  public Set<CFAEdge> reduceTargets(final Set<CFAEdge> testTargets) {
    Set<CFAEdge> targetsAfterRule1 = new HashSet<>(testTargets);
    // rule 1

    for (CFAEdge currentEdge : testTargets) {

      CFANode predecessor = currentEdge.getPredecessor();
      // if the predecessor has only a single incoming and outgoing edge we "remove" that incoming
      // edge from the graph by
      // checking if its part of the testgoals and ignoring it if its not. We then treat that edges
      // predecessor as the new predecessor
      while (predecessor.getNumLeavingEdges() == 1 && predecessor.getNumEnteringEdges() == 1) {
        if (testTargets.contains(predecessor.getEnteringEdge(0))) {
          targetsAfterRule1.remove(currentEdge);
        }
        predecessor = predecessor.getEnteringEdge(0).getPredecessor();
      }
      // if any of the incoming edges to the predecessor are a part of the testgoals and there are
      // no additional outgoing edges
      // then we can remove the edge under investigation from the testgoals
      if (predecessor.getNumLeavingEdges() == 1) {
        for (CFAEdge enteringEdge : CFAUtils.enteringEdges(predecessor)) {
          if (testTargets.contains(enteringEdge)) {
            targetsAfterRule1.remove(currentEdge);
            break;
          }
        }
      }
    }
    Set<CFAEdge> finalTargets = new HashSet<>(targetsAfterRule1);
    for (CFAEdge currentEdge : targetsAfterRule1) {

      CFANode successor = currentEdge.getSuccessor();

      while (successor.getNumLeavingEdges() == 1 && successor.getNumEnteringEdges() == 1) {
        if (testTargets.contains(successor.getLeavingEdge(0))) {
          finalTargets.remove(currentEdge);
        }
        successor = successor.getLeavingEdge(0).getSuccessor();
      }

      if (successor.getNumEnteringEdges() == 1) {
        for (CFAEdge leavingEdge : CFAUtils.leavingEdges(successor)) {
          if (testTargets.contains(leavingEdge)) {
            finalTargets.remove(currentEdge);
            break;
          }
        }
      }
    }

    return finalTargets;
  }
}
