// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class TestTargetMinimizerBasicEssential {


  public TestTargetMinimizerBasicEssential() {

  }

  public Set<CFAEdge> reduceTargets(Set<CFAEdge> testTargets) {
    Set<CFAEdge> rule1results = new HashSet<>(testTargets);
    // rule 1
    Iterator<CFAEdge> iter = testTargets.iterator();

    while (iter.hasNext()) {
      CFAEdge currentEdge = iter.next();

      CFANode predecessor = currentEdge.getPredecessor();
      // if the predecessor has only a single incoming and outgoing edge we "remove" that incoming
      // edge from the graph by
      // checking if its part of the testgoals and ignoring it if its not. We then treat that edges
      // predecessor as the new predecessor
      while (predecessor.getNumLeavingEdges() == 1 && predecessor.getNumEnteringEdges() == 1) {
        if (testTargets.contains(predecessor.getEnteringEdge(0))) {
          rule1results.remove(currentEdge);
        }
        predecessor = predecessor.getEnteringEdge(0).getPredecessor();
      }
      // if any of the incoming edges to the predecessor are a part of the testgoals and there are
      // no additional outgoing edges
      // then we can remove the edge under investigation from the testgoals
      if (predecessor.getNumLeavingEdges() != 1) {
        continue;
      } else {
        for (int i = 0; i < predecessor.getNumEnteringEdges(); i++) {
          if (testTargets.contains(predecessor.getEnteringEdge(i))) {
            rule1results.remove(currentEdge);
            break;
          }
        }

      }
    }
    Set<CFAEdge> rule2results = new HashSet<>(rule1results);
    iter = rule1results.iterator();
    while (iter.hasNext()) {
      CFAEdge currentEdge = iter.next();

      CFANode successor = currentEdge.getSuccessor();

      while (successor.getNumLeavingEdges() == 1 && successor.getNumEnteringEdges() == 1) {
        if (testTargets.contains(successor.getLeavingEdge(0))) {
          rule1results.remove(currentEdge);
        }
        successor = successor.getLeavingEdge(0).getSuccessor();
      }

      if (successor.getNumEnteringEdges() != 1) {
        continue;
      } else {
        for (int i = 0; i < successor.getNumLeavingEdges(); i++) {
          if (testTargets.contains(successor.getLeavingEdge(i))) {
            rule2results.remove(currentEdge);
            break;

          }
        }

      }
    }

    return rule2results;
  }

}
