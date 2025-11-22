// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.svlib;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibCfaEdgeStatement;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public final class SvLibStatementEdge extends AStatementEdge implements SvLibCfaEdge {
  public SvLibStatementEdge(
      String pRawStatement,
      SvLibCfaEdgeStatement pStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor) {
    super(pRawStatement, pStatement, pFileLocation, pPredecessor, pSuccessor);
  }

  @Override
  public SvLibCfaEdgeStatement getStatement() {
    return (SvLibCfaEdgeStatement) super.getStatement();
  }
}
