// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import com.google.common.base.Optional;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;



public class FunctionCallEdge extends AbstractCFAEdge {

  private static final long serialVersionUID = -7848426105619928428L;
  protected final AFunctionCall functionCall;
  protected final FunctionSummaryEdge summaryEdge;


  protected FunctionCallEdge(String pRawStatement, FileLocation pFileLocation, CFANode pPredecessor, CFANode pSuccessor,
      AFunctionCall pFunctionCall, FunctionSummaryEdge pSummaryEdge) {
    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor);
    functionCall = pFunctionCall;
    summaryEdge = pSummaryEdge;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.FunctionCallEdge;
  }

  public FunctionSummaryEdge getSummaryEdge() {
    return  summaryEdge;
  }



  public List<? extends AExpression> getArguments() {
    return functionCall.getFunctionCallExpression().getParameterExpressions();
  }

  @Override
  public String getCode() {
    return functionCall.getFunctionCallExpression().toASTString();
  }

  @Override
  public Optional<? extends AFunctionCall> getRawAST() {
    return Optional.of(functionCall);
  }

  @Override
  public FunctionEntryNode getSuccessor() {
    // the constructor enforces that the successor is always a FunctionEntryNode
    return (FunctionEntryNode)super.getSuccessor();
  }
}