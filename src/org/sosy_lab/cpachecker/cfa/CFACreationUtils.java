/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa;

import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;

/**
 * Helper class that contains some complex operations that may be useful during
 * the creation of a CFA.
 */
public class CFACreationUtils {

  // Static state is not beautiful, but here it doesn't matter.
  // Worst thing that can happen is too many dead code warnings.
  private static int lastDetectedDeadCode = -1;

  /**
   * This method adds this edge to the leaving and entering edges
   * of its predecessor and successor respectively, but it does so only
   * if the edge does not contain dead code
   */
  public static void addEdgeToCFA(CFAEdge edge, LogManager logger) {
    CFANode predecessor = edge.getPredecessor();

    // check control flow branching at predecessor
    if (edge.getEdgeType() == CFAEdgeType.AssumeEdge) {
      assert predecessor.getNumLeavingEdges() <= 1;
      if (predecessor.getNumLeavingEdges() > 0) {
        assert predecessor.getLeavingEdge(0).getEdgeType() == CFAEdgeType.AssumeEdge;
      }

    } else {
      assert predecessor.getNumLeavingEdges() == 0;
    }

    // no check control flow merging at successor, we might have many incoming edges

    // check if predecessor is reachable
    if (isReachableNode(predecessor)) {

      // all checks passed, add it to the CFA
      addEdgeUnconditionallyToCFA(edge);

    } else {
      // unreachable edge, don't add it to the CFA

      if (!edge.getDescription().isEmpty()) {
        // warn user, but not if its due to dead code produced by CIL
        Level level = Level.INFO;
        if (edge.getDescription().matches("^Goto: (switch|while)_\\d+_[a-z0-9]+$")) {
          // don't mention dead code produced by CIL on normal log levels
          level = Level.FINER;
        } else if (edge.getPredecessor().getNodeNumber() == lastDetectedDeadCode) {
          // don't warn on subsequent lines of dead code
          level = Level.FINER;
        }

        logger.logf(level, "%s: Dead code detected: %s", edge.getFileLocation(), edge.getRawStatement());
      }

      lastDetectedDeadCode = edge.getSuccessor().getNodeNumber();
    }
  }

  /**
   * This method adds this edge to the leaving and entering edges
   * of its predecessor and successor respectively.
   * It does so without further checks, so use with care and only if really
   * necessary.
   */
  public static void addEdgeUnconditionallyToCFA(CFAEdge edge) {
    edge.getPredecessor().addLeavingEdge(edge);
    edge.getSuccessor().addEnteringEdge(edge);
  }

  /**
   * Returns true if a node is reachable, that is if it contains an incoming edge.
   * Label nodes and function start nodes are always considered to be reachable.
   */
  public static boolean isReachableNode(CFANode node) {
    return (node.getNumEnteringEdges() > 0)
        || (node instanceof FunctionEntryNode)
        || (node.isLoopStart())
        || (node instanceof CLabelNode);
  }

  /**
   * Remove nodes from the CFA beginning at a certain node n until there is a node
   * that is reachable via some other path (not going through n).
   * Useful for eliminating dead node, if node n is not reachable.
   */
  public static void removeChainOfNodesFromCFA(CFANode n) {
    if (n.getNumEnteringEdges() > 0) {
      return;
    }

    for (int i = n.getNumLeavingEdges()-1; i >= 0; i--) {
      CFAEdge e = n.getLeavingEdge(i);
      CFANode succ = e.getSuccessor();

      removeEdgeFromNodes(e);
      removeChainOfNodesFromCFA(succ);
    }
  }

  public static void removeEdgeFromNodes(CFAEdge e) {
    e.getPredecessor().removeLeavingEdge(e);
    e.getSuccessor().removeEnteringEdge(e);
  }

  public static void removeSummaryEdgeFromNodes(FunctionSummaryEdge e) {
    e.getPredecessor().removeLeavingSummaryEdge(e);
    e.getSuccessor().removeEnteringSummaryEdge(e);
  }
}
