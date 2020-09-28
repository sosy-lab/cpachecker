// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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

/**
 * A utility class for computing dominance trees and dominance frontiers.
 *
 * <p>A dominance tree ({@link DomTree}) is created by {@link #createDomTree}. The resulting tree
 * cannot directly be used for tree traversal (e.g. depth-first search), as every node only contains
 * a reference to its parent, but no references to its children.
 *
 * <p>If references to children are required, a traversable version of the dominance tree ({@link
 * DomTraversable}) can by created by {@link #createDomTraversable(DomTree)}.
 *
 * <p>Dominance frontiers ({@link DomFrontiers}) are created by {@link
 * #createDomFrontiers(DomTree)}. A {@link DomFrontiers}-object contains the dominance frontier for
 * every node in the graph.
 *
 * <p>Implementation detail: the dominance tree and dominance frontier computation algorithms are
 * from "A Simple, Fast Dominance Algorithm" (Cooper et al.).
 */
public final class Dominance {

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

    // every node goes through the following stages (in this order):
    // visited.get(node) == null: node was not encountered during graph traversal
    // visited.get(node) == UNDEFINED: node was encountered during graph traversal, but has no ID
    // visited.get(node) != null && != UNDEFINED: node has ID
    Map<T, Integer> visited = new HashMap<>();
    Deque<T> stack = new ArrayDeque<>();
    int counter = 0;

    stack.push(pStartNode);

    outer:
    while (!stack.isEmpty()) {

      T current = stack.pop();

      Integer id = visited.get(current);
      if (id != null && id != UNDEFINED) { // node already has ID?
        continue;
      }

      for (T pred : pPredFunc.apply(current)) { // visit predecessors first
        if (!visited.containsKey(pred)) { // predecessor never encountered before?
          stack.push(current);
          stack.push(pred);
          visited.put(pred, UNDEFINED); // mark node as encountered
          continue outer; // continue with predecessor (top stack element)
        }
      }

      for (T succ : pSuccFunc.apply(current)) { // push successors onto the stack
        if (!visited.containsKey(succ)) { // successor never encountered before?
          stack.push(succ);
        }
      }

      visited.put(current, counter); // set a node's ID
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
   * @param pNodes an empty array for storing all nodes in ascending-ID-order. The array is filled
   *     in this method. The length of the array must be equal to the size of the ID-map.
   * @param pPredFunc the predecessor-function.
   * @return the created {@link DomInput}-object.
   */
  private static <T> DomInput createDomInput(
      Map<T, Integer> pIds,
      T[] pNodes,
      Function<? super T, ? extends Iterable<? extends T>> pPredFunc) {

    // predsList is accessed by a node's reverse-postorder-ID (index == ID)
    // and contains a list of predecessors for every node (the ID of a predecessor is stored)
    List<List<Integer>> predsList = new ArrayList<>(Collections.nCopies(pIds.size(), null));
    int predCount = 0; // counts how many node-predecessor relationships are in the whole graph

    List<Integer> preds = new ArrayList<>(); // stores the predecessors for a specific node
    for (Map.Entry<T, Integer> entry : pIds.entrySet()) {

      int id = entry.getValue();

      for (T pred : pPredFunc.apply(entry.getKey())) {

        Integer predId = pIds.get(pred);

        assert predId != null
            : "Node has no reverse-postorder-ID: "
                + pred
                + "\n"
                + "  Is the successor-function or predecessor-function incorrect?\n"
                + "  Has the graph changed (concurrency issue)?";

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
   * <p>Successors and predecessors of all graph nodes must not change during the creation of the
   * dominance tree.
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

    @SuppressWarnings("unchecked") // it's impossible to create a new generic array T[]
    T[] nodes = (T[]) new Object[ids.size()];

    DomInput input = createDomInput(ids, nodes, pPredFunc);

    int[] doms = computeDoms(input);

    return new DomTree<>(input, ids, nodes, doms);
  }

  /**
   * Iterative Algorithm for computing the immediate dominators of all nodes. For more information
   * on the algorithm, see "A Simple, Fast Dominance Algorithm" (Cooper et al.).
   *
   * @return doms[x] == immediate dominator of x
   */
  private static int[] computeDoms(final DomInput pInput) {

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

  /**
   * Creates the {@link DomFrontiers}-object that contains the dominance frontier for every node in
   * the dominance tree.
   *
   * @param <T> the node-type of the original graph.
   * @param pDomTree the {@link DomTree} (dominance tree) of the original graph.
   * @throws NullPointerException if {@code pDomTree} is {@code null}.
   * @return the created {@link DomFrontiers}-object.
   */
  public static <T> DomFrontiers<T> createDomFrontiers(DomTree<T> pDomTree) {

    Objects.requireNonNull(pDomTree, "pDomTree must not be null");

    DomFrontiers.Frontier[] frontiers = computeFrontiers(pDomTree.getInput(), pDomTree.getDoms());

    return new DomFrontiers<>(pDomTree.getIds(), pDomTree.getNodes(), frontiers);
  }

  /**
   * For more information on the algorithm, see "A Simple, Fast Dominance Algorithm" (Cooper et
   * al.).
   */
  private static DomFrontiers.Frontier[] computeFrontiers(
      final DomInput pInput, final int[] pDoms) {

    DomFrontiers.Frontier[] frontiers = new DomFrontiers.Frontier[pInput.getNodeCount()];
    for (int id = 0; id < frontiers.length; id++) {
      frontiers[id] = new DomFrontiers.Frontier();
    }

    int index = 0; // index for input data (data format is specified in DomInput)
    for (int id = 0; id < pInput.getNodeCount(); id++) { // all nodes

      if (pInput.data[index] == DomInput.DELIMITER) { // has no predecessors?
        index++; // skip delimiter
        continue;
      }

      if (pInput.data[index + 1] == DomInput.DELIMITER) { // has exactly one predecessor?
        index += 2; // skip only predecessor + delimiter
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

  /**
   * Creates the corresponding {@link DomTraversable} for a specified {@link DomTree}.
   *
   * <p>The resulting DomTraversable can be used to traverse the specified dominance tree.
   *
   * @param <T> the node-type of the original graph.
   * @param pDomTree the {@link DomTree} (dominance tree) of the original graph.
   * @throws NullPointerException if {@code pDomTree} is {@code null}.
   * @return the created {@link DomTraversable}-object.
   */
  public static <T> DomTraversable<T> createDomTraversable(DomTree<T> pDomTree) {

    Objects.requireNonNull(pDomTree, "pDomTree must not be null");

    return DomTraversable.create(pDomTree);
  }

  /**
   * DomInput stores the predecessors for every node as well as the number of nodes in the whole
   * graph.
   */
  private static final class DomInput {

    private static final int DELIMITER = -2;

    // the data array contains the predecessors (their IDs) of every node
    // the predecessors of a node are separated by a DELIMITER from other predecessors
    // the first group of predecessors are for the node with ID == 0,
    // the second group for node with ID == 1, ..., the last group for node with ID == nodeCount - 1
    // the array must contain exactly one DELIMITER per node and its last element must be DELIMITER
    //
    // p_X_Y: predecessor Y of node X
    // format example: [p_0_a, p_0_b, DELIMITER, p_1_c, DELIMITER, DELIMITER, p_3_d, ...]
    // - node 0 has 2 predecessors
    // - node 1 has 1 predecessor
    // - node 2 has 0 predecessors
    // - node 3 has (at least) 1 predecessor
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

    /** Number of nodes in the whole graph. */
    private int getNodeCount() {
      return nodeCount;
    }
  }

  /**
   * A data structure representing a dominance tree.
   *
   * <p>A node's parent in a dominance tree is its immediate dominator. All dominators of a node can
   * be obtained by creating a set out of the node and all its ancestors.
   *
   * <p>Depending on the structure of the graph, not all nodes have to be (transitively) connected
   * to a single root. This happens when there are some nodes that aren't dominated by start node
   * (i.e. the root of the dominance tree).
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
     * Returns the number of nodes in this tree.
     *
     * @return the number of nodes in this tree.
     */
    public int getNodeCount() {
      return nodes.length;
    }

    /**
     * Returns the ID for the specified node.
     *
     * <p>A valid ID for a node is {@code >= 0}, {@code < getNodeCount()}, and unique for every node
     * in this tree. All valid IDs are used (there is a node for every valid ID).
     *
     * @param pNode the node to get the ID for.
     * @return the ID of the node, if the node is contained in this tree; otherwise, {@link
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
     * in this tree. All valid IDs are used (there is a node for every valid ID).
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
     * Returns the ID of the specified node's parent.
     *
     * <p>If the node has a parent (immediate dominator), the ID of the parent is returned;
     * otherwise, {@link Dominance#UNDEFINED} is returned.
     *
     * <p>Use {@link #hasParent(int)} to find out, if a node has a parent.
     *
     * @param pId the node's ID.
     * @return if the node has a parent, the parent's ID; otherwise, {@link Dominance#UNDEFINED} is
     *     returned.
     * @throws IllegalArgumentException if the specified ID is not valid. Valid IDs must be {@code
     *     >= 0} and {@code < getNodeCount()}.
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
     * @throws IllegalArgumentException if the specified ID is not valid. Valid IDs must be {@code
     *     >= 0} and {@code < getNodeCount()}.
     */
    public boolean hasParent(int pId) {

      checkId(pId);

      return doms[pId] != UNDEFINED;
    }

    /**
     * Returns whether a specified ancestor-node is the ancestor of a specified descendant-node.
     *
     * <p>Returns {@code true} if and only if the the node with ID {@code pAncestorId} is an
     * ancestor of the node with ID {@code pDescendantId} in this dominance tree. A node is strictly
     * dominated by all its ancestors in the dominance tree.
     *
     * @param pAncestorId the ancestor-node's ID.
     * @param pDescendantId the descendant-node's ID.
     * @return true, if {@code pAncestorId} is indeed an ancestor of {@code pDescendantId} in this
     *     dominance tree; otherwise, false.
     * @throws IllegalArgumentException if any of the specified IDs is not valid. Valid IDs must be
     *     {@code >= 0} and {@code < getNodeCount()}.
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
   * A data structure representing a dominance tree node that can be used for tree traversal.
   *
   * <p>It's possible to iterate over all direct children of this tree node (see {@link
   * DomTraversable#iterator()}). Recursive iteration over all descendants enables full tree
   * traversal, if all nodes are (transitively) connected to this node.
   *
   * @param <T> the node-type of the original graph.
   */
  public static final class DomTraversable<T> implements Iterable<DomTraversable<T>> {

    private final T node;

    private DomTraversable<T> parent;
    private List<DomTraversable<T>> children;

    private DomTraversable(T pNode) {
      node = pNode;
      children = new ArrayList<>();
    }

    private static <T> DomTraversable<T> create(DomTree<T> pDomTree) {

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

      for (int id : frontier.getSet()) {
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

    /**
     * Returns the iterated dominance frontier for the specified set of nodes.
     *
     * @param pNodes the set of nodes to get the iterated dominance frontier for.
     * @return an unmodifiable set consisting of all nodes in the iterated dominance frontier.
     * @throws IllegalArgumentException if {@code pNodes} contains a node that has no dominance
     *     frontier (see {@link #getFrontier(Object) getFrontier}).
     * @throws NullPointerException if {@code pNodes} is {@code null}.
     */
    public Set<T> getIteratedFrontier(Set<T> pNodes) {

      Objects.requireNonNull(pNodes, "pNodes must not be null");

      Set<T> frontier = new HashSet<>();
      Set<Integer> seen = new HashSet<>(); // a node is in seen if it is or has been in the waitlist
      Deque<Integer> waitlist = new ArrayDeque<>();

      for (T node : pNodes) {

        Integer id = ids.get(node);

        if (id == null) {
          throw new IllegalArgumentException(
              "pNodes contains node that has no dominance frontier: " + node);
        }

        waitlist.add(id);
        seen.add(id);
      }

      while (!waitlist.isEmpty()) {

        int removed = waitlist.remove();

        for (int id : frontiers[removed].getSet()) {
          if (frontier.add(nodes[id])) {
            if (seen.add(id)) { // if not previously seen -> add to waitlist
              waitlist.add(id);
            }
          }
        }
      }

      return Collections.unmodifiableSet(frontier);
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

      private Set<Integer> getSet() {
        return set;
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
