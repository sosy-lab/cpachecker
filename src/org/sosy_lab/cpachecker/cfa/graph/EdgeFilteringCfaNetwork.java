// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Sets;
import com.google.common.graph.EndpointPair;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

final class EdgeFilteringCfaNetwork extends AbstractCfaNetwork {

  private final CfaNetwork delegate;
  private final Predicate<CFAEdge> filter;

  EdgeFilteringCfaNetwork(CfaNetwork pDelegate, Predicate<CFAEdge> pFilter) {

    delegate = checkNotNull(pDelegate);
    filter = checkNotNull(pFilter);
  }

  @Override
  public Set<CFAEdge> inEdges(CFANode pNode) {
    return Collections.unmodifiableSet(Sets.filter(delegate.inEdges(pNode), filter::test));
  }

  @Override
  public Set<CFAEdge> outEdges(CFANode pNode) {
    return Collections.unmodifiableSet(Sets.filter(delegate.outEdges(pNode), filter::test));
  }

  @Override
  public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
    return delegate.incidentNodes(pEdge);
  }

  @Override
  public Set<CFANode> nodes() {
    return delegate.nodes();
  }
}
