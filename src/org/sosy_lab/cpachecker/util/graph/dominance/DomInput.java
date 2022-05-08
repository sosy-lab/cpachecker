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
   * Traverses the graph and assigns every visited node its reverse post-order ID. All IDs are
   * stored in the resulting map (node to ID).
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

    // reverse order, e.g., [0,1,2] -> offset = 2 -> [|0-2|,|1-2|,|2-2|] -> [2,1,0]
    int offset = (postOrderIds.size() - 1);
    postOrderIds.replaceAll((node, value) -> Math.abs(value - offset));

    return ImmutableMap.copyOf(postOrderIds);
  }

  static <T> DomInput<T> forGraph(
      PredecessorsFunction<T> pPredecessorFunction,
      SuccessorsFunction<T> pSuccessorFunc,
      T pStartNode) {

    ImmutableMap<T, Integer> ids = createReversePostOrder(pSuccessorFunc, pStartNode);

    @SuppressWarnings("unchecked") // it's impossible to create a new generic array T[]
    T[] nodes = (T[]) new Object[ids.size()];

    // predecessors of node with ID == 0, predecessors of node with ID == 1, etc.
    List<List<Integer>> allPredecessors = new ArrayList<>(Collections.nCopies(ids.size(), null));
    int predecessorCounter = 0; // number of node-predecessor relationships in the graph

    List<Integer> currentNodePredecessors = new ArrayList<>();
    for (Map.Entry<T, Integer> nodeToId : ids.entrySet()) {

      T currentNode = nodeToId.getKey();
      int currentNodeId = nodeToId.getValue();

      for (T predecessor : pPredecessorFunction.predecessors(currentNode)) {

        // if there is no path from `pStartNode` to `predecessor`, it doesn't have an ID
        @Nullable Integer predecessorId = ids.get(predecessor);

        if (predecessorId != null) {
          currentNodePredecessors.add(predecessorId);
          predecessorCounter++;
        }
      }

      allPredecessors.set(currentNodeId, new ArrayList<>(currentNodePredecessors));
      nodes[currentNodeId] = currentNode;
      currentNodePredecessors.clear();
    }

    int[] predecessors = new int[predecessorCounter + allPredecessors.size()];

    int index = 0;
    for (List<Integer> nodePredecessors : allPredecessors) {

      for (int predecessorId : nodePredecessors) {
        predecessors[index++] = predecessorId;
      }

      predecessors[index++] = DELIMITER;
    }

    return new DomInput<>(ids, nodes, predecessors);
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
