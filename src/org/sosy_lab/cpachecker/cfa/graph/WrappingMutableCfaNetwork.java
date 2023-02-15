// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.graph.EndpointPair;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * A {@link MutableCfaNetwork} that represents its wrapped {@link MutableCFA}.
 *
 * <p>All modifying operations change the wrapped {@link MutableCFA}.
 *
 * <p>The CFA represented by a {@link WrappingMutableCfaNetwork} always matches the CFA represented
 * by its elements (e.g., {@link CFAEdge#getSuccessor()} and {@link
 * WrappingMutableCfaNetwork#successor(CFAEdge)} always return the same value). Endpoints of a CFA
 * edge and endpoints given as arguments to an {@code addEdge} method must match.
 */
final class WrappingMutableCfaNetwork extends WrappingCfaNetwork implements MutableCfaNetwork {

  private final MutableCFA mutableCfa;

  private WrappingMutableCfaNetwork(MutableCFA pMutableCfa) {
    super(pMutableCfa);
    mutableCfa = checkNotNull(pMutableCfa);
  }

  static MutableCfaNetwork wrap(MutableCFA pMutableCfa) {
    return CheckingCfaNetwork.wrapIfAssertionsEnabled(new WrappingMutableCfaNetwork(pMutableCfa));
  }

  @Override
  public boolean addNode(CFANode pNode) {
    return mutableCfa.addNode(checkNotNull(pNode));
  }

  @Override
  public boolean addEdge(CFANode pPredecessor, CFANode pSuccessor, CFAEdge pEdge) {
    checkArgument(
        pPredecessor.equals(pEdge.getPredecessor()),
        "Mismatch between specified predecessor and edge endpoint: %s not equal to %s",
        pPredecessor,
        pEdge.getPredecessor());
    checkArgument(
        pSuccessor.equals(pEdge.getSuccessor()),
        "Mismatch between specified successor and edge endpoint: %s not equal to %s ",
        pSuccessor,
        pEdge.getSuccessor());

    for (CFAEdge predecessorOutEdge : outEdges(pPredecessor)) {
      checkArgument(
          !successor(predecessorOutEdge).equals(pSuccessor),
          "Parallel edges are not allowed: %s is parallel to %s",
          predecessorOutEdge,
          pEdge);
    }

    addNode(pPredecessor);
    addNode(pSuccessor);

    if (pEdge instanceof FunctionSummaryEdge) {

      checkArgument(
          pPredecessor.getLeavingSummaryEdge() == null,
          "Leaving summary edge already exists: %s, cannot add: %s",
          pPredecessor.getLeavingSummaryEdge(),
          pEdge);
      checkArgument(
          pSuccessor.getEnteringSummaryEdge() == null,
          "Entering summary edge already exists: %s, cannot add: %s",
          pSuccessor.getEnteringSummaryEdge(),
          pEdge);

      pPredecessor.addLeavingSummaryEdge((FunctionSummaryEdge) pEdge);
      pSuccessor.addEnteringSummaryEdge((FunctionSummaryEdge) pEdge);

    } else {
      pPredecessor.addLeavingEdge(pEdge);
      pSuccessor.addEnteringEdge(pEdge);
    }

    return true;
  }

  @Override
  public boolean addEdge(EndpointPair<CFANode> pEndpoints, CFAEdge pNewEdge) {
    checkArgument(pEndpoints.isOrdered(), "Endpoints must be ordered");

    return addEdge(pEndpoints.source(), pEndpoints.target(), pNewEdge);
  }

  @Override
  public boolean removeNode(CFANode pNode) {
    checkNotNull(pNode);
    boolean nodeRemoved = mutableCfa.removeNode(pNode);
    if (nodeRemoved) {
      for (CFAEdge edge : incidentEdges(pNode)) {
        removeEdge(edge);
      }
      if (pNode instanceof FunctionExitNode exitNode) {
        exitNode.getEntryNode().removeExitNode();
      }
    }

    return nodeRemoved;
  }

  @Override
  public boolean removeEdge(CFAEdge pEdge) {
    CFANode predecessor = predecessor(pEdge);
    CFANode successor = successor(pEdge);
    Set<CFANode> nodes = nodes();

    if (nodes.contains(predecessor) && nodes.contains(successor)) {
      if (pEdge instanceof FunctionSummaryEdge) {
        // remove summary edge, if it exists
        if (pEdge.equals(predecessor.getLeavingSummaryEdge())
            && pEdge.equals(successor.getEnteringSummaryEdge())) {

          predecessor.removeLeavingSummaryEdge((FunctionSummaryEdge) pEdge);
          successor.removeEnteringSummaryEdge((FunctionSummaryEdge) pEdge);

          return true;
        }
      } else {
        // remove non-summary edge, if it exists
        if (CFAUtils.leavingEdges(predecessor).contains(pEdge)
            && CFAUtils.enteringEdges(successor).contains(pEdge)) {

          predecessor.removeLeavingEdge(pEdge);
          successor.removeEnteringEdge(pEdge);

          return true;
        }
      }
    }

    return false;
  }
}
