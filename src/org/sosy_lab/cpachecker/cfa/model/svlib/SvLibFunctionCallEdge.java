// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.svlib;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

public final class SvLibFunctionCallEdge extends FunctionCallEdge implements SvLibCfaEdge {
  public SvLibFunctionCallEdge(
      String pRawStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      SvLibFunctionCallAssignmentStatement pFunctionCall,
      FunctionSummaryEdge pSummaryEdge) {
    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor, pFunctionCall, pSummaryEdge);
  }

  @Override
  public SvLibFunctionCallAssignmentStatement getFunctionCall() {
    return (SvLibFunctionCallAssignmentStatement) super.getFunctionCall();
  }

  @Override
  public SvLibFunctionCallExpression getFunctionCallExpression() {
    return getFunctionCall().getFunctionCallExpression();
  }

  @Override
  public ImmutableList<SvLibTerm> getArguments() {
    return getFunctionCallExpression().getParameterExpressions();
  }

  @Override
  public SvLibProcedureEntryNode getSuccessor() {
    // the constructor enforces that the successor is always a SvLibProcedureEntryNode
    return (SvLibProcedureEntryNode) super.getSuccessor();
  }
}
