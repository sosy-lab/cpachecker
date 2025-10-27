// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.k3;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;

public class K3ProcedureReturnEdge extends FunctionReturnEdge implements K3CfaEdge {
  public K3ProcedureReturnEdge(
      FileLocation pFileLocation,
      FunctionExitNode pPredecessor,
      CFANode pSuccessor,
      K3ProcedureSummaryEdge pSummaryEdge) {
    super(pFileLocation, pPredecessor, pSuccessor, pSummaryEdge);
  }

  @Override
  public K3ProcedureSummaryEdge getSummaryEdge() {
    return (K3ProcedureSummaryEdge) super.getSummaryEdge();
  }
}
