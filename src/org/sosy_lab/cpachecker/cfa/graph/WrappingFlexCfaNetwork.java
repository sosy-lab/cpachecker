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
import com.google.common.graph.Network;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.graph.ForwardingMutableNetwork;

/**
 * A {@link FlexCfaNetwork} that uses its wrapped {@link MutableNetwork} as underlying data
 * structure for representing a CFA.
 *
 * <p>All modifying calls change the wrapped {@link MutableNetwork}.
 */
final class WrappingFlexCfaNetwork
    implements FlexCfaNetwork, ForwardingCfaNetwork, ForwardingMutableNetwork<CFANode, CFAEdge> {

  private final MutableNetwork<CFANode, CFAEdge> mutableNetwork;

  private WrappingFlexCfaNetwork(MutableNetwork<CFANode, CFAEdge> pMutableNetwork) {
    mutableNetwork = pMutableNetwork;
  }

  static FlexCfaNetwork wrap(MutableNetwork<CFANode, CFAEdge> pMutableNetwork) {
    return new WrappingFlexCfaNetwork(checkNotNull(pMutableNetwork));
  }

  @Override
  public Network<CFANode, CFAEdge> delegateNetwork() {
    return mutableNetwork;
  }

  @Override
  public MutableNetwork<CFANode, CFAEdge> delegateMutableNetwork() {
    return mutableNetwork;
  }

  @Override
  public CfaNetwork delegateCfaNetwork() {
    return new AbstractCfaNetwork() {

      @Override
      public Set<CFANode> nodes() {
        return mutableNetwork.nodes();
      }

      @Override
      public Set<CFAEdge> inEdges(CFANode pNode) {
        return mutableNetwork.inEdges(pNode);
      }

      @Override
      public Set<CFAEdge> outEdges(CFANode pNode) {
        return mutableNetwork.outEdges(pNode);
      }

      @Override
      public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
        return mutableNetwork.incidentNodes(pEdge);
      }
    };
  }

  @Override
  public boolean addEdge(CFANode pPredecessor, CFANode pSuccessor, CFAEdge pNewEdge) {
    return ForwardingMutableNetwork.super.addEdge(pPredecessor, pSuccessor, pNewEdge);
  }

  @Override
  public boolean addEdge(EndpointPair<CFANode> pEndpoints, CFAEdge pNewEdge) {
    return ForwardingMutableNetwork.super.addEdge(pEndpoints, pNewEdge);
  }

  // `FlexCfaNetwork` operations

  @Override
  public void insertPredecessor(CFANode pNewPredecessor, CFAEdge pNewInEdge, CFANode pNode) {

    ImmutableList<CFAEdge> nodeInEdges = ImmutableList.copyOf(inEdges(pNode));
    List<CFANode> nodePredecessors = new ArrayList<>(nodeInEdges.size());

    for (CFAEdge nodeInEdge : nodeInEdges) {
      nodePredecessors.add(predecessor(nodeInEdge));
      removeEdge(nodeInEdge);
    }

    addNode(pNewPredecessor);

    for (int i = 0; i < nodeInEdges.size(); i++) {
      addEdge(nodePredecessors.get(i), nodeInEdges.get(i), pNewPredecessor);
    }

    addEdge(pNewPredecessor, pNewInEdge, pNode);
  }

  @Override
  public void insertSuccessor(CFANode pNode, CFAEdge pNewOutEdge, CFANode pNewSuccessor) {

    ImmutableList<CFAEdge> nodeOutEdges = ImmutableList.copyOf(outEdges(pNode));
    List<CFANode> nodeSuccessors = new ArrayList<>(nodeOutEdges.size());

    for (CFAEdge nodeOutEdge : nodeOutEdges) {
      nodeSuccessors.add(successor(nodeOutEdge));
      removeEdge(nodeOutEdge);
    }

    addNode(pNewSuccessor);

    for (int i = 0; i < nodeOutEdges.size(); i++) {
      addEdge(pNewSuccessor, nodeOutEdges.get(i), nodeSuccessors.get(i));
    }

    addEdge(pNode, pNewOutEdge, pNewSuccessor);
  }

  @Override
  public void replaceNode(CFANode pNode, CFANode pNewNode) {

    addNode(pNewNode);

    for (CFAEdge inEdge : ImmutableList.copyOf(inEdges(pNode))) {
      CFANode nodePredecessor = predecessor(inEdge);
      removeEdge(inEdge);
      addEdge(nodePredecessor, inEdge, pNewNode);
    }

    for (CFAEdge outEdge : ImmutableList.copyOf(outEdges(pNode))) {
      CFANode nodeSuccessor = successor(outEdge);
      removeEdge(outEdge);
      addEdge(pNewNode, outEdge, nodeSuccessor);
    }

    removeNode(pNode);
  }

  @Override
  public void replaceEdge(CFAEdge pEdge, CFAEdge pNewEdge) {

    EndpointPair<CFANode> endpoints = incidentNodes(pEdge);
    removeEdge(pEdge);
    addEdge(endpoints, pNewEdge);
  }
}
