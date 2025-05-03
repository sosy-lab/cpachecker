// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqSwitchCaseGotoLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqSyntax;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqBitVectorReadWriteEvaluationStatement implements SeqBitVectorEvaluationStatement {

  private final Optional<BitVectorEvaluationExpression> evaluationExpression;

  private final SeqSwitchCaseGotoLabelStatement gotoLabel;

  public SeqBitVectorReadWriteEvaluationStatement(
      Optional<BitVectorEvaluationExpression> pEvaluationExpression,
      SeqSwitchCaseGotoLabelStatement pGotoLabel) {

    evaluationExpression = pEvaluationExpression;
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
  public String toASTString() throws UnrecognizedCodeException {
    SeqGotoStatement gotoStatement = new SeqGotoStatement(gotoLabel.getLabelName());
    // if bit vectors present: evaluate in if statement
    if (evaluationExpression.isPresent()) {
      SeqControlFlowStatement ifStatement =
          new SeqControlFlowStatement(
              evaluationExpression.orElseThrow(), SeqControlFlowStatementType.IF);
      return ifStatement.toASTString()
          + SeqSyntax.SPACE
          + SeqStringUtil.wrapInCurlyInwards(gotoStatement.toASTString());
    } else {
      // otherwise add only goto
      return gotoStatement.toASTString();
    }
  }

  @Override
  public SeqBitVectorEvaluationStatement cloneWithGotoLabelNumber(int pLabelNumber) {
    return new SeqBitVectorReadWriteEvaluationStatement(
        evaluationExpression, gotoLabel.cloneWithLabelNumber(pLabelNumber));
  }
}
