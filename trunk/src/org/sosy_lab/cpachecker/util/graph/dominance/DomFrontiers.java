// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph.dominance;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.Collections3;

/**
 * Represents dominance frontiers for nodes in a graph.
 *
 * <p>The dominance frontier of a node {@code D} consists of all nodes {@code F_i}, such that {@code
 * D} dominates a direct predecessor of {@code F_i}, but does not strictly dominate {@code F_i}.
 *
 * @param <T> the graph's node type
 */
public final class DomFrontiers<T> {

  private final DomInput<T> input;

  // frontiers.get(N) == dominance frontier of node N
  private final ImmutableList<ImmutableSet<T>> frontiers;

  private DomFrontiers(DomInput<T> pInput, ImmutableList<ImmutableSet<T>> pFrontiers) {
    input = pInput;
    frontiers = pFrontiers;
  }

  /**
   * Creates a new {@link DomFrontiers} instance that contains a dominance frontier for every node
   * in the specified dominator tree.
   *
   * @param <T> the graph's node type
   * @param pDomTree the dominator tree of the graph
   * @return a new {@link DomFrontiers} instance that contains a dominance frontier for every node
   *     in the specified dominator tree
   * @throws NullPointerException if {@code pDomTree == null}
   */
  public static <T> DomFrontiers<T> forDomTree(DomTree<T> pDomTree) {

    checkNotNull(pDomTree);

    return new DomFrontiers<>(pDomTree.getInput(), computeFrontiers(pDomTree));
  }

  /**
   * Returns a list containing the dominance frontier for each node. {@code frontiers.get(N) ==
   * dominance frontier of node N}.
   */
  private static <T> ImmutableList<ImmutableSet<T>> computeFrontiers(DomTree<T> pDomTree) {

    // For more information on the algorithm, see "A Simple, Fast Dominance Algorithm"
    // (Cooper et al.), Figure 5.

    DomInput<T> domInput = pDomTree.getInput();
    DomInput.PredecessorDataIterator predecessorsDataIterator = domInput.iteratePredecessorData();

    List<Set<Integer>> frontiers = new ArrayList<>(domInput.getNodeCount());
    for (int id = 0; id < domInput.getNodeCount(); id++) {
      frontiers.add(new HashSet<>());
    }

    while (predecessorsDataIterator.hasNextNode()) {

      int nodeId = predecessorsDataIterator.nextNode();

      if (!predecessorsDataIterator.hasNextPredecessor()) { // has no predecessors?
        continue;
      }

      int runner = predecessorsDataIterator.nextPredecessor();

      if (!predecessorsDataIterator.hasNextPredecessor()) { // has only one predecessor?
        continue;
      }

      do {

        while (runner != DomTree.UNDEFINED && runner != pDomTree.getParentId(nodeId)) {
          frontiers.get(runner).add(nodeId);
          runner = pDomTree.getParentId(runner);
        }

        if (predecessorsDataIterator.hasNextPredecessor()) {
          runner = predecessorsDataIterator.nextPredecessor();
        } else {
          runner = DomTree.UNDEFINED;
        }

      } while (runner != DomTree.UNDEFINED);
    }

    return Collections3.transformedImmutableListCopy(
        frontiers,
        frontier ->
            Collections3.transformedImmutableSetCopy(
                frontier, domInput::getNodeForReversePostOrderId));
  }

  /**
   * Returns the dominance frontier of the specified node.
   *
   * @param pNode the node to get the dominance frontier for
   * @return an immutable set representing the dominance frontier of the specified node
   * @throws NullPointerException if {@code pNode == null}
   * @throws IllegalArgumentException if {@code pNode} is unknown (i.e., was not visited during
   *     graph traversal for dominator tree construction)
   */
  public ImmutableSet<T> getFrontier(T pNode) {

    checkNotNull(pNode);

    @Nullable Integer id = input.getReversePostOrderId(pNode);
    checkArgument(id != null, "unknown node: %s", pNode);

    return frontiers.get(id);
  }

  /**
   * Returns the iterated dominance frontier for the specified set of nodes.
   *
   * <p>The dominance frontier of a set of nodes {@code Ds} is the union of their dominance
   * frontiers. The iterated dominance frontier of {@code Ds} is the transitive closure of the
   * dominance frontier of {@code Ds}.
   *
   * @param pNodes the set of nodes to get the iterated dominance frontier for
   * @return an immutable set consisting of all nodes in the iterated dominance frontier
   * @throws NullPointerException if {@code pNodes == null} or if any element is {@code null}
   * @throws IllegalArgumentException if any node in {@code pNodes} is unknown (i.e., was not
   *     visited during graph traversal for dominator tree construction)
   */
  public ImmutableSet<T> getIteratedFrontier(Set<T> pNodes) {

    checkNotNull(pNodes);

    Set<T> iteratedFrontier = new HashSet<>();

    Set<T> waitlisted = new HashSet<>();
    Deque<T> waitlist = new ArrayDeque<>();

    for (T node : pNodes) {

      checkNotNull(node);

      @Nullable Integer id = input.getReversePostOrderId(node);
      checkArgument(id != null, "pNodes contains a node that has no dominance frontier: %s", node);

      waitlist.add(node);
      waitlisted.add(node);
    }

    while (!waitlist.isEmpty()) {
      T node = waitlist.remove();
      for (T frontierNode : getFrontier(node)) {
        if (iteratedFrontier.add(frontierNode)) {
          if (waitlisted.add(frontierNode)) {
            waitlist.add(frontierNode);
          }
        }
      }
    }

    return ImmutableSet.copyOf(iteratedFrontier);
  }

  @Override
  public String toString() {
    return frontiers.toString();
  }
}
