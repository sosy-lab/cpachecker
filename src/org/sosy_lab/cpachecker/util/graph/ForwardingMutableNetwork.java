// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableNetwork;

public abstract class ForwardingMutableNetwork<N, E> extends ForwardingNetwork<N, E>
    implements MutableNetwork<N, E> {

  @Override
  protected abstract MutableNetwork<N, E> delegate();

  @Override
  public final boolean addEdge(EndpointPair<N> pEndpoints, E pEdge) {
    return delegate().addEdge(pEndpoints, pEdge);
  }

  @Override
  public final boolean addEdge(N pNodeU, N pNodeV, E pEdge) {
    return delegate().addEdge(pNodeU, pNodeV, pEdge);
  }

  @Override
  public final boolean addNode(N pNode) {
    return delegate().addNode(pNode);
  }

  @Override
  public final boolean removeEdge(E pEdge) {
    return delegate().removeEdge(pEdge);
  }

  @Override
  public final boolean removeNode(N pNode) {
    return delegate().removeNode(pNode);
  }
}
