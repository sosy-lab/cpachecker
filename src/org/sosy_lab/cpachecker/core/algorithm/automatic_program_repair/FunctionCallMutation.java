// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;

/**
 * This class represents the mutation of a function call in the CFA. A function call is represented
 * in three different edges (summary edge, call edge, return edge). In order to mutate a function
 * call all three edges must be replaced.
 */
public class FunctionCallMutation extends Mutation {
  private final FunctionCallEdgeAggregate functionCallEdgeAggregate;
  private final FunctionCallEdgeAggregate originalFunctionCallEdgeAggregate;

  public FunctionCallMutation(
      CFAEdge pSuspiciousEdge, FunctionCallEdgeAggregate pFunctionCallEdgeAggregate, FunctionCallEdgeAggregate pOriginalFunctionCallEdgeAggregate, CFA pCFA) {
    super(pSuspiciousEdge, pCFA);

    functionCallEdgeAggregate = pFunctionCallEdgeAggregate;
    originalFunctionCallEdgeAggregate = pOriginalFunctionCallEdgeAggregate;

    exchangeEdges(
        ImmutableList.of(
            functionCallEdgeAggregate.getSummaryEdge(),
            functionCallEdgeAggregate.getFunctionCallEdge(),
            functionCallEdgeAggregate.getFunctionReturnEdge()), originalFunctionCallEdgeAggregate);
  }

  @Override
  public CFAEdge getNewEdge() {
    switch (suspiciousEdge.getEdgeType()) {
      case FunctionCallEdge:
        return functionCallEdgeAggregate.getFunctionCallEdge();
      case FunctionReturnEdge:
        return functionCallEdgeAggregate.getFunctionReturnEdge();
      case CallToReturnEdge:
        return functionCallEdgeAggregate.getSummaryEdge();

      default:
        throw new RuntimeException(
            "FunctionCallMutation can only be initialized for FunctionCallEdge, FunctionReturnEdge or CallToReturnEdge");
    }
  }

  public static void exchangeEdges(List<CFAEdge> edgesToInsert, FunctionCallEdgeAggregate originalFunctionCallEdgeAggregate) {
    edgesToInsert.forEach(edge -> exchangeFunctionEdge(edge, originalFunctionCallEdgeAggregate));
  }

  private static void exchangeFunctionEdge(CFAEdge edgeToInsert, FunctionCallEdgeAggregate originalFunctionCallEdgeAggregate) {
    CFunctionSummaryEdge newSummaryEdge;
    CFAEdge originalEdge;

    if (edgeToInsert instanceof CFunctionCallEdge) {
      newSummaryEdge = ((CFunctionCallEdge) edgeToInsert).getSummaryEdge();
      originalEdge = originalFunctionCallEdgeAggregate.getFunctionCallEdge();
    } else if (edgeToInsert instanceof CFunctionReturnEdge) {
      newSummaryEdge = ((CFunctionReturnEdge) edgeToInsert).getSummaryEdge();
      originalEdge = originalFunctionCallEdgeAggregate.getFunctionReturnEdge();
    } else if (edgeToInsert instanceof CFunctionSummaryEdge) {
      newSummaryEdge = (CFunctionSummaryEdge) edgeToInsert;
      originalEdge = originalFunctionCallEdgeAggregate.getSummaryEdge();
    } else {
      throw new RuntimeException(
          "Must provide either FunctionCallEdge, FunctionReturnEdge or CallToReturnEdge");
    }

    exchangeEdge(edgeToInsert, originalEdge);

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
