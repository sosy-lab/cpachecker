// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.graph.ImmutableNetwork;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.graph.ForwardingNetwork;

/**
 * A {@link CfaNetwork} that never changes.
 *
 * <p>Due to it's immutability, it's possible to use a more efficient internal representation, which
 * can improve performance.
 */
final class ImmutableCfaNetwork extends ForwardingNetwork<CFANode, CFAEdge> implements CfaNetwork {

  private final ImmutableNetwork<CFANode, CFAEdge> delegate;
  private final ImmutableSet<FunctionEntryNode> entryNodes;
  private final ImmutableBiMap<FunctionEntryNode, Optional<FunctionExitNode>> entryExitNodes;

  private ImmutableCfaNetwork(CfaNetwork pCfaNetwork) {
    delegate = ImmutableNetwork.copyOf(pCfaNetwork);
    entryNodes = ImmutableSet.copyOf(pCfaNetwork.entryNodes());
    entryExitNodes = ImmutableBiMap.copyOf(Maps.toMap(entryNodes, pCfaNetwork::functionExitNode));
  }

  static ImmutableCfaNetwork copyOf(CfaNetwork pCfaNetwork) {
    return new ImmutableCfaNetwork(pCfaNetwork);
  }

  @Override
  public CfaNetwork immutableCopy() {
    return this;
  }

  @Override
  public ImmutableNetwork<CFANode, CFAEdge> delegate() {
    return delegate;
  }

  @Override
  public Set<FunctionEntryNode> entryNodes() {
    return entryNodes;
  }

  @Override
  public CFANode predecessor(CFAEdge pEdge) {
    return incidentNodes(pEdge).source();
  }

  @Override
  public CFANode successor(CFAEdge pEdge) {
    return incidentNodes(pEdge).target();
  }

  @Override
  public FunctionEntryNode functionEntryNode(FunctionExitNode pFunctionExitNode) {
    @Nullable FunctionEntryNode entryNode =
        entryExitNodes.inverse().get(Optional.of(pFunctionExitNode));
    checkState(
        entryNode != null,
        "Function exit node doesn't have a corresponding entry node: %s",
        pFunctionExitNode);
    return entryNode;
  }

  @Override
  public Optional<FunctionExitNode> functionExitNode(FunctionEntryNode pFunctionEntryNode) {
    return entryExitNodes.get(pFunctionEntryNode);
  }
}
