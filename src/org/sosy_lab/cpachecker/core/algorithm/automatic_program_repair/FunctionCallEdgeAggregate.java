// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;

/**
 * A function call is represented in three different edges throughout the cfa. This class is an
 * aggregate of these three edges.
 */
public class FunctionCallEdgeAggregate {
  private CFunctionSummaryEdge summaryEdge;
  private CFunctionCallEdge functionCallEdge;
  private CFunctionReturnEdge functionReturnEdge;

  public FunctionCallEdgeAggregate(
      CFunctionSummaryEdge pSummaryEdge,
      CFunctionCallEdge pFunctionCallEdge,
      CFunctionReturnEdge pFunctionReturnEdge) {
    summaryEdge = pSummaryEdge;
    functionCallEdge = pFunctionCallEdge;
    functionReturnEdge = pFunctionReturnEdge;
  }

  public FunctionCallEdgeAggregate(CFunctionSummaryEdge pSummaryEdge) {
    this(
        pSummaryEdge,
        CorrespondingEdgeProvider.findCorrespondingFunctionCallEdge(pSummaryEdge),
        CorrespondingEdgeProvider.findCorrespondingFunctionReturnEdge(pSummaryEdge));
  }

  public FunctionCallEdgeAggregate(CFunctionCallEdge pFunctionCallEdge) {
    this(
        pFunctionCallEdge.getSummaryEdge(),
        pFunctionCallEdge,
        CorrespondingEdgeProvider.findCorrespondingFunctionReturnEdge(pFunctionCallEdge.getSummaryEdge()));
  }

  public FunctionCallEdgeAggregate(CFunctionReturnEdge pFunctionReturnEdge) {
    this(
        pFunctionReturnEdge.getSummaryEdge(),
        CorrespondingEdgeProvider.findCorrespondingFunctionCallEdge(pFunctionReturnEdge.getSummaryEdge()),
        pFunctionReturnEdge);
  }

  public CFunctionCall getFunctionCall() {
    return summaryEdge.getExpression();
  }

  public CFunctionSummaryEdge getSummaryEdge() {
    return summaryEdge;
  }

  public CFunctionCallEdge getFunctionCallEdge() {
    return functionCallEdge;
  }

  public CFunctionReturnEdge getFunctionReturnEdge() {
    return functionReturnEdge;
  }
}
