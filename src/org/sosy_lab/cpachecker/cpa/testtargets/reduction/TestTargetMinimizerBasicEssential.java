// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets.reduction;

import com.google.common.base.Preconditions;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

/*
 * Based on paper
 * Takeshi Chusho: Test Data Selection and Quality Estimation Based on the Concept
 * of Essential Branches for Path Testing. IEEE TSE 13(5): 509-517 (1987)
 * https://doi.org/10.1109/TSE.1987.233196
 *
 * Only use rule 1 and 2
 */
public class TestTargetMinimizerBasicEssential {

  public Set<CFAEdge> reduceTargets(final Set<CFAEdge> testTargets) {
    Set<CFAEdge> targetsAfterRule1 = new HashSet<>(testTargets);
    // rule 1

    for (CFAEdge currentEdge : testTargets) {
      Preconditions.checkArgument(!TestTargetReductionUtils.isInputEdge(currentEdge));

      CFANode predecessor = currentEdge.getPredecessor();
      // if the predecessor has only a single incoming and outgoing edge we "remove" that incoming
      // edge from the graph by
      // checking if its part of the testgoals and ignoring it if its not. We then treat that edges
      // predecessor as the new predecessor
      while (predecessor.getNumLeavingEdges() == 1 && predecessor.getNumEnteringEdges() == 1) {
        if (targetsAfterRule1.contains(predecessor.getEnteringEdge(0))
            || TestTargetReductionUtils.isInputEdge(predecessor.getEnteringEdge(0))) {
          break;
        }
        predecessor = predecessor.getEnteringEdge(0).getPredecessor();
      }
      // if any of the incoming edges to the predecessor are a part of the testgoals and there are
      // no additional outgoing edges
      // then we can remove the edge under investigation from the testgoals
      if (predecessor.getNumLeavingEdges() == 1) {
        for (CFAEdge enteringEdge : CFAUtils.enteringEdges(predecessor)) {
          if (targetsAfterRule1.contains(enteringEdge)) {
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
        if (finalTargets.contains(successor.getLeavingEdge(0))) {
          break;
        }
        successor = successor.getLeavingEdge(0).getSuccessor();
      }

      if (successor.getNumEnteringEdges() == 1) {
        for (CFAEdge leavingEdge : CFAUtils.leavingEdges(successor)) {
          if (finalTargets.contains(leavingEdge)) {
            finalTargets.remove(currentEdge);
            break;
          }
        }
      }
    }

    return finalTargets;
  }
}
