// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableNetwork;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.graph.ForwardingNetwork;

public class ChangeRecordingCfaMutableNetwork extends ForwardingNetwork<CFANode, CFAEdge>
    implements CfaMutableNetwork {

  private final PlainCfaMutableNetwork delegate;

  private final Set<CFAEdge> addedEdges = new HashSet<>();
  private final Set<CFAEdge> removedEdges = new HashSet<>();
  private final Set<CFANode> addedNodes = new HashSet<>();
  private final Set<CFANode> removedNodes = new HashSet<>();

  ChangeRecordingCfaMutableNetwork(final MutableNetwork<CFANode, CFAEdge> pDelegate) {
    super(pDelegate);
    delegate = new PlainCfaMutableNetwork(pDelegate);
  }

  @Override
  public void insertPredecessor(
      final CFANode pNewPredecessor, final CFANode pNode, final CFAEdge pNewInEdge) {

    addedNodes.add(pNewPredecessor);
    addedEdges.add(pNewInEdge);

    delegate.insertPredecessor(pNewPredecessor, pNode, pNewInEdge);
  }

  @Override
  public void insertSuccessor(
      final CFANode pNode, final CFANode pNewSuccessor, final CFAEdge pNewOutEdge) {

    addedNodes.add(pNewSuccessor);
    addedEdges.add(pNewOutEdge);

    delegate.insertSuccessor(pNode, pNewSuccessor, pNewOutEdge);
  }

  @Override
  public void replace(final CFANode pNode, final CFANode pNewNode) {
    addedNodes.add(pNewNode);
    removedNodes.add(pNode);

    delegate.replace(pNode, pNewNode);
  }

  @Override
  public void replace(final CFAEdge pEdge, final CFAEdge pNewEdge) {
    removedEdges.add(pEdge);
    addedEdges.add(pNewEdge);

    delegate.replace(pEdge, pNewEdge);
  }

  @Override
  public boolean addEdge(final EndpointPair<CFANode> pEndpointPair, final CFAEdge pEdge) {
    return addEdge(pEndpointPair.nodeU(), pEndpointPair.nodeV(), pEdge);
  }

  @Override
  public boolean addEdge(
      final CFANode pPredecessor, final CFANode pSuccessor, final CFAEdge pEdge) {

    final boolean predecessorAlreadyPresent = delegate.nodes().contains(pPredecessor);
    final boolean successorAlreadyPresent = delegate.nodes().contains(pSuccessor);

    final boolean networkModified = delegate.addEdge(pPredecessor, pSuccessor, pEdge);

    if (networkModified) {
      addedEdges.add(pEdge);

      if (!predecessorAlreadyPresent) {
        addedNodes.add(pPredecessor);
      }

      if (!successorAlreadyPresent) {
        addedNodes.add(pSuccessor);
      }
    }
    return networkModified;
  }

  @Override
  public boolean addNode(final CFANode pNode) {
    final boolean networkModified = delegate.addNode(pNode);

    if (networkModified) {
      addedNodes.add(pNode);
    }
    return networkModified;
  }

  @Override
  public boolean removeEdge(final CFAEdge pEdge) {
    removedEdges.remove(pEdge);
    return delegate.removeEdge(pEdge);
  }

  @Override
  public boolean removeNode(final CFANode pNode) {
    final Set<CFAEdge> incidentEdges =
        ImmutableSet.<CFAEdge>builder()
            .addAll(delegate.inEdges(pNode))
            .addAll(delegate.outEdges(pNode))
            .build();

    final boolean networkModified = delegate.removeNode(pNode);

    if (networkModified) {
      removedNodes.add(pNode);
      removedEdges.addAll(incidentEdges);
    }
    return networkModified;
  }

  public Set<CFAEdge> getAddedEdges() {
    return ImmutableSet.copyOf(addedEdges);
  }

  public Set<CFANode> getAddedNodes() {
    return ImmutableSet.copyOf(addedNodes);
  }

  public Set<CFAEdge> getRemovedEdges() {
    return ImmutableSet.copyOf(removedEdges);
  }

  public Set<CFANode> getRemovedNodes() {
    return ImmutableSet.copyOf(removedNodes);
  }
}
