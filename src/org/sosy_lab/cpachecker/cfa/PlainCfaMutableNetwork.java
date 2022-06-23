// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.ImmutableList;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableNetwork;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.graph.ForwardingMutableNetwork;

public class PlainCfaMutableNetwork extends ForwardingMutableNetwork<CFANode, CFAEdge>
    implements CfaMutableNetwork {

  private final MutableNetwork<CFANode, CFAEdge> delegate;

  PlainCfaMutableNetwork(final MutableNetwork<CFANode, CFAEdge> pDelegate) {
    delegate = pDelegate;
  }

  @Override
  protected MutableNetwork<CFANode, CFAEdge> delegate() {
    return delegate;
  }

  @Override
  public void insertPredecessor(
      final CFANode pNewPredecessor, final CFANode pNode, final CFAEdge pNewInEdge) {

    final List<CFAEdge> nodeInEdges = ImmutableList.copyOf(inEdges(pNode));
    final List<CFANode> nodeUs = new ArrayList<>(nodeInEdges.size());

    for (final CFAEdge nodeInEdge : nodeInEdges) {
      nodeUs.add(incidentNodes(nodeInEdge).nodeU());
      removeEdge(nodeInEdge);
    }

    addNode(pNewPredecessor);

    for (int index = 0; index < nodeInEdges.size(); index++) {
      addEdge(nodeUs.get(index), pNewPredecessor, nodeInEdges.get(index));
    }

    addEdge(pNewPredecessor, pNode, pNewInEdge);
  }

  @Override
  public void insertSuccessor(
      final CFANode pNode, final CFANode pNewSuccessor, final CFAEdge pNewOutEdge) {

    final List<CFAEdge> nodeOutEdges = ImmutableList.copyOf(outEdges(pNode));
    final List<CFANode> nodeVs = new ArrayList<>(nodeOutEdges.size());

    for (final CFAEdge nodeOutEdge : nodeOutEdges) {
      nodeVs.add(incidentNodes(nodeOutEdge).nodeV());
      removeEdge(nodeOutEdge);
    }

    addNode(pNewSuccessor);

    for (int index = 0; index < nodeOutEdges.size(); index++) {
      addEdge(pNewSuccessor, nodeVs.get(index), nodeOutEdges.get(index));
    }

    addEdge(pNode, pNewSuccessor, pNewOutEdge);
  }

  @Override
  public void replace(final CFANode pNode, final CFANode pNewNode) {
    addNode(pNewNode);

    for (final CFAEdge inEdge : ImmutableList.copyOf(inEdges(pNode))) {
      final CFANode nodeU = incidentNodes(inEdge).nodeU();
      removeEdge(inEdge);
      addEdge(nodeU, pNewNode, inEdge);
    }

    for (final CFAEdge outEdge : ImmutableList.copyOf(outEdges(pNode))) {
      final CFANode nodeV = incidentNodes(outEdge).nodeV();
      removeEdge(outEdge);
      addEdge(pNewNode, nodeV, outEdge);
    }

    removeNode(pNode);
  }

  @Override
  public void replace(final CFAEdge pEdge, final CFAEdge pNewEdge) {
    final EndpointPair<CFANode> endpoints = incidentNodes(pEdge);
    removeEdge(pEdge);
    addEdge(endpoints.nodeU(), endpoints.nodeV(), pNewEdge);
  }
}
