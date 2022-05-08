// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph.dominance;

import com.google.common.collect.ImmutableMap;
import com.google.common.graph.PredecessorsFunction;
import com.google.common.graph.SuccessorsFunction;
import com.google.common.graph.Traverser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * DomInput stores the predecessors for every node as well as the number of nodes in the whole
 * graph.
 */
final class DomInput<T> {

  static final int DELIMITER = -2;

  private final ImmutableMap<T, Integer> ids;
  private final T[] nodes;

  private final int[] predecessors;

  private DomInput(ImmutableMap<T, Integer> pIds, T[] pNodes, int[] pPredecessors) {

    ids = pIds;
    nodes = pNodes;
    predecessors = pPredecessors;
  }

  /**
   * Traverses the graph and assigns every visited node its reverse-postorder-ID. All IDs are stored
   * in the resulting map (node to ID).
   */
  private static <T> ImmutableMap<T, Integer> createReversePostOrder(
      SuccessorsFunction<T> pSuccessorsFunction, T pStartNode) {

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

  private static int[] toArray(List<List<Integer>> pPredsPerNode, int pPredCount) {

    int[] data = new int[pPredCount + pPredsPerNode.size()];

    int index = 0;
    for (List<Integer> preds : pPredsPerNode) {

      for (int pred : preds) {
        data[index++] = pred;
      }

      data[index++] = DELIMITER;
    }

    return data;
  }

  static <T> DomInput<T> forGraph(
      PredecessorsFunction<T> pPredFunc, SuccessorsFunction<T> pSuccFunc, T pStartNode) {

    ImmutableMap<T, Integer> ids = createReversePostOrder(pSuccFunc, pStartNode);

    @SuppressWarnings("unchecked") // it's impossible to create a new generic array T[]
    T[] nodes = (T[]) new Object[ids.size()];

    // predsList is accessed by a node's reverse-postorder-ID (index == ID)
    // and contains a list of predecessors for every node (the ID of a predecessor is stored)
    List<List<Integer>> predsList = new ArrayList<>(Collections.nCopies(ids.size(), null));
    int predCount = 0; // counts how many node-predecessor relationships are in the whole graph

    List<Integer> preds = new ArrayList<>(); // stores the predecessors for a specific node
    for (Map.Entry<T, Integer> entry : ids.entrySet()) {

      int id = entry.getValue();

      for (T pred : pPredFunc.predecessors(entry.getKey())) {

        // if there is no path from the start node to pred, pred does not have an ID
        @Nullable Integer predId = ids.get(pred);

        if (predId != null) {
          preds.add(predId);
          predCount++;
        }
      }

      predsList.set(id, new ArrayList<>(preds));
      nodes[id] = entry.getKey();
      preds.clear();
    }

    return new DomInput<>(ids, nodes, toArray(predsList, predCount));
  }

  @Nullable Integer getReversePostOrderId(T pNode) {
    return ids.get(pNode);
  }

  T getNodeForReversePostOrderId(int pId) {
    return nodes[pId];
  }

  int[] getPredecessors() {
    return predecessors;
  }

  int getNodeCount() {
    return nodes.length;
  }
}
