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
 * A {@link CfaNetwork} that only contains those nodes of the wrapped {@link CfaNetwork}, for which
 * the specified predicate evaluates to {@code true}.
 *
 * <p>Only if both endpoints of an edge are part of a {@link CfaNetwork}, the edge is also part of
 * the {@link CfaNetwork}.
 */
final class NodeFilteringCfaNetwork extends AbstractCfaNetwork {

  private final CfaNetwork delegate;
  private final Predicate<CFANode> keepNodePredicate;

  private NodeFilteringCfaNetwork(CfaNetwork pDelegate, Predicate<CFANode> pKeepNodePredicate) {
    delegate = pDelegate;
    keepNodePredicate = pKeepNodePredicate;
  }

  static CfaNetwork of(CfaNetwork pDelegate, Predicate<CFANode> pKeepNodePredicate) {
    return new NodeFilteringCfaNetwork(checkNotNull(pDelegate), checkNotNull(pKeepNodePredicate));
  }

  @Override
  public Set<CFANode> nodes() {
    return Collections.unmodifiableSet(Sets.filter(delegate.nodes(), keepNodePredicate::test));
  }

  @Override
  public Set<CFAEdge> inEdges(CFANode pNode) {
    return Collections.unmodifiableSet(
        Sets.filter(
            delegate.inEdges(pNode), edge -> keepEdgePredicate.test(delegate.predecessor(edge))));
  }

  @Override
  public Set<CFAEdge> outEdges(CFANode pNode) {
    return Collections.unmodifiableSet(
        Sets.filter(
            delegate.outEdges(pNode), edge -> keepEdgePredicate.test(delegate.successor(edge))));
  }

  @Override
  public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
    return delegate.incidentNodes(pEdge);
  }
}
