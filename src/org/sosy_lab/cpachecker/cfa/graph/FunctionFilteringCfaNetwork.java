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
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

/**
 * A {@link CfaNetwork} that only contains those functions of the wrapped {@link CfaNetwork}, for
 * which the specified predicate evaluates to {@code true}.
 *
 * <p>Only if both endpoints of an edge are part of a {@link CfaNetwork}, the edge is also part of
 * the {@link CfaNetwork}.
 */
final class FunctionFilteringCfaNetwork extends AbstractCfaNetwork {

  private final CfaNetwork unfilteredDelegate;
  private final CfaNetwork filteredFunctionsDelegate;

  private FunctionFilteringCfaNetwork(
      CfaNetwork pDelegate, Predicate<AFunctionDeclaration> pRetainPredicate) {
    unfilteredDelegate = checkNotNull(pDelegate);
    checkNotNull(pRetainPredicate);
    filteredFunctionsDelegate =
        unfilteredDelegate.withFilteredNodes(node -> pRetainPredicate.test(node.getFunction()));
  }

  static CfaNetwork of(CfaNetwork pDelegate, Predicate<AFunctionDeclaration> pRetainPredicate) {
    return CheckingCfaNetwork.wrapIfAssertionsEnabled(
        new FunctionFilteringCfaNetwork(pDelegate, pRetainPredicate));
  }

  @Override
  public Set<CFANode> nodes() {
    return filteredFunctionsDelegate.nodes();
  }

  @Override
  public Set<CFAEdge> inEdges(CFANode pNode) {
    return filteredFunctionsDelegate.inEdges(pNode);
  }

  @Override
  public Set<CFAEdge> outEdges(CFANode pNode) {
    return filteredFunctionsDelegate.outEdges(pNode);
  }

  @Override
  public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
    return filteredFunctionsDelegate.incidentNodes(pEdge);
  }

  @Override
  public FunctionEntryNode functionEntryNode(FunctionExitNode pFunctionExitNode) {
    // The unfiltered delegate may provide a more performant implementation that returns the same
    // node that `super.functionExitNode` would.
    return unfilteredDelegate.functionEntryNode(pFunctionExitNode);
  }

  @Override
  public Optional<FunctionExitNode> functionExitNode(FunctionEntryNode pFunctionEntryNode) {
    // The unfiltered delegate may provide a more performant implementation that returns the same
    // node that `super.functionExitNode` would.
    return unfilteredDelegate.functionExitNode(pFunctionEntryNode);
  }
}
