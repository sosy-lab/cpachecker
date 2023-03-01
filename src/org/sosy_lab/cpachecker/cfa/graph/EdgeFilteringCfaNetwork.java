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
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

/**
 * A {@link CfaNetwork} that only contains those edges of the wrapped {@link CfaNetwork}, for which
 * the specified predicate evaluates to {@code true}.
 */
final class EdgeFilteringCfaNetwork extends AbstractCfaNetwork {

  private final CfaNetwork delegate;
  private final Predicate<CFAEdge> retainPredicate;

  private EdgeFilteringCfaNetwork(CfaNetwork pDelegate, Predicate<CFAEdge> pRetainPredicate) {
    delegate = checkNotNull(pDelegate);
    retainPredicate = checkNotNull(pRetainPredicate);
  }

  static CfaNetwork of(CfaNetwork pDelegate, Predicate<CFAEdge> pRetainPredicate) {
    return CheckingCfaNetwork.wrapIfAssertionsEnabled(
        new EdgeFilteringCfaNetwork(pDelegate, pRetainPredicate));
  }

  @Override
  public Set<CFANode> nodes() {
    return delegate.nodes();
  }

  @Override
  public Set<CFAEdge> inEdges(CFANode pNode) {
    return Collections.unmodifiableSet(Sets.filter(delegate.inEdges(pNode), retainPredicate::test));
  }

  @Override
  public Set<CFAEdge> outEdges(CFANode pNode) {
    return Collections.unmodifiableSet(
        Sets.filter(delegate.outEdges(pNode), retainPredicate::test));
  }

  @Override
  public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
    return delegate.incidentNodes(pEdge);
  }

  @Override
  public FunctionEntryNode functionEntryNode(FunctionExitNode pFunctionExitNode) {
    // `delegate` may provide a more performant implementation that returns the correct node
    return delegate.functionEntryNode(pFunctionExitNode);
  }

  @Override
  public Optional<FunctionExitNode> functionExitNode(FunctionEntryNode pFunctionEntryNode) {
    // `delegate` may provide a more performant implementation that returns the correct node
    return delegate.functionExitNode(pFunctionEntryNode);
  }
}
