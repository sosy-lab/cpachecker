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

import com.google.common.collect.ImmutableSet;
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
import java.util.Optional;
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
 * @param <T> the graph's node type
 */
public final class DomTree<T> implements Iterable<T> {

  /** Undefined ID. */
  static final int UNDEFINED = -1;

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
    // algorithm, see "A Simple, Fast Dominance Algorithm" (Cooper et al.), Figure 3.

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
   * @param pNode the node to get the ID for
   * @return the ID of the specified node
   * @throws NullPointerException if {@code pNode == null}
   * @throws IllegalArgumentException if {@code pNode} is unknown (i.e., was not visited during
   *     graph traversal for dominator tree construction)
   */
  private int getId(T pNode) {

    checkNotNull(pNode);

    @Nullable Integer id = input.getReversePostOrderId(pNode);
    checkArgument(id != null, "unknown node: %s", pNode);

    return id;
  }

  /**
   * Returns the root node of this dominator tree, if it exits.
   *
   * @return If this dominator tree contains a root (i.e., the dominator tree isn't empty), an
   *     optional containing the root node is returned. Otherwise, if this dominator tree doesn't
   *     contain a root node, an empty optional is returned.
   */
  public Optional<T> getRoot() {
    return input.getNodeCount() > 0
        ? Optional.of(input.getNodeForReversePostOrderId(DomInput.START_NODE_ID))
        : Optional.empty();
  }

  /**
   * Returns the parent (immediate dominator) of the specified node in this dominator tree, if it
   * exists.
   *
   * <p>All nodes except the root node have a parent in this dominator tree.
   *
   * @param pNode the node to get the parent for
   * @return If the specified node has a parent in this dominator tree, an optional containing the
   *     parent node is returned. Otherwise, if the specified node doesn't have a parent in this
   *     dominator tree, an empty optional is returned.
   * @throws NullPointerException if {@code pNode == null}
   * @throws IllegalArgumentException if {@code pNode} is unknown (i.e., was not visited during
   *     graph traversal for dominator tree construction)
   */
  public Optional<T> getParent(T pNode) {

    int parentId = doms[getId(pNode)];
    return parentId != UNDEFINED
        ? Optional.of(input.getNodeForReversePostOrderId(parentId))
        : Optional.empty();
  }

  /**
   * Returns the ID of the specified node's parent (immediate dominator), if it exists.
   *
   * @param pId the ID of the node to get the parent ID for
   * @return If the node has a parent in this dominator tree, the parent's ID is returned.
   *     Otherwise, if the node is the root and doesn't have a parent, {@link DomTree#UNDEFINED} is
   *     returned.
   * @throws IllegalArgumentException if the specified ID is not valid (i.e., if {@code ID < 0 || ID
   *     >= getNodeCount()})
   */
  int getParentId(int pId) {

    checkArgument(
        0 <= pId && pId < getNodeCount(), "pId must be 0 <= ID < getNodeCount(), but is: %s", pId);

    return doms[pId];
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
   * @throws NullPointerException if any parameter is {@code null}
   * @throws IllegalArgumentException if {@code pAncestorNode} or {@code pDescendantNode} is unknown
   *     (i.e., was not visited during graph traversal for dominator tree construction)
   */
  public boolean isAncestorOf(T pAncestorNode, T pDescendantNode) {

    int ancestorId = getId(pAncestorNode);
    int id = getId(pDescendantNode);
    while ((id = doms[id]) != UNDEFINED) {
      if (id == ancestorId) {
        return true;
      }
    }

    return false;
  }

  /**
   * Returns an immutable set containing the ancestors of the specified node in this dominator tree.
   *
   * <p>A node is strictly dominated by all its ancestors in a dominator tree.
   *
   * @param pNode the node to get the ancestors for
   * @return an immutable set containing the ancestors of the specified node
   * @throws NullPointerException if {@code pNode == null}
   * @throws IllegalArgumentException if {@code pNode} is unknown (i.e., was not visited during
   *     graph traversal for dominator tree construction)
   */
  public ImmutableSet<T> getAncestors(T pNode) {

    ImmutableSet.Builder<T> builder = ImmutableSet.builder();

    int id = getId(pNode);
    while ((id = doms[id]) != UNDEFINED) {
      builder.add(input.getNodeForReversePostOrderId(id));
    }

    return builder.build();
  }

  /**
   * Returns an unmodifiable iterator that returns all nodes in this dominator tree.
   *
   * @return an unmodifiable iterator that returns all nodes in this dominator tree
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

    iterator().forEachRemaining(mutableGraph::addNode);

    for (T node : this) {
      getParent(node).ifPresent(parent -> mutableGraph.putEdge(parent, node));
    }

    return ImmutableGraph.copyOf(mutableGraph);
  }

  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder();
    sb.append("[");

    boolean separator = false;
    for (T node : this) {

      if (separator) {
        sb.append(", ");
      } else {
        separator = true;
      }

      sb.append(node);
      getParent(node)
          .ifPresent(
              parent -> {
                sb.append(" --> ");
                sb.append(parent);
              });
    }

    sb.append("]");

    return sb.toString();
  }
}
