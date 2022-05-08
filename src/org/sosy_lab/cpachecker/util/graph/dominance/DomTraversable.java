// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph.dominance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A data structure representing a dominance tree node that can be used for tree traversal.
 *
 * <p>It's possible to iterate over all direct children of this tree node (see {@link
 * DomTraversable#iterator()}). Recursive iteration over all descendants enables full tree
 * traversal, if all nodes are (transitively) connected to this node.
 *
 * @param <T> the node-type of the original graph.
 */
public final class DomTraversable<T> implements Iterable<DomTraversable<T>> {

  private final T node;

  private DomTraversable<T> parent;
  private List<DomTraversable<T>> children;

  private DomTraversable(T pNode) {
    node = pNode;
    children = new ArrayList<>();
  }

  static <T> DomTraversable<T> create(DomTree<T> pDomTree) {

    List<DomTraversable<T>> traversables = new ArrayList<>(pDomTree.getNodeCount());

    for (T node : pDomTree) {
      traversables.add(new DomTraversable<>(node));
    }

    for (int id = 0; id < pDomTree.getNodeCount(); id++) {
      if (pDomTree.hasParent(id)) {
        DomTraversable<T> traversable = traversables.get(id);
        DomTraversable<T> parent = traversables.get(pDomTree.getParent(id));
        traversable.parent = parent;
        parent.children.add(traversable);
      }
    }

    for (DomTraversable<T> traversable : traversables) {
      traversable.children = Collections.unmodifiableList(traversable.children);
    }

    return traversables.get(traversables.size() - 1);
  }

  /**
   * Returns the original graph node for this DomTraversable.
   *
   * @return the original graph node.
   */
  public T getNode() {
    return node;
  }

  /**
   * Returns the parent DomTraversable, if the DomTraversable has a parent (i.e. is not the root).
   *
   * @return if the DomTraversable has valid parent, the parent DomTraversable is returned;
   *     otherwise {@code null} is returned.
   */
  public DomTraversable<T> getParent() {
    return parent;
  }

  /**
   * Returns an iterator over the direct children of this DomTraversable.
   *
   * <p>The children are returned in no specific order.
   *
   * @return an iterator over the direct children of this DomTraversable.
   */
  @Override
  public Iterator<DomTraversable<T>> iterator() {
    return children.iterator();
  }

  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder();
    sb.append("[");

    boolean insertSeparator = false;

    if (parent != null) {
      sb.append(node);
      sb.append(" --> ");
      sb.append(parent.node);
      insertSeparator = true;
    }

    for (DomTraversable<T> child : children) {

      if (insertSeparator) {
        sb.append(", ");
      } else {
        insertSeparator = true;
      }

      sb.append(child.node);
      sb.append(" --> ");
      sb.append(node);
    }

    sb.append("]");

    return sb.toString();
  }
}
