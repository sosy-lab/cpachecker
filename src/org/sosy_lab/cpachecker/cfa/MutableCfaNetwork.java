// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.MutableNetwork;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

public final class MutableCfaNetwork extends CfaNetwork
    implements MutableNetwork<CFANode, CFAEdge> {

  private final MutableCFA mutableCfa;
  private final CfaNetwork mutableCfaView;

  private MutableCfaNetwork(MutableCFA pMutableCfa, CfaNetwork pMutableCfaView) {
    mutableCfa = pMutableCfa;
    mutableCfaView = pMutableCfaView;
  }

  public static MutableCfaNetwork of(MutableCFA pMutableCfa) {
    return new MutableCfaNetwork(pMutableCfa, CfaNetwork.of(pMutableCfa));
  }

  @Override
  public Set<CFAEdge> inEdges(CFANode pNode) {
    return mutableCfaView.inEdges(pNode);
  }

  @Override
  public Set<CFAEdge> outEdges(CFANode pNode) {
    return mutableCfaView.outEdges(pNode);
  }

  @Override
  public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
    return mutableCfaView.incidentNodes(pEdge);
  }

  @Override
  public Set<CFANode> nodes() {
    return mutableCfaView.nodes();
  }

  // modifying operations

  @Override
  public boolean addNode(CFANode pNode) {

    checkNotNull(pNode);

    mutableCfa.addNode(pNode);

    // FIXME: only return true if the network was modified as a result of this call
    return true;
  }

  @Override
  public boolean removeNode(CFANode pNode) {

    checkNotNull(pNode);

    mutableCfa.removeNode(pNode);

    // FIXME: only return true if the network was modified as a result of this call
    return true;
  }

  @Override
  public boolean addEdge(CFANode pPredecessor, CFANode pSuccessor, CFAEdge pEdge) {

    checkArgument(
        pPredecessor.equals(pEdge.getPredecessor()),
        "mismatch between specified predecessor and edge endpoint: %s not equal to %s",
        pPredecessor,
        pEdge.getPredecessor());
    checkArgument(
        pSuccessor.equals(pEdge.getSuccessor()),
        "mismatch between specified successor and edge endpoint: %s not equal to %s ",
        pSuccessor,
        pEdge.getSuccessor());

    if (pEdge instanceof FunctionSummaryEdge) {

      checkArgument(
          pPredecessor.getLeavingSummaryEdge() == null,
          "leaving summary edge already exists: %s, cannot add: %s",
          pPredecessor.getLeavingSummaryEdge(),
          pEdge);
      checkArgument(
          pSuccessor.getEnteringSummaryEdge() == null,
          "entering summary edge already exists: %s, cannot add: %s",
          pSuccessor.getEnteringSummaryEdge(),
          pEdge);

      pPredecessor.addLeavingSummaryEdge((FunctionSummaryEdge) pEdge);
      pSuccessor.addEnteringSummaryEdge((FunctionSummaryEdge) pEdge);

    } else {

      for (CFAEdge predecessorOutEdge : CFAUtils.leavingEdges(pPredecessor)) {
        checkArgument(
            !predecessorOutEdge.getSuccessor().equals(pSuccessor),
            "parallel edges are not allowed: %s is parallel to %s",
            predecessorOutEdge,
            pEdge);
      }

      pPredecessor.addLeavingEdge(pEdge);
      pSuccessor.addEnteringEdge(pEdge);
    }

    return true;
  }

  @Override
  public boolean addEdge(EndpointPair<CFANode> pEndpoints, CFAEdge pEdge) {

    checkArgument(pEndpoints.isOrdered(), "endpoints must be ordered");

    return addEdge(pEndpoints.source(), pEndpoints.target(), pEdge);
  }

  /**
   * Adds the specified CFA edge between its predecessor ({@link CFAEdge#getPredecessor()}) and
   * successor ({@link CFAEdge#getSuccessor()}) to this network.
   *
   * <p>Calling this method has the same effect as calling {@code addEdge(edge.getPredecessor(),
   * edge.getSuccessor(), edge)}.
   *
   * @param pEdge the edge to add to this network
   * @return {@code true} if the network was modified as a result of this call
   * @throws IllegalArgumentException if introducing the edge would lead to parallel edges
   * @throws IllegalArgumentException if introducing the edge would lead to more than one summary
   *     edge leaving or entering a node
   */
  public boolean addEdge(CFAEdge pEdge) {
    return addEdge(pEdge.getPredecessor(), pEdge.getSuccessor(), pEdge);
  }

  @Override
  public boolean removeEdge(CFAEdge pEdge) {

    CFANode predecessor = pEdge.getPredecessor();
    CFANode successor = pEdge.getSuccessor();

    if (pEdge instanceof FunctionSummaryEdge) {
      if (pEdge.equals(predecessor.getLeavingSummaryEdge())
          && pEdge.equals(successor.getEnteringSummaryEdge())) {

        predecessor.removeLeavingSummaryEdge((FunctionSummaryEdge) pEdge);
        successor.removeEnteringSummaryEdge((FunctionSummaryEdge) pEdge);

        return true;
      }
    } else {
      if (CFAUtils.leavingEdges(predecessor).contains(pEdge)
          && CFAUtils.enteringEdges(successor).contains(pEdge)) {

        predecessor.removeLeavingEdge(pEdge);
        successor.removeEnteringEdge(pEdge);

        return true;
      }
    }

    return false;
  }
}
