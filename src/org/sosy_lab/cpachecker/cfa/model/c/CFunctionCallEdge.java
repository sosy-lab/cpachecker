// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.c;

import com.google.common.base.Optional;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;

public class CFunctionCallEdge extends FunctionCallEdge {



  private static final long serialVersionUID = -3203684033841624723L;

  public CFunctionCallEdge(String pRawStatement,
      FileLocation pFileLocation, CFANode pPredecessor, CFunctionEntryNode pSuccessor,
      CFunctionCall pFunctionCall, CFunctionSummaryEdge pSummaryEdge) {

    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor, pFunctionCall, pSummaryEdge);

  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.FunctionCallEdge;
  }

  @Override
  public CFunctionSummaryEdge getSummaryEdge() {
    return (CFunctionSummaryEdge) summaryEdge;
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<CExpression> getArguments() {
    return (List<CExpression>) functionCall.getFunctionCallExpression().getParameterExpressions();
  }

  @Override
  public String getCode() {
    return functionCall.getFunctionCallExpression().toASTString();
  }

  @Override
  public Optional<CFunctionCall> getRawAST() {
    return Optional.of((CFunctionCall)functionCall);
  }

  @Override
  public CFunctionEntryNode getSuccessor() {
    // the constructor enforces that the successor is always a FunctionEntryNode
    return (CFunctionEntryNode)super.getSuccessor();
  }
}