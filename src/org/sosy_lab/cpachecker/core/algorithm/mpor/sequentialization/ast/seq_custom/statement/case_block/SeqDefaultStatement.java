// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class SeqDefaultStatement implements SeqCaseBlockStatement {

  private final CStatementEdge edge;

  private final CExpressionAssignmentStatement pcUpdate;

  private final int targetPc;

  public SeqDefaultStatement(
      CStatementEdge pEdge, CExpressionAssignmentStatement pPcUpdate, int pTargetPc) {
    edge = pEdge;
    pcUpdate = pPcUpdate;
    targetPc = pTargetPc;
  }

  @Override
  public String toASTString() {
    return edge.getCode() + SeqSyntax.SPACE + pcUpdate.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(targetPc);
  }
}