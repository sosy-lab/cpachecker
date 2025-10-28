// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.k3;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ProcedureCallStatement;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

public final class K3ProcedureCallEdge extends FunctionCallEdge implements K3CfaEdge {
  public K3ProcedureCallEdge(
      String pRawStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      K3ProcedureCallStatement pFunctionCall,
      FunctionSummaryEdge pSummaryEdge) {
    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor, pFunctionCall, pSummaryEdge);
  }

  @Override
  public K3ProcedureCallStatement getFunctionCall() {
    return (K3ProcedureCallStatement) super.getFunctionCall();
  }

  @Override
  public K3ProcedureEntryNode getSuccessor() {
    // the constructor enforces that the successor is always a K3ProcedureEntryNode
    return (K3ProcedureEntryNode) super.getSuccessor();
  }
}
