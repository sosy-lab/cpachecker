// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqThreadLoopLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public class SeqThreadLoopGotoStatement implements SeqInjectedStatement {

  private final CBinaryExpression rSmallerK;

  private final SeqThreadLoopLabelStatement gotoLabel;

  public SeqThreadLoopGotoStatement(
      CBinaryExpression pRSmallerK, SeqThreadLoopLabelStatement pGotoLabel) {

    rSmallerK = pRSmallerK;
    gotoLabel = pGotoLabel;
  }

  @Override
  public boolean priorCriticalSection() {
    return false;
  }

  @Override
  public Optional<CIdExpression> getIdExpression() {
    return Optional.empty();
  }

  @Override
  public String toASTString() {
    SeqControlFlowStatement ifStatement =
        new SeqControlFlowStatement(rSmallerK, SeqControlFlowStatementType.IF);
    SeqGotoStatement gotoStatement = new SeqGotoStatement(gotoLabel.labelName);
    return ifStatement.toASTString()
        + SeqSyntax.SPACE
        + SeqStringUtil.wrapInCurlyInwards(gotoStatement.toASTString());
  }
}
