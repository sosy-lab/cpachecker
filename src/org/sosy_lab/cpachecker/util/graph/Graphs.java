// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableNetwork;
import java.util.ArrayList;
import java.util.List;

/** Static utility methods for Guava graphs. */
public final class Graphs {

  private Graphs() {}

  /**
   *
   *
   * <pre>{@code
   * Before:
   * --- a ---> [pNode] --- b ---->
   *
   * After:
   * --- a ---> [pNewPredecessor] --- pNewInEdge ---> [pNode] --- b ---->
   *
   * }</pre>
   *
   * @throws NullPointerException if any parameter is {@code null}
   * @throws IllegalArgumentException if the specified mutable network is undirected
   */
  public static <N, E> void insertPredecessor(
      MutableNetwork<N, E> pNetwork, N pNewPredecessor, N pNode, E pNewInEdge) {

    checkArgument(pNetwork.isDirected());

    ImmutableList<E> nodeInEdges = ImmutableList.copyOf(pNetwork.inEdges(pNode));
    List<N> nodePredecessors = new ArrayList<>(nodeInEdges.size());

    for (E nodeInEdge : nodeInEdges) {
      nodePredecessors.add(pNetwork.incidentNodes(nodeInEdge).source());
      pNetwork.removeEdge(nodeInEdge);
    }

    pNetwork.addNode(pNewPredecessor);

    for (int index = 0; index < nodeInEdges.size(); index++) {
      pNetwork.addEdge(nodePredecessors.get(index), pNewPredecessor, nodeInEdges.get(index));
    }

    pNetwork.addEdge(pNewPredecessor, pNode, pNewInEdge);
  }

  /**
   *
   *
   * <pre>{@code
   * Before:
   * --- a ---> [pNode] --- b ---->
   *
   * After:
   * --- a ---> [pNode] --- pNewOutEdge ---> [pNewSuccessor] --- b ---->
   *
   * }</pre>
   *
   * @throws NullPointerException if any parameter is {@code null}
   * @throws IllegalArgumentException if the specified mutable network is undirected
   */
  public static <N, E> void insertSuccessor(
      MutableNetwork<N, E> pNetwork, N pNode, N pNewSuccessor, E pNewOutEdge) {

    checkArgument(pNetwork.isDirected());

    ImmutableList<E> nodeOutEdges = ImmutableList.copyOf(pNetwork.outEdges(pNode));
    List<N> nodeSuccessors = new ArrayList<>(nodeOutEdges.size());

    for (E nodeOutEdge : nodeOutEdges) {
      nodeSuccessors.add(pNetwork.incidentNodes(nodeOutEdge).target());
      pNetwork.removeEdge(nodeOutEdge);
    }

    pNetwork.addNode(pNewSuccessor);

    for (int index = 0; index < nodeOutEdges.size(); index++) {
      pNetwork.addEdge(pNewSuccessor, nodeSuccessors.get(index), nodeOutEdges.get(index));
    }

    pNetwork.addEdge(pNode, pNewSuccessor, pNewOutEdge);
  }

  /**
   * Replaces a node with a different node in the specified mutable network.
   *
   * <pre>{@code
   * Before:
   * --- a ---> [pNode] --- b ---->
   *
   * After:
   * --- a ---> [pNewNode] --- b ---->
   *
   * }</pre>
   *
   * @throws NullPointerException if any parameter is {@code null}
   * @throws IllegalArgumentException if the specified mutable network is undirected
   */
  public static <N, E> void replaceNode(MutableNetwork<N, E> pNetwork, N pNode, N pNewNode) {

    checkArgument(pNetwork.isDirected());

    pNetwork.addNode(pNewNode);

    for (E inEdge : ImmutableList.copyOf(pNetwork.inEdges(pNode))) {
      N nodePredecessor = pNetwork.incidentNodes(inEdge).source();
      pNetwork.removeEdge(inEdge);
      pNetwork.addEdge(nodePredecessor, pNewNode, inEdge);
    }

    for (E outEdge : ImmutableList.copyOf(pNetwork.outEdges(pNode))) {
      N nodeSuccessor = pNetwork.incidentNodes(outEdge).target();
      pNetwork.removeEdge(outEdge);
      pNetwork.addEdge(pNewNode, nodeSuccessor, outEdge);
    }

    pNetwork.removeNode(pNode);
  }

  /**
   * Replaces an edge with a different edge in the specified mutable network.
   *
   * <pre>{@code
   * Before:
   * --- a ---> [X] --- pEdge ---> [Y] --- b ---->
   *
   * After:
   * --- a ---> [X] --- pNewEdge ---> [Y] --- b ---->
   *
   * }</pre>
   *
   * @throws NullPointerException if any parameter is {@code null}
   * @throws IllegalArgumentException if the specified mutable network is undirected
   */
  public static <N, E> void replaceEdge(MutableNetwork<N, E> pNetwork, E pEdge, E pNewEdge) {

    checkArgument(pNetwork.isDirected());

    EndpointPair<N> endpoints = pNetwork.incidentNodes(pEdge);
    pNetwork.removeEdge(pEdge);
    pNetwork.addEdge(endpoints.source(), endpoints.target(), pNewEdge);
  }
}
