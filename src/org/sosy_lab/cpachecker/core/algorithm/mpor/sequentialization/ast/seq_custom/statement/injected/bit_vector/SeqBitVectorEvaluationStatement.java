// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.injected.bit_vector;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqIfExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumptionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SeqBitVectorEvaluationStatement implements SeqInjectedBitVectorStatement {

  private final MPOROptions options;

  private final BitVectorEvaluationExpression evaluationExpression;

  public final SeqBlockLabelStatement gotoLabel;

  /**
   * The statement for evaluating bit vectors (including {@code if (...)}). Used for both {@link
   * ReductionMode#ACCESS_ONLY} and {@link ReductionMode#READ_AND_WRITE}.
   */
  public SeqBitVectorEvaluationStatement(
      MPOROptions pOptions,
      BitVectorEvaluationExpression pEvaluationExpression,
      SeqBlockLabelStatement pGotoLabel) {

    options = pOptions;
    evaluationExpression = pEvaluationExpression;
    gotoLabel = pGotoLabel;
  }

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    if (evaluationExpression.isEmpty()) {
      // no evaluation due to no global accesses -> just goto
      SeqGotoStatement gotoStatement = new SeqGotoStatement(gotoLabel);
      return gotoStatement.toASTString();

    } else if (options.nondeterminismSource.equals(NondeterminismSource.NEXT_THREAD)) {
      // for next_thread nondeterminism, we use goto instead of assume, if there is no conflict
      SeqIfExpression ifExpression = new SeqIfExpression(evaluationExpression.negate());
      SeqGotoStatement gotoStatement = new SeqGotoStatement(gotoLabel);
      return ifExpression.toASTStringWithSeqAstNodeBlock(ImmutableList.of(gotoStatement));

    } else {
      return SeqAssumptionBuilder.buildAssumption(evaluationExpression);
    }
  }

  public SeqBitVectorEvaluationStatement cloneWithGotoLabelNumber(int pLabelNumber) {
    return new SeqBitVectorEvaluationStatement(
        options, evaluationExpression, gotoLabel.cloneWithLabelNumber(pLabelNumber));
  }

  public BitVectorEvaluationExpression getEvaluationExpression() {
    return evaluationExpression;
  }
}
