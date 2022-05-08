// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph.dominance;

import com.google.common.graph.ElementOrder;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A data structure representing a dominance tree.
 *
 * <p>A node's parent in a dominance tree is its immediate dominator. All dominators of a node can
 * be obtained by creating a set out of the node and all its ancestors.
 *
 * <p>Depending on the structure of the graph, not all nodes have to be (transitively) connected to
 * a single root. This happens when there are some nodes that aren't dominated by start node (i.e.
 * the root of the dominance tree).
 *
 * <p>This class implements {@link Iterable}, which enables iteration over all contained nodes in
 * ascending ID-order.
 *
 * @param <T> the node-type of the original graph.
 */
public final class DomTree<T> implements Iterable<T> {

  private final DomInput<T> input;
  private final int[] doms;

  DomTree(DomInput<T> pInput, int[] pDoms) {
    input = pInput;
    doms = pDoms;
  }

  DomInput<T> getInput() {
    return input;
  }

  int[] getDoms() {
    return doms;
  }

  /**
   * @throws IllegalArgumentException if the specified ID is not valid. Valid IDs must fulfill
   *     {@code 0 <= ID < getNodeCount()}.
   */
  private void checkId(int pId) {
    if (pId < 0 || pId >= getNodeCount()) {
      throw new IllegalArgumentException("pId must fulfill 0 <= ID < getNodeCount(): " + pId);
    }
  }

  /**
   * Returns the number of nodes in this tree.
   *
   * @return the number of nodes in this tree.
   */
  public int getNodeCount() {
    return input.getNodeCount();
  }

  /**
   * Returns the ID for the specified node.
   *
   * <p>A valid ID for a node fulfills {@code 0 <= ID < getNodeCount()} and is unique for every node
   * in this tree. All valid IDs are used (there is a node for every valid ID).
   *
   * @param pNode the node to get the ID for.
   * @return the ID for the specified node.
   * @throws NullPointerException if {@code pNode} is {@code null}.
   * @throws IllegalArgumentException if {@code pNode} was not part of the original graph during
   *     graph traversal in {@link Dominance#createDomTree}.
   */
  public int getId(T pNode) {

    Objects.requireNonNull(pNode, "pNode must not be null");

    Integer id = input.getReversePostOrderId(pNode);

    if (id == null) {
      throw new IllegalArgumentException("unknown node: " + pNode);
    }

    return id;
  }

  /**
   * Returns the node for a specified ID.
   *
   * <p>A valid ID for a node is {@code >= 0}, {@code < getNodeCount()}, and unique for every node
   * in this tree. All valid IDs are used (there is a node for every valid ID).
   *
   * @param pId the ID to get the node for.
   * @return the node with the specified ID.
   * @throws IllegalArgumentException if the specified ID is not valid. Valid IDs must fulfill
   *     {@code 0 <= ID < getNodeCount()}.
   */
  public T getNode(int pId) {

    checkId(pId);

    return input.getNodeForReversePostOrderId(pId);
  }

  public int getRoot() {
    return getNodeCount() - 1;
  }

  /**
   * Returns the ID of the specified node's parent.
   *
   * <p>If the node has a parent (immediate dominator), the ID of the parent is returned; otherwise,
   * {@link Dominance#UNDEFINED} is returned.
   *
   * <p>Use {@link #hasParent(int)} to find out, if a node has a parent.
   *
   * @param pId the node's ID.
   * @return if the node has a parent, the parent's ID; otherwise, {@link Dominance#UNDEFINED} is
   *     returned.
   * @throws IllegalArgumentException if the specified ID is not valid. Valid IDs must fulfill
   *     {@code 0 <= ID < getNodeCount()}.
   */
  public int getParent(int pId) {

    checkId(pId);

    return doms[pId];
  }

  /**
   * Returns whether a node has a parent (immediate dominator) in the dominance tree.
   *
   * @param pId ID of the node.
   * @return true, if node has a parent in the dominance tree; otherwise, false.
   * @throws IllegalArgumentException if the specified ID is not valid. Valid IDs must fulfill
   *     {@code 0 <= ID < getNodeCount()}.
   */
  public boolean hasParent(int pId) {

    checkId(pId);

    return doms[pId] != Dominance.UNDEFINED;
  }

  /**
   * Returns whether a specified ancestor-node is the ancestor of a specified descendant-node.
   *
   * <p>Returns {@code true} if and only if the the node with ID {@code pAncestorId} is an ancestor
   * of the node with ID {@code pDescendantId} in this dominance tree. A node is strictly dominated
   * by all its ancestors in the dominance tree.
   *
   * @param pAncestorId the ancestor-node's ID.
   * @param pDescendantId the descendant-node's ID.
   * @return true, if {@code pAncestorId} is indeed an ancestor of {@code pDescendantId} in this
   *     dominance tree; otherwise, false.
   * @throws IllegalArgumentException if any of the specified IDs is not valid. Valid IDs must
   *     fulfill {@code 0 <= ID < getNodeCount()}.
   */
  public boolean isAncestorOf(int pAncestorId, int pDescendantId) {

    checkId(pAncestorId);
    checkId(pDescendantId);

    int id = pDescendantId;

    while ((id = doms[id]) != Dominance.UNDEFINED) {
      if (id == pAncestorId) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns an iterator over the nodes in this dominance tree in ascending ID-order.
   *
   * @return an iterator over the nodes in this dominance tree in ascending ID-order.
   */
  @Override
  public Iterator<T> iterator() {
    return new Iterator<>() {

      int index = 0;

      @Override
      public boolean hasNext() {
        return index < getNodeCount();
      }

      @Override
      public T next() {
        if (hasNext()) {
          return input.getNodeForReversePostOrderId(index++);
        } else {
          throw new NoSuchElementException();
        }
      }
    };
  }

  public ImmutableGraph<T> asGraph() {

    MutableGraph<T> mutableGraph =
        GraphBuilder.directed()
            .allowsSelfLoops(false)
            .nodeOrder(ElementOrder.stable())
            .expectedNodeCount(getNodeCount())
            .build();

    for (int id = 0; id < getNodeCount(); id++) {
      if (hasParent(id)) {
        T node = input.getNodeForReversePostOrderId(id);
        T parent = input.getNodeForReversePostOrderId(getParent(id));
        mutableGraph.addNode(node);
        mutableGraph.addNode(parent);
        mutableGraph.putEdge(parent, node);
      }
    }

    return ImmutableGraph.copyOf(mutableGraph);
  }

  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder();
    sb.append("[");

    for (int id = 0; id < getNodeCount(); id++) {

      sb.append(getNode(id));

      if (hasParent(id)) {
        sb.append(" --> ");
        sb.append(getNode(getParent(id)));
      }

      if (id != getNodeCount() - 1) {
        sb.append(", ");
      }
    }

    sb.append("]");

    return sb.toString();
  }
}
