// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph.dominance;

import com.google.common.collect.ImmutableMap;
import com.google.common.graph.PredecessorsFunction;
import com.google.common.graph.SuccessorsFunction;
import com.google.common.graph.Traverser;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * <p>If references to children are required, a traversable version of the dominance tree can by
 * created by {@link DomTree#asGraph()}.
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
  private static <T> ImmutableMap<T, Integer> createReversePostOrder(
      T pStartNode, SuccessorsFunction<T> pSuccessorsFunction) {

    Map<T, Integer> postOrderIds = new HashMap<>();

    Iterable<T> nodesInPostOrder =
        Traverser.forGraph(pSuccessorsFunction).depthFirstPostOrder(pStartNode);
    int counter = 0;
    for (T node : nodesInPostOrder) {
      postOrderIds.put(node, counter);
      counter++;
    }

    // reverse order, e.g. [0,1,2] -> offset = 2 -> [|0-2|,|1-2|,|2-2|] -> [2,1,0]
    int offset = (postOrderIds.size() - 1);
    postOrderIds.replaceAll((node, value) -> Math.abs(value - offset));

    return ImmutableMap.copyOf(postOrderIds);
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

        // if there is no path from the start node to pred, pred does not have an ID
        @Nullable Integer predId = pIds.get(pred);

        if (predId != null) {
          preds.add(predId);
          predCount++;
        }
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

    Map<T, Integer> ids = createReversePostOrder(pStartNode, pSuccFunc);

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
}
