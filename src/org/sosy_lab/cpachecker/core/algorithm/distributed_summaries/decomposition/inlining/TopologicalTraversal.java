// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.inlining;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.graph.SuccessorsFunction;
import com.google.common.graph.Traverser;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Interface is inspired by {@link Traverser} */
public class TopologicalTraversal<T> extends AbstractIterator<T> {

  private final T root;
  private final SuccessorsFunction<T> edges;

  /** All nodes without a unvisited incoming edge */
  private final List<T> waitlist;

  /** keeps track of every node that has incoming edges */
  private final Multiset<T> unvisitedNodes;

  private TopologicalTraversal(T pRoot, SuccessorsFunction<T> pEdges) {
    root = pRoot;
    edges = pEdges;

    waitlist = new ArrayList<>();
    waitlist.add(root);

    unvisitedNodes = initializeUnvisitedNodes();
  }

  private Multiset<T> initializeUnvisitedNodes() {

    Multiset<T> result = HashMultiset.create();

    List<T> toVisit = new ArrayList<>();
    toVisit.add(root);
    while (!toVisit.isEmpty()) {
      T current = toVisit.removeFirst();

      for (T succ : edges.successors(current)) {
        int countBefore = result.add(succ, 1);
        if (countBefore == 0) {
          // First time encountered -> add successors
          toVisit.add(succ);
        }
      }
    }

    return result;
  }

  @Override
  protected @Nullable T computeNext() {

    if (waitlist.isEmpty()) {
      return endOfData();
    }

    T current = waitlist.removeFirst();

    for (T succ : edges.successors(current)) {
      if (unvisitedNodes.remove(succ, 1) == 1) {
        // we removed the last incoming edge
        waitlist.add(succ);
      }
    }

    return current;
  }

  /**
   * Returns an iterator that traverses the nodes of a DAG in topological order.
   *
   * @param <T> The nodes. The hash function of these elements should not change during the
   *     traversal
   */
  public static final <T> Iterable<T> traverse(T root, SuccessorsFunction<T> edges) {
    return () -> new TopologicalTraversal<>(root, edges);
  }
}
