// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.nondet_num_statements;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.goto_labels.SeqLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqBranchStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqRoundGotoStatement implements SeqInjectedStatement {

  private final CBinaryExpression roundSmallerMax;

  private final CExpressionAssignmentStatement roundIncrement;

  private final SeqLabelStatement gotoLabel;

  public SeqRoundGotoStatement(
      CBinaryExpression pRoundSmallerMax,
      CExpressionAssignmentStatement pRoundIncrement,
      SeqLabelStatement pGotoLabel) {

    roundSmallerMax = pRoundSmallerMax;
    roundIncrement = pRoundIncrement;
    gotoLabel = pGotoLabel;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    SeqGotoStatement gotoStatement = new SeqGotoStatement(gotoLabel);
    SeqBranchStatement ifStatement =
        new SeqBranchStatement(
            roundSmallerMax,
            ImmutableList.of(roundIncrement.toASTString(), gotoStatement.toASTString()));
    return ifStatement.toASTString();
  }
}
