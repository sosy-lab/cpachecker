// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.graph;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableNetwork;

/**
 * A {@link MutableNetwork} that forwards all calls to a delegate {@link MutableNetwork}.
 *
 * <p>The delegate {@link MutableNetwork} is specified using {@link
 * ForwardingMutableNetwork#delegate()}.
 */
public abstract class ForwardingMutableNetwork<N, E> extends ForwardingNetwork<N, E>
    implements MutableNetwork<N, E> {

  /**
   * Returns the delegate {@link MutableNetwork} to forward all calls to.
   *
   * @return the delegate {@link MutableNetwork} to forward all calls to
   */
  @Override
  protected abstract MutableNetwork<N, E> delegate();

  @Override
  public boolean addNode(N pNode) {
    return delegate().addNode(pNode);
  }

  @Override
  public boolean addEdge(N pNodeU, N pNodeV, E pEdge) {
    return delegate().addEdge(pNodeU, pNodeV, pEdge);
  }

  @Override
  public boolean addEdge(EndpointPair<N> pEndpoints, E pEdge) {
    return delegate().addEdge(pEndpoints, pEdge);
  }

  @Override
  public boolean removeNode(N pNode) {
    return delegate().removeNode(pNode);
  }

  @Override
  public boolean removeEdge(E pEdge) {
    return delegate().removeEdge(pEdge);
  }
}
