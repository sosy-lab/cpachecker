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
public abstract class ForwardingNetwork<N, E> implements Network<N, E> {

  /**
   * Returns the delegate {@link Network} to forward all {@link Network} calls to.
   *
   * @return the delegate {@link Network} to forward all {@link Network} calls to
   */
  protected abstract Network<N, E> delegateNetwork();

  // network-level accessors

  @Override
  public Set<N> nodes() {
    return delegateNetwork().nodes();
  }

  @Override
  public Set<E> edges() {
    return delegateNetwork().edges();
  }

  @Override
  public Graph<N> asGraph() {
    return delegateNetwork().asGraph();
  }

  // network properties

  @Override
  public boolean isDirected() {
    return delegateNetwork().isDirected();
  }

  @Override
  public boolean allowsParallelEdges() {
    return delegateNetwork().allowsParallelEdges();
  }

  @Override
  public boolean allowsSelfLoops() {
    return delegateNetwork().allowsSelfLoops();
  }

  @Override
  public ElementOrder<N> nodeOrder() {
    return delegateNetwork().nodeOrder();
  }

  @Override
  public ElementOrder<E> edgeOrder() {
    return delegateNetwork().edgeOrder();
  }

  // element-level accessors

  @Override
  public Set<N> adjacentNodes(N pNode) {
    return delegateNetwork().adjacentNodes(pNode);
  }

  @Override
  public Set<N> predecessors(N pNode) {
    return delegateNetwork().predecessors(pNode);
  }

  @Override
  public Set<N> successors(N pNode) {
    return delegateNetwork().successors(pNode);
  }

  @Override
  public Set<E> incidentEdges(N pNode) {
    return delegateNetwork().incidentEdges(pNode);
  }

  @Override
  public Set<E> inEdges(N pNode) {
    return delegateNetwork().inEdges(pNode);
  }

  @Override
  public Set<E> outEdges(N pNode) {
    return delegateNetwork().outEdges(pNode);
  }

  @Override
  public int degree(N pNode) {
    return delegateNetwork().degree(pNode);
  }

  @Override
  public int inDegree(N pNode) {
    return delegateNetwork().inDegree(pNode);
  }

  @Override
  public int outDegree(N pNode) {
    return delegateNetwork().outDegree(pNode);
  }

  @Override
  public EndpointPair<N> incidentNodes(E pEdge) {
    return delegateNetwork().incidentNodes(pEdge);
  }

  @Override
  public Set<E> adjacentEdges(E pEdge) {
    return delegateNetwork().adjacentEdges(pEdge);
  }

  @Override
  public Set<E> edgesConnecting(N pNodeU, N pNodeV) {
    return delegateNetwork().edgesConnecting(pNodeU, pNodeV);
  }

  @Override
  public Set<E> edgesConnecting(EndpointPair<N> pEndpoints) {
    return delegateNetwork().edgesConnecting(pEndpoints);
  }

  @Override
  public Optional<E> edgeConnecting(N pNodeU, N pNodeV) {
    return delegateNetwork().edgeConnecting(pNodeU, pNodeV);
  }

  @Override
  public Optional<E> edgeConnecting(EndpointPair<N> pEndpoints) {
    return delegateNetwork().edgeConnecting(pEndpoints);
  }

  @Override
  public E edgeConnectingOrNull(N pNodeU, N pNodeV) {
    return delegateNetwork().edgeConnectingOrNull(pNodeU, pNodeV);
  }

  @Override
  public E edgeConnectingOrNull(EndpointPair<N> pEndpoints) {
    return delegateNetwork().edgeConnectingOrNull(pEndpoints);
  }

  @Override
  public boolean hasEdgeConnecting(N pNodeU, N pNodeV) {
    return delegateNetwork().hasEdgeConnecting(pNodeU, pNodeV);
  }

  @Override
  public boolean hasEdgeConnecting(EndpointPair<N> pEndpoints) {
    return delegateNetwork().hasEdgeConnecting(pEndpoints);
  }
}
