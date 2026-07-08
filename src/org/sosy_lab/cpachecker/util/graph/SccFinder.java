// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class SccFinder {

  private SccFinder() {}

  /** Factory for creating {@link StronglyConnectedComponent} instances rooted at a given node. */
  public interface SCCFactory<T, S extends StronglyConnectedComponent<T>> {
    S create(T pRootNode);
  }

  /**
   * Finds all strongly connected components of a graph given by an arbitrary node type and a
   * successor function (Tarjan's algorithm).
   *
   * @param pNodes the nodes to start the search from
   * @param pSuccessorFunction returns the successors of a given node
   */
  public static <T> ImmutableSet<StronglyConnectedComponent<T>> findSCCs(
      Collection<T> pNodes, Function<T, Collection<T>> pSuccessorFunction) {
    return findSCCs(
        pNodes, pSuccessorFunction, ImmutableList.of(), r -> new StronglyConnectedComponent<>(r));
  }

  /**
   * Finds all strongly connected components of a graph given by an arbitrary node type and a
   * successor function (Tarjan's algorithm).
   *
   * @param pNodes the nodes to start the search from
   * @param pSuccessorFunction returns the successors of a given node
   * @param pExcludeNodes nodes to ignore entirely (neither visited nor used as successors)
   */
  public static <T> ImmutableSet<StronglyConnectedComponent<T>> findSCCs(
      Collection<T> pNodes,
      Function<T, Collection<T>> pSuccessorFunction,
      Collection<T> pExcludeNodes) {
    return findSCCs(
        pNodes, pSuccessorFunction, pExcludeNodes, r -> new StronglyConnectedComponent<>(r));
  }

  /**
   * Finds all strongly connected components of a graph given by an arbitrary node type and a
   * successor function (Tarjan's algorithm).
   *
   * @param pNodes the nodes to start the search from
   * @param pSuccessorFunction returns the successors of a given node
   * @param pExcludeNodes nodes to ignore entirely (neither visited nor used as successors)
   * @param pSCCFactory used to instantiate SCC objects for the discovered components
   */
  public static <T, S extends StronglyConnectedComponent<T>> ImmutableSet<S> findSCCs(
      Collection<T> pNodes,
      Function<T, Collection<T>> pSuccessorFunction,
      Collection<T> pExcludeNodes,
      SCCFactory<T, S> pSCCFactory) {
    checkNotNull(pNodes);
    checkNotNull(pSuccessorFunction);
    checkNotNull(pExcludeNodes);
    checkNotNull(pSCCFactory);

    List<S> sccs = new ArrayList<>();

    // An array so that we can pass index by reference and not by value
    // Otherwise, the modification in the recursive calls do not show up outside
    int[] index = {0};

    Deque<T> dfsStack = new ArrayDeque<>();
    Set<T> onStack = new HashSet<>();
    Map<T, Integer> nodeIndex = new HashMap<>();

    // Map to store the topmost reachable ancestor with the minimum possible index value
    Map<T, Integer> nodeLowLink = new HashMap<>();

    for (T node : pNodes) {
      if (pExcludeNodes.contains(node)) {
        continue;
      }
      if (!nodeIndex.containsKey(node)) {
        strongConnect(
            node,
            index,
            nodeIndex,
            nodeLowLink,
            dfsStack,
            onStack,
            sccs,
            pExcludeNodes,
            pSuccessorFunction,
            pSCCFactory);
      }
    }
    return ImmutableSet.copyOf(Lists.reverse(sccs));
  }

  /** Recursively find {@link StronglyConnectedComponent}s using DFS traversal. */
  private static <T, S extends StronglyConnectedComponent<T>> void strongConnect(
      T pNode,
      int[] pIndex,
      Map<T, Integer> pNodeIndex,
      Map<T, Integer> pNodeLowLink,
      Deque<T> pDfsStack,
      Set<T> pOnStack,
      List<S> pSCCs,
      Collection<T> pExcludeNodes,
      Function<T, Collection<T>> pSuccessorFunction,
      SCCFactory<T, S> pSCCFactory) {

    pNodeIndex.put(pNode, pIndex[0]);
    pNodeLowLink.put(pNode, pIndex[0]);
    pIndex[0]++;
    pDfsStack.push(pNode);
    pOnStack.add(pNode);

    for (T successor : pSuccessorFunction.apply(pNode)) {
      if (pExcludeNodes.contains(successor)) {
        continue;
      }
      if (!pNodeIndex.containsKey(successor)) {
        // Successor has not yet been visited; recurse on it
        strongConnect(
            successor,
            pIndex,
            pNodeIndex,
            pNodeLowLink,
            pDfsStack,
            pOnStack,
            pSCCs,
            pExcludeNodes,
            pSuccessorFunction,
            pSCCFactory);
        pNodeLowLink.put(pNode, Math.min(pNodeLowLink.get(pNode), pNodeLowLink.get(successor)));
      } else if (pOnStack.contains(successor)) {
        // Successor is on the stack and hence in the current SCC.
        // Otherwise, (pNode, successor) is a cross-edge (not a back edge) in the DFS tree
        // and must be ignored.
        pNodeLowLink.put(pNode, Math.min(pNodeLowLink.get(pNode), pNodeIndex.get(successor)));
      }
    }

    // If pNode is a root node, pop the stack and generate an SCC
    if (pNodeIndex.get(pNode).intValue() == pNodeLowLink.get(pNode).intValue()) {
      T s;
      S scc = pSCCFactory.create(pNode);
      do {
        s = pDfsStack.pop();
        pOnStack.remove(s);
        scc.addNode(s);
      } while (!Objects.equals(pNode, s));
      pSCCs.add(scc);
    }
  }
}
