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
 * <p>The delegate {@link Network} is specified using {@link ForwardingNetwork#delegate()}.
 */
public abstract class ForwardingNetwork<N, E> implements Network<N, E> {

  /**
   * Returns the delegate {@link Network} to forward all calls to.
   *
   * @return the delegate {@link Network} to forward all calls to
   */
  protected abstract Network<N, E> delegate();

  // network-level accessors

  @Override
  public Set<N> nodes() {
    return delegate().nodes();
  }

  @Override
  public Set<E> edges() {
    return delegate().edges();
  }

  @Override
  public Graph<N> asGraph() {
    return delegate().asGraph();
  }

  // network properties

  @Override
  public boolean isDirected() {
    return delegate().isDirected();
  }

  @Override
  public boolean allowsParallelEdges() {
    return delegate().allowsParallelEdges();
  }

  @Override
  public boolean allowsSelfLoops() {
    return delegate().allowsSelfLoops();
  }

  @Override
  public ElementOrder<N> nodeOrder() {
    return delegate().nodeOrder();
  }

  @Override
  public ElementOrder<E> edgeOrder() {
    return delegate().edgeOrder();
  }

  // element-level accessors

  @Override
  public Set<N> adjacentNodes(N pNode) {
    return delegate().adjacentNodes(pNode);
  }

  @Override
  public Set<N> predecessors(N pNode) {
    return delegate().predecessors(pNode);
  }

  @Override
  public Set<N> successors(N pNode) {
    return delegate().successors(pNode);
  }

  @Override
  public Set<E> incidentEdges(N pNode) {
    return delegate().incidentEdges(pNode);
  }

  @Override
  public Set<E> inEdges(N pNode) {
    return delegate().inEdges(pNode);
  }

  @Override
  public Set<E> outEdges(N pNode) {
    return delegate().outEdges(pNode);
  }

  @Override
  public int degree(N pNode) {
    return delegate().degree(pNode);
  }

  @Override
  public int inDegree(N pNode) {
    return delegate().inDegree(pNode);
  }

  @Override
  public int outDegree(N pNode) {
    return delegate().outDegree(pNode);
  }

  @Override
  public EndpointPair<N> incidentNodes(E pEdge) {
    return delegate().incidentNodes(pEdge);
  }

  @Override
  public Set<E> adjacentEdges(E pEdge) {
    return delegate().adjacentEdges(pEdge);
  }

  @Override
  public Set<E> edgesConnecting(N pNodeU, N pNodeV) {
    return delegate().edgesConnecting(pNodeU, pNodeV);
  }

  @Override
  public Set<E> edgesConnecting(EndpointPair<N> pEndpoints) {
    return delegate().edgesConnecting(pEndpoints);
  }

  @Override
  public Optional<E> edgeConnecting(N pNodeU, N pNodeV) {
    return delegate().edgeConnecting(pNodeU, pNodeV);
  }

  @Override
  public Optional<E> edgeConnecting(EndpointPair<N> pEndpoints) {
    return delegate().edgeConnecting(pEndpoints);
  }

  @Override
  public E edgeConnectingOrNull(N pNodeU, N pNodeV) {
    return delegate().edgeConnectingOrNull(pNodeU, pNodeV);
  }

  @Override
  public E edgeConnectingOrNull(EndpointPair<N> pEndpoints) {
    return delegate().edgeConnectingOrNull(pEndpoints);
  }

  @Override
  public boolean hasEdgeConnecting(N pNodeU, N pNodeV) {
    return delegate().hasEdgeConnecting(pNodeU, pNodeV);
  }

  @Override
  public boolean hasEdgeConnecting(EndpointPair<N> pEndpoints) {
    return delegate().hasEdgeConnecting(pEndpoints);
  }
}
