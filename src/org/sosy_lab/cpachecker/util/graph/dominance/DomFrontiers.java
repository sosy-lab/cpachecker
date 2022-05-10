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
 * @param <T> the graph's node type
 */
public final class DomFrontiers<T> {

  private final DomInput<T> input;

  // dominance frontier for node with ID == 0, dominance frontier for node with ID == 1, etc.
  private final ImmutableList<ImmutableSet<Integer>> frontiers;

  private DomFrontiers(DomInput<T> pInput, List<? extends Set<Integer>> pFrontiers) {

    input = pInput;
    frontiers = Collections3.transformedImmutableListCopy(pFrontiers, ImmutableSet::copyOf);
  }

  /**
   * Creates a new {@link DomFrontiers} instance that contains a dominance frontier for every node
   * in the specified dominance tree.
   *
   * @param <T> the graph's node type
   * @param pDomTree the dominance tree of the graph
   * @return a new {@link DomFrontiers} instance that contains a dominance frontier for every node
   *     in the specified dominance tree
   * @throws NullPointerException if {@code pDomTree == null}
   */
  public static <T> DomFrontiers<T> forDomTree(DomTree<T> pDomTree) {

    checkNotNull(pDomTree);

    ImmutableList<ImmutableSet<Integer>> frontiers = computeFrontiers(pDomTree);

    return new DomFrontiers<>(pDomTree.getInput(), frontiers);
  }

  /**
   * For more information on the algorithm, see "A Simple, Fast Dominance Algorithm" (Cooper et
   * al.).
   */
  private static ImmutableList<ImmutableSet<Integer>> computeFrontiers(DomTree<?> pDomTree) {

    DomInput<?> domInput = pDomTree.getInput();
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

        while (runner != DomTree.UNDEFINED && runner != pDomTree.getParent(nodeId)) {
          frontiers.get(runner).add(nodeId);
          runner = pDomTree.getParent(runner);
        }

        if (predecessorsDataIterator.hasNextPredecessor()) {
          runner = predecessorsDataIterator.nextPredecessor();
        } else {
          runner = DomTree.UNDEFINED;
        }

      } while (runner != DomTree.UNDEFINED);
    }

    return Collections3.transformedImmutableListCopy(frontiers, ImmutableSet::copyOf);
  }

  private ImmutableSet<T> getFrontier(int pId) {

    return Collections3.transformedImmutableSetCopy(
        frontiers.get(pId), input::getNodeForReversePostOrderId);
  }

  /**
   * Returns the dominance frontier for the specified node.
   *
   * @param pNode the node to get the dominance frontier for
   * @return an immutable set consisting of the dominance frontier for the specified node
   * @throws NullPointerException if {@code pNode == null}
   * @throws IllegalArgumentException if {@code pNode} is unknown (i.e., was not visited during
   *     graph traversal for dominance tree construction)
   */
  public ImmutableSet<T> getFrontier(T pNode) {

    checkNotNull(pNode);

    @Nullable Integer id = input.getReversePostOrderId(pNode);

    checkArgument(id != null, "unknown node: %s", pNode);

    return getFrontier(id);
  }

  /**
   * Returns the iterated dominance frontier for the specified set of nodes.
   *
   * @param pNodes the set of nodes to get the iterated dominance frontier for
   * @return an unmodifiable set consisting of all nodes in the iterated dominance frontier
   * @throws NullPointerException if {@code pNodes == null} or if any element is {@code null}
   * @throws IllegalArgumentException if any node in {@code pNodes} is unknown (i.e., was not
   *     visited during graph traversal for dominance tree construction)
   */
  public ImmutableSet<T> getIteratedFrontier(Set<T> pNodes) {

    checkNotNull(pNodes);

    Set<T> iteratedFrontier = new HashSet<>();

    Set<Integer> waitlisted = new HashSet<>();
    Deque<Integer> waitlist = new ArrayDeque<>();

    for (T node : pNodes) {

      checkNotNull(node);

      @Nullable Integer id = input.getReversePostOrderId(node);

      checkArgument(id != null, "pNodes contains node that has no dominance frontier: %s", node);

      waitlist.add(id);
      waitlisted.add(id);
    }

    while (!waitlist.isEmpty()) {

      int removed = waitlist.remove();

      for (int id : frontiers.get(removed)) {
        if (iteratedFrontier.add(input.getNodeForReversePostOrderId(id))) {
          if (waitlisted.add(id)) {
            waitlist.add(id);
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
