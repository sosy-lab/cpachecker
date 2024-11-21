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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;

public class SeqThreadExitStatement implements SeqCaseBlockStatement {

  private final CExpressionAssignmentStatement pcUpdate;

  private final CExpressionAssignmentStatement assign;

  private final int targetPc;

  public SeqThreadExitStatement(
      int pThreadId, CExpressionAssignmentStatement pAssign, int pTargetPc) {

    pcUpdate = SeqStatements.buildPcUpdate(pThreadId, SeqUtil.EXIT_PC);
    assign = pAssign;
    targetPc = pTargetPc;
  }

  @Override
  public String toASTString() {
    return assign.toASTString() + SeqSyntax.SPACE + pcUpdate.toASTString();
  }

  @Override
  public Optional<Integer> getTargetPc() {
    return Optional.of(targetPc);
  }
}
