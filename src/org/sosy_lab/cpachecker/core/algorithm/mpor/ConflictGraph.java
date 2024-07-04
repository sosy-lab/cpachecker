// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A simple implementation of a directed graph with parallel edges. Nodes are Integers of thread
 * IDs. <br>
 * Edges are conflict relations between thread IDs as Integer tuples in the form (from, to) e.g.
 * (1,0).
 */
public class ConflictGraph {

  // TODO replace Integer thread IDs with MPORThread objects

  /** This HashMap represents the nodes (keys) and edges (keys as from, values (HashSets) as to). */
  private Map<Integer, Set<Integer>> graph;

  /** Creates an empty ConflictGraph. */
  public ConflictGraph() {
    graph = new HashMap<>();
  }

  /**
   * Creates a node with the given thread ID
   *
   * @param pThreadId node ID to be added
   * @throws IllegalArgumentException if a node with pThreadId already exists
   */
  public void addNode(int pThreadId) {
    checkArgument(!hasNode(pThreadId), "pThreadId is a node already");
    graph.put(pThreadId, new HashSet<>());
  }

  /**
   * Creates an edge as an int tuple in the form (from, to)
   *
   * @param pFrom the thread ID of the outgoing node
   * @param pTo the thread ID of the reached node
   * @throws IllegalArgumentException if a node with pFrom or pTo do not exist
   */
  public void addEdge(int pFrom, int pTo) {
    checkArgument(hasNode(pFrom), "pFrom ID does not exist as a node");
    checkArgument(hasNode(pTo), "pTo ID does not exist as a node");
    graph.get(pFrom).add(pTo);
  }

  /**
   * Returns the set of directly reachable active threads of the node pThreadId.
   *
   * @param pThreadId the ID of the thread
   * @return set of thread IDs that are directly reachable from pThreadId
   */
  public Set<Integer> getSuccessors(int pThreadId) {
    return graph.get(pThreadId);
  }

  /** Returns set of nodes with thread IDs, e.g. {0, 1, 3} */
  public Set<Integer> getNodes() {
    return graph.keySet();
  }

  /** Returns tuples with nodes and outgoing edges, e.g. {(0,{1,2}), (1,{0,3})} */
  public Map<Integer, Set<Integer>> getEdges() {
    return graph;
  }

  /**
   * @param pThreadId the thread ID / ID of the node
   * @return true if pThreadId exists as a key in the HashMap
   */
  private boolean hasNode(int pThreadId) {
    return graph.containsKey(pThreadId);
  }

  /**
   * Computes the Strongly Connected Components (SCCs) of the given graph based on Tarjan's SCC
   * Algorithm (1972) and the algorithms in {@link org.sosy_lab.cpachecker.util.GraphUtils}.
   *
   * <p>The algorithm returns the SCCs in reverse topological order (from maximal to minimal) and
   * has a linear complexity of O(N + E) where N is the number of nodes and E the number of edges.
   *
   * @return a set of sets of thread ids that form an SCC
   */
  @SuppressWarnings("unused")
  public static ImmutableSet<ImmutableSet<Integer>> computeSCCs(ConflictGraph pConflictGraph) {
    Preconditions.checkNotNull(pConflictGraph);

    // Variables for Trajan's algorithm
    int index = 0;
    Deque<Integer> stack = new ArrayDeque<>();
    Map<Integer, Integer> nodeIndex = new HashMap<>();
    Map<Integer, Integer> nodeLowLink = new HashMap<>();
    Set<Integer> onStack = new HashSet<>();
    List<Set<Integer>> sccList = new ArrayList<>();

    // Iterate over all nodes in the graph
    for (int node : pConflictGraph.getNodes()) {
      if (!nodeIndex.containsKey(node)) {
        strongConnect(node, pConflictGraph, index, stack, nodeIndex, nodeLowLink, onStack, sccList);
      }
    }

    // Convert the result list to ImmutableSet<ImmutableSet<Integer>>
    ImmutableSet.Builder<ImmutableSet<Integer>> rSccs = ImmutableSet.builder();
    for (Set<Integer> scc : sccList) {
      rSccs.add(ImmutableSet.copyOf(scc));
    }

    // TODO use the first element (sccs.iterator().next()) to get the maximal SCC
    return rSccs.build();
  }

  /**
   * Applies Tarjan's algorithm recursively to find and collect Strongly Connected Components
   * (SCCs).
   *
   * @param pNode the current node being visited
   * @param pConflictGraph the graph being analyzed
   * @param pIndex the current index in the DFS traversal
   * @param pStack the stack used to keep track of the nodes in the current path
   * @param pNodeIndex a map storing the index of each node
   * @param pNodeLowLink a map storing the lowest index reachable from each node
   * @param pOnStack a set to track nodes currently in the stack
   * @param pSccList a list to collect all the identified SCCs
   */
  public static void strongConnect(
      int pNode,
      ConflictGraph pConflictGraph,
      int pIndex,
      Deque<Integer> pStack,
      Map<Integer, Integer> pNodeIndex,
      Map<Integer, Integer> pNodeLowLink,
      Set<Integer> pOnStack,
      List<Set<Integer>> pSccList) {

    pNodeIndex.put(pNode, pIndex);
    pNodeLowLink.put(pNode, pIndex);
    pIndex++;
    pStack.push(pNode);
    pOnStack.add(pNode);

    // Consider successors of the node
    Set<Integer> successors = pConflictGraph.getSuccessors(pNode);
    if (successors != null) {
      for (Integer successor : pConflictGraph.getSuccessors(pNode)) {
        if (!pNodeIndex.containsKey(successor)) {
          // Successor has not yet been visited; recurse on it
          strongConnect(
              successor,
              pConflictGraph,
              pIndex,
              pStack,
              pNodeIndex,
              pNodeLowLink,
              pOnStack,
              pSccList);
          pNodeLowLink.put(pNode, Math.min(pNodeLowLink.get(pNode), pNodeLowLink.get(successor)));
        } else if (pOnStack.contains(successor)) {
          // Successor is in the stack and hence in the current SCC
          pNodeLowLink.put(pNode, Math.min(pNodeLowLink.get(pNode), pNodeIndex.get(successor)));
        }
      }
    }

    // If node is a root node, pop the stack and generate an SCC
    if (pNodeLowLink.get(pNode).equals(pNodeIndex.get(pNode))) {
      Set<Integer> scc = new HashSet<>();
      int currentNode;
      do {
        currentNode = pStack.pop();
        pOnStack.remove(currentNode);
        scc.add(currentNode);
      } while (pNode != currentNode);
      pSccList.add(scc);
    }
  }
}
