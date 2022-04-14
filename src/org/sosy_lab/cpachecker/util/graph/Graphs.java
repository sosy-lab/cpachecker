// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph;

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
   */
  public static <N, E> void insertPredecessor(
      MutableNetwork<N, E> pNetwork, N pNewPredecessor, N pNode, E pNewInEdge) {

    ImmutableList<E> nodeInEdges = ImmutableList.copyOf(pNetwork.inEdges(pNode));
    List<N> nodeUs = new ArrayList<>(nodeInEdges.size());

    for (E nodeInEdge : nodeInEdges) {
      nodeUs.add(pNetwork.incidentNodes(nodeInEdge).nodeU());
      pNetwork.removeEdge(nodeInEdge);
    }

    pNetwork.addNode(pNewPredecessor);

    for (int index = 0; index < nodeInEdges.size(); index++) {
      pNetwork.addEdge(nodeUs.get(index), pNewPredecessor, nodeInEdges.get(index));
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
   */
  public static <N, E> void insertSuccessor(
      MutableNetwork<N, E> pNetwork, N pNode, N pNewSuccessor, E pNewOutEdge) {

    ImmutableList<E> nodeOutEdges = ImmutableList.copyOf(pNetwork.outEdges(pNode));
    List<N> nodeVs = new ArrayList<>(nodeOutEdges.size());

    for (E nodeOutEdge : nodeOutEdges) {
      nodeVs.add(pNetwork.incidentNodes(nodeOutEdge).nodeV());
      pNetwork.removeEdge(nodeOutEdge);
    }

    pNetwork.addNode(pNewSuccessor);

    for (int index = 0; index < nodeOutEdges.size(); index++) {
      pNetwork.addEdge(pNewSuccessor, nodeVs.get(index), nodeOutEdges.get(index));
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
   */
  public static <N, E> void replaceNode(MutableNetwork<N, E> pNetwork, N pNode, N pNewNode) {

    pNetwork.addNode(pNewNode);

    for (E inEdge : ImmutableList.copyOf(pNetwork.inEdges(pNode))) {
      N nodeU = pNetwork.incidentNodes(inEdge).nodeU();
      pNetwork.removeEdge(inEdge);
      pNetwork.addEdge(nodeU, pNewNode, inEdge);
    }

    for (E outEdge : ImmutableList.copyOf(pNetwork.outEdges(pNode))) {
      N nodeV = pNetwork.incidentNodes(outEdge).nodeV();
      pNetwork.removeEdge(outEdge);
      pNetwork.addEdge(pNewNode, nodeV, outEdge);
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
   */
  public static <N, E> void replaceEdge(MutableNetwork<N, E> pNetwork, E pEdge, E pNewEdge) {

    EndpointPair<N> endpoints = pNetwork.incidentNodes(pEdge);
    pNetwork.removeEdge(pEdge);
    pNetwork.addEdge(endpoints.nodeU(), endpoints.nodeV(), pNewEdge);
  }
}
