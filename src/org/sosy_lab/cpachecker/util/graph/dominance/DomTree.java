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

import com.google.common.graph.ElementOrder;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.PredecessorsFunction;
import com.google.common.graph.SuccessorsFunction;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a dominance tree.
 *
 * <p>A node's parent in a dominance tree is its immediate dominator. All dominators of a node can
 * be obtained by creating a set out of the node and all its ancestors.
 *
 * <p>This class implements {@link Iterable}, which enables iteration over all contained nodes in
 * ascending ID-order.
 *
 * @param <T> the graph's node type
 */
public final class DomTree<T> implements Iterable<T> {

  /** Undefined ID. */
  public static final int UNDEFINED = -1;

  private final DomInput<T> input;

  // doms[x] == immediate dominator of x
  private final int[] doms;

  private DomTree(DomInput<T> pInput, int[] pDoms) {

    input = pInput;
    doms = pDoms;
  }

  /**
   * Creates a new dominance tree for the specified graph.
   *
   * <p>The graph must not be modified during dominance tree construction.
   *
   * <p>Only nodes reachable from the start node are will be part of the resulting dominance tree.
   *
   * @param <T> the graph's node type
   * @param pPredecessorFunction the graph's predecessor function (node -> iterable predecessors)
   * @param pSuccessorFunction the graph's successor function (node -> iterable successors)
   * @param pStartNode the start node graph traversal
   * @return a new dominance tree for the specified graph
   * @throws NullPointerException if any parameter is {@code null}.
   */
  public static <T> DomTree<T> forGraph(
      PredecessorsFunction<T> pPredecessorFunction,
      SuccessorsFunction<T> pSuccessorFunction,
      T pStartNode) {

    checkNotNull(pStartNode);
    checkNotNull(pSuccessorFunction);
    checkNotNull(pPredecessorFunction);

    DomInput<T> input = DomInput.forGraph(pPredecessorFunction, pSuccessorFunction, pStartNode);

    int[] doms = computeDoms(input);

    return new DomTree<>(input, doms);
  }

  /**
   * Iterative Algorithm for computing all immediate dominators. For more information on the
   * algorithm, see "A Simple, Fast Dominance Algorithm" (Cooper et al.).
   *
   * @return doms[x] == immediate dominator of x
   */
  private static int[] computeDoms(DomInput<?> pInput) {

    DomInput.PredecessorDataIterator predecessorsDataIterator = pInput.iteratePredecessorData();

    int startNode = 0; // the start node has the reverse post-order ID == 0
    int[] doms = new int[pInput.getNodeCount()]; // doms[x] == immediate dominator of x
    boolean changed = true;

    Arrays.fill(doms, UNDEFINED); // no immediate dominator is known
    doms[startNode] = startNode; // needed to 'seed' the computation, reverted afterwards

    while (changed) {

      changed = false;
      predecessorsDataIterator.reset();

      while (predecessorsDataIterator.hasNextNode()) {

        int nodeId = predecessorsDataIterator.nextNode();
        int idom = UNDEFINED; // immediate dominator of the current node

        // we cannot compute the immediate dominator of the start node
        if (nodeId == startNode) {
          continue;
        }

        while (predecessorsDataIterator.hasNextPredecessor()) {

          int predecessorId = predecessorsDataIterator.nextPredecessor();

          if (doms[predecessorId] != UNDEFINED) {
            idom = idom != UNDEFINED ? intersect(doms, predecessorId, idom) : predecessorId;
          }
        }

        if (doms[nodeId] != idom) {
          doms[nodeId] = idom;
          changed = true;
        }
      }
    }

    doms[startNode] = UNDEFINED; // the start node cannot have an immediate dominator

    return doms;
  }

  /**
   * Computes the intersection of doms(pFst) and doms(pSnd) (doms(x) == all nodes that dominate x).
   * Cooper et al. describe it as "[walking] up the the dominance tree from two different nodes
   * until a common parent is reached".
   */
  private static int intersect(final int[] pDoms, final int pFst, final int pSnd) {

    int fst = pFst;
    int snd = pSnd;

    // The comparisons look different from the paper, because we use reverse post-order IDs instead
    // of post-order IDs.
    while (fst != snd) {
      while (snd > fst) {
        snd = pDoms[snd];
      }
      while (fst > snd) {
        fst = pDoms[fst];
      }
    }

    return fst;
  }

  DomInput<T> getInput() {
    return input;
  }

  int[] getDoms() {
    return doms;
  }

  /**
   * Checks whether the specified ID is valid.
   *
   * @throws IllegalArgumentException if the specified ID is not valid (valid IDs must fulfill
   *     {@code 0 <= ID < getNodeCount()})
   */
  private void checkId(int pId) {
    checkArgument(
        0 <= pId && pId < getNodeCount(),
        "pId must fulfill 0 <= ID < getNodeCount(), but is: %s",
        pId);
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
   * @param pNode the node to get the ID for
   * @return the ID for the specified node
   * @throws NullPointerException if {@code pNode == null}
   * @throws IllegalArgumentException if {@code pNode} is unknown (i.e., was not visited during
   *     graph traversal for dominance tree construction)
   */
  public int getId(T pNode) {

    checkNotNull(pNode);

    @Nullable Integer id = input.getReversePostOrderId(pNode);

    checkArgument(id != null, "unknown node: %s", pNode);

    return id;
  }

  /**
   * Returns the node for the specified ID.
   *
   * <p>A valid ID for a node fulfills {@code 0 <= ID < getNodeCount()} and is unique for every node
   * in this tree. All valid IDs are used (there is a node for every valid ID).
   *
   * @param pId the ID to get the node for
   * @return the node with the specified ID
   * @throws IllegalArgumentException if the specified ID is not valid (valid IDs must fulfill
   *     {@code 0 <= ID < getNodeCount()})
   */
  public T getNode(int pId) {

    checkId(pId);

    return input.getNodeForReversePostOrderId(pId);
  }

  /**
   * Returns the ID of the root node, if a root exits.
   *
   * <p>Use {@link #getNodeCount()} to find out, whether this dominance tree contains any nodes.
   *
   * @return If this dominance tree contains a root (i.e., the dominance tree isn't empty), the ID
   *     of the root node is returned. Otherwise, if this dominance tree does not contain a root
   *     node, {@link DomTree#UNDEFINED} is returned.
   */
  public int getRootId() {
    return getNodeCount() > 0 ? 0 : DomTree.UNDEFINED;
  }

  /**
   * Returns the ID of the specified node's parent (immediate dominator), if it exists.
   *
   * <p>Use {@link #hasParent(int)} to find out, whether a node has a parent.
   *
   * @param pId the ID of the node to get the parent ID for
   * @return If the node has a parent, the parent's ID is returned. Otherwise, if the node is the
   *     root and doesn't have a parent, {@link DomTree#UNDEFINED} is returned.
   * @throws IllegalArgumentException if the specified ID is not valid (valid IDs must fulfill
   *     {@code 0 <= ID < getNodeCount()})
   */
  public int getParent(int pId) {

    checkId(pId);

    return doms[pId];
  }

  /**
   * Returns whether the specified node has a parent (immediate dominator) in the dominance tree.
   *
   * @param pId the ID of the node check whether it has a parent
   * @return If the node has a parent in the dominance tree, {@code true} is returned. Otherwise, if
   *     the node is the root and doesn't have a parent, {@code false} is returned.
   * @throws IllegalArgumentException if the specified ID is not valid (valid IDs must fulfill
   *     {@code 0 <= ID < getNodeCount()})
   */
  public boolean hasParent(int pId) {

    checkId(pId);

    return doms[pId] != UNDEFINED;
  }

  /**
   * Returns whether the specified ancestor-node is the ancestor of a specified descendant-node.
   *
   * <p>Returns {@code true} if and only if the the node with ID {@code pAncestorId} is an ancestor
   * of the node with ID {@code pDescendantId} in this dominance tree. A node is strictly dominated
   * by all its ancestors in the dominance tree.
   *
   * @param pAncestorId the ancestor-node's ID
   * @param pDescendantId the descendant-node's ID
   * @return If the ancestor-node is indeed an ancestor of the descendant-node, {@code true} is
   *     returned. Otherwise, if the ancestor-node is not an ancestor of the descendant-node, {@code
   *     false} is returned.
   * @throws IllegalArgumentException if the specified ID is not valid (valid IDs must fulfill
   *     {@code 0 <= ID < getNodeCount()})
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
   * Returns whether the specified ancestor-node is the ancestor of a specified descendant-node.
   *
   * <p>Returns {@code true} if and only if {@code pAncestorNode} is an ancestor of {@code
   * pDescendantNode} in this dominance tree. A node is strictly dominated by all its ancestors in
   * the dominance tree.
   *
   * @param pAncestorNode the ancestor-node
   * @param pDescendantNode the descendant-node
   * @return If the ancestor-node is indeed an ancestor of the descendant-node, {@code true} is
   *     returned. Otherwise, if the ancestor-node is not an ancestor of the descendant-node, {@code
   *     false} is returned.
   * @throws IllegalArgumentException if {@code pAncestorNode} or {@code pDescendantNode} is unknown
   *     (i.e., was not visited during graph traversal for dominance tree construction)
   */
  public boolean isAncestorOf(T pAncestorNode, T pDescendantNode) {
    return isAncestorOf(getId(pAncestorNode), getId(pDescendantNode));
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

  /**
   * Returns a {@link Graph} instance that represents this dominance tree.
   *
   * <p>The {@link Graph} representation can be used as a convenient way for dominance tree
   * traversal.
   *
   * @return a {@link Graph} instance that represents this dominance tree
   */
  public ImmutableGraph<T> asGraph() {

    MutableGraph<T> mutableGraph =
        GraphBuilder.directed()
            .allowsSelfLoops(false)
            .nodeOrder(ElementOrder.stable())
            .expectedNodeCount(getNodeCount())
            .build();

    for (int id = 0; id < getNodeCount(); id++) {
      T node = input.getNodeForReversePostOrderId(id);
      mutableGraph.addNode(node);
    }

    for (int id = 0; id < getNodeCount(); id++) {
      if (hasParent(id)) {
        T node = input.getNodeForReversePostOrderId(id);
        T parent = input.getNodeForReversePostOrderId(getParent(id));
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
