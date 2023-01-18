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
 * Checks whether the CFA represented by a {@link CfaNetwork} is the same CFA represented by its
 * individual elements (whether, e.g., {@link CFAEdge#getSuccessor()} and {@link
 * CfaNetwork#successor(CFAEdge)} return the same value).
 */
public final class CfaNetworkConsistent extends AbstractCfaValidator {

  private CfaValidationResult checkNodeInEdges(CfaNetwork pCfaNetwork, CFANode pNode) {
    Set<CFAEdge> inEdges = pCfaNetwork.inEdges(pNode);
    Set<CFAEdge> enteringEdges = CFAUtils.allEnteringEdges(pNode).toSet();
    if (!inEdges.equals(enteringEdges)) {
      return fail(
          "CfaNetwork inconsistent: cfaNetwork.inEdges(%s) is %s, but CFAUtils.allEnteringEdges(%s)"
              + " is %s",
          pNode, inEdges, pNode, enteringEdges);
    }
    return pass();
  }

  private CfaValidationResult checkNodeOutEdges(CfaNetwork pCfaNetwork, CFANode pNode) {
    Set<CFAEdge> outEdges = pCfaNetwork.outEdges(pNode);
    Set<CFAEdge> leavingEdges = CFAUtils.allLeavingEdges(pNode).toSet();
    if (!outEdges.equals(leavingEdges)) {
      return fail(
          "CfaNetwork inconsistent: cfaNetwork.outEdges(%s) is %s, but CFAUtils.allLeavingEdges(%s)"
              + " is %s",
          pNode, outEdges, pNode, leavingEdges);
    }
    return pass();
  }

  private CfaValidationResult checkEdgePredecessor(CfaNetwork pCfaNetwork, CFAEdge pEdge) {
    CFANode networkPredecessor = pCfaNetwork.predecessor(pEdge);
    CFANode elementPredecessor = pEdge.getPredecessor();
    if (!networkPredecessor.equals(elementPredecessor)) {
      return fail(
          "CfaNetwork inconsistent: cfaNetwork.predecessor(%s) is %s, but %s.getPredecessor() is"
              + " %s",
          pEdge, networkPredecessor, pEdge, elementPredecessor);
    }
    return pass();
  }

  private CfaValidationResult checkEdgeSuccessor(CfaNetwork pCfaNetwork, CFAEdge pEdge) {
    CFANode networkSuccessor = pCfaNetwork.successor(pEdge);
    CFANode elementSuccessor = pEdge.getSuccessor();
    if (!networkSuccessor.equals(elementSuccessor)) {
      return fail(
          "CfaNetwork inconsistent: cfaNetwork.successor(%s) is %s, but %s.getSuccessor() is %s",
          pEdge, networkSuccessor, pEdge, elementSuccessor);
    }
    return pass();
  }

  @Override
  public CfaValidationResult check(CfaNetwork pCfaNetwork, CfaMetadata pCfaMetadata) {
    return CfaValidator.createElementValidator(
            node ->
                checkNodeInEdges(pCfaNetwork, node).combine(checkNodeOutEdges(pCfaNetwork, node)),
            edge ->
                checkEdgePredecessor(pCfaNetwork, edge)
                    .combine(checkEdgeSuccessor(pCfaNetwork, edge)))
        .check(pCfaNetwork, pCfaMetadata);
  }
}
