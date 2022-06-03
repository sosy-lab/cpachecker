// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model;

import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class FunctionCallEdge extends AbstractCFAEdge {

  private static final long serialVersionUID = -7848426105619928428L;
  protected final AFunctionCall functionCall;
  protected final FunctionSummaryEdge summaryEdge;

  protected FunctionCallEdge(
      String pRawStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      AFunctionCall pFunctionCall,
      FunctionSummaryEdge pSummaryEdge) {
    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor);
    functionCall = pFunctionCall;
    summaryEdge = pSummaryEdge;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.FunctionCallEdge;
  }

  public FunctionSummaryEdge getSummaryEdge() {
    return summaryEdge;
  }

  public AFunctionCall getFunctionCall() {
    return functionCall;
  }

  public AFunctionCallExpression getFunctionCallExpression() {
    return getFunctionCall().getFunctionCallExpression();
  }

  public List<? extends AExpression> getArguments() {
    return getFunctionCallExpression().getParameterExpressions();
  }

  @Override
  public String getCode() {
    return functionCall.getFunctionCallExpression().toASTString();
  }

  @Override
  public Optional<AAstNode> getRawAST() {
    return Optional.of(functionCall);
  }

  @Override
  public FunctionEntryNode getSuccessor() {
    // the constructor enforces that the successor is always a FunctionEntryNode
    return (FunctionEntryNode) super.getSuccessor();
  }
}
