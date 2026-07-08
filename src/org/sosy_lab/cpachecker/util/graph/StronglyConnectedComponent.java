// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a strongly connected component (SCC) of a graph: a maximal set of nodes such that
 * every node is reachable from every other node in the set.
 *
 * @param <T> the type of the graph nodes
 */
public class StronglyConnectedComponent<T> {

  private final T root;

  // LinkedHashSet to preserve discovery order.
  private final Set<T> nodes = new LinkedHashSet<>();

  protected StronglyConnectedComponent(T pRoot) {
    root = checkNotNull(pRoot);
    nodes.add(root);
  }

  /** Adds a node to this SCC. */
  final void addNode(T pNode) {
    nodes.add(checkNotNull(pNode));
  }

  /** Returns the node this SCC was rooted at during discovery. */
  public final T getRootNode() {
    return root;
  }

  /** Returns an unmodifiable view of all nodes belonging to this SCC, including the root. */
  public final Set<T> getNodes() {
    return Collections.unmodifiableSet(nodes);
  }

  @Override
  public final boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (!(pOther instanceof StronglyConnectedComponent<?> other)) {
      return false;
    }
    return nodes.equals(other.nodes);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(nodes);
  }
}
