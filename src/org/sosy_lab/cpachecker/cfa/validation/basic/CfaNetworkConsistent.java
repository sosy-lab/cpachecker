// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.validation.basic;

import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.validation.AbstractCfaValidator;
import org.sosy_lab.cpachecker.cfa.validation.CfaValidationResult;
import org.sosy_lab.cpachecker.cfa.validation.CfaValidator;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * This {@link CfaValidator} ensures that the CFA represented by a {@link CfaNetwork} is the same
 * CFA represented by its individual elements (ensures that, e.g., {@link CFAEdge#getSuccessor()}
 * and {@link CfaNetwork#successor(CFAEdge)} return the same value).
 *
 * <p>Not all {@link CfaNetwork} implementations guarantee that the CFA represented by a {@link
 * CfaNetwork} is the same CFA represented by its individual elements, so check the documentation of
 * the {@link CfaNetwork} implementation before using this {@link CfaValidator}.
 */
public final class CfaNetworkConsistent extends AbstractCfaValidator {

  private CfaValidationResult checkNodeInEdges(CfaNetwork pCfaNetwork, CFANode pNode) {
    Set<CFAEdge> inEdges = pCfaNetwork.inEdges(pNode);
    Set<CFAEdge> enteringEdges = CFAUtils.allEnteringEdges(pNode).toSet();
    return check(
        inEdges.equals(enteringEdges),
        "CfaNetwork inconsistent: `cfaNetwork.inEdges(%1$s)` is %2$s"
            + ", but `CFAUtils.allEnteringEdges(%1$s)` is %3$s",
        pNode,
        inEdges,
        enteringEdges);
  }

  private CfaValidationResult checkNodeOutEdges(CfaNetwork pCfaNetwork, CFANode pNode) {
    Set<CFAEdge> outEdges = pCfaNetwork.outEdges(pNode);
    Set<CFAEdge> leavingEdges = CFAUtils.allLeavingEdges(pNode).toSet();
    return check(
        outEdges.equals(leavingEdges),
        "CfaNetwork inconsistent: `cfaNetwork.outEdges(%1$s)` is %2$s"
            + ", but `CFAUtils.allLeavingEdges(%1$s)` is %3$s",
        pNode,
        outEdges,
        leavingEdges);
  }

  private CfaValidationResult checkNode(CfaNetwork pCfaNetwork, CFANode pNode) {
    return checkNodeInEdges(pCfaNetwork, pNode).combine(checkNodeOutEdges(pCfaNetwork, pNode));
  }

  private CfaValidationResult checkEdgePredecessor(CfaNetwork pCfaNetwork, CFAEdge pEdge) {
    CFANode networkPredecessor = pCfaNetwork.predecessor(pEdge);
    CFANode elementPredecessor = pEdge.getPredecessor();
    return check(
        networkPredecessor.equals(elementPredecessor),
        "CfaNetwork inconsistent: `cfaNetwork.predecessor(%1$s)` is %2$s"
            + ", but `%1$s.getPredecessor()` is %3$s",
        pEdge,
        networkPredecessor,
        elementPredecessor);
  }

  private CfaValidationResult checkEdgeSuccessor(CfaNetwork pCfaNetwork, CFAEdge pEdge) {
    CFANode networkSuccessor = pCfaNetwork.successor(pEdge);
    CFANode elementSuccessor = pEdge.getSuccessor();
    return check(
        networkSuccessor.equals(elementSuccessor),
        "CfaNetwork inconsistent: `cfaNetwork.successor(%1$s)` is %2$s"
            + ", but `%1$s.getSuccessor()` is %3$s",
        pEdge,
        networkSuccessor,
        elementSuccessor);
  }

  private CfaValidationResult checkEdge(CfaNetwork pCfaNetwork, CFAEdge pEdge) {
    return checkEdgePredecessor(pCfaNetwork, pEdge).combine(checkEdgeSuccessor(pCfaNetwork, pEdge));
  }

  @Override
  public CfaValidationResult check(CfaNetwork pCfaNetwork, CfaMetadata pCfaMetadata) {
    return CfaValidator.createElementValidator(
            node -> checkNode(pCfaNetwork, node), edge -> checkEdge(pCfaNetwork, edge))
        .check(pCfaNetwork, pCfaMetadata);
  }
}
