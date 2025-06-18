// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.nondet_num_statements;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqIfExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqRoundGotoStatement implements SeqInjectedStatement {

  private final CBinaryExpression rSmallerK;

  private final CExpressionAssignmentStatement rIncrement;

  private final SeqLabelStatement gotoLabel;

  public SeqRoundGotoStatement(
      CBinaryExpression pRSmallerK,
      CExpressionAssignmentStatement pRIncrement,
      SeqLabelStatement pGotoLabel) {

    rSmallerK = pRSmallerK;
    rIncrement = pRIncrement;
    gotoLabel = pGotoLabel;
  }

  @Override
  public Optional<CIdExpression> getIdExpression() {
    return Optional.empty();
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    SeqIfExpression ifExpression = new SeqIfExpression(rSmallerK);
    SeqGotoStatement gotoStatement = new SeqGotoStatement(gotoLabel);
    String innerStatement =
        rIncrement.toASTString() + SeqSyntax.SPACE + gotoStatement.toASTString();
    return ifExpression.toASTString()
        + SeqSyntax.SPACE
        + SeqStringUtil.wrapInCurlyBracketsInwards(innerStatement);
  }
}
