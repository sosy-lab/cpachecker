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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.gotos.SeqGotoStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.single_control.SeqBranchStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.evaluation.BitVectorEvaluationExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * The statement for evaluating bit vectors (including {@code if (...)}). Used for both {@link
 * ReductionMode#ACCESS_ONLY} and {@link ReductionMode#READ_AND_WRITE}.
 */
public record SeqBitVectorEvaluationStatement(
    MPOROptions options,
    Optional<BitVectorEvaluationExpression> evaluationExpression,
    SeqBlockLabelStatement targetGoto)
    implements SeqInjectedStatementWithTargetGoto {

  @Override
  public String toASTString() throws UnrecognizedCodeException {
    if (evaluationExpression.isEmpty()) {
      // no evaluation due to no global accesses -> just goto
      SeqGotoStatement gotoStatement = new SeqGotoStatement(targetGoto.toCLabelStatement());
      return gotoStatement.toASTString();

    } else if (options.nondeterminismSource().equals(NondeterminismSource.NEXT_THREAD)) {
      // for next_thread nondeterminism, we use goto instead of assume, if there is no conflict
      String ifExpression = evaluationExpression.orElseThrow().toNegatedASTString();
      SeqGotoStatement gotoStatement = new SeqGotoStatement(targetGoto.toCLabelStatement());
      SeqBranchStatement ifStatement =
          new SeqBranchStatement(ifExpression, ImmutableList.of(gotoStatement.toASTString()));
      return ifStatement.toASTString();

    } else {
      return SeqAssumeFunction.buildAssumeFunctionCallStatement(
          evaluationExpression.orElseThrow().expression());
    }
  }

  @Override
  public SeqInjectedStatementWithTargetGoto withTargetNumber(int pTargetNumber) {
    return new SeqBitVectorEvaluationStatement(
        options, evaluationExpression, targetGoto.withLabelNumber(pTargetNumber));
  }

  @Override
  public boolean isPrunedWithTargetGoto() {
    return true;
  }

  @Override
  public boolean isPrunedWithEmptyBitVectorEvaluation() {
    return false;
  }
}
