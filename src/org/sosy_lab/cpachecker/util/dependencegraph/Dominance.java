/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.dependencegraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/** A utility class for calculating dominance trees and dominance frontiers. */
final class Dominance {

  /** Undefined ID. */
  public static final int UNDEFINED = -1;

  private Dominance() {}

  /**
   * Traverses the graph and assigns every visited node its reverse-postorder-ID. All IDs are stored
   * in the resulting map (node to ID).
   */
  private static <T> Map<T, Integer> createReversePostOrder(
      T pStartNode,
      Function<? super T, ? extends Iterable<? extends T>> pSuccFunc,
      Function<? super T, ? extends Iterable<? extends T>> pPredFunc) {

    // visited.get(node) == null: node not seen and not visited
    // visited.get(node) == UNDEFINED: node seen but not visited
    // visited.get(node) != null && != UNDEFINED: node seen and visited
    Map<T, Integer> visited = new HashMap<>();
    Deque<T> stack = new ArrayDeque<>();
    int counter = 0;

    stack.push(pStartNode);

    outer:
    while (!stack.isEmpty()) {

      T current = stack.pop();

      Integer id = visited.get(current);
      if (id != null && id != UNDEFINED) { // node already visited?
        continue;
      }

      for (T pred : pPredFunc.apply(current)) { // visit predecessors first
        if (!visited.containsKey(pred)) { // not seen and not visited?
          stack.push(current);
          stack.push(pred);
          visited.put(pred, UNDEFINED); // set node as seen but not visited
          continue outer; // continue with predecessor (top stack element)
        }
      }

      for (T succ : pSuccFunc.apply(current)) { // visit successors
        if (!visited.containsKey(succ)) {
          stack.push(succ);
        }
      }

      visited.put(current, counter); // set node as visited
      counter++;
    }

    // reverse order, e.g. [0,1,2] -> offset = 2 -> [|0-2|,|1-2|,|2-2|] -> [2,1,0]
    final int offset = (counter - 1);
    visited.replaceAll((node, value) -> Math.abs(value - offset));

    return visited;
  }

  /**
   * Creates a new {@link DomInput}-object.
   *
   * @param <T> the node-type of the specified graph.
   * @param pIds the ID-map (node to ID).
   * @param pNodes an empty array for storing all nodes in ascending ID-order. The array is filled
   *     in this method. The length of the array must be equal to the size of the ID-map.
   * @param pPredFunc the predecessor-function.
   * @return the created {@link DomInput}-object.
   */
  private static <T> DomInput createDomInput(
      Map<T, Integer> pIds,
      T[] pNodes,
      Function<? super T, ? extends Iterable<? extends T>> pPredFunc) {

    List<List<Integer>> predsList = new ArrayList<>(Collections.nCopies(pIds.size(), null));

    List<Integer> preds = new ArrayList<>();
    int predCount = 0;
    for (Map.Entry<T, Integer> entry : pIds.entrySet()) {
      int id = entry.getValue();

      for (T next : pPredFunc.apply(entry.getKey())) {
        Integer predId = pIds.get(next);
        assert predId != null
            : "Unknown node (missing order-ID): "
                + next
                + "; possible reasons: the graph changed, the predecessor- and successor-functions are incorrect";

        preds.add(predId);
        predCount++;
      }

      predsList.set(id, new ArrayList<>(preds));
      pNodes[id] = entry.getKey();
      preds.clear();
    }

    return DomInput.create(predsList, predCount);
  }

  /**
   * Creates the {@link DomTree} (dominance tree) for the specified graph.
   *
   * @param <T> the node-type of the specified graph.
   * @param pStartNode the start node for graph traversal and root for resulting dominance tree.
   * @param pSuccFunc the successor-{@link Function} (node to {@link Iterable}).
   * @param pPredFunc the predecessor-{@link Function} (node to {@link Iterable}).
   * @throws NullPointerException if any parameter is {@code null}.
   * @return the created {@link DomTree}-object.
   */
  public static <T> DomTree<T> createDomTree(
      T pStartNode,
      Function<? super T, ? extends Iterable<? extends T>> pSuccFunc,
      Function<? super T, ? extends Iterable<? extends T>> pPredFunc) {

    Objects.requireNonNull(pStartNode, "pStartNode must not be null");
    Objects.requireNonNull(pSuccFunc, "pSuccFunc must not be null");
    Objects.requireNonNull(pPredFunc, "pPredFunc must not be null");

    Map<T, Integer> ids = createReversePostOrder(pStartNode, pSuccFunc, pPredFunc);

    @SuppressWarnings("unchecked")
    T[] nodes = (T[]) new Object[ids.size()];

    DomInput input = createDomInput(ids, nodes, pPredFunc);

    int[] doms = calculateDoms(input);

    return new DomTree<>(input, ids, nodes, doms);
  }

  /**
   * Iterative Algorithm for calculating the immediate dominators of all nodes. For more information
   * on the algorithm, see "A Simple, Fast Dominance Algorithm" (Cooper et al.).
   */
  private static int[] calculateDoms(final DomInput pInput) {

    final int start = pInput.getNodeCount() - 1; // start node is node with the highest number
    int[] doms = new int[pInput.getNodeCount()]; // doms[x] == immediate dominator of x
    boolean changed = true;

    Arrays.fill(doms, UNDEFINED); // no immediate dominator is known
    doms[start] = start; // start node is (only) dominated by itself

    while (changed) {
      changed = false;

      int index = 0; // index for input data
      for (int id = 0; id < start; id++) { // all nodes in reverse-post-order (except start)
        int idom = UNDEFINED; // immediate dominator for node

        int pred;
        while ((pred = pInput.data[index]) != DomInput.DELIMITER) { // all predecessors of node

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

    return doms;
  }

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

  /**
   * Creates the {@link DomFrontiers}-object containing dominance frontiers for all nodes in the
   * dominance tree.
   *
   * @param <T> the node-type of the original graph.
   * @param pDomTree the {@link DomTree} (dominance tree) of the original graph.
   * @throws NullPointerException if {@code pDomTree} is {@code null}.
   * @return the created {@link DomFrontiers}-object.
   */
  public static <T> DomFrontiers<T> createDomFrontiers(DomTree<T> pDomTree) {

    Objects.requireNonNull(pDomTree, "pDomTree must not be null");

    DomFrontiers.Frontier[] frontiers = calculateFrontiers(pDomTree.getInput(), pDomTree.getDoms());

    return new DomFrontiers<>(pDomTree.getIds(), pDomTree.getNodes(), frontiers);
  }

  /**
   * For more information on the algorithm, see "A Simple, Fast Dominance Algorithm" (Cooper et
   * al.).
   */
  private static DomFrontiers.Frontier[] calculateFrontiers(
      final DomInput pInput, final int[] pDoms) {

    DomFrontiers.Frontier[] frontiers = new DomFrontiers.Frontier[pInput.getNodeCount()];
    for (int id = 0; id < frontiers.length; id++) {
      frontiers[id] = new DomFrontiers.Frontier();
    }

    int index = 0; // index for input data
    for (int id = 0; id < pInput.getNodeCount(); id++) { // all nodes

      if (pInput.data[index] == DomInput.DELIMITER) { // has zero predecessors?
        index++; // skip delimiter
        continue;
      }

      if (pInput.data[index + 1] == DomInput.DELIMITER) { // has only one predecessor?
        index += 2; // skip single predecessor + delimiter
        continue;
      }

      int runner;
      while ((runner = pInput.data[index]) != DomInput.DELIMITER) { // all predecessors of node

        while (runner != UNDEFINED && runner != pDoms[id]) {
          frontiers[runner].add(id);
          runner = pDoms[runner];
        }

        index++; // next predecessor
      }

      index++; // skip delimiter
    }

    return frontiers;
  }

  /** DomInput stores predecessors for all nodes and the number of nodes. */
  private static final class DomInput {

    private static final int DELIMITER = -2;

    // p_X_Y: predecessor Y of node X
    // format example: [p_0_0, p_0_1, DELIMITER, p_1_0, DELIMITER, DELIMITER, p_3_0, ...]
    // - node 0 has 2 predecessors
    // - node 1 has 1 predecessor
    // - node 2 has 0 predecessors
    // - node 3 has (at least) 1 predecessor
    // the array must contain exactly one delimiter for every node
    private final int[] data;

    private final int nodeCount;

    private DomInput(int[] pData, int pNodeCount) {
      data = pData;
      nodeCount = pNodeCount;
    }

    private static DomInput create(List<List<Integer>> pData, int pPredCount) {

      int[] data = new int[pPredCount + pData.size()];

      int index = 0;
      for (List<Integer> preds : pData) {
        for (int pred : preds) {
          data[index++] = pred;
        }
        data[index++] = DELIMITER;
      }

      return new DomInput(data, pData.size());
    }

    /** Number of nodes in the graph. */
    private int getNodeCount() {
      return nodeCount;
    }
  }

  /**
   * A data structure representing a dominance tree.
   *
   * <p>A node's parent in a dominance tree is its immediate dominator. All dominators for a node
   * can be obtained by collecting the node and all its ancestors.
   *
   * <p>Depending on the structure of the graph, a single {@code DomTree}-object can have multiple
   * roots, so not all nodes have to be (transitively) connected to a single root.
   *
   * <p>This class implements {@link Iterable}, which enables iteration over all contained nodes in
   * ascending ID-order.
   *
   * @param <T> the node-type of the original graph.
   */
  public static final class DomTree<T> implements Iterable<T> {

    private final DomInput input;
    private final Map<T, Integer> ids;
    private final T[] nodes;
    private final int[] doms;

    private DomTree(DomInput pInput, Map<T, Integer> pIds, T[] pNodes, int[] pDoms) {
      input = pInput;
      ids = pIds;
      nodes = pNodes;
      doms = pDoms;
    }

    private DomInput getInput() {
      return input;
    }

    private Map<T, Integer> getIds() {
      return ids;
    }

    private T[] getNodes() {
      return nodes;
    }

    private int[] getDoms() {
      return doms;
    }

    /**
     * @throws IllegalArgumentException if the specified ID is not valid. Valid IDs must be {@code
     *     >= 0} and {@code < getNodeCount()}.
     */
    private void checkId(int pId) {
      if (pId < 0 || pId >= nodes.length) {
        throw new IllegalArgumentException("pId must be >= 0 and < getNodeCount(): " + pId);
      }
    }

    /**
     * Returns the number of nodes in the tree.
     *
     * @return the number of nodes in the tree.
     */
    public int getNodeCount() {
      return nodes.length;
    }

    /**
     * Returns the ID for the specified node.
     *
     * <p>A valid ID for a node is {@code >= 0}, {@code < getNodeCount()}, and unique for every node
     * in the tree. All valid IDs are used (there is a node for every valid ID).
     *
     * @param pNode the node to get the ID for.
     * @return the ID of the node, if the node is contained in the tree; otherwise, {@link
     *     Dominance#UNDEFINED} is returned.
     * @throws NullPointerException if {@code pNode} is {@code null}.
     */
    public int getId(T pNode) {

      Objects.requireNonNull(pNode, "pNode must not be null");

      Integer id = ids.get(pNode);

      return id != null ? id : UNDEFINED;
    }

    /**
     * Returns the node for a specified ID.
     *
     * <p>A valid ID for a node is {@code >= 0}, {@code < getNodeCount()}, and unique for every node
     * in the tree. All valid IDs are used (there is a node for every valid ID).
     *
     * @param pId the ID to get the node for.
     * @return the node with the specified ID.
     * @throws IllegalArgumentException if the specified ID is not valid. Valid IDs must be {@code
     *     >= 0} and {@code < getNodeCount()}.
     */
    public T getNode(int pId) {

      checkId(pId);

      return nodes[pId];
    }

    /**
     * Returns the parent's ID of the specified node, if the node has a parent (immediate
     * dominator). Otherwise, {@link Dominance#UNDEFINED} is returned.
     *
     * <p>Use {@link #hasParent(int)} to find out, if a node has a parent.
     *
     * @param pId the node's ID.
     * @return if the node has a valid parent, the parent's ID; otherwise, {@link
     *     Dominance#UNDEFINED} is returned.
     * @throws IllegalArgumentException if the specified ID is not valid. Valid IDs must be {@code
     *     >= 0} and {@code < getNodeCount()}.
     */
    public int getParent(int pId) {

      checkId(pId);

      int idom = doms[pId];

      return idom != pId ? idom : UNDEFINED;
    }

    /**
     * Returns whether a node has a parent (immediate dominator) in the dominance tree.
     *
     * @param pId ID of the node.
     * @return true, if node has a parent; otherwise false.
     * @throws IllegalArgumentException if the specified ID is not valid. Valid IDs must be {@code
     *     >= 0} and {@code < getNodeCount()}.
     */
    public boolean hasParent(int pId) {

      checkId(pId);

      return doms[pId] != UNDEFINED && doms[pId] != pId;
    }

    @Override
    public Iterator<T> iterator() {
      return new Iterator<>() {

        int index = 0;

        @Override
        public boolean hasNext() {
          return index < nodes.length;
        }

        @Override
        public T next() {
          if (hasNext()) {
            return nodes[index++];
          } else {
            throw new NoSuchElementException();
          }
        }
      };
    }

    @Override
    public String toString() {

      StringBuilder sb = new StringBuilder();
      sb.append("[");

      for (int id = 0; id < nodes.length; id++) {

        sb.append(getNode(id));

        if (hasParent(id)) {
          sb.append(" --> ");
          sb.append(getNode(getParent(id)));
        }

        if (id != nodes.length - 1) {
          sb.append(", ");
        }
      }

      sb.append("]");

      return sb.toString();
    }
  }

  /**
   * A data structure containing dominance frontiers for all nodes in a graph.
   *
   * @param <T> the node-type of the original graph.
   */
  public static final class DomFrontiers<T> {

    private final Map<T, Integer> ids;
    private final T[] nodes;
    private final Frontier[] frontiers;

    private DomFrontiers(Map<T, Integer> pIds, T[] pNodes, Frontier[] pFrontiers) {
      ids = pIds;
      nodes = pNodes;
      frontiers = pFrontiers;
    }

    private Set<T> getFrontier(int pId) {

      Frontier frontier = frontiers[pId];
      Set<T> nodeSet = new HashSet<>();

      for (int id : frontier.set) {
        nodeSet.add(nodes[id]);
      }

      return Collections.unmodifiableSet(nodeSet);
    }

    /**
     * Returns the dominance frontier for the specified node.
     *
     * @param pNode the node to get the dominance frontier for.
     * @return if there is a dominance frontier for the specified node, the dominance frontier is
     *     returned; otherwise, {@code null} is returned (this only happens when the node was not
     *     discovered during the traversal of the original graph).
     * @throws NullPointerException if {@code pNode} is {@code null}.
     */
    public Set<T> getFrontier(T pNode) {

      Objects.requireNonNull(pNode, "pNode must not be null");

      Integer id = ids.get(pNode);

      return id != null ? getFrontier(id) : null;
    }

    @Override
    public String toString() {
      return Arrays.toString(frontiers);
    }

    private static final class Frontier {

      private Set<Integer> set;

      private Frontier() {
        set = new HashSet<>();
      }

      private void add(int pId) {
        set.add(pId);
      }

      @Override
      public String toString() {
        return set.toString();
      }
    }
  }
}
