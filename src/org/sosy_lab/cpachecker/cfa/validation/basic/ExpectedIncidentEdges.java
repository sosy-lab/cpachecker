// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.validation.basic;

import com.google.common.collect.Iterables;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.validation.AbstractCfaValidator;
import org.sosy_lab.cpachecker.cfa.validation.CfaValidationResult;
import org.sosy_lab.cpachecker.cfa.validation.CfaValidator;

/**
 * This {@link CfaValidator} ensures that CFA nodes of a certain type have the correct entering and
 * leaving edges.
 */
public final class ExpectedIncidentEdges extends AbstractCfaValidator {

  private CfaValidationResult checkNode(CfaNetwork pCfaNetwork, CFANode pNode) {
    switch (pCfaNetwork.outDegree(pNode)) {
      case 0:
        if (!(pNode instanceof CFATerminationNode)) {
          return fail("Dead end at node '%s'", pNode);
        }
        break;

      case 1:
        CFAEdge edge = Iterables.getOnlyElement(pCfaNetwork.outEdges(pNode));
        if (edge instanceof AssumeEdge) {
          return fail(
              "Only a single assume edge '%s' leaving node '%s', two assume edges expected",
              edge, pNode);
        }
        if (edge instanceof CFunctionSummaryStatementEdge) {
          // TODO: handle C specific edge in `cfa.validator.c` package
          return fail(
              "CFunctionSummaryStatementEdge is not paired with CFunctionCallEdge at node '%s'",
              pNode);
        }
        break;

      case 2:
        CFAEdge edge1 = pNode.getLeavingEdge(0);
        CFAEdge edge2 = pNode.getLeavingEdge(1);
        // TODO: handle C specific edge in `cfa.validator.c` package
        if (edge1 instanceof CFunctionSummaryStatementEdge) {
          if (!(edge2 instanceof CFunctionCallEdge)) {
            return fail(
                "CFunctionSummaryStatementEdge is not paired with CFunctionCallEdge at node '%s'",
                pNode);
          }
        } else if (edge2 instanceof CFunctionSummaryStatementEdge) {
          if (!(edge1 instanceof CFunctionCallEdge)) {
            return fail(
                "CFunctionSummaryStatementEdge is not paired with CFunctionCallEdge at node %s",
                pNode);
          }
        } else {
          if (!(edge1 instanceof AssumeEdge && edge2 instanceof AssumeEdge)) {
            return fail("Branching without conditions at node '%s'", pNode);
          }

          AssumeEdge ae1 = (AssumeEdge) edge1;
          AssumeEdge ae2 = (AssumeEdge) edge2;
          if (ae1.getTruthAssumption() == ae2.getTruthAssumption()) {
            return fail("Inconsistent branching at node '%s'", pNode);
          }
        }
        break;

      default:
        return fail(
            "Unexpected number of leaving edges at '%s': %s", pNode, pCfaNetwork.outEdges(pNode));
    }
    return pass();
  }

  @Override
  public CfaValidationResult check(CfaNetwork pCfaNetwork, CfaMetadata pCfaMetadata) {
    return CfaValidator.createNodeValidator(node -> checkNode(pCfaNetwork, node))
        .check(pCfaNetwork, pCfaMetadata);
  }
}
