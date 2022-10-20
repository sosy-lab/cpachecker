// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.Network;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

final class ForwardingCfaNetwork extends AbstractCfaNetwork {

  private Network<CFANode, CFAEdge> delegate;

  private ForwardingCfaNetwork(Network<CFANode, CFAEdge> pDelegate) {
    delegate = pDelegate;
  }

  static CfaNetwork of(Network<CFANode, CFAEdge> pDelegate) {
    return new ForwardingCfaNetwork(checkNotNull(pDelegate));
  }

  @Override
  public Set<CFANode> nodes() {
    return delegate.nodes();
  }

  @Override
  public Set<CFAEdge> inEdges(CFANode pNode) {
    return delegate.inEdges(pNode);
  }

  @Override
  public Set<CFAEdge> outEdges(CFANode pNode) {
    return delegate.outEdges(pNode);
  }

  @Override
  public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
    return delegate.incidentNodes(pEdge);
  }
}
