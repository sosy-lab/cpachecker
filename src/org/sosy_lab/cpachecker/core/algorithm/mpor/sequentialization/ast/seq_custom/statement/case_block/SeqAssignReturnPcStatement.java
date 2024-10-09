// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;

public class SeqAssignReturnPcStatement implements SeqCaseBlockStatement {

  private final CExpressionAssignmentStatement assign;

  public SeqAssignReturnPcStatement(CExpressionAssignmentStatement pAssign) {
    assign = pAssign;
  }

  @Override
  public String toASTString() {
    return assign.toASTString();
  }
}
