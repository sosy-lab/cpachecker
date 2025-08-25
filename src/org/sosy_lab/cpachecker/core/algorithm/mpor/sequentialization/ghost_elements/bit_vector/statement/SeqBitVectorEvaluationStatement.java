// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.statement;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.assumptions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqBitVectorEvaluationStatement implements SeqInjectedBitVectorStatement {

  private final BitVectorEvaluationExpression evaluationExpression;

  public final SeqBlockLabelStatement gotoLabel;

  /**
   * The statement for evaluating bit vectors (including {@code if (...)}). Used for both {@link
   * ReductionMode#ACCESS_ONLY} and {@link ReductionMode#READ_AND_WRITE}.
   */
  public SeqBitVectorEvaluationStatement(
      BitVectorEvaluationExpression pEvaluationExpression, SeqBlockLabelStatement pGotoLabel) {

    evaluationExpression = pEvaluationExpression;
    gotoLabel = pGotoLabel;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    SeqGotoStatement gotoStatement = new SeqGotoStatement(gotoLabel);
    if (evaluationExpression.isEmpty()) {
      // TODO still needed?
      // no evaluation due to no global accesses -> just goto
      return gotoStatement.toASTString();
    } else {
      return SeqAssumptionBuilder.buildAssumption(evaluationExpression.toASTString());
    }
  }

  public SeqBitVectorEvaluationStatement cloneWithGotoLabelNumber(int pLabelNumber) {
    return new SeqBitVectorEvaluationStatement(
        evaluationExpression, gotoLabel.cloneWithLabelNumber(pLabelNumber));
  }

  public BitVectorEvaluationExpression getEvaluationExpression() {
    return evaluationExpression;
  }
}
