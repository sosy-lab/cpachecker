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
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

public final class SvLibProcedureSummaryEdge extends FunctionSummaryEdge implements SvLibCfaEdge {
  public SvLibProcedureSummaryEdge(
      String pRawStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      SvLibProcedureCallStatement pExpression,
      SvLibProcedureEntryNode pFunctionEntry) {
    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor, pExpression, pFunctionEntry);
  }

  @Override
  public SvLibProcedureCallStatement getExpression() {
    return (SvLibProcedureCallStatement) super.getExpression();
  }
}
