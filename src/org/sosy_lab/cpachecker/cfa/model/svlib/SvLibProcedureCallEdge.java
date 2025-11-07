// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.svlib;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

public final class SvLibProcedureCallEdge extends FunctionCallEdge implements SvLibCfaEdge {
  public SvLibProcedureCallEdge(
      String pRawStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      SvLibProcedureCallStatement pFunctionCall,
      FunctionSummaryEdge pSummaryEdge) {
    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor, pFunctionCall, pSummaryEdge);
  }

  @Override
  public SvLibProcedureCallStatement getFunctionCall() {
    return (SvLibProcedureCallStatement) super.getFunctionCall();
  }

  @Override
  public SvLibProcedureEntryNode getSuccessor() {
    // the constructor enforces that the successor is always a SvLibProcedureEntryNode
    return (SvLibProcedureEntryNode) super.getSuccessor();
  }
}
