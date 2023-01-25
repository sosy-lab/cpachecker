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
import com.google.common.graph.Network;

/**
 * A {@link MutableNetwork} that forwards all calls to a delegate {@link MutableNetwork} or delegate
 * {@link Network}.
 *
 * <p>All {@link MutableNetwork} specific calls (i.e., add/remove method calls) are forwarded to the
 * delegate specified using {@link ForwardingMutableNetwork#delegateMutableNetwork()}. All other
 * calls are forwarded to the delegate specified using {@link
 * ForwardingMutableNetwork#delegateNetwork()}. These delegates can be the same {@link
 * MutableNetwork} instance (default implementation).
 */
public abstract class ForwardingMutableNetwork<N, E> extends ForwardingNetwork<N, E>
    implements MutableNetwork<N, E> {

  /**
   * Returns the delegate {@link MutableNetwork} to forward {@link MutableNetwork} specific calls
   * to.
   *
   * @return the delegate {@link MutableNetwork} to forward {@link MutableNetwork} specific calls to
   */
  protected abstract MutableNetwork<N, E> delegateMutableNetwork();

  @Override
  protected Network<N, E> delegateNetwork() {
    return delegateMutableNetwork();
  }

  @Override
  public boolean addNode(N pNode) {
    return delegateMutableNetwork().addNode(pNode);
  }

  @Override
  public boolean addEdge(N pNodeU, N pNodeV, E pEdge) {
    return delegateMutableNetwork().addEdge(pNodeU, pNodeV, pEdge);
  }

  @Override
  public boolean addEdge(EndpointPair<N> pEndpoints, E pEdge) {
    return delegateMutableNetwork().addEdge(pEndpoints, pEdge);
  }

  @Override
  public boolean removeNode(N pNode) {
    return delegateMutableNetwork().removeNode(pNode);
  }

  @Override
  public boolean removeEdge(E pEdge) {
    return delegateMutableNetwork().removeEdge(pEdge);
  }
}
