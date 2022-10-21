// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph;

import com.google.common.graph.ElementOrder;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import com.google.common.graph.Network;
import java.util.Optional;
import java.util.Set;

/**
 * A {@link Network} that forwards all calls to a delegate {@link Network}.
 *
 * <p>The delegate {@link Network} is specified using {@link ForwardingNetwork#delegateNetwork()}.
 */
public interface ForwardingNetwork<N, E> extends Network<N, E> {

  /**
   * Returns the delegate {@link Network} to forwards calls to.
   *
   * @return the delegate {@link Network} to forwards calls to
   */
  Network<N, E> delegateNetwork();

  // network-level accessors

  @Override
  default Set<N> nodes() {
    return delegateNetwork().nodes();
  }

  @Override
  default Set<E> edges() {
    return delegateNetwork().edges();
  }

  @Override
  default Graph<N> asGraph() {
    return delegateNetwork().asGraph();
  }

  // network properties

  @Override
  default boolean isDirected() {
    return delegateNetwork().isDirected();
  }

  @Override
  default boolean allowsParallelEdges() {
    return delegateNetwork().allowsParallelEdges();
  }

  @Override
  default boolean allowsSelfLoops() {
    return delegateNetwork().allowsSelfLoops();
  }

  @Override
  default ElementOrder<N> nodeOrder() {
    return delegateNetwork().nodeOrder();
  }

  @Override
  default ElementOrder<E> edgeOrder() {
    return delegateNetwork().edgeOrder();
  }

  // element-level accessors

  @Override
  default Set<N> adjacentNodes(N pNode) {
    return delegateNetwork().adjacentNodes(pNode);
  }

  @Override
  default Set<N> predecessors(N pNode) {
    return delegateNetwork().predecessors(pNode);
  }

  @Override
  default Set<N> successors(N pNode) {
    return delegateNetwork().successors(pNode);
  }

  @Override
  default Set<E> incidentEdges(N pNode) {
    return delegateNetwork().incidentEdges(pNode);
  }

  @Override
  default Set<E> inEdges(N pNode) {
    return delegateNetwork().inEdges(pNode);
  }

  @Override
  default Set<E> outEdges(N pNode) {
    return delegateNetwork().outEdges(pNode);
  }

  @Override
  default int degree(N pNode) {
    return delegateNetwork().degree(pNode);
  }

  @Override
  default int inDegree(N pNode) {
    return delegateNetwork().inDegree(pNode);
  }

  @Override
  default int outDegree(N pNode) {
    return delegateNetwork().outDegree(pNode);
  }

  @Override
  default EndpointPair<N> incidentNodes(E pEdge) {
    return delegateNetwork().incidentNodes(pEdge);
  }

  @Override
  default Set<E> adjacentEdges(E pEdge) {
    return delegateNetwork().adjacentEdges(pEdge);
  }

  @Override
  default Set<E> edgesConnecting(N pNodeU, N pNodeV) {
    return delegateNetwork().edgesConnecting(pNodeU, pNodeV);
  }

  @Override
  default Set<E> edgesConnecting(EndpointPair<N> pEndpoints) {
    return delegateNetwork().edgesConnecting(pEndpoints);
  }

  @Override
  default Optional<E> edgeConnecting(N pNodeU, N pNodeV) {
    return delegateNetwork().edgeConnecting(pNodeU, pNodeV);
  }

  @Override
  default Optional<E> edgeConnecting(EndpointPair<N> pEndpoints) {
    return delegateNetwork().edgeConnecting(pEndpoints);
  }

  @Override
  default E edgeConnectingOrNull(N pNodeU, N pNodeV) {
    return delegateNetwork().edgeConnectingOrNull(pNodeU, pNodeV);
  }

  @Override
  default E edgeConnectingOrNull(EndpointPair<N> pEndpoints) {
    return delegateNetwork().edgeConnectingOrNull(pEndpoints);
  }

  @Override
  default boolean hasEdgeConnecting(N pNodeU, N pNodeV) {
    return delegateNetwork().hasEdgeConnecting(pNodeU, pNodeV);
  }

  @Override
  default boolean hasEdgeConnecting(EndpointPair<N> pEndpoints) {
    return delegateNetwork().hasEdgeConnecting(pEndpoints);
  }
}
