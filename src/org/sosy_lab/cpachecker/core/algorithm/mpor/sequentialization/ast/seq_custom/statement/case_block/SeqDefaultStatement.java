// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class SeqDefaultStatement implements SeqCaseBlockStatement {

  private final CStatementEdge edge;

  private final CExpressionAssignmentStatement pcUpdate;

  public SeqDefaultStatement(CStatementEdge pEdge, CExpressionAssignmentStatement pPcUpdate) {
    edge = pEdge;
    pcUpdate = pPcUpdate;
  }

  @Override
  public String toASTString() {
    return edge.getCode() + SeqSyntax.SPACE + pcUpdate.toASTString();
  }
}
