// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.java;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodOrConstructorInvocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;

public final class JMethodCallEdge extends FunctionCallEdge {

  private static final long serialVersionUID = -4905542776822697507L;

  public JMethodCallEdge(
      String pRawStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      JMethodEntryNode pSuccessor,
      JMethodOrConstructorInvocation pFunctionCall,
      JMethodSummaryEdge pSummaryEdge) {

    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor, pFunctionCall, pSummaryEdge);
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.FunctionCallEdge;
  }

  @Override
  public JMethodSummaryEdge getSummaryEdge() {
    return (JMethodSummaryEdge) super.getSummaryEdge();
  }

  @Override
  public JMethodOrConstructorInvocation getFunctionCall() {
    return (JMethodOrConstructorInvocation) super.getFunctionCall();
  }

  @Override
  public JMethodInvocationExpression getFunctionCallExpression() {
    return getFunctionCall().getFunctionCallExpression();
  }

  @Override
  public List<JExpression> getArguments() {
    return getFunctionCallExpression().getParameterExpressions();
  }

  @Override
  public String getCode() {
    return getFunctionCall().getFunctionCallExpression().toASTString();
  }

  @Override
  public JMethodEntryNode getSuccessor() {
    // the constructor enforces that the successor is always a FunctionEntryNode
    return (JMethodEntryNode) super.getSuccessor();
  }
}
