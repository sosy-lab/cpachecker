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
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

public class K3ProcedureSummaryEdge extends FunctionSummaryEdge implements K3CfaEdge {
  public K3ProcedureSummaryEdge(
      String pRawStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      K3ProcedureCallStatement pExpression,
      K3ProcedureEntryNode pFunctionEntry) {
    super(pRawStatement, pFileLocation, pPredecessor, pSuccessor, pExpression, pFunctionEntry);
  }
}
