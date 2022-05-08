// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph.dominance;

import com.google.common.graph.PredecessorsFunction;
import com.google.common.graph.SuccessorsFunction;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
      T pStartNode, SuccessorsFunction<T> pSuccFunc, PredecessorsFunction<T> pPredFunc) {

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

      for (T pred : pPredFunc.predecessors(current)) { // visit predecessors first
        if (!visited.containsKey(pred)) { // predecessor never encountered before?
          stack.push(current);
          stack.push(pred);
          visited.put(pred, UNDEFINED); // mark node as encountered
          continue outer; // continue with predecessor (top stack element)
        }
      }

      for (T succ : pSuccFunc.successors(current)) { // push successors onto the stack
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
      Map<T, Integer> pIds, T[] pNodes, PredecessorsFunction<T> pPredFunc) {

    // predsList is accessed by a node's reverse-postorder-ID (index == ID)
    // and contains a list of predecessors for every node (the ID of a predecessor is stored)
    List<List<Integer>> predsList = new ArrayList<>(Collections.nCopies(pIds.size(), null));
    int predCount = 0; // counts how many node-predecessor relationships are in the whole graph

    List<Integer> preds = new ArrayList<>(); // stores the predecessors for a specific node
    for (Map.Entry<T, Integer> entry : pIds.entrySet()) {

      int id = entry.getValue();

      for (T pred : pPredFunc.predecessors(entry.getKey())) {

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
   * @param pSuccFunc the successor-function (node to {@link Iterable}).
   * @param pPredFunc the predecessor-function (node to {@link Iterable}).
   * @throws NullPointerException if any parameter is {@code null}.
   * @return the created {@link DomTree}-object.
   */
  public static <T> DomTree<T> createDomTree(
      T pStartNode, SuccessorsFunction<T> pSuccFunc, PredecessorsFunction<T> pPredFunc) {

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

      if (pInput.getValue(index) == DomInput.DELIMITER) { // has no predecessors?
        index++; // skip delimiter
        continue;
      }

      if (pInput.getValue(index + 1) == DomInput.DELIMITER) { // has exactly one predecessor?
        index += 2; // skip only predecessor + delimiter
        continue;
      }

      int runner;
      while ((runner = pInput.getValue(index)) != DomInput.DELIMITER) { // all predecessors of node

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
}
