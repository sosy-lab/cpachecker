// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.graph.ForwardingMutableNetwork;

/**
 * A {@link FlexCfaNetwork} that forwards as many calls as possible to a wrapped {@link
 * MutableNetwork}.
 */
final class ForwardingFlexCfaNetwork extends ForwardingMutableNetwork<CFANode, CFAEdge>
    implements FlexCfaNetwork {

  private final CfaNetwork delegate;

  private ForwardingFlexCfaNetwork(MutableNetwork<CFANode, CFAEdge> pDelegate) {
    super(pDelegate);
    delegate = new ForwardingCfaNetwork(pDelegate);
  }

  static FlexCfaNetwork of(MutableNetwork<CFANode, CFAEdge> pDelegate) {
    return new ForwardingFlexCfaNetwork(checkNotNull(pDelegate));
  }

  // `CfaNetwork` specific

  @Override
  public CFANode predecessor(CFAEdge pEdge) {
    return delegate.predecessor(pEdge);
  }

  @Override
  public CFANode successor(CFAEdge pEdge) {
    return delegate.predecessor(pEdge);
  }

  @Override
  public FunctionEntryNode functionEntryNode(FunctionSummaryEdge pFunctionSummaryEdge) {
    return delegate.functionEntryNode(pFunctionSummaryEdge);
  }

  @Override
  public Optional<FunctionExitNode> functionExitNode(FunctionEntryNode pFunctionEntryNode) {
    return delegate.functionExitNode(pFunctionEntryNode);
  }

  @Override
  public FunctionSummaryEdge functionSummaryEdge(FunctionCallEdge pFunctionCallEdge) {
    return delegate.functionSummaryEdge(pFunctionCallEdge);
  }

  @Override
  public FunctionSummaryEdge functionSummaryEdge(FunctionReturnEdge pFunctionReturnEdge) {
    return delegate.functionSummaryEdge(pFunctionReturnEdge);
  }

  // `FlexCfaNetwork` operations

  @Override
  public void insertPredecessor(CFANode pNewPredecessor, CFAEdge pNewInEdge, CFANode pNode) {
    ImmutableList<CFAEdge> nodeInEdges = ImmutableList.copyOf(inEdges(pNode));
    List<CFANode> nodePredecessors = new ArrayList<>(nodeInEdges.size());
    for (CFAEdge nodeInEdge : nodeInEdges) {
      nodePredecessors.add(incidentNodes(nodeInEdge).source());
      removeEdge(nodeInEdge);
    }
    addNode(pNewPredecessor);
    for (int index = 0; index < nodeInEdges.size(); index++) {
      addEdge(nodePredecessors.get(index), pNewPredecessor, nodeInEdges.get(index));
    }
    addEdge(pNewPredecessor, pNode, pNewInEdge);
  }

  @Override
  public void insertSuccessor(CFANode pNode, CFAEdge pNewOutEdge, CFANode pNewSuccessor) {
    ImmutableList<CFAEdge> nodeOutEdges = ImmutableList.copyOf(outEdges(pNode));
    List<CFANode> nodeSuccessors = new ArrayList<>(nodeOutEdges.size());
    for (CFAEdge nodeOutEdge : nodeOutEdges) {
      nodeSuccessors.add(incidentNodes(nodeOutEdge).target());
      removeEdge(nodeOutEdge);
    }
    addNode(pNewSuccessor);
    for (int index = 0; index < nodeOutEdges.size(); index++) {
      addEdge(pNewSuccessor, nodeSuccessors.get(index), nodeOutEdges.get(index));
    }
    addEdge(pNode, pNewSuccessor, pNewOutEdge);
  }

  @Override
  public void replaceNode(CFANode pNode, CFANode pNewNode) {
    addNode(pNewNode);
    for (CFAEdge inEdge : ImmutableList.copyOf(inEdges(pNode))) {
      CFANode nodePredecessor = incidentNodes(inEdge).source();
      removeEdge(inEdge);
      addEdge(nodePredecessor, pNewNode, inEdge);
    }
    for (CFAEdge outEdge : ImmutableList.copyOf(outEdges(pNode))) {
      CFANode nodeSuccessor = incidentNodes(outEdge).target();
      removeEdge(outEdge);
      addEdge(pNewNode, nodeSuccessor, outEdge);
    }
    removeNode(pNode);
  }

  @Override
  public void replaceEdge(CFAEdge pEdge, CFAEdge pNewEdge) {
    EndpointPair<CFANode> endpoints = incidentNodes(pEdge);
    removeEdge(pEdge);
    addEdge(endpoints.source(), endpoints.target(), pNewEdge);
  }
}
