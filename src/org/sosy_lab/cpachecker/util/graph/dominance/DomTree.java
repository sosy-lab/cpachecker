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
import com.google.common.graph.PredecessorsFunction;
import com.google.common.graph.SuccessorsFunction;
import java.util.Arrays;
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

  /** Undefined ID. */
  public static final int UNDEFINED = -1;

  private final DomInput<T> input;
  private final int[] doms;

  DomTree(DomInput<T> pInput, int[] pDoms) {
    input = pInput;
    doms = pDoms;
  }

  /**
   * Creates the {@link DomTree} (dominance tree) for the specified graph.
   *
   * <p>Successors and predecessors of all graph nodes must not change during the creation of the
   * dominance tree.
   *
   * @param <T> the node-type of the specified graph.
   * @param pStartNode the start node for graph traversal and root for resulting dominance tree.
   * @param pSuccFunc the successor-function (node to {@link Iterable}).
   * @param pPredFunc the predecessor-function (node to {@link Iterable}).
   * @throws NullPointerException if any parameter is {@code null}.
   * @return the created {@link DomTree}-object.
   */
  public static <T> DomTree<T> createDomTree(
      PredecessorsFunction<T> pPredFunc, SuccessorsFunction<T> pSuccFunc, T pStartNode) {

    Objects.requireNonNull(pStartNode, "pStartNode must not be null");
    Objects.requireNonNull(pSuccFunc, "pSuccFunc must not be null");
    Objects.requireNonNull(pPredFunc, "pPredFunc must not be null");

    DomInput<T> input = DomInput.forGraph(pPredFunc, pSuccFunc, pStartNode);

    int[] doms = computeDoms(input);

    return new DomTree<>(input, doms);
  }

  /**
   * Iterative Algorithm for computing the immediate dominators of all nodes. For more information
   * on the algorithm, see "A Simple, Fast Dominance Algorithm" (Cooper et al.).
   *
   * @return doms[x] == immediate dominator of x
   */
  private static int[] computeDoms(final DomInput<?> pInput) {

    final int startNode = pInput.getNodeCount() - 1; // the start node has the greatest ID
    int[] doms = new int[pInput.getNodeCount()]; // doms[x] == immediate dominator of x
    boolean changed = true;

    Arrays.fill(doms, UNDEFINED); // no immediate dominator is known
    doms[startNode] = startNode; // needed to 'seed' the computation, reverted afterwards

    while (changed) {
      changed = false;

      int index = 0; // index for input data (data format is specified in DomInput)
      for (int id = 0; id < startNode; id++) { // all nodes in reverse-post-order (except start)
        int idom = UNDEFINED; // immediate dominator for node

        int pred;
        while ((pred = pInput.getValue(index)) != DomInput.DELIMITER) { // all predecessors of node

          if (doms[pred] != UNDEFINED) { // does predecessor have an immediate dominator?
            if (idom != UNDEFINED) { // is idom already initialized?
              idom = intersect(doms, pred, idom); // update idom using predecessor
            } else {
              idom = pred; // initialize idom with predecessor
            }
          }

          index++; // next predecessor
        }

        if (doms[id] != idom) { // update immediate dominator for node?
          doms[id] = idom;
          changed = true;
        }

        index++; // skip delimiter
      }
    }

    doms[startNode] = UNDEFINED; // the start node cannot have an immediate dominator

    return doms;
  }

  /**
   * Computes the intersection of doms(pId1) and doms(pId2) (doms(x) == all nodes that dominate x).
   * Cooper et al. describe it as "[walking] up the the dominance tree from two different nodes
   * until a common parent is reached".
   */
  private static int intersect(final int[] pDoms, final int pId1, final int pId2) {

    int f1 = pId1;
    int f2 = pId2;

    while (f1 != f2) {
      while (f1 < f2) {
        f1 = pDoms[f1];
      }
      while (f2 < f1) {
        f2 = pDoms[f2];
      }
    }

    return f1;
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

    return doms[pId] != UNDEFINED;
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

    while ((id = doms[id]) != UNDEFINED) {
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
