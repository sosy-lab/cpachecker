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

/**
 * A {@link CfaNetwork} that forwards all calls to another {@link CfaNetwork}, but also filters all
 * returned edges using the specified predicate.
 *
 * <p>Only if the specified predicate returns {@code true}, the edge is part of the CFA represented
 * by a {@link EdgeFilteringCfaNetwork}.
 */
final class EdgeFilteringCfaNetwork extends AbstractCfaNetwork {

  private final CfaNetwork delegate;
  private final Predicate<CFAEdge> keepEdgePredicate;

  private EdgeFilteringCfaNetwork(CfaNetwork pDelegate, Predicate<CFAEdge> pKeepEdgePredicate) {
    delegate = pDelegate;
    keepEdgePredicate = pKeepEdgePredicate;
  }

  static CfaNetwork of(CfaNetwork pDelegate, Predicate<CFAEdge> pKeepEdgePredicate) {
    return new EdgeFilteringCfaNetwork(checkNotNull(pDelegate), checkNotNull(pKeepEdgePredicate));
  }

  @Override
  public Set<CFAEdge> inEdges(CFANode pNode) {
    return Collections.unmodifiableSet(
        Sets.filter(delegate.inEdges(pNode), keepEdgePredicate::test));
  }

  @Override
  public Set<CFAEdge> outEdges(CFANode pNode) {
    return Collections.unmodifiableSet(
        Sets.filter(delegate.outEdges(pNode), keepEdgePredicate::test));
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
