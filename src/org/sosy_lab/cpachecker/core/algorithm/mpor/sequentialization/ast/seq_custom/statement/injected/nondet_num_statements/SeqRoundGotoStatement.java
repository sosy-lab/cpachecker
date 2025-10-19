// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.nondet_num_statements;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqIfExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqRoundGotoStatement implements SeqInjectedStatement {

  private final CBinaryExpression roundSmallerK;

  private final CExpressionAssignmentStatement roundIncrement;

  private final SeqLabelStatement gotoLabel;

  public SeqRoundGotoStatement(
      CBinaryExpression pRoundSmallerK,
      CExpressionAssignmentStatement pRoundIncrement,
      SeqLabelStatement pGotoLabel) {

    roundSmallerK = pRoundSmallerK;
    roundIncrement = pRoundIncrement;
    gotoLabel = pGotoLabel;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    SeqIfExpression ifExpression = new SeqIfExpression(roundSmallerK);
    SeqGotoStatement gotoStatement = new SeqGotoStatement(gotoLabel);
    String innerStatement =
        roundIncrement.toASTString() + SeqSyntax.SPACE + gotoStatement.toASTString();
    return ifExpression.toASTString()
        + SeqSyntax.SPACE
        + SeqStringUtil.wrapInCurlyBracketsInwards(innerStatement);
  }
}
