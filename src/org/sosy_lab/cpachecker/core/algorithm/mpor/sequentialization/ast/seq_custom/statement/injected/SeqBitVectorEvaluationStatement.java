// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.SeqLogicalNotExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqThreadLoopLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;

public class SeqBitVectorEvaluationStatement implements SeqInjectedStatement {

  private final Optional<SeqLogicalNotExpression> threadBitVectors;

  private final SeqThreadLoopLabelStatement gotoLabel;

  public SeqBitVectorEvaluationStatement(
      Optional<SeqLogicalNotExpression> pThreadBitVectors, SeqThreadLoopLabelStatement pGotoLabel) {

    threadBitVectors = pThreadBitVectors;
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
    SeqGotoStatement gotoStatement = new SeqGotoStatement(gotoLabel.labelName);
    // if bit vectors present: evaluate in if statement
    if (threadBitVectors.isPresent()) {
      SeqControlFlowStatement ifStatement =
          new SeqControlFlowStatement(
              threadBitVectors.orElseThrow(), SeqControlFlowStatementType.IF);
      return ifStatement.toASTString()
          + SeqSyntax.SPACE
          + SeqStringUtil.wrapInCurlyInwards(gotoStatement.toASTString());
    } else {
      // otherwise add only goto
      return gotoStatement.toASTString();
    }
  }
}
