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
 * This class provides strategies for iterating through a CFA (a set of {@link CFANode}s connected
 * by {@link CFAEdge}s). Strategies differ for example in the direction (forwards/backwards), and
 * whether summary edges are recognized.
 *
 * <p>Instances of this class are always immutable, thread-safe and may be re-used. Thus, care must
 * be taken when calling methods of this class which return a CFATraversal instance. This never
 * mutate the instance on which they are called!
 *
 * <p>Right code: <code>
 * CFATraversal traversal = CFATraversal.allEdgesForward();
 * traversal = traversal.backwards();
 * traversal.traverse(...);
 * </code> Wrong code: <code>
 * CFATraversal traversal = CFATraversal.allEdgesForward();
 * traversal.backwards(); // WRONG!!! Has no effect!
 * traversal.traverse(...);
 * </code> For traversing the CFA, a {@link CFAVisitor} needs to be given. Several default
 * implementations are available.
 *
 * <p>Important: The instances of this class do not track a set of already visited nodes. Thus a
 * visitor may be called several times for a single node. If the visitor never specifies to stop the
 * traversal and the CFA contains loops, this will produce an infinite loop! It is strongly
 * recommended to use the {@link NodeCollectingCFAVisitor} to prevent this and visit each node only
 * once (wrap your own visitor in it).
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
