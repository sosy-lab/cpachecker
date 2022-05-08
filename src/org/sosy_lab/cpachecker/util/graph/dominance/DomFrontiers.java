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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A data structure containing dominance frontiers for all nodes in a graph.
 *
 * @param <T> the node-type of the original graph.
 */
public final class DomFrontiers<T> {

  private final DomInput<T> input;

  // dominance frontier for node with ID == 0, dominance frontier for node with ID == 1, etc.
  private final List<Set<Integer>> frontiers;

  private DomFrontiers(DomInput<T> pInput, List<Set<Integer>> pFrontiers) {

    input = pInput;
    frontiers = pFrontiers;
  }

  /**
   * Creates the {@link DomFrontiers}-object that contains the dominance frontier for every node in
   * the dominance tree.
   *
   * @param <T> the node-type of the original graph.
   * @param pDomTree the {@link DomTree} (dominance tree) of the original graph.
   * @throws NullPointerException if {@code pDomTree} is {@code null}.
   * @return the created {@link DomFrontiers}-object.
   */
  public static <T> DomFrontiers<T> forDomTree(DomTree<T> pDomTree) {

    checkNotNull(pDomTree);

    List<Set<Integer>> frontiers = computeFrontiers(pDomTree.getInput(), pDomTree.getDoms());

    return new DomFrontiers<>(pDomTree.getInput(), frontiers);
  }

  /**
   * For more information on the algorithm, see "A Simple, Fast Dominance Algorithm" (Cooper et
   * al.).
   */
  private static List<Set<Integer>> computeFrontiers(DomInput<?> pInput, int[] pDoms) {

    int[] predecessors = pInput.getPredecessors();

    List<Set<Integer>> frontiers = new ArrayList<>(pInput.getNodeCount());

    for (int id = 0; id < pInput.getNodeCount(); id++) {
      frontiers.add(new HashSet<>());
    }

    int index = 0; // index for input data (data format is specified in `DomInput`)
    for (int id = 0; id < pInput.getNodeCount(); id++) { // all nodes

      if (predecessors[index] == DomInput.DELIMITER) { // has no predecessors?
        index++; // skip delimiter
        continue;
      }

      if (predecessors[index + 1] == DomInput.DELIMITER) { // has only one predecessor?
        index += 2; // skip single predecessor + delimiter
        continue;
      }

      int runner;
      while ((runner = predecessors[index]) != DomInput.DELIMITER) { // all predecessors of node

        while (runner != DomTree.UNDEFINED && runner != pDoms[id]) {
          frontiers.get(runner).add(id);
          runner = pDoms[runner];
        }

        index++; // next predecessor
      }

      index++; // skip delimiter
    }

    return frontiers;
  }

  private Set<T> getFrontier(int pId) {

    Set<Integer> frontier = frontiers.get(pId);
    Set<T> nodeSet = new HashSet<>();

    for (int id : frontier) {
      nodeSet.add(input.getNodeForReversePostOrderId(id));
    }

    return Collections.unmodifiableSet(nodeSet);
  }

  /**
   * Returns the dominance frontier for the specified node.
   *
   * @param pNode the node to get the dominance frontier for.
   * @return the dominance frontier for the specified node.
   * @throws NullPointerException if {@code pNode} is {@code null}.
   * @throws IllegalArgumentException if {@code pNode} was not part of the original graph during
   *     graph traversal in {@link DomTree#forGraph}.
   */
  public Set<T> getFrontier(T pNode) {

    checkNotNull(pNode);

    @Nullable Integer id = input.getReversePostOrderId(pNode);

    checkArgument(id != null, "unknown node: %s", pNode);

    return getFrontier(id);
  }

  /**
   * Returns the iterated dominance frontier for the specified set of nodes.
   *
   * @param pNodes the set of nodes to get the iterated dominance frontier for.
   * @return an unmodifiable set consisting of all nodes in the iterated dominance frontier.
   * @throws NullPointerException if {@code pNodes} is {@code null}.
   * @throws IllegalArgumentException if {@code pNodes} contains a node that was not part of the
   *     original graph during graph traversal in {@link DomTree#forGraph}.
   */
  public Set<T> getIteratedFrontier(Set<T> pNodes) {

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

    return Collections.unmodifiableSet(iteratedFrontier);
  }

  @Override
  public String toString() {
    return frontiers.toString();
  }
}
