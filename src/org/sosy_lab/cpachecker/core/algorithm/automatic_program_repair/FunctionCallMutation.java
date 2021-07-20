// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;

public class FunctionCallMutation extends Mutation {
  private CFunctionSummaryEdge newSummaryEdge;
  private CFunctionCallEdge newFunctionCallEdge;
  private CFunctionReturnEdge newFunctionReturnEdge;

  public FunctionCallMutation(
      CFAEdge pSuspiciousEdge,
      CFunctionSummaryEdge pNewSummaryEdge,
      CFunctionCallEdge pNewFunctionCallEdge,
      CFunctionReturnEdge pNewFunctionReturnEdge,
      CFA pCFA) {
    super(pSuspiciousEdge, pCFA);

    newSummaryEdge = pNewSummaryEdge;
    newFunctionCallEdge = pNewFunctionCallEdge;
    newFunctionReturnEdge = pNewFunctionReturnEdge;
    exchangeEdges(List.of(newSummaryEdge, newFunctionCallEdge, newFunctionReturnEdge));
  }

  public CFAEdge getNewEdge() {
    switch (suspiciousEdge.getEdgeType()) {
      case FunctionCallEdge:
        return newFunctionCallEdge;
      case FunctionReturnEdge:
        return newFunctionReturnEdge;
      case CallToReturnEdge:
        return newSummaryEdge;
      default:
        throw new RuntimeException(
            "FunctionCallMutation can only be initialized for FunctionCallEdge, FunctionReturnEdge or CallToReturnEdge");
    }
  }

  public static void exchangeEdges(List<CFAEdge> edgesToInsert) {
    edgesToInsert.forEach(edge -> exchangeFunctionEdge(edge));
  }

  private static void exchangeFunctionEdge(CFAEdge edgeToInsert) {
    exchangeEdge(edgeToInsert);
    CFunctionSummaryEdge newSummaryEdge;
    if (edgeToInsert instanceof CFunctionCallEdge) {
      newSummaryEdge = ((CFunctionCallEdge) edgeToInsert).getSummaryEdge();
    } else if (edgeToInsert instanceof CFunctionReturnEdge) {
      newSummaryEdge = ((CFunctionReturnEdge) edgeToInsert).getSummaryEdge();
    } else if (edgeToInsert instanceof CFunctionSummaryEdge) {
      newSummaryEdge = (CFunctionSummaryEdge) edgeToInsert;
    } else {
      throw new RuntimeException(
          "Must provide either FunctionCallEdge, FunctionReturnEdge or CallToReturnEdge");
    }

    final CFANode predecessorNode = edgeToInsert.getPredecessor();
    final CFANode successorNode = edgeToInsert.getSuccessor();

    final FunctionSummaryEdge predecessorLeavingSummaryEdge =
        predecessorNode.getLeavingSummaryEdge();

    if (predecessorLeavingSummaryEdge != null
        && predecessorLeavingSummaryEdge.equals(newSummaryEdge)) {
      predecessorNode.removeLeavingSummaryEdge(predecessorLeavingSummaryEdge);
      predecessorNode.addLeavingSummaryEdge(newSummaryEdge);
    }

    final FunctionSummaryEdge successorEnteringSummaryEdge = successorNode.getEnteringSummaryEdge();

    if (successorEnteringSummaryEdge != null
        && successorEnteringSummaryEdge.equals(newSummaryEdge)) {
      successorNode.removeEnteringSummaryEdge(successorEnteringSummaryEdge);
      successorNode.addEnteringSummaryEdge(newSummaryEdge);
    }
  }
}
