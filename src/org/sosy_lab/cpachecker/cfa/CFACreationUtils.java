// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

/**
 * Helper class that contains some complex operations that may be useful during the creation of a
 * CFA.
 */
public class CFACreationUtils {

  // Static state is not beautiful, but here it doesn't matter.
  // Worst thing that can happen is too many dead code warnings.
  private static int lastDetectedDeadCode = -1;

  /**
   * This method adds this edge to the leaving and entering edges of its predecessor and successor
   * respectively, but it does so only if the edge does not contain dead code
   */
  public static void addEdgeToCFA(CFAEdge edge, LogManager logger) {
    addEdgeToCFA(edge, logger, true);
  }

  public static void addEdgeToCFA(CFAEdge edge, LogManager logger, boolean warnForDeadCode) {
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
        if (!warnForDeadCode) {
          level = Level.FINER;
        } else if (edge.getDescription().matches("^Goto: (switch|while|ldv)_\\d+(_[a-z0-9]+)?$")) {
          // don't mention dead code produced by CIL/LDV on normal log levels
          level = Level.FINER;
        } else if (edge.getPredecessor().getNodeNumber() == lastDetectedDeadCode) {
          // don't warn on subsequent lines of dead code
          level = Level.FINER;
        }

        logger.logf(
            level, "%s: Dead code detected: %s", edge.getFileLocation(), edge.getRawStatement());
      }

      lastDetectedDeadCode = edge.getSuccessor().getNodeNumber();
    }
  }

  /**
   * This method adds this edge to the leaving and entering edges of its predecessor and successor
   * respectively. It does so without further checks, so use with care and only if really necessary.
   */
  public static void addEdgeUnconditionallyToCFA(CFAEdge edge) {
    edge.getPredecessor().addLeavingEdge(edge);
    edge.getSuccessor().addEnteringEdge(edge);
  }

  /**
   * Returns true if a node is reachable, that is if it contains an incoming edge. Label nodes and
   * function start nodes are always considered to be reachable.
   */
  public static boolean isReachableNode(CFANode node) {
    return (node.getNumEnteringEdges() > 0)
        || (node instanceof FunctionEntryNode)
        || node.isLoopStart()
        || (node instanceof CFALabelNode);
  }

  /**
   * Remove nodes from the CFA beginning at a certain node n until there is a node that is reachable
   * via some other path (not going through n). Useful for eliminating dead node, if node n is not
   * reachable.
   */
  public static void removeChainOfNodesFromCFA(CFANode n) {
    if (n.getNumEnteringEdges() > 0) {
      return;
    }

    for (int i = n.getNumLeavingEdges() - 1; i >= 0; i--) {
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
}
