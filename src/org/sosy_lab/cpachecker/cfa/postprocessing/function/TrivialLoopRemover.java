// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import static com.google.common.base.Verify.verify;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;

/** Replace trivial loops in the CFA with an edge to a {@link CFATerminationNode}. Cf. #1713 */
public class TrivialLoopRemover {

  public static void removeTrivialLoops(MutableCFA cfa) {
    removeSelfLoops(cfa);
    removeLongerLoops(cfa);
  }

  /** Replace self loops in the CFA with an edge to a {@link CFATerminationNode}. */
  private static void removeSelfLoops(MutableCFA cfa) {
    List<CFANode> selfLoops = new ArrayList<>();

    // Identify nodes with self loops
    for (CFANode node : cfa.nodes()) {
      if (node.getNumLeavingEdges() == 1 && node.getLeavingEdge(0).getSuccessor().equals(node)) {
        CFAEdge edge = node.getLeavingEdge(0);
        verify(edge instanceof BlankEdge, "self loop with non-trivial edge %s", edge);
        verify(node instanceof CFALabelNode, "self-loop node that is not a label node at %s", node);
        selfLoops.add(node);
      }
    }

    for (CFANode node : selfLoops) {
      // Remove self loop
      CFAEdge edge = node.getLeavingEdge(0);
      CFACreationUtils.removeEdgeFromNodes(edge);

      // Add termination node and edge to it.
      CFANode newTerminationNode = new CFATerminationNode(node.getFunction());
      cfa.addNode(newTerminationNode);
      CFAEdge newEdge =
          new BlankEdge(
              edge.getRawStatement(),
              edge.getFileLocation(),
              node,
              newTerminationNode,
              edge.getDescription());
      CFACreationUtils.addEdgeUnconditionallyToCFA(newEdge);
    }
  }

  /**
   * Replace loops with only blank edges in the CFA with an edge to a {@link CFATerminationNode}.
   */
  private static void removeLongerLoops(MutableCFA cfa) {
    List<CFANode> trivialLoopHeads = new ArrayList<>();

    // Identify loops that have only chain of blank edges leading back to themselves.s
    for (CFANode node : cfa.nodes()) {
      if (node.getNumEnteringEdges() > 1) { // This makes sure that we add only one node per loop.
        CFANode currentNode = node;
        while (!(currentNode instanceof CFALabelNode) // labels be relevant for specification
            && currentNode.getNumLeavingEdges() == 1
            && currentNode.getLeavingEdge(0) instanceof BlankEdge) {

          currentNode = currentNode.getLeavingEdge(0).getSuccessor();
          if (currentNode.equals(node)) {
            trivialLoopHeads.add(node);
            break;
          }
          if (currentNode.getNumEnteringEdges() > 1) {
            break;
          }
        }
      }
    }

    for (CFANode node : trivialLoopHeads) {
      // Cut the loop by removing first edge
      CFAEdge firstEdge = node.getLeavingEdge(0);
      CFACreationUtils.removeEdgeFromNodes(firstEdge);

      // Remove the rest of the loop body's nodes and edges until we reach node again
      CFANode currentNode = firstEdge.getSuccessor();
      while (currentNode.getNumEnteringEdges() == 0) {
        verify(currentNode.getNumLeavingEdges() == 1);
        CFAEdge edge = currentNode.getLeavingEdge(0);
        cfa.removeNode(currentNode);
        CFACreationUtils.removeEdgeFromNodes(edge);
        currentNode = edge.getSuccessor();
      }
      verify(node.equals(currentNode));
      verify(node.getNumLeavingEdges() == 0);
      verify(node.getNumEnteringEdges() == 1);

      // Add termination node and edge to it.
      CFANode newTerminationNode = new CFATerminationNode(node.getFunction());
      cfa.addNode(newTerminationNode);
      CFAEdge newEdge =
          new BlankEdge(
              firstEdge.getRawStatement(),
              firstEdge.getFileLocation(),
              node,
              newTerminationNode,
              firstEdge.getDescription());
      CFACreationUtils.addEdgeUnconditionallyToCFA(newEdge);
    }
  }
}
