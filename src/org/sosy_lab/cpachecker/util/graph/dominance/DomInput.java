// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph.dominance;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.graph.PredecessorsFunction;
import com.google.common.graph.SuccessorsFunction;
import com.google.common.graph.Traverser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents the input for dominator tree and dominance frontier computation.
 *
 * <p>An instance of {@link DomInput} is created from a graph and stores a bidirectional mapping
 * between nodes and their reverse post-order IDs. Additionally, it contains the predecessors of
 * every node.
 *
 * @param <T> the graph's node type
 */
final class DomInput<T> {

  /** The ID of the start node (it's always {@code 0}, because we use reverse post-order IDs). */
  static final int START_NODE_ID = 0;

  private final ImmutableMap<T, Integer> ids;
  private final ImmutableList<T> nodes;

  // predecessorData[N] == predecessors of N
  private final int[][] predecessorData;

  private DomInput(Map<T, Integer> pIds, T[] pNodes, int[][] pPredecessorData) {
    ids = ImmutableMap.copyOf(pIds);
    nodes = ImmutableList.copyOf(pNodes);
    predecessorData = pPredecessorData;
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

  /**
   * Creates a new {@link DomInput} instance that doesn't contain any data.
   *
   * @param <T> the graph's node type
   * @return a new {@link DomInput} instance that doesn't contain any data.
   */
  static <T> DomInput<T> empty() {

    @SuppressWarnings("unchecked") // it's impossible to create a new generic array T[]
    T[] emptyNodesArray = (T[]) new Object[0];

    return new DomInput<>(ImmutableMap.of(), emptyNodesArray, new int[0][]);
  }

  /**
   * Creates a new {@link DomInput} instance for the specified graph.
   *
   * <p>Only nodes reachable from the start node are considered for {@link DomInput} creation.
   *
   * @param <T> the graph's node type
   * @param pPredecessorFunction the graph's predecessor function (node -> iterable predecessors)
   * @param pSuccessorFunction the graph's successor function (node -> iterable successors)
   * @param pStartNode the start node for graph traversal
   * @return a new {@link DomInput} instance for the specified graph.
   */
  static <T> DomInput<T> forGraph(
      PredecessorsFunction<T> pPredecessorFunction,
      SuccessorsFunction<T> pSuccessorFunction,
      T pStartNode) {

    ImmutableMap<T, Integer> ids = createReversePostOrder(pSuccessorFunction, pStartNode);
    assert ids.get(pStartNode) == START_NODE_ID;

    @SuppressWarnings("unchecked") // it's impossible to create a new generic array T[]
    T[] nodes = (T[]) new Object[ids.size()];

    // predecessors of node with ID == 0, predecessors of node with ID == 1, etc.
    List<List<Integer>> allPredecessors = new ArrayList<>(Collections.nCopies(ids.size(), null));
    List<Integer> currentNodePredecessors = new ArrayList<>();
    for (Map.Entry<T, Integer> nodeToId : ids.entrySet()) {

      T currentNode = nodeToId.getKey();
      int currentNodeId = nodeToId.getValue();

      for (T predecessor : pPredecessorFunction.predecessors(currentNode)) {

        // if there is no path from the start node to `predecessor`, it doesn't have an ID
        @Nullable Integer predecessorId = ids.get(predecessor);
        if (predecessorId != null) {
          currentNodePredecessors.add(predecessorId);
        }
      }

      allPredecessors.set(currentNodeId, new ArrayList<>(currentNodePredecessors));
      nodes[currentNodeId] = currentNode;
      currentNodePredecessors.clear();
    }

    assert ids.entrySet().stream()
        .allMatch(entry -> entry.getKey().equals(nodes[entry.getValue()]));

    int[][] predecessorData = new int[allPredecessors.size()][];
    for (int nodeId = 0; nodeId < allPredecessors.size(); nodeId++) {
      List<Integer> nodePredecessors = allPredecessors.get(nodeId);
      predecessorData[nodeId] = nodePredecessors.stream().mapToInt(Integer::intValue).toArray();
    }

    return new DomInput<>(ids, nodes, predecessorData);
  }

  @Nullable Integer getReversePostOrderId(T pNode) {
    return ids.get(pNode);
  }

  T getNodeForReversePostOrderId(int pId) {
    return nodes.get(pId);
  }

  PredecessorDataIterator iteratePredecessorData() {
    return new PredecessorDataIterator(predecessorData);
  }

  int getNodeCount() {
    return nodes.size();
  }

  /**
   * Iterator for predecessor data ({@code predecessorData[N] == predecessors of N}).
   *
   * <p>Iteration works like this:
   *
   * <pre>
   * while (predecessorDataIterator.hasNextNode()) {
   *   int node = predecessorDataIterator.nextNode();
   *   while (predecessorDataIterator.hasNextPredecessor()) {
   *     int predecessorOfNode = predecessorDataIterator.nextPredecessor();
   *   }
   * }
   * </pre>
   */
  static final class PredecessorDataIterator {

    private final int[][] predecessorData;

    private int nodeId;
    private int[] nodePredecessors;
    private int predecessorIndex;

    private PredecessorDataIterator(int[][] pPredecessorData) {
      predecessorData = pPredecessorData;
    }

    boolean hasNextNode() {
      return nodeId < predecessorData.length;
    }

    int nextNode() {

      if (!hasNextNode()) {
        throw new NoSuchElementException();
      }

      nodePredecessors = predecessorData[nodeId++];
      predecessorIndex = 0;

      return nodeId - 1;
    }

    boolean hasNextPredecessor() {
      return nodePredecessors != null && predecessorIndex < nodePredecessors.length;
    }

    int nextPredecessor() {

      if (!hasNextPredecessor()) {
        throw new NoSuchElementException();
      }

      return nodePredecessors[predecessorIndex++];
    }

    void reset() {
      nodeId = 0;
      nodePredecessors = null;
      predecessorIndex = 0;
    }
  }
}
