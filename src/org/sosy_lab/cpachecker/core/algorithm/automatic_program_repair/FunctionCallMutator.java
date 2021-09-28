// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;

public class FunctionCallMutator extends EdgeMutator {
  private final FunctionCallEdgeAggregate functionCallEdgeAggregate;

  public FunctionCallMutator(
      CFA cfa,
      Configuration config,
      LogManager logger,
      FunctionCallEdgeAggregate pFunctionCallEdgeAggregate) {
    super(cfa, config, logger);
    functionCallEdgeAggregate =
        new FunctionCallEdgeAggregate(
            (CFunctionSummaryEdge)
                CorrespondingEdgeProvider.findCorrespondingEdge(
                    pFunctionCallEdgeAggregate.getSummaryEdge(), getClonedCFA()),
            (CFunctionCallEdge)
                CorrespondingEdgeProvider.findCorrespondingEdge(
                    pFunctionCallEdgeAggregate.getFunctionCallEdge(), getClonedCFA()),
            (CFunctionReturnEdge)
                CorrespondingEdgeProvider.findCorrespondingEdge(
                    pFunctionCallEdgeAggregate.getFunctionReturnEdge(), getClonedCFA()));
  }

  public FunctionCallEdgeAggregate replaceFunctionCall(CFunctionCall newFunctionCall) {
    CFunctionSummaryEdge newFunctionSummaryEdge =
        replaceFunctionCallInFunctionSummaryEdge(newFunctionCall);
    CFunctionCallEdge newFunctionCallEdge =
        replaceFunctionCallInFunctionCallEdge(newFunctionSummaryEdge, newFunctionCall);
    CFunctionReturnEdge newFunctionReturnEdge =
        replaceFunctionCallInFunctionReturnEdge(newFunctionSummaryEdge);

    return new FunctionCallEdgeAggregate(
        newFunctionSummaryEdge, newFunctionCallEdge, newFunctionReturnEdge);
  }

  /** Returns a new function call edge with a different function call. */
  private CFunctionCallEdge replaceFunctionCallInFunctionCallEdge(
      CFunctionSummaryEdge summaryEdge, CFunctionCall newFunctionCall) {
    CFunctionCallEdge functionCallEdge = functionCallEdgeAggregate.getFunctionCallEdge();

    return new CFunctionCallEdge(
        functionCallEdge.getRawStatement(),
        functionCallEdge.getFileLocation(),
        functionCallEdge.getPredecessor(),
        functionCallEdge.getSuccessor(),
        newFunctionCall,
        summaryEdge);
  }

  /** Returns a new function summary edge with a different function call. */
  private CFunctionSummaryEdge replaceFunctionCallInFunctionSummaryEdge(
      CFunctionCall newFunctionCall) {
    CFunctionSummaryEdge summaryEdge = functionCallEdgeAggregate.getSummaryEdge();

    return new CFunctionSummaryEdge(
        newFunctionCall.toASTString(),
        summaryEdge.getFileLocation(),
        summaryEdge.getPredecessor(),
        summaryEdge.getSuccessor(),
        newFunctionCall,
        summaryEdge.getFunctionEntry());
  }

  /** Returns a new function return edge with a different function call. */
  private CFunctionReturnEdge replaceFunctionCallInFunctionReturnEdge(
      CFunctionSummaryEdge functionSummaryEdge) {
    CFunctionReturnEdge functionReturnEdge = functionCallEdgeAggregate.getFunctionReturnEdge();

    return new CFunctionReturnEdge(
        functionReturnEdge.getFileLocation(),
        functionReturnEdge.getPredecessor(),
        functionReturnEdge.getSuccessor(),
        functionSummaryEdge);
  }
}
