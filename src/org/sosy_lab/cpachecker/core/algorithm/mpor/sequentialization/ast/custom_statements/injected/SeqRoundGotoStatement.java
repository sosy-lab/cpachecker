// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.gotos.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqBranchStatement;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public final class SeqRoundGotoStatement extends SeqInjectedStatement {

  private final CBinaryExpression roundSmallerMax;

  private final CExpressionAssignmentStatement roundIncrement;

  private final SeqBlockLabelStatement targetGoto;

  public SeqRoundGotoStatement(
      CBinaryExpression pRoundSmallerMax,
      CExpressionAssignmentStatement pRoundIncrement,
      SeqBlockLabelStatement pTargetGoto) {

    roundSmallerMax = pRoundSmallerMax;
    roundIncrement = pRoundIncrement;
    targetGoto = pTargetGoto;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    SeqGotoStatement gotoStatement = new SeqGotoStatement(targetGoto);
    SeqBranchStatement ifStatement =
        new SeqBranchStatement(
            roundSmallerMax.toASTString(),
            ImmutableList.of(roundIncrement.toASTString(), gotoStatement.toASTString()));
    return ifStatement.toASTString();
  }

  @Override
  public Optional<SeqBlockLabelStatement> getTargetGoto() {
    return Optional.of(targetGoto);
  }

  @Override
  public SeqRoundGotoStatement withLabelNumber(int pLabelNumber) {
    return new SeqRoundGotoStatement(
        roundSmallerMax, roundIncrement, targetGoto.withLabelNumber(pLabelNumber));
  }
}
