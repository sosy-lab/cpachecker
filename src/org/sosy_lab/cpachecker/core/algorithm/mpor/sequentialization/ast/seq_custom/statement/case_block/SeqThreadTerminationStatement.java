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

public class SeqThreadTerminationStatement implements SeqCaseBlockStatement {

  private final CExpressionAssignmentStatement pcUpdate;

  // TODO add active var for main thread and make this not optional
  private final Optional<CExpressionAssignmentStatement> assign;

  public SeqThreadTerminationStatement(int pThreadId, CExpressionAssignmentStatement pAssign) {
    pcUpdate = SeqStatements.buildPcUpdate(pThreadId, SeqUtil.TERMINATION_PC);
    ;
    assign = Optional.of(pAssign);
  }

  public SeqThreadTerminationStatement(int pThreadId) {
    pcUpdate = SeqStatements.buildPcUpdate(pThreadId, SeqUtil.TERMINATION_PC);
    ;
    assign = Optional.empty();
  }

  @Override
  public String toASTString() {
    if (assign.isPresent()) {
      return assign.orElseThrow().toASTString() + SeqSyntax.SPACE + pcUpdate.toASTString();
    } else {
      return pcUpdate.toASTString();
    }
  }
}
