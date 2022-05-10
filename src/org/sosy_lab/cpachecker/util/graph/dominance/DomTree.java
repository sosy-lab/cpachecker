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
 * Represents a dominator tree (sometimes called dominance tree).
 *
 * <p>A node {@code D} dominates a node {@code N} if every path from the start node of a graph must
 * go through {@code D}. By definition, every node dominates itself, but only if node {@code D}
 * dominates a node {@code N} and {@code D != N}, it strictly dominates {@code N}.
 *
 * <p>Every node, except the start node of a graph, has an immediate dominator that is the unique
 * node that strictly dominates {@code N}, but doesn't strictly dominate any other node that
 * strictly dominates {@code N}. Due to the uniqueness of the immediate dominator, a tree can be
 * constructed, where the children of each node are the nodes it immediately dominates. This so
 * called dominator tree has as its single root the start node of a graph. All other nodes are
 * descendants of the start node. A node is strictly dominated by all its ancestors.
 *
 * <p>For performance reasons, every node has an ID that is unique in a dominator tree. For a
 * dominator tree {@code domTree}, IDs from the interval {@code [0, domTree.getNodeCount() - 1]} are
 * used.
 *
 * @param <T> the graph's node type
 */
public final class DomTree<T> implements Iterable<T> {

  /** Undefined ID. */
  public static final int UNDEFINED = -1;

  private final DomInput<T> input;

  // doms[N] == immediate dominator of N
  private final int[] doms;

  private DomTree(DomInput<T> pInput, int[] pDoms) {

    input = pInput;
    doms = pDoms;
  }

  /**
   * Creates a new dominator tree that doesn't contain any nodes.
   *
   * @param <T> the graph's node type
   * @return a new dominator tree that doesn't contain any nodes
   */
  public static <T> DomTree<T> empty() {
    return new DomTree<>(DomInput.empty(), new int[0]);
  }

  /**
   * Creates a new dominator tree for the specified graph.
   *
   * <p>Only nodes reachable from the start node will be part of the resulting dominator tree. The
   * graph must not be modified during dominator tree construction.
   *
   * @param <T> the graph's node type
   * @param pPredecessorFunction the graph's predecessor function (node -> iterable predecessors)
   * @param pSuccessorFunction the graph's successor function (node -> iterable successors)
   * @param pStartNode the start node for graph traversal that becomes the root node of the
   *     dominator tree
   * @return a new dominator tree for the specified graph
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
   * Creates a new dominator tree for the specified graph.
   *
   * <p>Only nodes reachable from the start node will be part of the resulting dominator tree. The
   * graph must not be modified during dominator tree construction.
   *
   * @param <T> the graph's node type
   * @param <G> the graph type
   * @param pGraph the graph that has a predecessor function (node -> iterable predecessors) and a
   *     successor function (node -> iterable successors)
   * @param pStartNode the start node for graph traversal that becomes the root node of the
   *     dominator tree
   * @return a new dominator tree for the specified graph
   * @throws NullPointerException if any parameter is {@code null}.
   */
  public static <T, G extends PredecessorsFunction<T> & SuccessorsFunction<T>> DomTree<T> forGraph(
      G pGraph, T pStartNode) {
    return forGraph(pGraph, pGraph, pStartNode);
  }

  /**
   * Returns an array containing the immediate dominator of each node ({@code doms[N] == immediate
   * dominator of N}).
   */
  private static int[] computeDoms(DomInput<?> pInput) {

    // Iterative Algorithm for computing all immediate dominators. For more information on the
    // algorithm, see "A Simple, Fast Dominance Algorithm" (Cooper et al.).

    DomInput.PredecessorDataIterator predecessorsDataIterator = pInput.iteratePredecessorData();

    int startNodeId = DomInput.START_NODE_ID;
    int[] doms = new int[pInput.getNodeCount()]; // doms[N] == immediate dominator of N
    boolean changed = true;

    Arrays.fill(doms, UNDEFINED); // no immediate dominator is known
    doms[startNodeId] = startNodeId; // needed to "seed" the computation, reverted afterwards

    while (changed) {

      changed = false;
      predecessorsDataIterator.reset();

      while (predecessorsDataIterator.hasNextNode()) {

        int nodeId = predecessorsDataIterator.nextNode();
        int idom = UNDEFINED; // immediate dominator of the current node

        // the start node has no immediate dominator, so we cannot compute it
        if (nodeId == startNodeId) {
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

    doms[startNodeId] = UNDEFINED; // the start node doesn't have an immediate dominator

    return doms;
  }

  /**
   * Computes the intersection of doms(pFst) and doms(pSnd) (doms(N) == all nodes that dominate N).
   * Cooper et al. describe it as "[walking] up the the dominator tree from two different nodes
   * until a common parent is reached".
   */
  private static int intersect(final int[] pDoms, final int pFst, final int pSnd) {

    int fst = pFst;
    int snd = pSnd;

    // The comparisons differ from the paper, because we use reverse post-order IDs instead
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

  /**
   * Returns the input from which this dominator tree was created.
   *
   * @return the input from which this dominator tree was created.
   */
  DomInput<T> getInput() {
    return input;
  }

  /**
   * Checks whether the specified ID is valid for this dominator tree.
   *
   * @throws IllegalArgumentException if the specified ID is not valid (i.e., if {@code ID < 0 || ID
   *     >= getNodeCount()})
   */
  private void checkId(int pId) {
    checkArgument(
        0 <= pId && pId < getNodeCount(), "pId must be 0 <= ID < getNodeCount(), but is: %s", pId);
  }

  /**
   * Returns the number of nodes in this dominator tree.
   *
   * @return the number of nodes in this dominator tree.
   */
  public int getNodeCount() {
    return input.getNodeCount();
  }

  /**
   * Returns the ID of the specified node.
   *
   * <p>A valid ID for a node is {@code 0 <= ID < getNodeCount()} and is unique for every node in
   * this dominator tree. All valid IDs are used (there is a node for every valid ID).
   *
   * @param pNode the node to get the ID for
   * @return the ID of the specified node
   * @throws NullPointerException if {@code pNode == null}
   * @throws IllegalArgumentException if {@code pNode} is unknown (i.e., was not visited during
   *     graph traversal for dominator tree construction)
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
   * <p>A valid ID for a node is {@code 0 <= ID < getNodeCount()} and is unique for every node in
   * this dominator tree. All valid IDs are used (there is a node for every valid ID).
   *
   * @param pId the ID to get the node for
   * @return the node with the specified ID
   * @throws IllegalArgumentException if the specified ID is not valid (i.e., if {@code ID < 0 || ID
   *     >= getNodeCount()})
   */
  public T getNode(int pId) {

    checkId(pId);

    return input.getNodeForReversePostOrderId(pId);
  }

  /**
   * Returns the ID of the root node, if a root exits.
   *
   * <p>Use {@link #getNodeCount()} to find out, whether this dominator tree contains any nodes.
   *
   * @return If this dominator tree contains a root (i.e., the dominator tree isn't empty), the ID
   *     of the root node is returned. Otherwise, if this dominator tree doesn't contain a root
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
   * @return If the node has a parent in this dominator tree, the parent's ID is returned.
   *     Otherwise, if the node is the root and doesn't have a parent, {@link DomTree#UNDEFINED} is
   *     returned.
   * @throws IllegalArgumentException if the specified ID is not valid (i.e., if {@code ID < 0 || ID
   *     >= getNodeCount()})
   */
  public int getParent(int pId) {

    checkId(pId);

    return doms[pId];
  }

  /**
   * Returns whether the specified node has a parent (immediate dominator) in this dominator tree.
   *
   * @param pId the ID of the node to check whether it has a parent
   * @return If the node has a parent in the dominator tree, {@code true} is returned. Otherwise, if
   *     the node is the root and doesn't have a parent, {@code false} is returned.
   * @throws IllegalArgumentException if the specified ID is not valid (i.e., if {@code ID < 0 || ID
   *     >= getNodeCount()})
   */
  public boolean hasParent(int pId) {

    checkId(pId);

    return doms[pId] != UNDEFINED;
  }

  /**
   * Returns whether the specified ancestor-node is the ancestor of a specified descendant-node.
   *
   * <p>Returns {@code true} if and only if the the node with ID {@code pAncestorId} is an ancestor
   * of the node with ID {@code pDescendantId} in this dominator tree. A node is strictly dominated
   * by all its ancestors in a dominator tree.
   *
   * @param pAncestorId the ancestor-node's ID
   * @param pDescendantId the descendant-node's ID
   * @return If the ancestor-node is indeed an ancestor of the descendant-node in this dominator
   *     tree, {@code true} is returned. Otherwise, if the ancestor-node is not an ancestor of the
   *     descendant-node, {@code false} is returned.
   * @throws IllegalArgumentException if any specified ID is not valid (i.e., if {@code ID < 0 || ID
   *     >= getNodeCount()})
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
   * pDescendantNode} in this dominator tree. A node is strictly dominated by all its ancestors in a
   * dominator tree.
   *
   * @param pAncestorNode the ancestor-node
   * @param pDescendantNode the descendant-node
   * @return If the ancestor-node is indeed an ancestor of the descendant-node in this dominator
   *     tree, {@code true} is returned. Otherwise, if the ancestor-node is not an ancestor of the
   *     descendant-node, {@code false} is returned.
   * @throws IllegalArgumentException if {@code pAncestorNode} or {@code pDescendantNode} is unknown
   *     (i.e., was not visited during graph traversal for dominator tree construction)
   */
  public boolean isAncestorOf(T pAncestorNode, T pDescendantNode) {
    return isAncestorOf(getId(pAncestorNode), getId(pDescendantNode));
  }

  /**
   * Returns an iterator that returns all nodes of a dominator tree in ascending ID-order (i.e.,
   * from {@code 0} to {@code domTree.getNodeCount() - 1}).
   *
   * @return an iterator that returns all nodes of a dominator tree in ascending ID-order
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
   * Returns an immutable {@link Graph} instance that represents this dominator tree.
   *
   * <p>This representation can be used as a convenient way for dominator tree traversal.
   *
   * @return an immutable {@link Graph} instance that represents this dominator tree
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
