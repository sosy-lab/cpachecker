// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A simple implementation of a directed graph with parallel edges. */
public class DirectedGraph<T> {

  /** This HashMap represents the nodes (keys) and edges (keys as from, values (HashSets) as to). */
  private Map<T, Set<T>> graph;

  /** Creates an empty ConflictGraph. */
  public DirectedGraph() {
    graph = new HashMap<>();
  }

  /**
   * Adds pNode to the graph.
   *
   * @throws IllegalArgumentException if pNode already exists
   */
  public void addNode(T pNode) {
    checkArgument(!hasNode(pNode), "pNode is a node already");
    graph.put(pNode, new HashSet<>());
  }

  /**
   * Creates an edge as a tuple in the form (from, to)
   *
   * @throws IllegalArgumentException if either pFrom or pTo do not exist in the graph
   */
  public void addEdge(T pFrom, T pTo) {
    checkArgument(hasNode(pFrom), "pFrom does not exist as a node");
    checkArgument(hasNode(pTo), "pTo does not exist as a node");
    graph.get(pFrom).add(pTo);
  }

  /** Returns the set of directly reachable nodes from pNode. */
  public Set<T> getSuccessors(T pNode) {
    return graph.get(pNode);
  }

  /** Returns set of nodes in the graph. */
  public Set<T> getNodes() {
    return graph.keySet();
  }

  /** Returns tuples with nodes and outgoing edges, e.g. {(0,{1,2}), (1,{0,3})} */
  public Map<T, Set<T>> getGraph() {
    return graph;
  }

  /** Returns {@code true} if pNode exists */
  public boolean hasNode(T pNode) {
    return graph.containsKey(pNode);
  }

  /**
   * Computes the Strongly Connected Components (SCCs) of the given graph based on Tarjan's SCC
   * Algorithm (1972) and the algorithms in {@link org.sosy_lab.cpachecker.util.GraphUtils}.
   *
   * <p>The algorithm returns the SCCs in reverse topological order (from maximal to minimal) and
   * has a linear complexity of O(N + E) where N is the number of nodes and E the number of edges.
   *
   * @return a set of sets of Nodes that form an SCC
   */
  public ImmutableSet<ImmutableSet<T>> computeSCCs() {
    int index = 0;
    Deque<T> stack = new ArrayDeque<>();
    Map<T, Integer> nodeIndex = new HashMap<>();
    Map<T, Integer> nodeLowLink = new HashMap<>();
    Set<T> onStack = new HashSet<>();
    List<Set<T>> sccList = new ArrayList<>();

    // Iterate over all nodes in the graph
    for (T node : getNodes()) {
      if (!nodeIndex.containsKey(node)) {
        strongConnect(node, index, stack, nodeIndex, nodeLowLink, onStack, sccList);
      }
    }

    // Convert the result list to ImmutableSet<ImmutableSet<Integer>>
    ImmutableSet.Builder<ImmutableSet<T>> rSccs = ImmutableSet.builder();
    for (Set<T> scc : sccList) {
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
   * @param pIndex the current index in the DFS traversal
   * @param pStack the stack used to keep track of the nodes in the current path
   * @param pNodeIndex a map storing the index of each node
   * @param pNodeLowLink a map storing the lowest index reachable from each node
   * @param pOnStack a set to track nodes currently in the stack
   * @param pSccList a list to collect all the identified SCCs
   */
  public void strongConnect(
      T pNode,
      int pIndex,
      Deque<T> pStack,
      Map<T, Integer> pNodeIndex,
      Map<T, Integer> pNodeLowLink,
      Set<T> pOnStack,
      List<Set<T>> pSccList) {

    pNodeIndex.put(pNode, pIndex);
    pNodeLowLink.put(pNode, pIndex);
    pIndex++;
    pStack.push(pNode);
    pOnStack.add(pNode);

    // Consider successors of the node
    Set<T> successors = getSuccessors(pNode);
    if (successors != null) {
      for (T successor : getSuccessors(pNode)) {
        if (!pNodeIndex.containsKey(successor)) {
          // Successor has not yet been visited; recurse on it
          strongConnect(successor, pIndex, pStack, pNodeIndex, pNodeLowLink, pOnStack, pSccList);
          pNodeLowLink.put(pNode, Math.min(pNodeLowLink.get(pNode), pNodeLowLink.get(successor)));
        } else if (pOnStack.contains(successor)) {
          // Successor is in the stack and hence in the current SCC
          pNodeLowLink.put(pNode, Math.min(pNodeLowLink.get(pNode), pNodeIndex.get(successor)));
        }
      }
    }

    // If node is a root node, pop the stack and generate an SCC
    if (pNodeLowLink.get(pNode).equals(pNodeIndex.get(pNode))) {
      Set<T> scc = new HashSet<>();
      T currentNode;
      do {
        currentNode = pStack.pop();
        pOnStack.remove(currentNode);
        scc.add(currentNode);
      } while (pNode != currentNode);
      pSccList.add(scc);
    }
  }

  /**
   * Searches for cycles in the graph.
   *
   * @return {@code true} if a cycle is found
   */
  public boolean containsCycle() {
    for (T node : getNodes()) {
      Set<T> currentCycle = new HashSet<>();
      currentCycle.add(node);
      if (handleCycle(node, currentCycle)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Recursively tries to create a cycle in the graph.
   *
   * @return {@code true} if a cycle is found, i.e. if a path from a node to itself can be created
   */
  private boolean handleCycle(T pCurrentNode, Set<T> pCycle) {
    Set<T> successors = getSuccessors(pCurrentNode);
    for (T successor : successors) {
      if (pCycle.contains(successor)) {
        return true; // cycle detected
      }
      // copy the cycle so that if we have multiple successors, we start from their origin cycle
      Set<T> newCycle = new HashSet<>(pCycle);
      newCycle.add(successor);
      if (handleCycle(successor, newCycle)) {
        return true; // propagate cycle detection upwards
      }
    }
    return false;
  }
}
