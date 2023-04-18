// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFATraversal.NodeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

/**
 * This class provides a strategy for a forward depth-first-search through a CFA (a set of {@link
 * CFANode}s connected by {@link CFAEdge}s). A given visitor sees all edges in the manner of a
 * depth-first search. If an edge was already see, the traversal is aborted.
 *
 * <p>Example: For the following CFA, A - B - C | | D E - F assume that A is the root, then the CFA
 * is traversed as follows: (A, D), (A, B), (B, E), (E, F), (B, C)
 */
public class CFAEdgeDFSTraversal {

  private final Function<CFANode, Iterable<CFAEdge>> edgeSupplier;
  private final Predicate<CFAEdge> ignoreEdge;

  private CFAEdgeDFSTraversal(
      Function<CFANode, Iterable<CFAEdge>> pEdgeSupplier, Predicate<CFAEdge> pIgnoreEdge) {
    edgeSupplier = pEdgeSupplier;
    ignoreEdge = pIgnoreEdge;
  }

  public static CFAEdgeDFSTraversal dfs() {
    return new CFAEdgeDFSTraversal(CFATraversal.FORWARD_EDGE_SUPPLIER, Predicates.alwaysFalse());
  }

  public void traverseOnce(final CFANode startingNode, final CFATraversal.CFAVisitor visitor) {
    traverse(startingNode, new NodeCollectingCFAVisitor(visitor));
  }

  /**
   * Traverse through the CFA according to the strategy represented by the current instance,
   * starting at a given node and passing each encountered node and edge to a given visitor.
   *
   * @param startingNode The starting node.
   * @param visitor The visitor to notify.
   */
  public void traverse(final CFANode startingNode, final CFATraversal.CFAVisitor visitor) {

    record CFANodeCFAEdgePair(CFANode successor, CFAEdge enteringEdge) {}

    Deque<CFANodeCFAEdgePair> toProcess = new ArrayDeque<>();
    Set<CFANode> discovered = new LinkedHashSet<>();

    toProcess.addLast(new CFANodeCFAEdgePair(startingNode, null));

    while (!toProcess.isEmpty()) {
      CFANodeCFAEdgePair cfaNodeCFAEdgePair = toProcess.removeLast();
      CFANode currentNode = cfaNodeCFAEdgePair.successor();
      CFAEdge entering = cfaNodeCFAEdgePair.enteringEdge();
      if (entering != null) {
        TraversalProcess result = visitor.visitEdge(entering);
        if (result == TraversalProcess.ABORT) {
          return;
        }
        if (result == TraversalProcess.SKIP) {
          continue;
        }
      }
      if (discovered.contains(currentNode)) {
        continue;
      }
      discovered.add(currentNode);
      CFATraversal.TraversalProcess result = visitor.visitNode(currentNode);
      if (result == TraversalProcess.ABORT) {
        return;
      }
      if (result == TraversalProcess.SKIP) {
        continue;
      }
      for (CFAEdge edge : edgeSupplier.apply(currentNode)) {
        if (ignoreEdge.apply(edge)) {
          continue;
        }
        toProcess.add(new CFANodeCFAEdgePair(edge.getSuccessor(), edge));
      }
    }
  }
}
