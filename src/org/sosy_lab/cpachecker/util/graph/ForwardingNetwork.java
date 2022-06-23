// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph;


import com.google.common.graph.ElementOrder;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import com.google.common.graph.Network;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class ForwardingNetwork<N, E> implements Network<N, E> {

  protected abstract Network<N, E> delegate();

  @Override
  public final Set<E> adjacentEdges(E pEdge) {
    return delegate().adjacentEdges(pEdge);
  }

  @Override
  public final Set<N> adjacentNodes(N pNode) {
    return delegate().adjacentNodes(pNode);
  }

  @Override
  public final boolean allowsParallelEdges() {
    return delegate().allowsParallelEdges();
  }

  @Override
  public final boolean allowsSelfLoops() {
    return delegate().allowsSelfLoops();
  }

  @Override
  public final Graph<N> asGraph() {
    return delegate().asGraph();
  }

  @Override
  public final int degree(N pNode) {
    return delegate().degree(pNode);
  }

  @Override
  public final Optional<E> edgeConnecting(EndpointPair<N> pEndpoints) {
    return delegate().edgeConnecting(pEndpoints);
  }

  @Override
  public final Optional<E> edgeConnecting(N pNodeU, N pNodeV) {
    return delegate().edgeConnecting(pNodeU, pNodeV);
  }

  @Override
  public final @Nullable E edgeConnectingOrNull(EndpointPair<N> pEndpoints) {
    return delegate().edgeConnectingOrNull(pEndpoints);
  }

  @Override
  public final @Nullable E edgeConnectingOrNull(N pNodeU, N pNodeV) {
    return delegate().edgeConnectingOrNull(pNodeU, pNodeV);
  }

  @Override
  public final ElementOrder<E> edgeOrder() {
    return delegate().edgeOrder();
  }

  @Override
  public final Set<E> edges() {
    return delegate().edges();
  }

  @Override
  public final Set<E> edgesConnecting(EndpointPair<N> pEndpoints) {
    return delegate().edgesConnecting(pEndpoints);
  }

  @Override
  public final Set<E> edgesConnecting(N pNodeU, N pNodeV) {
    return delegate().edgesConnecting(pNodeU, pNodeV);
  }

  @Override
  public final boolean hasEdgeConnecting(EndpointPair<N> pEndpoints) {
    return delegate().hasEdgeConnecting(pEndpoints);
  }

  @Override
  public final boolean hasEdgeConnecting(N pNodeU, N pNodeV) {
    return delegate().hasEdgeConnecting(pNodeU, pNodeV);
  }

  @Override
  public final int inDegree(N pNode) {
    return delegate().inDegree(pNode);
  }

  @Override
  public final Set<E> inEdges(N pNode) {
    return delegate().inEdges(pNode);
  }

  @Override
  public final Set<E> incidentEdges(N pNode) {
    return delegate().incidentEdges(pNode);
  }

  @Override
  public final EndpointPair<N> incidentNodes(E pEdge) {
    return delegate().incidentNodes(pEdge);
  }

  @Override
  public final boolean isDirected() {
    return delegate().isDirected();
  }

  @Override
  public final ElementOrder<N> nodeOrder() {
    return delegate().nodeOrder();
  }

  @Override
  public final Set<N> nodes() {
    return delegate().nodes();
  }

  @Override
  public final int outDegree(N pNode) {
    return delegate().outDegree(pNode);
  }

  @Override
  public final Set<E> outEdges(N pNode) {
    return delegate().outEdges(pNode);
  }

  @Override
  public final Set<N> predecessors(N pNode) {
    return delegate().predecessors(pNode);
  }

  @Override
  public final Set<N> successors(N pNode) {
    return delegate().successors(pNode);
  }
}
