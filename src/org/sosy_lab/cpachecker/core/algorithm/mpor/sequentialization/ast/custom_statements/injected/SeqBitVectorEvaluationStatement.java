// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.injected;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.c.export.CCompoundStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.export.CExpressionTree;
import org.sosy_lab.cpachecker.cfa.ast.c.export.CGotoStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.export.CIfStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.export.CNegatedExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.labels.SeqBlockLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * The statement for evaluating bit vectors (including {@code if (...)}). Used for both {@link
 * ReductionMode#ACCESS_ONLY} and {@link ReductionMode#READ_AND_WRITE}.
 */
public record SeqBitVectorEvaluationStatement(
    MPOROptions options,
    Optional<CExpressionTree> evaluationExpression,
    SeqBlockLabelStatement targetGoto)
    implements SeqInjectedStatementWithTargetGoto {

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    if (evaluationExpression.isEmpty()) {
      // no evaluation due to no global accesses -> just goto
      CGotoStatement gotoStatement = new CGotoStatement(targetGoto.toCLabelStatement());
      return gotoStatement.toASTString(pAAstNodeRepresentation);

    } else if (options.nondeterminismSource().equals(NondeterminismSource.NEXT_THREAD)) {
      // for next_thread nondeterminism, we use goto instead of assume, if there is no conflict
      CNegatedExpression ifExpression = evaluationExpression.orElseThrow().negate();
      CGotoStatement gotoStatement = new CGotoStatement(targetGoto.toCLabelStatement());
      CCompoundStatement compoundStatement = new CCompoundStatement(gotoStatement);
      CIfStatement ifStatement = new CIfStatement(ifExpression, compoundStatement);
      return ifStatement.toASTString(pAAstNodeRepresentation);

    } else {
      return SeqAssumeFunction.buildAssumeFunctionCallStatement(
              evaluationExpression.orElseThrow().expressionTree())
          .toASTString(pAAstNodeRepresentation);
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
