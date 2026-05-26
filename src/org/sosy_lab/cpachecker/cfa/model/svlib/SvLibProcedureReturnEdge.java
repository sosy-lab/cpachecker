// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.svlib;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;

public final class SvLibProcedureReturnEdge extends FunctionReturnEdge implements SvLibCfaEdge {
  public SvLibProcedureReturnEdge(
      FileLocation pFileLocation,
      FunctionExitNode pPredecessor,
      CFANode pSuccessor,
      SvLibProcedureSummaryEdge pSummaryEdge) {
    super(pFileLocation, pPredecessor, pSuccessor, pSummaryEdge);
  }

  @Override
  public SvLibProcedureSummaryEdge getSummaryEdge() {
    return (SvLibProcedureSummaryEdge) super.getSummaryEdge();
  }
}
